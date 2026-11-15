import { NOTIFICATION_CONFIG as C } from "./config";
import { computeStreak } from "./streak";
import { addDays, daysBetween, isoWeekday, localParts, localPartsOfIso, safeTimeZone, zonedTimeToUtc } from "./time";
import { DayFacts, DeepLink, Evaluation, NotificationTypeKey, Nudge, NudgeKind, NudgeState } from "./types";

/*
 * Pure decision logic: given the user's state, today's facts and "now", which nudge (if any) goes out and
 * when to look again. No Firestore here, so it is unit tested directly (test/rules.test.mjs).
 */

interface WindowSpec {
  kind: NudgeKind;
  pref: NotificationTypeKey;
  link: DeepLink;
  start: number;
  end: number;
}

const MEAL_KINDS: NudgeKind[] = ["meal_morning", "meal_afternoon", "meal_evening"];

/** Highest first when two windows are open at once. */
const PRIORITY: NudgeKind[] = ["streak_risk", "workout_late", "comeback", "meal_evening", "meal_afternoon", "meal_morning"];

/** A kind is skipped for the day once one of these went out (they ask for the same thing). */
const SUPPRESSED_BY: Partial<Record<NudgeKind, NudgeKind[]>> = {
  streak_risk: ["workout_late"],
  workout_late: ["streak_risk"],
  meal_evening: ["streak_risk"],
};

/** Same spread as `ReminderPlanner.defaultTrainingDays`, used while there is no program. */
export function defaultTrainingDays(daysPerWeek: number | null | undefined): number[] {
  const days = daysPerWeek == null || !Number.isFinite(daysPerWeek) ? 3 : Math.min(7, Math.max(1, Math.round(daysPerWeek)));
  switch (days) {
    case 1: return [3];
    case 2: return [2, 5];
    case 3: return [1, 3, 5];
    case 4: return [1, 2, 4, 5];
    case 5: return [1, 2, 3, 4, 5];
    case 6: return [1, 2, 3, 4, 5, 6];
    default: return [1, 2, 3, 4, 5, 6, 7];
  }
}

/** Training days for "workout late" and lapse counting: the program's days, else the commitment spread. */
export function effectiveTrainingDays(state: NudgeState): number[] {
  const plan = [...new Set(state.planTrainingDays.filter((d) => d >= 1 && d <= 7))].sort();
  return plan.length > 0 ? plan : defaultTrainingDays(state.daysPerWeek);
}

function workoutWindow(state: NudgeState): { start: number; end: number } | null {
  const raw = state.reminderHour * 60 + state.reminderMinute + C.workoutGraceMinutes;
  const start = Math.max(raw, C.awakeStartMinute);
  const end = Math.min(raw + C.workoutWindowMinutes, C.awakeEndMinute);
  return start < end ? { start, end } : null;
}

function windowsFor(date: string, state: NudgeState): WindowSpec[] {
  const t = state.types;
  const list: WindowSpec[] = [];
  if (t.streakRisk) list.push({ kind: "streak_risk", pref: "streakRisk", link: "home", ...C.streakRisk });
  if (t.workoutLate && effectiveTrainingDays(state).includes(isoWeekday(date))) {
    const w = workoutWindow(state);
    if (w) list.push({ kind: "workout_late", pref: "workoutLate", link: "workout", ...w });
  }
  if (t.comeback) list.push({ kind: "comeback", pref: "comeback", link: "workout", ...C.comeback });
  const sinceSignup = state.createdDate == null ? Infinity : daysBetween(state.createdDate, date);
  if (t.mealLogging && sinceSignup >= C.mealQuietDaysAfterSignup) {
    list.push({ kind: "meal_evening", pref: "mealLogging", link: "meal_log", ...C.mealEvening });
    list.push({ kind: "meal_afternoon", pref: "mealLogging", link: "meal_log", ...C.mealAfternoon });
    list.push({ kind: "meal_morning", pref: "mealLogging", link: "meal_log", ...C.mealMorning });
  }
  return list
    .filter((w) => w.start >= C.awakeStartMinute && w.end <= C.awakeEndMinute)
    .sort((a, b) => PRIORITY.indexOf(a.kind) - PRIORITY.indexOf(b.kind));
}

/** Cheap facts from the trigger-maintained state; the sweep re-reads today's documents before sending. */
export function factsFromState(state: NudgeState, date: string): DayFacts {
  const mealLoggedToday = state.lastMealDate === date;
  const mealParts = mealLoggedToday ? localPartsOfIso(state.lastMealAt, safeTimeZone(state.timeZone)) : null;
  return {
    workoutCompletedToday: state.lastWorkoutDate === date,
    sessionInProgress: false,
    mealLoggedToday,
    latestMealMinute: mealLoggedToday ? (mealParts?.date === date ? mealParts.minute : 0) : null,
  };
}

