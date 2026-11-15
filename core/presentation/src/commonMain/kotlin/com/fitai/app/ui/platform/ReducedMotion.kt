package com.fitai.app.ui.platform

import androidx.compose.runtime.Composable

/**
 * True when the user asked the system for less motion: Android "Remove animations"
 * (animator duration scale 0), iOS "Reduce Motion".
 */
@Composable
expect fun rememberReducedMotion(): Boolean
