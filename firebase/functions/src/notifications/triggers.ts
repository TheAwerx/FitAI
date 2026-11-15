import * as admin from "firebase-admin";
import {
  activeProgramDays,
  countDevices,
  settingsChanged,
  settingsFromUser,
  stateRef,
  updateState,
  withActivity,
} from "./state";
import { isLocalDate } from "./time";

type Firestore = admin.firestore.Firestore;
type DocData = admin.firestore.DocumentData;

/*
 * Firestore trigger bodies, kept free of the functions SDK so the emulator test can call them directly.
 * Each one is idempotent: triggers are delivered at least once and not in order.
 */

/** `users/{uid}`: notification switches, reminder time, language and time zone. */
export async function onUserWritten(
  db: Firestore,
  uid: string,
  before: DocData | undefined,
  after: DocData | undefined,
  nowMs = Date.now(),
) {
  if (!after) {
    await stateRef(db, uid).delete();
    return;
  }
  if (!settingsChanged(before, after)) return;
  await updateState(db, uid, (state) => ({ ...state, ...settingsFromUser(after) }), nowMs);
}

/** `users/{uid}/devices/{installationId}`: no devices means nothing is scheduled. */
export async function onDeviceWritten(db: Firestore, uid: string, nowMs = Date.now()) {
  const deviceCount = await countDevices(db, uid);
  await updateState(db, uid, (state) => (state.deviceCount === deviceCount ? null : { ...state, deviceCount }), nowMs);
}

/** `users/{uid}/workout_plans/{planId}`: training days of the active program. */
export async function onWorkoutPlanWritten(
  db: Firestore,
  uid: string,
  planId: string,
  before: DocData | undefined,
  after: DocData | undefined,
  nowMs = Date.now(),
) {
  const beforeDays = activeProgramDays(before);
  const afterDays = activeProgramDays(after);
  if (JSON.stringify(beforeDays) === JSON.stringify(afterDays)) return;
  await updateState(db, uid, (state) => {
    if (afterDays) {
      const same = state.activePlanId === planId && JSON.stringify(state.planTrainingDays) === JSON.stringify(afterDays);
      return same ? null : { ...state, activePlanId: planId, planTrainingDays: afterDays };
    }
    // A plan that stopped being active only clears the state if it was the one recorded. The new plan's
    // own trigger may already have run (writes land in any order).
    return state.activePlanId === planId ? { ...state, activePlanId: null, planTrainingDays: [] } : null;
  }, nowMs);
}

/** `users/{uid}/workout_sessions/{id}`: a session became completed, so today is done. */
export async function onWorkoutSessionWritten(
  db: Firestore,
  uid: string,
  before: DocData | undefined,
  after: DocData | undefined,
  nowMs = Date.now(),
) {
  if (after?.completed !== true || before?.completed === true) return;
  const date = after.localDate;
  if (!isLocalDate(date)) return;
  await updateState(db, uid, (state) => {
    const next = withActivity(state, date, { workout: true });
    return JSON.stringify(next) === JSON.stringify(state) ? null : next;
  }, nowMs);
}

/** `users/{uid}/meal_logs/{id}` created: a meal was logged. */
export async function onMealLogCreated(db: Firestore, uid: string, meal: DocData | undefined, nowMs = Date.now()) {
  const date = meal?.localDate;
  if (!isLocalDate(date)) return;
  const loggedAt = typeof meal?.loggedAt === "string" ? meal.loggedAt : null;
  await updateState(db, uid, (state) => {
    const next = withActivity(state, date, { mealLoggedAt: loggedAt });
    return JSON.stringify(next) === JSON.stringify(state) ? null : next;
  }, nowMs);
}
