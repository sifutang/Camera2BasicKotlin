package com.example.android.camera2basic.util

import android.graphics.ImageFormat
import android.media.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

object CommonUtil {

    private const val TAG = "CommonUtil"

    fun clamp(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else if (amount > high) high else amount
    }

    /**
     * take YUV image from image.
     */
    fun readYuvDataToBuffer(image: Image, format:Int, data: ByteArray): Boolean {
        require(!(format != ImageFormat.NV21 && format != ImageFormat.YV12)) {
            "output only support ImageFormat.NV21 and ImageFormat.YV12 for now"
        }

        val result = readYuvDataToBuffer(image, data)
        if (!result) {
            return false
        }

        // data(YU12): YYYY YYYY UU VV
        if (format == ImageFormat.NV21) {
            // convert to: YYYY YYYY VU VU
            val size = data.size
            val uv = ByteArray(size / 3)
            var uOffset = size / 6 * 4
            var vOffset = size / 6 * 5
            for (i in 0 until uv.size - 1 step 2) {
                uv[i] = data[vOffset++]
                uv[i + 1] = data[uOffset++]
            }

            val uvOffset = size / 3 * 2
            for (i in uvOffset until size) {
                data[i] = uv[i - uvOffset]
            }
        } else if (format == ImageFormat.YV12) {
            // convert to: YYYY YYYY VV UU
            val size = data.size
            val tmp = ByteArray(size / 6)
            val uOffset = size / 6 * 4
            val vOffset = size / 6 * 5
            System.arraycopy(data, uOffset, tmp, 0, tmp.size)
            System.arraycopy(data, vOffset, data, uOffset, tmp.size)
            System.arraycopy(tmp, 0, data, vOffset, tmp.size)
        }

        return true
    }

    /**
     * take YUV data from image, output data format-> YYYYYYYYUUVV
     */
    private fun readYuvDataToBuffer(image: Image, data: ByteArray): Boolean {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("only support ImageFormat.YUV_420_888 for mow")
        }

        val imageWidth = image.width
        val imageHeight = image.height
        val planes = image.planes
        var offset = 0
        for (plane in planes.indices) {
            val buffer = planes[plane].buffer ?: return false
            val rowStride = planes[plane].rowStride
            val pixelStride = planes[plane].pixelStride
            val planeWidth = if (plane == 0) imageWidth else imageWidth / 2
            val planeHeight = if (plane == 0) imageHeight else imageHeight / 2
            if (pixelStride == 1 && rowStride == planeWidth) {
                buffer.get(data, offset, planeWidth * planeHeight)
                offset += planeWidth * planeHeight
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride
                val rowData = ByteArray(rowStride)
                var colOffset: Int
                for (row in 0 until planeHeight - 1) {
                    colOffset = 0
                    buffer.get(rowData, 0, rowStride)
                    for (col in 0 until planeWidth) {
                        data[offset++] = rowData[colOffset]
                        colOffset += pixelStride
                    }
                }
                // Last row is special in some devices:
                // may not contain the full |rowStride| bytes of data
                colOffset = 0
                buffer.get(rowData, 0, min(rowStride, buffer.remaining()))
                for (col in 0 until planeWidth) {
                    data[offset++] = rowData[colOffset]
                    colOffset += pixelStride
                }
            }
        }

        return true
    }

    fun saveImageByteData(data: ByteArray, path: String, name: String): String? {
        val dir = File(path)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null
            }
        }

        val file = File(dir, name)
        var fos: FileOutputStream? = null
        var ret: String? = null
        try {
            fos = FileOutputStream(file)
            fos.write(data)
            ret = file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ret
    }
}