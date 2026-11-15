// Trial callables against the Firestore emulator (never the live project). Run: `npm test` in firebase/functions.
import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { after, before, beforeEach, describe, it } from "node:test";

const require = createRequire(import.meta.url);
const admin = require("firebase-admin");
const trial = require("../lib/trial.js");

const PROJECT_ID = "demo-fitai";
const PEPPER = "test-pepper-0123456789abcdef0123456789abcdef";
const DAY = 24 * 60 * 60 * 1000;
const T0 = Date.UTC(2026, 10, 15, 8, 0, 0);

const alice = { uid: "alice", anonymous: false };
const bob = { uid: "bob", anonymous: false };
const carol = { uid: "carol", anonymous: false };
const anon = { uid: "anon-1", anonymous: true };
const phone = { deviceKey: "a".repeat(64), platform: "android" };
const tablet = { deviceKey: "b".repeat(64), platform: "ios" };

let db;

function emulatorHost() {
  const host = process.env.FIRESTORE_EMULATOR_HOST;
  assert.ok(host, "FIRESTORE_EMULATOR_HOST is not set; run through `npm test` (firebase emulators:exec)");
  return host;
}

async function clearFirestore() {
  const url = `http://${emulatorHost()}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`;
  const response = await fetch(url, { method: "DELETE" });
  assert.equal(response.status, 200);
}

function request(data, auth) {
  return { data, auth, rawRequest: {}, acceptsStreaming: false };
}

function authOf(uid, provider = "password") {
  return { uid, token: { firebase: { sign_in_provider: provider } } };
}

async function rejectsWith(promise, code, message) {
  await assert.rejects(promise, (error) => {
    assert.equal(error.code, code);
    if (message) assert.equal(error.message, message);
    return true;
  });
}

before(() => {
  emulatorHost();
  admin.initializeApp({ projectId: PROJECT_ID });
  db = admin.firestore();
});

after(async () => {
  await Promise.all(admin.apps.map((app) => app.delete()));
});

beforeEach(async () => {
  await clearFirestore();
  process.env.TRIAL_DEVICE_PEPPER = PEPPER;
  delete process.env.FUNCTIONS_EMULATOR;
});