export interface LapseInfo {
  lapsed: boolean;
  /** Last active day before [date] (or the sign-up day); null when unknown. */
  anchor: string | null;
  neverActive: boolean;
  daysSince: number;
}

/** Lapsed users get only comeback nudges; see `lapsedMissedTrainingDays` in config.ts. */
export function lapseInfo(state: NudgeState, date: string, activeToday: boolean): LapseInfo {
  const earlier = state.activeDates.filter((d) => d < date);
  const lastActive = earlier.length > 0 ? earlier[earlier.length - 1] : null;
  const anchor = lastActive ?? state.createdDate;
  const neverActive = lastActive == null;
  if (activeToday || anchor == null || anchor >= date) {
    return { lapsed: false, anchor, neverActive, daysSince: 0 };
  }
  const daysSince = daysBetween(anchor, date);
  const training = effectiveTrainingDays(state);
  let missed = 0;
  for (let i = 1; i < daysSince; i++) {
    if (training.includes(isoWeekday(addDays(anchor, i)))) missed++;
  }
  const lapsed = daysSince >= C.lapsedMinDays && missed >= C.lapsedMissedTrainingDays;
  return { lapsed, anchor, neverActive, daysSince };
}

interface ComebackStage {
  variant: "start" | "first" | "second";
  anchor: string;
}

function comebackStage(state: NudgeState, date: string, lapse: LapseInfo): ComebackStage | null {
  if (!lapse.lapsed || lapse.anchor == null) return null;
  if (state.createdDate != null && daysBetween(state.createdDate, date) < C.comebackQuietDaysAfterSignup) return null;
  const count = state.comebackAnchor === lapse.anchor ? state.comebackCount : 0;
  if (count === 0) return { variant: lapse.neverActive ? "start" : "first", anchor: lapse.anchor };
  const sinceFirst = state.comebackLastDate == null ? Infinity : daysBetween(state.comebackLastDate, date);
  if (count === 1 && lapse.daysSince >= C.secondComebackMinDays && sinceFirst >= C.secondComebackAfterFirstDays) {
    return { variant: "second", anchor: lapse.anchor };
  }
  return null;
}

interface Verdict {
  ok: boolean;
  variant?: string;
  streakDays?: number;
  comebackAnchor?: string;
}

function check(kind: NudgeKind, state: NudgeState, date: string, facts: DayFacts): Verdict {
  const activeToday = facts.workoutCompletedToday || facts.mealLoggedToday || state.activeDates.includes(date);
  const lapse = lapseInfo(state, date, activeToday);
  if (kind === "comeback") {
    const stage = comebackStage(state, date, lapse);
    return stage ? { ok: true, variant: stage.variant, comebackAnchor: stage.anchor } : { ok: false };
  }
  if (lapse.lapsed) return { ok: false };
  switch (kind) {
    case "workout_late":
      return { ok: !facts.workoutCompletedToday && !facts.sessionInProgress, variant: "default" };
    case "meal_morning":
      return { ok: !facts.mealLoggedToday, variant: "default" };
    case "meal_afternoon":
      return { ok: facts.latestMealMinute == null || facts.latestMealMinute < C.mealAfternoon.since, variant: "default" };
    case "meal_evening":
      return { ok: facts.latestMealMinute == null || facts.latestMealMinute < C.mealEvening.since, variant: "default" };
    case "streak_risk": {
      if (activeToday) return { ok: false };
      const streak = computeStreak(date, state.activeDates, state.planTrainingDays, C.streakLookbackDays);
      const required = state.planTrainingDays.length === 0 || state.planTrainingDays.includes(isoWeekday(date));
      const ok = required && streak.freezeUsedThisWeek && streak.currentDays >= C.streakRisk.minDays;
      return { ok, variant: "default", streakDays: streak.currentDays };
    }
  }
}

function blockedBy(kind: NudgeKind, sentToday: NudgeKind[]): string | null {
  if (sentToday.includes(kind)) return "already_sent";
  if (sentToday.length >= C.maxPerDay) return "daily_cap";
  if (MEAL_KINDS.includes(kind) && sentToday.filter((k) => MEAL_KINDS.includes(k)).length >= C.maxMealNudgesPerDay) {
    return "meal_cap";
  }
  if ((SUPPRESSED_BY[kind] ?? []).some((k) => sentToday.includes(k))) return "suppressed";
  return null;
}

function sentKindsOn(state: NudgeState, date: string): NudgeKind[] {
  return state.sentDate === date ? state.sentKinds : [];
}

/**
 * Decides what to send now. [facts] defaults to the cheap state view; pass exact facts read from today's
 * documents to confirm a candidate. `nextCheckAtMs` assumes the returned nudge is sent.
 */
