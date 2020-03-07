package com.example.android.camera2basic.util

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log

class TextureHelper {

    companion object {
        private const val TAG = "TextureHelper"

        fun loadTexture(context:Context, resId:Int): Int {
            val textureObjectIds = intArrayOf(0)
            GLES20.glGenTextures(1, textureObjectIds, 0)
            if (textureObjectIds[0] == 0) {
                Log.w(TAG, "loadTexture: Could not generate a new OpenGL texture object.")
                return 0
            }

            val options = BitmapFactory.Options()
            options.inScaled = false
            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            if (bitmap == null) {
                Log.w(TAG, "loadTexture: Resource ID $resId could not be decoded.")
                GLES20.glDeleteTextures(1, textureObjectIds, 0)
                return 0
            }

            Log.d(TAG, "loadTexture: bitmap width = ${bitmap.width} , height = ${bitmap.height}")
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()

            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            return textureObjectIds[0]
        }
    }
}