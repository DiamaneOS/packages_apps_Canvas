/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.content.IntentSender
import android.net.Uri

sealed class EditStatus {
    // No operation in progress
    object Idle : EditStatus()

    // Operation in progress
    object Saving : EditStatus()
    object Sharing : EditStatus()
    object Deleting : EditStatus()

    // Operation completed
    object Saved : EditStatus()
    data class Shared(val uri: Uri, val mimeType: String) : EditStatus()
    data class Error(val message: String) : EditStatus()

    // Screenshot from SystemUI's Edit: deleted, or the system has to confirm the delete
    object Deleted : EditStatus()
    data class DeleteNeedsConfirmation(val intentSender: IntentSender) : EditStatus()
    object DeleteFailed : EditStatus()
    object CopyFailed : EditStatus()
}
