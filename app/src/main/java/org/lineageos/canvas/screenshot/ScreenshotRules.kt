/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.screenshot

/**
 * Decisions for SystemUI's screenshot Edit flow, kept free of Android classes so they can be
 * tested on the host.
 */
object ScreenshotRules {
    /** Extra SystemUI's screenshot Edit (ActionIntentCreator.createEdit) puts on ACTION_EDIT. */
    const val EXTRA_EDIT_SOURCE = "edit_source"
    const val EDIT_SOURCE_SCREENSHOT = "screenshot"

    /** MediaStore's authority; SystemUI removes the user id before handing the URI over. */
    const val MEDIA_STORE_AUTHORITY = "media"

    /** Where SystemUI saves screenshots: Environment.DIRECTORY_PICTURES/DIRECTORY_SCREENSHOTS. */
    private const val SCREENSHOTS_RELATIVE_PATH = "Pictures/Screenshots/"

    /** How long a copy may stay on the clipboard before Canvas removes its file. */
    const val CLIPBOARD_FILE_MAX_AGE_MS = 60 * 60 * 1000L

    /**
     * Whether an ACTION_EDIT request looks like SystemUI's screenshot Edit: the screenshot source
     * extra, a write grant and a MediaStore content URI. Anything else keeps Canvas's own flow.
     */
    fun isScreenshotRequest(
        isEditAction: Boolean,
        editSource: String?,
        hasWriteGrant: Boolean,
        scheme: String?,
        authority: String?,
    ) = isEditAction &&
            editSource == EDIT_SOURCE_SCREENSHOT &&
            hasWriteGrant &&
            scheme == "content" &&
            authority == MEDIA_STORE_AUTHORITY

    /** Whether MediaStore's RELATIVE_PATH of the item is SystemUI's screenshot folder. */
    fun isInScreenshotsFolder(relativePath: String?): Boolean {
        val path = relativePath?.trim()?.trimEnd('/')?.plus('/') ?: return false
        return path.equals(SCREENSHOTS_RELATIVE_PATH, ignoreCase = true)
    }

    /** What closing the editor does. */
    enum class Close {
        /** Leave; the screenshot stays as it is. */
        LEAVE,

        /** Ask whether to save the edits, discard them or delete the screenshot. */
        ASK,
    }

    fun onClose(isScreenshot: Boolean, hasEdits: Boolean) = when {
        isScreenshot && hasEdits -> Close.ASK
        else -> Close.LEAVE
    }

    /** Whether Save has to write: an unedited screenshot is left untouched. */
    fun saveWrites(hasEdits: Boolean) = hasEdits

    /** Clipboard file names: a new name per copy, so an older grant never reads a newer copy. */
    fun clipboardFileName(randomId: String, extension: String) = "copy-$randomId.$extension"

    /** Whether a file in Canvas's clipboard folder may be removed when Canvas starts. */
    fun isStaleClipboardFile(lastModifiedMs: Long, nowMs: Long) =
        nowMs - lastModifiedMs >= CLIPBOARD_FILE_MAX_AGE_MS || lastModifiedMs > nowMs
}
