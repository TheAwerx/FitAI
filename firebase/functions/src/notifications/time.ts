/**
 * Wall-clock math in IANA time zones without extra dependencies (Node ships full ICU).
 * Local dates are `yyyy-MM-dd` strings, like `localDate` in the app's documents.
 */

const MINUTE_MS = 60_000;
const DAY_MS = 24 * 60 * MINUTE_MS;

export interface LocalParts {
  /** `yyyy-MM-dd` */
  date: string;
  /** Minutes after local midnight, 0..1439. */
  minute: number;
  /** ISO weekday, 1 = Monday … 7 = Sunday. */
  isoWeekday: number;
}

const formatters = new Map<string, Intl.DateTimeFormat>();

function formatter(timeZone: string): Intl.DateTimeFormat {
  let f = formatters.get(timeZone);
  if (!f) {
    f = new Intl.DateTimeFormat("en-US", {
      timeZone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    });
    formatters.set(timeZone, f);
  }
  return f;
}

export function isValidTimeZone(timeZone: unknown): timeZone is string {
  if (typeof timeZone !== "string" || timeZone.length === 0 || timeZone.length > 64) return false;
  try {
    formatter(timeZone);
    return true;
  } catch {
    return false;
  }
}

/** Falls back to UTC for a missing or unknown zone. */
export function safeTimeZone(timeZone: unknown): string {
  return isValidTimeZone(timeZone) ? timeZone : "UTC";
}

function wallClock(ms: number, timeZone: string) {
  const parts: Record<string, number> = {};
  for (const p of formatter(timeZone).formatToParts(new Date(ms))) {
    if (p.type !== "literal") parts[p.type] = Number(p.value);
  }
  return parts as { year: number; month: number; day: number; hour: number; minute: number; second: number };
}

export function localParts(ms: number, timeZone: string): LocalParts {
  const w = wallClock(ms, timeZone);
  const date = formatDate(w.year, w.month, w.day);
  return { date, minute: w.hour * 60 + w.minute, isoWeekday: isoWeekday(date) };
}

/** Offset of [timeZone] from UTC at instant [ms], in ms (Istanbul → +3 h). */
function offsetMs(ms: number, timeZone: string): number {
  const w = wallClock(ms, timeZone);
  const asUtc = Date.UTC(w.year, w.month - 1, w.day, w.hour, w.minute, w.second);
  return asUtc - Math.floor(ms / 1000) * 1000;
}

/**
 * The instant at [minute] after local midnight on [date]. Wall times inside a DST jump (they don't
 * exist, or exist twice) resolve to a valid instant within an hour; nudge windows avoid those hours.
 */
export function zonedTimeToUtc(date: string, minute: number, timeZone: string): number {
  const [y, m, d] = date.split("-").map(Number);
  const guess = Date.UTC(y, m - 1, d, 0, 0) + minute * MINUTE_MS;
  const first = guess - offsetMs(guess, timeZone);
  const second = guess - offsetMs(first, timeZone);
  return Math.min(first, second);
}

export function formatDate(year: number, month: number, day: number): string {
  return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

export function isLocalDate(value: unknown): value is string {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

function dateToUtcMs(date: string): number {
  const [y, m, d] = date.split("-").map(Number);
  return Date.UTC(y, m - 1, d);
}

export function addDays(date: string, days: number): string {
  const t = new Date(dateToUtcMs(date) + days * DAY_MS);
  return formatDate(t.getUTCFullYear(), t.getUTCMonth() + 1, t.getUTCDate());
}

/** Whole days from [from] to [to]; positive when [to] is later. */
export function daysBetween(from: string, to: string): number {
  return Math.round((dateToUtcMs(to) - dateToUtcMs(from)) / DAY_MS);
}

export function isoWeekday(date: string): number {
  const day = new Date(dateToUtcMs(date)).getUTCDay();
  return day === 0 ? 7 : day;
}

/** ISO week key such as `2026-W46`, same as the app's `isoWeekKey`. */
export function isoWeekKey(date: string): string {
  const thursday = addDays(date, 4 - isoWeekday(date));
  const year = Number(thursday.slice(0, 4));
  const week = Math.floor(daysBetween(formatDate(year, 1, 1), thursday) / 7) + 1;
  return `${year}-W${String(week).padStart(2, "0")}`;
}

/** Local date and minute of an ISO instant in [timeZone]; null when it can't be parsed. */
export function localPartsOfIso(iso: unknown, timeZone: string): LocalParts | null {
  if (typeof iso !== "string") return null;
  const ms = Date.parse(iso);
  return Number.isFinite(ms) ? localParts(ms, timeZone) : null;
}
