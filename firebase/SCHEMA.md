# FitAI Firestore schema (v1)

The device database (SQLDelight) is the source of truth. Firestore keeps a cloud copy per
account so a sign-in on a new or wiped device can restore everything, and so Cloud Functions
(push notifications, quotas) can read what the user did today.

Rules live in `firestore.rules`; tests in `rules-tests/`.

## Conventions

- **Owner**: `request.auth.uid == {uid}` and the sign-in provider is not `anonymous`.
  Anonymous sessions (used only to call AI functions before sign-up) get no cloud copy.
- **Client sync starts after sign-in.** `users/{uid}.localUserId` is the on-device
  `users.id`; the client writes only when the latest local user has `firebase_uid == uid`.
- **Document ids are the on-device ids** (16 or 32 hex chars). Restore keeps them, so
  plan, session and exercise links still match.
- **Timestamps** are ISO-8601 UTC strings from the device (`2026-11-15T07:30:12.345Z`).
  `localDate` is `yyyy-MM-dd` in the device time zone at write time, for "did they log
  today" queries.
- **Units are metric.** Height and body measurements in cm, weights in kg. The ft/in
  toggle in onboarding only changes the picker.
- **Codes, not labels.** Enum fields hold the codes in `core/domain/.../model/ProfileEnums.kt`
  and `OnboardingAnswers.kt`. Labels are localized on the device.
- Every client document has `schemaVersion` (currently `1`) and `updatedAt` (device time
  of the last write).
- Rules check key sets, types, enum codes and ranges. Embedded lists (exercises, sets)
  are checked for type and size only, because rules can't loop.

## Collections

| Path | Written by | Read by |
|---|---|---|
| `users/{uid}` | client (owner) | owner, functions |
| `users/{uid}/workout_plans/{planId}` | client | owner, functions |
| `users/{uid}/diet_plans/{planId}` | client | owner, functions |
| `users/{uid}/workout_sessions/{sessionId}` | client | owner, functions |
| `users/{uid}/meal_logs/{mealId}` | client | owner, functions |
| `users/{uid}/measurements/{measurementId}` | client | owner, functions |
| `users/{uid}/devices/{installationId}` | client | owner, functions |
| `notification_state/{uid}` | functions only | nobody (server only) |
| `plan_quotas/{uid}` | functions only, *reserved* | owner (read-only) |
| `device_trials/{deviceKey}` | functions only, *reserved* | nobody (server only) |
| `guest_trials/{uid}` | functions only | owner (read-only) |
| `aiUsage/{uid}/days/{yyyy-mm-dd}` | functions only | nobody (server only) |
| `user_profiles/{uid}` | *legacy, frozen* | owner (read-only) |

Anything not listed is denied.

### `users/{uid}`: profile and every onboarding answer

Written on sign-in (`LinkSignedInAccountUseCase`), after a profile edit, and a second
after any synced preference changes (debounced).

