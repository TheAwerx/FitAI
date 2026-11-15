// Unit tests for the nudge decision logic (pure functions, no emulator). Run: npm test
import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { applySent, defaultTrainingDays, evaluate, lapseInfo, nextCheck } from "../lib/notifications/rules.js";
import { computeStreak } from "../lib/notifications/streak.js";
import { addDays, isoWeekKey, localParts, zonedTimeToUtc } from "../lib/notifications/time.js";
import { COPY_KEYS, COPY_TABLE, buildMessages, pushText } from "../lib/notifications/copy.js";
import { activeProgramDays, settingsFromUser, withActivity } from "../lib/notifications/state.js";

const TZ = "Europe/Istanbul"; // UTC+3, no DST
// 2026-11-16 is a Monday (ISO week 2026-W47).
const MON = "2026-11-16";
const TUE = "2026-11-17";
const WED = "2026-11-18";

/** Instant of local wall time [hh:mm] on [date] in Istanbul. */
const at = (date, hhmm, tz = TZ) => {
  const [h, m] = hhmm.split(":").map(Number);
  return zonedTimeToUtc(date, h * 60 + m, tz);
};

function state(overrides = {}) {
  return {
    enabled: true,
    types: { workoutLate: true, mealLogging: true, streakRisk: true, comeback: true },
    language: "tr",
    timeZone: TZ,
    reminderHour: 18,
    reminderMinute: 0,
    daysPerWeek: 3,
    createdDate: "2026-10-01",
    activePlanId: "p1",
    planTrainingDays: [1, 3, 5],
    deviceCount: 1,
    // Active Fri, Sat, Sun before Monday: engaged.
    activeDates: ["2026-11-13", "2026-11-14", "2026-11-15"],
    lastWorkoutDate: "2026-11-13",
    lastMealDate: "2026-11-15",
    lastMealAt: "2026-11-15T17:00:00.000Z",
    sentDate: null,
    sentKinds: [],
    lastSentAtMs: null,
    comebackAnchor: null,
    comebackCount: 0,
    comebackLastDate: null,
    ...overrides,
  };
}

const noFacts = { workoutCompletedToday: false, sessionInProgress: false, mealLoggedToday: false, latestMealMinute: null };

describe("time helpers", () => {
  it("converts between local wall time and UTC", () => {
    assert.equal(new Date(at(MON, "19:30")).toISOString(), "2026-11-16T16:30:00.000Z");
    assert.deepEqual(localParts(Date.parse("2026-11-15T22:30:00Z"), TZ), { date: MON, minute: 90, isoWeekday: 1 });
    // New York switches to daylight time on 2026-03-08.
    assert.equal(new Date(at("2026-03-07", "10:00", "America/New_York")).toISOString(), "2026-03-07T15:00:00.000Z");
    assert.equal(new Date(at("2026-03-09", "10:00", "America/New_York")).toISOString(), "2026-03-09T14:00:00.000Z");
  });

  it("uses ISO week keys like the app", () => {
    assert.equal(isoWeekKey("2026-11-02"), "2026-W45");
    assert.equal(isoWeekKey(MON), "2026-W47");
    assert.equal(isoWeekKey("2026-11-22"), "2026-W47");
    assert.equal(isoWeekKey("2027-01-01"), "2026-W53");
    assert.equal(isoWeekKey("2025-12-29"), "2026-W01");
    assert.equal(addDays("2026-12-31", 1), "2027-01-01");
  });
});

