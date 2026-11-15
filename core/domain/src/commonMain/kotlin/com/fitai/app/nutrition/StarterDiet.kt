package com.fitai.app.nutrition

import com.fitai.app.model.DietRestriction
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodTags
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.MealSlot
import com.fitai.app.model.PlanFood
import com.fitai.app.model.PlanMeal
import com.fitai.app.model.PlanMealOption
import com.fitai.app.model.UserProfilePayload
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.model.roundToTenth
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Rule-based meals for a set of daily targets, built from the food database: the starter plan for free users,
 * the dev stand-in for AI, and the fallback when the server returns targets only. Each meal gets the best
 * fitting template plus two alternatives that respect the diet restrictions; amounts are scaled so every
 * option meets the meal's share of the calories and protein.
 */
object StarterDiet {

    fun meals(targets: GeneratedDietPlan, profile: UserProfilePayload, foods: List<FoodItem>, locale: String): List<PlanMeal> {
        val byId = foods.associateBy { it.id }
        val restrictions = decodeProfileCodes(profile.dietRestrictions) - DietRestriction.None.code
        val goal = FitnessGoal.fromCode(profile.goal)
        val plantDiet = DietRestriction.Vegan.code in restrictions || DietRestriction.Vegetarian.code in restrictions
        // A calorie deficit leaves little room, so fat loss starts from the leanest, most protein-dense plates.
        val proteinFirst = goal == FitnessGoal.LoseFat
        val seed = (profile.age.toInt() + profile.weightKg.roundToInt()).coerceAtLeast(0)
        val usedMains = mutableSetOf<Template>()
        return slotShares(targets.dailyCalories, goal).mapNotNull { (slot, share) ->
            val kcal = targets.dailyCalories * share
            val protein = targets.proteinG * share
            // Protein goals get the most protein-dense plates first; others the curated order with a per-profile
            // rotation for variety. Tofu-style plates come last for omnivores.
            val (plantForward, classic) = templatesFor(slot)
                .filter { template -> template.isCompatible(byId, restrictions) }
                .partition { it.plantForward && !plantDiet }
            val ordered = if (proteinFirst) classic.sortedByDescending { it.proteinDensity(byId) } else classic.rotate(seed + slot.ordinal)
            // Two snacks of the same day don't open with the same plate.
            val candidates = (ordered + plantForward).sortedBy { it in usedMains }
            if (candidates.isEmpty()) return@mapNotNull null
            usedMains += candidates.first()
            val options = candidates.take(OPTIONS_PER_MEAL).map { it.build(byId, kcal, protein, locale) }
            PlanMeal(slot = slot.code, time = slot.defaultTime, options = options, selected = 0)
        }
    }

    /** Meal split: four meals, five with a mid-morning snack for muscle gain or big calorie budgets. */
    fun slotShares(calories: Int, goal: FitnessGoal?): List<Pair<MealSlot, Double>> =
        if (goal == FitnessGoal.BuildMuscle || calories >= FIVE_MEALS_FROM_KCAL) {
            listOf(
                MealSlot.Breakfast to 0.25,
                MealSlot.MorningSnack to 0.10,
                MealSlot.Lunch to 0.30,
                MealSlot.AfternoonSnack to 0.10,
                MealSlot.Dinner to 0.25,
            )
        } else {
            listOf(
                MealSlot.Breakfast to 0.25,
                MealSlot.Lunch to 0.35,
                MealSlot.AfternoonSnack to 0.10,
                MealSlot.Dinner to 0.30,
            )
        }

    private fun templatesFor(slot: MealSlot): List<Template> = when (slot) {
        MealSlot.Breakfast -> breakfast
        MealSlot.Lunch -> lunch
        MealSlot.Dinner -> dinner
        MealSlot.MorningSnack, MealSlot.AfternoonSnack, MealSlot.EveningSnack -> snacks
    }

    private fun <T> List<T>.rotate(by: Int): List<T> = if (isEmpty()) this else (by % size).let { drop(it) + take(it) }

    /** p: protein source (scaled for protein), c: carbs and the rest (scaled for calories), x: fixed amount. */
    private enum class Role { P, C, X }

    private class Item(val foodId: String, val amount: Double, val unit: String, val role: Role)

