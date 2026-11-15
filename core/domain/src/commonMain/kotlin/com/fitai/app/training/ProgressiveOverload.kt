package com.fitai.app.training

import com.fitai.app.model.CompletedSetRecord
import kotlin.math.roundToLong

enum class OverloadReason {
    /** No earlier session for this exercise; reps = bottom of the range. */
    NoHistory,

    /** Every set of the last session reached the top of the range. */
    IncreaseWeight,

    /** Same weight, aim for one more rep. */
    AddRep,

    /** The last two sessions both fell short of the bottom of the range. */
    Deload,

    /** No weight was logged; only reps progress. */
    Bodyweight,
}

data class SetSuggestion(
    val weightKg: Double?,
    val reps: Int,
    val reason: OverloadReason,
)

/** One logged set; [reps] null when it could not be parsed. */
data class LoggedSet(
    val weightKg: Double?,
    val reps: Int?,
)

data class RepRange(val min: Int, val max: Int) {
    companion object {
        private val numberRegex = Regex("""\d+""")

        /** "8-12", "8–12", "10", "10 reps" → range; "AMRAP" or no number → null. */
        fun parse(text: String): RepRange? {
            val numbers = numberRegex.findAll(text).mapNotNull { it.value.toIntOrNull() }.take(2).toList()
            return when (numbers.size) {
                0 -> null
                1 -> RepRange(numbers[0], numbers[0])
                else -> RepRange(minOf(numbers[0], numbers[1]), maxOf(numbers[0], numbers[1]))
            }
        }
    }
}

object ProgressiveOverload {
    const val UPPER_BODY_INCREMENT_KG = 2.5
    const val LOWER_BODY_INCREMENT_KG = 5.0
    const val DELOAD_FACTOR = 0.95

    private val lowerBodyKeywords = listOf(
        "leg", "quad", "hamstring", "glute", "calf", "calves", "lower body",
        "bacak", "uyluk", "kalça", "kalca", "baldır", "baldir", "alt vücut", "alt vucut",
        "squat", "deadlift", "lunge", "leg press", "hip thrust",
    )

    /**
     * @param recentSessions newest first; each entry is one earlier session's completed sets
     * of this exercise. Only the first two are used.
     */
    fun suggest(
        targetReps: String,
        muscleGroup: String,
        recentSessions: List<List<LoggedSet>>,
        exerciseName: String = "",
    ): SetSuggestion {
        val range = RepRange.parse(targetReps)
        val sessions = recentSessions
            .map { sets -> sets.filter { (it.reps ?: 0) > 0 } }
            .filter { it.isNotEmpty() }
            .take(2)
        val last = sessions.firstOrNull()
            ?: return SetSuggestion(weightKg = null, reps = range?.min ?: 0, reason = OverloadReason.NoHistory)

        val lastReps = last.map { it.reps ?: 0 }
        val workingWeight = last.mapNotNull { it.weightKg }.filter { it > 0.0 }.maxOrNull()

        if (workingWeight == null) {
            val reps = (lastReps.min() + 1).coerceAtLeast(range?.min ?: 0)
            return SetSuggestion(weightKg = null, reps = reps, reason = OverloadReason.Bodyweight)
        }
        if (range == null) {
            return SetSuggestion(workingWeight, lastReps.max() + 1, OverloadReason.AddRep)
        }
        if (lastReps.all { it >= range.max }) {
            val increment = if (isLowerBody(muscleGroup, exerciseName)) LOWER_BODY_INCREMENT_KG else UPPER_BODY_INCREMENT_KG
            return SetSuggestion(roundTo(workingWeight + increment, 0.5), range.min, OverloadReason.IncreaseWeight)
        }
        if (sessions.size == 2 && sessions.all { missedBottom(it, range) }) {
            return SetSuggestion(deload(workingWeight), range.min, OverloadReason.Deload)
        }
        val nextRep = (lastReps.min() + 1).coerceIn(range.min, range.max)
        return SetSuggestion(workingWeight, nextRep, OverloadReason.AddRep)
    }

    /** Matches [history] by [normalizeExerciseName] because regenerated plans get new exercise ids. */
    fun suggestFromHistory(
        exerciseName: String,
        targetReps: String,
        muscleGroup: String,
        history: List<CompletedSetRecord>,
        excludeSessionId: String? = null,
    ): SetSuggestion {
        val key = normalizeExerciseName(exerciseName)
        val recent = history
            .filter { it.sessionId != excludeSessionId && normalizeExerciseName(it.exerciseName) == key }
            .groupBy { it.sessionId }
            .values
            .sortedByDescending { sets -> sets.first().sessionStartedAt }
            .take(2)
            .map { sets -> sets.sortedBy { it.setNumber }.map { LoggedSet(it.weightKg, it.repsDone()) } }
        return suggest(targetReps, muscleGroup, recent, exerciseName)
    }

    fun isLowerBody(muscleGroup: String, exerciseName: String = ""): Boolean {
        val text = "${muscleGroup.lowercase()} ${exerciseName.lowercase()}"
        return lowerBodyKeywords.any { it in text }
    }

    /** −5%, snapped to 2.5 kg plates when the weight already sits on that grid, otherwise to 0.5 kg. */
    fun deload(weightKg: Double): Double {
        val step = if (isMultipleOf(weightKg, 2.5)) 2.5 else 0.5
        val target = roundTo(weightKg * DELOAD_FACTOR, step)
        val lowered = if (target >= weightKg) weightKg - step else target
        return lowered.coerceAtLeast(0.0)
    }

    private fun missedBottom(sets: List<LoggedSet>, range: RepRange): Boolean =
        sets.count { (it.reps ?: 0) < range.min } * 2 > sets.size

    private fun roundTo(value: Double, step: Double): Double = (value / step).roundToLong() * step

    private fun isMultipleOf(value: Double, step: Double): Boolean {
        val ratio = value / step
        return kotlin.math.abs(ratio - ratio.roundToLong()) < 1e-6
    }
}

private val leadingNumberRegex = Regex("""\d+""")

/** Case-, spacing- and punctuation-insensitive key: "Bench-Press " == "bench press". */
fun normalizeExerciseName(name: String): String =
    name.lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

/** Reps actually done; falls back to the first number of the target ("8-12" → 8). */
fun CompletedSetRecord.repsDone(): Int? =
    (actualReps?.let(::leadingInt) ?: leadingInt(targetReps))

private fun leadingInt(text: String): Int? = leadingNumberRegex.find(text)?.value?.toIntOrNull()
