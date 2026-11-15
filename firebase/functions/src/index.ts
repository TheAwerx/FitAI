import * as admin from "firebase-admin";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import {
  UserProfilePayload,
  asInt,
  asNumber,
  asString,
  exerciseSchema,
  normalizeExercise,
  parseLocale,
} from "./common";
import {
  adaptedSessionSchema,
  coachChatPrompt,
  coachReplySchema,
  coachSystemInstruction,
  nextMealPrompt,
  nextMealSchema,
  normalizeAdaptedSession,
  normalizeCoachReply,
  normalizeNextMeal,
  normalizeWeeklyReport,
  parseCoachChatRequest,
  parseNextMealRequest,
  parseTodaySessionRequest,
  parseWeeklyReportRequest,
  todaySessionPrompt,
  weeklyReportPrompt,
  weeklyReportSchema,
} from "./coach";
import { withDailyQuota } from "./rateLimit";

admin.initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");
const TRIAL_DAYS = 3;
const GEMINI_PLAN_MODEL = "gemini-3.1-pro-preview";
const GEMINI_VISION_MODEL = "gemini-3.5-flash";
const FUNCTIONS_REGION = "us-central1";

const geminiCallable = {
  region: FUNCTIONS_REGION,
  timeoutSeconds: 120,
  memory: "512MiB" as const,
  cors: true,
  invoker: "public" as const,
  secrets: [geminiApiKey],
};

const authCallable = {
  region: FUNCTIONS_REGION,
  cors: true,
  invoker: "public" as const,
};

const workoutSchema = {
  type: "OBJECT",
  properties: {
    title: { type: "STRING" },
    description: { type: "STRING" },
    daysPerWeek: { type: "INTEGER" },
    exercises: { type: "ARRAY", items: exerciseSchema },
  },
  required: ["title", "description", "daysPerWeek", "exercises"],
};

const dietSchema = {
  type: "OBJECT",
  properties: {
    dailyCalories: { type: "INTEGER" },
    proteinG: { type: "INTEGER" },
    carbsG: { type: "INTEGER" },
    fatG: { type: "INTEGER" },
  },
  required: ["dailyCalories", "proteinG", "carbsG", "fatG"],
};

const mealSchema = {
  type: "OBJECT",
  properties: {
    mealName: { type: "STRING" },
    calories: { type: "INTEGER" },
    proteinG: { type: "NUMBER" },
    carbsG: { type: "NUMBER" },
    fatG: { type: "NUMBER" },
  },
  required: ["mealName", "calories", "proteinG", "carbsG", "fatG"],
};

async function callGemini(
  apiKey: string,
  model: string,
  parts: object[],
  responseSchema: object,
  systemInstruction?: string,
): Promise<unknown> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": apiKey,
    },
    body: JSON.stringify({
      ...(systemInstruction ? { systemInstruction: { parts: [{ text: systemInstruction }] } } : {}),
      contents: [{ role: "user", parts }],
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema,
        temperature: 0.6,
        maxOutputTokens: 8192,
      },
    }),
  });
  const bodyText = await response.text();
  if (!response.ok) {
    throw new HttpsError(
      "internal",
      `Gemini error ${response.status}: ${bodyText.slice(0, 400)}`,
    );
  }
  const data = JSON.parse(bodyText) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new HttpsError("internal", "Empty Gemini response");
  return parseJson(text);
}

function parseJson(raw: string): unknown {
  const trimmed = raw.trim().replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "");
  try {
    return JSON.parse(trimmed);
  } catch {
    throw new HttpsError("internal", "Gemini returned invalid JSON");
  }
}

function requireAuth(request: CallableRequest): string {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication required");
  }
  return request.auth.uid;
}

