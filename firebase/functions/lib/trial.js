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
exports.getTrialStatus = exports.startTrial = exports.DEVICE_TRIALS_COLLECTION = exports.TRIALS_COLLECTION = exports.TRIAL_ERRORS = exports.TRIAL = void 0;
exports.hashDeviceKey = hashDeviceKey;
exports.resolvePepper = resolvePepper;
exports.callerOf = callerOf;
exports.parseDevice = parseDevice;
exports.trialStatusOf = trialStatusOf;
exports.noTrialStatus = noTrialStatus;
exports.startTrialFor = startTrialFor;
exports.trialStatusFor = trialStatusFor;
const admin = __importStar(require("firebase-admin"));
const node_crypto_1 = require("node:crypto");
const https_1 = require("firebase-functions/v2/https");
const params_1 = require("firebase-functions/params");
/**
 * Free trial: 7 days, once per account AND once per device (docs/trial.md, firebase/SCHEMA.md).
 * Mirror of `TrialPolicy` in core/domain/.../trial/TrialPolicy.kt.
 *
 * - `trials/{uid}`: the account's trial. Written once, never restarted.
 * - `device_trials/{hmac(deviceKey)}`: the device the trial was started on. A second account on the
 *   same device is refused. The raw device key never reaches Firestore; it is HMAC-SHA256'd with
 *   the `TRIAL_DEVICE_PEPPER` secret.
 *
 * Both documents are Admin SDK only. Times are server time; the client sends no dates.
 */
exports.TRIAL = {
    days: 7,
    keyVersion: 1,
};
/** Error messages the app maps to `TrialErrors`. */
exports.TRIAL_ERRORS = {
    accountRequired: "trial_requires_account",
    invalidDevice: "trial_invalid_device",
    notConfigured: "trial_not_configured",
};
const DAY_MS = 24 * 60 * 60 * 1000;
const PLATFORMS = ["android", "ios"];
/** The app sends a SHA-256 hex of the platform id; accept any opaque key of sane size. */
const DEVICE_KEY_PATTERN = /^[A-Za-z0-9_-]{16,128}$/;
const MIN_PEPPER_LENGTH = 32;
/** Only used when the functions emulator runs without `.secret.local`. */
const EMULATOR_PEPPER = "fitai-emulator-only-device-pepper-not-secret";
exports.TRIALS_COLLECTION = "trials";
exports.DEVICE_TRIALS_COLLECTION = "device_trials";
const trialDevicePepper = (0, params_1.defineSecret)("TRIAL_DEVICE_PEPPER");
const trialCallable = {
    region: "us-central1",
    cors: true,
    invoker: "public",
    secrets: [trialDevicePepper],
};
function hashDeviceKey(pepper, deviceKey) {
    return (0, node_crypto_1.createHmac)("sha256", pepper)
        .update(`fitai-trial-device:v${exports.TRIAL.keyVersion}:${deviceKey}`)
        .digest("hex");
}
/** The secret, or the fixed emulator value. Null in production when the secret is missing or short. */
function resolvePepper(raw, emulator) {
    const value = (raw !== null && raw !== void 0 ? raw : "").trim();
    if (value.length >= MIN_PEPPER_LENGTH)
        return value;
    if (emulator)
        return value.length > 0 ? value : EMULATOR_PEPPER;
    return null;
}
function callerOf(request) {
    var _a;
    const auth = request.auth;
    if (!(auth === null || auth === void 0 ? void 0 : auth.uid)) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required");
    }
    const token = auth.token;
    return { uid: auth.uid, anonymous: ((_a = token === null || token === void 0 ? void 0 : token.firebase) === null || _a === void 0 ? void 0 : _a.sign_in_provider) === "anonymous" };
}
/** `{ deviceKey, platform }` from the request; null when optional and absent. */
function parseDevice(value, required) {
    const record = value != null && typeof value === "object" && !Array.isArray(value) ?
        value :
        {};
    if (record.deviceKey == null && !required)
        return null;
    const deviceKey = record.deviceKey;
    const platform = record.platform;
    if (typeof deviceKey !== "string" || !DEVICE_KEY_PATTERN.test(deviceKey)) {
        throw new https_1.HttpsError("invalid-argument", exports.TRIAL_ERRORS.invalidDevice);
    }
    if (typeof platform !== "string" || !PLATFORMS.includes(platform)) {
        throw new https_1.HttpsError("invalid-argument", exports.TRIAL_ERRORS.invalidDevice);
    }
    return { deviceKey, platform: platform };
}
function iso(ms) {
    return new Date(ms).toISOString();
}
function millisOf(value) {
    if (value instanceof admin.firestore.Timestamp)
        return value.toMillis();
    if (typeof value === "number" && Number.isFinite(value))
        return value;
    return null;
}
function periodOf(snap) {
    const startedAtMs = millisOf(snap.get("startedAt"));
    const endsAtMs = millisOf(snap.get("endsAt"));
    return startedAtMs == null || endsAtMs == null ? null : { startedAtMs, endsAtMs };
}
function trialStatusOf(period, nowMs, started = false) {
    const msLeft = period.endsAtMs - nowMs;
    const active = msLeft > 0;
    return {
        state: active ? "active" : "expired",
        started,
        trialDays: exports.TRIAL.days,
        startedAt: iso(period.startedAtMs),
        endsAt: iso(period.endsAtMs),
        serverTime: iso(nowMs),
        daysRemaining: active ? Math.ceil(msLeft / DAY_MS) : 0,
        trialStartedAt: iso(period.startedAtMs),
        trialExpiresAt: iso(period.endsAtMs),
        isActive: active,
        hasSubscription: false,
    };
}
function noTrialStatus(state, nowMs) {
    return {
        state,
        started: false,
        trialDays: exports.TRIAL.days,
        startedAt: null,
        endsAt: null,
        serverTime: iso(nowMs),
        daysRemaining: 0,
        trialStartedAt: "",
        trialExpiresAt: "",
        isActive: false,
        hasSubscription: false,
    };
}
function accountDoc(period, deviceHash, platform) {
    return {
        schemaVersion: 1,
        startedAt: admin.firestore.Timestamp.fromMillis(period.startedAtMs),
        endsAt: admin.firestore.Timestamp.fromMillis(period.endsAtMs),
        trialDays: exports.TRIAL.days,
        deviceHash,
        platform,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };
}
function deviceDoc(uid, period, platform) {
    return {
        schemaVersion: 1,
        keyVersion: exports.TRIAL.keyVersion,
        firstUid: uid,
        startedAt: admin.firestore.Timestamp.fromMillis(period.startedAtMs),
        endsAt: admin.firestore.Timestamp.fromMillis(period.endsAtMs),
        platform,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };
}
/**
 * Starts the trial when neither the account nor the device has had one. Idempotent: an account
 * that already has a trial gets it back unchanged (never restarted), from any device. One
 * transaction reads both documents, so two accounts racing on one device can't both win.
 */
