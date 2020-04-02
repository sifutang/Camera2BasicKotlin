package com.example.android.camera2basic.watermark

import android.content.Context
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.example.android.camera2basic.R
import com.example.android.camera2basic.particles.Constants
import com.example.android.camera2basic.particles.data.VertexArray
import com.example.android.camera2basic.util.CommonUtil
import com.example.android.camera2basic.util.TextureHelper

class WaterMarkRender(context: Context) {

    companion object {
        private const val TAG = "WaterMarkRender"

        private const val VERTEX_COMPONENT_COUNT = 2
        private const val COORDINATE_COMPONENT_COUNT = 2
        private const val STRIDE =
                (VERTEX_COMPONENT_COUNT + COORDINATE_COMPONENT_COUNT) * Constants.BYTES_PRE_FLOAT
    }

    private var vertexData = floatArrayOf(
            // x, y, s, t
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
            1f,  1f, 1f, 0f
    )

    private val vertexArray = VertexArray(vertexData)
    private val waterShaderProgram = WaterMarkShaderProgram(context)
    private val textureId:Int?

    private var mvpMatrix = Array(16) { 0.0f }.toFloatArray()
    private var modelMatrix = Array(16) { 0.0f }.toFloatArray()
    private var viewMatrix = Array(16) { 0.0f }.toFloatArray()
    private var projectionMatrix = Array(16) { 0.0f }.toFloatArray()
    private val tmpMatrix = Array(16) {0.0f}.toFloatArray()

    private var renderWith = -1
    private var renderHeight = -1
    private var renderRect = RectF(50f, 50f, 200f, 200f)
    init {
        textureId = TextureHelper.loadTexture(context, R.drawable.watermark_logo)
    }

    /**
     * @param width: preview width
     * @param height: preview height
     */
    fun onSizeChanged(width: Int, height: Int) {
        renderWith = height
        renderHeight = width

        Log.d(TAG, "renderSize: renderWith = $renderWith, renderHeight = $renderHeight")

//        renderRect.set(0f, 0f, renderWith.toFloat(), renderHeight.toFloat())
//
//        Matrix.setLookAtM(viewMatrix, 0,
//            0F, 0F, 7F,
//            0F, 0F, 0F,
//            0F, 1F, 0F)
//
//        val ratio = 1F * renderWith / renderHeight
//        Matrix.frustumM(projectionMatrix, 0,
//            -ratio, ratio, -1F, 1F, 3F, 7F)
//
//        val scaleX = renderRect.width() / renderWith
//        val scaleY = renderRect.height() / renderHeight
//        Matrix.setIdentityM(modelMatrix, 0)
//        Log.d(TAG, "onSizeChanged: val = ${-0.5f / scaleX}, scaleX = $scaleX")
//        Matrix.translateM(modelMatrix, 0, -0.5f, 0.5f * renderHeight / renderWith, 1f)
////        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
//        Matrix.scaleM(modelMatrix, 0, scaleX * renderWith / renderHeight, scaleY, 1f)
//
//        Matrix.multiplyMM(tmpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tmpMatrix, 0)
//
//        Matrix.multiplyMM(tmpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tmpMatrix, 0)

        Matrix.setIdentityM(mvpMatrix, 0)
        val xStep = 2f / renderWith
        val yStep = 2f / renderHeight
        val top = yStep * renderRect.top
        val left = xStep * renderRect.left
        val h = CommonUtil.clamp(yStep * renderRect.height(), 0f, 1f)
        val w = CommonUtil.clamp(xStep * renderRect.width(), 0f, 1f)
        vertexData = floatArrayOf(
                // x, y, s, t
                -1f + left, 1f - top - h, 0f, 1f,
                -1f + left + w, 1f - top - h, 1f, 1f,
                -1f + left,  1f - top, 0f, 0f,
                -1f + left + w, 1f - top, 1f, 0f
        )
        vertexArray.updateBuffer(vertexData, 0, vertexData.size)
    }

    fun drawSelf() {
        waterShaderProgram.useProgram()

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
    }
}