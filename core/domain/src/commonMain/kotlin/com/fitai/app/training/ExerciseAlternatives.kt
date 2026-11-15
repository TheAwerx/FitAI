package com.fitai.app.training

import com.fitai.app.model.EquipmentOption

enum class EquipmentContext { Gym, Bodyweight }

enum class MuscleCategory(val keywords: List<String>) {
    Chest(listOf("göğüs", "gogus", "chest", "pec")),
    Back(listOf("sırt", "sirt", "back", "lat", "kanat")),
    Shoulders(listOf("omuz", "shoulder", "delt")),
    Biceps(listOf("biceps", "bicep", "pazu", "kol", "arm")),
    Triceps(listOf("triceps", "tricep", "arka kol")),
    Quads(listOf("quad", "ön bacak", "on bacak", "ön uyluk", "bacak", "leg", "thigh")),
    Hamstrings(listOf("hamstring", "arka bacak", "arka uyluk")),
    Glutes(listOf("glute", "kalça", "kalca", "hip", "popo")),
    Calves(listOf("calf", "calves", "baldır", "baldir")),
    Core(listOf("karın", "karin", "core", "abs", "abdominal", "oblique", "bel", "lower back")),
}

/** Names are kept bilingual as data; pick one with [displayName] and the app locale ("tr" | "en"). */
data class ExerciseAlternative(
    val nameTr: String,
    val nameEn: String,
    val muscle: MuscleCategory,
    val equipment: EquipmentContext,
) {
    fun displayName(locale: String): String = if (locale == "en") nameEn else nameTr
}

object ExerciseAlternativesCatalog {

    /** Category whose keyword appears earliest in the text ("Sırt & Biceps" → Back); longer keyword wins ties. */
    fun categoryOf(muscleGroup: String, exerciseName: String = ""): MuscleCategory? =
        earliestCategory(muscleGroup.lowercase()) ?: earliestCategory(exerciseName.lowercase())

    fun contextFor(equipmentCodes: Set<String>): EquipmentContext {
        val gymCodes = setOf(EquipmentOption.FullGym.code, EquipmentOption.Barbell.code, EquipmentOption.Dumbbells.code)
        return if (equipmentCodes.any { it in gymCodes }) EquipmentContext.Gym else EquipmentContext.Bodyweight
    }

    /** Same muscle category, [preferred] equipment first, without the exercise being replaced. */
    fun alternativesFor(
        muscleGroup: String,
        exerciseName: String,
        preferred: EquipmentContext,
    ): List<ExerciseAlternative> {
        val category = categoryOf(muscleGroup, exerciseName) ?: return emptyList()
        val current = normalizeExerciseName(exerciseName)
        return entries
            .filter { it.muscle == category }
            .filterNot { normalizeExerciseName(it.nameTr) == current || normalizeExerciseName(it.nameEn) == current }
            .sortedBy { if (it.equipment == preferred) 0 else 1 }
    }

    private fun earliestCategory(text: String): MuscleCategory? {
        if (text.isBlank()) return null
        return MuscleCategory.entries
            .flatMap { category -> category.keywords.map { keyword -> Triple(category, text.indexOf(keyword), keyword.length) } }
            .filter { it.second >= 0 }
            .minWithOrNull(compareBy<Triple<MuscleCategory, Int, Int>> { it.second }.thenByDescending { it.third })
            ?.first
    }

    private fun gym(tr: String, en: String, muscle: MuscleCategory) =
        ExerciseAlternative(tr, en, muscle, EquipmentContext.Gym)

    private fun bodyweight(tr: String, en: String, muscle: MuscleCategory) =
        ExerciseAlternative(tr, en, muscle, EquipmentContext.Bodyweight)

