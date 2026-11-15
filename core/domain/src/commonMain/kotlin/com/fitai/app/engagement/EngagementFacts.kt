package com.fitai.app.engagement

import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.CompletedSetRecord
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import com.fitai.app.model.WorkoutSession
import com.fitai.app.nutrition.targetsFor
import com.fitai.app.training.PersonalRecords
import com.fitai.app.training.repsDone
import com.fitai.app.util.isoTimestampToLocalDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** Dated engagement events derived from raw history; the input of streak, XP and badge rules. */
data class EngagementFacts(
    /** One entry per completed session. */
    val sessionDates: List<LocalDate>,
    /** One entry per PR (at most one per exercise per session). */
    val prDates: List<LocalDate>,
    val mealDates: List<LocalDate>,
    val photoMealDates: List<LocalDate>,
    val proteinGoalDates: Set<LocalDate>,
    val weighInDates: List<LocalDate>,
    val volumeByDate: Map<LocalDate, Double>,
) {
    val activeDates: Set<LocalDate> get() = (sessionDates + mealDates).toSet()

    fun dateVolumeReached(kg: Double): LocalDate? {
        var total = 0.0
        volumeByDate.entries.sortedBy { it.key }.forEach { (date, volume) ->
            total += volume
            if (total >= kg) return date
        }
        return null
    }

    companion object {
        /** [completedSessions] must only contain completed sessions; dates use [completedAt] when present. */
        fun from(
            completedSessions: List<WorkoutSession>,
            setHistory: List<CompletedSetRecord>,
            meals: List<MealLog>,
            measurements: List<BodyMeasurement>,
            diet: DietPlan?,
            trainingDaysOfWeek: Set<Int>,
            toDate: (String) -> LocalDate? = ::isoTimestampToLocalDate,
        ): EngagementFacts {
            val sessionDateById = completedSessions.mapNotNull { session ->
                toDate(session.completedAt ?: session.startedAt)?.let { session.id to it }
            }.toMap()
            val completedHistory = setHistory.filter { it.sessionId in sessionDateById }

            val prDates = sessionDateById.flatMap { (sessionId, date) ->
                List(PersonalRecords.detectForSession(completedHistory, sessionId).size) { date }
            }

            val volumeByDate = completedHistory
                .groupBy { sessionDateById.getValue(it.sessionId) }
                .mapValues { (_, sets) -> sets.sumOf { (it.weightKg ?: 0.0) * (it.repsDone() ?: 0) } }

            val datedMeals = meals.mapNotNull { meal -> toDate(meal.loggedAt)?.let { it to meal } }
            val proteinGoalDates = if (diet == null) {
                emptySet()
            } else {
                datedMeals.groupBy({ it.first }, { it.second.proteinG })
                    .filter { (date, proteins) ->
                        val isTrainingDay = date.dayOfWeek.isoDayNumber in trainingDaysOfWeek
                        val target = diet.targetsFor(isTrainingDay).proteinG
                        target > 0 && proteins.sum() >= target
                    }
                    .keys
            }

            return EngagementFacts(
                sessionDates = sessionDateById.values.toList(),
                prDates = prDates,
                mealDates = datedMeals.map { it.first },
                photoMealDates = datedMeals.filter { it.second.hasPhoto }.map { it.first },
                proteinGoalDates = proteinGoalDates,
                weighInDates = measurements.mapNotNull { toDate(it.measuredAt) },
                volumeByDate = volumeByDate,
            )
        }
    }
}
