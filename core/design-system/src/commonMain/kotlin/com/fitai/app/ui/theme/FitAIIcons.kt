package com.fitai.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FitAIIcons {
    val FitnessCenter: ImageVector
        get() {
            if (_fitnessCenter != null) return _fitnessCenter!!
            _fitnessCenter = ImageVector.Builder(
                name = "FitnessCenter",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20.57f, 14.86f)
                    lineTo(22f, 13.43f)
                    lineTo(20.57f, 12f)
                    lineTo(17f, 15.57f)
                    lineTo(8.43f, 7f)
                    lineTo(12f, 3.43f)
                    lineTo(10.57f, 2f)
                    lineTo(9.14f, 3.43f)
                    lineTo(7.71f, 2f)
                    lineTo(5.57f, 4.14f)
                    lineTo(4.14f, 2.71f)
                    lineTo(2.71f, 4.14f)
                    lineTo(4.14f, 5.57f)
                    lineTo(2f, 7.71f)
                    lineTo(3.43f, 9.14f)
                    lineTo(2f, 10.57f)
                    lineTo(3.43f, 12f)
                    lineTo(7f, 8.43f)
                    lineTo(15.57f, 17f)
                    lineTo(12f, 20.57f)
                    lineTo(13.43f, 22f)
                    lineTo(14.86f, 20.57f)
                    lineTo(16.29f, 22f)
                    lineTo(18.43f, 19.86f)
                    lineTo(19.86f, 21.29f)
                    lineTo(21.29f, 19.86f)
                    lineTo(19.86f, 18.43f)
                    lineTo(22f, 16.29f)
                    close()
                }
            }.build()
            return _fitnessCenter!!
        }

    val ChevronLeft: ImageVector
        get() {
            if (_chevronLeft != null) return _chevronLeft!!
            _chevronLeft = ImageVector.Builder(
                name = "ChevronLeft",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(15.41f, 7.41f)
                    lineTo(14f, 6f)
                    lineTo(8f, 12f)
                    lineTo(14f, 18f)
                    lineTo(15.41f, 16.59f)
                    lineTo(10.83f, 12f)
                    close()
                }
            }.build()
            return _chevronLeft!!
        }

    val ChevronRight: ImageVector
        get() {
            if (_chevronRight != null) return _chevronRight!!
            _chevronRight = ImageVector.Builder(
                name = "ChevronRight",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.59f, 16.59f)
                    lineTo(13.17f, 12f)
                    lineTo(8.59f, 7.41f)
                    lineTo(10f, 6f)
                    lineTo(16f, 12f)
                    lineTo(10f, 18f)
                    close()
                }
            }.build()
            return _chevronRight!!
        }

    val ArrowBack: ImageVector
        get() {
            if (_arrowBack != null) return _arrowBack!!
            _arrowBack = ImageVector.Builder(
                name = "ArrowBack",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20f, 11f)
                    horizontalLineTo(7.83f)
                    lineTo(13.42f, 5.41f)
                    lineTo(12f, 4f)
                    lineTo(4f, 12f)
                    lineTo(12f, 20f)
                    lineTo(13.41f, 18.59f)
                    lineTo(7.83f, 13f)
                    horizontalLineTo(20f)
                    close()
                }
            }.build()
            return _arrowBack!!
        }

    val Mail: ImageVector
        get() {
            if (_mail != null) return _mail!!
            _mail = ImageVector.Builder(
                name = "Mail",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20f, 4f)
                    horizontalLineTo(4f)
                    curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
                    lineTo(2f, 18f)
                    curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                    horizontalLineTo(20f)
                    curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                    verticalLineTo(6f)
                    curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                    close()
                    moveTo(20f, 8f)
                    lineTo(12f, 13f)
                    lineTo(4f, 8f)
                    verticalLineTo(6f)
                    lineTo(12f, 11f)
                    lineTo(20f, 6f)
                    verticalLineTo(8f)
                    close()
                }
            }.build()
            return _mail!!
        }

    val ArrowForward: ImageVector
        get() {
            if (_arrowForward != null) return _arrowForward!!
            _arrowForward = ImageVector.Builder(
                name = "ArrowForward",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 4f)
                    lineTo(10.59f, 5.41f)
                    lineTo(16.17f, 11f)
                    horizontalLineTo(4f)
                    verticalLineTo(13f)
                    horizontalLineTo(16.17f)
                    lineTo(10.59f, 18.59f)
                    lineTo(12f, 20f)
                    lineTo(20f, 12f)
                    close()
                }
            }.build()
            return _arrowForward!!
        }

    val Check: ImageVector
        get() {
            if (_check != null) return _check!!
            _check = ImageVector.Builder(
                name = "Check",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(9f, 16.17f)
                    lineTo(4.83f, 12f)
                    lineToRelative(-1.42f, 1.41f)
                    lineTo(9f, 19f)
                    lineTo(21f, 7f)
                    lineToRelative(-1.41f, -1.41f)
                    close()
                }
            }.build()
            return _check!!
        }

    val CheckCircle: ImageVector
        get() {
            if (_checkCircle != null) return _checkCircle!!
            _checkCircle = ImageVector.Builder(
                name = "CheckCircle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 2f)
                    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                    reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                    reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                    reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                    close()
                    moveTo(10f, 17f)
                    lineToRelative(-5f, -5f)
                    lineToRelative(1.41f, -1.41f)
                    lineTo(10f, 14.17f)
                    lineToRelative(7.59f, -7.59f)
                    lineTo(19f, 8f)
                    close()
                }
            }.build()
            return _checkCircle!!
        }

    val Favorite: ImageVector
        get() {
            if (_favorite != null) return _favorite!!
            _favorite = ImageVector.Builder(
                name = "Favorite",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 21.35f)
                    lineToRelative(-1.45f, -1.32f)
                    curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                    curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                    curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
                    curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                    curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                    curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                    close()
                }
            }.build()
            return _favorite!!
        }

    val LocalFireDepartment: ImageVector
        get() {
            if (_fire != null) return _fire!!
            _fire = ImageVector.Builder(
                name = "LocalFireDepartment",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 12.9f)
                    lineToRelative(-2.13f, 2.09f)
                    curveToRelative(-0.56f, 0.56f, -0.87f, 1.29f, -0.87f, 2.07f)
                    curveTo(9f, 18.68f, 10.35f, 20f, 12f, 20f)
                    reflectiveCurveToRelative(3f, -1.32f, 3f, -2.94f)
                    curveToRelative(0f, -0.78f, -0.31f, -1.52f, -0.87f, -2.07f)
                    close()
                    moveTo(16f, 6f)
                    lineToRelative(-0.44f, 0.55f)
                    curveTo(14.38f, 8.02f, 12f, 7.19f, 12f, 5.3f)
                    verticalLineTo(2f)
                    reflectiveCurveTo(8f, 6f, 6.32f, 9.86f)
                    curveTo(6.12f, 10.4f, 6f, 10.96f, 6f, 11.5f)
                    curveToRelative(0f, 1.5f, 0.58f, 2.86f, 1.53f, 3.9f)
                    lineToRelative(1.87f, -1.86f)
                    curveToRelative(0.83f, -0.83f, 2.17f, -0.83f, 3f, 0f)
                    curveToRelative(0.28f, 0.28f, 0.44f, 0.63f, 0.5f, 1f)
                    curveToRelative(1.78f, -0.68f, 3.1f, -2.4f, 3.1f, -4.4f)
                    curveToRelative(0f, -1.54f, -0.5f, -2.85f, -1.05f, -3.82f)
                    curveTo(17.9f, 4.46f, 16f, 6f, 16f, 6f)
                    close()
                }
            }.build()
            return _fire!!
        }

    val DirectionsRun: ImageVector
        get() {
            if (_run != null) return _run!!
            _run = ImageVector.Builder(
                name = "DirectionsRun",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(13.49f, 5.48f)
                    curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                    reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                    reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                    reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                    close()
                    moveTo(9.89f, 19.38f)
                    lineToRelative(1f, -4.4f)
                    lineToRelative(2.1f, 2f)
                    verticalLineToRelative(6f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-7.5f)
                    lineToRelative(-2.1f, -2f)
                    lineToRelative(0.6f, -3f)
                    curveToRelative(1.3f, 1.5f, 3.3f, 2.5f, 5.5f, 2.5f)
                    verticalLineToRelative(-2f)
                    curveToRelative(-1.9f, 0f, -3.5f, -1f, -4.3f, -2.4f)
                    lineToRelative(-1f, -1.6f)
                    curveToRelative(-0.4f, -0.6f, -1f, -1f, -1.7f, -1f)
                    curveToRelative(-0.3f, 0f, -0.5f, 0.1f, -0.8f, 0.1f)
                    lineTo(7.69f, 8.88f)
                    lineToRelative(-3.5f, -1.5f)
                    lineToRelative(-0.8f, 1.9f)
                    lineToRelative(3.2f, 1.4f)
                    lineToRelative(-3.1f, 9.32f)
                    lineToRelative(1.9f, 0.7f)
                    lineToRelative(3.1f, -7.32f)
                    lineToRelative(2.4f, 2.1f)
                    close()
                }
            }.build()
            return _run!!
        }

    val DirectionsWalk: ImageVector
        get() {
            if (_walk != null) return _walk!!
            _walk = ImageVector.Builder(
                name = "DirectionsWalk",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(13.5f, 5.5f)
                    curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                    reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                    reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                    reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                    close()
                    moveTo(9.8f, 8.9f)
                    lineTo(7f, 23f)
                    horizontalLineToRelative(2.1f)
                    lineToRelative(1.8f, -8f)
                    lineToRelative(2.1f, 2f)
                    verticalLineToRelative(6f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-7.5f)
                    lineToRelative(-2.1f, -2f)
                    lineToRelative(0.6f, -3f)
                    curveToRelative(1.3f, 1.5f, 3.3f, 2.5f, 5.5f, 2.5f)
                    verticalLineToRelative(-2f)
                    curveToRelative(-1.9f, 0f, -3.5f, -1f, -4.3f, -2.4f)
                    lineToRelative(-1f, -1.6f)
                    curveToRelative(-0.56f, -0.89f, -1.68f, -1.25f, -2.65f, -0.84f)
                    lineTo(6.09f, 10.4f)
                    lineToRelative(-3.02f, 1.4f)
                    lineToRelative(0.9f, 1.8f)
                    lineToRelative(2.1f, -1f)
                    close()
                }
            }.build()
            return _walk!!
        }

    val Block: ImageVector
        get() {
            if (_block != null) return _block!!
            _block = ImageVector.Builder(
                name = "Block",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 2f)
                    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                    reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                    reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                    reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                    close()
                    moveTo(4f, 12f)
                    curveToRelative(0f, -4.42f, 3.58f, -8f, 8f, -8f)
                    curveToRelative(1.85f, 0f, 3.55f, 0.63f, 4.9f, 1.69f)
                    lineTo(5.69f, 16.9f)
                    curveTo(4.63f, 15.55f, 4f, 13.85f, 4f, 12f)
                    close()
                    moveTo(12f, 20f)
                    curveToRelative(-1.85f, 0f, -3.55f, -0.63f, -4.9f, -1.69f)
                    lineTo(18.31f, 7.1f)
                    curveTo(19.37f, 8.45f, 20f, 10.15f, 20f, 12f)
                    curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
                    close()
                }
            }.build()
            return _block!!
        }

    val Domain: ImageVector
        get() {
            if (_domain != null) return _domain!!
            _domain = ImageVector.Builder(
                name = "Domain",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 7f)
                    verticalLineTo(3f)
                    horizontalLineTo(2f)
                    verticalLineToRelative(18f)
                    horizontalLineToRelative(20f)
                    verticalLineTo(7f)
                    horizontalLineTo(12f)
                    close()
                    moveTo(6f, 19f)
                    horizontalLineTo(4f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(6f, 15f)
                    horizontalLineTo(4f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(6f, 11f)
                    horizontalLineTo(4f)
                    verticalLineTo(9f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(6f, 7f)
                    horizontalLineTo(4f)
                    verticalLineTo(5f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(10f, 19f)
                    horizontalLineTo(8f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(10f, 15f)
                    horizontalLineTo(8f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(10f, 11f)
                    horizontalLineTo(8f)
                    verticalLineTo(9f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(10f, 7f)
                    horizontalLineTo(8f)
                    verticalLineTo(5f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(20f, 19f)
                    horizontalLineToRelative(-8f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(-2f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-2f)
                    horizontalLineToRelative(-2f)
                    verticalLineTo(9f)
                    horizontalLineToRelative(8f)
                    verticalLineToRelative(10f)
                    close()
                    moveTo(18f, 11f)
                    horizontalLineToRelative(-2f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-2f)
                    close()
                    moveTo(18f, 15f)
                    horizontalLineToRelative(-2f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(-2f)
                    close()
                }
            }.build()
            return _domain!!
        }

    val TipsAndUpdates: ImageVector
        get() {
            if (_tips != null) return _tips!!
            _tips = ImageVector.Builder(
                name = "TipsAndUpdates",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(7f, 20f)
                    horizontalLineToRelative(4f)
                    curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                    reflectiveCurveToRelative(-2f, -0.9f, -2f, -2f)
                    close()
                    moveTo(5f, 19f)
                    horizontalLineToRelative(8f)
                    verticalLineToRelative(-1f)
                    horizontalLineTo(5f)
                    verticalLineToRelative(1f)
                    close()
                    moveTo(16.5f, 9.5f)
                    curveToRelative(0f, 3.82f, -2.66f, 5.86f, -3.77f, 6.5f)
                    horizontalLineTo(6.27f)
                    curveTo(5.16f, 15.36f, 2.5f, 13.32f, 2.5f, 9.5f)
                    curveTo(2.5f, 5.36f, 5.86f, 2f, 10f, 2f)
                    curveToRelative(3.56f, 0f, 6.22f, 2.07f, 6.5f, 7.5f)
                    close()
                    moveTo(19.07f, 4.93f)
                    lineToRelative(1.41f, -1.41f)
                    lineToRelative(1.41f, 1.41f)
                    lineToRelative(-1.41f, 1.41f)
                    close()
                    moveTo(22f, 10f)
                    horizontalLineToRelative(-2f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(2f)
                    close()
                    moveTo(19.07f, 17.07f)
                    lineToRelative(1.41f, 1.41f)
                    lineToRelative(-1.41f, 1.41f)
                    lineToRelative(-1.41f, -1.41f)
                    close()
                }
            }.build()
            return _tips!!
        }

    val Weekend: ImageVector get() = Domain
    val Sprint: ImageVector get() = DirectionsRun
    val SportsMartialArts: ImageVector get() = FitnessCenter
    val MonitorWeight: ImageVector get() = Domain
    val LinearScale: ImageVector get() = ChevronRight
    val HorizontalRule: ImageVector
        get() {
            if (_rule != null) return _rule!!
            _rule = ImageVector.Builder(
                name = "HorizontalRule",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(4f, 11f)
                    horizontalLineToRelative(16f)
                    verticalLineToRelative(2f)
                    horizontalLineTo(4f)
                    close()
                }
            }.build()
            return _rule!!
        }

    val Lock: ImageVector
        get() {
            if (_lock != null) return _lock!!
            _lock = ImageVector.Builder(
                name = "Lock",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18f, 8f)
                    horizontalLineToRelative(-1f)
                    verticalLineTo(6f)
                    curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
                    reflectiveCurveTo(7f, 3.24f, 7f, 6f)
                    verticalLineToRelative(2f)
                    horizontalLineTo(6f)
                    curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                    verticalLineToRelative(10f)
                    curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                    horizontalLineToRelative(12f)
                    curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                    verticalLineTo(10f)
                    curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                    close()
                    moveTo(12f, 17f)
                    curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                    reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
                    reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
                    reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
                    close()
                    moveTo(15.1f, 8f)
                    horizontalLineTo(8.9f)
                    verticalLineTo(6f)
                    curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
                    curveToRelative(1.71f, 0f, 3.1f, 1.39f, 3.1f, 3.1f)
                    verticalLineToRelative(2f)
                    close()
                }
            }.build()
            return _lock!!
        }

    val Close: ImageVector by lazy {
        pathIcon("Close", "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12z")
    }
    val Add: ImageVector by lazy { pathIcon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val Person: ImageVector by lazy {
        pathIcon(
            "Person",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
        )
    }
    val Notifications: ImageVector by lazy {
        pathIcon(
            "Notifications",
            "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 " +
                "-1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
        )
    }
    val Star: ImageVector by lazy {
        pathIcon("Star", "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z")
    }
    val Edit: ImageVector by lazy {
        pathIcon(
            "Edit",
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39," +
                "-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }
    val VerifiedUser: ImageVector by lazy {
        pathIcon(
            "VerifiedUser",
            "M12,1L3,5v6c0,5.55 3.84,10.74 9,12 5.16,-1.26 9,-6.45 9,-12V5l-9,-4zM10,17l-4,-4 1.41,-1.41L10,14.17l6.59,-6.59L18,9l-8,8z",
        )
    }
    val ShowChart: ImageVector by lazy {
        pathIcon("ShowChart", "M3.5,18.49l6,-6.01 4,4L22,6.92l-1.41,-1.41 -7.09,7.97 -4,-4L2,16.99z")
    }
    val TrendingDown: ImageVector by lazy {
        pathIcon("TrendingDown", "M16,18l2.29,-2.29 -4.88,-4.88 -4,4L2,7.41 3.41,6l6,6 4,-4 6.3,6.29L22,12v6z")
    }
    val Schedule: ImageVector by lazy {
        pathIcon(
            "Schedule",
            "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 " +
                "-8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }
    val Bolt: ImageVector by lazy { pathIcon("Bolt", "M7,2v11h3v9l7,-12h-4l4,-8z") }
    val CalendarToday: ImageVector by lazy {
        pathIcon(
            "CalendarToday",
            "M20,3h-1V1h-2v2H7V1H5v2H4c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM20,21H4V8h16v13z",
        )
    }
    val Gift: ImageVector by lazy {
        pathIcon(
            "Gift",
            "M20,6h-2.18c0.11,-0.31 0.18,-0.65 0.18,-1 0,-1.66 -1.34,-3 -3,-3 -1.05,0 -1.96,0.54 -2.5,1.35l-0.5,0.67 " +
                "-0.5,-0.68C10.96,2.54 10.05,2 9,2 7.34,2 6,3.34 6,5c0,0.35 0.07,0.69 0.18,1H4c-1.11,0 -1.99,0.89 -1.99,2L2," +
                "19c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V8c0,-1.11 -0.89,-2 -2,-2zM15,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 " +
                "-1,-0.45 -1,-1 0.45,-1 1,-1zM9,4c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM20,19H4v-2h16v2z" +
                "M20,14H4V8h5.08L7,10.83 8.62,12 11,8.76l1,-1.36 1,1.36L15.38,12 17,10.83 14.92,8H20v6z",
        )
    }
    val Settings: ImageVector by lazy {
        pathIcon(
            "Settings",
            "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 " +
                "0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4," +
                "2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 " +
                "7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84," +
                "11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 " +
                "0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24," +
                "0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92," +
                "-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6" +
                "s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
        )
    }
    val PhotoCamera: ImageVector by lazy {
        pathIcon(
            "PhotoCamera",
            "M12,15.2a3.2,3.2 0,1 0,0 -6.4a3.2,3.2 0,1 0,0 6.4zM9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1," +
                "0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
        )
    }
    val Visibility: ImageVector by lazy {
        pathIcon(
            "Visibility",
            "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17c" +
                "-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
        )
    }
    val VisibilityOff: ImageVector by lazy {
        pathIcon(
            "VisibilityOff",
            "M12,7c2.76,0 5,2.24 5,5 0,0.65 -0.13,1.26 -0.36,1.83l2.92,2.92c1.51,-1.26 2.7,-2.89 3.43,-4.75 -1.73,-4.39 " +
                "-6,-7.5 -11,-7.5 -1.4,0 -2.74,0.25 -3.98,0.7l2.16,2.16C10.74,7.13 11.35,7 12,7zM2,4.27l2.28,2.28 0.46,0.46C3.08," +
                "8.3 1.78,10.02 1,12c1.73,4.39 6,7.5 11,7.5 1.55,0 3.03,-0.3 4.38,-0.84l0.42,0.42L19.73,22 21,20.73 3.27,3 2," +
                "4.27zM7.53,9.8l1.55,1.55c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.66 1.34,3 3,3 0.22,0 0.44,-0.03 0.65,-0.08l1.55," +
                "1.55c-0.67,0.33 -1.41,0.53 -2.2,0.53 -2.76,0 -5,-2.24 -5,-5 0,-0.79 0.2,-1.53 0.53,-2.2zM11.84,9.02l3.15,3.15 " +
                "0.02,-0.16c0,-1.66 -1.34,-3 -3,-3l-0.17,0.01z",
        )
    }
    val Trophy: ImageVector by lazy {
        pathIcon(
            "Trophy",
            "M19,5h-2V3H7v2H5C3.9,5 3,5.9 3,7v1c0,2.55 1.92,4.63 4.39,4.94c0.63,1.5 1.98,2.63 3.61,2.96V19H7v2h10v-2h-4v-3.1" +
                "c1.63,-0.33 2.98,-1.46 3.61,-2.96C19.08,12.63 21,10.55 21,8V7C21,5.9 20.1,5 19,5zM5,8V7h2v3.82C5.84,10.4 5,9.3 " +
                "5,8zM19,8c0,1.3 -0.84,2.4 -2,2.82V7h2V8z",
        )
    }
    val Restaurant: ImageVector by lazy {
        pathIcon(
            "Restaurant",
            "M11,9H9V2H7v7H5V2H3v7c0,2.12 1.66,3.84 3.75,3.97V22h2.5v-9.03C11.34,12.84 13,11.12 13,9V2h-2V9zM16,6v8h2.5v8H21V2C18.24,2 16,4.24 16,6z",
        )
    }
    val Home: ImageVector by lazy { pathIcon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z") }
    val ExpandMore: ImageVector by lazy { pathIcon("ExpandMore", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z") }
    val Warning: ImageVector by lazy { pathIcon("Warning", "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z") }
    val AutoAwesome: ImageVector by lazy {
        pathIcon(
            "AutoAwesome",
            "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20" +
                "l2.5,-5.5L17,12l-5.5,-2.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z",
        )
    }
    val MenuBook: ImageVector by lazy {
        pathIcon(
            "MenuBook",
            "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1," +
                "6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5" +
                "c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5," +
                "-0.5V6C22.4,5.55 21.75,5.25 21,5zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.7,0 -4.15,0.65 -5.5,1.5V8c1.35," +
                "-0.85 3.8,-1.5 5.5,-1.5c1.2,0 2.4,0.15 3.5,0.5V18.5z",
        )
    }
    val Straighten: ImageVector by lazy {
        pathIcon(
            "Straighten",
            "M21,6H3C1.9,6 1,6.9 1,8v8c0,1.1 0.9,2 2,2h18c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM21,16H3V8h2v4h2V8h2v4h2V8" +
                "h2v4h2V8h2v4h2V8h2v8z",
        )
    }
    val Logout: ImageVector by lazy {
        pathIcon(
            "Logout",
            "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5zM4,5h8V3H4c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h8v-2H4V5z",
        )
    }
    val Male: ImageVector by lazy {
        pathIcon(
            "Male",
            "M9.5,11c1.93,0 3.5,1.57 3.5,3.5S11.43,18 9.5,18S6,16.43 6,14.5S7.57,11 9.5,11zM9.5,9C6.46,9 4,11.46 4,14.5" +
                "S6.46,20 9.5,20s5.5,-2.46 5.5,-5.5c0,-1.16 -0.36,-2.23 -0.97,-3.12L18,7.42V10h2V4h-6v2h2.58l-3.97,3.97C11.73," +
                "9.36 10.66,9 9.5,9z",
        )
    }
    val Female: ImageVector by lazy {
        pathIcon(
            "Female",
            "M17.5,9.5C17.5,6.46 15.04,4 12,4S6.5,6.46 6.5,9.5c0,2.7 1.94,4.93 4.5,5.4V17H9v2h2v2h2v-2h2v-2h-2v-2.1C15.56," +
                "14.43 17.5,12.2 17.5,9.5zM8.5,9.5C8.5,7.57 10.07,6 12,6s3.5,1.57 3.5,3.5S13.93,13 12,13S8.5,11.43 8.5,9.5z",
        )
    }

    /** Brand mark in its own colors; draw with `Image`, not a tinted `Icon`. */
    val GoogleLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "GoogleLogo",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            addPath(
                addPathNodes(
                    "M24,9.5c3.54,0 6.71,1.22 9.21,3.6l6.85,-6.85C35.9,2.38 30.47,0 24,0 14.62,0 6.51,5.38 2.56,13.22l7.98," +
                        "6.19C12.43,13.72 17.74,9.5 24,9.5z",
                ),
                fill = SolidColor(Color(0xFFEA4335)),
            )
            addPath(
                addPathNodes(
                    "M46.98,24.55c0,-1.57 -0.15,-3.09 -0.38,-4.55H24v9.02h12.94c-0.58,2.96 -2.26,5.48 -4.78,7.18l7.73,6" +
                        "c4.51,-4.18 7.09,-10.36 7.09,-17.65z",
                ),
                fill = SolidColor(Color(0xFF4285F4)),
            )
            addPath(
                addPathNodes(
                    "M10.53,28.59c-0.48,-1.45 -0.76,-2.99 -0.76,-4.59s0.27,-3.14 0.76,-4.59l-7.98,-6.19C0.92,16.46 0,20.12 0," +
                        "24c0,3.88 0.92,7.54 2.56,10.78l7.97,-6.19z",
                ),
                fill = SolidColor(Color(0xFFFBBC05)),
            )
            addPath(
                addPathNodes(
                    "M24,48c6.48,0 11.93,-2.13 15.89,-5.81l-7.73,-6c-2.15,1.45 -4.92,2.3 -8.16,2.3 -6.26,0 -11.57,-4.22 " +
                        "-13.47,-9.91l-7.98,6.19C6.51,42.62 14.62,48 24,48z",
                ),
                fill = SolidColor(Color(0xFF34A853)),
            )
        }.build()
    }

    val AppleLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppleLogo",
            defaultWidth = 18.dp,
            defaultHeight = 22.dp,
            viewportWidth = 384f,
            viewportHeight = 512f,
        ).apply {
            addPath(
                addPathNodes(
                    "M318.7,268.7c-0.2,-36.7 16.4,-64.4 50,-84.8 -18.8,-26.9 -47.2,-41.7 -84.7,-44.6 -35.5,-2.8 -74.3,20.7 " +
                        "-88.5,20.7 -15,0 -49.4,-19.7 -76.4,-19.7C63.3,141.2 4,184.8 4,273.5c0,26.2 4.8,53.3 14.4,81.2 12.8," +
                        "36.7 59,126.7 107.2,125.2 25.2,-0.6 43,-17.9 75.8,-17.9 31.8,0 48.3,17.9 76.4,17.9 48.6,-0.7 90.4," +
                        "-82.5 102.6,-119.3 -65.2,-30.7 -61.7,-90 -61.7,-91.9zM262.1,104.5c27.3,-32.4 24.8,-61.9 24,-72.5 " +
                        "-24.1,1.4 -52,16.4 -67.9,34.9 -17.5,19.8 -27.8,44.3 -25.6,71.9 26.1,2 49.9,-11.4 69.5,-34.3z",
                ),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }
}

private fun pathIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()

private var _fitnessCenter: ImageVector? = null
private var _chevronLeft: ImageVector? = null
private var _chevronRight: ImageVector? = null
private var _arrowBack: ImageVector? = null
private var _mail: ImageVector? = null
private var _arrowForward: ImageVector? = null
private var _check: ImageVector? = null
private var _checkCircle: ImageVector? = null
private var _favorite: ImageVector? = null
private var _fire: ImageVector? = null
private var _run: ImageVector? = null
private var _walk: ImageVector? = null
private var _block: ImageVector? = null
private var _domain: ImageVector? = null
private var _tips: ImageVector? = null
private var _rule: ImageVector? = null
private var _lock: ImageVector? = null