function normalizeWorkout(parsed: Record<string, unknown>) {
  const exercises = Array.isArray(parsed.exercises) ? parsed.exercises : [];
  return {
    title: asString(parsed.title, "Antrenman planı"),
    description: asString(parsed.description, ""),
    daysPerWeek: Math.min(7, Math.max(1, asInt(parsed.daysPerWeek, 3))),
    exercises: exercises.map(normalizeExercise),
  };
}

function normalizeDiet(parsed: Record<string, unknown>) {
  return {
    dailyCalories: Math.max(800, asInt(parsed.dailyCalories, 2000)),
    proteinG: Math.max(0, asInt(parsed.proteinG, 120)),
    carbsG: Math.max(0, asInt(parsed.carbsG, 200)),
    fatG: Math.max(0, asInt(parsed.fatG, 60)),
  };
}

function normalizeMeal(parsed: Record<string, unknown>) {
  return {
    mealName: asString(parsed.mealName, "Öğün"),
    calories: Math.max(0, asInt(parsed.calories, 0)),
    proteinG: Math.max(0, asNumber(parsed.proteinG, 0)),
    carbsG: Math.max(0, asNumber(parsed.carbsG, 0)),
    fatG: Math.max(0, asNumber(parsed.fatG, 0)),
  };
}

function trialStatus(startedAt: Date, hasSubscription = false) {
  const expiresAt = new Date(startedAt);
  expiresAt.setDate(expiresAt.getDate() + TRIAL_DAYS);
  const now = new Date();
  const msRemaining = expiresAt.getTime() - now.getTime();
  const daysRemaining = Math.max(0, Math.ceil(msRemaining / (1000 * 60 * 60 * 24)));
  const isActive = msRemaining > 0 || hasSubscription;
  return {
    trialStartedAt: startedAt.toISOString(),
    trialExpiresAt: expiresAt.toISOString(),
    daysRemaining,
    isActive,
    hasSubscription,
  };
}

export const generateWorkoutPlan = onCall(geminiCallable, async (request) => {
  requireAuth(request);
  const profile = request.data as UserProfilePayload;
  const prompt =
    "Generate a personalized workout plan as JSON. " +
    "User: " + JSON.stringify(profile) + ". " +
    "Use Turkish exercise names where appropriate. Keep daysPerWeek between 3 and 6.";
  const parsed = await callGemini(
    geminiApiKey.value(),
    GEMINI_PLAN_MODEL,
    [{ text: prompt }],
    workoutSchema,
  );
  return normalizeWorkout((parsed ?? {}) as Record<string, unknown>);
});

export const generateDietPlan = onCall(geminiCallable, async (request) => {
  requireAuth(request);
  const profile = request.data as UserProfilePayload;
  const prompt =
    "Generate a daily diet macro plan as JSON for this user: " +
    JSON.stringify(profile) +
    ". Respect diet restrictions.";
  const parsed = await callGemini(
    geminiApiKey.value(),
    GEMINI_PLAN_MODEL,
    [{ text: prompt }],
    dietSchema,
  );
  return normalizeDiet((parsed ?? {}) as Record<string, unknown>);
});

