import * as admin from "firebase-admin";
import { NOTIFICATION_CONFIG as C } from "./config";
import { evaluate } from "./rules";
import { addDays, isLocalDate, localParts, localPartsOfIso, safeTimeZone } from "./time";
import {
  DEFAULT_NOTIFICATION_TYPES,
  Language,
  NOTIFICATION_TYPE_KEYS,
  NotificationTypes,
  NudgeKind,
  NudgeState,
} from "./types";

type Firestore = admin.firestore.Firestore;
type DocData = admin.firestore.DocumentData;
const { Timestamp, FieldValue } = admin.firestore;

/**
 * `notification_state/{uid}`: one server-only document per account (rules deny all client access).
 * It copies the few `users/{uid}` fields the nudges need, keeps a small activity summary maintained by
 * the session and meal triggers, the per-day sent log, and `nextCheckAt`, the only field the sweep
 * queries. Functions never add fields to `users/{uid}` itself (the client overwrites it).
 */
export const STATE_COLLECTION = "notification_state";
export const STATE_SCHEMA_VERSION = 1;

const NUDGE_KINDS: NudgeKind[] = ["workout_late", "meal_morning", "meal_afternoon", "meal_evening", "streak_risk", "comeback"];

export function stateRef(db: Firestore, uid: string) {
  return db.collection(STATE_COLLECTION).doc(uid);
}

function userRef(db: Firestore, uid: string) {
  return db.collection("users").doc(uid);
}

function int(value: unknown, fallback: number, min: number, max: number): number {
  const n = typeof value === "number" && Number.isFinite(value) ? Math.round(value) : fallback;
  return Math.min(max, Math.max(min, n));
}

function str(value: unknown): string | null {
  return typeof value === "string" && value.length > 0 ? value : null;
}

function dateOrNull(value: unknown): string | null {
  return isLocalDate(value) ? value : null;
}

function millis(value: unknown): number | null {
  if (value instanceof Timestamp) return value.toMillis();
  if (typeof value === "number" && Number.isFinite(value)) return value;
  return null;
}

export function parseTypes(raw: unknown): NotificationTypes {
  const record = raw != null && typeof raw === "object" ? (raw as Record<string, unknown>) : {};
  const types = { ...DEFAULT_NOTIFICATION_TYPES };
  for (const key of NOTIFICATION_TYPE_KEYS) {
    if (typeof record[key] === "boolean") types[key] = record[key] as boolean;
  }
  return types;
}

export function emptyState(): NudgeState {
  return {
    enabled: false,
    types: { ...DEFAULT_NOTIFICATION_TYPES },
    language: "tr",
    timeZone: "UTC",
    reminderHour: 18,
    reminderMinute: 0,
    daysPerWeek: 3,
    createdDate: null,
    activePlanId: null,
    planTrainingDays: [],
    deviceCount: 0,
    activeDates: [],
    lastWorkoutDate: null,
    lastMealDate: null,
    lastMealAt: null,
    sentDate: null,
    sentKinds: [],
    lastSentAtMs: null,
    comebackAnchor: null,
    comebackCount: 0,
    comebackLastDate: null,
  };
}

/** The fields of `users/{uid}` (see SCHEMA.md) that drive nudges. */
export function settingsFromUser(user: DocData): Partial<NudgeState> {
  const timeZone = safeTimeZone(user.timeZone);
  const created = localPartsOfIso(user.createdAt, timeZone);
  return {
    enabled: user.notificationsEnabled === true,
    types: parseTypes(user.notificationTypes),
    language: (user.language === "en" ? "en" : "tr") as Language,
    timeZone,
    reminderHour: int(user.reminderHour, 18, 0, 23),
    reminderMinute: int(user.reminderMinute, 0, 0, 59),
    daysPerWeek: int(user.daysPerWeek, 3, 1, 7),
    createdDate: created?.date ?? null,
  };
}

/** Did a `users/{uid}` write change anything [settingsFromUser] reads? */
export function settingsChanged(before: DocData | undefined, after: DocData | undefined): boolean {
  if (!before || !after) return true;
  return JSON.stringify(settingsFromUser(before)) !== JSON.stringify(settingsFromUser(after));
}

/** ISO weekdays of an active program, or null when [plan] is not the active program. */
export function activeProgramDays(plan: DocData | undefined): number[] | null {
  if (!plan || plan.isActive !== true || plan.kind === "adhoc" || Number(plan.daysPerWeek ?? 0) <= 0) return null;
  const exercises: unknown[] = Array.isArray(plan.exercises) ? plan.exercises : [];
  const days = new Set<number>();
  for (const e of exercises) {
    const day = (e as Record<string, unknown> | null)?.dayOfWeek;
    if (typeof day === "number" && day >= 1 && day <= 7) days.add(day);
  }
  return [...days].sort();
}

/** Records one active day (completed session or meal). Idempotent, so trigger retries are harmless. */
export function withActivity(
  state: NudgeState,
  date: string,
  activity: { workout?: boolean; mealLoggedAt?: string | null },
): NudgeState {
  const dates = [...new Set([...state.activeDates, date])].sort().slice(-C.maxActiveDates);
  const next: NudgeState = { ...state, activeDates: dates };
  if (activity.workout && (state.lastWorkoutDate == null || date > state.lastWorkoutDate)) {
    next.lastWorkoutDate = date;
  }
  if (activity.mealLoggedAt !== undefined) {
    const at = activity.mealLoggedAt ?? null;
    if (state.lastMealDate == null || date > state.lastMealDate) {
      next.lastMealDate = date;
      next.lastMealAt = at;
    } else if (date === state.lastMealDate && at != null && (state.lastMealAt == null || at > state.lastMealAt)) {
      next.lastMealAt = at;
    }
  }
  return next;
}

