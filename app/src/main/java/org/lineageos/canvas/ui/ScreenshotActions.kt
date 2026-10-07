/*
 * SPDX-FileCopyrightText: 2026 The DiamaneOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.ui

/**
 * What Done offers for a screenshot opened by SystemUI's Edit. [hasEdits] tells whether there
 * are active edits; without them Save leaves the file untouched and the copy is the original.
 */
class ScreenshotActions(
    val onSave: (hasEdits: Boolean) -> Unit,
    val onCopyAndDelete: (hasEdits: Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onDiscard: () -> Unit,
)
