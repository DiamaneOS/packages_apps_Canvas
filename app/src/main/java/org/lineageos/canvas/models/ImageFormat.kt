/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.canvas.models

import android.graphics.Bitmap
import android.net.Uri

data class ImageFormat(
    val mimeType: String,
    val extension: String,
    val compressFormat: Bitmap.CompressFormat,
) {
    companion object {
        val JPEG = ImageFormat(
            "image/jpeg",
            "jpg",
            Bitmap.CompressFormat.JPEG,
        )

        val PNG = ImageFormat(
            "image/png",
            "png",
            Bitmap.CompressFormat.PNG,
        )

        val WEBP = ImageFormat(
            "image/webp",
            "webp",
            Bitmap.CompressFormat.WEBP_LOSSLESS,
        )

        fun fromMimeTypeOrNull(mimeType: String?) = when (mimeType) {
            JPEG.mimeType -> JPEG
            PNG.mimeType -> PNG
            WEBP.mimeType -> WEBP
            else -> null
        }

        fun fromMimeType(mimeType: String) =
            fromMimeTypeOrNull(mimeType) ?: error("Unsupported image type: $mimeType")
    }
}

data class Image(
    val uri: Uri,
    val mimeType: String,
    val format: ImageFormat,
)