| Field | Type | Source screen / origin | Notes |
|---|---|---|---|
| `schemaVersion` | int | — | `1` |
| `localUserId` | string | device | SQLDelight `users.id`, reused on restore |
| `name` | string ≤ 80 | Onboarding → Ad | |
| `email` | string ≤ 254 | Auth | Firebase account email (empty for Apple private relay without email) |
| `age` | int 10..120 | Onboarding → Yaş | picker 13..80 |
| `heightCm` | number 50..272 | Onboarding → Boy | picker 140..210 |
| `weightKg` | number 20..500 | Onboarding → Kilo, Profile edit | picker 40..180 |
| `gender` | `male` \| `female` \| null | Onboarding → Cinsiyet | |
| `goal` | `lose_fat` \| `build_muscle` \| `get_stronger` \| `improve_endurance` \| `stay_healthy` | Onboarding → Hedef | |
| `activityLevel` | `sedentary` \| `light` \| `moderate` \| `very` \| `athlete` | Onboarding → Aktivite | |
| `equipment` | list of `bodyweight`, `dumbbells`, `barbell`, `resistance_bands`, `pull_up_bar`, `full_gym` (≤ 6) | Onboarding → Ekipman | empty = bodyweight |
| `dietRestrictions` | list of `none`, `vegetarian`, `vegan`, `pescatarian`, `halal`, `kosher`, `gluten-free`, `dairy-free` (≤ 8) | Onboarding → Diyet | |
| `experience` | `none` \| `beginner` \| `intermediate` \| `advanced` \| null | Onboarding → Deneyim | |
| `obstacle` | `time` \| `knowledge` \| `motivation` \| `injury` \| `equipment` \| `never_tried` \| null | Onboarding → Engel | |
| `focusAreas` | list of `personal_plan`, `smart_overload`, `plate_photo`, `log_lifts`, `streak`, `ai_coach` (≤ 3) | Onboarding → Odak | |
| `daysPerWeek` | int 2..6 | Onboarding → Gün, pledge | |
| `sessionMinutes` | int 15..90 | Onboarding → Süre | |
| `attributionSource` | `tiktok` \| `instagram` \| `youtube` \| `friend` \| `gym` \| `search` \| `other` \| null | Onboarding → Kaynak | |
| `commitmentConfirmedAt` | ISO string \| null | Onboarding → parmak izi taahhüdü | |
| `notificationsEnabled` | bool | Onboarding → bildirim izni, Settings | |
| `notificationTypes` | map of exactly `workoutLate`, `mealLogging`, `streakRisk`, `comeback` (bool) | Settings → Akıllı bildirimler | server push types, all `true` by default; see "Push notifications" |
| `reminderHour` | int 0..23 | Settings | local time |
| `reminderMinute` | int 0..59 | Settings | local time |
| `language` | `tr` \| `en` | app language at write time | for server-sent text |
| `timeZone` | string ≤ 64 | device (IANA, e.g. `Europe/Istanbul`) | for server-side "today" |
| `createdAt` | ISO string | device | local profile creation |
| `updatedAt` | ISO string | device | last write |

Device-only (not synced): streak freeze week, starter-challenge flags, funnel stage,
`requires_login`. The app language is stored per device.

### `users/{uid}/workout_plans/{planId}`

Written when a plan is generated (AI or starter), when Today Mode saves a one-off plan, and
when an exercise is replaced. The plan that was active before is rewritten with
`isActive = false`.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion` | int | |
| `id` | string | equals `{planId}` |
| `title` | string ≤ 200 | |
| `description` | string ≤ 2000 | |
| `daysPerWeek` | int 0..7 | `0` = Today Mode one-off plan |
| `kind` | `program` \| `adhoc` | derived from `daysPerWeek` |
| `isActive` | bool | at most one active program; restore keeps the newest |
| `createdAt` | ISO string | |
| `updatedAt` | ISO string | |
| `exercises` | list ≤ 150 of maps | see below |

Exercise map: `id` (string), `dayOfWeek` (int, ISO 1 = Monday; `≤ 0` = archived after a
replace but still referenced by history), `name`, `sets` (int), `reps` (string, e.g.
`8-12`, `30 sn`), `restSeconds` (int), `muscleGroup` (string), `notes` (string | null).

### `users/{uid}/diet_plans/{planId}`

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `createdAt`, `updatedAt` | | as above |
| `dailyCalories` | int 0..10000 | |
| `proteinG`, `carbsG`, `fatG` | int 0..2000 | |
| `isActive` | bool | |

### `users/{uid}/workout_sessions/{sessionId}`

Written when a session starts, a few seconds after each completed set (debounced), and
when it finishes. Push notifications use `localDate` + `completed`.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `updatedAt` | | |
| `planId` | string | `workout_plans/{planId}` |
| `startedAt` | ISO string | |
| `completedAt` | ISO string \| null | null while in progress or abandoned |
| `completed` | bool | `completedAt != null` |
| `durationMinutes` | int \| null | |
| `notes` | string \| null ≤ 2000 | |
| `localDate` | `yyyy-MM-dd` | day of `startedAt` in the device time zone |
| `totalSets`, `completedSets` | int | |
| `volumeKg` | number | Σ weight × reps of completed sets |
| `exercises` | list ≤ 40 of maps | `sessionExerciseId`, `exerciseId`, `orderIndex`, `name`, `muscleGroup`, `sets` (list of `id`, `setNumber`, `targetReps`, `actualReps` \| null, `weightKg` \| null, `completed`, `completedAt` \| null) |

### `users/{uid}/meal_logs/{mealId}`

Written on log, deleted on delete.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `updatedAt` | | |
| `mealName` | string ≤ 200 | |
| `calories` | int 0..10000 | |
| `proteinG`, `carbsG`, `fatG` | number 0..2000 | |
| `source` | `photo` \| `manual` | `photo` when analysed from a plate photo |
| `photoUrl` | string \| null | today only the marker `analyzed_photo`; images are not uploaded |
| `loggedAt` | ISO string | |
| `localDate` | `yyyy-MM-dd` | |

### `users/{uid}/measurements/{measurementId}`

Written on add or edit, deleted on delete.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `updatedAt` | | |
| `weightKg` | number 20..500 | |
| `bodyFatPct` | number 0..80 \| null | |
| `chestCm`, `waistCm`, `hipCm`, `armCm` | number 0..300 \| null | |
| `photoUrl` | string \| null | |
| `measuredAt` | ISO string | |
| `localDate` | `yyyy-MM-dd` | |

### `users/{uid}/devices/{installationId}`

One per app installation that should get server pushes. `installationId` is a random
32-hex id kept in the app's settings (new after sign-out). Written by
`SyncPushRegistrationUseCase` on launch, sign-in, token change and when reminders are
turned on (skipped when nothing changed in the last 7 days); deleted when reminders are
turned off and before sign-out. Functions delete it when FCM reports the token dead.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion` | int | `1` |
| `platform` | `android` \| `ios` | |
| `fcmToken` | string 1..4096 | FCM registration token |
| `language` | `tr` \| `en` | app language at write time |
| `timeZone` | string 1..64 | device IANA zone |
| `appVersion` | string ≤ 32 | e.g. `1.0.0-dev` |
| `updatedAt` | ISO string | |

