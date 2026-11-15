import * as admin from "firebase-admin";
import type { messaging } from "firebase-admin";
import { buildMessages } from "./copy";
import { NOTIFICATION_CONFIG as C } from "./config";
import { applySent, evaluate } from "./rules";
import { STATE_COLLECTION, stateFromDoc, stateRef, stateToDoc } from "./state";
import { localPartsOfIso, safeTimeZone } from "./time";
import { DayFacts, Evaluation, NudgeKind } from "./types";

type Firestore = admin.firestore.Firestore;
const { Timestamp, FieldValue } = admin.firestore;

/** The part of `admin.messaging()` the sweep uses; tests pass a stub. */
export interface Messenger {
  sendEach(messages: messaging.TokenMessage[]): Promise<messaging.BatchResponse>;
}

export interface SweepOptions {
  db: Firestore;
  messenger: Messenger;
  nowMs?: number;
  pageSize?: number;
  concurrency?: number;
  /** Stop paging after this long; the rest is picked up by the next run. */
  budgetMs?: number;
  log?: (message: string, data?: Record<string, unknown>) => void;
}

export interface SentRecord {
  uid: string;
  kind: NudgeKind;
  variant: string;
  localDate: string;
  tokens: number;
  delivered: number;
  title: string;
  body: string;
  link: string;
}

export interface SweepResult {
  checked: number;
  sent: SentRecord[];
  reasons: Record<string, number>;
  invalidTokensRemoved: number;
}

/** Token errors that mean "this installation is gone": the device document is deleted. */
const DEAD_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

/**
 * Exact facts for [date] from today's documents. Two small queries on the automatic `localDate`
 * index; only run when the cheap state view already found something to send.
 */
export async function loadDayFacts(
  db: Firestore,
  uid: string,
  date: string,
  timeZone: string,
  nowMs: number,
): Promise<DayFacts> {
  const user = db.collection("users").doc(uid);
  const [sessions, meals] = await Promise.all([
    user.collection("workout_sessions").where("localDate", "==", date).get(),
    user.collection("meal_logs").where("localDate", "==", date).get(),
  ]);
  let workoutCompletedToday = false;
  let sessionInProgress = false;
  for (const s of sessions.docs) {
    if (s.get("completed") === true) {
      workoutCompletedToday = true;
      continue;
    }
    const started = Date.parse(String(s.get("startedAt") ?? ""));
    if (Number.isFinite(started) && started <= nowMs && nowMs - started < C.sessionInProgressMinutes * 60_000) {
      sessionInProgress = true;
    }
  }
  let latestMealMinute: number | null = null;
  for (const m of meals.docs) {
    const parts = localPartsOfIso(m.get("loggedAt"), timeZone);
    const minute = parts?.date === date ? parts.minute : 0;
    latestMealMinute = latestMealMinute == null ? minute : Math.max(latestMealMinute, minute);
  }
  return { workoutCompletedToday, sessionInProgress, mealLoggedToday: !meals.empty, latestMealMinute };
}

async function mapLimit<T>(items: T[], limit: number, run: (item: T) => Promise<void>): Promise<void> {
  let index = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (index < items.length) {
      const item = items[index++];
      await run(item);
    }
  });
  await Promise.all(workers);
}

/**
 * One pass over every account whose `nextCheckAt` is due. Per account:
 * 1. evaluate on the stored state (no extra reads);
 * 2. if something would be sent, confirm with today's sessions and meals;
 * 3. in a transaction, re-evaluate on the fresh state, record the nudge in the sent log and store the
 *    next check (at most once per kind per local day, even if two runs overlap);
 * 4. send to every device token and delete devices whose token is dead.
 */
