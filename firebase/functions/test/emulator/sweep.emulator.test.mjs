// End-to-end run against the Firestore emulator (no live project, nothing is really sent):
// seed accounts the way the app writes them, run the trigger bodies, run the scheduled sweep once with a
// stubbed messaging client, and check what would have been sent. Run: npm run test:emulator
import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import admin from "firebase-admin";
import { runNotificationSweep } from "../../lib/notifications/sweep.js";
import {
  onDeviceWritten,
  onMealLogCreated,
  onUserWritten,
  onWorkoutPlanWritten,
  onWorkoutSessionWritten,
} from "../../lib/notifications/triggers.js";
import { zonedTimeToUtc } from "../../lib/notifications/time.js";

const PROJECT_ID = "demo-fitai";
const MON = "2026-11-16";
// Monday 19:35 in Istanbul (UTC+3) = 11:35 in New York (UTC-5).
const NOW = zonedTimeToUtc(MON, 19 * 60 + 35, "Europe/Istanbul");
const EARLIER = NOW - 6 * 60 * 60_000;

let db;

/** Records every message; tokens starting with "dead" fail like an uninstalled app. */
function stubMessenger() {
  const sent = [];
  return {
    sent,
    async sendEach(messages) {
      sent.push(...messages);
      const responses = messages.map((m) => m.token.startsWith("dead") ?
        { success: false, error: { code: "messaging/registration-token-not-registered", message: "gone" } } :
        { success: true, messageId: `projects/${PROJECT_ID}/messages/${m.token}` });
      return {
        responses,
        successCount: responses.filter((r) => r.success).length,
        failureCount: responses.filter((r) => !r.success).length,
      };
    },
  };
}

function userDoc(overrides = {}) {
  return {
    schemaVersion: 1,
    localUserId: "0123456789abcdef",
    name: "Ada",
    email: "ada@example.com",
    notificationsEnabled: true,
    notificationTypes: { workoutLate: true, mealLogging: true, streakRisk: true, comeback: true },
    reminderHour: 18,
    reminderMinute: 0,
    language: "tr",
    timeZone: "Europe/Istanbul",
    daysPerWeek: 3,
    createdAt: "2026-10-01T09:00:00Z",
    updatedAt: "2026-11-16T08:00:00Z",
    ...overrides,
  };
}

function planDoc(id) {
  return {
    schemaVersion: 1,
    id,
    title: "Plan",
    description: "",
    daysPerWeek: 3,
    kind: "program",
    isActive: true,
    createdAt: "2026-11-01T08:00:00Z",
    updatedAt: "2026-11-01T08:00:00Z",
    exercises: [
      { id: "e1", dayOfWeek: 1, name: "Squat", sets: 3, reps: "8", restSeconds: 90, muscleGroup: "Legs", notes: null },
      { id: "e2", dayOfWeek: 3, name: "Bench", sets: 3, reps: "8", restSeconds: 90, muscleGroup: "Chest", notes: null },
      { id: "e3", dayOfWeek: 5, name: "Row", sets: 3, reps: "8", restSeconds: 90, muscleGroup: "Back", notes: null },
    ],
  };
}

function deviceDoc(token, overrides = {}) {
  return {
    schemaVersion: 1,
    platform: "android",
    fcmToken: token,
    language: "tr",
    timeZone: "Europe/Istanbul",
    appVersion: "1.0.0",
    updatedAt: "2026-11-16T08:00:00Z",
    ...overrides,
  };
}

function sessionDoc(id, localDate, completed) {
  return {
    schemaVersion: 1,
    id,
    planId: "plan1",
    startedAt: `${localDate}T05:00:00Z`,
    completedAt: completed ? `${localDate}T05:50:00Z` : null,
    completed,
    durationMinutes: completed ? 50 : null,
    notes: null,
    localDate,
    totalSets: 9,
    completedSets: completed ? 9 : 2,
    volumeKg: 1000,
    exercises: [],
    updatedAt: `${localDate}T05:50:00Z`,
  };
}

