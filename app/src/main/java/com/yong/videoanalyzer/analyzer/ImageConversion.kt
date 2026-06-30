package com.yong.videoanalyzer.analyzer

import android.graphics.Bitmap
import android.media.Image
import java.nio.ByteBuffer

/**
 * Convert YUV_420_888 [Image] to an ARGB [Bitmap] instance
 * - apply scaled within [maxSize] px size.
 */
internal fun Image.toScaledBitmap(maxSize: Int): Bitmap {
    val scale = minOf(1.0f, maxSize.toFloat() / maxOf(width, height))
    val dstWidth = maxOf(1, (width * scale).toInt())
    val dstHeight = maxOf(1, (height * scale).toInt())

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]
    val yBytes = yPlane.buffer.toByteArray()
    val uBytes = uPlane.buffer.toByteArray()
    val vBytes = vPlane.buffer.toByteArray()

    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride

    val argb = IntArray(dstWidth * dstHeight)
    for (dy in 0 until dstHeight) {
        val sy = dy * height / dstHeight
        val yRow = sy * yRowStride
        val uvRow = (sy / 2) * uvRowStride
        for (dx in 0 until dstWidth) {
            val sx = dx * width / dstWidth
            val uvCol = (sx / 2) * uvPixelStride

            val y = yBytes[yRow + sx * yPixelStride].toInt() and 0xff
            val u = (uBytes[uvRow + uvCol].toInt() and 0xff) - 128
            val v = (vBytes[uvRow + uvCol].toInt() and 0xff) - 128

            // BT.601 YUV -> RGB
            val r = (y + 1.370705f * v).toInt().coerceIn(0, 255)
            val g = (y - 0.337633f * u - 0.698001f * v).toInt().coerceIn(0, 255)
            val b = (y + 1.732446f * u).toInt().coerceIn(0, 255)

            argb[dy * dstWidth + dx] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(argb, dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val bytes = ByteArray(remaining())
    get(bytes)
    return bytes
}