describe("streak (server copy of Streaks.compute)", () => {
  const nov = (d) => `2026-11-${String(d).padStart(2, "0")}`;
  const mwf = [1, 3, 5];
  it("matches the app's StreaksTest cases", () => {
    assert.equal(computeStreak(nov(11), [], mwf, 60).currentDays, 0);
    assert.equal(computeStreak(nov(8), [nov(2), nov(4), nov(6)], mwf, 60).currentDays, 7);
    const today = computeStreak(nov(9), [nov(2), nov(4), nov(6)], mwf, 60);
    assert.equal(today.currentDays, 7);
    assert.equal(today.todayActive, false);
    const frozen = computeStreak(nov(6), [nov(2), nov(3), nov(5), nov(6)], mwf, 60);
    assert.equal(frozen.currentDays, 4);
    assert.equal(frozen.freezeUsedThisWeek, true);
    assert.equal(computeStreak(nov(7), [nov(2), nov(7)], mwf, 60).currentDays, 1);
    assert.equal(computeStreak(nov(11), [nov(5), nov(7), nov(8), nov(9), nov(11)], [1, 2, 3, 4, 5, 6, 7], 60).currentDays, 5);
    assert.equal(computeStreak(nov(6), [nov(6)], mwf, 60).freezeUsedThisWeek, false);
    assert.equal(computeStreak(nov(6), [nov(2), nov(3), nov(6)], [], 60).currentDays, 1);
  });
});

describe("workout late", () => {
  it("fires on a training day after the reminder time plus grace", () => {
    const e = evaluate(state(), at(MON, "19:35"), noFacts);
    assert.equal(e.send?.kind, "workout_late");
    assert.equal(e.send?.link, "workout");
    assert.equal(e.localDate, MON);
  });

  it("waits for the window and schedules its start", () => {
    const e = evaluate(state(), at(MON, "19:00"));
    assert.equal(e.send, null);
    assert.equal(e.nextCheckAtMs, at(MON, "19:30"));
  });

  it("is skipped when today's workout is done or in progress, and on rest days", () => {
    assert.equal(evaluate(state(), at(MON, "19:35"), { ...noFacts, workoutCompletedToday: true }).send, null);
    assert.equal(evaluate(state({ lastWorkoutDate: MON }), at(MON, "19:35")).send, null);
    assert.equal(evaluate(state(), at(MON, "19:35"), { ...noFacts, sessionInProgress: true }).send, null);
    assert.equal(evaluate(state(), at(TUE, "19:35"), noFacts).send, null);
  });

  it("never lands in quiet hours", () => {
    const late = state({ reminderHour: 21, reminderMinute: 30 });
    assert.equal(evaluate(late, at(MON, "21:50"), noFacts).send, null);
    const early = state({ reminderHour: 6, reminderMinute: 0 });
    assert.equal(evaluate(early, at(MON, "07:45"), noFacts).reason, "quiet_hours");
    assert.equal(evaluate(early, at(MON, "08:05"), noFacts).send?.kind, "workout_late");
  });

  it("falls back to the commitment spread without a program", () => {
    assert.deepEqual(defaultTrainingDays(4), [1, 2, 4, 5]);
    const noPlan = state({ activePlanId: null, planTrainingDays: [], daysPerWeek: 2 });
    assert.equal(evaluate(noPlan, at(TUE, "19:35"), noFacts).send?.kind, "workout_late");
    assert.equal(evaluate(noPlan, at(MON, "19:35"), noFacts).send, null);
  });
});

describe("meal nudges", () => {
  it("late morning: nothing logged today", () => {
    assert.equal(evaluate(state(), at(MON, "11:10"), noFacts).send?.kind, "meal_morning");
    const logged = { ...noFacts, mealLoggedToday: true, latestMealMinute: 8 * 60 };
    assert.equal(evaluate(state(), at(MON, "11:10"), logged).send, null);
  });

  it("mid-afternoon: nothing since 11:00", () => {
    const breakfastOnly = { ...noFacts, mealLoggedToday: true, latestMealMinute: 8 * 60 + 30 };
    assert.equal(evaluate(state(), at(TUE, "15:45"), breakfastOnly).send?.kind, "meal_afternoon");
    const lunch = { ...noFacts, mealLoggedToday: true, latestMealMinute: 13 * 60 };
    assert.equal(evaluate(state(), at(TUE, "15:45"), lunch).send, null);
  });

  it("evening: nothing since 17:00", () => {
    const lunch = { ...noFacts, mealLoggedToday: true, latestMealMinute: 13 * 60 };
    assert.equal(evaluate(state(), at(TUE, "20:45"), lunch).send?.kind, "meal_evening");
  });

  it("at most one meal nudge a day", () => {
    const s = state({ sentDate: TUE, sentKinds: ["meal_morning"], lastSentAtMs: at(TUE, "11:05") });
    const e = evaluate(s, at(TUE, "15:45"), noFacts);
    assert.equal(e.send, null);
    assert.equal(e.reason, "meal_afternoon:meal_cap");
  });

  it("uses the stored last meal before reading documents", () => {
    const s = state({ lastMealDate: TUE, lastMealAt: "2026-11-17T10:00:00.000Z" }); // 13:00 local
    assert.equal(evaluate(s, at(TUE, "15:45")).send, null);
    assert.equal(evaluate(s, at(TUE, "20:45")).send?.kind, "meal_evening");
  });
});

