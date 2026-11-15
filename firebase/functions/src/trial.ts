import * as admin from "firebase-admin";
import { createHmac } from "node:crypto";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";

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
export const TRIAL = {
  days: 7,
  keyVersion: 1,
} as const;

/** Error messages the app maps to `TrialErrors`. */
export const TRIAL_ERRORS = {
  accountRequired: "trial_requires_account",
  invalidDevice: "trial_invalid_device",
  notConfigured: "trial_not_configured",
} as const;

export type TrialState = "eligible" | "active" | "expired" | "used";
export type DevicePlatform = "android" | "ios";

export interface TrialCaller {
  uid: string;
  anonymous: boolean;
}

export interface DeviceInput {
  deviceKey: string;
  platform: DevicePlatform;
}

export interface TrialStatusResponse {
  state: TrialState;
  /** True only on the startTrial call that created the trial. */
  started: boolean;
  trialDays: number;
  startedAt: string | null;
  endsAt: string | null;
  serverTime: string;
  daysRemaining: number;
  // Read by app builds from before the device trial (`TrialStatusDto`); keep until they are gone.
  trialStartedAt: string;
  trialExpiresAt: string;
  isActive: boolean;
  hasSubscription: boolean;
}

interface TrialPeriod {
  startedAtMs: number;
  endsAtMs: number;
}

const DAY_MS = 24 * 60 * 60 * 1000;
const PLATFORMS: readonly string[] = ["android", "ios"];
/** The app sends a SHA-256 hex of the platform id; accept any opaque key of sane size. */
const DEVICE_KEY_PATTERN = /^[A-Za-z0-9_-]{16,128}$/;
const MIN_PEPPER_LENGTH = 32;
/** Only used when the functions emulator runs without `.secret.local`. */
const EMULATOR_PEPPER = "fitai-emulator-only-device-pepper-not-secret";

export const TRIALS_COLLECTION = "trials";
export const DEVICE_TRIALS_COLLECTION = "device_trials";

const trialDevicePepper = defineSecret("TRIAL_DEVICE_PEPPER");

const trialCallable = {
  region: "us-central1",
  cors: true,
  invoker: "public" as const,
  secrets: [trialDevicePepper],
};

export function hashDeviceKey(pepper: string, deviceKey: string): string {
  return createHmac("sha256", pepper)
    .update(`fitai-trial-device:v${TRIAL.keyVersion}:${deviceKey}`)
    .digest("hex");
}

/** The secret, or the fixed emulator value. Null in production when the secret is missing or short. */
export function resolvePepper(raw: string | undefined, emulator: boolean): string | null {
  const value = (raw ?? "").trim();
  if (value.length >= MIN_PEPPER_LENGTH) return value;
  if (emulator) return value.length > 0 ? value : EMULATOR_PEPPER;
  return null;
}

export function callerOf(request: CallableRequest<unknown>): TrialCaller {
  const auth = request.auth;
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }
  const token = auth.token as { firebase?: { sign_in_provider?: string } } | undefined;
  return { uid: auth.uid, anonymous: token?.firebase?.sign_in_provider === "anonymous" };
}

/** `{ deviceKey, platform }` from the request; null when optional and absent. */
export function parseDevice(value: unknown, required: boolean): DeviceInput | null {
  const record = value != null && typeof value === "object" && !Array.isArray(value) ?
    value as Record<string, unknown> :
    {};
  if (record.deviceKey == null && !required) return null;
  const deviceKey = record.deviceKey;
  const platform = record.platform;
  if (typeof deviceKey !== "string" || !DEVICE_KEY_PATTERN.test(deviceKey)) {
    throw new HttpsError("invalid-argument", TRIAL_ERRORS.invalidDevice);
  }
  if (typeof platform !== "string" || !PLATFORMS.includes(platform)) {
    throw new HttpsError("invalid-argument", TRIAL_ERRORS.invalidDevice);
  }
  return { deviceKey, platform: platform as DevicePlatform };
}

function iso(ms: number): string {
  return new Date(ms).toISOString();
}

function millisOf(value: unknown): number | null {
  if (value instanceof admin.firestore.Timestamp) return value.toMillis();
  if (typeof value === "number" && Number.isFinite(value)) return value;
  return null;
}

function periodOf(snap: admin.firestore.DocumentSnapshot): TrialPeriod | null {
  const startedAtMs = millisOf(snap.get("startedAt"));
  const endsAtMs = millisOf(snap.get("endsAt"));
  return startedAtMs == null || endsAtMs == null ? null : { startedAtMs, endsAtMs };
}