Functions must not add fields to `users/{uid}` itself: the client overwrites that
document and the rules allow only the keys listed above. Server state lives in
`notification_state/{uid}` instead.

### Reserved for the next features (rules ready, nothing writes yet)

**`plan_quotas/{uid}`**, the plan-generation counter. Owner can read; only functions
write. Suggested fields: `workoutPlansGenerated` (int), `dietPlansGenerated` (int),
`periodStart` (timestamp), `limit` (int), `updatedAt` (timestamp).

**`device_trials/{deviceKey}`** keeps device-based trial tracking. No client access.
`deviceKey` is a server-side hash of a device identifier. Suggested fields: `firstSeenAt`,
`trialStartedAt`, `trialExpiresAt` (timestamps), `uids` (list of string).

### Server-only today

**`guest_trials/{uid}`**: `trialStartedAt`, `trialExpiresAt` (ISO strings),
`daysRemaining` (int), `isActive`, `hasSubscription` (bool). Written by
`initGuestTrial` / `getTrialStatus`; the client only reads it.

**`aiUsage/{uid}/days/{yyyy-mm-dd}`**: one counter per AI function, `updatedAt`, and
`expiresAt` (TTL). Admin SDK only.

**`notification_state/{uid}`**: push scheduling state, see below. Admin SDK only.

### Legacy

**`user_profiles/{uid}`** is the old profile document (name, email, age, heightCm,
weightKg, goal, activityLevel, comma-joined equipment and dietRestrictions, createdAt,
updatedAt). Rules make it read-only. On sign-in, when `users/{uid}` is missing, the client
restores from it and writes `users/{uid}`. App builds older than this schema can't sign in
once these rules are deployed, so deploy the rules together with the new client.

## Sign-in sync flow

1. **Device has a profile** (fresh onboarding): link the uid, write `users/{uid}`, merge
   older cloud history into the device (cloud plans come back inactive), then upload all
   plans and history.
2. **Device is empty** (sign-out or reinstall): read `users/{uid}`, falling back to
   `user_profiles/{uid}`, then restore the profile, onboarding answers, settings, plans,
   sessions, meals and measurements. With an active workout and diet plan restored, the
   app goes straight to Main. Otherwise it opens plan setup, which builds plans from the
   restored answers.
3. **Neither**: onboarding.

After sign-in each local write queues the matching document write. Writes go out in
order, and the Firestore SDK holds them while the device is offline.

## Push notifications

Server-sent "smart" nudges: `functions/src/notifications/*`, exported from `index.ts`.
Tests: `functions/test/rules.test.mjs` (decision logic) and
`functions/test/emulator/sweep.emulator.test.mjs` (Firestore emulator, stubbed FCM).

### How it runs