function mealDoc(id, localDate, loggedAt) {
  return {
    schemaVersion: 1,
    id,
    mealName: "Menemen",
    calories: 400,
    proteinG: 20,
    carbsG: 20,
    fatG: 25,
    source: "manual",
    photoUrl: null,
    loggedAt,
    localDate,
    updatedAt: loggedAt,
  };
}

/** Writes a document and runs the trigger the deployed function would run for it. */
async function write(path, data, { trigger = true } = {}) {
  const ref = db.doc(path);
  const beforeData = (await ref.get()).data();
  await ref.set(data);
  if (!trigger) return;
  const [, uid, sub, id] = path.split("/");
  if (!sub) await onUserWritten(db, uid, beforeData, data, EARLIER);
  else if (sub === "devices") await onDeviceWritten(db, uid, EARLIER);
  else if (sub === "workout_plans") await onWorkoutPlanWritten(db, uid, id, beforeData, data, EARLIER);
  else if (sub === "workout_sessions") await onWorkoutSessionWritten(db, uid, beforeData, data, EARLIER);
  else if (sub === "meal_logs" && !beforeData) await onMealLogCreated(db, uid, data, EARLIER);
}

/** An account the way the app syncs it after sign-in: profile, active plan, device, recent activity. */
async function account(uid, { user = {}, tokens = [`tok-${uid}`], device = {} } = {}) {
  await write(`users/${uid}`, userDoc(user));
  await write(`users/${uid}/workout_plans/plan1`, planDoc("plan1"));
  for (const [i, token] of tokens.entries()) await write(`users/${uid}/devices/install-${i}`, deviceDoc(token, device));
  // Active Fri–Sun: engaged, not lapsed.
  await write(`users/${uid}/meal_logs/m-fri`, mealDoc("m-fri", "2026-11-13", "2026-11-13T09:00:00Z"));
  await write(`users/${uid}/workout_sessions/s-fri`, sessionDoc("s-fri", "2026-11-13", true));
  await write(`users/${uid}/meal_logs/m-sun`, mealDoc("m-sun", "2026-11-15", "2026-11-15T10:00:00Z"));
}

before(() => {
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST, "run through `npm run test:emulator`");
  admin.initializeApp({ projectId: PROJECT_ID });
  db = admin.firestore();
});

after(async () => {
  await admin.app().delete();
});

