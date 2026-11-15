package com.fitai.app.ai

import com.fitai.app.model.AdaptedSession
import com.fitai.app.model.CoachChatReply
import com.fitai.app.model.CoachChatRequest
import com.fitai.app.model.GeneratedExercise
import com.fitai.app.model.NextMealRequest
import com.fitai.app.model.NextMealSuggestion
import com.fitai.app.model.TodayLocation
import com.fitai.app.model.TodayModeRequest
import com.fitai.app.model.WeeklyCoachReport
import com.fitai.app.model.WeeklyReportRequest
import kotlin.math.abs
import kotlin.math.roundToInt

/** Rule-based offline answers for the coach callables; text follows `request.locale`. */
internal object MockCoachAnswers {

    private const val MINUTES_PER_EXERCISE = 8

    fun adaptTodaySession(request: TodayModeRequest): AdaptedSession {
        val en = request.locale == "en"
        val painAreas = request.painAreas.flatMap { bodyAreasOf(it) }.toSet()
        val atGym = request.location == TodayLocation.GYM
        val lowEnergy = request.energy <= 2

        val safe = request.plannedExercises.filter { bodyAreasOf(it.muscleGroup).none { area -> area in painAreas } }
        val usedNames = mutableSetOf<String>()
        val adapted = safe
            .map { if (atGym) it else it.toBodyweight(en, usedNames) }
            .map { if (lowEnergy) it.copy(sets = (it.sets - 1).coerceAtLeast(2)) else it }
            .distinctBy { it.name }
        val maxExercises = (request.availableMinutes / MINUTES_PER_EXERCISE).coerceAtLeast(1)
        val dayOfWeek = request.plannedExercises.firstOrNull()?.dayOfWeek ?: 1
        val exercises = adapted.take(maxExercises).ifEmpty { listOf(mobilityExercise(dayOfWeek, en)) }

        val notes = buildList {
            if (adapted.size > maxExercises) {
                add(if (en) "Trimmed to ${exercises.size} exercises to fit ${request.availableMinutes} min." else "${request.availableMinutes} dakikaya sığması için ${exercises.size} egzersize indirildi.")
            }
            if (safe.size < request.plannedExercises.size) {
                add(if (en) "Skipped moves that load the painful area." else "Ağrıyan bölgeyi zorlayan hareketler çıkarıldı.")
            }
            if (!atGym) {
                add(if (en) "Swapped to bodyweight alternatives." else "Vücut ağırlığı alternatiflerine geçildi.")
            }
            if (lowEnergy) {
                add(if (en) "Low energy: one set less per exercise." else "Enerji düşük: her harekette bir set az.")
            }
        }
        return AdaptedSession(
            title = if (en) "Today's session · ${request.availableMinutes} min" else "Bugünün seansı · ${request.availableMinutes} dk",
            note = notes.joinToString(" ").ifEmpty {
                if (en) "Your plan fits today. Go for it!" else "Planın bugüne uyuyor. Hadi başlayalım!"
            },
            exercises = exercises,
        )
    }

    fun suggestNextMeal(request: NextMealRequest): NextMealSuggestion {
        val en = request.locale == "en"
        val plantBased = request.profile.dietRestrictions.lowercase().let { "vegan" in it || "vegetarian" in it }
        val loggedToday = request.mealsLoggedToday.map { it.lowercase() }
        val candidates = mealTemplates
            .filter { !(plantBased && it.hasMeat) }
            .filter { template -> loggedToday.none { it.contains(template.titleTr.lowercase()) || it.contains(template.titleEn.lowercase()) } }
            .ifEmpty { mealTemplates.filter { !(plantBased && it.hasMeat) } }

        val remaining = request.remainingCalories
        val proteinShare = if (remaining > 0) request.remainingProteinG * 4 / remaining else 0.0
        val template = when {
            remaining < 350 -> candidates.firstOrNull { it.isSnack }
            proteinShare >= 0.3 -> candidates.filterNot { it.isSnack }.maxByOrNull { it.proteinG / it.calories }
            request.isTrainingDay -> candidates.filterNot { it.isSnack }.maxByOrNull { it.carbsG / it.calories }
            else -> candidates.filterNot { it.isSnack }.let { it.getOrNull(loggedToday.size % it.size.coerceAtLeast(1)) }
        } ?: candidates.first()

        val scale = if (remaining <= 0) 0.5 else (remaining.toDouble() / template.calories).coerceIn(0.5, 1.4)
        val calories = (template.calories * scale).roundToInt()
        return NextMealSuggestion(
            title = if (en) template.titleEn else template.titleTr,
            description = if (en) {
                "Fits your remaining ${remaining.coerceAtLeast(0)} kcal and ${request.remainingProteinG.pretty()} g protein. Portion ×${scale.pretty()}."
            } else {
                "Kalan ${remaining.coerceAtLeast(0)} kcal ve ${request.remainingProteinG.pretty()} g proteine göre. Porsiyon ×${scale.pretty()}."
            },
            items = if (en) template.itemsEn else template.itemsTr,
            calories = calories,
            proteinG = (template.proteinG * scale).roundTo1(),
            carbsG = (template.carbsG * scale).roundTo1(),
            fatG = (template.fatG * scale).roundTo1(),
        )
    }

