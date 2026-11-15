package com.fitai.app.sync

import com.fitai.app.model.LocalUserProfile
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.util.nowIsoTimestamp
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private sealed interface SyncJob {
    data object Profile : SyncJob
    data class WorkoutPlan(val planId: String) : SyncJob
    data class DietPlan(val planId: String) : SyncJob
    data class Session(val sessionId: String) : SyncJob
    data object LatestSession : SyncJob
    data class Meal(val mealId: String) : SyncJob
    data class MealDeleted(val mealId: String) : SyncJob
    data class Measurement(val id: String) : SyncJob
    data class MeasurementDeleted(val id: String) : SyncJob
    data class CustomFood(val id: String) : SyncJob
    data class CustomFoodDeleted(val id: String) : SyncJob
}

/**
 * Mirrors single device writes to Firestore after sign-in. Jobs run one at a time and read the current
 * row when they run, so the newest state always goes out last. Each write is started before the next
 * job (the SDK keeps them in order and queues them offline); the server ack is awaited off the queue.
 * Nothing is sent until the latest local user is linked to the signed-in, non-anonymous account.
 */
@OptIn(FlowPreview::class)
internal class CloudSyncScheduler(
    private val local: CloudSyncLocalDataSource,
    private val remote: FirestoreSyncRemoteDataSource,
    private val preferences: UserPreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val jobs = Channel<SyncJob>(Channel.UNLIMITED)
    private val profileEdits = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val setEdits = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        scope.launch { for (job in jobs) run(job) }
        scope.launch { profileEdits.debounce(PROFILE_DEBOUNCE).collect { jobs.send(SyncJob.Profile) } }
        scope.launch { setEdits.debounce(SET_DEBOUNCE).collect { jobs.send(SyncJob.LatestSession) } }
    }

    /** Profile fields or synced preferences changed; several edits in a row become one write. */
    fun profileChanged() {
        profileEdits.tryEmit(Unit)
    }

    fun workoutPlanChanged(planId: String) = enqueue(SyncJob.WorkoutPlan(planId))

    fun dietPlanChanged(planId: String) = enqueue(SyncJob.DietPlan(planId))

    fun sessionChanged(sessionId: String) = enqueue(SyncJob.Session(sessionId))

    /** A set of the running session changed; re-sends the latest session a few seconds later. */
    fun sessionSetChanged() {
        setEdits.tryEmit(Unit)
    }

    fun mealChanged(mealId: String) = enqueue(SyncJob.Meal(mealId))

    fun mealDeleted(mealId: String) = enqueue(SyncJob.MealDeleted(mealId))

    fun measurementChanged(id: String) = enqueue(SyncJob.Measurement(id))

    fun measurementDeleted(id: String) = enqueue(SyncJob.MeasurementDeleted(id))

    fun customFoodChanged(id: String) = enqueue(SyncJob.CustomFood(id))

    fun customFoodDeleted(id: String) = enqueue(SyncJob.CustomFoodDeleted(id))

    private fun enqueue(job: SyncJob) {
        jobs.trySend(job)
    }

    private suspend fun run(job: SyncJob) {
        try {
            val target = target() ?: return
            val uid = target.uid
            val now = nowIsoTimestamp()
            val zone = TimeZone.currentSystemDefault()
            when (job) {
                SyncJob.Profile -> {
                    val doc = buildCloudUserProfile(target.user, preferences, target.email).toDocument(now)
                    issue(job) { remote.setUser(uid, doc) }
                }
                is SyncJob.WorkoutPlan -> local.workoutPlan(job.planId)?.let { (plan, exercises) ->
                    val doc = plan.toDocument(exercises, now)
                    issue(job) { remote.setWorkoutPlan(uid, doc) }
                }
                is SyncJob.DietPlan -> local.dietPlan(job.planId)?.let { plan ->
                    val doc = plan.toDocument(now)
                    issue(job) { remote.setDietPlan(uid, doc) }
                }
                is SyncJob.Session -> local.session(job.sessionId)?.let { session ->
                    val doc = session.toDocument(now, zone)
                    issue(job) { remote.setSession(uid, doc) }
                }
                SyncJob.LatestSession -> local.latestSession(target.user.id)?.let { session ->
                    val doc = session.toDocument(now, zone)
                    issue(job) { remote.setSession(uid, doc) }
                }
                is SyncJob.Meal -> local.meal(job.mealId)?.let { meal ->
                    val doc = meal.toDocument(now, zone)
                    issue(job) { remote.setMeal(uid, doc) }
                }
                is SyncJob.MealDeleted -> issue(job) { remote.deleteMeal(uid, job.mealId) }
                is SyncJob.Measurement -> local.measurement(job.id)?.let { measurement ->
                    val doc = measurement.toDocument(now, zone)
                    issue(job) { remote.setMeasurement(uid, doc) }
                }
                is SyncJob.MeasurementDeleted -> issue(job) { remote.deleteMeasurement(uid, job.id) }
                is SyncJob.CustomFood -> local.customFood(job.id)?.let { row ->
                    val doc = row.toDocument(now)
                    issue(job) { remote.setCustomFood(uid, doc) }
                }
                is SyncJob.CustomFoodDeleted -> issue(job) { remote.deleteCustomFood(uid, job.id) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Cloud sync skipped for $job", e)
        }
    }

    /**
     * Runs [write] undispatched: the SDK call that queues the write happens before this returns, which keeps
     * writes in job order. Only the wait for the server continues in the background.
     */
    private fun issue(job: SyncJob, write: suspend () -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                write()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Cloud write failed for $job", e)
            }
        }
    }

    private suspend fun target(): SyncTarget? {
        val account = remote.currentAccount() ?: return null
        val user = local.latestUser()?.takeIf { it.firebaseUid == account.uid } ?: return null
        return SyncTarget(account.uid, account.email, user)
    }

    private data class SyncTarget(val uid: String, val email: String?, val user: LocalUserProfile)

    private companion object {
        val PROFILE_DEBOUNCE = 1.seconds
        val SET_DEBOUNCE = 1500.milliseconds
    }
}
