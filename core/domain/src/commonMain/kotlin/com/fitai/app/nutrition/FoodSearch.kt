package com.fitai.app.nutrition

import com.fitai.app.model.FoodItem

/**
 * Accent- and case-insensitive form used for matching: Turkish letters fold to ASCII
 * (ı/İ→i, ş→s, ğ→g, ç→c, ö→o, ü→u), other Latin accents are dropped, and punctuation becomes a space.
 */
fun normalizeFoodText(text: String): String {
    val out = StringBuilder(text.length)
    for (ch in text) {
        val folded = when (ch) {
            'ı', 'İ', 'I', 'î', 'Î', 'í', 'ì', 'ï' -> 'i'
            'ş', 'Ş' -> 's'
            'ğ', 'Ğ' -> 'g'
            'ç', 'Ç' -> 'c'
            'ö', 'Ö', 'ó', 'ò', 'ô' -> 'o'
            'ü', 'Ü', 'û', 'Û', 'ú', 'ù' -> 'u'
            'â', 'Â', 'á', 'à', 'ä' -> 'a'
            'é', 'è', 'ê', 'ë' -> 'e'
            'ñ' -> 'n'
            '̇' -> null // combining dot left by lowercasing "İ" elsewhere
            else -> ch.lowercaseChar()
        } ?: continue
        out.append(if (folded.isLetterOrDigit()) folded else ' ')
    }
    return out.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
}

/**
 * Ranks [foods] for [query]. Every query word must start a word of the food's name or keywords (or, for
 * longer words, appear inside one). Order: exact name; name starting with the query as whole words; every
 * query word a whole word of the name (Turkish possessive forms count, so "peynir" matches "kaşar
 * peyniri" and "çorba" matches "mercimek çorbası"); name starting with the query mid-word ("peynirli");
 * name words starting with the query words; other-language name or keywords; inside-a-word hits. Ties go to
 * custom foods, then shorter names, then A–Z.
 */
fun searchFoods(foods: List<FoodItem>, query: String, locale: String, limit: Int = 50): List<FoodItem> {
    val q = normalizeFoodText(query)
    if (q.isEmpty()) return emptyList()
    val tokens = q.split(' ')
    return foods.asSequence()
        .mapNotNull { food -> rankFood(food, q, tokens, locale)?.let { food to it } }
        .sortedWith(
            compareBy<Pair<FoodItem, Int>> { it.second }
                .thenBy { if (it.first.isCustom) 0 else 1 }
                .thenBy { it.first.name(locale).length }
                .thenBy { normalizeFoodText(it.first.name(locale)) },
        )
        .take(limit)
        .map { it.first }
        .toList()
}

private fun rankFood(food: FoodItem, q: String, tokens: List<String>, locale: String): Int? {
    val primary = normalizeFoodText(food.name(locale))
    val secondary = normalizeFoodText(food.name(if (locale == "en") "tr" else "en"))
    val keywords = food.keywords.map(::normalizeFoodText)
    val primaryWords = primary.split(' ')
    if (primary == q) return 0
    if (primaryWords.size >= tokens.size && tokens.indices.all { isWholeWord(primaryWords[it], tokens[it]) }) return 1
    if (tokens.all { t -> primaryWords.any { isWholeWord(it, t) } }) return 2
    if (primary.startsWith(q)) return 3
    if (tokens.all { t -> primaryWords.any { it.startsWith(t) } }) return 4
    val otherWords = secondary.split(' ') + keywords.flatMap { it.split(' ') }
    val allWords = primaryWords + otherWords
    if (secondary == q || secondary.startsWith(q) || keywords.any { it == q || it.startsWith(q) }) return 5
    if (tokens.all { t -> allWords.any { it.startsWith(t) } }) return 5
    // Inside-a-word matches ("peynir" in "lorpeyniri") only for words long enough to be meaningful.
    if (tokens.all { t -> (t.length >= 3 && allWords.any { it.contains(t) }) || allWords.any { it.startsWith(t) } }) return 6
    return null
}

/** [word] is [token] itself or its Turkish possessive form ("peyniri", "corbasi", "sutu"). */
private fun isWholeWord(word: String, token: String): Boolean =
    word == token || (word.startsWith(token) && word.substring(token.length) in POSSESSIVE_SUFFIXES)

private val POSSESSIVE_SUFFIXES = setOf("i", "si", "u", "su")