- **`notification_state/{uid}`** (server only) holds what the decision needs, so the
  sweep reads one document per due account: copies of the `users/{uid}` settings
  (`enabled`, `types`, `language`, `timeZone`, `reminderHour/Minute`, `daysPerWeek`,
  `createdDate`), the active program's `planTrainingDays`, `deviceCount`, an activity
  summary (`activeDates` — last 60 local days with a completed session or a meal,
  `lastWorkoutDate`, `lastMealDate`, `lastMealAt`), the sent log (`sentDate`,
  `sentKinds`, `lastSentAt`, comeback counter) and **`nextCheckAt`** (Timestamp or null).
- **Triggers** keep it current: `users/{uid}` (settings), `devices/*` (count),
  `workout_plans/*` (active program days), `workout_sessions/*` (a session became
  completed → today is done), `meal_logs/*` created. The first trigger for an account
  builds the state from the existing documents (one-time read of the last 60 days).
  All trigger bodies are idempotent; every change recomputes `nextCheckAt`.
- **`notificationSweep`** runs every 15 minutes and queries only
  `notification_state where nextCheckAt <= now` (automatic single-field index, paged).
  No user scan and no time-zone buckets: `nextCheckAt` is the next window start in the
  user's own zone, so DST and travel are handled by recomputing it. Accounts with
  notifications off, no device, or nothing left to send have `nextCheckAt = null`.
- Per due account: evaluate on the state → if a nudge would go out, read today's
  `workout_sessions` / `meal_logs` (`localDate == today`) to confirm → transaction:
  re-evaluate on the fresh state, append to the sent log, store the next check → send to
  every device token → delete devices whose token FCM reports as not registered.
  At most once per kind per local day even if two runs overlap.

### Nudges (local time of `users/{uid}.timeZone`)

| Kind (`type`) | Switch | Window | Sent when | Opens (`link`) |
|---|---|---|---|---|
| `workout_late` | `workoutLate` | reminder time + 90 min, for 2 h (clamped to 08:00–22:00) | training day (active program days, else the `daysPerWeek` spread), no completed session today, no session started in the last 3 h | `workout` |
| `meal_morning` | `mealLogging` | 11:00–12:00 | no meal logged today | `meal_log` |
| `meal_afternoon` | `mealLogging` | 15:30–16:30 | nothing logged since 11:00 | `meal_log` |
| `meal_evening` | `mealLogging` | 20:30–21:30 | nothing logged since 17:00 | `meal_log` |
| `streak_risk` | `streakRisk` | 20:00–21:30 | today is a required day, nothing done today, streak ≥ 3 days and this week's freeze is already used (same rules as `Streaks.kt`) | `home` |
| `comeback` | `comeback` | 18:00–19:30 | lapsed (below); first after ≥ 2 missed training days and ≥ 3 days, second after ≥ 7 days and ≥ 4 days after the first, then nothing until new activity | `workout` |

Limits: `notificationsEnabled` must be true and a device registered; quiet hours
22:00–08:00; at most 2 server pushes a day, at most 1 meal nudge a day, 90 minutes
between pushes; `streak_risk` replaces `workout_late` and `meal_evening` that day;
nothing on the sign-up day, no meal nudges on day 1 (the local "snap your lunch" fires),
no comeback in the first week. **Lapsed** users (no activity today, ≥ 2 planned training
days missed and ≥ 3 days since the last active day) get only comeback nudges.
Copy is in Turkish and English by `language` (`functions/src/notifications/copy.ts`).
Tuning: `functions/src/notifications/config.ts`.

Payload: `notification` (title, body) + `data` `{ type, link, localDate, source: "fitai_push" }`.
Android uses channel `fitai_smart_nudges` and click action
`com.fitai.app.action.OPEN_LINK`; APNs gets a collapse id per kind and a TTL to the
window end (max 2 h).

### Local reminders vs. server pushes

The app keeps scheduling its local reminders (they work offline and fire at exact times):
the daily workout reminder at the chosen time on training days, the D0–D7 onboarding
sequence and the trial-end reminder. The server owns only the conditional nudges above.
The one overlap is the meal reminder: while this device is registered for pushes
(`PushRegistrationStatus.Registered`) the fixed local 12:30 meal reminder is left out
and the server's "nothing logged" nudges replace it; without a registration (offline,
no Firebase, signed out) the local one stays. The `mealLogging` switch turns both off.
`workout_late` is a follow-up 90 minutes after the local workout reminder, never the
same message twice.
