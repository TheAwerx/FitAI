"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.dietSchema = exports.workoutSchema = exports.TRAINING_DAY_PATTERNS = void 0;
exports.trainingDays = trainingDays;
exports.sessionMinutes = sessionMinutes;
exports.workoutPlanPrompt = workoutPlanPrompt;
exports.normalizeWorkoutPlan = normalizeWorkoutPlan;
exports.dietReference = dietReference;
exports.dietPlanPrompt = dietPlanPrompt;
exports.normalizeDietPlan = normalizeDietPlan;
const https_1 = require("firebase-functions/v2/https");
const common_1 = require("./common");
// Plan prompts for generateWorkoutPlan / generateDietPlan. Every onboarding answer the app sends is used:
// goal, gender, age, height, weight, activity, experience, obstacle, focus areas, days per week,
// session length, equipment and diet restrictions. Rule-based twin on the device: StarterPlans.kt.
const SAFETY_RULES = "Never give medical diagnoses or prescribe medication or supplements as treatment. " +
    "Never recommend extreme diets (very low calorie intake, multi-day fasting, dehydration, " +
    "crash cutting) or unprescribed drugs.";
function languageRule(locale) {
    return locale === "tr" ?
        "Write every user-facing text field (title, description, name, muscleGroup, notes) in Turkish. " +
            "Use exercise names common in Turkish gyms (e.g. \"Bench press\", \"Squat\", \"Barfiks\", \"Şınav\")." :
        "Write every user-facing text field (title, description, name, muscleGroup, notes) in English.";
}
const GOALS = {
    lose_fat: "lose body fat while keeping muscle",
    build_muscle: "build muscle (hypertrophy)",
    get_stronger: "get stronger (maximal strength)",
    improve_endurance: "improve endurance and conditioning",
    stay_healthy: "stay healthy and generally fit",
};
const ACTIVITY = {
    sedentary: { label: "sedentary (desk job, little walking)", factor: 1.2 },
    light: { label: "lightly active", factor: 1.375 },
    moderate: { label: "moderately active", factor: 1.55 },
    very: { label: "very active", factor: 1.725 },
    athlete: { label: "athlete-level activity", factor: 1.9 },
};
const EQUIPMENT = {
    bodyweight: "bodyweight only",
    dumbbells: "dumbbells",
    barbell: "barbell and plates",
    resistance_bands: "resistance bands",
    pull_up_bar: "pull-up bar",
    full_gym: "full commercial gym (machines, cables, barbells, dumbbells)",
};
const EXPERIENCE = {
    none: "has never trained with weights",
    beginner: "beginner (under 6 months of training)",
    intermediate: "intermediate (6 months to 2 years of consistent training)",
    advanced: "advanced (2+ years of consistent training)",
};
const OBSTACLE_RULES = {
    time: "Their biggest obstacle is time: favor compound lifts, supersets or circuits and short rests " +
        "(45-75 s), and never exceed the session length.",
    knowledge: "Their biggest obstacle is not knowing what to do: use well-known, simple exercises and give " +
        "every exercise a one-sentence technique cue in notes.",
    motivation: "Their biggest obstacle is motivation: keep sessions short and varied, open each day with an " +
        "easy win, and use notes for small, concrete targets.",
    injury: "They train around an injury or pain: prefer stable, joint-friendly variations (machines, " +
        "dumbbells, controlled tempo), no jumps or high-impact moves, no near-maximal loads (6+ reps), " +
        "and add a stop-if-it-hurts note to the first exercise of each day. Do not diagnose.",
    equipment: "Their biggest obstacle is limited equipment: use only the listed equipment and progress " +
        "bodyweight moves (tempo, pauses, harder variations).",
    never_tried: "They have never followed a program: treat them as a complete beginner (full-body days, " +
        "2-3 sets, technique notes).",
};
const FOCUS_RULES = {
    personal_plan: "a plan that clearly fits their answers",
    smart_overload: "progressive overload: give the first exercise of each day a progression rule in notes " +
        "(e.g. add 2.5-5% once every set reaches the top of the rep range)",
    plate_photo: "tracking meals with plate photos",
    log_lifts: "logging lifts: keep exercise names specific and stable so progress is comparable",
    streak: "consistency: make every session realistic to finish",
    ai_coach: "coaching: notes may include short, practical cues",
};
const DIET = {
    none: "no restrictions",
    vegetarian: "vegetarian",
    vegan: "vegan",
    pescatarian: "pescatarian",
    halal: "halal",
    kosher: "kosher",
    "gluten-free": "gluten-free",
    "dairy-free": "dairy-free",
};
/** Training days per week (ISO weekday, 1 = Monday), same spread as StarterPlans.TRAINING_DAYS. */
exports.TRAINING_DAY_PATTERNS = {
    2: [1, 4],
    3: [1, 3, 5],
    4: [1, 2, 4, 5],
    5: [1, 2, 3, 5, 6],
    6: [1, 2, 3, 4, 5, 6],
};
const DEFAULT_SESSION_MINUTES = 45;
function trainingDays(profile) {
    if (profile.daysPerWeek != null)
        return (0, common_1.clamp)(profile.daysPerWeek, 2, 6);
    switch (profile.activityLevel) {
        case "moderate": return 4;
        case "very":
        case "athlete": return 5;
        default: return 3;
    }
}
function sessionMinutes(profile) {
    var _a;
    return (_a = profile.sessionMinutes) !== null && _a !== void 0 ? _a : DEFAULT_SESSION_MINUTES;
}
function exercisesPerSession(minutes) {
    if (minutes <= 15)
        return 3;
    if (minutes <= 30)
        return 4;
    if (minutes <= 45)
        return 5;
    if (minutes <= 60)
        return 6;
    return 7;
}
function bmi(profile) {
    const m = profile.heightCm / 100;
    return Math.round((profile.weightKg / (m * m)) * 10) / 10;
}
function labels(codes, table) {
    return codes.map((code) => table[code]).filter((label) => !!label);
}
/** Human-readable facts for the prompt; the name is left out on purpose. */
function profileFacts(profile) {
    var _a, _b, _c, _d, _e, _f, _g, _h;
    const equipment = labels((0, common_1.splitCodes)(profile.equipment), EQUIPMENT);
    const diet = labels((0, common_1.splitCodes)(profile.dietRestrictions), DIET);
    const focus = labels((_a = profile.focusAreas) !== null && _a !== void 0 ? _a : [], FOCUS_RULES);
    return [
        `- Goal: ${(_b = GOALS[profile.goal]) !== null && _b !== void 0 ? _b : GOALS.stay_healthy}.`,
        `- Gender: ${(_c = profile.gender) !== null && _c !== void 0 ? _c : "not given"}. Age: ${profile.age}. Height: ${profile.heightCm} cm. ` +
            `Weight: ${profile.weightKg} kg (BMI ${bmi(profile)}).`,
        `- Daily activity outside training: ${(_e = (_d = ACTIVITY[profile.activityLevel]) === null || _d === void 0 ? void 0 : _d.label) !== null && _e !== void 0 ? _e : ACTIVITY.moderate.label}.`,
        `- Training experience: ${(_g = EXPERIENCE[(_f = profile.experience) !== null && _f !== void 0 ? _f : ""]) !== null && _g !== void 0 ? _g : "not given (assume beginner)"}.`,
        `- Training days per week: ${trainingDays(profile)}. Session length: about ${sessionMinutes(profile)} minutes.`,
        `- Equipment: ${equipment.length ? equipment.join(", ") : EQUIPMENT.bodyweight}.`,
        `- Biggest obstacle: ${(_h = profile.obstacle) !== null && _h !== void 0 ? _h : "not given"}.`,
        `- Wants FitAI for: ${focus.length ? focus.join("; ") : "a personal plan"}.`,
        `- Diet: ${diet.length ? diet.join(", ") : DIET.none}.`,
    ];
}
// generateWorkoutPlan
exports.workoutSchema = {
    type: "OBJECT",
    properties: {
        title: { type: "STRING" },
        description: { type: "STRING" },
        daysPerWeek: { type: "INTEGER" },
        exercises: { type: "ARRAY", items: common_1.exerciseSchema },
    },
    required: ["title", "description", "daysPerWeek", "exercises"],
};
function workoutPlanPrompt(profile, locale) {
    const days = trainingDays(profile);
    const minutes = sessionMinutes(profile);
    const pattern = exports.TRAINING_DAY_PATTERNS[days];
    const novice = profile.experience == null || profile.experience === "none" ||
        profile.experience === "beginner" || profile.obstacle === "never_tried";
    const equipment = (0, common_1.splitCodes)(profile.equipment);
    const lowImpact = profile.age >= 50 || bmi(profile) >= 30 || profile.obstacle === "injury";
    return [
        "You are a certified strength coach. Build this user's weekly workout plan as JSON.",
        "User:",
        ...profileFacts(profile),
        "Rules:",
        `- Exactly ${days} training days. Use these dayOfWeek values (1 = Monday): ${pattern.join(", ")}. ` +
            `Set daysPerWeek to ${days}. Rest days have no exercises.`,
        `- Each session must fit in ${minutes} minutes: about 5 min warm-up, about 45 s per set plus restSeconds. ` +
            `That is about ${exercisesPerSession(minutes)} exercises per day.`,
        "- Split: 2 days full body; 3 days full body for beginners, push/pull/legs otherwise; 4 days upper/lower; " +
            "5 days push/pull/legs plus upper/lower; 6 days push/pull/legs twice.",
        "- Sets, reps and rest by goal: get stronger 4-6 reps and 120-180 s on main lifts; build muscle 8-12 reps " +
            "and 60-90 s; lose fat or endurance 12-15 reps and 45-60 s; stay healthy 10-12 reps and 60-75 s.",
        novice ?
            "- Beginner volume: 2-3 sets per exercise, simple and stable movements, main compound lift first." :
            profile.experience === "advanced" ?
                "- Advanced volume: 4-5 sets on main lifts, 3-4 on accessories; harder variations are fine." :
                "- Intermediate volume: 3-4 sets per exercise.",
        equipment.includes("full_gym") ?
            "- Any gym equipment is available." :
            "- Use only the listed equipment (bodyweight is always available); never prescribe a machine, " +
                "cable or barbell the user doesn't have.",
        lowImpact ?
            "- Prefer low-impact options: no jumping, no running drills, controlled tempo." :
            "- Impact work is fine when it serves the goal.",
        profile.obstacle && OBSTACLE_RULES[profile.obstacle] ? "- " + OBSTACLE_RULES[profile.obstacle] : "",
        profile.goal === "lose_fat" || profile.goal === "improve_endurance" ?
            "- Finish each day with a short conditioning finisher (as the last exercise, reps like \"5 dk\" / \"5 min\")." :
            "",
        "- reps is a string like \"8-12\" or \"30 sn\". muscleGroup is one short word (e.g. Chest / Göğüs).",
        "- notes are optional, at most one sentence; use them for the cues the rules above ask for.",
        "- title: short (max 6 words), mentions the goal. description: 1-2 sentences saying how the plan fits " +
            "their days, session length, equipment and obstacle.",
        "- " + SAFETY_RULES,
        "- " + languageRule(locale),
    ].filter((line) => line.length > 0).join("\n");
}
function normalizeWorkoutPlan(parsed, profile, locale) {
    const p = (0, common_1.asRecord)(parsed);
    const exercises = (Array.isArray(p.exercises) ? p.exercises : [])
        .map(common_1.normalizeExercise)
        .filter((exercise) => exercise.name.trim().length > 0);
    if (exercises.length === 0) {
        throw new https_1.HttpsError("internal", "Gemini returned an empty workout plan");
    }
    const distinctDays = new Set(exercises.map((exercise) => exercise.dayOfWeek)).size;
    const days = trainingDays(profile);
    return {
        title: (0, common_1.asString)(p.title, locale === "tr" ? "Kişisel antrenman planı" : "Personal workout plan"),
        description: (0, common_1.asString)(p.description, ""),
        // The exercises are the truth: the app shows one training day per distinct dayOfWeek.
        daysPerWeek: (0, common_1.clamp)(distinctDays || days, 1, 7),
        exercises,
    };
}
// generateDietPlan
exports.dietSchema = {
    type: "OBJECT",
    properties: {
        dailyCalories: { type: "INTEGER" },
        proteinG: { type: "INTEGER" },
        carbsG: { type: "INTEGER" },
        fatG: { type: "INTEGER" },
    },
    required: ["dailyCalories", "proteinG", "carbsG", "fatG"],
};
const GOAL_CALORIE_DELTA = {
    lose_fat: -450,
    build_muscle: 300,
    get_stronger: 200,
};
/** Mifflin-St Jeor, same formula as StarterPlans.diet on the device; the AI answer is kept close to it. */
function dietReference(profile) {
    var _a, _b, _c;
    const genderOffset = profile.gender === "female" ? -161 : 5;
    const bmr = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + genderOffset;
    const factor = (_b = (_a = ACTIVITY[profile.activityLevel]) === null || _a === void 0 ? void 0 : _a.factor) !== null && _b !== void 0 ? _b : ACTIVITY.moderate.factor;
    const tdee = bmr * factor;
    const minCalories = profile.gender === "male" ? 1500 : 1200;
    const calories = Math.max(minCalories, Math.round(tdee + ((_c = GOAL_CALORIE_DELTA[profile.goal]) !== null && _c !== void 0 ? _c : 0)));
    const vegan = (0, common_1.splitCodes)(profile.dietRestrictions).includes("vegan");
    const perKg = ["build_muscle", "get_stronger", "lose_fat"].includes(profile.goal) ? 2.0 : 1.6;
    const proteinG = Math.round(profile.weightKg * perKg * (vegan ? 1.1 : 1));
    const fatG = Math.round((calories * 0.25) / 9);
    const carbsG = Math.max(0, Math.round((calories - proteinG * 4 - fatG * 9) / 4));
    return { bmr: Math.round(bmr), tdee: Math.round(tdee), calories, proteinG, carbsG, fatG, minCalories };
}
function dietPlanPrompt(profile, reference) {
    const days = trainingDays(profile);
    const minutes = sessionMinutes(profile);
    return [
        "You are a sports nutritionist. Set this user's daily calorie and macro targets as JSON.",
        "User:",
        ...profileFacts(profile),
        `Reference (Mifflin-St Jeor): BMR ${reference.bmr} kcal, maintenance ${reference.tdee} kcal, ` +
            `goal target ${reference.calories} kcal, protein ${reference.proteinG} g, carbs ${reference.carbsG} g, ` +
            `fat ${reference.fatG} g.`,
        "Rules:",
        "- Start from the reference and adjust only for what it misses: " +
            `${days} training days a week of about ${minutes} minutes, the experience level, and the diet.`,
        "- Stay within 10% of the reference calories and never below " + reference.minCalories + " kcal.",
        "- Protein: 1.6-2.2 g per kg body weight; the high end for fat loss, muscle or strength goals. " +
            "Vegan diets need about 10% more protein from plant sources.",
        "- Fat at least 20% of calories. Carbohydrates fill the rest; more for endurance or many training days.",
        "- The targets must be reachable with the diet restrictions (e.g. vegan, gluten-free, halal).",
        "- calories must match the macros: protein 4, carbs 4, fat 9 kcal per gram (within 5%).",
        "- " + SAFETY_RULES,
    ].join("\n");
}
/**
 * Keeps the AI answer near [reference]: calories within 20%, protein within -20%/+25% and at most 45% of
 * calories, fat 20-40% of calories. Carbs are recomputed when the macros don't add up to the calories.
 */
function normalizeDietPlan(parsed, reference) {
    const p = (0, common_1.asRecord)(parsed);
    const calories = (0, common_1.clamp)((0, common_1.asInt)(p.dailyCalories, reference.calories), Math.max(reference.minCalories, Math.round(reference.calories * 0.8)), Math.round(reference.calories * 1.2));
    const proteinG = Math.min((0, common_1.clamp)((0, common_1.asInt)(p.proteinG, reference.proteinG), Math.round(reference.proteinG * 0.8), Math.round(reference.proteinG * 1.25)), Math.round((calories * 0.45) / 4));
    const fatG = (0, common_1.clamp)((0, common_1.asInt)(p.fatG, reference.fatG), Math.round((calories * 0.2) / 9), Math.round((calories * 0.4) / 9));
    let carbsG = Math.max(0, (0, common_1.asInt)(p.carbsG, reference.carbsG));
    const fromMacros = proteinG * 4 + carbsG * 4 + fatG * 9;
    if (Math.abs(fromMacros - calories) > calories * 0.05) {
        carbsG = Math.max(0, Math.round((calories - proteinG * 4 - fatG * 9) / 4));
    }
    return { dailyCalories: calories, proteinG, carbsG, fatG };
}
//# sourceMappingURL=plans.js.map