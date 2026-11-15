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
exports.notificationsOnMealCreated = exports.notificationsOnSessionWritten = exports.notificationsOnPlanWritten = exports.notificationsOnDeviceWritten = exports.notificationsOnUserWritten = exports.notificationSweep = void 0;
const admin = __importStar(require("firebase-admin"));
const firebase_functions_1 = require("firebase-functions");
const firestore_1 = require("firebase-functions/v2/firestore");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const config_1 = require("./config");
const sweep_1 = require("./sweep");
const triggers_1 = require("./triggers");
/*
 * Dynamic push notifications ("nudges"). Overview, state document and the coexistence with the app's
 * local reminders: firebase/SCHEMA.md → "Push notifications". `admin.initializeApp()` runs in ../index.ts.
 */
const triggerOptions = { region: config_1.NOTIFICATIONS_REGION, retry: false };
/** Every 15 minutes: accounts whose `nextCheckAt` is due. */
exports.notificationSweep = (0, scheduler_1.onSchedule)({
    schedule: "every 15 minutes",
    timeZone: "Etc/UTC",
    region: config_1.NOTIFICATIONS_REGION,
    timeoutSeconds: 540,
    memory: "512MiB",
    maxInstances: 1,
    retryCount: 0,
}, async () => {
    const result = await (0, sweep_1.runNotificationSweep)({
        db: admin.firestore(),
        messenger: admin.messaging(),
        log: (message, data) => firebase_functions_1.logger.warn(message, data),
    });
    firebase_functions_1.logger.info("notification sweep", {
        checked: result.checked,
        sent: result.sent.length,
        reasons: result.reasons,
        invalidTokensRemoved: result.invalidTokensRemoved,
    });
});
exports.notificationsOnUserWritten = (0, firestore_1.onDocumentWritten)(Object.assign(Object.assign({}, triggerOptions), { document: "users/{uid}" }), (event) => { var _a, _b; return (0, triggers_1.onUserWritten)(admin.firestore(), event.params.uid, (_a = event.data) === null || _a === void 0 ? void 0 : _a.before.data(), (_b = event.data) === null || _b === void 0 ? void 0 : _b.after.data()); });
exports.notificationsOnDeviceWritten = (0, firestore_1.onDocumentWritten)(Object.assign(Object.assign({}, triggerOptions), { document: "users/{uid}/devices/{installationId}" }), (event) => (0, triggers_1.onDeviceWritten)(admin.firestore(), event.params.uid));
exports.notificationsOnPlanWritten = (0, firestore_1.onDocumentWritten)(Object.assign(Object.assign({}, triggerOptions), { document: "users/{uid}/workout_plans/{planId}" }), (event) => {
    var _a, _b;
    return (0, triggers_1.onWorkoutPlanWritten)(admin.firestore(), event.params.uid, event.params.planId, (_a = event.data) === null || _a === void 0 ? void 0 : _a.before.data(), (_b = event.data) === null || _b === void 0 ? void 0 : _b.after.data());
});
exports.notificationsOnSessionWritten = (0, firestore_1.onDocumentWritten)(Object.assign(Object.assign({}, triggerOptions), { document: "users/{uid}/workout_sessions/{sessionId}" }), (event) => { var _a, _b; return (0, triggers_1.onWorkoutSessionWritten)(admin.firestore(), event.params.uid, (_a = event.data) === null || _a === void 0 ? void 0 : _a.before.data(), (_b = event.data) === null || _b === void 0 ? void 0 : _b.after.data()); });
exports.notificationsOnMealCreated = (0, firestore_1.onDocumentCreated)(Object.assign(Object.assign({}, triggerOptions), { document: "users/{uid}/meal_logs/{mealId}" }), (event) => { var _a; return (0, triggers_1.onMealLogCreated)(admin.firestore(), event.params.uid, (_a = event.data) === null || _a === void 0 ? void 0 : _a.data()); });
//# sourceMappingURL=index.js.map