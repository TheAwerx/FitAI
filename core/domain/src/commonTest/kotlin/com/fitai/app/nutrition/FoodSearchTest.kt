package com.fitai.app.nutrition

import com.fitai.app.model.FoodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoodSearchTest {

    private fun food(id: String, tr: String, en: String, custom: Boolean = false, keywords: List<String> = emptyList()) =
        FoodItem(id, tr, en, "dish", 100.0, 5.0, 10.0, 3.0, keywords = keywords, isCustom = custom)

    private val foods = listOf(
        food("lentils", "Haşlanmış yeşil mercimek", "Boiled green lentils"),
        food("lentil_soup", "Mercimek çorbası", "Red lentil soup", keywords = listOf("corba")),
        food("lentil_kofte", "Mercimek köftesi", "Lentil kofte"),
        food("white_cheese", "Beyaz peynir", "Turkish white cheese (feta)", keywords = listOf("feta")),
        food("kashar", "Kaşar peyniri", "Kashar cheese"),
        food("ayran", "Ayran", "Ayran (yogurt drink)"),
        food("yogurt", "Yoğurt (tam yağlı)", "Plain yogurt (whole)"),
        food("iskender", "İskender kebap", "Iskender kebab"),
        food("chicken_doner", "Tavuk döner", "Chicken doner (meat)"),
        food("cig_kofte", "Çiğ köfte (etsiz)", "Cig kofte (meatless)"),
        food("cheese_omelette", "Peynirli omlet", "Cheese omelette"),
    )

    @Test
    fun normalizationFoldsTurkishLettersAndCase() {
        assertEquals("kasar peyniri", normalizeFoodText("Kaşar Peyniri"))
        assertEquals("iskender", normalizeFoodText("İSKENDER"))
        assertEquals("isik", normalizeFoodText("IŞIK"))
        assertEquals("cig kofte etsiz", normalizeFoodText("Çiğ köfte (etsiz)"))
        assertEquals("sut ve yogurt", normalizeFoodText("  Süt   ve  Yoğurt! "))
        assertEquals("creme brulee", normalizeFoodText("Crème brûlée"))
    }

    @Test
    fun accentFreeQueriesFindTurkishNames() {
        assertEquals("kashar", searchFoods(foods, "kasar", "tr").first().id)
        assertEquals("iskender", searchFoods(foods, "iskender", "tr").first().id)
        assertEquals("yogurt", searchFoods(foods, "yogurt", "tr").first().id)
        assertEquals("chicken_doner", searchFoods(foods, "doner", "tr").first().id)
        assertEquals("cig_kofte", searchFoods(foods, "cig kofte", "tr").first().id)
    }

    @Test
    fun prefixMatchesRankBeforeWordAndInnerMatches() {
        val ids = searchFoods(foods, "mercimek", "tr").map { it.id }
        // Names starting with the query first (shorter first), then names containing the word.
        assertEquals(listOf("lentil_soup", "lentil_kofte", "lentils"), ids)
        assertEquals("ayran", searchFoods(foods, "ayran", "tr").first().id)
        // Whole words, including the possessive "peyniri", beat a name that only starts with the query.
        assertEquals(listOf("white_cheese", "kashar", "cheese_omelette"), searchFoods(foods, "peynir", "tr").map { it.id })
        assertEquals("lentil_soup", searchFoods(foods, "corba", "tr").first().id)
    }

    @Test
    fun otherLanguageNamesAndKeywordsMatch() {
        assertEquals("white_cheese", searchFoods(foods, "feta", "tr").first().id)
        assertTrue(searchFoods(foods, "cheese", "tr").map { it.id }.containsAll(listOf("white_cheese", "kashar")))
        assertEquals("lentil_soup", searchFoods(foods, "lentil soup", "en").first().id)
        assertEquals("lentil_soup", searchFoods(foods, "corba", "tr").first().id)
    }

    @Test
    fun multiWordQueriesNeedEveryWord() {
        assertEquals(listOf("lentil_kofte"), searchFoods(foods, "merc kof", "tr").map { it.id })
        assertTrue(searchFoods(foods, "mercimek pizza", "tr").isEmpty())
        assertTrue(searchFoods(foods, "   ", "tr").isEmpty())
    }

    @Test
    fun customFoodsComeFirstWithinTheSameRank() {
        val withCustom = foods + food("custom_1", "Mercimek çorbası (annem)", "Mum's lentil soup", custom = true)
        val ids = searchFoods(withCustom, "mercimek", "tr").map { it.id }
        assertEquals("custom_1", ids.first())
    }
}
