package com.fitai.app.model

enum class FitnessGoal(val code: String) {
    LoseFat("lose_fat"),
    BuildMuscle("build_muscle"),
    GetStronger("get_stronger"),
    ImproveEndurance("improve_endurance"),
    StayHealthy("stay_healthy"),
    ;

    companion object {
        fun fromCode(code: String): FitnessGoal? = entries.find { it.code == code }
    }
}

enum class ActivityLevel(val code: String) {
    Sedentary("sedentary"),
    Light("light"),
    Moderate("moderate"),
    Very("very"),
    Athlete("athlete"),
    ;

    companion object {
        fun fromCode(code: String): ActivityLevel? = entries.find { it.code == code }
    }
}

enum class DietRestriction(val code: String) {
    None("none"),
    Vegetarian("vegetarian"),
    Vegan("vegan"),
    Pescatarian("pescatarian"),
    Halal("halal"),
    Kosher("kosher"),
    GlutenFree("gluten-free"),
    DairyFree("dairy-free"),
    ;

    companion object {
        fun fromCode(code: String): DietRestriction? = entries.find { it.code == code }
    }
}

enum class EquipmentOption(val code: String) {
    Bodyweight("bodyweight"),
    Dumbbells("dumbbells"),
    Barbell("barbell"),
    ResistanceBands("resistance_bands"),
    PullUpBar("pull_up_bar"),
    FullGym("full_gym"),
    ;

    companion object {
        fun fromCode(code: String): EquipmentOption? = entries.find { it.code == code }
    }
}

fun encodeProfileCodes(codes: Collection<String>): String =
    codes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")

fun decodeProfileCodes(raw: String): Set<String> =
    raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

fun toggleDietRestriction(current: Set<String>, code: String): Set<String> {
    if (code == DietRestriction.None.code) {
        return setOf(DietRestriction.None.code)
    }
    val withoutNone = current - DietRestriction.None.code
    return if (code in withoutNone) {
        val next = withoutNone - code
        if (next.isEmpty()) setOf(DietRestriction.None.code) else next
    } else {
        withoutNone + code
    }
}

fun toggleEquipmentOption(current: Set<String>, code: String): Set<String> {
    if (code == EquipmentOption.Bodyweight.code) {
        return if (EquipmentOption.Bodyweight.code in current) emptySet() else setOf(code)
    }
    val withoutNone = current - EquipmentOption.Bodyweight.code
    return if (code in withoutNone) withoutNone - code else withoutNone + code
}
