package com.fitai.app.repository

import com.fitai.app.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    /** Newest first. */
    fun observeMeasurementsForUser(userId: String): Flow<List<BodyMeasurement>>
    suspend fun getMeasurementsForUser(userId: String): List<BodyMeasurement>
    suspend fun getLatestMeasurement(userId: String): BodyMeasurement?
    /** [start] inclusive, [end] exclusive ISO timestamps; newest first. */
    suspend fun getMeasurementsInRange(userId: String, start: String, end: String): List<BodyMeasurement>
    /** Ignores [BodyMeasurement.id] and returns the stored measurement with a generated id. */
    suspend fun addMeasurement(measurement: BodyMeasurement): BodyMeasurement
    suspend fun updateMeasurement(measurement: BodyMeasurement)
    suspend fun deleteMeasurement(id: String)
}