    private class Template(
        val titleTr: String,
        val titleEn: String,
        val noteTr: String,
        val noteEn: String,
        val items: List<Item>,
        /** Built around soy, tofu or chia; offered last unless the diet is vegetarian or vegan. */
        val plantForward: Boolean = false,
    ) {
        fun isCompatible(byId: Map<String, FoodItem>, restrictions: Set<String>): Boolean {
            val foods = items.map { byId[it.foodId] ?: return false }
            if (foods.any { !it.isAllowedFor(restrictions) }) return false
            if (DietRestriction.Kosher.code in restrictions) {
                val meat = foods.any { FoodTags.MEAT in it.tags || FoodTags.POULTRY in it.tags }
                if (meat && foods.any { FoodTags.DAIRY in it.tags }) return false
            }
            return true
        }

        /** Share of the template's calories that comes from protein, at its base amounts. */
        fun proteinDensity(byId: Map<String, FoodItem>): Double {
            val totals = items.map { item -> byId.getValue(item.foodId).macrosFor(item.unit, item.amount) }
            val kcal = totals.sumOf { it.calories }
            return if (kcal > 0) totals.sumOf { it.proteinG } * 4 / kcal else 0.0
        }

        fun build(byId: Map<String, FoodItem>, kcal: Double, protein: Double, locale: String): PlanMealOption {
            val (p, c) = scaleFactors(byId, kcal, protein)
            val foods = items.map { item ->
                val food = byId.getValue(item.foodId)
                val factor = when (item.role) {
                    Role.P -> p
                    Role.C -> c
                    Role.X -> 1.0
                }
                val amount = roundAmount(item.amount * factor, item.unit)
                val grams = (food.gramsOf(item.unit) ?: 1.0) * amount
                val macros = food.macrosFor(grams)
                PlanFood(
                    foodId = food.id,
                    name = food.name(locale),
                    amount = amount,
                    unit = if (food.gramsOf(item.unit) != null) item.unit else FoodUnits.GRAM,
                    grams = grams.roundToTenth(),
                    calories = macros.calories.roundToInt(),
                    proteinG = macros.proteinG.roundToTenth(),
                    carbsG = macros.carbsG.roundToTenth(),
                    fatG = macros.fatG.roundToTenth(),
                )
            }
            val en = locale == "en"
            return PlanMealOption(title = if (en) titleEn else titleTr, note = if (en) noteEn else noteTr, foods = foods)
        }

        /**
         * Solves protein-item and other-item factors so the meal hits [kcal] and [protein]; falls back to one
         * factor on calories when the template can't move protein independently.
         */
        private fun scaleFactors(byId: Map<String, FoodItem>, kcal: Double, protein: Double): Pair<Double, Double> {
            fun sum(role: Role, pick: (com.fitai.app.model.Macros) -> Double) =
                items.filter { it.role == role }.sumOf { pick(byId.getValue(it.foodId).macrosFor(it.unit, it.amount)) }
            val kp = sum(Role.P) { it.calories }
            val kc = sum(Role.C) { it.calories }
            val kx = sum(Role.X) { it.calories }
            val pp = sum(Role.P) { it.proteinG }
            val pc = sum(Role.C) { it.proteinG }
            val px = sum(Role.X) { it.proteinG }
            val kcalLeft = kcal - kx
            val proteinLeft = protein - px
            val det = pp * kc - pc * kp
            if (kp > 0 && kc > 0 && abs(det) > 1e-6) {
                // Protein first, within realistic portions; then the rest fills the calories. When the rest
                // hits its limit, calories win and the protein items take up the difference.
                val a = (((proteinLeft * kc) - (pc * kcalLeft)) / det).coerceIn(MIN_PROTEIN_FACTOR, MAX_PROTEIN_FACTOR)
                val bRaw = (kcalLeft - a * kp) / kc
                val b = bRaw.coerceIn(MIN_FACTOR, MAX_FACTOR)
                if (b == bRaw) return a to b
                val proteinCap = if (pp > 0) (proteinLeft * PROTEIN_OVERSHOOT - b * pc) / pp else MAX_PROTEIN_FACTOR
                val a2 = minOf((kcalLeft - b * kc) / kp, proteinCap).coerceIn(MIN_PROTEIN_FACTOR, MAX_PROTEIN_FACTOR)
                return a2 to b
            }
            val flex = kp + kc
            val single = if (flex > 0) (kcalLeft / flex).coerceIn(MIN_FACTOR, MAX_FACTOR) else 1.0
            return single to single
        }
    }