export const analyzeMealPhoto = onCall(geminiCallable, async (request) => {
  requireAuth(request);
  const { imageBase64, mimeType, locale } = request.data as {
    imageBase64: string;
    mimeType: string;
    locale?: string;
  };
  if (!imageBase64) {
    throw new HttpsError("invalid-argument", "imageBase64 is required");
  }
  const language = parseLocale(locale) === "en" ? "English" : "Turkish";
  const prompt = [
    "Analyze this meal photo and return JSON with the nutrition of everything on the plate.",
    "Recognize Turkish cuisine accurately (e.g. mercimek çorbası, kuru fasulye, pilav, bulgur pilavı, " +
      "mantı, lahmacun, pide, döner, köfte, menemen, karnıyarık, imam bayıldı, dolma, sarma, " +
      "zeytinyağlı sebze, cacık, ayran, simit, börek, baklava) as well as international dishes.",
    "Estimate realistic portions from visual cues (plate or bowl size, cutlery, bread slices, depth of food) " +
      "using typical Turkish home or esnaf lokantası servings; do not assume tiny or oversized portions.",
    "Account for hidden calories typical of the dish: cooking oil or butter, sauces, " +
      "and side items such as bread, rice or yogurt that are visible.",
    "calories must be consistent with the macros (protein 4, carbs 4, fat 9 kcal per gram).",
    `mealName: a short, natural ${language} name for the whole meal (combine items, e.g. "Kuru fasulye ve pilav").`,
  ].join("\n");
  const parsed = await callGemini(
    geminiApiKey.value(),
    GEMINI_VISION_MODEL,
    [
      { inline_data: { mime_type: mimeType || "image/jpeg", data: imageBase64 } },
      { text: prompt },
    ],
    mealSchema,
  );
  return normalizeMeal((parsed ?? {}) as Record<string, unknown>);
});

export const suggestNextMeal = onCall(geminiCallable, async (request) => {
  const uid = requireAuth(request);
  const input = parseNextMealRequest(request.data);
  return withDailyQuota(uid, "suggestNextMeal", async () => {
    const parsed = await callGemini(
      geminiApiKey.value(),
      GEMINI_VISION_MODEL,
      [{ text: nextMealPrompt(input) }],
      nextMealSchema,
    );
    return normalizeNextMeal(parsed, input.locale);
  });
});

export const adaptTodaySession = onCall(geminiCallable, async (request) => {
  const uid = requireAuth(request);
  const input = parseTodaySessionRequest(request.data);
  return withDailyQuota(uid, "adaptTodaySession", async () => {
    const parsed = await callGemini(
      geminiApiKey.value(),
      GEMINI_PLAN_MODEL,
      [{ text: todaySessionPrompt(input) }],
      adaptedSessionSchema,
    );
    return normalizeAdaptedSession(parsed, input);
  });
});

export const coachChat = onCall(geminiCallable, async (request) => {
  const uid = requireAuth(request);
  const input = parseCoachChatRequest(request.data);
  return withDailyQuota(uid, "coachChat", async () => {
    const parsed = await callGemini(
      geminiApiKey.value(),
      GEMINI_VISION_MODEL,
      [{ text: coachChatPrompt(input) }],
      coachReplySchema,
      coachSystemInstruction(input.locale),
    );
    return normalizeCoachReply(parsed, input.locale);
  });
});

export const weeklyCoachReport = onCall(geminiCallable, async (request) => {
  const uid = requireAuth(request);
  const input = parseWeeklyReportRequest(request.data);
  return withDailyQuota(uid, "weeklyCoachReport", async () => {
    const parsed = await callGemini(
      geminiApiKey.value(),
      GEMINI_VISION_MODEL,
      [{ text: weeklyReportPrompt(input) }],
      weeklyReportSchema,
    );
    return normalizeWeeklyReport(parsed, input);
  });
});

export const initGuestTrial = onCall(authCallable, async (request) => {
  const uid = requireAuth(request);
  const ref = admin.firestore().collection("guest_trials").doc(uid);
  const existing = await ref.get();
  if (existing.exists) {
    const data = existing.data()!;
    return trialStatus(new Date(data.trialStartedAt), data.hasSubscription ?? false);
  }
  const startedAt = new Date();
  const status = trialStatus(startedAt);
  await ref.set(status);
  return status;
});

export const getTrialStatus = onCall(authCallable, async (request) => {
  const uid = requireAuth(request);
  const ref = admin.firestore().collection("guest_trials").doc(uid);
  const doc = await ref.get();
  if (!doc.exists) {
    const startedAt = new Date();
    const status = trialStatus(startedAt);
    await ref.set(status);
    return status;
  }
  const data = doc.data()!;
  return trialStatus(new Date(data.trialStartedAt), data.hasSubscription ?? false);
});
