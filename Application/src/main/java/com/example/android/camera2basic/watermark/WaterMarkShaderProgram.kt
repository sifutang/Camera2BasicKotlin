package com.example.android.camera2basic.watermark

import android.content.Context
import android.opengl.GLES20
import com.example.android.camera2basic.R
import com.example.android.camera2basic.util.ShaderProgram

class WaterMarkShaderProgram(context: Context) :
        ShaderProgram(context, R.raw.vertex_water_mark, R.raw.fragment_water_mark) {

    companion object {
        private const val POSITION_ATTRIBUTE = "aPosition"
        private const val COORDINATE = "aWaterMarkTextureCoordinate"
        private const val SAMPLER = "sWaterMarkSampler"
        private const val MVP_MATRIX = "uMVPMatrix"
    }

    // Attribute locations
    private var aPositionLocation = -1
    private var aTextureCoordinateLocation = -1

    // Uniform locations
    private var uTextureSamplerLocation = -1
    private var uMvpMatrixLocation = -1

    init {
        aPositionLocation = GLES20.glGetAttribLocation(programId, POSITION_ATTRIBUTE)
        aTextureCoordinateLocation = GLES20.glGetAttribLocation(programId, COORDINATE)
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, SAMPLER)
        uMvpMatrixLocation = GLES20.glGetUniformLocation(programId, MVP_MATRIX)
    }

    fun setUniform(matrix: FloatArray, textureId: Int) {
        // texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureSamplerLocation, 0)

        // mvp matrix
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, matrix, 0)
    }

    fun getPositionAttributeLoc(): Int {
        return aPositionLocation
    }

    fun getTextureCoordinateAttributeLoc(): Int {
        return aTextureCoordinateLocation
    }
}