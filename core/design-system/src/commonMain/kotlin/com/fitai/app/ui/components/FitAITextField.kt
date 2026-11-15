package com.fitai.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes

@Composable
fun FitAITextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon ->
            {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FitAIColors.OnSurfaceMuted,
                )
            }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { msg ->
            { Text(msg) }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        shape = FitAIShapes.button,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FitAIColors.Primary,
            unfocusedBorderColor = FitAIColors.SurfaceVariant,
            focusedLabelColor = FitAIColors.PrimaryLight,
            unfocusedLabelColor = FitAIColors.OnSurfaceMuted,
            cursorColor = FitAIColors.Primary,
            focusedTextColor = FitAIColors.OnBackground,
            unfocusedTextColor = FitAIColors.OnSurface,
            errorBorderColor = FitAIColors.Error,
            errorLabelColor = FitAIColors.Error,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
