"use strict";
/**
 * Wall-clock math in IANA time zones without extra dependencies (Node ships full ICU).
 * Local dates are `yyyy-MM-dd` strings, like `localDate` in the app's documents.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.isValidTimeZone = isValidTimeZone;
exports.safeTimeZone = safeTimeZone;
exports.localParts = localParts;
exports.zonedTimeToUtc = zonedTimeToUtc;
exports.formatDate = formatDate;
exports.isLocalDate = isLocalDate;
exports.addDays = addDays;
exports.daysBetween = daysBetween;
exports.isoWeekday = isoWeekday;
exports.isoWeekKey = isoWeekKey;
exports.localPartsOfIso = localPartsOfIso;
const MINUTE_MS = 60000;
const DAY_MS = 24 * 60 * MINUTE_MS;
const formatters = new Map();
function formatter(timeZone) {
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
function isValidTimeZone(timeZone) {
    if (typeof timeZone !== "string" || timeZone.length === 0 || timeZone.length > 64)
        return false;
    try {
        formatter(timeZone);
        return true;
    }
    catch (_a) {
        return false;
    }
}
/** Falls back to UTC for a missing or unknown zone. */
function safeTimeZone(timeZone) {
    return isValidTimeZone(timeZone) ? timeZone : "UTC";
}
function wallClock(ms, timeZone) {
    const parts = {};
    for (const p of formatter(timeZone).formatToParts(new Date(ms))) {
        if (p.type !== "literal")
            parts[p.type] = Number(p.value);
    }
    return parts;
}
function localParts(ms, timeZone) {
    const w = wallClock(ms, timeZone);
    const date = formatDate(w.year, w.month, w.day);
    return { date, minute: w.hour * 60 + w.minute, isoWeekday: isoWeekday(date) };
}
/** Offset of [timeZone] from UTC at instant [ms], in ms (Istanbul → +3 h). */
function offsetMs(ms, timeZone) {
    const w = wallClock(ms, timeZone);
    const asUtc = Date.UTC(w.year, w.month - 1, w.day, w.hour, w.minute, w.second);
    return asUtc - Math.floor(ms / 1000) * 1000;
}
/**
 * The instant at [minute] after local midnight on [date]. Wall times inside a DST jump (they don't
 * exist, or exist twice) resolve to a valid instant within an hour; nudge windows avoid those hours.
 */
function zonedTimeToUtc(date, minute, timeZone) {
    const [y, m, d] = date.split("-").map(Number);
    const guess = Date.UTC(y, m - 1, d, 0, 0) + minute * MINUTE_MS;
    const first = guess - offsetMs(guess, timeZone);
    const second = guess - offsetMs(first, timeZone);
    return Math.min(first, second);
}
function formatDate(year, month, day) {
    return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}
function isLocalDate(value) {
    return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
}
function dateToUtcMs(date) {
    const [y, m, d] = date.split("-").map(Number);
    return Date.UTC(y, m - 1, d);
}
function addDays(date, days) {
    const t = new Date(dateToUtcMs(date) + days * DAY_MS);
    return formatDate(t.getUTCFullYear(), t.getUTCMonth() + 1, t.getUTCDate());
}
/** Whole days from [from] to [to]; positive when [to] is later. */
function daysBetween(from, to) {
    return Math.round((dateToUtcMs(to) - dateToUtcMs(from)) / DAY_MS);
}
function isoWeekday(date) {
    const day = new Date(dateToUtcMs(date)).getUTCDay();
    return day === 0 ? 7 : day;
}
/** ISO week key such as `2026-W46`, same as the app's `isoWeekKey`. */
function isoWeekKey(date) {
    const thursday = addDays(date, 4 - isoWeekday(date));
    const year = Number(thursday.slice(0, 4));
    const week = Math.floor(daysBetween(formatDate(year, 1, 1), thursday) / 7) + 1;
    return `${year}-W${String(week).padStart(2, "0")}`;
}
/** Local date and minute of an ISO instant in [timeZone]; null when it can't be parsed. */
function localPartsOfIso(iso, timeZone) {
    if (typeof iso !== "string")
        return null;
    const ms = Date.parse(iso);
    return Number.isFinite(ms) ? localParts(ms, timeZone) : null;
}
//# sourceMappingURL=time.js.map