export function evaluate(state: NudgeState, nowMs: number, facts?: DayFacts): Evaluation {
  const timeZone = safeTimeZone(state.timeZone);
  const now = localParts(nowMs, timeZone);
  const base = { localDate: now.date, localMinute: now.minute };
  if (!state.enabled) return { ...base, send: null, nextCheckAtMs: null, reason: "disabled" };
  if (state.deviceCount <= 0) return { ...base, send: null, nextCheckAtMs: null, reason: "no_devices" };

  const dayFacts = facts ?? factsFromState(state, now.date);
  const sentToday = sentKindsOn(state, now.date);
  let send: Nudge | null = null;
  let comebackAnchor: string | undefined;
  let reason = "no_window";
  let gapRetryAtMs: number | null = null;

  if (now.minute < C.awakeStartMinute || now.minute >= C.awakeEndMinute) {
    reason = "quiet_hours";
  } else if (state.createdDate === now.date) {
    reason = "signup_day";
  } else {
    for (const w of windowsFor(now.date, state)) {
      if (now.minute < w.start || now.minute >= w.end) continue;
      const blocked = blockedBy(w.kind, sentToday);
      if (blocked) {
        reason = `${w.kind}:${blocked}`;
        continue;
      }
      const verdict = check(w.kind, state, now.date, dayFacts);
      if (!verdict.ok) {
        reason = `${w.kind}:condition`;
        continue;
      }
      const windowEndMs = zonedTimeToUtc(now.date, w.end, timeZone);
      if (state.lastSentAtMs != null && nowMs - state.lastSentAtMs < C.minGapMinutes * 60_000) {
        const retry = state.lastSentAtMs + C.minGapMinutes * 60_000;
        if (retry < windowEndMs) gapRetryAtMs = gapRetryAtMs == null ? retry : Math.min(gapRetryAtMs, retry);
        reason = `${w.kind}:gap`;
        continue;
      }
      send = {
        kind: w.kind,
        pref: w.pref,
        link: w.link,
        variant: verdict.variant ?? "default",
        streakDays: verdict.streakDays,
        windowEndMs,
      };
      comebackAnchor = verdict.comebackAnchor;
      reason = "send";
      break;
    }
  }

  const after = send ? applySent(state, send, nowMs, now.date, comebackAnchor) : state;
  const nextCheckAtMs = nextCheck(after, nowMs, gapRetryAtMs);
  return { ...base, send, nextCheckAtMs, reason, comebackAnchor };
}

/** The state after [nudge] went out: sent log, gap clock and the comeback counter. */
export function applySent(
  state: NudgeState,
  nudge: Nudge,
  nowMs: number,
  localDate: string,
  comebackAnchor?: string,
): NudgeState {
  const sentKinds = [...sentKindsOn(state, localDate), nudge.kind];
  const next: NudgeState = { ...state, sentDate: localDate, sentKinds, lastSentAtMs: nowMs };
  if (nudge.kind === "comeback" && comebackAnchor) {
    const count = state.comebackAnchor === comebackAnchor ? state.comebackCount : 0;
    next.comebackAnchor = comebackAnchor;
    next.comebackCount = count + 1;
    next.comebackLastDate = localDate;
  }
  return next;
}

/**
 * Next window start worth a look: later windows today, then the following days. Windows already open
 * were just evaluated and are not revisited, except when only the minimum gap held them back.
 * Days the user would be lapsed (assuming no new activity) only schedule the comeback window; new
 * activity reschedules through the session and meal triggers.
 */
export function nextCheck(state: NudgeState, nowMs: number, gapRetryAtMs: number | null = null): number | null {
  if (!state.enabled || state.deviceCount <= 0) return null;
  const timeZone = safeTimeZone(state.timeZone);
  const now = localParts(nowMs, timeZone);
  const gapUntil = state.lastSentAtMs == null ? -Infinity : state.lastSentAtMs + C.minGapMinutes * 60_000;
  let best = gapRetryAtMs;
  for (let offset = 0; offset <= C.scheduleHorizonDays; offset++) {
    const date = addDays(now.date, offset);
    if (date === state.createdDate) continue;
    const sent = sentKindsOn(state, date);
    const activeOnDate = state.activeDates.includes(date) || state.lastWorkoutDate === date || state.lastMealDate === date;
    const lapse = lapseInfo(state, date, activeOnDate);
    for (const w of windowsFor(date, state)) {
      if (offset === 0 && w.start <= now.minute) continue;
      if (blockedBy(w.kind, sent)) continue;
      if (w.kind === "comeback" ? comebackStage(state, date, lapse) == null : lapse.lapsed) continue;
      let at = zonedTimeToUtc(date, w.start, timeZone);
      if (at < gapUntil) {
        if (gapUntil >= zonedTimeToUtc(date, w.end, timeZone)) continue;
        at = gapUntil;
      }
      if (at <= nowMs) continue;
      best = best == null ? at : Math.min(best, at);
    }
    if (best != null && best <= zonedTimeToUtc(addDays(date, 1), 0, timeZone)) break;
  }
  return best;
}
