package com.fitai.app

import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.billing.SubscriptionProduct
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import com.fitai.app.viewmodel.paywall.PaywallState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The paywall offers "7 days free" only while this device and account haven't used the trial. */
class PaywallTrialStateTest {

    private val yearly = SubscriptionProduct("fitai_pro_yearly", SubscriptionPeriod.Yearly, "999,99 ₺/yıl", "83,33 ₺/ay", 7, 54)
    private val monthly = SubscriptionProduct("fitai_pro_monthly", SubscriptionPeriod.Monthly, "179,99 ₺/ay", "179,99 ₺/ay", 0, null)

    private fun state(trial: TrialStatus?, selected: String = yearly.id) = PaywallState(
        loading = false,
        products = listOf(yearly, monthly),
        selectedProductId = selected,
        trialStatus = trial,
    )

    @Test
    fun eligibleDeviceSeesTheTrial() {
        val s = state(TrialStatus(TrialState.Eligible))
        assertTrue(s.trialOffered)
        assertEquals(7, s.selectedTrialDays)
        assertEquals(0, s.trialDaysFor(monthly))
        assertNull(s.trialNotice)
    }

    @Test
    fun usedDeviceGetsSubscribeAndAnExplanation() {
        val s = state(TrialStatus(TrialState.UsedOnDevice))
        assertFalse(s.trialOffered)
        assertEquals(0, s.selectedTrialDays)
        assertEquals(0, s.trialDaysFor(yearly))
        assertEquals(TrialState.UsedOnDevice, s.trialNotice?.state)
    }

    @Test
    fun accountThatUsedItOrIsInItDoesNotGetItAgain() {
        assertEquals(0, state(TrialStatus(TrialState.Expired)).selectedTrialDays)
        assertEquals(TrialState.Expired, state(TrialStatus(TrialState.Expired)).trialNotice?.state)
        val active = state(TrialStatus(TrialState.Active, daysRemaining = 3))
        assertEquals(0, active.selectedTrialDays)
        assertEquals(3, active.trialNotice?.daysRemaining)
    }

    @Test
    fun unknownStatusStillOffersItBecauseTheStartIsServerChecked() {
        val s = state(trial = null)
        assertTrue(s.trialOffered)
        assertEquals(7, s.selectedTrialDays)
        assertEquals(0, state(trial = null, selected = monthly.id).selectedTrialDays)
    }

    @Test
    fun requestingTheTrialBlocksOtherActions() {
        assertTrue(state(null).copy(requestingTrial = true).busy)
    }
}
