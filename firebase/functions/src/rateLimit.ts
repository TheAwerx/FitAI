import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";

export const AI_DAILY_LIMITS = {
  suggestNextMeal: 30,
  adaptTodaySession: 10,
  coachChat: 60,
  weeklyCoachReport: 5,
} as const;

export type AiFeature = keyof typeof AI_DAILY_LIMITS;

const USAGE_RETENTION_DAYS = 7;

function usageRef(uid: string) {
  const day = new Date().toISOString().slice(0, 10);
  return admin.firestore().collection("aiUsage").doc(uid).collection("days").doc(day);
}

async function consumeDailyQuota(uid: string, feature: AiFeature) {
  const ref = usageRef(uid);
  const limit = AI_DAILY_LIMITS[feature];
  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const used = Number(snap.get(feature) ?? 0);
    if (used >= limit) {
      throw new HttpsError("resource-exhausted", "daily_limit");
    }
    const expiresAt = new Date();
    expiresAt.setUTCDate(expiresAt.getUTCDate() + USAGE_RETENTION_DAYS);
    tx.set(
      ref,
      {
        [feature]: used + 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
      },
      { merge: true },
    );
  });
  return ref;
}

/** Counts one use (UTC day) before running; refunds it if the call fails so Gemini errors don't burn quota. */
export async function withDailyQuota<T>(
  uid: string,
  feature: AiFeature,
  run: () => Promise<T>,
): Promise<T> {
  const ref = await consumeDailyQuota(uid, feature);
  try {
    return await run();
  } catch (error) {
    await ref
      .set({ [feature]: admin.firestore.FieldValue.increment(-1) }, { merge: true })
      .catch(() => undefined);
    throw error;
  }
}