beforeEach(async () => {
  const host = process.env.FIRESTORE_EMULATOR_HOST;
  await fetch(`http://${host}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, { method: "DELETE" });
});

describe("notification sweep on the emulator", () => {
  it("triggers build the state the sweep needs", async () => {
    await account("alice");
    const state = (await db.doc("notification_state/alice").get()).data();
    assert.equal(state.enabled, true);
    assert.equal(state.deviceCount, 1);
    assert.deepEqual(state.planTrainingDays, [1, 3, 5]);
    assert.deepEqual(state.activeDates, ["2026-11-13", "2026-11-15"]);
    assert.equal(state.lastWorkoutDate, "2026-11-13");
    assert.equal(state.createdDate, "2026-10-01");
    // Built at 13:35 local on Monday with nothing logged today: next is the afternoon meal window.
    assert.equal(state.nextCheckAt.toMillis(), zonedTimeToUtc(MON, 15 * 60 + 30, "Europe/Istanbul"));
  });

  it("sends exactly the nudges each account should get, once", async () => {
    // alice: training day, reminder 18:00, nothing done today → workout late (Turkish).
    await account("alice");
    // bob: finished today's workout; the session trigger marked the day done.
    await account("bob", { user: { language: "en" } });
    await write("users/bob/workout_sessions/s-mon", sessionDoc("s-mon", MON, true));
    // carol: finished too, but the trigger hasn't run yet; the sweep's own read catches it.
    await account("carol");
    await write("users/carol/workout_sessions/s-mon", sessionDoc("s-mon", MON, true), { trigger: false });
    // dave: notifications off.
    await account("dave", { user: { notificationsEnabled: false } });
    // erin: two devices, one of them uninstalled.
    await account("erin", { tokens: ["tok-erin", "dead-erin"] });
    // frank: New York, 11:35 there, no meal yet → late-morning meal nudge in English.
    await account("frank", { user: { language: "en", timeZone: "America/New_York" }, device: { timeZone: "America/New_York" } });
    // gina: turned "workout late" off in Settings.
    await account("gina", { user: { notificationTypes: { workoutLate: false, mealLogging: true, streakRisk: true, comeback: true } } });
    // hank: a session started 20 minutes ago and still running.
    await account("hank");
    await write("users/hank/workout_sessions/s-mon", { ...sessionDoc("s-mon", MON, false), startedAt: new Date(NOW - 20 * 60_000).toISOString() });
    // Everyone due now (as if their window had just opened).
    for (const uid of ["alice", "bob", "carol", "dave", "erin", "frank", "gina", "hank"]) {
      const ref = db.doc(`notification_state/${uid}`);
      if ((await ref.get()).get("nextCheckAt")) await ref.update({ nextCheckAt: admin.firestore.Timestamp.fromMillis(NOW - 60_000) });
    }

    const messenger = stubMessenger();
    const result = await runNotificationSweep({ db, messenger, nowMs: NOW, pageSize: 3 });

    const byUid = Object.fromEntries(result.sent.map((s) => [s.uid, s]));
    assert.deepEqual(Object.keys(byUid).sort(), ["alice", "erin", "frank"]);
    assert.equal(byUid.alice.kind, "workout_late");
    assert.equal(byUid.alice.title, "Bugünkü antrenmanın hâlâ seni bekliyor");
    assert.equal(byUid.alice.link, "workout");
    assert.equal(byUid.frank.kind, "meal_morning");
    assert.equal(byUid.frank.localDate, MON);
    assert.equal(byUid.frank.title, "How did your day start?");
    assert.equal(byUid.erin.tokens, 2);
    assert.equal(byUid.erin.delivered, 1);
    assert.equal(result.invalidTokensRemoved, 1);
    assert.equal(result.checked, 7); // dave has no nextCheckAt

    // Payload the devices receive.
    const alice = messenger.sent.find((m) => m.token === "tok-alice");
    assert.deepEqual(alice.data, { type: "workout_late", link: "workout", localDate: MON, source: "fitai_push" });
    assert.equal(alice.android.notification.channelId, "fitai_smart_nudges");

    // The dead device is gone and the trigger dropped the count.
    const erinDevices = await db.collection("users/erin/devices").get();
    assert.deepEqual(erinDevices.docs.map((d) => d.get("fcmToken")), ["tok-erin"]);
    await onDeviceWritten(db, "erin", NOW);
    assert.equal((await db.doc("notification_state/erin").get()).get("deviceCount"), 1);

    // Sent log and next check.
    const aliceState = (await db.doc("notification_state/alice").get()).data();
    assert.equal(aliceState.sentDate, MON);
    assert.deepEqual(aliceState.sentKinds, ["workout_late"]);
    assert.ok(aliceState.nextCheckAt.toMillis() > NOW);

    // A second run at the same moment sends nothing (dedupe), even if forced due again.
    await db.doc("notification_state/alice").update({ nextCheckAt: admin.firestore.Timestamp.fromMillis(NOW - 1) });
    const again = stubMessenger();
    const second = await runNotificationSweep({ db, messenger: again, nowMs: NOW });
    assert.equal(again.sent.length, 0);
    assert.equal(second.sent.length, 0);
  });

  it("a logged meal cancels the pending meal nudge", async () => {
    await account("ivy", { user: { timeZone: "America/New_York", language: "en" } });
    await db.doc("notification_state/ivy").update({ nextCheckAt: admin.firestore.Timestamp.fromMillis(NOW - 60_000) });
    await write("users/ivy/meal_logs/m-mon", mealDoc("m-mon", MON, "2026-11-16T13:00:00Z")); // 08:00 New York
    const messenger = stubMessenger();
    const result = await runNotificationSweep({ db, messenger, nowMs: NOW });
    assert.equal(messenger.sent.length, 0);
    assert.equal(result.sent.length, 0);
  });

  it("a deleted account loses its state", async () => {
    await account("jack");
    await db.doc("users/jack").delete();
    await onUserWritten(db, "jack", userDoc(), undefined, NOW);
    assert.equal((await db.doc("notification_state/jack").get()).exists, false);
  });
});
