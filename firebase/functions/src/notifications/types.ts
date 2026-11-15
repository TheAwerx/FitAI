/** Per-type switches from `users/{uid}.notificationTypes` (Settings → Smart notifications). */
export interface NotificationTypes {
  workoutLate: boolean;
  mealLogging: boolean;
  streakRisk: boolean;
  comeback: boolean;
}

export type NotificationTypeKey = keyof NotificationTypes;

export const NOTIFICATION_TYPE_KEYS: NotificationTypeKey[] = ["workoutLate", "mealLogging", "streakRisk", "comeback"];

export const DEFAULT_NOTIFICATION_TYPES: NotificationTypes = {
  workoutLate: true,
  mealLogging: true,
  streakRisk: true,
  comeback: true,
};

/** What a nudge is about. Stored in the sent log, so values must stay stable. */
export type NudgeKind =
  | "workout_late"
  | "meal_morning"
  | "meal_afternoon"
  | "meal_evening"
  | "streak_risk"
  | "comeback";

/** Screen the app opens on tap (`NotificationDeepLink` in core/presentation). */
export type DeepLink = "workout" | "meal_log" | "home";

export type Language = "tr" | "en";

/**
 * Plain-data view of `notification_state/{uid}` (server only). Timestamps are epoch ms here; the
 * Firestore document stores them as Timestamps (see state.ts).
 */
export interface NudgeState {
  // Copied from users/{uid}
  enabled: boolean;
  types: NotificationTypes;
  language: Language;
  timeZone: string;
  reminderHour: number;
  reminderMinute: number;
  daysPerWeek: number;
  /** Local date of `users/{uid}.createdAt`; null when unknown. */
  createdDate: string | null;
  // From the active workout plan
  activePlanId: string | null;
  /** ISO weekdays of the active program's exercises; empty when there is no program. */
  planTrainingDays: number[];
  // From users/{uid}/devices
  deviceCount: number;
  // Activity, kept by the session and meal triggers
  /** Unique local dates with a completed session or a logged meal, ascending, capped. */
  activeDates: string[];
  lastWorkoutDate: string | null;
  lastMealDate: string | null;
  /** `loggedAt` of the newest meal created on `lastMealDate`. */
  lastMealAt: string | null;
  // Sent log (dedupe, caps)
  sentDate: string | null;
  sentKinds: NudgeKind[];
  lastSentAtMs: number | null;
  /** Inactivity stretch the comeback counter belongs to (its last active date). */
  comebackAnchor: string | null;
  comebackCount: number;
  comebackLastDate: string | null;
}

/** What happened today, either from the state (cheap) or read from today's documents (exact). */
export interface DayFacts {
  workoutCompletedToday: boolean;
  sessionInProgress: boolean;
  mealLoggedToday: boolean;
  /** Local minute of the latest meal logged today; null when none. */
  latestMealMinute: number | null;
}

export interface Nudge {
  kind: NudgeKind;
  pref: NotificationTypeKey;
  link: DeepLink;
  /** Copy variant, e.g. the second comeback or "never started". */
  variant: string;
  /** Current streak for `streak_risk`. */
  streakDays?: number;
  /** End of the window, for the push TTL. */
  windowEndMs: number;
}

export interface Evaluation {
  localDate: string;
  localMinute: number;
  send: Nudge | null;
  /** When to look at this user again, assuming [send] (if any) goes out now. Null = nothing scheduled. */
  nextCheckAtMs: number | null;
  /** Why nothing is sent, for logs and tests. */
  reason: string;
  /** For a comeback: the inactivity stretch (last active day) it belongs to. */
  comebackAnchor?: string;
}