    fun coachChat(request: CoachChatRequest): CoachChatReply {
        val en = request.locale == "en"
        val message = request.message.lowercase()
        val topic = when {
            listOf("protein", "yemek", "öğün", "meal", "diet", "diyet", "kalori", "calorie").any { it in message } -> Topic.Nutrition
            listOf("ağrı", "agri", "sakat", "pain", "injur", "hurt").any { it in message } -> Topic.Pain
            listOf("kilo", "weight", "tartı", "scale").any { it in message } -> Topic.Weight
            listOf("motiv", "üşen", "lazy", "tired", "yorgun").any { it in message } -> Topic.Motivation
            listOf("uyku", "sleep", "dinlen", "rest").any { it in message } -> Topic.Recovery
            else -> Topic.Training
        }
        val greeting = request.profile.name.substringBefore(' ').let { if (it.isBlank()) "" else "$it, " }
        val advice = if (en) topic.en else topic.tr
        val context = request.contextSummary.trim().substringBefore('\n').take(140)
        val contextLine = when {
            context.isEmpty() -> ""
            en -> " Based on your recent data: $context"
            else -> " Son verilerine göre: $context"
        }
        return CoachChatReply(reply = "$greeting$advice$contextLine".replaceFirstChar { it.uppercase() })
    }

    fun weeklyCoachReport(request: WeeklyReportRequest): WeeklyCoachReport {
        val en = request.locale == "en"
        val stats = request.stats
        val adherence = if (stats.sessionsPlanned > 0) stats.sessionsCompleted.toDouble() / stats.sessionsPlanned else 0.0
        val volumeChangePct = if (stats.previousWeekVolumeKg > 0) {
            ((stats.totalVolumeKg - stats.previousWeekVolumeKg) / stats.previousWeekVolumeKg * 100).roundToInt()
        } else {
            null
        }
        val proteinOk = stats.proteinTargetG > 0 && stats.avgProteinG >= stats.proteinTargetG * 0.9

        val headline = when {
            adherence >= 1.0 -> if (en) "Perfect week — every planned session done!" else "Kusursuz hafta — planlanan tüm seanslar tamam!"
            adherence >= 0.6 -> if (en) "Solid week, keep the momentum." else "Sağlam bir hafta, ivmeyi koru."
            stats.sessionsCompleted > 0 -> if (en) "A start is a start — let's build on it." else "Başlamak yarısıdır — üzerine koyalım."
            else -> if (en) "New week, fresh start." else "Yeni hafta, yeni başlangıç."
        }
        val summary = if (en) {
            "You completed ${stats.sessionsCompleted}/${stats.sessionsPlanned} sessions, lifted ${stats.totalVolumeKg.pretty()} kg in total and logged ${stats.mealsLogged} meals."
        } else {
            "${stats.sessionsCompleted}/${stats.sessionsPlanned} seans tamamladın, toplam ${stats.totalVolumeKg.pretty()} kg kaldırdın ve ${stats.mealsLogged} öğün kaydettin."
        }
        val highlights = buildList {
            volumeChangePct?.let {
                add(
                    when {
                        en && it >= 0 -> "Volume up $it% vs last week."
                        en -> "Volume down ${abs(it)}% vs last week."
                        it >= 0 -> "Hacim geçen haftaya göre %$it arttı."
                        else -> "Hacim geçen haftaya göre %${abs(it)} azaldı."
                    },
                )
            }
            if (stats.proteinTargetG > 0) {
                add(
                    if (en) {
                        "Average protein ${stats.avgProteinG.pretty()} g / ${stats.proteinTargetG} g target."
                    } else {
                        "Ortalama protein ${stats.avgProteinG.pretty()} g / hedef ${stats.proteinTargetG} g."
                    },
                )
            }
            stats.weightChangeKg?.let {
                add(if (en) "Weight change: ${it.signed()} kg." else "Kilo değişimi: ${it.signed()} kg.")
            }
            if (stats.prCount > 0) {
                add(if (en) "${stats.prCount} new personal records!" else "${stats.prCount} yeni kişisel rekor!")
            }
        }
        val focus = buildList {
            if (adherence < 1.0 && stats.sessionsPlanned > 0) {
                add(if (en) "Hit all ${stats.sessionsPlanned} planned sessions." else "Planlanan ${stats.sessionsPlanned} seansın hepsini tamamla.")
            }
            if (stats.proteinTargetG > 0 && !proteinOk) {
                add(if (en) "Add a protein source to every meal." else "Her öğüne bir protein kaynağı ekle.")
            }
            if (stats.mealsLogged < 14) {
                add(if (en) "Log at least two meals a day." else "Günde en az iki öğün kaydet.")
            }
            if (isEmpty()) {
                add(if (en) "Add a little weight or one rep to your main lifts." else "Ana hareketlerde biraz ağırlık ya da bir tekrar ekle.")
            }
        }
        return WeeklyCoachReport(headline = headline, summary = summary, highlights = highlights, nextWeekFocus = focus)
    }

