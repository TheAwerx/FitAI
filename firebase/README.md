# Firebase + Gemini (FitAI)

Gemini API anahtarı **cihaza konmaz**. Mobil uygulama Firebase Callable Functions üzerinden
`generateWorkoutPlan`, `generateDietPlan`, `analyzeMealPhoto`, `suggestNextMeal`,
`adaptTodaySession`, `coachChat`, `weeklyCoachReport` çağırır; Functions gizli
`GEMINI_API_KEY` ile Gemini’ye proxy olur.

Firebase yapılandırılmazsa uygulama otomatik `MockAIRepository` kullanır.

Proje ID: `fitai-1e8a4`

## Senin yapman gereken manuel adımlar

### 1. Gemini API anahtarı

1. [Google AI Studio](https://aistudio.google.com/apikey) → **Create API key**
2. Anahtarı kopyala (sadece Functions secret’ına girecek)
3. [Google Cloud Console](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com?project=fitai-1e8a4)
   içinde **Generative Language API**’yi etkinleştir

### 2. Firebase Console

Aynı proje (`fitai-1e8a4`):

1. **Blaze (pay as you go)** plana geç — 2nd gen Functions ve dışarı Gemini çağrısı için gerekli
2. **Authentication → Sign-in method**
   - Anonymous: Enable (guest / trial Gemini çağrıları için zorunlu)
   - Email/Password: Enable (kayıt/giriş ekranı)
   - Google: Enable. Bu adım Web OAuth client üretir (`….apps.googleusercontent.com`).
     Üretilmeden Android "Google ile devam et" `google_client_missing` verir.
   - Android uygulamasına debug (ve yayın) SHA-1 ekle. Google, imza parmak izi
     kayıtlı değilse kimlik penceresini açmaz.
   - Google'ı açtıktan sonra `google-services.json` dosyasını yeniden indir.
     Uygulama Web client id'yi bu dosyadaki `default_web_client_id` alanından okur.
     Elle vermek için `google_web_client_id` string'i doldurulabilir.
3. **Firestore Database** oluştur (production, tercihen `eur3` veya `nam5`)
4. Android `google-services.json` dosyası `composeApp/google-services.json` konumunda olmalı
   (git’e eklenmez). iOS `GoogleService-Info.plist` `iosApp/iosApp/` altında.

Paket adı: `com.fitai.app`

### 3. CLI, secret ve deploy

```bash
npm i -g firebase-tools
firebase login
cd firebase
firebase use fitai-1e8a4
cd functions
npm install
cd ..
firebase functions:secrets:set GEMINI_API_KEY
# yapıştır → Enter
firebase deploy --only functions,firestore:rules
```

İlk deploy Billing + Cloud Build / Artifact Registry izinleri isteyebilir; Console’daki
uyarıları onayla.

### 4. Doğrulama

Firebase Console → Functions listesinde şunlar `us-central1` bölgesinde olmalı:

- `generateWorkoutPlan`
- `generateDietPlan`
- `analyzeMealPhoto`
- `initGuestTrial`
- `getTrialStatus`
- `suggestNextMeal`
- `adaptTodaySession`
- `coachChat`
- `weeklyCoachReport`

### 5. Yeni fonksiyon deploy + IAM (her yeni callable için zorunlu)

Sadece AI koç fonksiyonlarını deploy etmek için:

```bash
cd firebase
firebase deploy --only functions:suggestNextMeal,functions:adaptTodaySession,functions:coachChat,functions:weeklyCoachReport,functions:analyzeMealPhoto
```

2nd gen callable’lar Cloud Run servisidir. `invoker: "public"` olsa bile ilk oluşturma
yarıda kalırsa servis `allUsers` → `roles/run.invoker` almayabilir ve istemci
`UNAUTHENTICATED` görür. Deploy sonrası her yeni servis için çalıştır (servis adı
fonksiyon adının küçük harflisidir):

```bash
for fn in suggestnextmeal adapttodaysession coachchat weeklycoachreport; do
  gcloud run services add-iam-policy-binding "$fn" \
    --region=us-central1 \
    --project=fitai-1e8a4 \
    --member=allUsers \
    --role=roles/run.invoker
done
```

Kontrol: `gcloud run services get-iam-policy coachchat --region=us-central1 --project=fitai-1e8a4`

## AI koç callable’ları

Tüm istekler `locale` (`"tr"` | `"en"`) taşır; cevap metinleri bu dilde döner. JSON alanları
`docs/FitAI_Uygulama_Plani.md` Bölüm 6 ile aynıdır.

| Callable | Model | Günlük limit |
|---|---|---|
| `suggestNextMeal` | `GEMINI_VISION_MODEL` (flash) | 30 |
| `adaptTodaySession` | `GEMINI_PLAN_MODEL` (pro) | 10 |
| `coachChat` | `GEMINI_VISION_MODEL` (flash) | 60 |
| `weeklyCoachReport` | `GEMINI_VISION_MODEL` (flash) | 5 |

Geçersiz istek → `invalid-argument`. Limit aşımı → `resource-exhausted` (mesaj `daily_limit`).
`analyzeMealPhoto` artık opsiyonel `locale` alır (yoksa Türkçe); cevap şekli değişmedi.

### Rate limit koleksiyonu

`aiUsage/{uid}/days/{yyyy-mm-dd}` (UTC gün). Alanlar: fonksiyon adı başına sayaç
(`suggestNextMeal`, `adaptTodaySession`, `coachChat`, `weeklyCoachReport`), `updatedAt`,
`expiresAt` (+7 gün). Sayaç Gemini çağrısından önce transaction ile artar; çağrı hata
verirse geri alınır. Yalnız Admin SDK yazar; `firestore.rules` bu koleksiyona istemci
erişimi vermez. Eski kayıtları otomatik silmek için Firestore TTL politikası
(`days` collection group, alan `expiresAt`) Console’dan açılabilir.

Uygulamada onboarding’i bitirince plan üretimi mock yerine Gemini çıktısı döner.
`google-services.json` yoksa veya Firebase init başarısızsa mock’a düşülür.

## Firestore şeması ve kurallar

Koleksiyonlar, alanlar, hangi ekrandan geldikleri ve kimin yazdığı: [`SCHEMA.md`](SCHEMA.md).
`firestore.rules` yalnız sahibine (anonim olmayan giriş) izin verir, alanları doğrular;
sunucu koleksiyonlarına (`guest_trials`, `plan_quotas`, `device_trials`, `aiUsage`)
istemci yazamaz. Kuralları değiştirince emülatörde test et (canlı projeye dokunmaz,
`demo-fitai` projesi kullanılır; Firestore emülatörü JDK 21 ister):

```bash
cd firebase/rules-tests
npm install
JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" npm test
```

Kurallar yeni istemciyle birlikte deploy edilmeli: eski sürümler `user_profiles/{uid}`
yazdığı için yeni kurallarla girişte hata alır.

`generateWorkoutPlan` / `generateDietPlan` tüm onboarding cevaplarını kullanır (cinsiyet,
yaş, boy, kilo, hedef, aktivite, deneyim, engel, odak, gün, süre, ekipman, diyet) ve
`locale` alır. Prompt'lar `functions/src/plans.ts` içinde; diyet cevabı Mifflin-St Jeor
referansının ±%20'si içinde tutulur.

## Push bildirimleri (akıllı hatırlatmalar)

Tasarım, tetikleyiciler ve yerel hatırlatmalarla iş bölümü: [`SCHEMA.md`](SCHEMA.md) → "Push notifications".
Kod `functions/src/notifications/`: 15 dakikada bir `notificationSweep` (yalnız `nextCheckAt` zamanı
gelen `notification_state/{uid}` belgeleri; istemci erişimi yok), Firestore tetikleyicileri `notificationsOn*`.

Canlıya almadan önce (sahibin adımları):

1. **APNs anahtarı**: Apple Developer → Keys → APNs anahtarı (.p8) oluştur; Firebase Console →
   Project settings → Cloud Messaging → Apple app configuration → anahtarı, Key ID ve Team ID ile yükle.
   iOS hedefinde Push Notifications yeteneği açık olmalı (`iosApp/iosApp/iosApp.entitlements`,
   App ID'de Push Notifications). Simülatör gerçek push almaz; cihazda dene.
2. **Firestore konumu**: tetikleyiciler `us-central1` (`functions/src/notifications/config.ts`,
   `NOTIFICATIONS_REGION`). Veritabanı `eur3` ise bu bölgeyi Avrupa'ya çevir.
3. **Cloud Scheduler API** zamanlanmış fonksiyon için açık olmalı (Blaze planı; ilk deploy sorar).
4. Kurallar, fonksiyonlar ve yeni istemci birlikte deploy edilmeli (yeni `notificationTypes` alanı
   zorunlu; push açık cihazlarda yerel 12:30 öğün hatırlatması kalkar, fonksiyonlar yoksa öğün
   hatırlatması gelmez):

```bash
cd firebase
firebase deploy --only firestore:rules,functions:notificationSweep,functions:notificationsOnUserWritten,functions:notificationsOnDeviceWritten,functions:notificationsOnPlanWritten,functions:notificationsOnSessionWritten,functions:notificationsOnMealCreated
```

Testler (canlı projeye dokunmaz, `demo-fitai`):

```bash
cd firebase/functions
npm ci
npm run test:only              # karar mantığı (birim, emülatörsüz)
JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" npm run test:emulator   # emülatörde uçtan uca, FCM taklit
```

## Mimari

```
App (KMP) → Firebase Auth (anonim veya e-posta)
         → Callable Functions (us-central1)
         → Gemini (API key sadece secret)
         → JSON (workout / diet / meal)
         → SQLDelight (offline kaynak)
```
