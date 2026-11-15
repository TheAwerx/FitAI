"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.STATE_SCHEMA_VERSION = exports.STATE_COLLECTION = void 0;
exports.stateRef = stateRef;
exports.parseTypes = parseTypes;
exports.emptyState = emptyState;
exports.settingsFromUser = settingsFromUser;
exports.settingsChanged = settingsChanged;
exports.activeProgramDays = activeProgramDays;
exports.withActivity = withActivity;
exports.stateFromDoc = stateFromDoc;
exports.stateToDoc = stateToDoc;
exports.nextCheckAfterUpdate = nextCheckAfterUpdate;
exports.countDevices = countDevices;
exports.loadInitialState = loadInitialState;
exports.updateState = updateState;
const admin = __importStar(require("firebase-admin"));
const config_1 = require("./config");
const rules_1 = require("./rules");
const time_1 = require("./time");
const types_1 = require("./types");
const { Timestamp, FieldValue } = admin.firestore;
/**
 * `notification_state/{uid}`: one server-only document per account (rules deny all client access).
 * It copies the few `users/{uid}` fields the nudges need, keeps a small activity summary maintained by
 * the session and meal triggers, the per-day sent log, and `nextCheckAt`, the only field the sweep
 * queries. Functions never add fields to `users/{uid}` itself (the client overwrites it).
 */
