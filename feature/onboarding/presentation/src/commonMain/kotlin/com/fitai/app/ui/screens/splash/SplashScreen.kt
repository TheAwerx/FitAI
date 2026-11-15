package com.fitai.app.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAITheme
import fitai.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieAnimatable
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

private val SplashFume = listOf(
    Color(0xFF6E6E6E),
    Color(0xFF525252),
    Color(0xFF3A3A3A),
)

@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compositionResult = rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/splash_dumbbell.json").decodeToString(),
        )
    }
    val composition by compositionResult
    val animatable = rememberLottieAnimatable()
    LaunchedEffect(Unit) {
        val loaded = runCatching { compositionResult.await() }.getOrNull()
        if (loaded == null) {
            onContinue()
            return@LaunchedEffect
        }
        animatable.animate(
            composition = loaded,
            iterations = 1,
        )
        onContinue()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(SplashFume)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { animatable.progress },
            ),
            contentDescription = null,
            modifier = Modifier.size(300.dp),
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
