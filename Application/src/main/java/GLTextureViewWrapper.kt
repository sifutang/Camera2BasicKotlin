import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import com.example.android.camera2basic.filter.FilterRender
import com.example.android.camera2basic.particles.ParticlesRender
import com.example.android.camera2basic.util.EglHelper
import com.example.android.camera2basic.util.OpenGlUtils
import com.example.android.camera2basic.watermark.WaterMarkRender
import javax.microedition.khronos.egl.EGL10

class GLTextureViewWrapper(
        private var context: Context,
        outputSurfaceTexture: SurfaceTexture
): EglHelper.Render, SurfaceTexture.OnFrameAvailableListener {

    companion object {
        private const val TAG = "GLTextureViewWrapper"
    }

    private var eglHelper = EglHelper(outputSurfaceTexture)
    private var mInputSurfaceTexture: SurfaceTexture? = null

    private var mFilterRender: FilterRender? = null
    private var mParticleRender: ParticlesRender? = null
    private var mWaterMarkRender: WaterMarkRender? = null

    private var mOesTextureId = -1
    private var transformMatrix = FloatArray(16)
    private val mLock = Object()

    private var mDrawParticles = false

    init {
        eglHelper.setRender(this)
    }

    fun release() {
        eglHelper.release()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        eglHelper.requestDraw()
    }

    override fun onSurfaceCreated(egl: EGL10?) {
        Log.d(TAG, "onSurfaceCreated")
        mOesTextureId = OpenGlUtils.createOESTextureObject()

        mFilterRender = FilterRender(context)
        mParticleRender = ParticlesRender(context)
        mWaterMarkRender = WaterMarkRender(context)
    }

    override fun onSurfaceChanged(egl: EGL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: width = $width, height = $height")
        if (mInputSurfaceTexture == null) {
            mInputSurfaceTexture = SurfaceTexture(mOesTextureId)
            mInputSurfaceTexture!!.setOnFrameAvailableListener(this)
        }
        mInputSurfaceTexture!!.setDefaultBufferSize(width, height)
        synchronized(mLock) {
            mLock.notify()
        }

        GLES20.glViewport(0, 0, height, width)
        mParticleRender?.onSizeChanged(width, height)
        mWaterMarkRender?.onSizeChanged(width, height)
        mFilterRender?.onSizeChanged(width, height)
    }

    override fun onDrawFrame(egl: EGL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        mInputSurfaceTexture!!.updateTexImage()
        mInputSurfaceTexture!!.getTransformMatrix(transformMatrix)
        mFilterRender?.drawTexture(transformMatrix, mOesTextureId)
        mWaterMarkRender?.drawSelf()
        if (mDrawParticles) {
            mParticleRender?.drawSelf()
        }
    }

    override fun onSurfaceDestroy() {
        Log.d(TAG, "onSurfaceDestroy")
        mInputSurfaceTexture?.release()
        mInputSurfaceTexture = null
        OpenGlUtils.deleteTexture(mOesTextureId)
        mFilterRender?.release()
    }

    fun setPreviewSize(previewSize: Size) {
        Log.d(TAG, "setPreviewSize: $previewSize")
        eglHelper.setSize(previewSize.width, previewSize.height)
    }

    fun getInputSurfaceTexture(): SurfaceTexture {
        if (mInputSurfaceTexture == null) {
            synchronized(mLock) {
                mLock.wait()
            }
        }

        return mInputSurfaceTexture!!
    }

    fun drawParticle(draw: Boolean) {
        mDrawParticles = draw
    }

    fun particlesShowing(): Boolean {
        return mDrawParticles
    }

    fun onProgressChanged(progress: Int) {
        mFilterRender?.onProgressChanged(progress)
    }
}