package com.fitai.app.training

import com.fitai.app.model.ActivityLevel
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.Gender
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.GeneratedExercise
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.TrainingExperience
import com.fitai.app.model.UserProfilePayload
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.model.SessionLength
import kotlin.math.roundToInt

/**
 * Rule-based plans for users without the personal AI plan, and the offline stand-in for AI generation.
 * Exercise names come from [ExerciseAlternativesCatalog] so "Replace" keeps working on them.
 */
object StarterPlans {

    const val MIN_DAYS = 2
    const val MAX_DAYS = 6

    fun workout(profile: UserProfilePayload, locale: String, personal: Boolean = false): GeneratedWorkoutPlan {
        val days = (profile.daysPerWeek ?: defaultDays(profile.activityLevel)).coerceIn(MIN_DAYS, MAX_DAYS)
        val minutes = profile.sessionMinutes ?: SessionLength.DEFAULT
        val equipment = decodeProfileCodes(profile.equipment)
        val library = libraryFor(equipment)
        val advanced = profile.experience == TrainingExperience.Intermediate.code ||
            profile.experience == TrainingExperience.Advanced.code
        val split = splitFor(days, advanced, library)
        val perSession = exercisesPerSession(minutes)
        val prescription = prescriptionFor(profile.goal, profile.experience)
        val exercises = split.zip(TRAINING_DAYS.getValue(days)).flatMap { (template, dayOfWeek) ->
            template.take(perSession).map { move ->
                GeneratedExercise(
                    dayOfWeek = dayOfWeek,
                    name = move.name(locale),
                    sets = prescription.sets,
                    reps = prescription.reps,
                    restSeconds = prescription.restSeconds,
                    muscleGroup = muscleLabel(move.muscle, locale),
                )
            }
        }
        val en = locale == "en"
        return GeneratedWorkoutPlan(
            title = when {
                personal && en -> "Your FitAI plan · $days days"
                personal -> "Kişisel FitAI planın · $days gün"
                en -> "Starter program · $days days"
                else -> "Başlangıç programı · $days gün"
            },
            description = if (en) {
                "$days days a week, about $minutes min per session."
            } else {
                "Haftada $days gün, seans başı yaklaşık $minutes dk."
            },
            daysPerWeek = days,
            exercises = exercises,
        )
    }

    /** Mifflin-St Jeor with the onboarding activity level and a goal adjustment. */
    fun diet(profile: UserProfilePayload): GeneratedDietPlan {
        val genderOffset = if (profile.gender == Gender.Female.code) -161 else 5
        val bmr = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age + genderOffset
        val multiplier = when (ActivityLevel.fromCode(profile.activityLevel)) {
            ActivityLevel.Sedentary -> 1.2
            ActivityLevel.Light -> 1.375
            ActivityLevel.Moderate, null -> 1.55
            ActivityLevel.Very -> 1.725
            ActivityLevel.Athlete -> 1.9
        }
        val goal = FitnessGoal.fromCode(profile.goal)
        val calories = (bmr * multiplier + when (goal) {
            FitnessGoal.LoseFat -> -450
            FitnessGoal.BuildMuscle -> 300
            FitnessGoal.GetStronger -> 200
            else -> 0
        }).roundToInt().coerceAtLeast(MIN_CALORIES)
        val proteinPerKg = when (goal) {
            FitnessGoal.BuildMuscle, FitnessGoal.GetStronger, FitnessGoal.LoseFat -> 2.0
            else -> 1.6
        }
        val protein = (profile.weightKg * proteinPerKg).roundToInt()
        val fat = (calories * 0.25 / 9).roundToInt()
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return GeneratedDietPlan(dailyCalories = calories, proteinG = protein, carbsG = carbs, fatG = fat)
    }

    private fun defaultDays(activityLevel: String): Int = when (ActivityLevel.fromCode(activityLevel)) {
        ActivityLevel.Sedentary, ActivityLevel.Light, null -> 3
        ActivityLevel.Moderate -> 4
        ActivityLevel.Very, ActivityLevel.Athlete -> 5
    }

