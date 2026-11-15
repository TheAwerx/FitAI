"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.exerciseSchema = void 0;
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
exports.normalizeExercise = normalizeExercise;
const https_1 = require("firebase-functions/v2/https");
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
function parseProfile(value) {
    const p = requireRecord(value, "profile");
    const text = (v) => truncate(asString(v, "").trim(), 200);
    return {
        name: text(p.name),
        age: clamp(asInt(p.age, 30), 10, 100),
        heightCm: clamp(asNumber(p.heightCm, 170), 100, 250),
        weightKg: clamp(asNumber(p.weightKg, 70), 25, 350),
        goal: text(p.goal),
        activityLevel: text(p.activityLevel),
        equipment: text(p.equipment),
        dietRestrictions: text(p.dietRestrictions),
    };
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