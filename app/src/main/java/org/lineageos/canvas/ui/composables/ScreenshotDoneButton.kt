/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.lineageos.canvas.R
import org.lineageos.canvas.ui.ScreenshotActions

/**
 * Done for a screenshot opened by SystemUI's Edit, in place of Save: Save, Copy and delete,
 * Delete, then Canvas's Share and Save as.
 */
@Composable
fun ScreenshotDoneButton(
    screenshotActions: ScreenshotActions,
    hasEdits: Boolean,
    onShare: () -> Unit,
    onSaveAs: () -> Unit,
) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        val done = stringResource(R.string.screenshot_done)

        ToolbarTooltip(done, TooltipAnchorPosition.Below) {
            Button(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    contentDescription = null,
                )

                Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                Text(done)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val choose: (() -> Unit) -> () -> Unit = { action ->
                {
                    expanded = false
                    action()
                }
            }

            DoneMenuItem(R.string.save, Icons.Outlined.Save, choose {
                screenshotActions.onSave(hasEdits)
            })
            DoneMenuItem(R.string.screenshot_copy_and_delete, Icons.Outlined.ContentCopy, choose {
                screenshotActions.onCopyAndDelete(hasEdits)
            })
            DoneMenuItem(R.string.screenshot_delete, Icons.Outlined.Delete, choose {
                screenshotActions.onDelete()
            })

            HorizontalDivider()

            DoneMenuItem(R.string.share, Icons.Outlined.Share, choose(onShare))
            DoneMenuItem(R.string.save_as, Icons.Outlined.SaveAs, choose(onSaveAs))
        }
    }
}

@Composable
private fun DoneMenuItem(label: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
    )
}
