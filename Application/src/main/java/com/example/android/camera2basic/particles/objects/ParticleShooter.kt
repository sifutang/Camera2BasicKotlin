package com.example.android.camera2basic.particles.objects

import android.graphics.Color
import android.opengl.Matrix
import com.example.android.camera2basic.util.Geometry
import java.util.*

class ParticleShooter(private var position: Geometry.Point,
                      private var direction: Geometry.Vector,
                      private var angleVarianceInDegrees: Float,
                      private var speedVariance: Float) {


    private var random = Random()
    private var rotationMatrix = FloatArray(16)
    private var directionVector = FloatArray(4)
    private var resultVector = FloatArray(4)

    init {
        directionVector[0] = direction.x
        directionVector[1] = direction.y
        directionVector[2] = direction.z
    }

    fun addParticles(particleSystem: ParticleSystem, currentTime: Float, count: Int) {
        for (i in 0 until count) {
            Matrix.setRotateEulerM(rotationMatrix, 0,
                    (random.nextFloat() - 0.5f) * angleVarianceInDegrees,
                    (random.nextFloat() - 0.5f) * angleVarianceInDegrees,
                    (random.nextFloat() - 0.5f) * angleVarianceInDegrees)

            Matrix.multiplyMV(resultVector,
                    0, rotationMatrix, 0, directionVector, 0)
            val speedAdjustment: Float = 1f + random.nextFloat() * speedVariance
            val thisDirection: Geometry.Vector = Geometry.Vector(
                    resultVector[0] * speedAdjustment,
                    resultVector[1] * speedAdjustment,
                    resultVector[2] * speedAdjustment
            )
            particleSystem.addParticle(
                    position,
                    Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
                    thisDirection,
                    currentTime
            )
        }
    }
}