describe("dedupe, caps, gap and switches", () => {
  it("sends each kind once per local day", () => {
    const sent = state({ sentDate: MON, sentKinds: ["workout_late"], lastSentAtMs: at(MON, "19:30") });
    // workout_late is not repeated; another kind may still go out.
    assert.equal(evaluate(sent, at(MON, "21:15"), noFacts).send?.kind, "meal_evening");
    const sentNoMeals = { ...sent, types: { ...sent.types, mealLogging: false } };
    assert.equal(evaluate(sentNoMeals, at(MON, "21:15"), noFacts).send, null);
    const yesterday = state({ sentDate: "2026-11-15", sentKinds: ["workout_late"], lastSentAtMs: at("2026-11-15", "19:30") });
    assert.equal(evaluate(yesterday, at(MON, "19:35"), noFacts).send?.kind, "workout_late");
  });

  it("stops at the daily cap", () => {
    const s = state({ sentDate: MON, sentKinds: ["meal_morning", "meal_afternoon"], lastSentAtMs: at(MON, "15:30") });
    assert.equal(evaluate(s, at(MON, "19:35"), noFacts).reason, "workout_late:daily_cap");
    // Nothing more today; the next look is tomorrow's first window.
    assert.equal(evaluate(s, at(MON, "19:35"), noFacts).nextCheckAtMs, at(TUE, "11:00"));
  });

  it("keeps a minimum gap and retries inside the window", () => {
    const s = state({ sentDate: MON, sentKinds: ["meal_afternoon"], lastSentAtMs: at(MON, "19:00") });
    const e = evaluate(s, at(MON, "19:35"), noFacts);
    assert.equal(e.send, null);
    assert.equal(e.reason, "workout_late:gap");
    assert.equal(e.nextCheckAtMs, at(MON, "20:30"));
  });

  it("respects notificationsEnabled, devices and per-type switches", () => {
    assert.deepEqual(
      [evaluate(state({ enabled: false }), at(MON, "19:35")).nextCheckAtMs, evaluate(state({ deviceCount: 0 }), at(MON, "19:35")).nextCheckAtMs],
      [null, null],
    );
    const noMeals = state({ types: { workoutLate: true, mealLogging: false, streakRisk: true, comeback: true } });
    assert.equal(evaluate(noMeals, at(MON, "11:10"), noFacts).send, null);
    assert.equal(evaluate(noMeals, at(MON, "10:00")).nextCheckAtMs, at(MON, "19:30"));
    const noWorkout = state({ types: { workoutLate: false, mealLogging: true, streakRisk: true, comeback: true } });
    assert.equal(evaluate(noWorkout, at(MON, "19:35"), noFacts).send, null);
  });

  it("is silent at night and schedules the next morning", () => {
    const e = evaluate(state(), at(MON, "22:30"));
    assert.equal(e.reason, "quiet_hours");
    assert.equal(e.nextCheckAtMs, at(TUE, "11:00"));
  });

  it("is silent on the sign-up day and skips meal nudges the day after", () => {
    const fresh = state({ createdDate: MON, activeDates: [], lastWorkoutDate: null, lastMealDate: null });
    assert.equal(evaluate(fresh, at(MON, "11:10"), noFacts).reason, "signup_day");
    assert.equal(evaluate(fresh, at(TUE, "11:10"), noFacts).send, null);
    assert.equal(evaluate(fresh, at(WED, "11:10"), noFacts).send?.kind, "meal_morning");
  });
});

