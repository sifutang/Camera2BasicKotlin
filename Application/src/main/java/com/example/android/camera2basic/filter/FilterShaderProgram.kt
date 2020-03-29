package com.example.android.camera2basic.filter

import android.content.Context
import android.opengl.GLES20
import com.example.android.camera2basic.R
import com.example.android.camera2basic.util.ShaderProgram
import com.example.android.camera2basic.util.TextureHelper

class FilterShaderProgram(context: Context) :
        ShaderProgram(context, R.raw.vertex_filter, R.raw.fragment_filter) {

    companion object {
        // Attribute constants.
        private const val POSITION_ATTRIBUTE = "a_Position"
        private const val TEXTURE_COORDINATE_ATTRIBUTE = "a_TextureCoordinate"

        // Uniform constants.
        private const val TEXTURE_SAMPLER_UNIFORM = "u_TextureSampler"
        private const val LOOKUP_TABLE = "u_LookupTable"
        private const val INTENSITY = "u_Intensity"
    }

    // Uniform locations
    private var uTextureSamplerLocation = -1
    private var uLookupTableLocation = -1
    private var uIntensityLocation = -1

    // Attribute locations
    private var aPositionLocation = -1
    private var aTextureCoordinateLocation = -1

    private var lookupTableId = -1

    init {
        // Retrieve attribute locations for the shader program.
        aPositionLocation = GLES20.glGetAttribLocation(programId, POSITION_ATTRIBUTE)
        aTextureCoordinateLocation = GLES20.glGetAttribLocation(programId, TEXTURE_COORDINATE_ATTRIBUTE)

        // Retrieve uniform locations for the shader program
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, TEXTURE_SAMPLER_UNIFORM)
        uLookupTableLocation = GLES20.glGetUniformLocation(programId, LOOKUP_TABLE)
        uIntensityLocation = GLES20.glGetUniformLocation(programId, INTENSITY)

        lookupTableId = TextureHelper.loadTexture(context, R.drawable.lookup)
    }

    fun setUniform(textureId: Int,  intensity: Float) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureSamplerLocation, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lookupTableId)
        GLES20.glUniform1i(uLookupTableLocation, 1)

        GLES20.glUniform1f(uIntensityLocation, intensity)
    }

    fun getPositionAttributeLoc(): Int {
        return aPositionLocation
    }

    fun getTextureCoordinateAttributeLoc(): Int {
        return aTextureCoordinateLocation
    }
}