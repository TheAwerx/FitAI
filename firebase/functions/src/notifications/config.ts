/**
 * Tuning for server-sent nudges. Times are minutes after local midnight in the user's own time zone
 * (`users/{uid}.timeZone`). Every window lies inside the awake hours, so nothing is sent in quiet hours.
 * The design and how these coexist with the app's local reminders: firebase/SCHEMA.md, "Push notifications".
 */
export const NOTIFICATION_CONFIG = {
  /** Quiet hours are everything outside [awakeStart, awakeEnd). */
  awakeStartMinute: 8 * 60,
  awakeEndMinute: 22 * 60,
  /** Server pushes per user per local day (the app's own local reminders are not counted). */
  maxPerDay: 2,
  /** At most one meal nudge a day, at the first window where nothing was logged. */
  maxMealNudgesPerDay: 1,
  /** Minimum time between two server pushes to the same user. */
  minGapMinutes: 90,
  /** "Workout late" opens this long after the user's reminder time on a training day ... */
  workoutGraceMinutes: 90,
  /** ... and stays open this long. */
  workoutWindowMinutes: 120,
  /** A session started this recently and not finished counts as "in progress": no nudge mid-workout. */
  sessionInProgressMinutes: 180,
  mealMorning: { start: 11 * 60, end: 12 * 60 },
  /** Afternoon: nothing logged since `since` (lunch missing). */
  mealAfternoon: { start: 15 * 60 + 30, end: 16 * 60 + 30, since: 11 * 60 },
  /** Evening: nothing logged since `since` (dinner missing). */
  mealEvening: { start: 20 * 60 + 30, end: 21 * 60 + 30, since: 17 * 60 },
  streakRisk: { start: 20 * 60, end: 21 * 60 + 30, minDays: 3 },
  comeback: { start: 18 * 60, end: 19 * 60 + 30 },
  /**
   * Lapsed = no activity today, at least this many planned training days missed since the last active
   * day, and at least `lapsedMinDays` calendar days since it. Lapsed users get only comeback nudges.
   */
  lapsedMissedTrainingDays: 2,
  lapsedMinDays: 3,
  /** Second (last) comeback: this many days of silence and this many days after the first one. */
  secondComebackMinDays: 7,
  secondComebackAfterFirstDays: 4,
  /**
   * No comeback nudges during the first week: the app's local onboarding sequence (D0–D7) covers it.
   * Daily nudges start the day after sign-up.
   */
  comebackQuietDaysAfterSignup: 7,
  /** Meal nudges start on day 2: day 1 has the local "snap your lunch" onboarding reminder. */
  mealQuietDaysAfterSignup: 2,
  /** Active dates kept in `notification_state/{uid}.activeDates` (newest last). */
  maxActiveDates: 60,
  /** Streak lookback in days for the server copy of `Streaks.compute`. */
  streakLookbackDays: 60,
  /** How far ahead `nextCheckAt` looks for the next window. */
  scheduleHorizonDays: 8,
  /** Push time-to-live: a nudge that can't be delivered within this is dropped. */
  pushTtlMinutes: 120,
} as const;

/** Must match the Firestore location for the triggers (nam5 → us-central1; eur3 needs a European region). */
export const NOTIFICATIONS_REGION = "us-central1";

/** Android channel created by the app (`ReminderNotifications.CHANNEL_SMART`). */
export const ANDROID_CHANNEL_ID = "fitai_smart_nudges";
/** Intent action the app's MainActivity handles; the payload `link` picks the screen. */
export const ANDROID_CLICK_ACTION = "com.fitai.app.action.OPEN_LINK";
export const ANDROID_ICON = "ic_stat_fitai";
export const ACCENT_COLOR = "#46F1C5";
