import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc } from "firebase/firestore";
import {
  deviceDoc,
  dietPlanDoc,
  mealDoc,
  measurementDoc,
  sessionDoc,
  userDoc,
  workoutPlanDoc,
} from "./fixtures.mjs";

const PROJECT_ID = "demo-fitai";
let env;

const signedIn = (uid, provider = "password") =>
  env.authenticatedContext(uid, { firebase: { sign_in_provider: provider } }).firestore();
const anonymous = (uid) => signedIn(uid, "anonymous");
const guest = () => env.unauthenticatedContext().firestore();

async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

/** assertFails with the case name in the error, for loops over many cases. */
async function failsFor(label, promise) {
  try {
    await assertFails(promise);
  } catch (error) {
    throw new Error(`expected denial: ${label}: ${error.message}`);
  }
}

function omit(object, key) {
  const copy = { ...object };
  delete copy[key];
  return copy;
}

before(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080").split(":");
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8"),
      host,
      port: Number(port),
    },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

describe("users/{uid}", () => {
  it("owner with a real sign-in can write and read the full profile", async () => {
    const db = signedIn("alice");
    await assertSucceeds(setDoc(doc(db, "users/alice"), userDoc()));
    await assertSucceeds(getDoc(doc(db, "users/alice")));
    await assertSucceeds(setDoc(doc(db, "users/alice"), userDoc({ gender: null, experience: null, obstacle: null, attributionSource: null, commitmentConfirmedAt: null, focusAreas: [] })));
  });

  it("works for google and apple sign-ins too", async () => {
    await assertSucceeds(setDoc(doc(signedIn("g", "google.com"), "users/g"), userDoc()));
    await assertSucceeds(setDoc(doc(signedIn("a", "apple.com"), "users/a"), userDoc()));
  });

  it("denies other users, anonymous sessions and signed-out clients", async () => {
    await seed("users/alice", userDoc());
    await assertFails(getDoc(doc(signedIn("bob"), "users/alice")));
    await assertFails(setDoc(doc(signedIn("bob"), "users/alice"), userDoc()));
    await assertFails(setDoc(doc(anonymous("alice"), "users/alice"), userDoc()));
    await assertFails(getDoc(doc(anonymous("alice"), "users/alice")));
    await assertFails(getDoc(doc(guest(), "users/alice")));
  });

  it("can't be deleted from the client", async () => {
    await seed("users/alice", userDoc());
    await assertFails(deleteDoc(doc(signedIn("alice"), "users/alice")));
  });

  it("rejects unknown codes, bad ranges and wrong types", async () => {
    const db = signedIn("alice");
    const bad = [
      { goal: "get_huge" },
      { activityLevel: "couch" },
      { gender: "other" },
      { experience: "pro" },
      { obstacle: "weather" },
      { equipment: ["dumbbells", "laser"] },
      { dietRestrictions: "vegan" },
      { focusAreas: ["streak", "ai_coach", "log_lifts", "plate_photo"] },
      { age: 7 },
      { age: "31" },
      { heightCm: 400 },
      { weightKg: 10 },
      { daysPerWeek: 9 },
      { sessionMinutes: 2 },
      { reminderHour: 24 },
      { language: "de" },
      { timeZone: "" },
      { name: "x".repeat(81) },
      { createdAt: "yesterday" },
      { schemaVersion: 0 },
      { isAdmin: true },
      { notificationTypes: { workoutLate: true, mealLogging: true, streakRisk: true } },
      { notificationTypes: { workoutLate: true, mealLogging: true, streakRisk: true, comeback: true, marketing: true } },
      { notificationTypes: { workoutLate: "yes", mealLogging: true, streakRisk: true, comeback: true } },
      { notificationTypes: true },
    ];
    for (const overrides of bad) {
      await failsFor(JSON.stringify(overrides), setDoc(doc(db, "users/alice"), userDoc(overrides)));
    }
    await assertFails(setDoc(doc(db, "users/alice"), omit(userDoc(), "obstacle")));
    await assertFails(setDoc(doc(db, "users/alice"), omit(userDoc(), "notificationTypes")));
  });
});

