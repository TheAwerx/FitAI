# FitAI Uygulama Planı (Strateji Raporundan Koda)

Kaynak: `docs/FitAI_Rekabet_ve_Buyume_Stratejisi.html` (Bölüm 2, 3.4, 4.2, 5, 6).
Tarih: 2026-11-07. Durum: Dalga 1–5 tamamlandı. Kalan: Functions deploy + IAM, gerçek RevenueCat, cihazda onboarding/bildirim/foto testi.

## 1. Kapsam

| # | Rapor maddesi | Kod karşılığı | Dalga / Ajan |
|---|---|---|---|
| 1 | Erişim katmanı (Free / Trial / Pro) ve ücretsiz kota | `EntitlementRepository`, `UsageLimitRepository`, `CheckFeatureAccessUseCase` | 1 / Foundation |
| 2 | Analitik ve kaynak ölçümü | `AnalyticsTracker`, `AnalyticsEvents` | 1 / Foundation |
| 3 | Kullanıcı tercihleri (kaynak, taahhüt, hatırlatma saati) | `UserPreferencesRepository` | 1 / Foundation |
| 4 | Abonelik altyapısı (RevenueCat'e hazır) | `BillingRepository` + mock | 1 / Foundation |
| 5 | Haftalık istatistik (rapor + ana sayfa) | `WeeklyStats`, `ComputeWeeklyStatsUseCase` | 1 / Foundation |
| 6 | Yeni AI çağrıları (istemci) | `AIRepository` + Firebase + Mock | 1 / Foundation |
| 7 | Yeni AI çağrıları (sunucu) | `firebase/functions/src/index.ts` | 1 / Backend |
| 8 | Progressive overload, set kopyala, dinlenme sayacı, egzersiz değiştir, PR, seans özeti | `feature/workout`, `core/domain/training` | 2 / Workout |
| 9 | Bugün Modu (adaptif seans) | `feature/workout` | 2 / Workout |
| 10 | Onboarding sırası, e-posta adımı yok, kaynak sorusu, plan önizleme, taahhüt, paywall planları | `feature/onboarding` | 2 / Onboarding |
| 11 | Tabak → Plan, antrenman günü makroları, fotoğraf kotası | `feature/nutrition` | 3 / Nutrition |
| 12 | Bağlamsal AI Koç + haftalık rapor | yeni `feature/coach` | 3 / Coach |
| 13 | Seri, XP, rozet, Gün 0 listesi, ilerleme sekmesi, koç girişi | `feature/home`, `core/domain/engagement` | 4 / Engagement |
| 14 | Yerel bildirimler (D0–D7, antrenman, öğün, trial bitişi), izin ekranı | platform kodu + `core/domain/notifications` | 4 / Notifications |

**Kapsam dışı (sonraki faz):** çok oyunculu Disiplin Ligi (sunucu ister), sosyal akış, gerçek RevenueCat anahtarları, Ramazan modu, veri dışa aktarma (CSV), giyilebilir cihaz.

## 2. Dalgalar

```
Dalga 1:  Foundation (Kotlin core)   ‖  Backend (TypeScript)
Dalga 2:  Workout                    ‖  Onboarding + Paywall
Dalga 3:  Nutrition                  ‖  Coach
Dalga 4:  Engagement + Home/Progress ‖  Notifications
Dalga 5:  Entegrasyon build'i, inceleme, MEMORY_BANK
```

Her dalga, önceki dalganın build'i yeşil olunca başlar.

## 3. Ortak kurallar (tüm ajanlar)

- Önce `PROJECT_GOALS.md` ve `.cursor/rules/*.mdc` dosyalarını oku.
- ViewModel yalnız UseCase alır. UseCase `UseCaseState` döner (`useCaseCatching`). UseCase yalnız Repository arayüzü kullanır.
- MVI: `[X]Contract.kt` (State, Intent, Effect), `[X]ViewModel.kt` (`BaseViewModel`), `[X]Screen.kt`.
- Arayüz metni sabit yazılmaz. `core/presentation/.../localization/Strings.kt` içinde **kendi bölge işaretinin hemen altına** ekle (bkz. Bölüm 5). Arayüz + `StringsTr` + `StringsEn` üçünü birlikte güncelle. Eklemeden önce anahtar adını ara.
- Tasarım: `FitAIColors`, `FitAISpacing`, `FitAIShapes`, `MaterialTheme.typography`, `core/design-system` bileşenleri. Onboarding/paywall için `ui-design-system-core` tablosuna uy.
- Koin: her yeni UseCase / Repository / ViewModel kaydedilir. Yeni modül `composeApp/.../di/KoinModules.kt` listesine eklenir.
- **Yeni `CREATE TABLE` veya `ALTER TABLE` ekleme.** Projede SQLDelight migration yok; mevcut kurulumlar bozulur. Mevcut tablolara yeni **sorgu** eklemek serbesttir. Küçük kalıcı veri için `multiplatform-settings` kullan.
- Build: kendi modüllerini derle, sonunda `./gradlew :composeApp:assembleDebug`. Hata varsa düzelt; devam etme.
- Paralel ajan çalışır. Sahibi olmadığın dosyaya dokunma. Build hatası başka ajanın dosyasındaysa 60 sn bekle, tekrar dene (en fazla 5 kez), sonra raporla.
- `MEMORY_BANK.md` dosyasını güncelleme; özetini dönüş mesajında ver.
- Git commit yapma.

## 4. Dalga 1 sözleşmeleri (Foundation sağlar, diğerleri kullanır)

Paket kökü `com.fitai.app`. Arayüzler `core/domain`, uygulamalar `core/data`.

```kotlin
// access/AccessModels.kt
enum class AccessTier { Free, Trial, Pro }
enum class QuotaPeriod { Week, Month }
enum class GatedFeature(val freeQuota: Int, val period: QuotaPeriod) {
    MealPhotoAnalysis(3, QuotaPeriod.Week),
    TodayMode(1, QuotaPeriod.Week),
    PlanRegeneration(1, QuotaPeriod.Month),
    NextMealSuggestion(0, QuotaPeriod.Week),
    CoachChat(0, QuotaPeriod.Week),
    WeeklyReportFull(0, QuotaPeriod.Week),
}
data class FeatureAccess(val feature: GatedFeature, val tier: AccessTier, val allowed: Boolean, val remaining: Int?) // remaining null = sınırsız

interface EntitlementRepository { suspend fun currentTier(): AccessTier }   // Trial aktif veya abonelik → Trial/Pro
interface UsageLimitRepository {
    suspend fun usedInCurrentPeriod(feature: GatedFeature): Int
    suspend fun recordUse(feature: GatedFeature)
}
class CheckFeatureAccessUseCase   // invoke(feature): UseCaseState<FeatureAccess>
class RecordFeatureUseUseCase     // invoke(feature): UseCaseState<Unit>  (yalnız Free katmanda sayar)

// analytics/AnalyticsTracker.kt
interface AnalyticsTracker { fun track(event: String, params: Map<String, String> = emptyMap()) }
object AnalyticsEvents { /* onboarding_step_viewed, onboarding_completed, attribution_selected,
   commitment_confirmed, paywall_viewed, purchase_started, purchase_completed, trial_started,
   workout_started, workout_completed, set_completed, meal_photo_analyzed, next_meal_suggested,
   today_mode_used, coach_message_sent, weekly_report_opened, notification_permission_result */ }
class TrackEventUseCase

// preferences/UserPreferencesRepository.kt
interface UserPreferencesRepository {
    var attributionSource: String?      // tiktok, instagram, youtube, friend, gym, search, other
    var commitmentDaysPerWeek: Int?
    var commitmentConfirmedAt: String?
    var reminderHour: Int               // varsayılan 18
    var reminderMinute: Int             // varsayılan 0
    var notificationsEnabled: Boolean
    var streakFreezeUsedWeek: String?   // ISO hafta anahtarı, ör. "2026-W45"
}

// billing/BillingModels.kt
enum class SubscriptionPeriod { Monthly, Yearly }
data class SubscriptionProduct(val id: String, val period: SubscriptionPeriod, val priceLabel: String,
    val pricePerMonthLabel: String, val trialDays: Int, val savingsPercent: Int?)
interface BillingRepository {
    suspend fun getProducts(): Result<List<SubscriptionProduct>>
    suspend fun purchase(productId: String): Result<Unit>   // mock: aboneliği aktif eder
    suspend fun restore(): Result<Boolean>
}
class GetSubscriptionProductsUseCase; class PurchaseSubscriptionUseCase; class RestorePurchasesUseCase

// stats/WeeklyStats.kt  (AI sözleşmesinin de parçası)
@Serializable data class WeeklyStats(
    val weekKey: String, val sessionsCompleted: Int, val sessionsPlanned: Int,
    val totalVolumeKg: Double, val previousWeekVolumeKg: Double,
    val avgProteinG: Double, val proteinTargetG: Int, val avgCalories: Int, val calorieTarget: Int,
    val mealsLogged: Int, val weightChangeKg: Double? = null, val prCount: Int = 0)
class ComputeWeeklyStatsUseCase   // invoke(weekOffset: Int = 0): UseCaseState<WeeklyStats>
```

Mock fiyatlar: TR `179,99 ₺/ay`, `999,99 ₺/yıl` (7 gün trial, %54 tasarruf). EN `$12.99/mo`, `$44.99/yr`. Etiket dili cihaz diline göre seçilir.

## 5. Strings.kt bölge işaretleri

Foundation, `Strings.kt` içinde üç bölüme (interface, `StringsTr`, `StringsEn`) şu işaretleri ekler. Her ajan yalnız kendi işaretinin **hemen altına** yazar:

```
// region:foundation:interface   // region:foundation:tr   // region:foundation:en
// region:workout:interface      // region:workout:tr      // region:workout:en
// region:onboarding:interface   // region:onboarding:tr   // region:onboarding:en
// region:nutrition:interface    // region:nutrition:tr    // region:nutrition:en
// region:coach:interface        // region:coach:tr        // region:coach:en
// region:engagement:interface   // region:engagement:tr   // region:engagement:en
// region:notifications:interface // region:notifications:tr // region:notifications:en
```

## 6. AI çağrı sözleşmeleri (Backend ve Foundation aynı JSON'u uygular)

Tüm istekler `locale` ("tr" | "en") taşır. Cevap dili `locale` olur. Türk mutfağı ve Türkçe porsiyon dili ("1 kepçe", "yarım ekmek") tercih edilir.

| Callable | İstek | Cevap | Model |
|---|---|---|---|
| `suggestNextMeal` | `{ profile: UserProfilePayload, remainingCalories: Int, remainingProteinG, remainingCarbsG, remainingFatG: Double, mealsLoggedToday: [String], isTrainingDay: Boolean, locale }` | `{ title, description, items: [String], calories: Int, proteinG, carbsG, fatG: Double }` | vision/flash |
| `adaptTodaySession` | `{ profile, availableMinutes: Int, energy: Int(1-5), painAreas: [String], location: "gym"\|"home"\|"hotel"\|"outdoor", plannedExercises: [GeneratedExercise], locale }` | `{ title, note, exercises: [GeneratedExercise] }` | plan/pro |
| `coachChat` | `{ profile, contextSummary: String, history: [{ role: "user"\|"coach", text }], message: String, locale }` | `{ reply: String }` | flash |
| `weeklyCoachReport` | `{ profile, stats: WeeklyStats, locale }` | `{ headline, summary, highlights: [String], nextWeekFocus: [String] }` | flash |

Kotlin modelleri `core/domain/.../model/AiCoachModels.kt` içindedir: `NextMealRequest`, `NextMealSuggestion`, `TodayModeRequest`, `AdaptedSession`, `CoachMessage`, `CoachChatRequest`, `CoachChatReply`, `WeeklyReportRequest`, `WeeklyCoachReport`. `MockAIRepository` her çağrı için kural tabanlı çevrimdışı cevap üretir (Bugün Modu: süreye göre egzersiz kırpma, ağrı bölgesini çıkarma, ev/otelde vücut ağırlığına geçiş).

## 7. Dalga 2–4 özet görevleri

**Workout (Dalga 2).** `core/domain/training`: `ProgressiveOverload` saf fonksiyonu (son seans tüm setlerde üst tekrar sınırına ulaştıysa +2,5 kg üst vücut / +5 kg alt vücut; ulaşamadıysa aynı; iki seans üst üste düştüyse −%5), Epley e1RM ile PR tespiti. Aktif seansta önerilen ağırlık/tekrar ön dolu, "önceki seti kopyala", dinlenme sayacı. Egzersiz "Değiştir" (aynı kas grubu + ekipman statik alternatif kataloğu). Seans bitince özet ekranı (süre, hacim, PR). Bugün Modu sheet'i → `adaptTodaySession` → pasif ad-hoc plan → seans başlat. Kota: `GatedFeature.TodayMode`.

**Onboarding + Paywall (Dalga 2).** Yeni sıra: Hedef → Ad → Yaş → Boy → Kilo → Aktivite → Ekipman → Diyet → Kaynak sorusu → Üretim → Plan önizleme → Taahhüt (basılı tut) → Paywall. E-posta adımı kalkar (e-posta Auth'ta alınır). Paywall: `BillingRepository` ürünleri, yıllık varsayılan, tasarruf rozeti, "Deneme bitmeden 24 saat önce hatırlatırız" metni, geri yükleme. Analitik olayları.

**Nutrition (Dalga 3).** Antrenman günü makro ayarı (karbonhidrat +%20, yağ −%10; dinlenme günü karbonhidrat −%15, yağ +%10; protein sabit; kalori 4/4/9 ile yeniden hesap). Kalan makro kartı. Fotoğraf analizinden sonra "Sonraki öğün" önerisi (Pro; Free'de kilitli önizleme → paywall). Fotoğraf kotası göstergesi (Free 3/hafta).

**Coach (Dalga 3).** Yeni `feature/coach/{domain,data,presentation}` modülleri. Son 14 günün bağlam özeti, sohbet (Pro), haftalık rapor (Free: yerel istatistik + başlık; Pro: tam AI raporu, ISO hafta başına önbellek). Sohbet geçmişi Settings'te JSON (son 30 mesaj). Dışa açılan composable: `CoachScreen(onBack, onNavigatePaywall)`.

**Engagement + Home/Progress (Dalga 4).** `core/domain/engagement`: plana duyarlı seri (planlı dinlenme günü bozmaz; gün, antrenman veya öğün kaydıyla aktif olur; haftada 1 dondurma), XP (seans +100, PR +50, protein hedefi +30, fotoğraflı öğün +20), rozetler, Gün 0 listesi (ilk seans, ilk öğün fotoğrafı, ilk tartı). `ProgressRepository` (`body_measurements` tablosu mevcut). Ana sayfa kartları, ilerleme sekmesi (kilo trendi, PR listesi, haftalık hacim, rozetler, tartı girişi), Ana sayfada AI Koç kartı → `CoachScreen`.

**Notifications (Dalga 4).** `core/domain/notifications/ReminderScheduler` + Android (AlarmManager + BroadcastReceiver + kanal + BOOT_COMPLETED) ve iOS (`UNUserNotificationCenter`) uygulamaları. Antrenman günlerinde hatırlatma, öğle öğün hatırlatması, D0–D7 dizisi, trial bitişinden 24 saat önce hatırlatma. Paywall sonrası izin ekranı ("Antrenman saatini hatırlatayım mı?" + saat seçimi). Bildirim metinleri yerelleştirilmiş olarak dışarıdan verilir.

## 8. Bitti tanımı

- `./gradlew :composeApp:assembleDebug` başarılı.
- Firebase Functions `npm run build` başarılı.
- Sabit metin yok; TR ve EN dolu.
- Yeni ekranlar Koin ve navigasyona bağlı.
- `MEMORY_BANK.md` en üstte tarihli özet.
