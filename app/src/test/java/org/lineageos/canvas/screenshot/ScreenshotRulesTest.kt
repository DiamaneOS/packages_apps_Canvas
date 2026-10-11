/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.screenshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRulesTest {
    private fun request(
        isEditAction: Boolean = true,
        editSource: String? = "screenshot",
        hasWriteGrant: Boolean = true,
        scheme: String? = "content",
        authority: String? = "media",
    ) = ScreenshotRules.isScreenshotRequest(
        isEditAction, editSource, hasWriteGrant, scheme, authority
    )

    @Test
    fun systemUiScreenshotEdit_isScreenshotRequest() {
        assertTrue(request())
    }

    @Test
    fun otherRequests_keepCanvasFlow() {
        assertFalse(request(isEditAction = false))
        // SystemUI's clipboard overlay sends "clipboard" and no write grant
        assertFalse(request(editSource = "clipboard", hasWriteGrant = false))
        assertFalse(request(editSource = null))
        assertFalse(request(hasWriteGrant = false))
        assertFalse(request(scheme = "file"))
        // A user id in the authority, another provider, Canvas's own provider
        assertFalse(request(authority = "10@media"))
        assertFalse(request(authority = "com.android.providers.downloads.documents"))
        assertFalse(request(authority = "org.lineageos.canvas.fileprovider"))
    }

    @Test
    fun screenshotsFolder_matchesSystemUiFolderOnly() {
        assertTrue(ScreenshotRules.isInScreenshotsFolder("Pictures/Screenshots/"))
        assertTrue(ScreenshotRules.isInScreenshotsFolder("Pictures/Screenshots"))
        assertTrue(ScreenshotRules.isInScreenshotsFolder("pictures/screenshots/"))
        assertFalse(ScreenshotRules.isInScreenshotsFolder("Pictures/Screenshots/Old/"))
        assertFalse(ScreenshotRules.isInScreenshotsFolder("Pictures/"))
        assertFalse(ScreenshotRules.isInScreenshotsFolder("DCIM/Camera/"))
        assertFalse(ScreenshotRules.isInScreenshotsFolder("Download/Pictures/Screenshots/"))
        assertFalse(ScreenshotRules.isInScreenshotsFolder(""))
        assertFalse(ScreenshotRules.isInScreenshotsFolder(null))
    }

    @Test
    fun close_asksOnlyForEditedScreenshots() {
        assertEquals(ScreenshotRules.Close.ASK, ScreenshotRules.onClose(true, true))
        assertEquals(ScreenshotRules.Close.LEAVE, ScreenshotRules.onClose(true, false))
        assertEquals(ScreenshotRules.Close.LEAVE, ScreenshotRules.onClose(false, true))
        assertEquals(ScreenshotRules.Close.LEAVE, ScreenshotRules.onClose(false, false))
    }

    @Test
    fun save_writesOnlyEdits() {
        assertTrue(ScreenshotRules.saveWrites(hasEdits = true))
        assertFalse(ScreenshotRules.saveWrites(hasEdits = false))
    }

    @Test
    fun clipboardFiles_getNewNamesAndExpire() {
        val first = ScreenshotRules.clipboardFileName("a", "png")
        val second = ScreenshotRules.clipboardFileName("b", "png")
        assertEquals("copy-a.png", first)
        assertNotEquals(first, second)

        val hour = ScreenshotRules.CLIPBOARD_FILE_MAX_AGE_MS
        assertFalse(ScreenshotRules.isStaleClipboardFile(lastModifiedMs = 1_000, nowMs = 1_000))
        assertFalse(ScreenshotRules.isStaleClipboardFile(1_000, 1_000 + hour - 1))
        assertTrue(ScreenshotRules.isStaleClipboardFile(1_000, 1_000 + hour))
        // A clock that went back must not keep a file forever
        assertTrue(ScreenshotRules.isStaleClipboardFile(lastModifiedMs = 5_000, nowMs = 1_000))
    }
}