describe("users/{uid} subcollections", () => {
  it("accepts the documents the app writes", async () => {
    const db = signedIn("alice");
    await assertSucceeds(setDoc(doc(db, "users/alice/workout_plans/p1"), workoutPlanDoc("p1")));
    await assertSucceeds(setDoc(doc(db, "users/alice/workout_plans/p2"), workoutPlanDoc("p2", { daysPerWeek: 0, kind: "adhoc", isActive: false })));
    await assertSucceeds(setDoc(doc(db, "users/alice/diet_plans/d1"), dietPlanDoc("d1")));
    await assertSucceeds(setDoc(doc(db, "users/alice/workout_sessions/s1"), sessionDoc("s1")));
    await assertSucceeds(setDoc(doc(db, "users/alice/workout_sessions/s2"), sessionDoc("s2", { completedAt: null, completed: false, durationMinutes: null })));
    await assertSucceeds(setDoc(doc(db, "users/alice/meal_logs/m1"), mealDoc("m1")));
    await assertSucceeds(setDoc(doc(db, "users/alice/meal_logs/m2"), mealDoc("m2", { source: "manual", photoUrl: null })));
    await assertSucceeds(setDoc(doc(db, "users/alice/measurements/w1"), measurementDoc("w1")));
    await assertSucceeds(setDoc(doc(db, "users/alice/devices/install-1"), deviceDoc()));
    await assertSucceeds(getDocs(collection(db, "users/alice/workout_sessions")));
  });

  it("is owner only", async () => {
    await seed("users/alice/meal_logs/m1", mealDoc("m1"));
    const bob = signedIn("bob");
    await assertFails(getDoc(doc(bob, "users/alice/meal_logs/m1")));
    await assertFails(getDocs(collection(bob, "users/alice/meal_logs")));
    await assertFails(setDoc(doc(bob, "users/alice/meal_logs/m2"), mealDoc("m2")));
    await assertFails(setDoc(doc(anonymous("alice"), "users/alice/meal_logs/m2"), mealDoc("m2")));
    await assertFails(deleteDoc(doc(bob, "users/alice/meal_logs/m1")));
  });

  it("lets the owner delete meals, measurements and devices but not plans or sessions", async () => {
    await seed("users/alice/meal_logs/m1", mealDoc("m1"));
    await seed("users/alice/measurements/w1", measurementDoc("w1"));
    await seed("users/alice/devices/install-1", deviceDoc());
    await seed("users/alice/workout_plans/p1", workoutPlanDoc("p1"));
    await seed("users/alice/diet_plans/d1", dietPlanDoc("d1"));
    await seed("users/alice/workout_sessions/s1", sessionDoc("s1"));
    const db = signedIn("alice");
    await assertSucceeds(deleteDoc(doc(db, "users/alice/meal_logs/m1")));
    await assertSucceeds(deleteDoc(doc(db, "users/alice/measurements/w1")));
    await assertSucceeds(deleteDoc(doc(db, "users/alice/devices/install-1")));
    await assertFails(deleteDoc(doc(db, "users/alice/workout_plans/p1")));
    await assertFails(deleteDoc(doc(db, "users/alice/diet_plans/d1")));
    await assertFails(deleteDoc(doc(db, "users/alice/workout_sessions/s1")));
  });

  it("rejects malformed documents", async () => {
    const db = signedIn("alice");
    const cases = [
      ["users/alice/workout_plans/p1", workoutPlanDoc("other")],
      ["users/alice/workout_plans/p1", workoutPlanDoc("p1", { daysPerWeek: 0, kind: "adhoc", isActive: true })],
      ["users/alice/workout_plans/p1", workoutPlanDoc("p1", { kind: "adhoc" })],
      ["users/alice/workout_plans/p1", workoutPlanDoc("p1", { exercises: "Bench press" })],
      ["users/alice/diet_plans/d1", dietPlanDoc("d1", { dailyCalories: -10 })],
      ["users/alice/diet_plans/d1", dietPlanDoc("d1", { proteinG: 12.5 })],
      ["users/alice/workout_sessions/s1", sessionDoc("s1", { completed: false })],
      ["users/alice/workout_sessions/s1", sessionDoc("s1", { localDate: "15.11.2026" })],
      ["users/alice/workout_sessions/s1", sessionDoc("s1", { completedSets: 5 })],
      ["users/alice/workout_sessions/s1", sessionDoc("s1", { planId: "../../x" })],
      ["users/alice/meal_logs/m1", mealDoc("m1", { source: "ai" })],
      ["users/alice/meal_logs/m1", mealDoc("m1", { calories: 20000 })],
      ["users/alice/meal_logs/m1", omit(mealDoc("m1"), "localDate")],
      ["users/alice/measurements/w1", measurementDoc("w1", { weightKg: 5 })],
      ["users/alice/measurements/w1", measurementDoc("w1", { bodyFatPct: 95 })],
      ["users/alice/devices/install-1", deviceDoc({ platform: "web" })],
      ["users/alice/devices/install-1", deviceDoc({ fcmToken: "" })],
      ["users/alice/devices/install-1", omit(deviceDoc(), "schemaVersion")],
      ["users/alice/devices/install-1", deviceDoc({ language: "de" })],
      ["users/alice/devices/install-1", deviceDoc({ appVersion: "x".repeat(33) })],
      ["users/alice/devices/bad.id", deviceDoc()],
      ["users/alice/unknown/x", { a: 1 }],
    ];
    for (const [path, data] of cases) {
      await failsFor(path + " " + JSON.stringify(data).slice(0, 80), setDoc(doc(db, path), data));
    }
  });
});

