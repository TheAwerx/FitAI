// Valid documents in the exact shape the app writes (core/data/.../sync/FirestoreDocuments.kt).
// FirestoreMappersTest checks the Kotlin side keeps these keys.

export const NOW = "2026-11-15T07:30:12.345Z";

export function userDoc(overrides = {}) {
  return {
    schemaVersion: 1,
    localUserId: "0123456789abcdef0123456789abcdef",
    name: "Ada",
    email: "ada@example.com",
    age: 31,
    heightCm: 168.5,
    weightKg: 64.5,
    gender: "female",
    goal: "build_muscle",
    activityLevel: "moderate",
    equipment: ["dumbbells", "pull_up_bar"],
    dietRestrictions: ["vegetarian"],
    experience: "beginner",
    obstacle: "time",
    focusAreas: ["plate_photo", "smart_overload"],
    daysPerWeek: 4,
    sessionMinutes: 45,
    attributionSource: "instagram",
    commitmentConfirmedAt: "2026-11-01T10:05:00Z",
    notificationsEnabled: true,
    reminderHour: 18,
    reminderMinute: 0,
    language: "tr",
    timeZone: "Europe/Istanbul",
    createdAt: "2026-11-01T10:00:00Z",
    updatedAt: NOW,
    ...overrides,
  };
}

export function workoutPlanDoc(id, overrides = {}) {
  return {
    schemaVersion: 1,
    id,
    title: "Kişisel FitAI planın · 4 gün",
    description: "Haftada 4 gün, seans başı yaklaşık 45 dk.",
    daysPerWeek: 4,
    kind: "program",
    isActive: true,
    createdAt: "2026-11-10T08:00:00Z",
    updatedAt: NOW,
    exercises: [
      { id: "e1", dayOfWeek: 1, name: "Bench press", sets: 4, reps: "8-12", restSeconds: 90, muscleGroup: "Göğüs", notes: null },
      { id: "e2", dayOfWeek: -1, name: "Old row", sets: 3, reps: "10", restSeconds: 60, muscleGroup: "Sırt", notes: "replaced" },
    ],
    ...overrides,
  };
}

export function dietPlanDoc(id, overrides = {}) {
  return {
    schemaVersion: 1,
    id,
    dailyCalories: 2100,
    proteinG: 130,
    carbsG: 240,
    fatG: 70,
    isActive: true,
    createdAt: "2026-11-10T08:00:00Z",
    updatedAt: NOW,
    ...overrides,
  };
}

export function sessionDoc(id, overrides = {}) {
  return {
    schemaVersion: 1,
    id,
    planId: "a1b2c3d4e5f60718",
    startedAt: "2026-11-15T06:00:00Z",
    completedAt: "2026-11-15T06:50:00.120Z",
    completed: true,
    durationMinutes: 50,
    notes: null,
    localDate: "2026-11-15",
    totalSets: 3,
    completedSets: 2,
    volumeKg: 920.5,
    exercises: [
      {
        sessionExerciseId: "se1",
        exerciseId: "e1",
        orderIndex: 0,
        name: "Bench press",
        muscleGroup: "Göğüs",
        sets: [
          { id: "x1", setNumber: 1, targetReps: "8-12", actualReps: "10", weightKg: 60, completed: true, completedAt: "2026-11-15T06:10:00Z" },
        ],
      },
    ],
    updatedAt: NOW,
    ...overrides,
  };
}

export function mealDoc(id, overrides = {}) {
  return {
    schemaVersion: 1,
    id,
    mealName: "Mercimek çorbası ve pilav",
    calories: 520,
    proteinG: 22.5,
    carbsG: 80,
    fatG: 12.4,
    source: "photo",
    photoUrl: "analyzed_photo",
    loggedAt: "2026-11-15T11:00:00Z",
    localDate: "2026-11-15",
    updatedAt: NOW,
    ...overrides,
  };
}

export function measurementDoc(id, overrides = {}) {
  return {
    schemaVersion: 1,
    id,
    weightKg: 72.4,
    bodyFatPct: null,
    chestCm: null,
    waistCm: 81,
    hipCm: null,
    armCm: null,
    photoUrl: null,
    measuredAt: "2026-11-15T06:00:00Z",
    localDate: "2026-11-15",
    updatedAt: NOW,
    ...overrides,
  };
}

export function deviceDoc(overrides = {}) {
  return {
    platform: "android",
    fcmToken: "fcm-token-123",
    language: "tr",
    timeZone: "Europe/Istanbul",
    appVersion: "1.0.0",
    updatedAt: NOW,
    ...overrides,
  };
}

export const USER_KEYS = Object.keys(userDoc());
export const WORKOUT_PLAN_KEYS = Object.keys(workoutPlanDoc("p"));
export const DIET_PLAN_KEYS = Object.keys(dietPlanDoc("d"));
export const SESSION_KEYS = Object.keys(sessionDoc("s"));
export const MEAL_KEYS = Object.keys(mealDoc("m"));
export const MEASUREMENT_KEYS = Object.keys(measurementDoc("w"));
