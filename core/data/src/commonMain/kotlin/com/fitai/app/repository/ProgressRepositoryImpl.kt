package com.fitai.app.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.model.BodyMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class ProgressRepositoryImpl(
    private val database: FitAiDatabase,
) : ProgressRepository {

    override fun observeMeasurementsForUser(userId: String): Flow<List<BodyMeasurement>> =
        database.progressQueries
            .selectMeasurementsForUser(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getMeasurementsForUser(userId: String): List<BodyMeasurement> =
        withContext(Dispatchers.Default) {
            database.progressQueries.selectMeasurementsForUser(userId).executeAsList().map { it.toDomain() }
        }

    override suspend fun getLatestMeasurement(userId: String): BodyMeasurement? = withContext(Dispatchers.Default) {
        database.progressQueries.selectLatestMeasurementForUser(userId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getMeasurementsInRange(userId: String, start: String, end: String): List<BodyMeasurement> =
        withContext(Dispatchers.Default) {
            database.progressQueries.selectMeasurementsForUserInRange(userId, start, end)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun addMeasurement(measurement: BodyMeasurement): BodyMeasurement =
        withContext(Dispatchers.Default) {
            val stored = measurement.copy(id = newId())
            database.progressQueries.insertMeasurement(
                id = stored.id,
                user_id = stored.userId,
                weight_kg = stored.weightKg,
                body_fat_pct = stored.bodyFatPct,
                chest_cm = stored.chestCm,
                waist_cm = stored.waistCm,
                hip_cm = stored.hipCm,
                arm_cm = stored.armCm,
                photo_url = stored.photoUrl,
                measured_at = stored.measuredAt,
            )
            stored
        }

    override suspend fun updateMeasurement(measurement: BodyMeasurement) = withContext(Dispatchers.Default) {
        database.progressQueries.updateMeasurement(
            weight_kg = measurement.weightKg,
            body_fat_pct = measurement.bodyFatPct,
            chest_cm = measurement.chestCm,
            waist_cm = measurement.waistCm,
            hip_cm = measurement.hipCm,
            arm_cm = measurement.armCm,
            photo_url = measurement.photoUrl,
            measured_at = measurement.measuredAt,
            id = measurement.id,
        )
        Unit
    }

    override suspend fun deleteMeasurement(id: String) = withContext(Dispatchers.Default) {
        database.progressQueries.deleteMeasurement(id)
        Unit
    }

    private fun newId(): String = buildString {
        repeat(16) { append(Random.nextInt(16).toString(16)) }
    }
}
