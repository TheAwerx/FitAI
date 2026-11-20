# 🧠 AI Memory Bank

Bu dosya (Memory Bank), proje üzerinde çalışan tüm yapay zeka ajanlarının (AI Agents) paylaşımlı hafızasıdır. Yeni bir özellik eklendiğinde, önemli bir hata (bug) düzeltilğinde veya mimari bir refactor yapıldığında, ilgili değişikliğin kısa bir özeti tarih/saat ile birlikte en üste (veya ilgili kategoriye) eklenmelidir. Bu sayede bir sonraki işlemde (veya farklı bir AI modeli devreye girdiğinde) projenin güncel durumu anında kavranabilir.

## 📝 Son Değişiklikler (Recent Changes)

## 2026-11-20 — Impeccable ile UI yenileme (Ana sayfa, Antrenman)
- Skill: `.claude/skills/impeccable/` (pbakaus/impeccable v4.10.0, elle kopya). Web dedektör hook'u ve alt ajanlar kurulmadı (HTML/JSX kuralları, Compose'a uymuyor). Launcher ikilisi çalıştırılmadı; `DESIGN.md` doğrudan okunur
- Tema: `FitAITheme` 15 tipografi rolünün hepsini tanımlıyor (`bodySmall`, `titleSmall` vb. ~50 yerde Roboto'ya düşüyordu); renk şemasında `surfaceContainer*`, `secondaryContainer` (nane, seçili çip), `outline` `#6B6B6B` var (Material'ın mor varsayılanları sızıyordu). Android sistem çubuğu ikonları açık renk. `DESIGN.md` kodla eşlendi (Primary `#46F1C5`, Sora/Lexend)
- Bileşen: `ExerciseRow`, `WorkoutPlanCard` içerik yuvası; plan/öğün kartı ve TrialBanner'daki çift dolgu (32dp) giderildi; `FitAITextField` etiketi isteğe bağlı ve tek satır
- Ana sayfa: günün egzersizleri plan kartının içinde. Antrenman: gün listesi tek kartta, aktif seansta tek "Tekrar / Ağırlık (kg)" başlığı + hizalı kopyala ikonu
- Kural: alan etiketine sığmayan dar alanlarda sütun başlığı kullan; yeni ekranlarda tanımsız tipografi rolü kalmasın
- Önce/sonra: `docs/screenshots/ui-refresh/`. `assembleDevDebug` + `linkDebugFrameworkIosSimulatorArm64` başarılı. Dal `ui-refresh/impeccable` main'e birleştirildi (fast-forward). Beslenme, Profil, Onboarding/Paywall henüz bu geçişten geçmedi

## 2026-11-16 — iOS Dev / QA / Prod xcconfig
- `iosApp/Configuration`: `Config.xcconfig` (takım, bundle id, SemVer + build numarası) ve onu dahil eden `Dev`, `QA`, `Prod`. Xcode: Debug→Dev, QA→QA, Release→Prod. Şemalar `iosApp` (günlük Debug), `QA`, `Prod`
- Sürüm tek kaynak bu xcconfig; Android `versionName` / `versionCode` oradan okunur. Flavor `qa` eklendi (ekran adı FitAI QA)
- Dev yerel plan; QA ve Prod `generateWorkoutPlan` / `generateDietPlan`. Bundle id üçünde `com.fitai.app` (Firebase plist ile aynı). Push: Dev `development`, QA/Prod `production`
- `:composeApp:assembleDebug` ve `compileKotlinIosSimulatorArm64` başarılı

