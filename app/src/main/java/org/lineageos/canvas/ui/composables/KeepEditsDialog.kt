/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.lineageos.canvas.R

/**
 * Asked when closing an edited screenshot: save the edits, discard them (the screenshot stays as
 * it was) or delete the screenshot. Dismissing it returns to the editor.
 */
@Composable
fun KeepEditsDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screenshot_keep_edits_title)) },
        text = { Text(stringResource(R.string.screenshot_keep_edits_message)) },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.screenshot_delete))
            }
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.screenshot_discard))
            }
        },
    )
}
