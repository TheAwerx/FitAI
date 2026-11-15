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
| `users/{uid}/custom_foods/{foodId}` | client | owner, functions |
| `users/{uid}/devices/{installationId}` | client, *reserved* | owner, functions |
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

Written when a plan is generated and when the user swaps in an alternative meal.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `createdAt`, `updatedAt` | | as above |
| `dailyCalories` | int 0..10000 | |
| `proteinG`, `carbsG`, `fatG` | int 0..2000 | |
| `isActive` | bool | |
| `meals` | list ≤ 8 of maps | detailed plan; `[]` for target-only plans (older plans) |

Meal map: `slot` (meal slot code, see below), `time` (`HH:mm`), `selected` (int, index of the option in use),
`options` (list ≤ 3; `[0]` is the main suggestion, the rest are alternatives). Option map: `title` (≤ 200),
`note` (prep tip, ≤ 300), `foods` (list ≤ 12). Food map: `foodId` (food database id \| null for AI foods
not in the database), `name` (≤ 120), `amount` (number), `unit` (unit code), `grams` (number, weight of the
amount), `calories` (int), `proteinG`, `carbsG`, `fatG` (number). Rules check the list type and size only.

Meal slot codes (`MealSlot` in `FoodModels.kt`): `breakfast`, `morning_snack`, `lunch`, `afternoon_snack`,
`dinner`, `evening_snack`. Unit codes (`FoodUnits`): `g`, `ml`, `piece`, `slice`, `portion`, `bowl`, `glass`,
`tea_glass`, `cup`, `mug`, `tbsp`, `dsp`, `tsp`, `handful`, `ladle`, `can`, `pack`, `scoop`.

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

Written on log and edit, deleted on delete. One document per diary entry: a photo-analysed meal, a quick
add, or one food from the food database (a planned meal logged with "ate as planned" is one entry per food).

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
| `slot` | meal slot code \| null | null for entries logged before meal slots; the app places them by time |
| `foodId` | string ≤ 64 (doc id chars) \| null | bundled food id or `custom_…`; null for photo and quick-add entries |
| `servingUnit` | unit code \| null | |
| `servingAmount` | number 0..5000 \| null | amount of `servingUnit` |
| `grams` | number 0..5000 \| null | weight of the entry |

`source` stays `photo` \| `manual`: food database and plan entries are `manual`.

### `users/{uid}/custom_foods/{foodId}`

Foods the user created in the food search. Written on create, deleted on delete. `{foodId}` is
`custom_` + 16 hex chars, the same id the device and `meal_logs.foodId` use.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion`, `id`, `createdAt`, `updatedAt` | | as above |
| `name` | non-empty string ≤ 80 | |
| `kcalPer100g` | number 0..900 | |
| `proteinPer100g`, `carbsPer100g`, `fatPer100g` | number 0..100 | |
| `servingUnit` | unit code | the household measure the food is logged by (`g` = by weight only) |
| `servingGrams` | number 0..5000 | weight of one `servingUnit` |

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

### Reserved for the next features (rules ready, nothing writes yet)

**`users/{uid}/devices/{installationId}`**, for push. Owner can write:
`platform` (`android` \| `ios`), `fcmToken` (string ≤ 4096), `language` (`tr` \| `en`),
`timeZone` (string ≤ 64), `appVersion` (string ≤ 32), `updatedAt` (ISO string). The
reminder function would read today's `workout_sessions` / `meal_logs` by `localDate`
and send to these tokens. Per-user queries such as
`users/{uid}/meal_logs where localDate == today` use the automatic single-field index. A
`collectionGroup('workout_sessions')` query across users needs a collection-group index
in a new `firestore.indexes.json`. Functions must not add fields to `users/{uid}` itself:
the client overwrites that document and the rules allow only the keys listed above.

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
   sessions, meals, measurements and custom foods. With an active workout and diet plan restored, the
   app goes straight to Main. Otherwise it opens plan setup, which builds plans from the
   restored answers.
3. **Neither**: onboarding.

After sign-in each local write queues the matching document write. Writes go out in
order, and the Firestore SDK holds them while the device is offline.