describe("startTrial", () => {
  it("gives the first account on a device the 7-day trial", async () => {
    const status = await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    assert.equal(status.state, "active");
    assert.equal(status.started, true);
    assert.equal(status.daysRemaining, 7);
    assert.equal(status.trialDays, 7);
    assert.equal(status.startedAt, new Date(T0).toISOString());
    assert.equal(status.endsAt, new Date(T0 + 7 * DAY).toISOString());
    assert.equal(status.isActive, true);

    const hash = trial.hashDeviceKey(PEPPER, phone.deviceKey);
    const account = await db.doc("trials/alice").get();
    const device = await db.doc(`device_trials/${hash}`).get();
    assert.equal(account.get("deviceHash"), hash);
    assert.equal(account.get("endsAt").toMillis(), T0 + 7 * DAY);
    assert.equal(device.get("firstUid"), "alice");
    assert.equal(device.get("platform"), "android");
    assert.equal(device.get("keyVersion"), 1);
  });

  it("never stores the raw device key", async () => {
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    const devices = await db.collection("device_trials").get();
    const accounts = await db.collection("trials").get();
    const dump = JSON.stringify([...devices.docs, ...accounts.docs].map((d) => ({ id: d.id, ...d.data() })));
    assert.ok(!dump.includes(phone.deviceKey), "raw device key leaked into Firestore");
    assert.equal(devices.docs[0].id.length, 64);
  });

  it("refuses a second account on the same device", async () => {
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    const status = await trial.startTrialFor(db, bob, phone, PEPPER, T0 + DAY);
    assert.equal(status.state, "used");
    assert.equal(status.started, false);
    assert.equal(status.isActive, false);
    assert.equal((await db.doc("trials/bob").get()).exists, false);
    assert.equal((await trial.trialStatusFor(db, bob, phone, PEPPER, T0 + DAY)).state, "used");
  });

  it("keeps (doesn't restart) the account's trial on a second device", async () => {
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    const status = await trial.startTrialFor(db, alice, tablet, PEPPER, T0 + 3 * DAY);
    assert.equal(status.state, "active");
    assert.equal(status.started, false);
    assert.equal(status.startedAt, new Date(T0).toISOString());
    assert.equal(status.daysRemaining, 4);
    assert.equal((await trial.trialStatusFor(db, alice, tablet, PEPPER, T0 + 3 * DAY)).daysRemaining, 4);
    // Only the device the trial started on is claimed.
    const tabletHash = trial.hashDeviceKey(PEPPER, tablet.deviceKey);
    assert.equal((await db.doc(`device_trials/${tabletHash}`).get()).exists, false);
  });

  it("lets exactly one of several accounts racing on one device win", async () => {
    const callers = ["u1", "u2", "u3", "u4", "u5"].map((uid) => ({ uid, anonymous: false }));
    const results = await Promise.all(callers.map((caller) => trial.startTrialFor(db, caller, phone, PEPPER, T0)));
    const winners = results.filter((status) => status.started);
    assert.equal(winners.length, 1);
    assert.equal(results.filter((status) => status.state === "used").length, 4);
    const accounts = await db.collection("trials").get();
    assert.equal(accounts.size, 1);
    const device = await db.doc(`device_trials/${trial.hashDeviceKey(PEPPER, phone.deviceKey)}`).get();
    assert.equal(device.get("firstUid"), accounts.docs[0].id);
  });

  it("is idempotent for the same account racing itself", async () => {
    const results = await Promise.all([1, 2, 3].map(() => trial.startTrialFor(db, alice, phone, PEPPER, T0)));
    assert.equal(results.filter((status) => status.started).length, 1);
    assert.ok(results.every((status) => status.state === "active"));
  });

  it("expires by server time and is never restarted", async () => {
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    const lastMoment = await trial.trialStatusFor(db, alice, phone, PEPPER, T0 + 7 * DAY - 1);
    assert.equal(lastMoment.state, "active");
    assert.equal(lastMoment.daysRemaining, 1);
    const ended = await trial.trialStatusFor(db, alice, phone, PEPPER, T0 + 7 * DAY);
    assert.equal(ended.state, "expired");
    assert.equal(ended.daysRemaining, 0);
    assert.equal(ended.isActive, false);
    const again = await trial.startTrialFor(db, alice, tablet, PEPPER, T0 + 30 * DAY);
    assert.equal(again.state, "expired");
    assert.equal(again.started, false);
    assert.equal(again.endsAt, new Date(T0 + 7 * DAY).toISOString());
  });

  it("restores the account document from the device claim instead of restarting", async () => {
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    await db.doc("trials/alice").delete();
    const status = await trial.startTrialFor(db, alice, phone, PEPPER, T0 + 2 * DAY);
    assert.equal(status.started, false);
    assert.equal(status.startedAt, new Date(T0).toISOString());
    assert.equal((await db.doc("trials/alice").get()).exists, true);
  });

  it("refuses anonymous sessions", async () => {
    await rejectsWith(trial.startTrialFor(db, anon, phone, PEPPER, T0), "permission-denied", "trial_requires_account");
    assert.equal((await db.collection("device_trials").get()).size, 0);
  });
});

