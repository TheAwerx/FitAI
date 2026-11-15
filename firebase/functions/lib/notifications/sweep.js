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
exports.loadDayFacts = loadDayFacts;
exports.runNotificationSweep = runNotificationSweep;
const admin = __importStar(require("firebase-admin"));
const copy_1 = require("./copy");
const config_1 = require("./config");
const rules_1 = require("./rules");
const state_1 = require("./state");
const time_1 = require("./time");
const { Timestamp, FieldValue } = admin.firestore;
/** Token errors that mean "this installation is gone": the device document is deleted. */
const DEAD_TOKEN_CODES = new Set([
    "messaging/registration-token-not-registered",
    "messaging/invalid-registration-token",
]);
/**
 * Exact facts for [date] from today's documents. Two small queries on the automatic `localDate`
 * index; only run when the cheap state view already found something to send.
 */
async function loadDayFacts(db, uid, date, timeZone, nowMs) {
    var _a;
    const user = db.collection("users").doc(uid);
    const [sessions, meals] = await Promise.all([
        user.collection("workout_sessions").where("localDate", "==", date).get(),
        user.collection("meal_logs").where("localDate", "==", date).get(),
    ]);
    let workoutCompletedToday = false;
    let sessionInProgress = false;
    for (const s of sessions.docs) {
        if (s.get("completed") === true) {
            workoutCompletedToday = true;
            continue;
        }
        const started = Date.parse(String((_a = s.get("startedAt")) !== null && _a !== void 0 ? _a : ""));
        if (Number.isFinite(started) && started <= nowMs && nowMs - started < config_1.NOTIFICATION_CONFIG.sessionInProgressMinutes * 60000) {
            sessionInProgress = true;
        }
    }
    let latestMealMinute = null;
    for (const m of meals.docs) {
        const parts = (0, time_1.localPartsOfIso)(m.get("loggedAt"), timeZone);
        const minute = (parts === null || parts === void 0 ? void 0 : parts.date) === date ? parts.minute : 0;
        latestMealMinute = latestMealMinute == null ? minute : Math.max(latestMealMinute, minute);
    }
    return { workoutCompletedToday, sessionInProgress, mealLoggedToday: !meals.empty, latestMealMinute };
}
async function mapLimit(items, limit, run) {
    let index = 0;
    const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
        while (index < items.length) {
            const item = items[index++];
            await run(item);
        }
    });
    await Promise.all(workers);
}
/**
 * One pass over every account whose `nextCheckAt` is due. Per account:
 * 1. evaluate on the stored state (no extra reads);
 * 2. if something would be sent, confirm with today's sessions and meals;
 * 3. in a transaction, re-evaluate on the fresh state, record the nudge in the sent log and store the
 *    next check (at most once per kind per local day, even if two runs overlap);
 * 4. send to every device token and delete devices whose token is dead.
 */