    private fun exercisesPerSession(minutes: Int): Int = when {
        minutes <= 15 -> 3
        minutes <= 30 -> 4
        minutes <= 45 -> 5
        minutes <= 60 -> 6
        else -> 7
    }

    private data class Prescription(val sets: Int, val reps: String, val restSeconds: Int)

    private fun prescriptionFor(goal: String, experience: String?): Prescription {
        val base = when (FitnessGoal.fromCode(goal)) {
            FitnessGoal.GetStronger -> Prescription(5, "4-6", 150)
            FitnessGoal.BuildMuscle -> Prescription(4, "8-12", 90)
            FitnessGoal.LoseFat, FitnessGoal.ImproveEndurance -> Prescription(3, "12-15", 60)
            FitnessGoal.StayHealthy, null -> Prescription(3, "10-12", 75)
        }
        val novice = experience == TrainingExperience.None.code || experience == TrainingExperience.Beginner.code
        return if (novice) base.copy(sets = (base.sets - 1).coerceAtLeast(2)) else base
    }

    private val TRAINING_DAYS = mapOf(
        2 to listOf(1, 4),
        3 to listOf(1, 3, 5),
        4 to listOf(1, 2, 4, 5),
        5 to listOf(1, 2, 3, 5, 6),
        6 to listOf(1, 2, 3, 4, 5, 6),
    )

    private fun splitFor(days: Int, advanced: Boolean, lib: Library): List<List<Move>> = when (days) {
        2 -> listOf(lib.fullA, lib.fullB)
        3 -> if (advanced) listOf(lib.push, lib.pull, lib.legs) else listOf(lib.fullA, lib.fullB, lib.fullC)
        4 -> listOf(lib.upper, lib.lower, lib.upper.rotate(), lib.lower.rotate())
        5 -> listOf(lib.push, lib.pull, lib.legs, lib.upper, lib.lower)
        else -> listOf(lib.push, lib.pull, lib.legs, lib.push.rotate(), lib.pull.rotate(), lib.legs.rotate())
    }

    private fun List<Move>.rotate(): List<Move> = drop(1) + take(1)

    private data class Move(val tr: String, val en: String, val muscle: MuscleCategory) {
        fun name(locale: String) = if (locale == "en") en else tr
    }

    private class Library(
        val push: List<Move>,
        val pull: List<Move>,
        val legs: List<Move>,
        val upper: List<Move>,
        val lower: List<Move>,
        val fullA: List<Move>,
        val fullB: List<Move>,
        val fullC: List<Move>,
    )

    private fun libraryFor(equipment: Set<String>): Library {
        val gym = EquipmentOption.FullGym.code in equipment || EquipmentOption.Barbell.code in equipment
        val home = !gym && (EquipmentOption.Dumbbells.code in equipment || EquipmentOption.ResistanceBands.code in equipment)
        val pullUp = if (EquipmentOption.PullUpBar.code in equipment || gym) PULL_UP else INVERTED_ROW
        return when {
            gym -> gymLibrary
            home -> homeLibrary(pullUp)
            else -> bodyweightLibrary(pullUp)
        }
    }

    private val PULL_UP = Move("Barfiks", "Pull-up", MuscleCategory.Back)
    private val INVERTED_ROW = Move("Masa altı ters row", "Inverted table row", MuscleCategory.Back)