describe("server-only and reserved collections", () => {
  it("guest_trials is readable by its own session, never writable", async () => {
    await seed("guest_trials/anon1", { trialStartedAt: "2026-11-15T00:00:00.000Z", trialExpiresAt: "2026-11-18T00:00:00.000Z", daysRemaining: 3, isActive: true, hasSubscription: false });
    await assertSucceeds(getDoc(doc(anonymous("anon1"), "guest_trials/anon1")));
    await assertFails(getDoc(doc(signedIn("bob"), "guest_trials/anon1")));
    await assertFails(setDoc(doc(anonymous("anon1"), "guest_trials/anon1"), { isActive: true, hasSubscription: true }));
    await assertFails(getDocs(collection(signedIn("bob"), "guest_trials")));
  });

  it("plan_quotas is read-only for its owner", async () => {
    await seed("plan_quotas/alice", { workoutPlansGenerated: 1, dietPlansGenerated: 1 });
    await assertSucceeds(getDoc(doc(signedIn("alice"), "plan_quotas/alice")));
    await assertFails(getDoc(doc(signedIn("bob"), "plan_quotas/alice")));
    await assertFails(setDoc(doc(signedIn("alice"), "plan_quotas/alice"), { workoutPlansGenerated: 0 }));
  });

  it("trials is closed to clients, even the owner", async () => {
    await seed("trials/alice", { schemaVersion: 1, trialDays: 7, deviceHash: "hash1", platform: "android" });
    await assertFails(getDoc(doc(signedIn("alice"), "trials/alice")));
    await assertFails(setDoc(doc(signedIn("alice"), "trials/alice"), { trialDays: 365 }));
    await assertFails(setDoc(doc(signedIn("bob"), "trials/bob"), { trialDays: 7 }));
    await assertFails(deleteDoc(doc(signedIn("alice"), "trials/alice")));
    await assertFails(getDocs(collection(signedIn("alice"), "trials")));
    await assertFails(getDoc(doc(anonymous("alice"), "trials/alice")));
  });

  it("device_trials and aiUsage are closed to clients", async () => {
    await seed("device_trials/hash1", { firstUid: "alice", platform: "android", keyVersion: 1 });
    await assertFails(deleteDoc(doc(signedIn("alice"), "device_trials/hash1")));
    await assertFails(getDocs(collection(signedIn("alice"), "device_trials")));
    await seed("aiUsage/alice/days/2026-11-15", { coachChat: 3 });
    const db = signedIn("alice");
    await assertFails(getDoc(doc(db, "device_trials/hash1")));
    await assertFails(setDoc(doc(db, "device_trials/hash2"), { uids: ["alice"] }));
    await assertFails(getDoc(doc(db, "aiUsage/alice/days/2026-11-15")));
    await assertFails(setDoc(doc(db, "aiUsage/alice/days/2026-11-15"), { coachChat: 0 }));
  });

  it("notification_state (push schedule and sent log) is closed to clients", async () => {
    await seed("notification_state/alice", { enabled: true, sentKinds: ["workout_late"], nextCheckAt: null });
    const db = signedIn("alice");
    await assertFails(getDoc(doc(db, "notification_state/alice")));
    await assertFails(setDoc(doc(db, "notification_state/alice"), { sentKinds: [] }));
    await assertFails(deleteDoc(doc(db, "notification_state/alice")));
    await assertFails(getDocs(collection(db, "notification_state")));
  });

  it("legacy user_profiles is read-only for its owner", async () => {
    await seed("user_profiles/alice", { name: "Ada", goal: "lose_fat" });
    await assertSucceeds(getDoc(doc(signedIn("alice"), "user_profiles/alice")));
    await assertFails(getDoc(doc(signedIn("bob"), "user_profiles/alice")));
    await assertFails(setDoc(doc(signedIn("alice"), "user_profiles/alice"), { name: "Ada" }));
  });

  it("anything else is denied", async () => {
    await assertFails(setDoc(doc(signedIn("alice"), "random/doc"), { a: 1 }));
    await assertFails(getDoc(doc(signedIn("alice"), "random/doc")));
  });
});