    private enum class Topic(val tr: String, val en: String) {
        Nutrition(
            tr = "öğünlerini proteinle kur: her tabağa avuç içi kadar tavuk, yumurta, yoğurt ya da baklagil ekle.",
            en = "build each meal around protein: add a palm-sized portion of chicken, eggs, yogurt or legumes.",
        ),
        Pain(
            tr = "ağrıyan bölgeyi zorlama; bugün o bölgeyi çalıştırmayan hareketleri seç, ağrı sürerse bir uzmana görün.",
            en = "don't push through pain; pick moves that spare that area today and see a professional if it persists.",
        ),
        Weight(
            tr = "tartıyı haftada 2-3 kez aynı saatte kullan ve günlük değil haftalık ortalamaya bak.",
            en = "weigh in 2-3 times a week at the same time and watch the weekly average, not daily swings.",
        ),
        Motivation(
            tr = "bugün sadece 10 dakika başla; çoğu zaman başlamak devam etmeyi getirir.",
            en = "commit to just 10 minutes today; starting is usually what gets you going.",
        ),
        Recovery(
            tr = "toparlanma da antrenmanın parçası: 7-9 saat uyku ve yeterli su performansını doğrudan artırır.",
            en = "recovery is part of training: 7-9 hours of sleep and enough water directly boost performance.",
        ),
        Training(
            tr = "planına sadık kal ve her seansta bir öncekinden biraz daha fazlasını hedefle.",
            en = "stick to your plan and aim for a little more than last time in every session.",
        ),
    }

    private class MealTemplate(
        val titleTr: String,
        val titleEn: String,
        val itemsTr: List<String>,
        val itemsEn: List<String>,
        val calories: Int,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val hasMeat: Boolean = false,
        val isSnack: Boolean = false,
    )

    private val mealTemplates = listOf(
        MealTemplate(
            titleTr = "Izgara tavuk ve bulgur pilavı",
            titleEn = "Grilled chicken with bulgur pilaf",
            itemsTr = listOf("150 g ızgara tavuk göğsü", "1 kepçe bulgur pilavı", "Çoban salata"),
            itemsEn = listOf("150 g grilled chicken breast", "1 ladle bulgur pilaf", "Shepherd's salad"),
            calories = 550, proteinG = 45.0, carbsG = 55.0, fatG = 14.0, hasMeat = true,
        ),
        MealTemplate(
            titleTr = "Izgara köfte ve piyaz",
            titleEn = "Grilled köfte with bean salad",
            itemsTr = listOf("5 adet ızgara köfte", "1 kase piyaz", "Yeşil salata"),
            itemsEn = listOf("5 grilled köfte", "1 bowl white bean salad", "Green salad"),
            calories = 580, proteinG = 40.0, carbsG = 30.0, fatG = 32.0, hasMeat = true,
        ),
        MealTemplate(
            titleTr = "Mercimek çorbası ve yoğurt",
            titleEn = "Lentil soup with yogurt",
            itemsTr = listOf("1 kase mercimek çorbası", "Yarım ekmek", "1 kase yoğurt"),
            itemsEn = listOf("1 bowl lentil soup", "Half a bread", "1 bowl yogurt"),
            calories = 450, proteinG = 22.0, carbsG = 65.0, fatG = 10.0,
        ),
        MealTemplate(
            titleTr = "Kuru fasulye ve bulgur pilavı",
            titleEn = "White beans with bulgur pilaf",
            itemsTr = listOf("1 kepçe kuru fasulye", "1 kepçe bulgur pilavı", "1 kase cacık"),
            itemsEn = listOf("1 ladle white bean stew", "1 ladle bulgur pilaf", "1 bowl cacık"),
            calories = 600, proteinG = 28.0, carbsG = 90.0, fatG = 12.0,
        ),
        MealTemplate(
            titleTr = "Menemen ve beyaz peynir",
            titleEn = "Menemen with feta",
            itemsTr = listOf("2 yumurtalı menemen", "1 dilim tam buğday ekmeği", "1 dilim beyaz peynir"),
            itemsEn = listOf("2-egg menemen", "1 slice whole wheat bread", "1 slice feta"),
            calories = 480, proteinG = 26.0, carbsG = 35.0, fatG = 26.0,
        ),
        MealTemplate(
            titleTr = "Süzme yoğurt, ceviz ve muz",
            titleEn = "Strained yogurt, walnuts and banana",
            itemsTr = listOf("1 kase süzme yoğurt", "3 adet ceviz", "1 küçük muz"),
            itemsEn = listOf("1 bowl strained yogurt", "3 walnuts", "1 small banana"),
            calories = 300, proteinG = 18.0, carbsG = 32.0, fatG = 11.0, isSnack = true,
        ),
    )

