package com.example.android.camera2basic.filter

import android.content.Context
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
    }

    private val vertexArray = VertexArray(vertexData)
    private var filterShaderProgram = FilterShaderProgram(context)
    private var cameraShaderProgram = CameraShaderProgram(context)
    private val frameBuffer = IntArray(1) {-1}
    private val frameBufferTexture = IntArray(1) {-1}
    private var renderWith = -1
    private var renderHeight = -1

    private var filterProgress = 1f

    /**
     * @param width: preview width
     * @param height: preview height
     */
    fun onSizeChanged(width: Int, height: Int) {
        renderWith = height
        renderHeight = width
        if (frameBufferTexture[0] > 0) {
            release()
        }

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
        filterShaderProgram.setUniform(frameBufferTexture[0], filterProgress)
        positionLoc = filterShaderProgram.getPositionAttributeLoc()
        coordinateLoc = filterShaderProgram.getTextureCoordinateAttributeLoc()
        vertexArray.setVertexAttributePointer(
                0, positionLoc, VERTEX_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttributePointer(
                2, coordinateLoc, COORDINATE_COMPONENT_COUNT, STRIDE
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        vertexArray.disableVertexAttributeArray(positionLoc)
        vertexArray.disableVertexAttributeArray(coordinateLoc)
    }

    fun release() {
        Log.d(TAG, "release: ")
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
        GLES20.glDeleteTextures(1, frameBufferTexture, 0)
    }

    fun onProgressChanged(progress: Int) {
        filterProgress = progress / 100f
    }
}