describe("getTrialStatus", () => {
  it("tells an anonymous session (paywall before sign-up) whether the device can still start one", async () => {
    assert.equal((await trial.trialStatusFor(db, anon, phone, PEPPER, T0)).state, "eligible");
    await trial.startTrialFor(db, alice, phone, PEPPER, T0);
    assert.equal((await trial.trialStatusFor(db, anon, phone, PEPPER, T0)).state, "used");
    assert.equal((await trial.trialStatusFor(db, anon, tablet, PEPPER, T0)).state, "eligible");
  });

  it("reports eligible for a new account on a new device and nothing is written", async () => {
    const status = await trial.trialStatusFor(db, carol, tablet, PEPPER, T0);
    assert.equal(status.state, "eligible");
    assert.equal(status.startedAt, null);
    assert.equal((await db.collection("trials").get()).size, 0);
    assert.equal((await db.collection("device_trials").get()).size, 0);
  });

  it("falls back to the account only when no device key is sent (older builds)", async () => {
    assert.equal((await trial.trialStatusFor(db, bob, null, PEPPER, T0)).state, "eligible");
    await trial.startTrialFor(db, bob, phone, PEPPER, T0);
    assert.equal((await trial.trialStatusFor(db, bob, null, PEPPER, T0)).state, "active");
  });
});

describe("callables", () => {
  const data = { deviceKey: phone.deviceKey, platform: "android" };

  it("refuse unauthenticated callers", async () => {
    await rejectsWith(Promise.resolve().then(() => trial.startTrial.run(request(data, undefined))), "unauthenticated");
    await rejectsWith(Promise.resolve().then(() => trial.getTrialStatus.run(request(data, undefined))), "unauthenticated");
  });

  it("refuse to start a trial for an anonymous session", async () => {
    await rejectsWith(trial.startTrial.run(request(data, authOf("anon-1", "anonymous"))), "permission-denied", "trial_requires_account");
    const status = await trial.getTrialStatus.run(request(data, authOf("anon-1", "anonymous")));
    assert.equal(status.state, "eligible");
  });

  it("validate the device key and platform", async () => {
    const bad = [
      {},
      { deviceKey: "short", platform: "android" },
      { deviceKey: "x".repeat(200), platform: "android" },
      { deviceKey: "../../etc/passwd/aaaaaaaa", platform: "android" },
      { deviceKey: phone.deviceKey, platform: "web" },
      { deviceKey: 12345678901234567890, platform: "ios" },
    ];
    for (const payload of bad) {
      await rejectsWith(trial.startTrial.run(request(payload, authOf("alice"))), "invalid-argument", "trial_invalid_device");
    }
  });

  it("use server time and ignore dates sent by the client", async () => {
    const before = Date.now();
    const status = await trial.startTrial.run(request({ ...data, endsAt: "2099-01-01T00:00:00Z", now: 0 }, authOf("alice", "google.com")));
    assert.equal(status.started, true);
    const endsAt = Date.parse(status.endsAt);
    assert.ok(endsAt >= before + 7 * DAY && endsAt <= Date.now() + 7 * DAY);
    const second = await trial.startTrial.run(request(data, authOf("bob", "password")));
    assert.equal(second.state, "used");
  });

  it("refuse to start without the pepper secret in production, work with the emulator fallback", async () => {
    delete process.env.TRIAL_DEVICE_PEPPER;
    await rejectsWith(trial.startTrial.run(request(data, authOf("alice"))), "failed-precondition", "trial_not_configured");
    process.env.FUNCTIONS_EMULATOR = "true";
    const status = await trial.startTrial.run(request(data, authOf("alice")));
    assert.equal(status.started, true);
  });
});

describe("device key hashing", () => {
  it("depends on the pepper and is stable", () => {
    const a = trial.hashDeviceKey(PEPPER, phone.deviceKey);
    assert.equal(a, trial.hashDeviceKey(PEPPER, phone.deviceKey));
    assert.notEqual(a, trial.hashDeviceKey(`${PEPPER}-other`, phone.deviceKey));
    assert.match(a, /^[0-9a-f]{64}$/);
  });

  it("accepts only a long pepper outside the emulator", () => {
    assert.equal(trial.resolvePepper(undefined, false), null);
    assert.equal(trial.resolvePepper("short", false), null);
    assert.equal(trial.resolvePepper(PEPPER, false), PEPPER);
    assert.ok(trial.resolvePepper(undefined, true));
  });
});