describe("streak at risk", () => {
  // Every day is a training day (no program); Tue 17th missed used this week's freeze.
  const streakState = () => state({
    activePlanId: null,
    planTrainingDays: [],
    activeDates: ["2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15", MON],
    lastWorkoutDate: MON,
    lastMealDate: MON,
    lastMealAt: "2026-11-16T10:00:00.000Z",
  });

  it("fires in the evening when today would end the streak", () => {
    const e = evaluate(streakState(), at(WED, "20:05"), noFacts);
    assert.equal(e.send?.kind, "streak_risk");
    assert.equal(e.send?.streakDays, 7);
    assert.equal(pushText(e.send, "en").title, "Keep your 7-day streak going");
    assert.equal(pushText(e.send, "tr").title, "7 günlük serin devam etsin");
  });

  it("stays quiet while the weekly freeze is still unused or today is already active", () => {
    const s = streakState();
    s.activeDates = [...s.activeDates, TUE];
    assert.equal(evaluate(s, at(WED, "20:05"), noFacts).send?.kind, "workout_late"); // Wed is in the default spread
    const activeToday = evaluate(streakState(), at(WED, "20:05"), { ...noFacts, mealLoggedToday: true, latestMealMinute: 9 * 60 });
    assert.equal(activeToday.send?.kind, "workout_late");
  });

  it("replaces the workout and dinner nudges for the day", () => {
    const after = applySent(streakState(), { kind: "streak_risk", pref: "streakRisk", link: "home", variant: "default", windowEndMs: 0 }, at(WED, "20:05"), WED);
    assert.equal(evaluate(after, at(WED, "21:40"), noFacts).send, null);
    const later = evaluate(after, at(WED, "21:00"), noFacts);
    assert.equal(later.send, null);
    assert.equal(later.reason, "meal_evening:suppressed");
  });
});

describe("comeback", () => {
  // Last active Tue 10th; Wed 11th and Fri 13th (training days) missed.
  const lapsed = () => state({ activeDates: ["2026-11-09", "2026-11-10"], lastWorkoutDate: "2026-11-10", lastMealDate: "2026-11-10" });

  it("replaces daily nudges once the user has lapsed", () => {
    const info = lapseInfo(lapsed(), MON, false);
    assert.equal(info.lapsed, true);
    assert.equal(info.anchor, "2026-11-10");
    assert.equal(evaluate(lapsed(), at(MON, "11:10"), noFacts).send, null);
    assert.equal(evaluate(lapsed(), at(MON, "10:00")).nextCheckAtMs, at(MON, "18:00"));
    const e = evaluate(lapsed(), at(MON, "18:10"), noFacts);
    assert.equal(e.send?.kind, "comeback");
    assert.equal(e.send?.variant, "first");
    assert.equal(e.comebackAnchor, "2026-11-10");
  });

  it("does not count planned rest days as lapse", () => {
    // Active Fri 13th, weekend off, Monday is the next training day.
    const weekend = state({ activeDates: ["2026-11-13"], lastWorkoutDate: "2026-11-13", lastMealDate: "2026-11-13" });
    assert.equal(lapseInfo(weekend, MON, false).lapsed, false);
    assert.equal(evaluate(weekend, at(MON, "18:10"), noFacts).send, null);
  });

  it("sends a second one after a week of silence, then nothing", () => {
    const e = evaluate(lapsed(), at(MON, "18:10"), noFacts);
    const once = applySent(lapsed(), e.send, at(MON, "18:10"), MON, e.comebackAnchor);
    assert.equal(once.comebackCount, 1);
    // Next: Friday 20th, 4 days after the first one (10 days since the last activity).
    assert.equal(e.nextCheckAtMs, at("2026-11-20", "18:00"));
    const second = evaluate(once, at("2026-11-20", "18:05"), noFacts);
    assert.equal(second.send?.variant, "second");
    const twice = applySent(once, second.send, at("2026-11-20", "18:05"), "2026-11-20", second.comebackAnchor);
    assert.equal(nextCheck(twice, at("2026-11-20", "18:05")), null);
  });

  it("starts over after new activity and uses its own copy for users who never started", () => {
    const s = state({ createdDate: "2026-11-01", activeDates: [], lastWorkoutDate: null, lastMealDate: null });
    const e = evaluate(s, at(MON, "18:10"), noFacts);
    assert.equal(e.send?.variant, "start");
    const young = state({ createdDate: "2026-11-11", activeDates: [], lastWorkoutDate: null, lastMealDate: null });
    assert.equal(evaluate(young, at(MON, "18:10"), noFacts).send, null);
    const back = withActivity(applySent(lapsed(), { kind: "comeback", pref: "comeback", link: "workout", variant: "first", windowEndMs: 0 }, at(MON, "18:10"), MON, "2026-11-10"), MON, { workout: true });
    assert.equal(lapseInfo(back, TUE, false).lapsed, false);
  });
});