    /** Household amounts: halves for pieces and spoons, whole pieces from three up, 5–10 g steps for weights. */
    internal fun roundAmount(amount: Double, unit: String): Double {
        val step = when (unit) {
            FoodUnits.GRAM, FoodUnits.ML -> if (amount >= 50) 10.0 else 5.0
            FoodUnits.PIECE, FoodUnits.SLICE, FoodUnits.HANDFUL -> if (amount >= 3) 1.0 else 0.5
            else -> 0.5
        }
        val rounded = floor(amount / step + 0.5) * step
        return rounded.coerceAtLeast(step)
    }

    private fun t(titleTr: String, titleEn: String, noteTr: String, noteEn: String, vararg items: Item) =
        Template(titleTr, titleEn, noteTr, noteEn, items.toList())

    private fun plant(titleTr: String, titleEn: String, noteTr: String, noteEn: String, vararg items: Item) =
        Template(titleTr, titleEn, noteTr, noteEn, items.toList(), plantForward = true)

    private fun p(id: String, amount: Double, unit: String) = Item(id, amount, unit, Role.P)
    private fun c(id: String, amount: Double, unit: String) = Item(id, amount, unit, Role.C)
    private fun x(id: String, amount: Double, unit: String) = Item(id, amount, unit, Role.X)

    private const val G = FoodUnits.GRAM
    private const val PIECE = FoodUnits.PIECE
    private const val SLICE = FoodUnits.SLICE
    private const val PORTION = FoodUnits.PORTION
    private const val BOWL = FoodUnits.BOWL
    private const val GLASS = FoodUnits.GLASS
    private const val TBSP = FoodUnits.TBSP
    private const val TSP = FoodUnits.TSP
    private const val HANDFUL = FoodUnits.HANDFUL
    private const val SCOOP = FoodUnits.SCOOP

    private val breakfast = listOf(
        t(
            "Klasik Türk kahvaltısı", "Classic Turkish breakfast",
            "Ekmeği tam buğdaydan seç, çayı şekersiz iç.", "Pick whole-wheat bread and drink your tea unsweetened.",
            x("egg_boiled", 2.0, PIECE), p("cheese_white", 1.0, SLICE), x("olive_black", 5.0, PIECE),
            x("tomato", 1.0, PIECE), x("cucumber", 0.5, PIECE), c("bread_whole_wheat", 2.0, SLICE),
        ),
        t(
            "Menemen tabağı", "Menemen plate",
            "Az yağda pişir, biberi bol koy.", "Cook it in little oil with plenty of peppers.",
            p("menemen", 250.0, G), c("bread_whole_wheat", 1.0, SLICE), x("cucumber", 0.5, PIECE),
        ),
        t(
            "Sütlü yulaf kasesi", "Oats with milk",
            "Yulafı sütte 3–4 dk pişir ya da akşamdan ıslat.", "Simmer the oats in milk for 3–4 min or soak them overnight.",
            c("oats", 5.0, TBSP), p("milk_semi", 1.0, GLASS), x("banana", 1.0, PIECE), x("walnuts", 3.0, PIECE),
        ),
        t(
            "Süzme yoğurt ve granola", "Strained yogurt and granola",
            "Meyveyi yoğurdun üstüne doğra, balı en son ekle.", "Chop the fruit over the yogurt and add the honey last.",
            p("yogurt_strained", 1.0, BOWL), c("granola", 3.0, TBSP), x("strawberry", 1.0, GLASS), x("honey", 1.0, TSP),
        ),
        t(
            "Kaşarlı tam buğday tost", "Whole-wheat cheese toastie",
            "Tostu yağsız tavada ya da tost makinesinde yap.", "Toast it in a dry pan or a sandwich press.",
            c("bread_whole_wheat", 2.0, SLICE), p("cheese_kashar", 2.0, SLICE), x("tomato", 1.0, PIECE), x("green_pepper", 2.0, PIECE),
        ),
        t(
            "Omlet ve pirinç patlağı", "Omelette with rice cakes",
            "Omleti bir tatlı kaşığı zeytinyağında pişir.", "Cook the omelette in a teaspoon of olive oil.",
            p("omelette", 120.0, G), x("cheese_white", 1.0, SLICE), x("tomato", 1.0, PIECE), c("rice_cake", 2.0, PIECE),
        ),
        t(
            "Yumurta ve avokado", "Eggs and avocado",
            "Avokadoyu ezip ekmeğe sür, üstüne pul biber serp.", "Mash the avocado on the bread and sprinkle chili flakes.",
            p("egg_boiled", 2.0, PIECE), x("avocado", 0.5, PIECE), c("bread_whole_wheat", 2.0, SLICE), x("tomato", 1.0, PIECE),
        ),
        plant(
            "Sebzeli tofu kahvaltısı", "Tofu and vegetable scramble",
            "Tofuyu ufalayıp domates ve biberle 5 dk sotele.", "Crumble the tofu and saute it with tomato and pepper for 5 min.",
            p("tofu", 150.0, G), x("tomato", 1.0, PIECE), x("green_pepper", 2.0, PIECE), x("olive_oil", 1.0, TSP),
            c("bread_whole_wheat", 1.0, SLICE), x("olive_black", 5.0, PIECE),
        ),
        plant(
            "Fıstık ezmeli muzlu yulaf", "Peanut butter banana oats",
            "Yulafı bitki sütünde pişir, muzu dilimleyip ekle.", "Cook the oats in plant milk and top with sliced banana.",
            c("oats", 5.0, TBSP), p("milk_soy", 1.0, GLASS), x("banana", 1.0, PIECE), c("peanut_butter", 1.0, TBSP),
        ),
        plant(
            "Chia puding", "Chia pudding",
            "Chia'yı soya sütüyle karıştırıp en az 4 saat buzdolabında beklet.", "Stir chia into soy milk and chill for at least 4 hours.",
            c("chia", 2.0, TBSP), p("milk_soy", 1.0, GLASS), x("banana", 1.0, PIECE), c("peanut_butter", 1.0, TBSP),
        ),
        t(
            "Nohutlu avokado tabağı", "Chickpea and avocado plate",
            "Nohudu limon ve kimyonla ez, sebzelerle ye.", "Mash the chickpeas with lemon and cumin; eat with the vegetables.",
            p("chickpeas_boiled", 0.5, GLASS), x("avocado", 0.5, PIECE), x("tomato", 1.0, PIECE), x("cucumber", 0.5, PIECE),
            c("rice_cake", 2.0, PIECE),
        ),
    )

