import { HttpsError } from "firebase-functions/v2/https";

export type Locale = "tr" | "en";

export interface UserProfilePayload {
  name: string;
  age: number;
  heightCm: number;
  weightKg: number;
  goal: string;
  activityLevel: string;
  equipment: string;
  dietRestrictions: string;
}

export interface GeneratedExercise {
  dayOfWeek: number;
  name: string;
  sets: number;
  reps: string;
  restSeconds: number;
  muscleGroup: string;
  notes: string | null;
}

export const exerciseSchema = {
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

export function asInt(value: unknown, fallback: number): number {
  const n = Number(value);
  return Number.isFinite(n) ? Math.round(n) : fallback;
}

export function asNumber(value: unknown, fallback: number): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

export function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function round1(value: number): number {
  return Math.round(value * 10) / 10;
}

export function truncate(value: string, maxLength: number): string {
  return value.length > maxLength ? value.slice(0, maxLength) : value;
}

export function asRecord(value: unknown): Record<string, unknown> {
  return value != null && typeof value === "object" && !Array.isArray(value) ?
    value as Record<string, unknown> :
    {};
}

export function asStringArray(value: unknown, maxItems: number, maxLength: number): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .filter((v): v is string => typeof v === "string" && v.trim().length > 0)
    .map((v) => truncate(v.trim(), maxLength))
    .slice(0, maxItems);
}

export function invalid(message: string): never {
  throw new HttpsError("invalid-argument", message);
}

export function requireRecord(value: unknown, field: string): Record<string, unknown> {
  if (value == null || typeof value !== "object" || Array.isArray(value)) {
    invalid(`${field} must be an object`);
  }
  return value as Record<string, unknown>;
}

export function requireNumber(value: unknown, field: string): number {
  if (typeof value !== "number" && typeof value !== "string") {
    invalid(`${field} must be a number`);
  }
  const n = Number(value);
  if (!Number.isFinite(n)) invalid(`${field} must be a number`);
  return n;
}

export function parseLocale(value: unknown): Locale {
  return typeof value === "string" && value.toLowerCase().startsWith("en") ? "en" : "tr";
}

export function parseProfile(value: unknown): UserProfilePayload {
  const p = requireRecord(value, "profile");
  const text = (v: unknown) => truncate(asString(v, "").trim(), 200);
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

export function normalizeExercise(item: unknown): GeneratedExercise {
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