describe("state helpers", () => {
  it("reads the settings the app writes to users/{uid}", () => {
    const s = settingsFromUser({
      notificationsEnabled: true,
      notificationTypes: { workoutLate: false, mealLogging: true, streakRisk: true, comeback: false },
      language: "en",
      timeZone: "Not/AZone",
      reminderHour: 7,
      reminderMinute: 45,
      daysPerWeek: 5,
      createdAt: "2026-11-01T22:30:00Z",
    });
    assert.equal(s.timeZone, "UTC");
    assert.equal(s.types.workoutLate, false);
    assert.equal(s.language, "en");
    assert.equal(s.createdDate, "2026-11-01");
    // Older documents without notificationTypes: everything on.
    assert.deepEqual(settingsFromUser({ timeZone: TZ }).types, { workoutLate: true, mealLogging: true, streakRisk: true, comeback: true });
  });

  it("takes training days only from the active program", () => {
    const plan = { isActive: true, kind: "program", daysPerWeek: 3, exercises: [{ dayOfWeek: 1 }, { dayOfWeek: 3 }, { dayOfWeek: -1 }, { dayOfWeek: 1 }] };
    assert.deepEqual(activeProgramDays(plan), [1, 3]);
    assert.equal(activeProgramDays({ ...plan, isActive: false }), null);
    assert.equal(activeProgramDays({ ...plan, kind: "adhoc", daysPerWeek: 0 }), null);
  });

  it("records activity idempotently and keeps the newest meal", () => {
    let s = withActivity(state(), MON, { mealLoggedAt: "2026-11-16T09:00:00Z" });
    s = withActivity(s, MON, { mealLoggedAt: "2026-11-16T06:00:00Z" });
    s = withActivity(s, MON, { workout: true });
    s = withActivity(s, MON, { workout: true });
    assert.equal(s.activeDates.filter((d) => d === MON).length, 1);
    assert.equal(s.lastMealAt, "2026-11-16T09:00:00Z");
    assert.equal(s.lastWorkoutDate, MON);
  });
});

describe("copy and messages", () => {
  it("has every message in Turkish and English, short and filled in", () => {
    for (const language of ["tr", "en"]) {
      assert.deepEqual(Object.keys(COPY_TABLE[language]).sort(), [...COPY_KEYS].sort());
      for (const key of COPY_KEYS) {
        const { title, body } = COPY_TABLE[language][key];
        assert.ok(title.length > 0 && title.length <= 50, `${language} ${key} title`);
        assert.ok(body.length > 0 && body.length <= 110, `${language} ${key} body`);
      }
    }
  });

  it("builds one message per token with the deep link and platform options", () => {
    const nudge = { kind: "meal_morning", pref: "mealLogging", link: "meal_log", variant: "default", windowEndMs: at(MON, "12:00") };
    const messages = buildMessages(nudge, "en", ["t1", "t2"], MON, at(MON, "11:00"));
    assert.equal(messages.length, 2);
    assert.equal(messages[0].token, "t1");
    assert.deepEqual(messages[0].data, { type: "meal_morning", link: "meal_log", localDate: MON, source: "fitai_push" });
    assert.equal(messages[0].notification.title, "How did your day start?");
    assert.equal(messages[0].android.notification.channelId, "fitai_smart_nudges");
    assert.equal(messages[0].android.notification.clickAction, "com.fitai.app.action.OPEN_LINK");
    assert.equal(messages[0].android.ttl, 90 * 60_000);
    assert.equal(messages[0].apns.headers["apns-collapse-id"], "meal_morning");
  });
});