    private val gymLibrary = Library(
        push = listOf(
            Move("Bench press", "Bench press", MuscleCategory.Chest),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            Move("Eğimli dambıl press", "Incline dumbbell press", MuscleCategory.Chest),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
            Move("Kablo pushdown", "Cable pushdown", MuscleCategory.Triceps),
            Move("Kablo crossover", "Cable crossover", MuscleCategory.Chest),
            Move("Baş üstü dambıl extension", "Overhead dumbbell extension", MuscleCategory.Triceps),
        ),
        pull = listOf(
            Move("Lat pulldown", "Lat pulldown", MuscleCategory.Back),
            Move("Barbell row", "Barbell row", MuscleCategory.Back),
            Move("Oturarak kablo row", "Seated cable row", MuscleCategory.Back),
            Move("Face pull", "Face pull", MuscleCategory.Shoulders),
            Move("Barbell curl", "Barbell curl", MuscleCategory.Biceps),
            Move("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
            Move("Kablo crunch", "Cable crunch", MuscleCategory.Core),
        ),
        legs = listOf(
            Move("Squat", "Back squat", MuscleCategory.Quads),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Leg press", "Leg press", MuscleCategory.Quads),
            Move("Leg curl", "Leg curl", MuscleCategory.Hamstrings),
            Move("Hip thrust", "Hip thrust", MuscleCategory.Glutes),
            Move("Ayakta calf raise", "Standing calf raise", MuscleCategory.Calves),
            Move("Asılı bacak kaldırma", "Hanging leg raise", MuscleCategory.Core),
        ),
        upper = listOf(
            Move("Bench press", "Bench press", MuscleCategory.Chest),
            Move("Barbell row", "Barbell row", MuscleCategory.Back),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            Move("Lat pulldown", "Lat pulldown", MuscleCategory.Back),
            Move("Barbell curl", "Barbell curl", MuscleCategory.Biceps),
            Move("Kablo pushdown", "Cable pushdown", MuscleCategory.Triceps),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
        ),
        lower = listOf(
            Move("Squat", "Back squat", MuscleCategory.Quads),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Bulgar split squat", "Bulgarian split squat", MuscleCategory.Quads),
            Move("Leg curl", "Leg curl", MuscleCategory.Hamstrings),
            Move("Hip thrust", "Hip thrust", MuscleCategory.Glutes),
            Move("Ayakta calf raise", "Standing calf raise", MuscleCategory.Calves),
            Move("Ab tekerleği", "Ab wheel rollout", MuscleCategory.Core),
        ),
        fullA = listOf(
            Move("Squat", "Back squat", MuscleCategory.Quads),
            Move("Bench press", "Bench press", MuscleCategory.Chest),
            Move("Barbell row", "Barbell row", MuscleCategory.Back),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Kablo pushdown", "Cable pushdown", MuscleCategory.Triceps),
            Move("Plank", "Plank", MuscleCategory.Core),
        ),
        fullB = listOf(
            Move("Leg press", "Leg press", MuscleCategory.Quads),
            Move("Eğimli dambıl press", "Incline dumbbell press", MuscleCategory.Chest),
            Move("Lat pulldown", "Lat pulldown", MuscleCategory.Back),
            Move("Hip thrust", "Hip thrust", MuscleCategory.Glutes),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
            Move("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
            Move("Kablo crunch", "Cable crunch", MuscleCategory.Core),
        ),
        fullC = listOf(
            Move("Goblet squat", "Goblet squat", MuscleCategory.Quads),
            Move("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
            Move("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
            Move("Arnold press", "Arnold press", MuscleCategory.Shoulders),
            Move("Leg curl", "Leg curl", MuscleCategory.Hamstrings),
            Move("Skull crusher", "Skull crusher", MuscleCategory.Triceps),
            Move("Ab tekerleği", "Ab wheel rollout", MuscleCategory.Core),
        ),
    )

    private fun homeLibrary(pull: Move) = Library(
        push = listOf(
            Move("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            Move("Şınav", "Push-up", MuscleCategory.Chest),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
            Move("Baş üstü dambıl extension", "Overhead dumbbell extension", MuscleCategory.Triceps),
            Move("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
            Move("Elmas şınav", "Diamond push-up", MuscleCategory.Triceps),
        ),
        pull = listOf(
            Move("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
            pull,
            Move("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
            Move("Yüzüstü Y-T-W", "Prone Y-T-W raise", MuscleCategory.Shoulders),
            Move("Konsantrasyon curl", "Concentration curl", MuscleCategory.Biceps),
            Move("Superman", "Superman hold", MuscleCategory.Back),
            Move("Dead bug", "Dead bug", MuscleCategory.Core),
        ),
        legs = listOf(
            Move("Goblet squat", "Goblet squat", MuscleCategory.Quads),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Bulgar split squat", "Bulgarian split squat", MuscleCategory.Quads),
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
            Move("Plank", "Plank", MuscleCategory.Core),
        ),
        upper = listOf(
            Move("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
            Move("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            pull,
            Move("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
            Move("Baş üstü dambıl extension", "Overhead dumbbell extension", MuscleCategory.Triceps),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
        ),
        lower = listOf(
            Move("Goblet squat", "Goblet squat", MuscleCategory.Quads),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Bulgar split squat", "Bulgarian split squat", MuscleCategory.Quads),
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
            Move("Dead bug", "Dead bug", MuscleCategory.Core),
        ),
        fullA = listOf(
            Move("Goblet squat", "Goblet squat", MuscleCategory.Quads),
            Move("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
            Move("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
            Move("Dambıl omuz press", "Dumbbell shoulder press", MuscleCategory.Shoulders),
            Move("Romen deadlift", "Romanian deadlift", MuscleCategory.Hamstrings),
            Move("Baş üstü dambıl extension", "Overhead dumbbell extension", MuscleCategory.Triceps),
            Move("Plank", "Plank", MuscleCategory.Core),
        ),
        fullB = listOf(
            Move("Bulgar split squat", "Bulgarian split squat", MuscleCategory.Quads),
            Move("Şınav", "Push-up", MuscleCategory.Chest),
            pull,
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Yana açış", "Lateral raise", MuscleCategory.Shoulders),
            Move("Hammer curl", "Hammer curl", MuscleCategory.Biceps),
            Move("Dead bug", "Dead bug", MuscleCategory.Core),
        ),
        fullC = listOf(
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
            Move("Tek kol dambıl row", "One-arm dumbbell row", MuscleCategory.Back),
            Move("Dambıl göğüs press", "Dumbbell bench press", MuscleCategory.Chest),
            Move("Tek bacak kalça köprüsü", "Single-leg glute bridge", MuscleCategory.Hamstrings),
            Move("Elmas şınav", "Diamond push-up", MuscleCategory.Triceps),
            Move("Mountain climber", "Mountain climber", MuscleCategory.Core),
        ),
    )

    private fun bodyweightLibrary(pull: Move) = Library(
        push = listOf(
            Move("Şınav", "Push-up", MuscleCategory.Chest),
            Move("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
            Move("Eğimli şınav", "Incline push-up", MuscleCategory.Chest),
            Move("Sandalye dip", "Bench dip", MuscleCategory.Triceps),
            Move("Ayak yüksekte şınav", "Decline push-up", MuscleCategory.Chest),
            Move("Elmas şınav", "Diamond push-up", MuscleCategory.Triceps),
            Move("Plank", "Plank", MuscleCategory.Core),
        ),
        pull = listOf(
            pull,
            Move("Superman", "Superman hold", MuscleCategory.Back),
            Move("Havlu ile row", "Towel row", MuscleCategory.Back),
            Move("Havlu curl", "Towel curl", MuscleCategory.Biceps),
            Move("Yüzüstü Y-T-W", "Prone Y-T-W raise", MuscleCategory.Shoulders),
            Move("Dar tutuş ters row", "Close-grip inverted row", MuscleCategory.Biceps),
            Move("Dead bug", "Dead bug", MuscleCategory.Core),
        ),
        legs = listOf(
            Move("Vücut ağırlığı squat", "Bodyweight squat", MuscleCategory.Quads),
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Basamak çıkma", "Step-up", MuscleCategory.Quads),
            Move("Tek bacak kalça köprüsü", "Single-leg glute bridge", MuscleCategory.Hamstrings),
            Move("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
            Move("Duvarda oturma", "Wall sit", MuscleCategory.Quads),
        ),
        upper = listOf(
            Move("Şınav", "Push-up", MuscleCategory.Chest),
            pull,
            Move("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
            Move("Havlu ile row", "Towel row", MuscleCategory.Back),
            Move("Sandalye dip", "Bench dip", MuscleCategory.Triceps),
            Move("Havlu curl", "Towel curl", MuscleCategory.Biceps),
            Move("Yüzüstü Y-T-W", "Prone Y-T-W raise", MuscleCategory.Shoulders),
        ),
        lower = listOf(
            Move("Vücut ağırlığı squat", "Bodyweight squat", MuscleCategory.Quads),
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Basamak çıkma", "Step-up", MuscleCategory.Quads),
            Move("Tek bacak kalça köprüsü", "Single-leg glute bridge", MuscleCategory.Hamstrings),
            Move("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
            Move("Bisiklet mekik", "Bicycle crunch", MuscleCategory.Core),
        ),
        fullA = listOf(
            Move("Vücut ağırlığı squat", "Bodyweight squat", MuscleCategory.Quads),
            Move("Şınav", "Push-up", MuscleCategory.Chest),
            pull,
            Move("Kalça köprüsü", "Glute bridge", MuscleCategory.Glutes),
            Move("Pike şınav", "Pike push-up", MuscleCategory.Shoulders),
            Move("Sandalye dip", "Bench dip", MuscleCategory.Triceps),
            Move("Plank", "Plank", MuscleCategory.Core),
        ),
        fullB = listOf(
            Move("Geri lunge", "Reverse lunge", MuscleCategory.Quads),
            Move("Eğimli şınav", "Incline push-up", MuscleCategory.Chest),
            Move("Superman", "Superman hold", MuscleCategory.Back),
            Move("Tek bacak kalça köprüsü", "Single-leg glute bridge", MuscleCategory.Hamstrings),
            Move("Yüzüstü Y-T-W", "Prone Y-T-W raise", MuscleCategory.Shoulders),
            Move("Havlu curl", "Towel curl", MuscleCategory.Biceps),
            Move("Dead bug", "Dead bug", MuscleCategory.Core),
        ),
        fullC = listOf(
            Move("Basamak çıkma", "Step-up", MuscleCategory.Quads),
            Move("Elmas şınav", "Diamond push-up", MuscleCategory.Triceps),
            Move("Havlu ile row", "Towel row", MuscleCategory.Back),
            Move("Ayak yüksekte şınav", "Decline push-up", MuscleCategory.Chest),
            Move("Duvarda oturma", "Wall sit", MuscleCategory.Quads),
            Move("Tek bacak calf raise", "Single-leg calf raise", MuscleCategory.Calves),
            Move("Mountain climber", "Mountain climber", MuscleCategory.Core),
        ),
    )

    private fun muscleLabel(muscle: MuscleCategory, locale: String): String {
        val en = locale == "en"
        return when (muscle) {
            MuscleCategory.Chest -> if (en) "Chest" else "Göğüs"
            MuscleCategory.Back -> if (en) "Back" else "Sırt"
            MuscleCategory.Shoulders -> if (en) "Shoulders" else "Omuz"
            MuscleCategory.Biceps -> "Biceps"
            MuscleCategory.Triceps -> "Triceps"
            MuscleCategory.Quads -> if (en) "Quads" else "Ön bacak"
            MuscleCategory.Hamstrings -> if (en) "Hamstrings" else "Arka bacak"
            MuscleCategory.Glutes -> if (en) "Glutes" else "Kalça"
            MuscleCategory.Calves -> if (en) "Calves" else "Baldır"
            MuscleCategory.Core -> if (en) "Core" else "Karın"
        }
    }

    private const val MIN_CALORIES = 1200
}
