package com.fitai.app.training

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalRecordsTest {

    @Test
    fun epleyEstimate() {
        assertEquals(100.0, PersonalRecords.epley(100.0, 0))
        assertEquals(80.0 * (1 + 8 / 30.0), PersonalRecords.epley(80.0, 8))
    }

    @Test
    fun firstLogIsBaselineNotPr() {
        val history = listOf(record("s1", "2026-11-01T10:00:00Z", "Bench Press", 60.0, "10"))
        assertTrue(PersonalRecords.detectForSession(history, "s1").isEmpty())
    }

    @Test
    fun detectsOnePrPerExerciseWhenE1rmBeatsEarlierBest() {
        val history = listOf(
            record("s2", "2026-11-05T10:00:00Z", "bench press", 62.5, "8", setNumber = 1),
            record("s2", "2026-11-05T10:00:00Z", "bench press", 60.0, "12", setNumber = 2),
            record("s2", "2026-11-05T10:00:00Z", "Squat", 90.0, "5"),
            record("s1", "2026-11-01T10:00:00Z", "Bench Press", 60.0, "10"),
            record("s1", "2026-11-01T10:00:00Z", "Squat", 100.0, "5"),
        )
        val prs = PersonalRecords.detectForSession(history, "s2")
        assertEquals(1, prs.size)
        val pr = prs.single()
        assertEquals(60.0, pr.weightKg)
        assertEquals(12, pr.reps)
        assertEquals(PersonalRecords.epley(60.0, 10), pr.previousE1rm)
    }

    @Test
    fun laterSessionsDoNotCountAsPrevious() {
        val history = listOf(
            record("s2", "2026-11-05T10:00:00Z", "Row", 70.0, "10"),
            record("s1", "2026-11-01T10:00:00Z", "Row", 60.0, "10"),
        )
        assertEquals(1, PersonalRecords.countForSessions(history, listOf("s1", "s2")))
    }

    @Test
    fun bestPerExerciseIgnoresBodyweightSets() {
        val history = listOf(
            record("s1", "2026-11-01T10:00:00Z", "Push-up", null, "20"),
            record("s1", "2026-11-01T10:00:00Z", "Squat", 100.0, "5"),
            record("s2", "2026-11-05T10:00:00Z", "squat", 105.0, "3"),
        )
        val best = PersonalRecords.bestPerExercise(history)
        assertEquals(1, best.size)
        assertEquals(PersonalRecords.epley(100.0, 5), best.single().e1rm)
    }
}
