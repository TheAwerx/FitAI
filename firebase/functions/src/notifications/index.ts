import * as admin from "firebase-admin";
import { logger } from "firebase-functions";
import { onDocumentCreated, onDocumentWritten } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { NOTIFICATIONS_REGION } from "./config";
import { runNotificationSweep } from "./sweep";
import {
  onDeviceWritten,
  onMealLogCreated,
  onUserWritten,
  onWorkoutPlanWritten,
  onWorkoutSessionWritten,
} from "./triggers";

/*
 * Dynamic push notifications ("nudges"). Overview, state document and the coexistence with the app's
 * local reminders: firebase/SCHEMA.md → "Push notifications". `admin.initializeApp()` runs in ../index.ts.
 */

const triggerOptions = { region: NOTIFICATIONS_REGION, retry: false };

/** Every 15 minutes: accounts whose `nextCheckAt` is due. */
export const notificationSweep = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Etc/UTC",
    region: NOTIFICATIONS_REGION,
    timeoutSeconds: 540,
    memory: "512MiB",
    maxInstances: 1,
    retryCount: 0,
  },
  async () => {
    const result = await runNotificationSweep({
      db: admin.firestore(),
      messenger: admin.messaging(),
      log: (message, data) => logger.warn(message, data),
    });
    logger.info("notification sweep", {
      checked: result.checked,
      sent: result.sent.length,
      reasons: result.reasons,
      invalidTokensRemoved: result.invalidTokensRemoved,
    });
  },
);

export const notificationsOnUserWritten = onDocumentWritten({ ...triggerOptions, document: "users/{uid}" }, (event) =>
  onUserWritten(admin.firestore(), event.params.uid, event.data?.before.data(), event.data?.after.data()),
);

export const notificationsOnDeviceWritten = onDocumentWritten(
  { ...triggerOptions, document: "users/{uid}/devices/{installationId}" },
  (event) => onDeviceWritten(admin.firestore(), event.params.uid),
);

export const notificationsOnPlanWritten = onDocumentWritten(
  { ...triggerOptions, document: "users/{uid}/workout_plans/{planId}" },
  (event) => onWorkoutPlanWritten(
    admin.firestore(),
    event.params.uid,
    event.params.planId,
    event.data?.before.data(),
    event.data?.after.data(),
  ),
);

export const notificationsOnSessionWritten = onDocumentWritten(
  { ...triggerOptions, document: "users/{uid}/workout_sessions/{sessionId}" },
  (event) => onWorkoutSessionWritten(admin.firestore(), event.params.uid, event.data?.before.data(), event.data?.after.data()),
);

export const notificationsOnMealCreated = onDocumentCreated(
  { ...triggerOptions, document: "users/{uid}/meal_logs/{mealId}" },
  (event) => onMealLogCreated(admin.firestore(), event.params.uid, event.data?.data()),
);
