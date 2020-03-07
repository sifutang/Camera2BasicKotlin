package com.example.android.camera2basic.particles.program

import android.content.Context
import android.opengl.GLES20
import com.example.android.camera2basic.R
import com.example.android.camera2basic.util.ShaderProgram

class ParticleShaderProgram(context: Context) :
        ShaderProgram(context, R.raw.particle_vertex_shader, R.raw.particle_fragment_shader) {

    companion object {

        // Uniform constants.
        const val U_TIME = "u_Time"
        const val U_MATRIX = "u_Matrix"
        const val U_TEXTURE_UNIT = "u_TextureUnit"

        // Attribute constants.
        const val A_POSITION = "a_Position"
        const val A_COLOR = "a_Color"
        const val A_DIRECTION_VECTOR = "a_DirectionVector"
        const val A_PARTICLE_START_TIME = "a_ParticleStartTime"
    }

    // Uniform locations
    private var uMatrixLocation = -1
    private var uTimeLocation = -1
    private var uTextureUnitLocation = -1

    // Attribute locations
    private var aPositionLocation = -1
    private var aColorLocation = -1
    private var aDirectionVectorLocation = -1
    private var aParticleStartTimeLocation = -1

    init {
        // Retrieve uniform locations for the shader program
        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX)
        uTimeLocation = GLES20.glGetUniformLocation(programId, U_TIME)
        uTextureUnitLocation = GLES20.glGetUniformLocation(programId, U_TEXTURE_UNIT)

        // Retrieve attribute locations for the shader program.
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION)
        aColorLocation = GLES20.glGetAttribLocation(programId, A_COLOR)
        aDirectionVectorLocation = GLES20.glGetAttribLocation(programId, A_DIRECTION_VECTOR)
        aParticleStartTimeLocation = GLES20.glGetAttribLocation(programId, A_PARTICLE_START_TIME)
    }

    fun setUniforms(matrix: FloatArray, elapsedTime: Float, textureId: Int) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        GLES20.glUniform1f(uTimeLocation, elapsedTime)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureUnitLocation, 0)
    }

    fun getPositionAttributeLocation(): Int {
        return aPositionLocation
    }

    fun getColorAttributeLocation(): Int {
        return aColorLocation
    }

    fun getDirectionAttributeLocation(): Int {
        return aDirectionVectorLocation
    }

    fun getParticleStartTimeAttributeLocation(): Int {
        return aParticleStartTimeLocation
    }
}