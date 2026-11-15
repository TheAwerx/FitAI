package com.fitai.app.ui.screens.nutrition.photo

import androidx.compose.runtime.Composable

/** Longest side, in pixels, of the JPEG handed to [rememberMealPhotoPicker] callers. */
internal const val MEAL_PHOTO_MAX_SIDE_PX = 1280
internal const val MEAL_PHOTO_JPEG_QUALITY = 80
internal const val MEAL_PHOTO_MIME_TYPE = "image/jpeg"

class MealPhotoPicker(
    val isCameraAvailable: Boolean,
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
)

/**
 * Camera / gallery launcher. [onPicked] receives a downscaled JPEG
 * ([MEAL_PHOTO_MAX_SIDE_PX], quality [MEAL_PHOTO_JPEG_QUALITY]); [onCancelled] fires when the user
 * backs out or the image cannot be decoded.
 */
@Composable
expect fun rememberMealPhotoPicker(
    onPicked: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancelled: () -> Unit,
): MealPhotoPicker
