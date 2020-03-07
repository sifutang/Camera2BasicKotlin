package com.example.android.camera2basic.particles.objects

import android.graphics.Color
import android.opengl.GLES20
import android.util.Log
import com.example.android.camera2basic.particles.Constants
import com.example.android.camera2basic.particles.program.ParticleShaderProgram
import com.example.android.camera2basic.particles.data.VertexArray
import com.example.android.camera2basic.util.Geometry

class ParticleSystem(maxParticleCount: Int) {

    companion object {
        private const val TAG = "ParticleSystem"
        private const val POSITION_COMPONENT_COUNT = 3
        private const val COLOR_COMPONENT_COUNT = 3
        private const val VECTOR_COMPONENT_COUNT = 3
        private const val PARTICLE_START_TIME_COMPONENT_COUNT = 1

        private const val TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT +
                COLOR_COMPONENT_COUNT +
                VECTOR_COMPONENT_COUNT +
                PARTICLE_START_TIME_COMPONENT_COUNT
        private const val STRIDE = TOTAL_COMPONENT_COUNT * Constants.BYTES_PRE_FLOAT
    }

    private var particles: FloatArray = FloatArray(maxParticleCount * TOTAL_COMPONENT_COUNT)
    private var vertexArray: VertexArray? = null
    private var maxParticleCount = -1
    private var currentParticleCount = -1
    private var nextParticle = 0

    init {
        this.maxParticleCount = maxParticleCount
        vertexArray = VertexArray(particles)
    }

    fun addParticle(position: Geometry.Point,
                    color: Int,
                    direction: Geometry.Vector,
                    particleStartTime: Float) {
        val particleOffset = nextParticle * TOTAL_COMPONENT_COUNT
        var currentOffset = particleOffset
        nextParticle++

        if (currentParticleCount < maxParticleCount) {
            currentParticleCount++
        }

        if (nextParticle == maxParticleCount) {
            // Start over at the beginning, but keep currentParticleCount
            // so that all the other particles still get drawn.
            nextParticle = 0
        }

        particles[currentOffset++] = position.x
        particles[currentOffset++] = position.y
        particles[currentOffset++] = position.z

        particles[currentOffset++] = Color.red(color) / 255f
        particles[currentOffset++] = Color.green(color) / 255f
        particles[currentOffset++] = Color.blue(color) / 255f

        particles[currentOffset++] = direction.x
        particles[currentOffset++] = direction.y
        particles[currentOffset++] = direction.z

        particles[currentOffset++] = particleStartTime
        Log.d(TAG, "addParticle: currentOffset = $currentOffset")

        vertexArray?.updateBuffer(particles, particleOffset, TOTAL_COMPONENT_COUNT)
    }

    fun bindData(particleProgram: ParticleShaderProgram) {
        var dataOffset = 0
        vertexArray?.setVertexAttributePointer(
                dataOffset,
                particleProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        )
        dataOffset += POSITION_COMPONENT_COUNT

        vertexArray?.setVertexAttributePointer(
                dataOffset,
                particleProgram.getColorAttributeLocation(),
                COLOR_COMPONENT_COUNT,
                STRIDE
        )
        dataOffset += COLOR_COMPONENT_COUNT

        vertexArray?.setVertexAttributePointer(
                dataOffset,
                particleProgram.getDirectionAttributeLocation(),
                VECTOR_COMPONENT_COUNT,
                STRIDE
        )
        dataOffset += VECTOR_COMPONENT_COUNT

        vertexArray?.setVertexAttributePointer(
                dataOffset,
                particleProgram.getParticleStartTimeAttributeLocation(),
                PARTICLE_START_TIME_COMPONENT_COUNT,
                STRIDE
        )
    }

    fun draw() {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, currentParticleCount)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}