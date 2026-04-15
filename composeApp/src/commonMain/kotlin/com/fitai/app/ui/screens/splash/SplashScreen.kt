package com.fitai.app.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.ui.theme.FitAISpacing
import fitai.composeapp.generated.resources.Res
import fitai.composeapp.generated.resources.action_continue
import fitai.composeapp.generated.resources.app_name
import fitai.composeapp.generated.resources.splash_loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = stringResource(Res.string.splash_loading),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = FitAISpacing.md),
        )
        FitAIButton(
            text = stringResource(Res.string.action_continue),
            onClick = onContinue,
            modifier = Modifier.padding(top = FitAISpacing.xl),
            variant = FitAIButtonVariant.Primary,
        )
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    FitAITheme {
        SplashScreen(onContinue = {})
    }
}