export function trialStatusOf(period: TrialPeriod, nowMs: number, started = false): TrialStatusResponse {
  const msLeft = period.endsAtMs - nowMs;
  const active = msLeft > 0;
  return {
    state: active ? "active" : "expired",
    started,
    trialDays: TRIAL.days,
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

export function noTrialStatus(state: "eligible" | "used", nowMs: number): TrialStatusResponse {
  return {
    state,
    started: false,
    trialDays: TRIAL.days,
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

function accountDoc(period: TrialPeriod, deviceHash: string, platform: string) {
  return {
    schemaVersion: 1,
    startedAt: admin.firestore.Timestamp.fromMillis(period.startedAtMs),
    endsAt: admin.firestore.Timestamp.fromMillis(period.endsAtMs),
    trialDays: TRIAL.days,
    deviceHash,
    platform,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function deviceDoc(uid: string, period: TrialPeriod, platform: string) {
  return {
    schemaVersion: 1,
    keyVersion: TRIAL.keyVersion,
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
export async function startTrialFor(
  db: admin.firestore.Firestore,
  caller: TrialCaller,
  device: DeviceInput,
  pepper: string,
  nowMs: number = Date.now(),
): Promise<TrialStatusResponse> {
  if (caller.anonymous) {
    throw new HttpsError("permission-denied", TRIAL_ERRORS.accountRequired);
  }
  const deviceHash = hashDeviceKey(pepper, device.deviceKey);
  const accountRef = db.collection(TRIALS_COLLECTION).doc(caller.uid);
  const deviceRef = db.collection(DEVICE_TRIALS_COLLECTION).doc(deviceHash);
  return db.runTransaction(async (tx) => {
    const [accountSnap, deviceSnap] = await tx.getAll(accountRef, deviceRef);
    const accountPeriod = accountSnap.exists ? periodOf(accountSnap) : null;
    if (accountPeriod) return trialStatusOf(accountPeriod, nowMs);

    if (deviceSnap.exists) {
      const devicePeriod = periodOf(deviceSnap);
      if (devicePeriod && deviceSnap.get("firstUid") === caller.uid) {
        // The account document is gone but this device holds the account's claim: restore, don't restart.
        tx.set(accountRef, accountDoc(devicePeriod, deviceHash, String(deviceSnap.get("platform") ?? device.platform)));
        return trialStatusOf(devicePeriod, nowMs);
      }
      return noTrialStatus("used", nowMs);
    }

    const period = { startedAtMs: nowMs, endsAtMs: nowMs + TRIAL.days * DAY_MS };
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
export async function trialStatusFor(
  db: admin.firestore.Firestore,
  caller: TrialCaller,
  device: DeviceInput | null,
  pepper: string | null,
  nowMs: number = Date.now(),
): Promise<TrialStatusResponse> {
  if (!caller.anonymous) {
    const accountSnap = await db.collection(TRIALS_COLLECTION).doc(caller.uid).get();
    const period = accountSnap.exists ? periodOf(accountSnap) : null;
    if (period) return trialStatusOf(period, nowMs);
  }
  if (device && pepper) {
    const deviceSnap = await db.collection(DEVICE_TRIALS_COLLECTION).doc(hashDeviceKey(pepper, device.deviceKey)).get();
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

function isEmulator(): boolean {
  return process.env.FUNCTIONS_EMULATOR === "true";
}

/** `{ deviceKey, platform }` → trial status. Requires a real (non-anonymous) account. */
export const startTrial = onCall(trialCallable, async (request) => {
  const caller = callerOf(request);
  if (caller.anonymous) {
    throw new HttpsError("permission-denied", TRIAL_ERRORS.accountRequired);
  }
  const device = parseDevice(request.data, true)!;
  const pepper = resolvePepper(trialDevicePepper.value(), isEmulator());
  if (!pepper) {
    console.error("TRIAL_DEVICE_PEPPER is not set; refusing to start trials");
    throw new HttpsError("failed-precondition", TRIAL_ERRORS.notConfigured);
  }
  return startTrialFor(admin.firestore(), caller, device, pepper);
});

/** Optional `{ deviceKey, platform }` → trial status. Anonymous sessions get the device answer only. */
export const getTrialStatus = onCall(trialCallable, async (request) => {
  const caller = callerOf(request);
  const device = parseDevice(request.data, false);
  const pepper = resolvePepper(trialDevicePepper.value(), isEmulator());
  if (device && !pepper) console.error("TRIAL_DEVICE_PEPPER is not set; device trials are not checked");
  return trialStatusFor(admin.firestore(), caller, device, pepper);
});
