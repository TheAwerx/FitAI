package com.fitai.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.ui.theme.FitAISpacing
import fitai.composeapp.generated.resources.Res
import fitai.composeapp.generated.resources.action_primary_example
import fitai.composeapp.generated.resources.home_subtitle
import fitai.composeapp.generated.resources.home_title
import fitai.composeapp.generated.resources.sample_email_label
import fitai.composeapp.generated.resources.sample_email_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(Res.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        FitAICard {
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.titleMedium,
            )
            FitAITextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(Res.string.sample_email_label),
                placeholder = stringResource(Res.string.sample_email_placeholder),
                modifier = Modifier.padding(top = FitAISpacing.sm),
            )
            FitAIButton(
                text = stringResource(Res.string.action_primary_example),
                onClick = {},
                modifier = Modifier.padding(top = FitAISpacing.md),
                variant = FitAIButtonVariant.Secondary,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    FitAITheme {
        HomeScreen()
    }
}
