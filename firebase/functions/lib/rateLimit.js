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
exports.AI_DAILY_LIMITS = void 0;
exports.withDailyQuota = withDailyQuota;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
exports.AI_DAILY_LIMITS = {
    suggestNextMeal: 30,
    adaptTodaySession: 10,
    coachChat: 60,
    weeklyCoachReport: 5,
};
const USAGE_RETENTION_DAYS = 7;
function usageRef(uid) {
    const day = new Date().toISOString().slice(0, 10);
    return admin.firestore().collection("aiUsage").doc(uid).collection("days").doc(day);
}
async function consumeDailyQuota(uid, feature) {
    const ref = usageRef(uid);
    const limit = exports.AI_DAILY_LIMITS[feature];
    await admin.firestore().runTransaction(async (tx) => {
        var _a;
        const snap = await tx.get(ref);
        const used = Number((_a = snap.get(feature)) !== null && _a !== void 0 ? _a : 0);
        if (used >= limit) {
            throw new https_1.HttpsError("resource-exhausted", "daily_limit");
        }
        const expiresAt = new Date();
        expiresAt.setUTCDate(expiresAt.getUTCDate() + USAGE_RETENTION_DAYS);
        tx.set(ref, {
            [feature]: used + 1,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
        }, { merge: true });
    });
    return ref;
}
/** Counts one use (UTC day) before running; refunds it if the call fails so Gemini errors don't burn quota. */
async function withDailyQuota(uid, feature, run) {
    const ref = await consumeDailyQuota(uid, feature);
    try {
        return await run();
    }
    catch (error) {
        await ref
            .set({ [feature]: admin.firestore.FieldValue.increment(-1) }, { merge: true })
            .catch(() => undefined);
        throw error;
    }
}
//# sourceMappingURL=rateLimit.js.map