exports.STATE_COLLECTION = "notification_state";
exports.STATE_SCHEMA_VERSION = 1;
const NUDGE_KINDS = ["workout_late", "meal_morning", "meal_afternoon", "meal_evening", "streak_risk", "comeback"];
function stateRef(db, uid) {
    return db.collection(exports.STATE_COLLECTION).doc(uid);
}
function userRef(db, uid) {
    return db.collection("users").doc(uid);
}
function int(value, fallback, min, max) {
    const n = typeof value === "number" && Number.isFinite(value) ? Math.round(value) : fallback;
    return Math.min(max, Math.max(min, n));
}
function str(value) {
    return typeof value === "string" && value.length > 0 ? value : null;
}
function dateOrNull(value) {
    return (0, time_1.isLocalDate)(value) ? value : null;
}
function millis(value) {
    if (value instanceof Timestamp)
        return value.toMillis();
    if (typeof value === "number" && Number.isFinite(value))
        return value;
    return null;
}
function parseTypes(raw) {
    const record = raw != null && typeof raw === "object" ? raw : {};
    const types = Object.assign({}, types_1.DEFAULT_NOTIFICATION_TYPES);
    for (const key of types_1.NOTIFICATION_TYPE_KEYS) {
        if (typeof record[key] === "boolean")
            types[key] = record[key];
    }
    return types;
}
function emptyState() {
    return {
        enabled: false,
        types: Object.assign({}, types_1.DEFAULT_NOTIFICATION_TYPES),
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
function settingsFromUser(user) {
    var _a;
    const timeZone = (0, time_1.safeTimeZone)(user.timeZone);
    const created = (0, time_1.localPartsOfIso)(user.createdAt, timeZone);
    return {
        enabled: user.notificationsEnabled === true,
        types: parseTypes(user.notificationTypes),
        language: (user.language === "en" ? "en" : "tr"),
        timeZone,
        reminderHour: int(user.reminderHour, 18, 0, 23),
        reminderMinute: int(user.reminderMinute, 0, 0, 59),
        daysPerWeek: int(user.daysPerWeek, 3, 1, 7),
        createdDate: (_a = created === null || created === void 0 ? void 0 : created.date) !== null && _a !== void 0 ? _a : null,
    };
}
/** Did a `users/{uid}` write change anything [settingsFromUser] reads? */
function settingsChanged(before, after) {
    if (!before || !after)
        return true;
    return JSON.stringify(settingsFromUser(before)) !== JSON.stringify(settingsFromUser(after));
}
/** ISO weekdays of an active program, or null when [plan] is not the active program. */
function activeProgramDays(plan) {
    var _a;
    if (!plan || plan.isActive !== true || plan.kind === "adhoc" || Number((_a = plan.daysPerWeek) !== null && _a !== void 0 ? _a : 0) <= 0)
        return null;
    const exercises = Array.isArray(plan.exercises) ? plan.exercises : [];
    const days = new Set();
    for (const e of exercises) {
        const day = e === null || e === void 0 ? void 0 : e.dayOfWeek;
        if (typeof day === "number" && day >= 1 && day <= 7)
            days.add(day);
    }
    return [...days].sort();
}
/** Records one active day (completed session or meal). Idempotent, so trigger retries are harmless. */
function withActivity(state, date, activity) {
    var _a;
    const dates = [...new Set([...state.activeDates, date])].sort().slice(-config_1.NOTIFICATION_CONFIG.maxActiveDates);
    const next = Object.assign(Object.assign({}, state), { activeDates: dates });
    if (activity.workout && (state.lastWorkoutDate == null || date > state.lastWorkoutDate)) {
        next.lastWorkoutDate = date;
    }
    if (activity.mealLoggedAt !== undefined) {
        const at = (_a = activity.mealLoggedAt) !== null && _a !== void 0 ? _a : null;
        if (state.lastMealDate == null || date > state.lastMealDate) {
            next.lastMealDate = date;
            next.lastMealAt = at;
        }
        else if (date === state.lastMealDate && at != null && (state.lastMealAt == null || at > state.lastMealAt)) {
            next.lastMealAt = at;
        }
    }
    return next;
}
function stateFromDoc(data) {
    const base = emptyState();
    if (!data)
        return base;
    return {
        enabled: data.enabled === true,
        types: parseTypes(data.types),
        language: data.language === "en" ? "en" : "tr",
        timeZone: (0, time_1.safeTimeZone)(data.timeZone),
        reminderHour: int(data.reminderHour, base.reminderHour, 0, 23),
        reminderMinute: int(data.reminderMinute, base.reminderMinute, 0, 59),
        daysPerWeek: int(data.daysPerWeek, base.daysPerWeek, 1, 7),
        createdDate: dateOrNull(data.createdDate),
        activePlanId: str(data.activePlanId),
        planTrainingDays: Array.isArray(data.planTrainingDays) ?
            data.planTrainingDays.filter((d) => typeof d === "number" && d >= 1 && d <= 7) : [],
        deviceCount: int(data.deviceCount, 0, 0, 1000),
        activeDates: Array.isArray(data.activeDates) ? data.activeDates.filter(time_1.isLocalDate).sort() : [],
        lastWorkoutDate: dateOrNull(data.lastWorkoutDate),
        lastMealDate: dateOrNull(data.lastMealDate),
        lastMealAt: str(data.lastMealAt),
        sentDate: dateOrNull(data.sentDate),
        sentKinds: Array.isArray(data.sentKinds) ? data.sentKinds.filter((k) => NUDGE_KINDS.includes(k)) : [],
        lastSentAtMs: millis(data.lastSentAt),
        comebackAnchor: dateOrNull(data.comebackAnchor),
        comebackCount: int(data.comebackCount, 0, 0, 100),
        comebackLastDate: dateOrNull(data.comebackLastDate),
    };
}
function stateToDoc(state, nextCheckAtMs) {
    const { lastSentAtMs } = state, rest = __rest(state, ["lastSentAtMs"]);
    return Object.assign(Object.assign({ schemaVersion: exports.STATE_SCHEMA_VERSION }, rest), { lastSentAt: lastSentAtMs == null ? null : Timestamp.fromMillis(lastSentAtMs), nextCheckAt: nextCheckAtMs == null ? null : Timestamp.fromMillis(nextCheckAtMs), updatedAt: FieldValue.serverTimestamp() });
}
/** After a settings, device, plan or activity change: due now if a window is open, else the next window. */
function nextCheckAfterUpdate(state, nowMs) {
    const evaluation = (0, rules_1.evaluate)(state, nowMs);
    return evaluation.send ? nowMs : evaluation.nextCheckAtMs;
}
async function countDevices(db, uid) {
    const snap = await userRef(db, uid).collection("devices").count().get();
    return snap.data().count;
}
/**
 * First state for an account: settings, devices, the active program and the last weeks of activity.
 * Runs once per account (the first trigger after this feature ships). Null without `users/{uid}`.
 */
async function loadInitialState(db, uid, nowMs) {
    const user = await userRef(db, uid).get();
    if (!user.exists)
        return null;
    let state = Object.assign(Object.assign({}, emptyState()), settingsFromUser(user.data()));
    state.deviceCount = await countDevices(db, uid);
    const plans = await userRef(db, uid).collection("workout_plans").where("isActive", "==", true).get();
    const program = plans.docs
        .map((d) => ({ id: d.id, data: d.data(), days: activeProgramDays(d.data()) }))
        .filter((p) => p.days != null)
        .sort((a, b) => { var _a, _b; return String((_a = b.data.createdAt) !== null && _a !== void 0 ? _a : "").localeCompare(String((_b = a.data.createdAt) !== null && _b !== void 0 ? _b : "")); })[0];
    if (program) {
        state.activePlanId = program.id;
        state.planTrainingDays = program.days;
    }
    const since = (0, time_1.addDays)((0, time_1.localParts)(nowMs, state.timeZone).date, -config_1.NOTIFICATION_CONFIG.maxActiveDates);
    const [sessions, meals] = await Promise.all([
        userRef(db, uid).collection("workout_sessions").where("localDate", ">=", since).select("localDate", "completed").get(),
        userRef(db, uid).collection("meal_logs").where("localDate", ">=", since).select("localDate", "loggedAt").get(),
    ]);
    for (const s of sessions.docs) {
        const date = s.get("localDate");
        if (s.get("completed") === true && (0, time_1.isLocalDate)(date))
            state = withActivity(state, date, { workout: true });
    }
    for (const m of meals.docs) {
        const date = m.get("localDate");
        if ((0, time_1.isLocalDate)(date))
            state = withActivity(state, date, { mealLoggedAt: str(m.get("loggedAt")) });
    }
    return state;
}
/**
 * Applies [mutate] to the account's state in a transaction and reschedules it. [mutate] returns null
 * for "no change". A missing state is built first with [loadInitialState].
 */
async function updateState(db, uid, mutate, nowMs = Date.now()) {
    const ref = stateRef(db, uid);
    const existing = await ref.get();
    const initial = existing.exists ? null : await loadInitialState(db, uid, nowMs);
    if (!existing.exists && !initial)
        return null;
    return db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const current = snap.exists ? stateFromDoc(snap.data()) : initial;
        if (!current)
            return null;
        const next = mutate(current);
        if (!next) {
            if (!snap.exists)
                tx.set(ref, stateToDoc(current, nextCheckAfterUpdate(current, nowMs)));
            return current;
        }
        tx.set(ref, stateToDoc(next, nextCheckAfterUpdate(next, nowMs)));
        return next;
    });
}
//# sourceMappingURL=state.js.map