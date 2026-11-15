package com.fitai.app.access

import com.fitai.app.domain.UseCaseState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private class FakePlanQuotaRepository(
    var limit: Int = PlanQuotaPolicy.LIMIT,
    var used: Int = 0,
    var failReads: Boolean = false,
) : PlanQuotaRepository {
    var recordCalls = 0
    var markExhaustedCalls = 0

    override suspend fun getQuota(): PlanQuota {
        if (failReads) error("offline")
        return PlanQuota(limit, used)
    }

    override suspend fun recordRegeneration(): PlanQuota {
        recordCalls++
        used++
        return PlanQuota(limit, used)
    }

    override suspend fun markExhausted(): PlanQuota {
        markExhaustedCalls++
        used = limit
        return PlanQuota(limit, used)
    }
}

class PlanQuotaTest {

    @Test
    fun defaultPolicyIsThreeRightsWithoutReset() {
        assertEquals(3, PlanQuotaPolicy.LIMIT)
        assertNull(PlanQuotaPolicy.RESET_PERIOD_DAYS)
    }

    @Test
    fun successfulGenerationSpendsOneRight() = runTest {
        val repository = FakePlanQuotaRepository()
        val spend = SpendPlanRegenerationUseCase(repository)

        val result = spend { UseCaseState.Success("plan") }

        val success = assertIs<UseCaseState.Success<PlanRegeneration<String>>>(result)
        assertEquals("plan", success.data.value)
        assertEquals(2, success.data.quota.remaining)
        assertEquals(1, repository.recordCalls)
    }

    @Test
    fun failedGenerationKeepsTheRight() = runTest {
        val repository = FakePlanQuotaRepository(used = 1)
        val spend = SpendPlanRegenerationUseCase(repository)

        val result = spend<String> { UseCaseState.Error("network") }

        assertEquals(UseCaseState.Error("network"), result)
        assertEquals(0, repository.recordCalls)
        assertEquals(2, repository.getQuota().remaining)
    }

    @Test
    fun exhaustedQuotaBlocksWithoutGenerating() = runTest {
        val repository = FakePlanQuotaRepository(used = 3)
        val spend = SpendPlanRegenerationUseCase(repository)
        var generated = false

        val result = spend {
            generated = true
            UseCaseState.Success("plan")
        }

        assertEquals(UseCaseState.Error(PlanQuotaErrors.EXHAUSTED), result)
        assertFalse(generated)
        assertEquals(0, repository.recordCalls)
    }

    @Test
    fun threeRegenerationsThenExhausted() = runTest {
        val repository = FakePlanQuotaRepository()
        val spend = SpendPlanRegenerationUseCase(repository)

        val remaining = (1..3).map {
            val result = assertIs<UseCaseState.Success<PlanRegeneration<Int>>>(spend { UseCaseState.Success(it) })
            result.data.quota.remaining
        }

        assertEquals(listOf(2, 1, 0), remaining)
        assertTrue(repository.getQuota().exhausted)
        assertEquals(UseCaseState.Error(PlanQuotaErrors.EXHAUSTED), spend { UseCaseState.Success(4) })
        assertEquals(3, repository.recordCalls)
    }

    @Test
    fun serverExhaustedAlignsLocalCopyWithoutSpending() = runTest {
        val repository = FakePlanQuotaRepository(used = 1)
        val spend = SpendPlanRegenerationUseCase(repository)

        val result = spend<String> { UseCaseState.Error(PlanQuotaErrors.EXHAUSTED) }

        assertEquals(UseCaseState.Error(PlanQuotaErrors.EXHAUSTED), result)
        assertEquals(0, repository.recordCalls)
        assertEquals(1, repository.markExhaustedCalls)
        assertTrue(repository.getQuota().exhausted)
    }

    @Test
    fun unreadableQuotaDoesNotGenerate() = runTest {
        val repository = FakePlanQuotaRepository(failReads = true)
        val spend = SpendPlanRegenerationUseCase(repository)
        var generated = false

        val result = spend {
            generated = true
            UseCaseState.Success("plan")
        }

        assertIs<UseCaseState.Error>(result)
        assertFalse(generated)
    }

    @Test
    fun quotaWithoutResetKeepsCount() {
        val quota = planQuotaOf(limit = 3, used = 2, periodStartMillis = 0L, resetPeriodDays = null, nowMillis = Long.MAX_VALUE)
        assertEquals(1, quota.remaining)
        assertNull(quota.resetsAtEpochMillis)
    }

    @Test
    fun quotaWithResetStartsOverAfterThePeriod() {
        val day = PlanQuotaPolicy.DAY_MILLIS
        val inPeriod = planQuotaOf(limit = 3, used = 3, periodStartMillis = 0L, resetPeriodDays = 30, nowMillis = 29 * day)
        val afterPeriod = planQuotaOf(limit = 3, used = 3, periodStartMillis = 0L, resetPeriodDays = 30, nowMillis = 30 * day)

        assertTrue(inPeriod.exhausted)
        assertEquals(30 * day, inPeriod.resetsAtEpochMillis)
        assertEquals(3, afterPeriod.remaining)
    }
}