## 2026-11-15 — Ekstra testler: özellik listesi + fiziksel test
- `docs/FitAI_Ozellik_Test_Listesi.md` / `.csv` (UTF-8 BOM, Notion'a aktarılabilir): 118 madde; Pixel_9a emülatöründe (dev) elle test, Firebase emülatörü (functions 50/50, sweep 4/4, kurallar 16/16), iOS sim duman testi. 88 ✅, 7 🔧, 8 ❌, 15 ⏸; kanıtlar `docs/screenshots/qa/`
- Düzeltildi: uygulama içi dil değişince AI/plan dili (`FitAIApp` SideEffect kapsamı), dinlenme günü ipucu + seçili gün çipi görünür + "Aktif plan" etiketi, hızlı kalori makrosuz kaydedilir, sekme değişince eski arama sorgusu, TR ondalık virgül + EN "1 day", mock fiyat dili
- Açık: yarım bırakılan seans açık kalıyor ve hacme giriyor (G10), diyet makroları hedefe oturmuyor, `check:diet` yalnız kaloriyi kontrol ediyor (H3), tartı profil kilosunu güncellemiyor (J3, ürün kararı), alternatif listesi ekipmana göre sıralanmıyor (G5), paywall'dan dönüş hep Ana sayfa (C14), hesapsız deneme CTA'sı sessiz (C13), ft/in seçici tekrar (D7)
- Dev flavor `DevPlanAIRepository`: yalnız planlar yerel; koç, rapor, Bugün Modu, sonraki öğün ve fotoğraf canlı callable'a gider (N9). Hesap/deneme/kota/FCM deploy ve gerçek hesapla test edilmeli; fiziksel telefon bağlı değildi

## 2026-11-15 — Detaylı diyet planı, gıda veritabanı ve manuel öğün girişi
- Diyet planı öğün öğün: saat, ev ölçüsü + gram, gıda/öğün makroları, hazırlık notu, öğün başına 2 alternatif (tek dokunuşla değiştir), plan toplamı vs hedef. `StarterDiet` gıda veritabanından kısıtlara uygun üretir; sunucu öğünsüz dönerse istemci tamamlar
- Gıda veritabanı: 298 gıda (`core/data/.../files/foods.json`, `core/data/fooddb/build_foods.py` + README; USDA FDC CC0, tarif tahmini, etiket ortalaması — JSON elle değil script ile üretilir). Aksan/Türkçe harf duyarsız arama, kişisel gıdalar (`custom_foods`)
- FatSecret tarzı günlük: öğün kartında "+" → arama (plan/son/sık/kendi gıdaların) → ölçü + miktar + canlı makro → ekle; düzenle/sil, "Planladığımı yedim", hızlı kalori. `NutritionFlow` sekme içi yığın; bildirimdeki öğün linki de buraya açılır
- SQLDelight migration kuruldu: `migrations/1.sqm` (v1→v2), `databases/*.db`, `DatabaseMigrationTest` (SQLDelight `verifyMigrations` çok yavaş olduğu için kapalı). Yeni tablo/kolon artık migration ile eklenir
- Firestore: `diet_plans.meals`, `meal_logs` slot/foodId/servingUnit/servingAmount/grams (`localDate`, `source` aynı), `custom_foods`. Functions `generateDietPlan` öğünlü şema; deploy yapılmadı
- Testler, functions build + `check:diet`, kural testleri, iOS link başarılı; emülatörde eski build üstüne kurulum ve tüm akış doğrulandı

## 2026-11-15 — Firebase dinamik bildirimler (FCM) ve tetikleyiciler
- Sunucu `functions/src/notifications`: 15 dk'da bir `notificationSweep` yalnız `notification_state/{uid}.nextCheckAt` zamanı gelenleri okur (sunucuya özel belge); tetikleyiciler kullanıcı, cihaz, plan, seans ve öğün yazımlarında durumu günceller. Türler: antrenman gecikti (hatırlatma +90 dk), öğün kaydı (11:00 / 15:30 / 20:30), seri riski (20:00), geri dönüş (en fazla 2). TR/EN, 22:00–08:00 sessiz, günde en fazla 2, aynı tür günde 1 kez, ölü token silinir. Ayarlar `config.ts`
- İstemci: FCM token `users/{uid}/devices/{installationId}` (açılış, giriş, token yenileme; kapatma ve çıkışta silinir). Android yerel FCM + `fitai_smart_nudges` kanalı, iOS SPM FirebaseMessaging + `PushBridge` + `aps-environment`. Bildirime dokununca Antrenman / Öğün kaydı / Ana sayfa açılır (yerel hatırlatmalar da)
- Ayarlar → Akıllı bildirimler: 4 anahtar, `users/{uid}.notificationTypes`. Push kayıtlıyken yerel 12:30 öğün hatırlatması düşer, sunucu yalnız kayıt yoksa gönderir; günlük antrenman hatırlatması ve D0–D7 dizisi yerelde kalır
- Deploy öncesi: APNs anahtarı (.p8) yüklenmeli, Firestore bölgesi `NOTIFICATIONS_REGION` (us-central1) ile eşleşmeli, Cloud Scheduler açık olmalı; kurallar + fonksiyonlar istemciyle birlikte deploy edilmeli. Deploy yapılmadı
- Testler: functions 50/50 + emülatör 4/4 (FCM taklit), kurallar 16/16, Gradle, iOS link ve xcodebuild simülatör başarılı

## 2026-11-15 — Deneme süresi hesap + cihaz başına bir kez
- 7 günlük deneme sunucuda: `startTrial` / `getTrialStatus` (`functions/src/trial.ts`), `trials/{uid}` + `device_trials/{HMAC(deviceKey)}` tek transaction; aynı cihazda ikinci hesap `used`, aynı hesap başka cihazda denemesini korur, süre sunucu saatiyle. `initGuestTrial` ve herkese verilen 3 günlük `guest_trials` kaldırıldı (eski koleksiyon salt okunur)
- Cihaz anahtarı: Android `ANDROID_ID`, iOS Keychain UUID (`ThisDeviceOnly`, iCloud yok); istemcide SHA-256, sunucuda `TRIAL_DEVICE_PEPPER` ile HMAC. Secret deploy'dan önce bir kez oluşturulmalı, asla değişmemeli
- Paywall cihazı kayıttan önce sorar: kullanılmışsa "7 gün ücretsiz" yok, "Abone ol" + açıklama kartı. Deneme CTA'sı mock satın alma yapmaz, isteği kaydeder; PlanSetup kayıttan sonra başlatır (Trial da AI plan alır), çevrimdışıysa Home tekrar dener
- Önbellek: 10 dk tazeleme, aktif deneme 3 gün çevrimdışı geçerli, saat geri alınırsa geçersiz. Dev'de `LocalTrialRepository` aynı kurallar. Debug Ayarlar'da durum + cihaz anahtarı öneki
- Açıklar ve sonraki adımlar `docs/trial.md` (fabrika ayarı, root, çoklu cihaz, RevenueCat mağaza denemesi, Play Integrity / DeviceCheck). Functions 19/19, kurallar 15/15, Gradle + iOS link başarılı. Deploy yapılmadı

## 2026-11-15 — Firestore şeması özelleşti, planlar tüm onboarding cevaplarından
- Şema `firebase/SCHEMA.md`: `users/{uid}` (profil + tüm onboarding cevapları, dil, saat dilimi) ve alt koleksiyonlar `workout_plans`, `diet_plans`, `workout_sessions`, `meal_logs`, `measurements` (`localDate` ile); ayrılmış `devices`, `device_trials`. Eski `user_profiles` salt okunur, girişte `users/{uid}`'ye taşınır
- `firestore.rules`: catch-all yok; anonim olmayan sahip, alan/kod/aralık doğrulaması (`hasExactly` — istemciye yeni alan eklenirse kural, `SCHEMA.md`, `FirestoreMappersTest` ve `firebase/rules-tests` birlikte güncellenmeli). Kural testleri emülatörde (JDK 21 gerekir, Android Studio JBR)
- Senkron: SQLDelight kaynak; girişte tam yükleme veya geri yükleme (`UserDataSyncRepository`, `RestoredWithPlans` → Main), sonra `CloudSyncScheduler` + repository dekoratörleri her yazımı sırayla Firestore'a yansıtır
- Plan: `UserProfilePayload` engel/odak/`locale` aldı; `functions/src/plans.ts` prompt'ları tüm cevapları kullanır, diyet Mifflin referansının ±%20'si; `StarterPlans`/Mock da engel, odak, vegan protein ve cinsiyet tabanını uygular
- Kurallar yeni istemciyle birlikte deploy edilmeli (eski build'ler `user_profiles`'a yazar). Deploy yapılmadı

## 2026-11-15 — Antrenman planı yenileme hakkı (3) ve üretim animasyonu
- "Planı yenile" hesap başına 3 hak (`PlanQuotaPolicy.LIMIT`, opsiyonel sıfırlama; sunucu `PLAN_QUOTA`). Plan kurulumundaki ilk plan ücretsiz (`planPurpose=initial`), hak yalnız başarılı üretimde düşer; Ayarlar'daki "Planı yeniden üret" aynı hakkı kullanır
- Sunucu: `generateWorkoutPlan` → `withPlanQuota` (`planQuota.ts`), sayaç `plan_quotas/{uid}` (yalnız Admin SDK yazar, sahibi okur), transaction + kilit, bitince `plan_quota_exhausted`. İstek artık `{ profile, planPurpose }`. Deploy yapılmadı
- İstemci: dev'de ayarlarda yerel sayaç, prod'da Firestore (çevrimdışı yerel kopya). Antrenman sekmesinde "2/3 hak kaldı" çipi, onay diyaloğu, 0'da pasif buton
- `PlanGenerationOverlay`: kas grubu kartları karılıp Pzt–Paz ızgarasına dağılıyor, 3 adım, en az 4,5 sn, azaltılmış harekette statik
- `assembleDevDebug/ProdDebug`, domain+data testleri, functions build, iOS link, kural testleri (14/14) başarılı; emülatörde 3→0 hak ve animasyon doğrulandı

## 2026-11-15 — Eksik AI callable'ları deploy edildi
- `suggestNextMeal`, `adaptTodaySession`, `coachChat`, `weeklyCoachReport` → `fitai-1e8a4` / `us-central1`, Node.js 24
- Node.js 20 decommission (2026-10-30) yüzünden `functions/package.json` `engines.node` 24 oldu
- Eski altı fonksiyon da Node.js 24'e alındı: `generateWorkoutPlan`, `generateDietPlan`, `analyzeMealPhoto`, `initGuestTrial`, `getTrialStatus`. Konsoldaki on fonksiyon da `nodejs24`
- `coachChat` çağrısı Cloud Run 403 vermedi (boş gövde 400). `gcloud` yoktu; `invoker: "public"` ilk oluşturmada yetti

## 2026-11-15 — Çıkış oturumu siler ve logine döner
- `SignOutUseCase`: Firebase çıkış, yerel veritabanı + ayarlar (dil hariç), hatırlatıcılar, Android Credential Manager
- `requires_login` sonraki açılışta da `AuthRoute(signIn = true)` açar; geri yığın temizlenir
- Giriş sonrası profil yoksa onboarding, buluttan geldiyse plan kurulumu

## 2026-11-15 — Google giriş `google_client_missing`
- Yeni `composeApp/google-services.json` ve `iosApp/GoogleService-Info.plist` (OAuth client'lar dahil)
- `google_web_client_id`: Web client `529662166222-eh59s1sn2mbdo2ro3pd8vj70v18m5rr1`
- iOS `Info.plist` URL scheme: reversed iOS client id
- Debug SHA-1 ve SHA-256 `com.fitai.app` uygulamasına kayıtlı

## 2026-11-11 — Symmetry'den ilham alan huni (Welcome → Onboarding → Paywall → Signup → Plan)
- Akış: Welcome "Başla" → onboarding (3 tanıtım slaytı, Hedef, Engel, Odak, Kaynak, Deneyim, Cinsiyet, Ad/Yaş/Boy/Kilo, 2 ifade, 3 hikaye, Aktivite, Ekipman, Gün, Süre, Diyet, bildirim izni) → sahte "planın hazırlanıyor %" → sonuç (hedef tarihi + grafik) → alıntı → parmak izi taahhüt → Paywall → **zorunlu** Signup (`AuthRoute(funnel = true)`) → `PlanSetupRoute` → Main
- Onboarding'de gerçek plan üretilmez. `SetupInitialPlansUseCase`: Pro → `AIRepository` (kişisel), değilse `StarterPlans` (statik katalog, gün/süre/ekipman/deneyim). Dev'de `MockAIRepository` da `StarterPlans(personal = true)` kullanır
- Yeni cevaplar `UserPreferencesRepository` içinde (cinsiyet, deneyim, engel, odak, süre); `UserProfilePayload` opsiyonel alanlar aldı. Functions prompt'u bu alanları henüz kullanmıyor (deploy yok)
- Huni kaldığı yerden devam eder: `AppPreferences.funnelStage` (Paywall/Signup/PlanSetup). Uygulama içi dil seçimi `languageOverride` + `AppLanguage.override` (plan başlıkları da bu dili kullanır)
- Giriş: `LinkSignedInAccountUseCase` → `SignInOutcome` (yerel profil bağlandı / buluttan geri yüklendi / profil yok → onboarding). Şifre sıfırlama eklendi
- Paywall: "Planını seç", plan özeti kartı, üstü çizili fiyat + haftalık fiyat, X → çıkış teklifi alt sayfası. Misafir deneme kaldırıldı
- Main: her sekmede "Başlangıç Görevleri" pill + alt sayfa (bildirim, ilk antrenman, ilk öğün fotoğrafı, ilk tartı; hediye, "tekrar gösterme"). Ana sayfada seri çipi + PRO rozeti; antrenman bitince `StreakScreen` → `SessionSummary`. Gün 0 kartı kaldırıldı
- Profil: hub (istatistik, Antrenmanlar/Grafik sekmeleri, haftalık trend), `SettingsScreen`, `MeasurementsScreen` (kilo/bel grafiği, tartı ekle). Ortak `FitAICharts.kt` (çizgi grafik, halka, segment kontrol, kesikli çerçeve)
- Welcome'daki kanıt satırları dürüst tutuldu: sahte puan, kullanıcı sayısı veya yorum yok
- Yeni tablo yok. `assembleDebug` başarılı. Emülatörde tüm huni, görevler, seri, profil, ayarlar ve ölçümler duman testinden geçti

## 2026-11-07 — Dev / Prod build variants
- Android flavor `environment`: `dev` (varsayılan) ve `prod`. `devDebug` launcher adı FitAI Dev
- Dev: `generateWorkoutPlan` / `generateDietPlan` Cloud Functions çağrılmaz; `MockAIRepository` sabit planları kullanılır. Öğün fotoğrafı remote kalır
- Prod: mevcut `FirebaseAIRepository`. iOS `FITAI_ENVIRONMENT` (xcconfig, varsayılan `dev`)
- `:composeApp:assembleDebug` başarılı

## 2026-11-07 — Strateji planı koda döküldü (4 dalga, 8 ajan)
- Plan: `docs/FitAI_Uygulama_Plani.md` (sözleşmeler, dosya sahipliği, `Strings.kt` bölge işaretleri `// region:<alan>:<interface|tr|en>`)
- Core: `access/` (Free/Trial/Pro + `GatedFeature` kotaları), `billing/` (mock, RevenueCat'e hazır), `analytics/` (log), `preferences/`, `stats/WeeklyStats`, `ProgressRepository`, `training/` (overload, PR, alternatif katalog), `nutrition/` (antrenman/dinlenme günü makroları), `engagement/` (seri, XP, rozet, Gün 0), `notifications/` (planlayıcı)
- AI: `suggestNextMeal`, `adaptTodaySession`, `coachChat`, `weeklyCoachReport` (istemci + `firebase/functions`, günlük kota `aiUsage/{uid}/days`). Deploy ve `run.invoker` IAM adımı yapılmadı
- Workout: öneri ön dolu set, set kopyala, dinlenme sayacı, Değiştir, seans özeti, Bugün Modu (pasif ad-hoc plan, `days_per_week = 0`)
- Onboarding: Hedef ilk, e-posta adımı yok, kaynak sorusu, plan önizleme, basılı tut taahhüt, yeni paywall, sonra bildirim izni (`navigateAfterPaywall`)
- Nutrition: gerçek kamera/galeri seçici (Android + iOS), Tabak → Plan, Sonraki öğün (Pro), fotoğraf kotası; `photo_url = "analyzed_photo"`
- Yeni modül `feature:coach` (sohbet + haftalık rapor); Home slot ile `CoachEntryCard`; Progress sekmesi açıldı
- Kaldırıldı: `moko-permissions` (iOS link'te K/N çöküyordu); composeApp `generateResClass = never` (çift `Res` sınıfı)
- `MainTabShell` sistem geri tuşu; Workout gün çipleri yerelleşti (`weekdayShort`)
- Kural (2026-11-15'te değişti): şema değişikliği artık `migrations/<n>.sqm` + `databases/<n>.db` + `DatabaseMigrationTest` ile yapılır (o tarihe kadar migration kurulu değildi)
- `assembleDebug`, `linkDebugFrameworkIosSimulatorArm64`, `:core:domain:testDebugUnitTest` başarılı; emülatörde sekmeler, Koç, Bugün Modu duman testi geçti

## 2026-11-07 — Rakip (Symmetry) analizi ve strateji planı
- `docs/FitAI_Rekabet_ve_Buyume_Stratejisi.html`: rakip analizi, MVP matrisi, farklılaşma, GTM, gelir modeli, 90 günlük yol haritası
- Kanca: "Antrenmanın ve tabağın, tek AI koçta"; MVP eksikleri: progressive overload, streak, bildirim akışı, abonelik altyapısı
- Önerilen yeni özellikler: Tabak → Plan, Bugün Modu, Bağlamsal AI Koç; oyunlaştırma güce değil plan uyumuna bağlı (Disiplin Ligi)

## 2026-11-07 — Paywall, onboarding tasarım dili
- Paywall: koyu zemin, `OnSurface` başlık, nane ikon kuyusu, seçili planda 2dp `Primary` kenarlık + radio, sabit alt çubukta tek Primary CTA
- Kural: `ui-design-system-core` içine Welcome / adımlar / üretim / paywall dili eklendi
- `:composeApp:assembleDebug` başarılı

## 2026-11-07 — Splash yalnızca Lottie, otomatik geçiş
- Ekranda sadece dumbbell animasyonu; isim, yükleniyor ve devam butonu kalktı
- Animasyon bir kez oynayınca mevcut sonraki rota (Welcome / Onboarding / Main)
- Arka plan füme geçiş `#6E6E6E` → `#525252` → `#3A3A3A`
- `:composeApp:assembleDebug` başarılı

## 2026-11-07 — Splash Lottie dumbbell animasyonu
- LottieFiles “Gym & Fitness Gold” (Resistance): ağırlıklar bara kayıyor; dolgu `#46F1C5`, düz arka plan katmanı çıkarıldı
- Asset: `core/design-system/.../files/splash_dumbbell.json`; oynatıcı Compottie 2.1.0 (CMP 1.10)
- `:composeApp:assembleDebug` başarılı

## 2026-09-22 — generateDietPlan UNAUTHENTICATED IAM düzeltmesi
- Endpoint kodu / Firebase Auth değil: Cloud Run `roles/run.invoker` eksikti
- İlk create fail sonrası `generateDietPlan` ve `analyzeMealPhoto` `allUsers` almamıştı; `generateWorkoutPlan` almıştı
- IAM: her iki servise `allUsers` → `roles/run.invoker` verildi

## 2026-09-22 — Gemini modelleri 3.x’e güncellendi
- Yeni hesaplarda `gemini-2.5-pro` 404 (`NOT_FOUND`) dönüyordu
- Plan: `gemini-3.1-pro-preview`; öğün fotoğrafı: `gemini-3.5-flash`

## 2026-09-20 — Gemini API Firebase Functions üzerinden bağlandı
- Callable client: `FitAiFunctions` (`us-central1`, 120s timeout); `FirebaseAIRepository` Koin ile `AiRemoteDataSource` inject ediyor
- Firebase bootstrap idempotent (google-services / native `FirebaseApp.configure` sonrası tekrar init mock’a düşürmüyor)
- Functions: `GEMINI_API_KEY` secret, structured JSON, `invoker: public`; workout/diet `gemini-2.5-pro`, meal photo `gemini-2.5-flash`
- AI UseCase’ler çağrı öncesi anonymous auth; Android `INTERNET` izni
- `.firebaserc` → `fitai-1e8a4`; adımlar `firebase/README.md`

## 2026-09-20 — Onboarding numeric picker snap/selection fix
- `FitAINumericPicker`: viewport merceği `viewportStartOffset`/`viewportEndOffset` ile hesaplanıyor (sağdaki görünür değer yanlış seçiliyordu)
- Snap `SnapPosition.Center`; kaydırınca takılma / yanlış değer döngüsü giderildi
- `:composeApp:assembleDebug` başarılı

## 2026-08-27 — Onboarding screens matched to Stitch
- Welcome through Plan Generation aligned to Stitch FitAI screens (hero, chrome, copy, mint `#46F1C5`)
- Age / Height / Weight: snap sliding numeric pickers; height cm/ft toggle
- Goal / Activity / Equipment / Diet: Stitch card/chip layouts + icons
- Theme: Sora + Lexend fonts; `:composeApp:assembleDebug` successful

## 2026-08-12 — Onboarding / Profile Stitch tamamlandı
- Domain: `ProfileEnums` (goal/activity/diet/equipment kodları) + `UpdateProfileUseCase` + `RegeneratePlansUseCase`
- Onboarding: Stitch 10 adım (Name→Email opsiyonel→Age→Height→Weight→Goal→Activity→Equipment→Diet→Generating); kodlar SQLDelight’a kaydedilir → Gemini/Mock plan
- Başarı sonrası `PaywallRoute` (FitAI Premium); Start Trial / Continue Guest → Main
- Profile Hub: özet + edit + “Planı yeniden üret” CTA; trial_expired → Paywall
- `:composeApp:assembleDebug` başarılı

## 2026-08-12 — Stitch main-tab screen brief
- `.stitch/STITCH_MAIN_TABS.md`: Home / Workout / Nutrition / Progress / Profile + alt ekranlar için Stitch talimatları

## 2026-08-12 — FitAI DESIGN.md (Stitch design system)
- `.stitch/DESIGN.md` eklendi: YAML token’lar + Overview/Colors/Typography/Layout/Elevation/Shapes/Components
- Kaynak: `FitAIColors` / `FitAISpacing` / `FitAIShapes` / shared components
- Kök `DESIGN.md` → `.stitch/DESIGN.md` symlink (kolay erişim)

## 2026-07-22 — `.cursor/rules` birebir mimari uyum (firebase/ hariç)
- Feature başına `:domain` / `:data` / `:presentation` Gradle modülleri eklendi
- `UseCaseState`, `BaseViewModel`, `ApiRequest` + `AiRemoteDataSource`, `UserLocalDataSource`
- ViewModel’ler yalnızca UseCase inject ediyor; `*Mvi` → `*Contract` (State/Intent/Effect)
- Lokalizasyon: `LocalStrings` + `StringsTr`/`StringsEn`; `NavigationRoot` (eski AppNavHost)
- Auth repository `feature:auth:data` altında; `firebase/` backend’e dokunulmadı
- `:composeApp:assembleDebug` başarılı

---
*(Yeni geliştirme yaptıkça veya büyük bir hata çözüldüğünde yukarıdaki listeye en güncel gelişmeyi en üste gelecek şekilde eklemeyi unutmayın.)*
