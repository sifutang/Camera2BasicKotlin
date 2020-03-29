package com.example.android.camera2basic.watermark

import android.content.Context
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.example.android.camera2basic.R
import com.example.android.camera2basic.particles.Constants
import com.example.android.camera2basic.particles.data.VertexArray
import com.example.android.camera2basic.util.TextureHelper

class WaterMarkRender(context: Context) {

    companion object {
        private const val TAG = "WaterMarkRender"

        private val WATER_MARK_DATA = floatArrayOf(
                // x, y, s, t
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f,
                -1f,  1f, 0f, 0f,
                1f,  1f, 1f, 0f
        )
        private const val VERTEX_COMPONENT_COUNT = 2
        private const val COORDINATE_COMPONENT_COUNT = 2
        private const val STRIDE =
                (VERTEX_COMPONENT_COUNT + COORDINATE_COMPONENT_COUNT) * Constants.BYTES_PRE_FLOAT
    }

    private val vertexArray = VertexArray(WATER_MARK_DATA)
    private val waterShaderProgram = WaterMarkShaderProgram(context)
    private val textureId:Int?

    private var mvpMatrix = Array(16) { 0.0f }.toFloatArray()
    private var modelMatrix = Array(16) { 0.0f }.toFloatArray()
    private var viewMatrix = Array(16) { 0.0f }.toFloatArray()
    private var projectionMatrix = Array(16) { 0.0f }.toFloatArray()

    private var renderWith = -1
    private var renderHeight = -1
    private var renderRect = Rect(50, 50, 200, 200)
    init {
        textureId = TextureHelper.loadTexture(context, R.drawable.watermark_logo)
    }

    /**
     * @param width: preview width
     * @param height: preview height
     */
    fun onSizeChanged(width: Int, height: Int) {
        Log.d(TAG, "renderSize: width = $width, height = $height")
        renderWith = height
        renderHeight = width
//        Matrix.setLookAtM(viewMatrix, 0,
//            0F, 0F, 7F,
//            0F, 0F, 0F,
//            0F, 1F, 0F)
//
//        val ratio = 1F * width / height
//        Matrix.frustumM(projectionMatrix, 0,
//            -ratio, ratio, -1F, 1F, 3F, 7F)
//
//        Matrix.setIdentityM(modelMatrix, 0)
//        Matrix.translateM(modelMatrix, 0, -0.8f, 0.5f, 0f)
//        Matrix.scaleM(modelMatrix, 0, 1 / 5f, 1 / 5f, 1.0f)
//
//        val tmpMatrix = Array(16) {0.0f}.toFloatArray()
//        Matrix.multiplyMM(tmpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(modelMatrix, 0, projectionMatrix, 0, tmpMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    fun drawSelf() {
        waterShaderProgram.useProgram()
        GLES20.glViewport(renderRect.left,
                renderHeight - renderRect.top - renderRect.height(),
                renderRect.width(),
                renderRect.height()
        )

        // blend
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        waterShaderProgram.setUniform(mvpMatrix, textureId!!)
        val positionLoc = waterShaderProgram.getPositionAttributeLoc()
        val coordinateLoc = waterShaderProgram.getTextureCoordinateAttributeLoc()
        vertexArray.setVertexAttributePointer(
                0, positionLoc, VERTEX_COMPONENT_COUNT, STRIDE
        )
        vertexArray.setVertexAttributePointer(
                2, coordinateLoc, COORDINATE_COMPONENT_COUNT, STRIDE
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionLoc)
        GLES20.glDisableVertexAttribArray(coordinateLoc)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glViewport(0, 0, renderWith, renderHeight)
    }
}