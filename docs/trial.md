# Free trial: once per account, once per device

The 7-day free trial unlocks everything Pro does (`AccessTier.Trial`). It can be used once
per account **and** once per device, so opening new e-mail accounts on the same phone does
not give a new trial. The server decides, with server time.

## Flow

1. **Paywall** (before sign-up, anonymous session). The app calls `getTrialStatus` with
   its device key. If the device already started a trial for any account, the yearly plan
   shows no "7 days free" label, the CTA says "Subscribe", and a card explains why
   ("Ücretsiz deneme bu cihazda kullanıldı"). When the check fails (offline) the trial
   is still offered, because the start is verified anyway.
2. **"7 gün ücretsiz başla"** on the yearly plan no longer runs the mock purchase. Before
   sign-up it only records the request (`trial_requested` in settings). Signed in (paywall
   opened from Settings), it starts the trial at once.
3. **Sign-up → plan setup.** `PlanSetupViewModel` calls `StartRequestedTrialUseCase`,
   which calls `startTrial`. Started: the tier is Trial and the first plans come from AI,
   like Pro. Refused: starter plans, plus a one-line reason on the setup screen. Failed
   (offline): starter plans, the request stays and Home retries it on the next refresh.
4. **During the trial** Home shows "Deneme: N gün kaldı", Settings shows "Pro deneme · N
   gün kaldı", and the trial-end reminder is scheduled 24 h before `endsAt`.
5. **Expiry** by server time: the tier drops to Free, so the existing gates and paywall
   apply. The trial is never restarted, on any device.

The same account on a second device keeps its running (or finished) trial; it does not
start a new one. Only the device the trial **started** on is marked as used.

## Server (`firebase/functions/src/trial.ts`)

- `startTrial({ deviceKey, platform })`: needs a non-anonymous account. One Firestore
  transaction reads `trials/{uid}` and `device_trials/{hmac}`: an existing account trial
  comes back unchanged; a device claimed by another account answers `used`; otherwise both
  documents are written. Two accounts racing on one device can't both win.
- `getTrialStatus({ deviceKey?, platform? })`: account trial if any, otherwise `eligible` /
  `used` for the device. Anonymous sessions only get the device answer. Writes nothing.
- Raw device ids are never stored. The app sends `SHA-256("fitai-device:v1:" + platform id)`;
  the server stores `HMAC-SHA256(TRIAL_DEVICE_PEPPER, "fitai-trial-device:v1:" + key)`.
- Without the `TRIAL_DEVICE_PEPPER` secret, `startTrial` refuses (`trial_not_configured`)
  and `getTrialStatus` skips the device check. The functions emulator uses a fixed test
  pepper when no `.secret.local` is present.
- Schema and rules: `firebase/SCHEMA.md`, `firebase/firestore.rules` (both collections are
  closed to clients). The old `initGuestTrial` and its 3-day `guest_trials` trial for any
  session (anonymous included) are gone.

## Client

| Piece | Where |
|---|---|
| Rules (mirror of the server), offline resolution | `core/domain/.../trial/TrialPolicy.kt` |
| Use cases | `core/domain/.../trial/TrialUseCases.kt` |
| Prod repository (callables + cache) | `core/data/.../trial/FirebaseTrialRepository.kt` |
| Dev fake (same rules, local ledger) | `core/data/.../trial/LocalTrialRepository.kt` |
| Device key, Android | `core/data/src/androidMain/.../trial/AndroidDeviceKeyProvider.kt` |
| Device key, iOS | `core/data/src/iosMain/.../trial/IosDeviceKeyProvider.kt` |
| Strings | `Strings.kt`, region `trial` |

**Device key.** Android uses `Settings.Secure.ANDROID_ID` (since Android 8 it is scoped to
the signing key, user and device, so it survives reinstalls; a random id in a separate
prefs file is the fallback when it is missing). iOS stores a random UUID in the Keychain
with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` and no iCloud sync, so it survives
reinstalls but not a wipe or a restore to another phone; `identifierForVendor` is the
fallback if the Keychain fails.

**Offline.** The last server answer is cached in app settings with the server time. It is
reused for 10 minutes, then refreshed. Offline, an active trial counts down from the cached
server time and stays usable for 3 days after the last answer; after that, or if the device
clock moves backwards, the tier is Free until the server answers again. A finished or
refused trial never turns active offline. The cache is cleared on sign-out.

**Dev flavor** uses `LocalTrialRepository`: the same rules on a ledger in app settings,
keyed by the same device key and the Firebase uid (or the local profile when Firebase is
not configured). The ledger survives sign-out (kept like the language), so a second
account on the same emulator is refused; it does not survive uninstall or "clear data".

**Checking reinstall persistence.** Debug builds show a line under Settings → Subscription:
`Deneme (debug): <state> · <days> gün · cihaz <8 chars> (<source>, <backend>)`. The 8
characters are the start of the hashed device key. Note them, uninstall, reinstall: they
must be the same (`android_id` / `keychain`). On a prod build, a second account after the
reinstall must also see `used`.

## Before production

1. Create the pepper once (32+ random characters) and never change it, or every device
   becomes eligible again:
   `openssl rand -hex 32 | firebase functions:secrets:set TRIAL_DEVICE_PEPPER --data-file=-`
2. Deploy functions and rules together, before shipping the new app:
   `firebase deploy --only functions,firestore:rules`. The CLI will ask to delete
   `initGuestTrial`; answer yes. `startTrial` is a new Cloud Run service; check its
   `allUsers` → `roles/run.invoker` binding as in `firebase/README.md` §5.
3. Old app builds keep working: `getTrialStatus` still returns the legacy fields. They
   no longer get the free 3-day guest trial.

## Known gaps and what would close them

| Gap | Effect | What would close it |
|---|---|---|
| Factory reset (Android) / device wipe (iOS) | New `ANDROID_ID` / Keychain item, so the device is eligible again | Play Integrity device recall bits / Apple DeviceCheck (2 bits per device that survive resets) |
| Rooted / jailbroken devices, modified app | `ANDROID_ID` can be changed and any device key can be sent; the key is not attested | Play Integrity verdict and App Attest checked in `startTrial` before accepting the key |
| Several devices | Each device gives one trial (one account per device), so a person with N devices can get N trials | Bind trials to something scarcer (verified phone number, payment method via store trials) |
| Store free trials once RevenueCat is live | Google Play / App Store track intro-offer eligibility per store account, not per device; a new Google/Apple account gets a new store trial | Offer the intro offer only when `getTrialStatus` says `eligible`, otherwise show the product without it; let the RevenueCat webhook write `trials/{uid}` and `device_trials` on `INITIAL_PURCHASE` with `period_type = TRIAL` |
| Offline grace | An active trial stays usable up to 3 days without a server check (AI features need the network anyway) | Shorter `TrialPolicy.OFFLINE_GRACE_MILLIS` |
| Client-side gating | `AccessTier` is computed on the device; a patched app can unlock local features | Check the trial or subscription in each AI callable (server quotas by tier) |
| Same account, other device | Not marked as a used device; another account can start a trial there | Intentional for shared tablets; mark every device an active trial was used on if abuse shows up |
| Emulator / simulator | Fresh ids per emulator image | Play Integrity / App Attest (fail on emulators) |