    private enum class BodyArea { Legs, Back, Chest, Shoulders, Arms, Core }

    private val areaKeywords = mapOf(
        BodyArea.Legs to listOf("bacak", "leg", "diz", "knee", "kalça", "hip", "glute", "quad", "hamstring", "baldır", "calf", "ayak bileği", "ankle"),
        BodyArea.Back to listOf("sırt", "sirt", "back", "bel", "lower back", "lat"),
        BodyArea.Chest to listOf("göğüs", "gogus", "chest", "pec"),
        BodyArea.Shoulders to listOf("omuz", "shoulder", "delt"),
        BodyArea.Arms to listOf("kol", "arm", "biceps", "triceps", "dirsek", "elbow", "bilek", "wrist"),
        BodyArea.Core to listOf("karın", "karin", "core", "abs", "abdominal"),
    )

    private fun bodyAreasOf(text: String): Set<BodyArea> {
        val normalized = text.lowercase()
        return areaKeywords.filterValues { keywords -> keywords.any { it in normalized } }.keys
    }

    private val bodyweightNames = listOf("push-up", "pull-up", "chin-up", "lunge", "plank", "dip", "şınav", "barfiks", "burpee", "squat jump", "bodyweight", "vücut ağırlığı")

    private fun bodyweightAlternatives(area: BodyArea?, en: Boolean): List<String> = when (area) {
        BodyArea.Legs -> if (en) listOf("Bodyweight squat", "Reverse lunge", "Glute bridge") else listOf("Vücut ağırlığı squat", "Geri lunge", "Kalça köprüsü")
        BodyArea.Back -> if (en) listOf("Superman hold", "Towel row") else listOf("Superman", "Havlu ile row")
        BodyArea.Chest -> if (en) listOf("Push-up", "Incline push-up") else listOf("Şınav", "Eğimli şınav")
        BodyArea.Shoulders -> if (en) listOf("Pike push-up", "Arm circles") else listOf("Pike şınav", "Kol çevirme")
        BodyArea.Arms -> if (en) listOf("Bench dip", "Diamond push-up") else listOf("Sandalye dip", "Elmas şınav")
        BodyArea.Core -> listOf(PLANK, "Dead bug")
        null -> listOf("Burpee", "Jumping jack")
    }

    private fun GeneratedExercise.toBodyweight(en: Boolean, usedNames: MutableSet<String>): GeneratedExercise {
        if (bodyweightNames.any { it in name.lowercase() }) return this.also { usedNames += name }
        val options = bodyweightAlternatives(bodyAreasOf(muscleGroup).firstOrNull(), en)
        val replacement = options.firstOrNull { it !in usedNames } ?: options.first()
        usedNames += replacement
        val reps = when {
            replacement != PLANK -> "12-15"
            en -> "30-45 s"
            else -> "30-45 sn"
        }
        return copy(name = replacement, reps = reps)
    }

    private const val PLANK = "Plank"

    private fun mobilityExercise(dayOfWeek: Int, en: Boolean) = GeneratedExercise(
        dayOfWeek = dayOfWeek,
        name = if (en) "Full-body mobility flow" else "Tüm vücut mobilite akışı",
        sets = 2,
        reps = if (en) "5 min" else "5 dk",
        restSeconds = 30,
        muscleGroup = if (en) "Mobility" else "Mobilite",
    )

    private fun Double.roundTo1(): Double = (this * 10).roundToInt() / 10.0

    private fun Double.pretty(): String {
        val rounded = roundTo1()
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    private fun Double.signed(): String = if (this > 0) "+${pretty()}" else pretty()
}
