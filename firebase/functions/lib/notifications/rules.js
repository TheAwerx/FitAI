"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.defaultTrainingDays = defaultTrainingDays;
exports.effectiveTrainingDays = effectiveTrainingDays;
exports.factsFromState = factsFromState;
exports.lapseInfo = lapseInfo;
exports.evaluate = evaluate;
exports.applySent = applySent;
exports.nextCheck = nextCheck;
const config_1 = require("./config");
const streak_1 = require("./streak");
const time_1 = require("./time");
const MEAL_KINDS = ["meal_morning", "meal_afternoon", "meal_evening"];
/** Highest first when two windows are open at once. */
const PRIORITY = ["streak_risk", "workout_late", "comeback", "meal_evening", "meal_afternoon", "meal_morning"];
/** A kind is skipped for the day once one of these went out (they ask for the same thing). */
const SUPPRESSED_BY = {
    streak_risk: ["workout_late"],
    workout_late: ["streak_risk"],
    meal_evening: ["streak_risk"],
};
/** Same spread as `ReminderPlanner.defaultTrainingDays`, used while there is no program. */
function defaultTrainingDays(daysPerWeek) {
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
function effectiveTrainingDays(state) {
    const plan = [...new Set(state.planTrainingDays.filter((d) => d >= 1 && d <= 7))].sort();
    return plan.length > 0 ? plan : defaultTrainingDays(state.daysPerWeek);
}
function workoutWindow(state) {
    const raw = state.reminderHour * 60 + state.reminderMinute + config_1.NOTIFICATION_CONFIG.workoutGraceMinutes;
    const start = Math.max(raw, config_1.NOTIFICATION_CONFIG.awakeStartMinute);
    const end = Math.min(raw + config_1.NOTIFICATION_CONFIG.workoutWindowMinutes, config_1.NOTIFICATION_CONFIG.awakeEndMinute);
    return start < end ? { start, end } : null;
}
function windowsFor(date, state) {
    const t = state.types;
    const list = [];
    if (t.streakRisk)
        list.push(Object.assign({ kind: "streak_risk", pref: "streakRisk", link: "home" }, config_1.NOTIFICATION_CONFIG.streakRisk));
    if (t.workoutLate && effectiveTrainingDays(state).includes((0, time_1.isoWeekday)(date))) {
        const w = workoutWindow(state);
        if (w)
            list.push(Object.assign({ kind: "workout_late", pref: "workoutLate", link: "workout" }, w));
    }
    if (t.comeback)
        list.push(Object.assign({ kind: "comeback", pref: "comeback", link: "workout" }, config_1.NOTIFICATION_CONFIG.comeback));
    const sinceSignup = state.createdDate == null ? Infinity : (0, time_1.daysBetween)(state.createdDate, date);
    if (t.mealLogging && sinceSignup >= config_1.NOTIFICATION_CONFIG.mealQuietDaysAfterSignup) {
        list.push(Object.assign({ kind: "meal_evening", pref: "mealLogging", link: "meal_log" }, config_1.NOTIFICATION_CONFIG.mealEvening));
        list.push(Object.assign({ kind: "meal_afternoon", pref: "mealLogging", link: "meal_log" }, config_1.NOTIFICATION_CONFIG.mealAfternoon));
        list.push(Object.assign({ kind: "meal_morning", pref: "mealLogging", link: "meal_log" }, config_1.NOTIFICATION_CONFIG.mealMorning));
    }
    return list
        .filter((w) => w.start >= config_1.NOTIFICATION_CONFIG.awakeStartMinute && w.end <= config_1.NOTIFICATION_CONFIG.awakeEndMinute)
        .sort((a, b) => PRIORITY.indexOf(a.kind) - PRIORITY.indexOf(b.kind));
}
/** Cheap facts from the trigger-maintained state; the sweep re-reads today's documents before sending. */
function factsFromState(state, date) {
    const mealLoggedToday = state.lastMealDate === date;
    const mealParts = mealLoggedToday ? (0, time_1.localPartsOfIso)(state.lastMealAt, (0, time_1.safeTimeZone)(state.timeZone)) : null;
    return {
        workoutCompletedToday: state.lastWorkoutDate === date,
        sessionInProgress: false,
        mealLoggedToday,
        latestMealMinute: mealLoggedToday ? ((mealParts === null || mealParts === void 0 ? void 0 : mealParts.date) === date ? mealParts.minute : 0) : null,
    };
}
/** Lapsed users get only comeback nudges; see `lapsedMissedTrainingDays` in config.ts. */
function lapseInfo(state, date, activeToday) {
    const earlier = state.activeDates.filter((d) => d < date);
    const lastActive = earlier.length > 0 ? earlier[earlier.length - 1] : null;
    const anchor = lastActive !== null && lastActive !== void 0 ? lastActive : state.createdDate;
    const neverActive = lastActive == null;
    if (activeToday || anchor == null || anchor >= date) {
        return { lapsed: false, anchor, neverActive, daysSince: 0 };
    }
    const daysSince = (0, time_1.daysBetween)(anchor, date);
    const training = effectiveTrainingDays(state);
    let missed = 0;
    for (let i = 1; i < daysSince; i++) {
        if (training.includes((0, time_1.isoWeekday)((0, time_1.addDays)(anchor, i))))
            missed++;
    }
    const lapsed = daysSince >= config_1.NOTIFICATION_CONFIG.lapsedMinDays && missed >= config_1.NOTIFICATION_CONFIG.lapsedMissedTrainingDays;
    return { lapsed, anchor, neverActive, daysSince };
}
function comebackStage(state, date, lapse) {
    if (!lapse.lapsed || lapse.anchor == null)
        return null;
    if (state.createdDate != null && (0, time_1.daysBetween)(state.createdDate, date) < config_1.NOTIFICATION_CONFIG.comebackQuietDaysAfterSignup)
        return null;
    const count = state.comebackAnchor === lapse.anchor ? state.comebackCount : 0;
    if (count === 0)
        return { variant: lapse.neverActive ? "start" : "first", anchor: lapse.anchor };
    const sinceFirst = state.comebackLastDate == null ? Infinity : (0, time_1.daysBetween)(state.comebackLastDate, date);
    if (count === 1 && lapse.daysSince >= config_1.NOTIFICATION_CONFIG.secondComebackMinDays && sinceFirst >= config_1.NOTIFICATION_CONFIG.secondComebackAfterFirstDays) {
        return { variant: "second", anchor: lapse.anchor };
    }
    return null;
}
function check(kind, state, date, facts) {
    const activeToday = facts.workoutCompletedToday || facts.mealLoggedToday || state.activeDates.includes(date);
    const lapse = lapseInfo(state, date, activeToday);
    if (kind === "comeback") {
        const stage = comebackStage(state, date, lapse);
        return stage ? { ok: true, variant: stage.variant, comebackAnchor: stage.anchor } : { ok: false };
    }
    if (lapse.lapsed)
        return { ok: false };
    switch (kind) {
        case "workout_late":
            return { ok: !facts.workoutCompletedToday && !facts.sessionInProgress, variant: "default" };
        case "meal_morning":
            return { ok: !facts.mealLoggedToday, variant: "default" };
        case "meal_afternoon":
            return { ok: facts.latestMealMinute == null || facts.latestMealMinute < config_1.NOTIFICATION_CONFIG.mealAfternoon.since, variant: "default" };
        case "meal_evening":
            return { ok: facts.latestMealMinute == null || facts.latestMealMinute < config_1.NOTIFICATION_CONFIG.mealEvening.since, variant: "default" };
        case "streak_risk": {
            if (activeToday)
                return { ok: false };
            const streak = (0, streak_1.computeStreak)(date, state.activeDates, state.planTrainingDays, config_1.NOTIFICATION_CONFIG.streakLookbackDays);
            const required = state.planTrainingDays.length === 0 || state.planTrainingDays.includes((0, time_1.isoWeekday)(date));
            const ok = required && streak.freezeUsedThisWeek && streak.currentDays >= config_1.NOTIFICATION_CONFIG.streakRisk.minDays;
            return { ok, variant: "default", streakDays: streak.currentDays };
        }
    }
}
function blockedBy(kind, sentToday) {
    var _a;
    if (sentToday.includes(kind))
        return "already_sent";
    if (sentToday.length >= config_1.NOTIFICATION_CONFIG.maxPerDay)
        return "daily_cap";
    if (MEAL_KINDS.includes(kind) && sentToday.filter((k) => MEAL_KINDS.includes(k)).length >= config_1.NOTIFICATION_CONFIG.maxMealNudgesPerDay) {
        return "meal_cap";
    }
    if (((_a = SUPPRESSED_BY[kind]) !== null && _a !== void 0 ? _a : []).some((k) => sentToday.includes(k)))
        return "suppressed";
    return null;
}
function sentKindsOn(state, date) {
    return state.sentDate === date ? state.sentKinds : [];
}
/**
 * Decides what to send now. [facts] defaults to the cheap state view; pass exact facts read from today's
 * documents to confirm a candidate. `nextCheckAtMs` assumes the returned nudge is sent.
 */
function evaluate(state, nowMs, facts) {
    var _a;
    const timeZone = (0, time_1.safeTimeZone)(state.timeZone);
    const now = (0, time_1.localParts)(nowMs, timeZone);
    const base = { localDate: now.date, localMinute: now.minute };
    if (!state.enabled)
        return Object.assign(Object.assign({}, base), { send: null, nextCheckAtMs: null, reason: "disabled" });
    if (state.deviceCount <= 0)
        return Object.assign(Object.assign({}, base), { send: null, nextCheckAtMs: null, reason: "no_devices" });
    const dayFacts = facts !== null && facts !== void 0 ? facts : factsFromState(state, now.date);
    const sentToday = sentKindsOn(state, now.date);
    let send = null;
    let comebackAnchor;
    let reason = "no_window";
    let gapRetryAtMs = null;
    if (now.minute < config_1.NOTIFICATION_CONFIG.awakeStartMinute || now.minute >= config_1.NOTIFICATION_CONFIG.awakeEndMinute) {
        reason = "quiet_hours";
    }
    else if (state.createdDate === now.date) {
        reason = "signup_day";
    }
    else {
        for (const w of windowsFor(now.date, state)) {
            if (now.minute < w.start || now.minute >= w.end)
                continue;
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
            const windowEndMs = (0, time_1.zonedTimeToUtc)(now.date, w.end, timeZone);
            if (state.lastSentAtMs != null && nowMs - state.lastSentAtMs < config_1.NOTIFICATION_CONFIG.minGapMinutes * 60000) {
                const retry = state.lastSentAtMs + config_1.NOTIFICATION_CONFIG.minGapMinutes * 60000;
                if (retry < windowEndMs)
                    gapRetryAtMs = gapRetryAtMs == null ? retry : Math.min(gapRetryAtMs, retry);
                reason = `${w.kind}:gap`;
                continue;
            }
            send = {
                kind: w.kind,
                pref: w.pref,
                link: w.link,
                variant: (_a = verdict.variant) !== null && _a !== void 0 ? _a : "default",
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
    return Object.assign(Object.assign({}, base), { send, nextCheckAtMs, reason, comebackAnchor });
}
/** The state after [nudge] went out: sent log, gap clock and the comeback counter. */
function applySent(state, nudge, nowMs, localDate, comebackAnchor) {
    const sentKinds = [...sentKindsOn(state, localDate), nudge.kind];
    const next = Object.assign(Object.assign({}, state), { sentDate: localDate, sentKinds, lastSentAtMs: nowMs });
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
function nextCheck(state, nowMs, gapRetryAtMs = null) {
    if (!state.enabled || state.deviceCount <= 0)
        return null;
    const timeZone = (0, time_1.safeTimeZone)(state.timeZone);
    const now = (0, time_1.localParts)(nowMs, timeZone);
    const gapUntil = state.lastSentAtMs == null ? -Infinity : state.lastSentAtMs + config_1.NOTIFICATION_CONFIG.minGapMinutes * 60000;
    let best = gapRetryAtMs;
    for (let offset = 0; offset <= config_1.NOTIFICATION_CONFIG.scheduleHorizonDays; offset++) {
        const date = (0, time_1.addDays)(now.date, offset);
        if (date === state.createdDate)
            continue;
        const sent = sentKindsOn(state, date);
        const activeOnDate = state.activeDates.includes(date) || state.lastWorkoutDate === date || state.lastMealDate === date;
        const lapse = lapseInfo(state, date, activeOnDate);
        for (const w of windowsFor(date, state)) {
            if (offset === 0 && w.start <= now.minute)
                continue;
            if (blockedBy(w.kind, sent))
                continue;
            if (w.kind === "comeback" ? comebackStage(state, date, lapse) == null : lapse.lapsed)
                continue;
            let at = (0, time_1.zonedTimeToUtc)(date, w.start, timeZone);
            if (at < gapUntil) {
                if (gapUntil >= (0, time_1.zonedTimeToUtc)(date, w.end, timeZone))
                    continue;
                at = gapUntil;
            }
            if (at <= nowMs)
                continue;
            best = best == null ? at : Math.min(best, at);
        }
        if (best != null && best <= (0, time_1.zonedTimeToUtc)((0, time_1.addDays)(date, 1), 0, timeZone))
            break;
    }
    return best;
}
//# sourceMappingURL=rules.js.map