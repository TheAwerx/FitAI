# 🧠 AI Memory Bank

Bu dosya (Memory Bank), proje üzerinde çalışan tüm yapay zeka ajanlarının (AI Agents) paylaşımlı hafızasıdır. Yeni bir özellik eklendiğinde, önemli bir hata (bug) düzeltilğinde veya mimari bir refactor yapıldığında, ilgili değişikliğin kısa bir özeti tarih/saat ile birlikte en üste (veya ilgili kategoriye) eklenmelidir. Bu sayede bir sonraki işlemde (veya farklı bir AI modeli devreye girdiğinde) projenin güncel durumu anında kavranabilir.

## 📝 Son Değişiklikler (Recent Changes)

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
- Kural: yeni `CREATE/ALTER TABLE` yok (SQLDelight migration kurulu değil)
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