export function stateFromDoc(data: DocData | undefined): NudgeState {
  const base = emptyState();
  if (!data) return base;
  return {
    enabled: data.enabled === true,
    types: parseTypes(data.types),
    language: data.language === "en" ? "en" : "tr",
    timeZone: safeTimeZone(data.timeZone),
    reminderHour: int(data.reminderHour, base.reminderHour, 0, 23),
    reminderMinute: int(data.reminderMinute, base.reminderMinute, 0, 59),
    daysPerWeek: int(data.daysPerWeek, base.daysPerWeek, 1, 7),
    createdDate: dateOrNull(data.createdDate),
    activePlanId: str(data.activePlanId),
    planTrainingDays: Array.isArray(data.planTrainingDays) ?
      data.planTrainingDays.filter((d: unknown) => typeof d === "number" && d >= 1 && d <= 7) : [],
    deviceCount: int(data.deviceCount, 0, 0, 1000),
    activeDates: Array.isArray(data.activeDates) ? data.activeDates.filter(isLocalDate).sort() : [],
    lastWorkoutDate: dateOrNull(data.lastWorkoutDate),
    lastMealDate: dateOrNull(data.lastMealDate),
    lastMealAt: str(data.lastMealAt),
    sentDate: dateOrNull(data.sentDate),
    sentKinds: Array.isArray(data.sentKinds) ? data.sentKinds.filter((k: unknown) => NUDGE_KINDS.includes(k as NudgeKind)) : [],
    lastSentAtMs: millis(data.lastSentAt),
    comebackAnchor: dateOrNull(data.comebackAnchor),
    comebackCount: int(data.comebackCount, 0, 0, 100),
    comebackLastDate: dateOrNull(data.comebackLastDate),
  };
}

export function stateToDoc(state: NudgeState, nextCheckAtMs: number | null): DocData {
  const { lastSentAtMs, ...rest } = state;
  return {
    schemaVersion: STATE_SCHEMA_VERSION,
    ...rest,
    lastSentAt: lastSentAtMs == null ? null : Timestamp.fromMillis(lastSentAtMs),
    nextCheckAt: nextCheckAtMs == null ? null : Timestamp.fromMillis(nextCheckAtMs),
    updatedAt: FieldValue.serverTimestamp(),
  };
}

/** After a settings, device, plan or activity change: due now if a window is open, else the next window. */
export function nextCheckAfterUpdate(state: NudgeState, nowMs: number): number | null {
  const evaluation = evaluate(state, nowMs);
  return evaluation.send ? nowMs : evaluation.nextCheckAtMs;
}

export async function countDevices(db: Firestore, uid: string): Promise<number> {
  const snap = await userRef(db, uid).collection("devices").count().get();
  return snap.data().count;
}

/**
 * First state for an account: settings, devices, the active program and the last weeks of activity.
 * Runs once per account (the first trigger after this feature ships). Null without `users/{uid}`.
 */
export async function loadInitialState(db: Firestore, uid: string, nowMs: number): Promise<NudgeState | null> {
  const user = await userRef(db, uid).get();
  if (!user.exists) return null;
  let state: NudgeState = { ...emptyState(), ...settingsFromUser(user.data()!) };
  state.deviceCount = await countDevices(db, uid);

  const plans = await userRef(db, uid).collection("workout_plans").where("isActive", "==", true).get();
  const program = plans.docs
    .map((d) => ({ id: d.id, data: d.data(), days: activeProgramDays(d.data()) }))
    .filter((p) => p.days != null)
    .sort((a, b) => String(b.data.createdAt ?? "").localeCompare(String(a.data.createdAt ?? "")))[0];
  if (program) {
    state.activePlanId = program.id;
    state.planTrainingDays = program.days!;
  }

  const since = addDays(localParts(nowMs, state.timeZone).date, -C.maxActiveDates);
  const [sessions, meals] = await Promise.all([
    userRef(db, uid).collection("workout_sessions").where("localDate", ">=", since).select("localDate", "completed").get(),
    userRef(db, uid).collection("meal_logs").where("localDate", ">=", since).select("localDate", "loggedAt").get(),
  ]);
  for (const s of sessions.docs) {
    const date = s.get("localDate");
    if (s.get("completed") === true && isLocalDate(date)) state = withActivity(state, date, { workout: true });
  }
  for (const m of meals.docs) {
    const date = m.get("localDate");
    if (isLocalDate(date)) state = withActivity(state, date, { mealLoggedAt: str(m.get("loggedAt")) });
  }
  return state;
}

/**
 * Applies [mutate] to the account's state in a transaction and reschedules it. [mutate] returns null
 * for "no change". A missing state is built first with [loadInitialState].
 */
export async function updateState(
  db: Firestore,
  uid: string,
  mutate: (state: NudgeState) => NudgeState | null,
  nowMs: number = Date.now(),
): Promise<NudgeState | null> {
  const ref = stateRef(db, uid);
  const existing = await ref.get();
  const initial = existing.exists ? null : await loadInitialState(db, uid, nowMs);
  if (!existing.exists && !initial) return null;
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const current = snap.exists ? stateFromDoc(snap.data()) : initial;
    if (!current) return null;
    const next = mutate(current);
    if (!next) {
      if (!snap.exists) tx.set(ref, stateToDoc(current, nextCheckAfterUpdate(current, nowMs)));
      return current;
    }
    tx.set(ref, stateToDoc(next, nextCheckAfterUpdate(next, nowMs)));
    return next;
  });
}
