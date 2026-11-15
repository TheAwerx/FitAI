"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onUserWritten = onUserWritten;
exports.onDeviceWritten = onDeviceWritten;
exports.onWorkoutPlanWritten = onWorkoutPlanWritten;
exports.onWorkoutSessionWritten = onWorkoutSessionWritten;
exports.onMealLogCreated = onMealLogCreated;
const state_1 = require("./state");
const time_1 = require("./time");
/*
 * Firestore trigger bodies, kept free of the functions SDK so the emulator test can call them directly.
 * Each one is idempotent: triggers are delivered at least once and not in order.
 */
/** `users/{uid}`: notification switches, reminder time, language and time zone. */
async function onUserWritten(db, uid, before, after, nowMs = Date.now()) {
    if (!after) {
        await (0, state_1.stateRef)(db, uid).delete();
        return;
    }
    if (!(0, state_1.settingsChanged)(before, after))
        return;
    await (0, state_1.updateState)(db, uid, (state) => (Object.assign(Object.assign({}, state), (0, state_1.settingsFromUser)(after))), nowMs);
}
/** `users/{uid}/devices/{installationId}`: no devices means nothing is scheduled. */
async function onDeviceWritten(db, uid, nowMs = Date.now()) {
    const deviceCount = await (0, state_1.countDevices)(db, uid);
    await (0, state_1.updateState)(db, uid, (state) => (state.deviceCount === deviceCount ? null : Object.assign(Object.assign({}, state), { deviceCount })), nowMs);
}
/** `users/{uid}/workout_plans/{planId}`: training days of the active program. */
async function onWorkoutPlanWritten(db, uid, planId, before, after, nowMs = Date.now()) {
    const beforeDays = (0, state_1.activeProgramDays)(before);
    const afterDays = (0, state_1.activeProgramDays)(after);
    if (JSON.stringify(beforeDays) === JSON.stringify(afterDays))
        return;
    await (0, state_1.updateState)(db, uid, (state) => {
        if (afterDays) {
            const same = state.activePlanId === planId && JSON.stringify(state.planTrainingDays) === JSON.stringify(afterDays);
            return same ? null : Object.assign(Object.assign({}, state), { activePlanId: planId, planTrainingDays: afterDays });
        }
        // A plan that stopped being active only clears the state if it was the one recorded. The new plan's
        // own trigger may already have run (writes land in any order).
        return state.activePlanId === planId ? Object.assign(Object.assign({}, state), { activePlanId: null, planTrainingDays: [] }) : null;
    }, nowMs);
}
/** `users/{uid}/workout_sessions/{id}`: a session became completed, so today is done. */
async function onWorkoutSessionWritten(db, uid, before, after, nowMs = Date.now()) {
    if ((after === null || after === void 0 ? void 0 : after.completed) !== true || (before === null || before === void 0 ? void 0 : before.completed) === true)
        return;
    const date = after.localDate;
    if (!(0, time_1.isLocalDate)(date))
        return;
    await (0, state_1.updateState)(db, uid, (state) => {
        const next = (0, state_1.withActivity)(state, date, { workout: true });
        return JSON.stringify(next) === JSON.stringify(state) ? null : next;
    }, nowMs);
}
/** `users/{uid}/meal_logs/{id}` created: a meal was logged. */
async function onMealLogCreated(db, uid, meal, nowMs = Date.now()) {
    const date = meal === null || meal === void 0 ? void 0 : meal.localDate;
    if (!(0, time_1.isLocalDate)(date))
        return;
    const loggedAt = typeof (meal === null || meal === void 0 ? void 0 : meal.loggedAt) === "string" ? meal.loggedAt : null;
    await (0, state_1.updateState)(db, uid, (state) => {
        const next = (0, state_1.withActivity)(state, date, { mealLoggedAt: loggedAt });
        return JSON.stringify(next) === JSON.stringify(state) ? null : next;
    }, nowMs);
}
//# sourceMappingURL=triggers.js.map