export async function runNotificationSweep(options: SweepOptions): Promise<SweepResult> {
  const { db, messenger } = options;
  const nowMs = options.nowMs ?? Date.now();
  const pageSize = options.pageSize ?? 200;
  const concurrency = options.concurrency ?? 16;
  const deadline = Date.now() + (options.budgetMs ?? 7 * 60_000);
  const log = options.log ?? (() => undefined);
  const result: SweepResult = { checked: 0, sent: [], reasons: {}, invalidTokensRemoved: 0 };

  let cursor: admin.firestore.QueryDocumentSnapshot | null = null;
  for (;;) {
    let query = db.collection(STATE_COLLECTION)
      .where("nextCheckAt", "<=", Timestamp.fromMillis(nowMs))
      .orderBy("nextCheckAt")
      .limit(pageSize);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;
    await mapLimit(page.docs, concurrency, async (doc) => {
      result.checked++;
      try {
        await processAccount(db, messenger, doc.id, doc.data(), nowMs, result);
      } catch (error) {
        result.reasons.error = (result.reasons.error ?? 0) + 1;
        log("notification check failed", { uid: doc.id, error: String(error) });
      }
    });
    cursor = page.docs[page.docs.length - 1];
    if (page.size < pageSize || Date.now() > deadline) break;
  }
  return result;
}

async function processAccount(
  db: Firestore,
  messenger: Messenger,
  uid: string,
  data: admin.firestore.DocumentData,
  nowMs: number,
  result: SweepResult,
): Promise<void> {
  const first = evaluate(stateFromDoc(data), nowMs);
  let facts: DayFacts | undefined;
  if (first.send) {
    facts = await loadDayFacts(db, uid, first.localDate, safeTimeZone(data.timeZone), nowMs);
  }

  const ref = stateRef(db, uid);
  const decided = await db.runTransaction<{ evaluation: Evaluation; language: "tr" | "en" } | null>(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return null;
    const fresh = stateFromDoc(snap.data());
    const evaluation = evaluate(fresh, nowMs, facts);
    const verified = evaluation.send != null && facts != null &&
      evaluation.localDate === first.localDate && evaluation.send.kind === first.send?.kind;
    if (evaluation.send && !verified) {
      // Something changed since the query; look again on the next run instead of sending unverified.
      tx.update(ref, { nextCheckAt: Timestamp.fromMillis(nowMs), updatedAt: FieldValue.serverTimestamp() });
      count(result, "changed");
      return null;
    }
    if (!evaluation.send) {
      tx.update(ref, {
        nextCheckAt: evaluation.nextCheckAtMs == null ? null : Timestamp.fromMillis(evaluation.nextCheckAtMs),
        updatedAt: FieldValue.serverTimestamp(),
      });
      count(result, evaluation.reason);
      return null;
    }
    const after = applySent(fresh, evaluation.send, nowMs, evaluation.localDate, evaluation.comebackAnchor);
    tx.set(ref, stateToDoc(after, evaluation.nextCheckAtMs));
    return { evaluation, language: fresh.language };
  });
  if (!decided?.evaluation.send) return;

  const { evaluation, language } = decided;
  const nudge = evaluation.send!;
  const devices = await db.collection("users").doc(uid).collection("devices").get();
  const byToken = new Map<string, string[]>();
  for (const d of devices.docs) {
    const token = d.get("fcmToken");
    if (typeof token === "string" && token.length > 0) byToken.set(token, [...(byToken.get(token) ?? []), d.id]);
  }
  const tokens = [...byToken.keys()];
  const messages = buildMessages(nudge, language, tokens, evaluation.localDate, nowMs);
  let delivered = 0;
  if (messages.length > 0) {
    const response = await messenger.sendEach(messages);
    const dead: string[] = [];
    response.responses.forEach((r, i) => {
      if (r.success) delivered++;
      else if (r.error && DEAD_TOKEN_CODES.has(r.error.code)) dead.push(...(byToken.get(tokens[i]) ?? []));
    });
    await Promise.all(dead.map((id) => db.collection("users").doc(uid).collection("devices").doc(id).delete()));
    result.invalidTokensRemoved += dead.length;
  }
  count(result, "send");
  result.sent.push({
    uid,
    kind: nudge.kind,
    variant: nudge.variant,
    localDate: evaluation.localDate,
    tokens: tokens.length,
    delivered,
    title: String(messages[0]?.notification?.title ?? ""),
    body: String(messages[0]?.notification?.body ?? ""),
    link: nudge.link,
  });
}

function count(result: SweepResult, reason: string) {
  result.reasons[reason] = (result.reasons[reason] ?? 0) + 1;
}
