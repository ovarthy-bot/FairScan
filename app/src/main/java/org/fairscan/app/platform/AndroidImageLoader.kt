/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.fairscan.app.platform

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.util.component1
import androidx.core.util.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fairscan.app.domain.ImageLoader
import java.io.IOException

class AndroidImageLoader(
    private val contentResolver: ContentResolver
) : ImageLoader {

    override suspend fun load(uri: Uri): Bitmap {
        val bitmap = loadBitmapFromUri(contentResolver, uri)
        return ensureArgb8888(bitmap)
    }
}

suspend fun loadBitmapFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    maxPixels: Int = 12_000_000,
): Bitmap = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= 28) {
        decodeWithImageDecoder(contentResolver, uri, maxPixels)
    } else {
        decodeWithBitmapFactory(contentResolver, uri)
    }
}

@RequiresApi(28)
private fun decodeWithImageDecoder(
    contentResolver: ContentResolver,
    uri: Uri,
    maxPixels: Int
): Bitmap {
    val source = ImageDecoder.createSource(contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (width, height) = info.size
        val scale = computeScale(width, height, maxPixels)
        decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
    }
}

private fun decodeWithBitmapFactory(contentResolver: ContentResolver, uri: Uri, ): Bitmap {
    val options = BitmapFactory.Options()
    val inputStream = contentResolver.openInputStream(uri)
        ?: throw IOException("Cannot open input stream for uri: $uri")
    return inputStream.use {
        BitmapFactory.decodeStream(it, null, options)
            ?: throw IOException("Failed to decode bitmap for uri: $uri")
    }
}

private fun computeScale(width: Int, height: Int, maxPixels: Int): Float {
    val pixels = width * height
    return if (pixels > maxPixels) {
        maxPixels.toFloat() / pixels
    } else {
        1f
    }
}

private fun ensureArgb8888(bitmap: Bitmap): Bitmap {
    return if (bitmap.config != Bitmap.Config.ARGB_8888) {
        bitmap.copy(Bitmap.Config.ARGB_8888, true)
    } else {
        bitmap
    }
}

suspend fun renderPdfPagesFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    maxPixelsPerPage: Int = 12_000_000,
): List<Bitmap> = withContext(Dispatchers.IO) {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext bitmaps
        pfd.use { descriptor ->
            val renderer = android.graphics.pdf.PdfRenderer(descriptor)
            renderer.use { pdfRenderer ->
                val pageCount = pdfRenderer.pageCount
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val scale = 2.5f
                    var targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    var targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    val pixels = targetWidth.toLong() * targetHeight.toLong()
                    if (pixels > maxPixelsPerPage) {
                        val factor = kotlin.math.sqrt(maxPixelsPerPage.toDouble() / pixels)
                        targetWidth = (targetWidth * factor).toInt().coerceAtLeast(1)
                        targetHeight = (targetHeight * factor).toInt().coerceAtLeast(1)
                    }
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }
            }
        }
    } catch (e: Exception) {
        // Return whatever pages were decoded
    }
    bitmaps
}

