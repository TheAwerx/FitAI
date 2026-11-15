import * as admin from "firebase-admin";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";

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
export const PLAN_QUOTA = {
  limit: 3,
  resetPeriodDays: null as number | null,
  initialFreeLimit: 3,
  inFlightSeconds: 150,
} as const;

/** Error messages the app maps to `PlanQuotaErrors`. */
export const PLAN_QUOTA_ERRORS = {
  exhausted: "plan_quota_exhausted",
  inProgress: "plan_generation_in_progress",
  initialLimit: "plan_initial_limit",
} as const;

type PlanPurpose = "initial" | "regenerate";

const DAY_MS = 24 * 60 * 60 * 1000;

interface PlanQuotaDoc {
  used?: number;
  initialUsed?: number;
  periodStartedAtMs?: number | null;
  inFlightUntilMs?: number | null;
}

function quotaRef(uid: string) {
  return admin.firestore().collection("plan_quotas").doc(uid);
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value != null && typeof value === "object" && !Array.isArray(value) ?
    value as Record<string, unknown> :
    null;
}

/**
 * New clients send `{ profile, planPurpose }`. Older builds send the bare profile; those calls
 * count as regenerations.
 */
function unwrapPlanRequest(data: unknown): { purpose: PlanPurpose; profile: unknown } {
  const record = asRecord(data);
  const profile = record ? asRecord(record.profile) : null;
  if (record && profile) {
    return { purpose: record.planPurpose === "initial" ? "initial" : "regenerate", profile };
  }
  return { purpose: "regenerate", profile: data };
}

/** `used` for the current period; a finished period reads as zero. */
function currentUsed(doc: PlanQuotaDoc, nowMs: number): { used: number; periodStartedAtMs: number | null } {
  const used = Math.max(0, Number(doc.used ?? 0));
  const start = doc.periodStartedAtMs ?? null;
  if (PLAN_QUOTA.resetPeriodDays == null || start == null) return { used, periodStartedAtMs: start };
  if (nowMs >= start + PLAN_QUOTA.resetPeriodDays * DAY_MS) return { used: 0, periodStartedAtMs: null };
  return { used, periodStartedAtMs: start };
}

/** Checks the quota and takes the per-account lock. Nothing is spent here. */
async function reserve(uid: string, purpose: PlanPurpose): Promise<void> {
  const ref = quotaRef(uid);
  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const doc = (snap.data() ?? {}) as PlanQuotaDoc;
    const now = Date.now();
    if ((doc.inFlightUntilMs ?? 0) > now) {
      throw new HttpsError("aborted", PLAN_QUOTA_ERRORS.inProgress);
    }
    if (purpose === "initial") {
      if (Number(doc.initialUsed ?? 0) >= PLAN_QUOTA.initialFreeLimit) {
        throw new HttpsError("resource-exhausted", PLAN_QUOTA_ERRORS.initialLimit);
      }
    } else {
      const { used } = currentUsed(doc, now);
      if (used >= PLAN_QUOTA.limit) {
        throw new HttpsError("resource-exhausted", PLAN_QUOTA_ERRORS.exhausted, {
          limit: PLAN_QUOTA.limit,
          remaining: 0,
        });
      }
    }
    tx.set(ref, { inFlightUntilMs: now + PLAN_QUOTA.inFlightSeconds * 1000 }, { merge: true });
  });
}

/** Spends the right (or the free initial generation) after the plan was built. */
async function commit(uid: string, purpose: PlanPurpose): Promise<void> {
  const ref = quotaRef(uid);
  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const doc = (snap.data() ?? {}) as PlanQuotaDoc;
    const now = Date.now();
    const { used, periodStartedAtMs } = currentUsed(doc, now);
    const update: Record<string, unknown> = {
      limit: PLAN_QUOTA.limit,
      resetPeriodDays: PLAN_QUOTA.resetPeriodDays,
      inFlightUntilMs: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    if (purpose === "initial") {
      update.initialUsed = Number(doc.initialUsed ?? 0) + 1;
      update.used = used;
      update.periodStartedAtMs = periodStartedAtMs;
    } else {
      update.used = used + 1;
      update.periodStartedAtMs = periodStartedAtMs ?? now;
    }
    tx.set(ref, update, { merge: true });
  });
}

async function release(uid: string): Promise<void> {
  await quotaRef(uid).set({ inFlightUntilMs: null }, { merge: true }).catch(() => undefined);
}

/**
 * Wraps the `generateWorkoutPlan` handler. The handler receives the bare profile as `request.data`.
 * A right is spent only after the handler returns; failures release the lock and spend nothing.
 * `plan_quotas/{uid}` is written here only (Admin SDK); clients may read their own document.
 */
export function withPlanQuota<T>(
  handler: (request: CallableRequest<unknown>) => Promise<T>,
): (request: CallableRequest<unknown>) => Promise<T> {
  return async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Authentication required");
    }
    const { purpose, profile } = unwrapPlanRequest(request.data);
    await reserve(uid, purpose);
    let result: T;
    try {
      result = await handler({ ...request, data: profile });
    } catch (error) {
      await release(uid);
      throw error;
    }
    try {
      await commit(uid, purpose);
    } catch (error) {
      // The plan is already built; do not fail the call because the counter write did.
      console.error("plan quota commit failed", uid, error);
      await release(uid);
    }
    return result;
  };
}
