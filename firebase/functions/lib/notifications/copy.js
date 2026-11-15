"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.COPY_TABLE = exports.COPY_KEYS = void 0;
exports.pushText = pushText;
exports.buildMessages = buildMessages;
const config_1 = require("./config");
/**
 * Short, friendly, no guilt: say what is possible now, never what was missed. Keys are
 * `kind` or `kind:variant`. `{days}` is the current streak.
 */
const COPY = {
    tr: {
        "workout_late": {
            title: "Bugünkü antrenmanın hâlâ seni bekliyor",
            body: "Planladığın saat geçti ama gün bitmedi. Vaktin azsa 20 dakikalık Bugün Modu da sayılır.",
        },
        "meal_morning": {
            title: "Güne ne ile başladın?",
            body: "Kahvaltını birkaç saniyede kaydet, günün makroları netleşsin.",
        },
        "meal_afternoon": {
            title: "Öğle yemeğini kaydettin mi?",
            body: "Tabağının fotoğrafını çek, makroları ben hesaplayayım.",
        },
        "meal_evening": {
            title: "Akşam yemeğini ekleyelim mi?",
            body: "Günün öğünlerini kaydet, yarınki önerilerim sana daha iyi uysun.",
        },
        "streak_risk": {
            title: "{days} günlük serin devam etsin",
            body: "Kısa bir seans ya da tek bir öğün kaydı bugünü sayar.",
        },
        "comeback:first": {
            title: "Kaldığın yerden devam edelim",
            body: "Ara vermek normal. Bugün kısa bir seansla yeniden başlayabilirsin.",
        },
        "comeback:start": {
            title: "Planın hazır, ilk adım senin",
            body: "İlk seansın 20 dakika bile sürebilir. Hazır olduğunda buradayım.",
        },
        "comeback:second": {
            title: "Planın hâlâ burada",
            body: "Hazır olduğunda 20 dakikalık bir Bugün Modu seansı iyi bir başlangıç olur.",
        },
    },
    en: {
        "workout_late": {
            title: "Today's workout is still waiting",
            body: "Your planned time has passed, but the day isn't over. A 20-minute Today Mode session counts too.",
        },
        "meal_morning": {
            title: "How did your day start?",
            body: "Log breakfast in a few seconds to keep today's macros on track.",
        },
        "meal_afternoon": {
            title: "Logged your lunch?",
            body: "Snap your plate and I'll count the macros.",
        },
        "meal_evening": {
            title: "Add your dinner?",
            body: "Log today's meals so tomorrow's suggestions fit you better.",
        },
        "streak_risk": {
            title: "Keep your {days}-day streak going",
            body: "A short session or one logged meal counts for today.",
        },
        "comeback:first": {
            title: "Pick up where you left off",
            body: "Breaks happen. A short session today is a great way back in.",
        },
        "comeback:start": {
            title: "Your plan is ready when you are",
            body: "Your first session can be just 20 minutes. I'm here when you're ready.",
        },
        "comeback:second": {
            title: "Your plan is still here",
            body: "Whenever you're ready, a 20-minute Today Mode session is a good restart.",
        },
    },
};
function pushText(nudge, language) {
    var _a, _b, _c, _d;
    const table = (_a = COPY[language]) !== null && _a !== void 0 ? _a : COPY.tr;
    const text = (_c = (_b = table[`${nudge.kind}:${nudge.variant}`]) !== null && _b !== void 0 ? _b : table[nudge.kind]) !== null && _c !== void 0 ? _c : COPY.tr[nudge.kind];
    const days = String((_d = nudge.streakDays) !== null && _d !== void 0 ? _d : "");
    return { title: text.title.replace("{days}", days), body: text.body.replace("{days}", days) };
}
/** Every key the copy table must have, per language (checked in tests). */
exports.COPY_KEYS = Object.keys(COPY.tr);
exports.COPY_TABLE = COPY;
/**
 * One FCM message per device token. `data.link` is the screen the app opens on tap; Android uses the
 * app's channel and click action so the tap reaches MainActivity even when the system shows the push.
 */
function buildMessages(nudge, language, tokens, localDate, nowMs) {
    const text = pushText(nudge, language);
    const ttlMs = Math.max(15 * 60000, Math.min(config_1.NOTIFICATION_CONFIG.pushTtlMinutes * 60000, nudge.windowEndMs - nowMs + 30 * 60000));
    const data = { type: nudge.kind, link: nudge.link, localDate, source: "fitai_push" };
    return tokens.map((token) => ({
        token,
        notification: text,
        data,
        android: {
            priority: "normal",
            ttl: ttlMs,
            collapseKey: nudge.kind,
            notification: {
                channelId: config_1.ANDROID_CHANNEL_ID,
                icon: config_1.ANDROID_ICON,
                color: config_1.ACCENT_COLOR,
                tag: nudge.kind,
                clickAction: config_1.ANDROID_CLICK_ACTION,
            },
        },
        apns: {
            headers: {
                "apns-push-type": "alert",
                "apns-priority": "5",
                "apns-expiration": String(Math.floor((nowMs + ttlMs) / 1000)),
                "apns-collapse-id": nudge.kind,
            },
            payload: { aps: { sound: "default", "thread-id": `fitai.${nudge.pref}` } },
        },
    }));
}
//# sourceMappingURL=copy.js.map