    private val snacks = listOf(
        t(
            "Yoğurt ve meyve", "Yogurt and fruit",
            "Şekersiz yoğurdu taze meyveyle tatlandır.", "Sweeten plain yogurt with fresh fruit.",
            p("yogurt_strained", 1.0, BOWL), c("strawberry", 0.5, GLASS),
        ),
        t(
            "Elma ve ceviz", "Apple and walnuts",
            "Cevizi önceden ölç; kuruyemiş kalorisi hızlı birikir.", "Measure the walnuts first; nut calories add up fast.",
            x("apple", 1.0, PIECE), c("walnuts", 4.0, PIECE),
        ),
        t(
            "Ayran ve leblebi", "Ayran and roasted chickpeas",
            "Tuzu az ayranı tercih et.", "Pick a low-salt ayran.",
            x("ayran", 1.0, GLASS), p("leblebi", 1.0, HANDFUL),
        ),
        t(
            "Protein shake", "Protein shake",
            "Antrenmandan sonraki bir saat içinde iç.", "Drink it within an hour after training.",
            x("whey", 1.0, SCOOP), c("milk_semi", 1.0, GLASS), c("banana", 0.5, PIECE),
        ),
        t(
            "Humus ve havuç", "Hummus and carrots",
            "Havucu çubuk şeklinde doğra, humusa batır.", "Cut the carrots into sticks for dipping.",
            p("hummus", 3.0, TBSP), x("carrot", 2.0, PIECE),
        ),
        t(
            "Muz ve badem", "Banana and almonds",
            "Bademi çiğ ya da kuru kavrulmuş seç.", "Choose raw or dry-roasted almonds.",
            x("banana", 1.0, PIECE), c("almonds", 15.0, PIECE),
        ),
        plant(
            "Edamame", "Edamame",
            "Haşlayıp üstüne bir tutam tuz serp.", "Boil and sprinkle with a pinch of salt.",
            p("edamame", 100.0, G),
        ),
        t(
            "Kuru kayısı ve fındık", "Dried apricots and hazelnuts",
            "Kükürtsüz (koyu renkli) kayısıyı tercih et.", "Prefer unsulphured (darker) apricots.",
            x("apricot_dried", 4.0, PIECE), c("hazelnuts", 0.5, HANDFUL),
        ),
    )

