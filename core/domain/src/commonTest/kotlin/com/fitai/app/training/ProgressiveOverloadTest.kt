package com.fitai.app.training

import com.fitai.app.model.CompletedSetRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressiveOverloadTest {

    private fun sets(weight: Double?, vararg reps: Int) = reps.map { LoggedSet(weight, it) }

    @Test
    fun parsesRepRanges() {
        assertEquals(RepRange(8, 12), RepRange.parse("8-12"))
        assertEquals(RepRange(8, 12), RepRange.parse("8–12"))
        assertEquals(RepRange(10, 10), RepRange.parse("10"))
        assertEquals(RepRange(10, 12), RepRange.parse("12-10 reps"))
        assertNull(RepRange.parse("AMRAP"))
    }

    @Test
    fun noHistorySuggestsBottomOfRangeWithoutWeight() {
        val suggestion = ProgressiveOverload.suggest("8-12", "Göğüs", emptyList())
        assertEquals(SetSuggestion(null, 8, OverloadReason.NoHistory), suggestion)
    }

    @Test
    fun allSetsAtTopIncreasesUpperBodyBy2_5() {
        val suggestion = ProgressiveOverload.suggest("8-12", "Chest", listOf(sets(60.0, 12, 12, 12)))
        assertEquals(SetSuggestion(62.5, 8, OverloadReason.IncreaseWeight), suggestion)
    }

    @Test
    fun allSetsAtTopIncreasesLowerBodyBy5() {
        assertEquals(105.0, ProgressiveOverload.suggest("8-10", "Bacak", listOf(sets(100.0, 10, 10, 11))).weightKg)
        assertEquals(65.0, ProgressiveOverload.suggest("8-10", "Hamstrings", listOf(sets(60.0, 10, 10))).weightKg)
        assertEquals(45.0, ProgressiveOverload.suggest("10", "Kalça", listOf(sets(40.0, 10, 10))).weightKg)
    }

    @Test
    fun missingTopKeepsWeightAndTargetsNextRep() {
        val suggestion = ProgressiveOverload.suggest("8-12", "Sırt", listOf(sets(50.0, 12, 10, 9)))
        assertEquals(SetSuggestion(50.0, 10, OverloadReason.AddRep), suggestion)
    }

    @Test
    fun twoSessionsBelowBottomDeloadFivePercent() {
        val history = listOf(sets(60.0, 6, 5, 5), sets(60.0, 7, 6, 6))
        val suggestion = ProgressiveOverload.suggest("8-12", "Chest", history)
        assertEquals(OverloadReason.Deload, suggestion.reason)
        assertEquals(57.5, suggestion.weightKg)
        assertEquals(8, suggestion.reps)
    }

    @Test
    fun singleBadSessionDoesNotDeload() {
        val suggestion = ProgressiveOverload.suggest("8-12", "Chest", listOf(sets(60.0, 6, 5, 5), sets(57.5, 10, 9, 9)))
        assertEquals(SetSuggestion(60.0, 8, OverloadReason.AddRep), suggestion)
    }

    @Test
    fun deloadRoundsAndAlwaysLowers() {
        assertEquals(57.5, ProgressiveOverload.deload(60.0))
        assertEquals(17.5, ProgressiveOverload.deload(20.0))
        assertEquals(21.0, ProgressiveOverload.deload(22.0))
    }

    @Test
    fun bodyweightSuggestsRepsOnly() {
        val suggestion = ProgressiveOverload.suggest("8-12", "Göğüs", listOf(sets(null, 12, 11, 10)))
        assertEquals(SetSuggestion(null, 11, OverloadReason.Bodyweight), suggestion)
    }

    @Test
    fun historyMatchesByNormalizedNameAndSkipsCurrentSession() {
        val history = listOf(
            record("s2", "2026-11-05T10:00:00Z", "bench  press", 60.0, "12"),
            record("s2", "2026-11-05T10:00:00Z", "bench  press", 60.0, "12"),
            record("s1", "2026-11-01T10:00:00Z", "Bench-Press", 57.5, "12"),
            record("current", "2026-11-07T10:00:00Z", "Bench Press", 80.0, "12"),
            record("s2", "2026-11-05T10:00:00Z", "Squat", 100.0, "5"),
        )
        val suggestion = ProgressiveOverload.suggestFromHistory("Bench Press", "8-12", "Göğüs", history, "current")
        assertEquals(SetSuggestion(62.5, 8, OverloadReason.IncreaseWeight), suggestion)
    }

    @Test
    fun normalizesExerciseNames() {
        assertEquals("bench press", normalizeExerciseName("  Bench-Press "))
        assertEquals(normalizeExerciseName("Şınav"), normalizeExerciseName("şınav"))
    }
}

internal fun record(
    sessionId: String,
    startedAt: String,
    name: String,
    weight: Double?,
    reps: String,
    setNumber: Int = 1,
) = CompletedSetRecord(
    sessionId = sessionId,
    sessionStartedAt = startedAt,
    sessionExerciseId = "$sessionId-$name",
    exerciseName = name,
    muscleGroup = "",
    setNumber = setNumber,
    targetReps = "8-12",
    actualReps = reps,
    weightKg = weight,
    completedAt = startedAt,
)
