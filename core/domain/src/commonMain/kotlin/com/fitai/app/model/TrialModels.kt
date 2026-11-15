package com.fitai.app.model

import kotlinx.serialization.Serializable

data class TrialStatus(
    val trialStartedAt: String,
    val trialExpiresAt: String,
    val daysRemaining: Int,
    val isActive: Boolean,
    val hasSubscription: Boolean,
)

@Serializable
data class TrialStatusDto(
    val trialStartedAt: String,
    val trialExpiresAt: String,
    val daysRemaining: Int,
    val isActive: Boolean,
    val hasSubscription: Boolean = false,
)