    private val lunch = listOf(
        t(
            "Izgara tavuk ve bulgur pilavı", "Grilled chicken with bulgur",
            "Tavuğu yoğurt ve baharatla 30 dk marine et.", "Marinate the chicken in yogurt and spices for 30 min.",
            p("chicken_breast_grilled", 120.0, G), c("bulgur_pilaf", 1.0, PORTION), x("shepherd_salad", 1.0, PORTION),
        ),
        t(
            "Mercimek çorbası ve tavuklu salata", "Lentil soup and chicken salad",
            "Çorbaya limon sık, salatayı zeytinyağıyla az soslandır.", "Squeeze lemon into the soup; dress the salad lightly.",
            x("lentil_soup", 1.0, BOWL), p("chicken_salad", 1.0, PORTION), c("bread_whole_wheat", 1.0, SLICE),
        ),
        t(
            "Etli kuru fasulye, bulgur, cacık", "Bean stew with bulgur and cacik",
            "Pilavı yarım porsiyon tut, fasulyeyi bol ye.", "Keep the bulgur to half a portion and fill up on beans.",
            p("beans_meat", 1.0, PORTION), c("bulgur_pilaf", 0.5, PORTION), x("cacik", 0.5, BOWL),
        ),
        t(
            "Izgara köfte ve piyaz", "Grilled kofte with bean salad",
            "Köfteyi ızgarada ya da fırında pişir.", "Grill or bake the kofte rather than frying.",
            p("kofte_grilled", 4.0, PIECE), c("piyaz", 1.0, PORTION), x("green_salad", 1.0, PORTION),
        ),
        t(
            "Ton balıklı salata", "Tuna salad",
            "Ton balığının yağını süz, salatayı limonla tatlandır.", "Drain the tuna and dress the salad with lemon.",
            p("tuna_salad", 1.0, PORTION), c("bread_whole_wheat", 1.0, SLICE),
        ),
        t(
            "Tavuk şiş ve pilav", "Chicken shish with rice",
            "Şişi sebzelerle birlikte ızgara yap.", "Grill the skewers together with vegetables.",
            p("chicken_shish", 1.0, PORTION), c("rice_boiled", 1.0, PORTION), x("shepherd_salad", 1.0, PORTION),
        ),
        t(
            "Fırın somon ve patates", "Baked salmon with potato",
            "Somonu limon ve dereotuyla 15 dk fırınla.", "Bake the salmon with lemon and dill for 15 min.",
            p("salmon_baked", 120.0, G), c("potato_baked", 1.0, PIECE), x("green_salad", 1.0, PORTION),
        ),
        t(
            "Nohut yemeği ve bulgur", "Chickpea stew with bulgur",
            "Nohudu akşamdan ıslat, yemeği zeytinyağıyla pişir.", "Soak the chickpeas overnight and cook with olive oil.",
            p("chickpea_stew", 1.0, PORTION), c("bulgur_pilaf", 0.5, PORTION), x("shepherd_salad", 1.0, PORTION),
        ),
        t(
            "Zeytinyağlı kuru fasulye ve pirinç", "Bean stew with rice",
            "Fasulyeyi zeytinyağı ve domatesle pişir.", "Cook the beans with olive oil and tomato.",
            p("beans_olive_oil", 1.0, PORTION), c("rice_boiled", 1.0, PORTION), x("shepherd_salad", 1.0, PORTION),
        ),
        t(
            "Mercimek çorbası ve kinoa", "Lentil soup with quinoa",
            "Kinoayı haşlamadan önce iyice yıka.", "Rinse the quinoa well before boiling.",
            p("lentil_soup", 1.0, BOWL), c("quinoa", 1.0, PORTION), x("green_salad", 1.0, PORTION),
        ),
        plant(
            "Tofu, sebze ve esmer pirinç", "Tofu, vegetables and brown rice",
            "Tofuyu suyunu çektikten sonra kızart.", "Press the tofu before pan-frying it.",
            p("tofu", 150.0, G), x("roasted_vegetables", 1.0, PORTION), c("rice_brown", 1.0, PORTION),
        ),
        t(
            "Mercimek köftesi ve ayran", "Lentil kofte with ayran",
            "Marul yaprağına sararak ye.", "Wrap them in lettuce leaves.",
            p("lentil_kofte", 5.0, PIECE), x("lettuce", 1.0, HANDFUL), x("ayran", 1.0, GLASS),
        ),
    )