async function startTrialFor(db, caller, device, pepper, nowMs = Date.now()) {
    if (caller.anonymous) {
        throw new https_1.HttpsError("permission-denied", exports.TRIAL_ERRORS.accountRequired);
    }
    const deviceHash = hashDeviceKey(pepper, device.deviceKey);
    const accountRef = db.collection(exports.TRIALS_COLLECTION).doc(caller.uid);
    const deviceRef = db.collection(exports.DEVICE_TRIALS_COLLECTION).doc(deviceHash);
    return db.runTransaction(async (tx) => {
        var _a;
        const [accountSnap, deviceSnap] = await tx.getAll(accountRef, deviceRef);
        const accountPeriod = accountSnap.exists ? periodOf(accountSnap) : null;
        if (accountPeriod)
            return trialStatusOf(accountPeriod, nowMs);
        if (deviceSnap.exists) {
            const devicePeriod = periodOf(deviceSnap);
            if (devicePeriod && deviceSnap.get("firstUid") === caller.uid) {
                // The account document is gone but this device holds the account's claim: restore, don't restart.
                tx.set(accountRef, accountDoc(devicePeriod, deviceHash, String((_a = deviceSnap.get("platform")) !== null && _a !== void 0 ? _a : device.platform)));
                return trialStatusOf(devicePeriod, nowMs);
            }
            return noTrialStatus("used", nowMs);
        }
        const period = { startedAtMs: nowMs, endsAtMs: nowMs + exports.TRIAL.days * DAY_MS };
        tx.set(accountRef, accountDoc(period, deviceHash, device.platform));
        tx.set(deviceRef, deviceDoc(caller.uid, period, device.platform));
        return trialStatusOf(period, nowMs, true);
    });
}
/**
 * Account trial when there is one (signed-in callers), otherwise whether this device can still
 * start one. Anonymous sessions (the paywall before sign-up) only learn the device answer.
 * Without a pepper the device is not checked; [startTrialFor] still refuses to run.
 */
async function trialStatusFor(db, caller, device, pepper, nowMs = Date.now()) {
    if (!caller.anonymous) {
        const accountSnap = await db.collection(exports.TRIALS_COLLECTION).doc(caller.uid).get();
        const period = accountSnap.exists ? periodOf(accountSnap) : null;
        if (period)
            return trialStatusOf(period, nowMs);
    }
    if (device && pepper) {
        const deviceSnap = await db.collection(exports.DEVICE_TRIALS_COLLECTION).doc(hashDeviceKey(pepper, device.deviceKey)).get();
        if (deviceSnap.exists) {
            const period = periodOf(deviceSnap);
            if (period && !caller.anonymous && deviceSnap.get("firstUid") === caller.uid) {
                return trialStatusOf(period, nowMs);
            }
            return noTrialStatus("used", nowMs);
        }
    }
    return noTrialStatus("eligible", nowMs);
}
function isEmulator() {
    return process.env.FUNCTIONS_EMULATOR === "true";
}
/** `{ deviceKey, platform }` → trial status. Requires a real (non-anonymous) account. */
exports.startTrial = (0, https_1.onCall)(trialCallable, async (request) => {
    const caller = callerOf(request);
    if (caller.anonymous) {
        throw new https_1.HttpsError("permission-denied", exports.TRIAL_ERRORS.accountRequired);
    }
    const device = parseDevice(request.data, true);
    const pepper = resolvePepper(trialDevicePepper.value(), isEmulator());
    if (!pepper) {
        console.error("TRIAL_DEVICE_PEPPER is not set; refusing to start trials");
        throw new https_1.HttpsError("failed-precondition", exports.TRIAL_ERRORS.notConfigured);
    }
    return startTrialFor(admin.firestore(), caller, device, pepper);
});
/** Optional `{ deviceKey, platform }` → trial status. Anonymous sessions get the device answer only. */
exports.getTrialStatus = (0, https_1.onCall)(trialCallable, async (request) => {
    const caller = callerOf(request);
    const device = parseDevice(request.data, false);
    const pepper = resolvePepper(trialDevicePepper.value(), isEmulator());
    if (device && !pepper)
        console.error("TRIAL_DEVICE_PEPPER is not set; device trials are not checked");
    return trialStatusFor(admin.firestore(), caller, device, pepper);
});
//# sourceMappingURL=trial.js.map