    val entries: List<ExerciseAlternative> = listOf(
        gym("Bench press", "Bench press", MuscleCategory.Chest),
        gym("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
        gym("Eğimli dambıl press", "Incline dumbbell press", MuscleCategory.Chest),
        gym("Kablo crossover", "Cable crossover", MuscleCategory.Chest),
        bodyweight("Şınav", "Push-up", MuscleCategory.Chest),
        bodyweight("Eğimli şınav", "Incline push-up", MuscleCategory.Chest),
        bodyweight("Ayak yüksekte şınav", "Decline push-up", MuscleCategory.Chest),
        bodyweight("Geniş tutuş şınav", "Wide push-up", MuscleCategory.Chest),

        gym("Barbell row", "Barbell row", MuscleCategory.Back),
        gym("Lat pulldown", "Lat pulldown", MuscleCategory.Back),
        gym("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
        gym("Oturarak kablo row", "Seated cable row", MuscleCategory.Back),
        bodyweight("Barfiks", "Pull-up", MuscleCategory.Back),
        bodyweight("Masa altı ters row", "Inverted table row", MuscleCategory.Back),
        bodyweight("Superman", "Superman hold", MuscleCategory.Back),
        bodyweight("Havlu ile row", "Towel row", MuscleCategory.Back),

        gym("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
        gym("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
        gym("Arnold press", "Arnold press", MuscleCategory.Shoulders),
        gym("Face pull", "Face pull", MuscleCategory.Shoulders),
        bodyweight("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
        bodyweight("Duvarda amut tutuşu", "Wall handstand hold", MuscleCategory.Shoulders),
        bodyweight("Yüzüstü Y-T-W", "Prone Y-T-W raise", MuscleCategory.Shoulders),

        gym("Barbell curl", "Barbell curl", MuscleCategory.Biceps),
        gym("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
        gym("Kablo curl", "Cable curl", MuscleCategory.Biceps),
        gym("Konsantrasyon curl", "Concentration curl", MuscleCategory.Biceps),
        bodyweight("Ters tutuş barfiks", "Chin-up", MuscleCategory.Biceps),
        bodyweight("Havlu curl", "Towel curl", MuscleCategory.Biceps),
        bodyweight("Dar tutuş ters row", "Close-grip inverted row", MuscleCategory.Biceps),

        gym("Kablo pushdown", "Cable pushdown", MuscleCategory.Triceps),
        gym("Baş üstü dambıl extension", "Overhead dumbbell extension", MuscleCategory.Triceps),
        gym("Dar tutuş bench press", "Close-grip bench press", MuscleCategory.Triceps),
        gym("Skull crusher", "Skull crusher", MuscleCategory.Triceps),
        bodyweight("Sandalye dip", "Bench dip", MuscleCategory.Triceps),
        bodyweight("Elmas şınav", "Diamond push-up", MuscleCategory.Triceps),
        bodyweight("Vücut ağırlığı triceps extension", "Bodyweight triceps extension", MuscleCategory.Triceps),

        gym("Squat", "Back squat", MuscleCategory.Quads),
        gym("Leg press", "Leg press", MuscleCategory.Quads),
        gym("Goblet squat", "Goblet squat", MuscleCategory.Quads),
        gym("Bulgar split squat", "Bulgarian split squat", MuscleCategory.Quads),
        bodyweight("Vücut ağırlığı squat", "Bodyweight squat", MuscleCategory.Quads),
        bodyweight("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
        bodyweight("Basamak çıkma", "Step-up", MuscleCategory.Quads),
        bodyweight("Duvarda oturma", "Wall sit", MuscleCategory.Quads),

        gym("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
        gym("Leg curl", "Leg curl", MuscleCategory.Hamstrings),
        gym("Good morning", "Good morning", MuscleCategory.Hamstrings),
        gym("Tek bacak RDL", "Single-leg RDL", MuscleCategory.Hamstrings),
        bodyweight("Tek bacak kalça köprüsü", "Single-leg glute bridge", MuscleCategory.Hamstrings),
        bodyweight("Nordic curl", "Nordic curl", MuscleCategory.Hamstrings),
        bodyweight("Havlu ile leg curl", "Towel slider leg curl", MuscleCategory.Hamstrings),

        gym("Hip thrust", "Hip thrust", MuscleCategory.Glutes),
        gym("Kablo kickback", "Cable kickback", MuscleCategory.Glutes),
        gym("Sumo deadlift", "Sumo deadlift", MuscleCategory.Glutes),
        bodyweight("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
        bodyweight("Eşek tekmesi", "Donkey kick", MuscleCategory.Glutes),
        bodyweight("Fire hydrant", "Fire hydrant", MuscleCategory.Glutes),

        gym("Ayakta calf raise", "Standing calf raise", MuscleCategory.Calves),
        gym("Oturarak calf raise", "Seated calf raise", MuscleCategory.Calves),
        gym("Leg press calf raise", "Leg press calf raise", MuscleCategory.Calves),
        bodyweight("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
        bodyweight("Basamakta calf raise", "Step calf raise", MuscleCategory.Calves),
        bodyweight("İp atlama", "Jump rope", MuscleCategory.Calves),

        gym("Kablo crunch", "Cable crunch", MuscleCategory.Core),
        gym("Asılı bacak kaldırma", "Hanging leg raise", MuscleCategory.Core),
        gym("Ab tekerleği", "Ab wheel rollout", MuscleCategory.Core),
        gym("Pallof press", "Pallof press", MuscleCategory.Core),
        bodyweight("Plank", "Plank", MuscleCategory.Core),
        bodyweight("Dead bug", "Dead bug", MuscleCategory.Core),
        bodyweight("Bisiklet mekik", "Bicycle crunch", MuscleCategory.Core),
        bodyweight("Mountain climber", "Mountain climber", MuscleCategory.Core),
    )
}
