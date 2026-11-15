package com.fitai.app.training

import com.fitai.app.model.CompletedSetRecord

data class PersonalRecord(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val e1rm: Double,
    val achievedAt: String,
    /** Best e1RM before this record; null for [PersonalRecords.bestPerExercise] entries. */
    val previousE1rm: Double? = null,
)

/**
 * Estimated 1RM via Epley. Exercises are matched by [normalizeExerciseName]. A set only counts as
 * a PR when an earlier session of the same exercise exists, so a first log is a baseline, not a PR.
 */
object PersonalRecords {

    private const val EPSILON = 1e-6

    fun epley(weightKg: Double, reps: Int): Double = weightKg * (1 + reps / 30.0)

    /** Best set per exercise across all [history], highest e1RM first. */
    fun bestPerExercise(history: List<CompletedSetRecord>): List<PersonalRecord> =
        history.mapNotNull { it.toRecord() }
            .groupBy { normalizeExerciseName(it.exerciseName) }
            .values
            .map { records -> records.maxBy { it.e1rm } }
            .sortedByDescending { it.e1rm }

    /** At most one PR per exercise: the session's best set when it beats every earlier session. */
    fun detectForSession(history: List<CompletedSetRecord>, sessionId: String): List<PersonalRecord> {
        val sessionSets = history.filter { it.sessionId == sessionId }
        val startedAt = sessionSets.firstOrNull()?.sessionStartedAt ?: return emptyList()
        val earlier = history.filter { it.sessionId != sessionId && it.sessionStartedAt < startedAt }
        val previousBest = earlier.mapNotNull { it.toRecord() }
            .groupBy { normalizeExerciseName(it.exerciseName) }
            .mapValues { (_, records) -> records.maxOf { it.e1rm } }

        return sessionSets.mapNotNull { it.toRecord() }
            .groupBy { normalizeExerciseName(it.exerciseName) }
            .mapNotNull { (key, records) ->
                val best = records.maxBy { it.e1rm }
                val previous = previousBest[key] ?: return@mapNotNull null
                if (best.e1rm > previous + EPSILON) best.copy(previousE1rm = previous) else null
            }
            .sortedByDescending { it.e1rm }
    }

    fun countForSessions(history: List<CompletedSetRecord>, sessionIds: Collection<String>): Int =
        sessionIds.sumOf { detectForSession(history, it).size }

    private fun CompletedSetRecord.toRecord(): PersonalRecord? {
        val weight = weightKg?.takeIf { it > 0.0 } ?: return null
        val reps = repsDone()?.takeIf { it > 0 } ?: return null
        return PersonalRecord(
            exerciseName = exerciseName,
            weightKg = weight,
            reps = reps,
            e1rm = epley(weight, reps),
            achievedAt = completedAt ?: sessionStartedAt,
        )
    }
}
