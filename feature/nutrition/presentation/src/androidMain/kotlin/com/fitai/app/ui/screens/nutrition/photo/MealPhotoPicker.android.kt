package com.fitai.app.ui.screens.nutrition.photo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Declared in this module's manifest; a subclass avoids merger clashes with other FileProviders. */
class MealPhotoFileProvider : FileProvider()

private const val CAPTURE_DIR = "meal_photos"

@Composable
actual fun rememberMealPhotoPicker(
    onPicked: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancelled: () -> Unit,
): MealPhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPicked by rememberUpdatedState(onPicked)
    val latestOnCancelled by rememberUpdatedState(onCancelled)
    // Survives the camera activity recreating ours after process death.
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun deliver(uri: Uri, deleteAfter: Boolean) {
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val encoded = runCatching { context.encodeForUpload(uri) }.getOrNull()
                if (deleteAfter) runCatching { context.contentResolver.delete(uri, null, null) }
                encoded
            }
            if (bytes != null) latestOnPicked(bytes, MEAL_PHOTO_MIME_TYPE) else latestOnCancelled()
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) latestOnCancelled() else deliver(uri, deleteAfter = false)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCaptureUri?.toUri()
        pendingCaptureUri = null
        when {
            uri == null -> latestOnCancelled()
            saved -> deliver(uri, deleteAfter = true)
            else -> {
                runCatching { context.contentResolver.delete(uri, null, null) }
                latestOnCancelled()
            }
        }
    }
    val hasCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    return remember(context, gallery, camera, hasCamera) {
        val launchGallery = {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        MealPhotoPicker(
            isCameraAvailable = hasCamera,
            launchCamera = {
                val uri = runCatching { context.newCaptureUri() }.getOrNull()
                if (uri == null) {
                    launchGallery()
                } else {
                    pendingCaptureUri = uri.toString()
                    try {
                        camera.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        pendingCaptureUri = null
                        launchGallery()
                    }
                }
            },
            launchGallery = launchGallery,
        )
    }
}

private fun Context.newCaptureUri(): Uri {
    val dir = File(cacheDir, CAPTURE_DIR).apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.mealphoto.fileprovider", file)
}

private fun Context.encodeForUpload(uri: Uri): ByteArray? {
    val bitmap = decodeDownscaled(uri) ?: return null
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, MEAL_PHOTO_JPEG_QUALITY, out)
        bitmap.recycle()
        out.toByteArray()
    }
}

private fun Context.decodeDownscaled(uri: Uri): Bitmap? {
    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies EXIF orientation; BitmapFactory below does not.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
            decoder.setTargetSampleSize(sampleSizeFor(max(info.size.width, info.size.height)))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(longest) }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } ?: return null
    return decoded.scaledToMaxSide()
}

private fun sampleSizeFor(longestSide: Int): Int {
    var sample = 1
    while (longestSide / (sample * 2) >= MEAL_PHOTO_MAX_SIDE_PX) sample *= 2
    return sample
}

private fun Bitmap.scaledToMaxSide(): Bitmap {
    val longest = max(width, height)
    if (longest <= MEAL_PHOTO_MAX_SIDE_PX) return this
    val scale = MEAL_PHOTO_MAX_SIDE_PX.toFloat() / longest
    val scaled = Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
    if (scaled != this) recycle()
    return scaled
}
