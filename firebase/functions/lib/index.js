"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.getTrialStatus = exports.initGuestTrial = exports.weeklyCoachReport = exports.coachChat = exports.adaptTodaySession = exports.suggestNextMeal = exports.analyzeMealPhoto = exports.generateDietPlan = exports.generateWorkoutPlan = void 0;
const admin = __importStar(require("firebase-admin"));
const https_1 = require("firebase-functions/v2/https");
const params_1 = require("firebase-functions/params");
const common_1 = require("./common");
const coach_1 = require("./coach");
const rateLimit_1 = require("./rateLimit");
const planQuota_1 = require("./planQuota");
admin.initializeApp();
const geminiApiKey = (0, params_1.defineSecret)("GEMINI_API_KEY");
const TRIAL_DAYS = 3;
const GEMINI_PLAN_MODEL = "gemini-3.1-pro-preview";
const GEMINI_VISION_MODEL = "gemini-3.5-flash";
const FUNCTIONS_REGION = "us-central1";
const geminiCallable = {
    region: FUNCTIONS_REGION,
    timeoutSeconds: 120,
    memory: "512MiB",
    cors: true,
    invoker: "public",
    secrets: [geminiApiKey],
};
const authCallable = {
    region: FUNCTIONS_REGION,
    cors: true,
    invoker: "public",
};
const workoutSchema = {
    type: "OBJECT",
    properties: {
        title: { type: "STRING" },
        description: { type: "STRING" },
        daysPerWeek: { type: "INTEGER" },
        exercises: { type: "ARRAY", items: common_1.exerciseSchema },
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
async function callGemini(apiKey, model, parts, responseSchema, systemInstruction) {
    var _a, _b, _c, _d, _e;
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`;
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": apiKey,
        },
        body: JSON.stringify(Object.assign(Object.assign({}, (systemInstruction ? { systemInstruction: { parts: [{ text: systemInstruction }] } } : {})), { contents: [{ role: "user", parts }], generationConfig: {
                responseMimeType: "application/json",
                responseSchema,
                temperature: 0.6,
                maxOutputTokens: 8192,
            } })),
    });
    const bodyText = await response.text();
    if (!response.ok) {
        throw new https_1.HttpsError("internal", `Gemini error ${response.status}: ${bodyText.slice(0, 400)}`);
    }
    const data = JSON.parse(bodyText);
    const text = (_e = (_d = (_c = (_b = (_a = data === null || data === void 0 ? void 0 : data.candidates) === null || _a === void 0 ? void 0 : _a[0]) === null || _b === void 0 ? void 0 : _b.content) === null || _c === void 0 ? void 0 : _c.parts) === null || _d === void 0 ? void 0 : _d[0]) === null || _e === void 0 ? void 0 : _e.text;
    if (!text)
        throw new https_1.HttpsError("internal", "Empty Gemini response");
    return parseJson(text);
}
function parseJson(raw) {
    const trimmed = raw.trim().replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "");
    try {
        return JSON.parse(trimmed);
    }
    catch (_a) {
        throw new https_1.HttpsError("internal", "Gemini returned invalid JSON");
    }
}
function requireAuth(request) {
    var _a;
    if (!((_a = request.auth) === null || _a === void 0 ? void 0 : _a.uid)) {
        throw new https_1.HttpsError("unauthenticated", "Authentication required");
    }
    return request.auth.uid;
}
function normalizeWorkout(parsed) {
    const exercises = Array.isArray(parsed.exercises) ? parsed.exercises : [];
    return {
        title: (0, common_1.asString)(parsed.title, "Antrenman planı"),
        description: (0, common_1.asString)(parsed.description, ""),
        daysPerWeek: Math.min(7, Math.max(1, (0, common_1.asInt)(parsed.daysPerWeek, 3))),
        exercises: exercises.map(common_1.normalizeExercise),
    };
}
function normalizeDiet(parsed) {
    return {
        dailyCalories: Math.max(800, (0, common_1.asInt)(parsed.dailyCalories, 2000)),
        proteinG: Math.max(0, (0, common_1.asInt)(parsed.proteinG, 120)),
        carbsG: Math.max(0, (0, common_1.asInt)(parsed.carbsG, 200)),
        fatG: Math.max(0, (0, common_1.asInt)(parsed.fatG, 60)),
    };
}
function normalizeMeal(parsed) {
    return {
        mealName: (0, common_1.asString)(parsed.mealName, "Öğün"),
        calories: Math.max(0, (0, common_1.asInt)(parsed.calories, 0)),
        proteinG: Math.max(0, (0, common_1.asNumber)(parsed.proteinG, 0)),
        carbsG: Math.max(0, (0, common_1.asNumber)(parsed.carbsG, 0)),
        fatG: Math.max(0, (0, common_1.asNumber)(parsed.fatG, 0)),
    };
}
function trialStatus(startedAt, hasSubscription = false) {
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
exports.generateWorkoutPlan = (0, https_1.onCall)(geminiCallable, (0, planQuota_1.withPlanQuota)(async (request) => {
    requireAuth(request);
    const profile = request.data;
    const prompt = "Generate a personalized workout plan as JSON. " +
        "User: " + JSON.stringify(profile) + ". " +
        "Use Turkish exercise names where appropriate. Keep daysPerWeek between 3 and 6.";
    const parsed = await callGemini(geminiApiKey.value(), GEMINI_PLAN_MODEL, [{ text: prompt }], workoutSchema);
    return normalizeWorkout((parsed !== null && parsed !== void 0 ? parsed : {}));
}));
exports.generateDietPlan = (0, https_1.onCall)(geminiCallable, async (request) => {
    requireAuth(request);
    const profile = request.data;
    const prompt = "Generate a daily diet macro plan as JSON for this user: " +
        JSON.stringify(profile) +
        ". Respect diet restrictions.";
    const parsed = await callGemini(geminiApiKey.value(), GEMINI_PLAN_MODEL, [{ text: prompt }], dietSchema);
    return normalizeDiet((parsed !== null && parsed !== void 0 ? parsed : {}));
});
exports.analyzeMealPhoto = (0, https_1.onCall)(geminiCallable, async (request) => {
    requireAuth(request);
    const { imageBase64, mimeType, locale } = request.data;
    if (!imageBase64) {
        throw new https_1.HttpsError("invalid-argument", "imageBase64 is required");
    }
    const language = (0, common_1.parseLocale)(locale) === "en" ? "English" : "Turkish";
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
    const parsed = await callGemini(geminiApiKey.value(), GEMINI_VISION_MODEL, [
        { inline_data: { mime_type: mimeType || "image/jpeg", data: imageBase64 } },
        { text: prompt },
    ], mealSchema);
    return normalizeMeal((parsed !== null && parsed !== void 0 ? parsed : {}));
});
exports.suggestNextMeal = (0, https_1.onCall)(geminiCallable, async (request) => {
    const uid = requireAuth(request);
    const input = (0, coach_1.parseNextMealRequest)(request.data);
    return (0, rateLimit_1.withDailyQuota)(uid, "suggestNextMeal", async () => {
        const parsed = await callGemini(geminiApiKey.value(), GEMINI_VISION_MODEL, [{ text: (0, coach_1.nextMealPrompt)(input) }], coach_1.nextMealSchema);
        return (0, coach_1.normalizeNextMeal)(parsed, input.locale);
    });
});
exports.adaptTodaySession = (0, https_1.onCall)(geminiCallable, async (request) => {
    const uid = requireAuth(request);
    const input = (0, coach_1.parseTodaySessionRequest)(request.data);
    return (0, rateLimit_1.withDailyQuota)(uid, "adaptTodaySession", async () => {
        const parsed = await callGemini(geminiApiKey.value(), GEMINI_PLAN_MODEL, [{ text: (0, coach_1.todaySessionPrompt)(input) }], coach_1.adaptedSessionSchema);
        return (0, coach_1.normalizeAdaptedSession)(parsed, input);
    });
});
exports.coachChat = (0, https_1.onCall)(geminiCallable, async (request) => {
    const uid = requireAuth(request);
    const input = (0, coach_1.parseCoachChatRequest)(request.data);
    return (0, rateLimit_1.withDailyQuota)(uid, "coachChat", async () => {
        const parsed = await callGemini(geminiApiKey.value(), GEMINI_VISION_MODEL, [{ text: (0, coach_1.coachChatPrompt)(input) }], coach_1.coachReplySchema, (0, coach_1.coachSystemInstruction)(input.locale));
        return (0, coach_1.normalizeCoachReply)(parsed, input.locale);
    });
});
exports.weeklyCoachReport = (0, https_1.onCall)(geminiCallable, async (request) => {
    const uid = requireAuth(request);
    const input = (0, coach_1.parseWeeklyReportRequest)(request.data);
    return (0, rateLimit_1.withDailyQuota)(uid, "weeklyCoachReport", async () => {
        const parsed = await callGemini(geminiApiKey.value(), GEMINI_VISION_MODEL, [{ text: (0, coach_1.weeklyReportPrompt)(input) }], coach_1.weeklyReportSchema);
        return (0, coach_1.normalizeWeeklyReport)(parsed, input);
    });
});
exports.initGuestTrial = (0, https_1.onCall)(authCallable, async (request) => {
    var _a;
    const uid = requireAuth(request);
    const ref = admin.firestore().collection("guest_trials").doc(uid);
    const existing = await ref.get();
    if (existing.exists) {
        const data = existing.data();
        return trialStatus(new Date(data.trialStartedAt), (_a = data.hasSubscription) !== null && _a !== void 0 ? _a : false);
    }
    const startedAt = new Date();
    const status = trialStatus(startedAt);
    await ref.set(status);
    return status;
});
exports.getTrialStatus = (0, https_1.onCall)(authCallable, async (request) => {
    var _a;
    const uid = requireAuth(request);
    const ref = admin.firestore().collection("guest_trials").doc(uid);
    const doc = await ref.get();
    if (!doc.exists) {
        const startedAt = new Date();
        const status = trialStatus(startedAt);
        await ref.set(status);
        return status;
    }
    const data = doc.data();
    return trialStatus(new Date(data.trialStartedAt), (_a = data.hasSubscription) !== null && _a !== void 0 ? _a : false);
});
//# sourceMappingURL=index.js.map