    private val dinner = listOf(
        t(
            "Fırın somon ve sebze", "Baked salmon with vegetables",
            "Sebzeleri somonla aynı tepside fırınla.", "Roast the vegetables in the same tray as the salmon.",
            p("salmon_baked", 120.0, G), x("roasted_vegetables", 1.0, PORTION), c("bulgur_pilaf", 0.5, PORTION),
        ),
        t(
            "Izgara tavuk ve zeytinyağlı fasulye", "Grilled chicken with green beans",
            "Fasulyeyi az yağla pişir, yoğurtla ye.", "Cook the beans with little oil and eat with yogurt.",
            p("chicken_breast_grilled", 120.0, G), x("green_beans_olive_oil", 1.0, PORTION), x("yogurt_whole", 0.5, BOWL),
            c("bread_whole_wheat", 1.0, SLICE),
        ),
        t(
            "Izgara köfte ve sebze", "Grilled kofte with vegetables",
            "Köfteyi yağsız kıymadan hazırla.", "Use lean mince for the kofte.",
            p("kofte_grilled", 4.0, PIECE), x("zucchini_grilled", 1.0, PORTION), c("rice_boiled", 0.5, PORTION),
        ),
        t(
            "Tavuk sote ve pirinç", "Chicken saute with rice",
            "Sebzeyi bol, yağı az kullan.", "Go heavy on vegetables and light on oil.",
            p("chicken_saute", 1.0, PORTION), c("rice_boiled", 0.5, PORTION), x("shepherd_salad", 1.0, PORTION),
        ),
        t(
            "Tas kebabı ve bulgur", "Beef stew with bulgur",
            "Eti yağsız kuşbaşıdan seç.", "Pick lean beef cubes.",
            p("tas_kebab", 1.0, PORTION), c("bulgur_pilaf", 0.5, PORTION), x("yogurt_whole", 0.5, BOWL),
        ),
        t(
            "Izgara levrek ve patates", "Grilled sea bass with potato",
            "Balığı limon ve defne yaprağıyla ızgara yap.", "Grill the fish with lemon and bay leaf.",
            p("sea_bass", 150.0, G), c("potato_boiled", 1.0, PIECE), x("green_salad", 1.0, PORTION),
        ),
        t(
            "Dana bonfile ve fırın patates", "Beef tenderloin with baked potato",
            "Eti dinlendirdikten sonra dilimle.", "Let the meat rest before slicing.",
            p("beef_tenderloin", 120.0, G), c("potato_baked", 1.0, PIECE), x("green_salad", 1.0, PORTION),
        ),
        t(
            "Ispanak yemeği ve süzme yoğurt", "Spinach stew with strained yogurt",
            "Ispanağı fazla pişirme; yoğurdu sarımsakla karıştır.", "Don't overcook the spinach; mix garlic into the yogurt.",
            x("spinach_dish", 1.0, PORTION), p("yogurt_strained", 1.0, BOWL), c("bulgur_pilaf", 0.5, PORTION),
        ),
        t(
            "Sebze türlü ve nohut", "Vegetable stew with chickpeas",
            "Türlüyü zeytinyağıyla, kısık ateşte pişir.", "Cook the stew slowly with olive oil.",
            x("turlu", 1.0, PORTION), p("chickpeas_boiled", 0.5, GLASS), c("bulgur_pilaf", 0.5, PORTION),
        ),
        t(
            "Mercimekli pilav ve salata", "Lentils and rice with salad",
            "Mercimeği pirinçle birlikte pişir, üstüne karamelize soğan ekle.", "Cook lentils with the rice and top with caramelized onion.",
            p("lentils_boiled", 1.0, GLASS), c("rice_boiled", 0.5, PORTION), x("green_salad", 1.0, PORTION),
        ),
        plant(
            "Tofu sote ve kinoa", "Tofu saute with quinoa",
            "Mantarla birlikte soya sosunda sotele.", "Saute with mushrooms in a little soy sauce.",
            p("tofu", 150.0, G), x("mushroom_saute", 1.0, PORTION), c("quinoa", 1.0, PORTION),
        ),
    )

    private const val OPTIONS_PER_MEAL = 3
    private const val FIVE_MEALS_FROM_KCAL = 2600
    private const val MIN_FACTOR = 0.4
    private const val MAX_FACTOR = 3.0
    private const val MIN_PROTEIN_FACTOR = 0.75
    private const val MAX_PROTEIN_FACTOR = 2.0
    /** How far protein may exceed the meal's share when the other foods are already at their limit. */
    private const val PROTEIN_OVERSHOOT = 1.2
}
