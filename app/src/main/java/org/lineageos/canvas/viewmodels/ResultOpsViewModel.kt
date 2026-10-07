/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.canvas.R
import org.lineageos.canvas.models.EditStatus
import org.lineageos.canvas.models.ImageFormat
import org.lineageos.canvas.models.Image
import org.lineageos.canvas.screenshot.ScreenshotFiles
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * View model used by the activity to do actions with the final bitmap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultOpsViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Application's [ContentResolver].
     */
    private val contentResolver = application.contentResolver

    /**
     * The source URI of the image.
     */
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri = _uri.asStateFlow()

    /**
     * Whether the image is a screenshot opened by SystemUI's Edit, which gets the Done choices.
     */
    private val _isScreenshot = MutableStateFlow(false)
    val isScreenshot = _isScreenshot.asStateFlow()

    /**
     * The status of the save operation.
     */
    private val _editStatus = MutableStateFlow<EditStatus>(EditStatus.Idle)
    val saveStatus = _editStatus.asStateFlow()

    val image = uri
        .filterNotNull()
        .mapLatest {
            // We know this is always non-null because we filter it
            val mimeType = contentResolver.getType(it)!!
            Image(
                uri = it,
                mimeType = mimeType,
                format = ImageFormat.fromMimeType(mimeType),
            )
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun suggestedFilename(format: ImageFormat): String {
        val fileName = uri.value?.lastPathSegment ?: "image"
        val baseName = fileName.substringBeforeLast(".")
        return "${baseName}_edit.${format.extension}"
    }

    /**
     * Set the URI of the image.
     *
     * @param uri the URI of the image
     * @param isScreenshotRequest whether the request looks like SystemUI's screenshot Edit; the
     *   image's folder is then checked before Done is offered
     */
    fun setUri(uri: Uri, isScreenshotRequest: Boolean = false) {
        if (_uri.value != uri || !isScreenshotRequest) {
            _isScreenshot.value = false
        }
        _uri.value = uri

        if (isScreenshotRequest) {
            viewModelScope.launch {
                val isScreenshot = withContext(Dispatchers.IO) {
                    ScreenshotFiles.isInScreenshotsFolder(contentResolver, uri)
                }
                if (_uri.value == uri) {
                    _isScreenshot.value = isScreenshot
                }
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ScreenshotFiles.pruneStaleClipboardFiles(getApplication<Application>())
        }
    }

    /**
     * Delete the screenshot with the access SystemUI granted.
     */
    fun deleteScreenshot() {
        val uri = _uri.value ?: return

        viewModelScope.launch {
            _editStatus.value = EditStatus.Deleting
            _editStatus.value = withContext(Dispatchers.IO) { deleteStatus(uri) }
        }
    }

    /**
     * Copy the edited image, or the original if [bitmap] is null, to the clipboard, then delete
     * the screenshot. If the copy fails, nothing is deleted.
     */
    fun copyAndDeleteScreenshot(bitmap: ImageBitmap?, label: CharSequence) {
        viewModelScope.launch {
            _editStatus.value = EditStatus.Deleting
            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val image = image.filterNotNull().first()
                    ScreenshotFiles.copyToClipboard(
                        getApplication<Application>(),
                        image.uri,
                        image.format,
                        bitmap?.asAndroidBitmap(),
                        label,
                    )
                    image.uri
                }.fold(
                    onSuccess = { uri ->
                        // SystemUI shows no clipboard preview for this copy, so say it here,
                        // whether or not the delete that follows goes through.
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                getApplication<Application>(), R.string.screenshot_copied, Toast.LENGTH_SHORT
                            ).show()
                        }
                        deleteStatus(uri)
                    },
                    onFailure = { EditStatus.CopyFailed },
                )
            }
        }
    }

    /**
     * The system's delete confirmation is showing; don't show it again for the same request.
     */
    fun onDeleteConfirmationShown() {
        _editStatus.value = EditStatus.Deleting
    }

    /**
     * The system's delete confirmation ended; [deleted] tells whether the user allowed it.
     */
    fun onDeleteConfirmationResult(deleted: Boolean) {
        _editStatus.value = when (deleted) {
            true -> EditStatus.Deleted
            false -> EditStatus.Idle
        }
    }

    private fun deleteStatus(uri: Uri) = when (
        val result = ScreenshotFiles.delete(contentResolver, uri)
    ) {
        is ScreenshotFiles.DeleteResult.Deleted -> EditStatus.Deleted
        is ScreenshotFiles.DeleteResult.NeedsConfirmation ->
            EditStatus.DeleteNeedsConfirmation(result.intentSender)

        is ScreenshotFiles.DeleteResult.Failed -> EditStatus.DeleteFailed
    }

    fun saveImage(bitmap: ImageBitmap) {
        viewModelScope.launch {
            val image = image.filterNotNull().first()
            saveImageToUri(bitmap, image.uri, image.format)
        }
    }

    fun saveImageToUri(bitmap: ImageBitmap, targetUri: Uri, format: ImageFormat) {
        viewModelScope.launch {
            _editStatus.value = EditStatus.Saving

            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val outputStream = contentResolver.openOutputStream(targetUri, "wt")
                        ?: throw IOException("Unable to open output stream")

                    outputStream.use {
                        if (!bitmap.asAndroidBitmap().compress(format.compressFormat, 100, it)) {
                            throw IOException("Image compression failed")
                        }
                    }
                }.fold(
                    onSuccess = {
                        EditStatus.Saved
                    },
                    onFailure = {
                        EditStatus.Error("Failed")
                    }
                )
            }
        }
    }

    fun shareImage(bitmap: ImageBitmap) {
        val context = getApplication<Application>()

        viewModelScope.launch {
            _editStatus.value = EditStatus.Sharing

            _editStatus.value = withContext(Dispatchers.IO) {
                runCatching {
                    val image = image.filterNotNull().first()

                    val imagesDir = File(context.filesDir, "images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val file = File(imagesDir, "share_temp.${image.format.extension}")

                    FileOutputStream(file).use { out ->
                        if (!bitmap.asAndroidBitmap()
                                .compress(image.format.compressFormat, 100, out)
                        ) {
                            throw IOException("Image compression failed")
                        }
                    }

                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    ) to image.mimeType
                }.fold(
                    onSuccess = { (uri, mimeType) ->
                        EditStatus.Shared(uri, mimeType)
                    },
                    onFailure = {
                        EditStatus.Error("Failed")
                    }
                )
            }
        }
    }
}
