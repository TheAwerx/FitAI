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
Object.defineProperty(exports, "__esModule", { value: true });
exports.PLAN_QUOTA_ERRORS = exports.PLAN_QUOTA = void 0;
exports.withPlanQuota = withPlanQuota;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
/**
 * Workout plan regeneration rights ("Planı yenile"). Mirror of `PlanQuotaPolicy` in
 * `core/domain/.../access/PlanQuota.kt`; the limit stored in `plan_quotas/{uid}` is what the app shows.
 *
 * - `limit`: successful regenerations per period.
 * - `resetPeriodDays`: days until `used` starts over; null means never.
 * - `initialFreeLimit`: free "initial" generations per account (plan setup, plus a retry or a
 *   new-device restore). Past it, initial calls are refused instead of spending a right silently.
 * - `inFlightSeconds`: one generation at a time per account, so parallel calls cannot overspend.
 *   Must outlive the callable timeout (120 s).
 */
exports.PLAN_QUOTA = {
    limit: 3,
    resetPeriodDays: null,
    initialFreeLimit: 3,
    inFlightSeconds: 150,
};
/** Error messages the app maps to `PlanQuotaErrors`. */
exports.PLAN_QUOTA_ERRORS = {
    exhausted: "plan_quota_exhausted",
    inProgress: "plan_generation_in_progress",
    initialLimit: "plan_initial_limit",
};
const DAY_MS = 24 * 60 * 60 * 1000;
function quotaRef(uid) {
    return admin.firestore().collection("plan_quotas").doc(uid);
}
function asRecord(value) {
    return value != null && typeof value === "object" && !Array.isArray(value) ?
        value :
        null;
}
/**
 * New clients send `{ profile, planPurpose }`. Older builds send the bare profile; those calls
 * count as regenerations.
 */
function unwrapPlanRequest(data) {
    const record = asRecord(data);
    const profile = record ? asRecord(record.profile) : null;
    if (record && profile) {
        return { purpose: record.planPurpose === "initial" ? "initial" : "regenerate", profile };
    }
    return { purpose: "regenerate", profile: data };
}
/** `used` for the current period; a finished period reads as zero. */
function currentUsed(doc, nowMs) {
    var _a, _b;
    const used = Math.max(0, Number((_a = doc.used) !== null && _a !== void 0 ? _a : 0));
    const start = (_b = doc.periodStartedAtMs) !== null && _b !== void 0 ? _b : null;
    if (exports.PLAN_QUOTA.resetPeriodDays == null || start == null)
        return { used, periodStartedAtMs: start };
    if (nowMs >= start + exports.PLAN_QUOTA.resetPeriodDays * DAY_MS)
        return { used: 0, periodStartedAtMs: null };
    return { used, periodStartedAtMs: start };
}
/** Checks the quota and takes the per-account lock. Nothing is spent here. */
async function reserve(uid, purpose) {
    const ref = quotaRef(uid);
    await admin.firestore().runTransaction(async (tx) => {
        var _a, _b, _c;
        const snap = await tx.get(ref);
        const doc = ((_a = snap.data()) !== null && _a !== void 0 ? _a : {});
        const now = Date.now();
        if (((_b = doc.inFlightUntilMs) !== null && _b !== void 0 ? _b : 0) > now) {
            throw new https_1.HttpsError("aborted", exports.PLAN_QUOTA_ERRORS.inProgress);
        }
        if (purpose === "initial") {
            if (Number((_c = doc.initialUsed) !== null && _c !== void 0 ? _c : 0) >= exports.PLAN_QUOTA.initialFreeLimit) {
                throw new https_1.HttpsError("resource-exhausted", exports.PLAN_QUOTA_ERRORS.initialLimit);
            }
        }
        else {
            const { used } = currentUsed(doc, now);
            if (used >= exports.PLAN_QUOTA.limit) {
                throw new https_1.HttpsError("resource-exhausted", exports.PLAN_QUOTA_ERRORS.exhausted, {
                    limit: exports.PLAN_QUOTA.limit,
                    remaining: 0,
                });
            }
        }
        tx.set(ref, { inFlightUntilMs: now + exports.PLAN_QUOTA.inFlightSeconds * 1000 }, { merge: true });
    });
}
/** Spends the right (or the free initial generation) after the plan was built. */
async function commit(uid, purpose) {
    const ref = quotaRef(uid);
    await admin.firestore().runTransaction(async (tx) => {
        var _a, _b;
        const snap = await tx.get(ref);
        const doc = ((_a = snap.data()) !== null && _a !== void 0 ? _a : {});
        const now = Date.now();
        const { used, periodStartedAtMs } = currentUsed(doc, now);
        const update = {
            limit: exports.PLAN_QUOTA.limit,
            resetPeriodDays: exports.PLAN_QUOTA.resetPeriodDays,
            inFlightUntilMs: null,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        if (purpose === "initial") {
            update.initialUsed = Number((_b = doc.initialUsed) !== null && _b !== void 0 ? _b : 0) + 1;
            update.used = used;
            update.periodStartedAtMs = periodStartedAtMs;
        }
        else {
            update.used = used + 1;
            update.periodStartedAtMs = periodStartedAtMs !== null && periodStartedAtMs !== void 0 ? periodStartedAtMs : now;
        }
        tx.set(ref, update, { merge: true });
    });
}
async function release(uid) {
    await quotaRef(uid).set({ inFlightUntilMs: null }, { merge: true }).catch(() => undefined);
}
/**
 * Wraps the `generateWorkoutPlan` handler. The handler receives the bare profile as `request.data`.
 * A right is spent only after the handler returns; failures release the lock and spend nothing.
 * `plan_quotas/{uid}` is written here only (Admin SDK); clients may read their own document.
 */
function withPlanQuota(handler) {
    return async (request) => {
        var _a;
        const uid = (_a = request.auth) === null || _a === void 0 ? void 0 : _a.uid;
        if (!uid) {
            throw new https_1.HttpsError("unauthenticated", "Authentication required");
        }
        const { purpose, profile } = unwrapPlanRequest(request.data);
        await reserve(uid, purpose);
        let result;
        try {
            result = await handler(Object.assign(Object.assign({}, request), { data: profile }));
        }
        catch (error) {
            await release(uid);
            throw error;
        }
        try {
            await commit(uid, purpose);
        }
        catch (error) {
            // The plan is already built; do not fail the call because the counter write did.
            console.error("plan quota commit failed", uid, error);
            await release(uid);
        }
        return result;
    };
}
//# sourceMappingURL=planQuota.js.map