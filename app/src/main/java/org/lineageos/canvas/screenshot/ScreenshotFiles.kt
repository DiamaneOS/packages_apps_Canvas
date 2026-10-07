/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.screenshot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import org.lineageos.canvas.models.ImageFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * The screenshot Edit flow's file work: detection, deletion and the clipboard copy. Uses only the
 * access SystemUI granted for the one screenshot URI and Canvas's own files; no permissions.
 */
object ScreenshotFiles {
    private const val TAG = "CanvasScreenshot"

    /** Canvas's private folder for clipboard copies; the only folder the "clipboard" path shares. */
    private const val CLIPBOARD_DIR = "clipboard"

    /** Intent-level check, without I/O. */
    fun isScreenshotRequest(intent: Intent, uri: Uri) = ScreenshotRules.isScreenshotRequest(
        isEditAction = intent.action == Intent.ACTION_EDIT,
        editSource = intent.getStringExtra(ScreenshotRules.EXTRA_EDIT_SOURCE),
        hasWriteGrant = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0,
        scheme = uri.scheme,
        authority = uri.authority,
    )

    /** Whether the granted MediaStore item sits in the screenshot folder. Blocking. */
    fun isInScreenshotsFolder(contentResolver: ContentResolver, uri: Uri) = runCatching {
        contentResolver.query(
            uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null
        )?.use { cursor ->
            cursor.moveToFirst() && ScreenshotRules.isInScreenshotsFolder(cursor.getString(0))
        } ?: false
    }.getOrElse {
        Log.w(TAG, "Cannot read where the image is saved", it)
        false
    }

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        data class NeedsConfirmation(val intentSender: IntentSender) : DeleteResult
        data object Failed : DeleteResult
    }

    /**
     * Deletes the screenshot with the granted access. If MediaStore refuses, asks the system for
     * a delete request, which the user confirms in a system dialog. Blocking.
     */
    fun delete(contentResolver: ContentResolver, uri: Uri): DeleteResult {
        try {
            if (contentResolver.delete(uri, null) > 0) {
                return DeleteResult.Deleted
            }
            Log.w(TAG, "MediaStore deleted nothing; asking for a delete request")
        } catch (e: SecurityException) {
            // Includes RecoverableSecurityException.
            Log.w(TAG, "No delete access; asking for a delete request", e)
        }

        return runCatching {
            DeleteResult.NeedsConfirmation(
                MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender
            )
        }.getOrElse {
            Log.e(TAG, "Cannot ask to delete the screenshot", it)
            DeleteResult.Failed
        }
    }

    /**
     * Puts the image on the clipboard: the edited [bitmap] if there is one, else the original
     * bytes. Canvas keeps one copy: earlier copies are removed first, and each copy gets a new
     * name, so a reader granted an older copy cannot read this one. The clipboard grants readers
     * read access to this one file URI only. Blocking.
     */
    fun copyToClipboard(
        context: Context,
        sourceUri: Uri,
        format: ImageFormat,
        bitmap: Bitmap?,
        label: CharSequence,
    ) {
        val dir = File(context.filesDir, CLIPBOARD_DIR)
        clearClipboardFiles(dir)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Cannot create the clipboard folder")
        }

        val file = File(
            dir,
            ScreenshotRules.clipboardFileName(UUID.randomUUID().toString(), format.extension),
        )
        try {
            FileOutputStream(file).use { out ->
                if (bitmap != null) {
                    if (!bitmap.compress(format.compressFormat, 100, out)) {
                        throw IOException("Image compression failed")
                    }
                } else {
                    val input = context.contentResolver.openInputStream(sourceUri)
                        ?: throw IOException("Cannot read the screenshot")
                    input.use { it.copyTo(out) }
                }
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val clip = ClipData.newUri(context.contentResolver, label, uri)
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }

    /** Removes clipboard copies older than the clipboard's own clear time. */
    fun pruneStaleClipboardFiles(context: Context) {
        val now = System.currentTimeMillis()
        File(context.filesDir, CLIPBOARD_DIR).listFiles()?.forEach {
            if (ScreenshotRules.isStaleClipboardFile(it.lastModified(), now)) {
                it.delete()
            }
        }
    }

    private fun clearClipboardFiles(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }
}
