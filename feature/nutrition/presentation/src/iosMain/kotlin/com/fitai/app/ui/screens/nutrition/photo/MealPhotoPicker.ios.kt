package com.fitai.app.ui.screens.nutrition.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val IMAGE_TYPE_IDENTIFIER = "public.image"

@Composable
actual fun rememberMealPhotoPicker(
    onPicked: (bytes: ByteArray, mimeType: String) -> Unit,
    onCancelled: () -> Unit,
): MealPhotoPicker {
    val hostController = LocalUIViewController.current
    val scope = rememberCoroutineScope()
    val latestOnPicked by rememberUpdatedState(onPicked)
    val latestOnCancelled by rememberUpdatedState(onCancelled)

    // UIKit holds picker delegates weakly, so they live in the composition.
    val delegates = remember {
        val onImage: (UIImage?) -> Unit = { image ->
            if (image == null) {
                latestOnCancelled()
            } else {
                scope.launch {
                    val bytes = withContext(Dispatchers.Default) { image.toUploadJpeg() }
                    if (bytes != null) latestOnPicked(bytes, MEAL_PHOTO_MIME_TYPE) else latestOnCancelled()
                }
            }
        }
        CameraDelegate(onImage) to GalleryDelegate(onImage)
    }
    val cameraAvailable = remember {
        UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
        )
    }
    return remember(hostController, cameraAvailable) {
        val launchGallery = {
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = configuration)
            picker.delegate = delegates.second
            hostController.topmost().presentViewController(picker, animated = true, completion = null)
        }
        MealPhotoPicker(
            isCameraAvailable = cameraAvailable,
            launchCamera = {
                if (cameraAvailable) {
                    val picker = UIImagePickerController()
                    picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                    picker.delegate = delegates.first
                    hostController.topmost().presentViewController(picker, animated = true, completion = null)
                } else {
                    launchGallery()
                }
            },
            launchGallery = launchGallery,
        )
    }
}

private fun UIViewController.topmost(): UIViewController {
    var top = this
    while (true) top = top.presentedViewController ?: return top
}

private class CameraDelegate(
    private val onResult: (UIImage?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, null)
        onResult(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onResult(null)
    }
}

private class GalleryDelegate(
    private val onResult: (UIImage?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null || !provider.hasItemConformingToTypeIdentifier(IMAGE_TYPE_IDENTIFIER)) {
            onResult(null)
            return
        }
        provider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE_IDENTIFIER) { data, _ ->
            val image = data?.let { UIImage(data = it) }
            dispatch_async(dispatch_get_main_queue()) { onResult(image) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toUploadJpeg(): ByteArray? {
    val (width, height) = size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = min(1.0, MEAL_PHOTO_MAX_SIDE_PX / max(width, height))
    val targetWidth = floor(width * scale)
    val targetHeight = floor(height * scale)
    val format = UIGraphicsImageRendererFormat.preferredFormat().apply {
        this.scale = 1.0
        opaque = true
    }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
    // drawInRect also bakes in imageOrientation, so camera shots are uploaded upright.
    val resized = renderer.imageWithActions { _ ->
        drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    }
    return UIImageJPEGRepresentation(resized, MEAL_PHOTO_JPEG_QUALITY / 100.0)?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
    return result
}
