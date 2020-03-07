package com.example.android.camera2basic.particles

import android.content.Context
import android.opengl.Matrix
import com.example.android.camera2basic.R
import com.example.android.camera2basic.particles.objects.ParticleShooter
import com.example.android.camera2basic.particles.objects.ParticleSystem
import com.example.android.camera2basic.particles.program.ParticleShaderProgram
import com.example.android.camera2basic.util.Geometry
import com.example.android.camera2basic.util.MatrixHelper
import com.example.android.camera2basic.util.TextureHelper

class ParticlesRender(context: Context) {

    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    private var viewProjectionMatrix = FloatArray(16)

    private var particleProgram = ParticleShaderProgram(context)
    private var particleSystem = ParticleSystem(10000)
    private var particleShooter: ParticleShooter? = null
    private var globalStartTime = System.nanoTime()

    private var textureId = -1

    init {
        textureId = TextureHelper.loadTexture(context, R.drawable.particle_texture)
        val particleDirection = Geometry.Vector(0f, 0.5f, 0f)
        val angleVarianceInDegrees = 360f
        val speedVariance = 5f
        particleShooter = ParticleShooter(
                Geometry.Point(0f, 1f, 0f),
                particleDirection,
                angleVarianceInDegrees,
                speedVariance
        )
    }

    fun onSizeChanged(width: Int, height: Int) {
        MatrixHelper.perspectiveM(projectionMatrix,
                45f, 1.0f * width / height, 1f, 10f)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(viewMatrix, 0, 0f, -1.5f, -5f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    fun drawSelf() {
        val currentTime = (System.nanoTime() - globalStartTime) / 1000000000f
        particleShooter?.addParticles(particleSystem, currentTime, 5)
        particleProgram.useProgram()
        particleProgram.setUniforms(viewProjectionMatrix, currentTime, textureId)
        particleSystem.bindData(particleProgram)
        particleSystem.draw()
    }
}