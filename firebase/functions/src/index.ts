import * as admin from "firebase-admin";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import {
  asInt,
  asNumber,
  asRecord,
  asString,
  parseLocale,
  parseProfile,
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
import {
  DIET_MAX_OUTPUT_TOKENS,
  dietPlanPrompt,
  dietReference,
  dietSchema,
  normalizeDietPlan,
  normalizeWorkoutPlan,
  workoutPlanPrompt,
  workoutSchema,
} from "./plans";
// Dynamic push notifications (src/notifications, firebase/SCHEMA.md "Push notifications").
// The handlers use admin lazily, so exporting before initializeApp() below is fine.
export {
  notificationSweep,
  notificationsOnUserWritten,
  notificationsOnDeviceWritten,
  notificationsOnPlanWritten,
  notificationsOnSessionWritten,
  notificationsOnMealCreated,
} from "./notifications";
import { withDailyQuota } from "./rateLimit";
import { withPlanQuota } from "./planQuota";

// One free trial per account and per device (trial.ts). `initGuestTrial` was removed with it.
export { getTrialStatus, startTrial } from "./trial";

admin.initializeApp();

const geminiApiKey = defineSecret("GEMINI_API_KEY");
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
  maxOutputTokens = 8192,
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
        maxOutputTokens,
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

function normalizeMeal(parsed: Record<string, unknown>) {
  return {
    mealName: asString(parsed.mealName, "Öğün"),
    calories: Math.max(0, asInt(parsed.calories, 0)),
    proteinG: Math.max(0, asNumber(parsed.proteinG, 0)),
    carbsG: Math.max(0, asNumber(parsed.carbsG, 0)),
    fatG: Math.max(0, asNumber(parsed.fatG, 0)),
  };
}

// Both plan callables take the app's UserProfilePayload (all onboarding answers) plus `locale`.
// generateWorkoutPlan arrives as `{ profile, planPurpose }`; withPlanQuota unwraps it to the bare profile.
export const generateWorkoutPlan = onCall(geminiCallable, withPlanQuota(async (request) => {
  requireAuth(request);
  const data = asRecord(request.data);
  const profile = parseProfile(data);
  const locale = parseLocale(data.locale);
  const parsed = await callGemini(
    geminiApiKey.value(),
    GEMINI_PLAN_MODEL,
    [{ text: workoutPlanPrompt(profile, locale) }],
    workoutSchema,
  );
  return normalizeWorkoutPlan(parsed, profile, locale);
}));

// The detailed diet (three options per meal) is a long answer; the app waits up to 240 s for it.
export const generateDietPlan = onCall({ ...geminiCallable, timeoutSeconds: 300 }, async (request) => {
  requireAuth(request);
  const profile = parseProfile(request.data);
  const locale = parseLocale(asRecord(request.data).locale);
  const reference = dietReference(profile);
  const parsed = await callGemini(
    geminiApiKey.value(),
    GEMINI_PLAN_MODEL,
    [{ text: dietPlanPrompt(profile, reference, locale) }],
    dietSchema,
    undefined,
    DIET_MAX_OUTPUT_TOKENS,
  );
  return normalizeDietPlan(parsed, reference, profile);
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
