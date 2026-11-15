"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.exerciseSchema = exports.MAX_FOCUS_AREAS = exports.FOCUS_AREAS = exports.OBSTACLES = exports.EXPERIENCES = exports.GENDERS = void 0;
exports.asInt = asInt;
exports.asNumber = asNumber;
exports.asString = asString;
exports.clamp = clamp;
exports.round1 = round1;
exports.truncate = truncate;
exports.asRecord = asRecord;
exports.asStringArray = asStringArray;
exports.invalid = invalid;
exports.requireRecord = requireRecord;
exports.requireNumber = requireNumber;
exports.parseLocale = parseLocale;
exports.parseProfile = parseProfile;
exports.splitCodes = splitCodes;
exports.normalizeExercise = normalizeExercise;
const https_1 = require("firebase-functions/v2/https");
// Onboarding codes, same as core/domain/.../model/ProfileEnums.kt and OnboardingAnswers.kt.
exports.GENDERS = ["male", "female"];
exports.EXPERIENCES = ["none", "beginner", "intermediate", "advanced"];
exports.OBSTACLES = ["time", "knowledge", "motivation", "injury", "equipment", "never_tried"];
exports.FOCUS_AREAS = [
    "personal_plan", "smart_overload", "plate_photo", "log_lifts", "streak", "ai_coach",
];
exports.MAX_FOCUS_AREAS = 3;
exports.exerciseSchema = {
    type: "OBJECT",
    properties: {
        dayOfWeek: { type: "INTEGER" },
        name: { type: "STRING" },
        sets: { type: "INTEGER" },
        reps: { type: "STRING" },
        restSeconds: { type: "INTEGER" },
        muscleGroup: { type: "STRING" },
        notes: { type: "STRING" },
    },
    required: ["dayOfWeek", "name", "sets", "reps", "restSeconds", "muscleGroup"],
};
function asInt(value, fallback) {
    const n = Number(value);
    return Number.isFinite(n) ? Math.round(n) : fallback;
}
function asNumber(value, fallback) {
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
}
function asString(value, fallback) {
    return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}
function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
}
function round1(value) {
    return Math.round(value * 10) / 10;
}
function truncate(value, maxLength) {
    return value.length > maxLength ? value.slice(0, maxLength) : value;
}
function asRecord(value) {
    return value != null && typeof value === "object" && !Array.isArray(value) ?
        value :
        {};
}
function asStringArray(value, maxItems, maxLength) {
    if (!Array.isArray(value))
        return [];
    return value
        .filter((v) => typeof v === "string" && v.trim().length > 0)
        .map((v) => truncate(v.trim(), maxLength))
        .slice(0, maxItems);
}
function invalid(message) {
    throw new https_1.HttpsError("invalid-argument", message);
}
function requireRecord(value, field) {
    if (value == null || typeof value !== "object" || Array.isArray(value)) {
        invalid(`${field} must be an object`);
    }
    return value;
}
function requireNumber(value, field) {
    if (typeof value !== "number" && typeof value !== "string") {
        invalid(`${field} must be a number`);
    }
    const n = Number(value);
    if (!Number.isFinite(n))
        invalid(`${field} must be a number`);
    return n;
}
function parseLocale(value) {
    return typeof value === "string" && value.toLowerCase().startsWith("en") ? "en" : "tr";
}
function pickCode(value, allowed) {
    return typeof value === "string" && allowed.includes(value) ? value : undefined;
}
function parseProfile(value) {
    const p = requireRecord(value, "profile");
    const text = (v) => truncate(asString(v, "").trim(), 200);
    const profile = {
        name: text(p.name),
        age: clamp(asInt(p.age, 30), 10, 100),
        heightCm: clamp(asNumber(p.heightCm, 170), 100, 250),
        weightKg: clamp(asNumber(p.weightKg, 70), 25, 350),
        goal: text(p.goal),
        activityLevel: text(p.activityLevel),
        equipment: text(p.equipment),
        dietRestrictions: text(p.dietRestrictions),
    };
    const gender = pickCode(p.gender, exports.GENDERS);
    const experience = pickCode(p.experience, exports.EXPERIENCES);
    const obstacle = pickCode(p.obstacle, exports.OBSTACLES);
    const focusAreas = asStringArray(p.focusAreas, exports.FOCUS_AREAS.length, 40)
        .filter((code) => exports.FOCUS_AREAS.includes(code))
        .slice(0, exports.MAX_FOCUS_AREAS);
    if (gender)
        profile.gender = gender;
    if (experience)
        profile.experience = experience;
    if (obstacle)
        profile.obstacle = obstacle;
    if (focusAreas.length)
        profile.focusAreas = focusAreas;
    if (p.daysPerWeek != null)
        profile.daysPerWeek = clamp(asInt(p.daysPerWeek, 3), 1, 7);
    if (p.sessionMinutes != null)
        profile.sessionMinutes = clamp(asInt(p.sessionMinutes, 45), 10, 180);
    return profile;
}
/** Comma-joined codes from the app ("dumbbells,pull_up_bar") as a clean list. */
function splitCodes(value) {
    return value.split(",").map((code) => code.trim()).filter((code) => code.length > 0);
}
function normalizeExercise(item) {
    const e = asRecord(item);
    return {
        dayOfWeek: clamp(asInt(e.dayOfWeek, 1), 1, 7),
        name: asString(e.name, "Egzersiz"),
        sets: Math.max(1, asInt(e.sets, 3)),
        reps: asString(e.reps, "8-12"),
        restSeconds: Math.max(0, asInt(e.restSeconds, 90)),
        muscleGroup: asString(e.muscleGroup, ""),
        notes: e.notes == null ? null : asString(e.notes, ""),
    };
}
//# sourceMappingURL=common.js.map