async function runNotificationSweep(options) {
    var _a, _b, _c, _d, _e;
    const { db, messenger } = options;
    const nowMs = (_a = options.nowMs) !== null && _a !== void 0 ? _a : Date.now();
    const pageSize = (_b = options.pageSize) !== null && _b !== void 0 ? _b : 200;
    const concurrency = (_c = options.concurrency) !== null && _c !== void 0 ? _c : 16;
    const deadline = Date.now() + ((_d = options.budgetMs) !== null && _d !== void 0 ? _d : 7 * 60000);
    const log = (_e = options.log) !== null && _e !== void 0 ? _e : (() => undefined);
    const result = { checked: 0, sent: [], reasons: {}, invalidTokensRemoved: 0 };
    let cursor = null;
    for (;;) {
        let query = db.collection(state_1.STATE_COLLECTION)
            .where("nextCheckAt", "<=", Timestamp.fromMillis(nowMs))
            .orderBy("nextCheckAt")
            .limit(pageSize);
        if (cursor)
            query = query.startAfter(cursor);
        const page = await query.get();
        if (page.empty)
            break;
        await mapLimit(page.docs, concurrency, async (doc) => {
            var _a;
            result.checked++;
            try {
                await processAccount(db, messenger, doc.id, doc.data(), nowMs, result);
            }
            catch (error) {
                result.reasons.error = ((_a = result.reasons.error) !== null && _a !== void 0 ? _a : 0) + 1;
                log("notification check failed", { uid: doc.id, error: String(error) });
            }
        });
        cursor = page.docs[page.docs.length - 1];
        if (page.size < pageSize || Date.now() > deadline)
            break;
    }
    return result;
}
async function processAccount(db, messenger, uid, data, nowMs, result) {
    var _a, _b, _c, _d, _e, _f, _g;
    const first = (0, rules_1.evaluate)((0, state_1.stateFromDoc)(data), nowMs);
    let facts;
    if (first.send) {
        facts = await loadDayFacts(db, uid, first.localDate, (0, time_1.safeTimeZone)(data.timeZone), nowMs);
    }
    const ref = (0, state_1.stateRef)(db, uid);
    const decided = await db.runTransaction(async (tx) => {
        var _a;
        const snap = await tx.get(ref);
        if (!snap.exists)
            return null;
        const fresh = (0, state_1.stateFromDoc)(snap.data());
        const evaluation = (0, rules_1.evaluate)(fresh, nowMs, facts);
        const verified = evaluation.send != null && facts != null &&
            evaluation.localDate === first.localDate && evaluation.send.kind === ((_a = first.send) === null || _a === void 0 ? void 0 : _a.kind);
        if (evaluation.send && !verified) {
            // Something changed since the query; look again on the next run instead of sending unverified.
            tx.update(ref, { nextCheckAt: Timestamp.fromMillis(nowMs), updatedAt: FieldValue.serverTimestamp() });
            count(result, "changed");
            return null;
        }
        if (!evaluation.send) {
            tx.update(ref, {
                nextCheckAt: evaluation.nextCheckAtMs == null ? null : Timestamp.fromMillis(evaluation.nextCheckAtMs),
                updatedAt: FieldValue.serverTimestamp(),
            });
            count(result, evaluation.reason);
            return null;
        }
        const after = (0, rules_1.applySent)(fresh, evaluation.send, nowMs, evaluation.localDate, evaluation.comebackAnchor);
        tx.set(ref, (0, state_1.stateToDoc)(after, evaluation.nextCheckAtMs));
        return { evaluation, language: fresh.language };
    });
    if (!(decided === null || decided === void 0 ? void 0 : decided.evaluation.send))
        return;
    const { evaluation, language } = decided;
    const nudge = evaluation.send;
    const devices = await db.collection("users").doc(uid).collection("devices").get();
    const byToken = new Map();
    for (const d of devices.docs) {
        const token = d.get("fcmToken");
        if (typeof token === "string" && token.length > 0)
            byToken.set(token, [...((_a = byToken.get(token)) !== null && _a !== void 0 ? _a : []), d.id]);
    }
    const tokens = [...byToken.keys()];
    const messages = (0, copy_1.buildMessages)(nudge, language, tokens, evaluation.localDate, nowMs);
    let delivered = 0;
    if (messages.length > 0) {
        const response = await messenger.sendEach(messages);
        const dead = [];
        response.responses.forEach((r, i) => {
            var _a;
            if (r.success)
                delivered++;
            else if (r.error && DEAD_TOKEN_CODES.has(r.error.code))
                dead.push(...((_a = byToken.get(tokens[i])) !== null && _a !== void 0 ? _a : []));
        });
        await Promise.all(dead.map((id) => db.collection("users").doc(uid).collection("devices").doc(id).delete()));
        result.invalidTokensRemoved += dead.length;
    }
    count(result, "send");
    result.sent.push({
        uid,
        kind: nudge.kind,
        variant: nudge.variant,
        localDate: evaluation.localDate,
        tokens: tokens.length,
        delivered,
        title: String((_d = (_c = (_b = messages[0]) === null || _b === void 0 ? void 0 : _b.notification) === null || _c === void 0 ? void 0 : _c.title) !== null && _d !== void 0 ? _d : ""),
        body: String((_g = (_f = (_e = messages[0]) === null || _e === void 0 ? void 0 : _e.notification) === null || _f === void 0 ? void 0 : _f.body) !== null && _g !== void 0 ? _g : ""),
        link: nudge.link,
    });
}
function count(result, reason) {
    var _a;
    result.reasons[reason] = ((_a = result.reasons[reason]) !== null && _a !== void 0 ? _a : 0) + 1;
}
//# sourceMappingURL=sweep.js.map