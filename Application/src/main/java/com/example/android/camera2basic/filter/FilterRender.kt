package com.example.android.camera2basic.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.util.Log
import com.example.android.camera2basic.camera.CameraRender
import com.example.android.camera2basic.camera.CameraShaderProgram
import com.example.android.camera2basic.particles.Constants
import com.example.android.camera2basic.particles.data.VertexArray

class FilterRender(context: Context) {
    companion object {
        private val vertexData = floatArrayOf(
                -1f,  1f, 0f, 1f,
                -1f, -1f, 0f, 0f,
                1f,  1f, 1f, 1f,
                1f, -1f, 1f, 0f
        )

        private const val TAG = "FilterRender"
        private const val VERTEX_COMPONENT_COUNT = 2
        private const val COORDINATE_COMPONENT_COUNT = 2
        private const val STRIDE =
                (VERTEX_COMPONENT_COUNT + COORDINATE_COMPONENT_COUNT) * Constants.BYTES_PRE_FLOAT
        private const val FILTER_ITEM_COUNT = 3
    }

    private val vertexArray = VertexArray(vertexData)
    private var filterShaderProgram = FilterShaderProgram(context)
    private var cameraShaderProgram = CameraShaderProgram(context)

    private val frameBuffer = IntArray(1) {-1}
    private val frameBufferTexture = IntArray(1) {-1}

    private var renderWith = -1
    private var renderHeight = -1

    private var filterProgress = 0f

    /**
     * w|h|x|y|x|y...
     */
    private var filterPositions = IntArray(FILTER_ITEM_COUNT * FILTER_ITEM_COUNT * 2 + 2)
    private var filterLookupTableBitmaps: ArrayList<Bitmap> =
            ArrayList(FILTER_ITEM_COUNT * FILTER_ITEM_COUNT)

    init {
        val lookupTables = listOf(
                "lolita.png", "fresh.png", "coral.png", // index:678
                "music.png", "makalong.png", "oxygen.png", // index:345
                "first_love.png", "glossy.png", "bearch.png" // index:012
        )
        for (effect in lookupTables) {
            val bitmap = BitmapFactory.decodeStream(context.assets.open("lookupfilter/$effect"))
            filterLookupTableBitmaps.add(bitmap)
        }
    }

    /**
     * @param width: preview width
     * @param height: preview height
     */
    fun onSizeChanged(width: Int, height: Int) {
        if (renderHeight == width && renderWith == height) {
            return
        }

        renderWith = height
        renderHeight = width
        if (frameBufferTexture[0] > 0) {
            release()
        }

        // create fbo
        GLES20.glGenFramebuffers(1, frameBuffer, 0)
        GLES20.glGenTextures(1, frameBufferTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, renderWith, renderHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0)
        val code = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (code != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "onSizeChanged: fbo err")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // calculate filter position
        val w = renderWith / FILTER_ITEM_COUNT
        val h = renderHeight / FILTER_ITEM_COUNT
        filterPositions[0] = w
        filterPositions[1] = h
        // view port left-bottom is (0, 0)
        val size = FILTER_ITEM_COUNT * FILTER_ITEM_COUNT
        for (i in 0 until size) {
            val x = w * (i % FILTER_ITEM_COUNT)
            val y = h * (i / FILTER_ITEM_COUNT)
            filterPositions[i * 2 + 2] = x
            filterPositions[i * 2 + 3] = y
        }
    }

    fun drawTexture(transformMatrix: FloatArray, oesTextureId: Int) {
        // convert oes to 2d
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glViewport(0, 0, renderWith, renderHeight)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        cameraShaderProgram.useProgram()
        cameraShaderProgram.setUniform(transformMatrix, oesTextureId)
        var positionLoc = cameraShaderProgram.getPositionAttributeLoc()
        var coordinateLoc = cameraShaderProgram.getTextureCoordinateAttributeLoc()
        vertexArray.setVertexAttributePointer(
                0, positionLoc, CameraRender.VERTEX_COMPONENT_COUNT, CameraRender.STRIDE
        )
        vertexArray.setVertexAttributePointer(
                2, coordinateLoc, CameraRender.COORDINATE_COMPONENT_COUNT, CameraRender.STRIDE
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        vertexArray.disableVertexAttributeArray(positionLoc)
        vertexArray.disableVertexAttributeArray(coordinateLoc)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // draw filter
        filterShaderProgram.useProgram()
        positionLoc = filterShaderProgram.getPositionAttributeLoc()
        coordinateLoc = filterShaderProgram.getTextureCoordinateAttributeLoc()
        vertexArray.setVertexAttributePointer(
                0, positionLoc, VERTEX_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttributePointer(
                2, coordinateLoc, COORDINATE_COMPONENT_COUNT, STRIDE
        )

        val itemW = filterPositions[0]
        val itemH = filterPositions[1]
        for (i in 0 until FILTER_ITEM_COUNT * FILTER_ITEM_COUNT) {
            GLES20.glViewport(filterPositions[i * 2 + 2], filterPositions[i * 2 + 3], itemW, itemH)
            filterShaderProgram.setUniform(
                    frameBufferTexture[0], filterLookupTableBitmaps[i], filterProgress)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        vertexArray.disableVertexAttributeArray(positionLoc)
        vertexArray.disableVertexAttributeArray(coordinateLoc)
        GLES20.glViewport(0, 0, renderWith, renderHeight)
    }

    fun release() {
        Log.d(TAG, "release: ")
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
        GLES20.glDeleteTextures(1, frameBufferTexture, 0)
        for (bitmap in filterLookupTableBitmaps) {
            bitmap.recycle()
        }
    }

    fun onProgressChanged(progress: Int) {
        filterProgress = progress / 100f
    }
}