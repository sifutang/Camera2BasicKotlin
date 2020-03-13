package com.example.android.camera2basic.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import com.example.android.camera2basic.camera.CameraRender
import com.example.android.camera2basic.particles.ParticlesRender
import javax.microedition.khronos.egl.*

class GlTextureViewWrapper(
        context: Context,
        private var textureView: TextureView
): SurfaceTexture.OnFrameAvailableListener {

    companion object {
        private const val TAG = "MyGlSurfaceProvider"
        private const val M_INIT = 0
        private const val M_DRAW = 1
        private const val M_UN_INIT = 2
    }

    private var mGlThread: HandlerThread = HandlerThread("gl_thread")
    private var mGlHandler: Handler

    private var mInputSurfaceTexture: SurfaceTexture? = null
    private var mCameraRender: CameraRender? = null
    private var mParticleRender: ParticlesRender? = null

    private var mEgl: EGL10? = null
    private var mEglDisplay = EGL10.EGL_NO_DISPLAY
    private var mEglContext = EGL10.EGL_NO_CONTEXT
    private var mEglConfig = arrayOfNulls<EGLConfig>(1)
    private var mEglSurface: EGLSurface? = null
    private var mContext = context

    private var mOesTextureId = -1
    private var mTransformMatrix = FloatArray(16)
    private val mLock = Object()

    private var mDrawParticles = false

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        mGlHandler.sendMessage(Message.obtain(mGlHandler, M_DRAW))
    }

    init {
        mGlThread.start()
        mGlHandler = object : Handler(mGlThread.looper) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    M_INIT -> {
                        initEGL()
                    }
                    M_DRAW -> {
                        drawFrame()
                    }
                    M_UN_INIT -> {
                        unInitEGL()
                    }
                }
            }
        }
        mGlHandler.sendEmptyMessage(M_INIT)
    }

    fun setSize(width: Int, height: Int) {
        val surfaceTexture = getInputSurfaceTexture()
        surfaceTexture.setDefaultBufferSize(width, height)
        surfaceTexture.setOnFrameAvailableListener(this)
        GLES20.glViewport(0, 0, height, width)
        mParticleRender?.onSizeChanged(width, height)
    }

    fun getInputSurfaceTexture(): SurfaceTexture {
        synchronized(mLock) {
            while (mOesTextureId == -1) {
                mLock.wait()
            }
        }

        if (mInputSurfaceTexture == null) {
            mInputSurfaceTexture = SurfaceTexture(mOesTextureId)
        }

        return mInputSurfaceTexture!!
    }

    fun release() {
        Log.d(TAG, "release: ")
        mGlHandler.sendEmptyMessage(M_UN_INIT)
        mGlThread.quitSafely()
        mInputSurfaceTexture?.release()
        mInputSurfaceTexture = null
    }

    fun drawParticle(draw: Boolean) {
        mDrawParticles = draw
    }

    fun particlesShowing(): Boolean {
        return mDrawParticles
    }

    private fun unInitEGL() {
        OpenGlUtils.deleteTexture(mOesTextureId)
        if (mEglDisplay != EGL10.EGL_NO_DISPLAY) {
            mEgl?.eglDestroySurface(mEglDisplay, mEglSurface)
            mEgl?.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            mEgl?.eglDestroyContext(mEglDisplay, mEglContext)
            mEgl?.eglTerminate(mEglDisplay)
        }
    }

    private fun initEGL() {
        mEgl = EGLContext.getEGL() as EGL10

        mEglDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed! " + mEgl!!.eglGetError())
        }

        val version = IntArray(2)
        if (!mEgl!!.eglInitialize(mEglDisplay, version)) {
            throw RuntimeException("eglInitialize failed! " + mEgl!!.eglGetError())
        }

        val attributes = intArrayOf(EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_BUFFER_SIZE, 32,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_SURFACE_TYPE,
                EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE)
        val configsNum = IntArray(1)

        if (!mEgl!!.eglChooseConfig(mEglDisplay, attributes, mEglConfig, 1, configsNum)) {
            throw RuntimeException("eglChooseConfig failed! " + mEgl!!.eglGetError())
        }

        if (textureView.surfaceTexture == null) {
            throw RuntimeException("not output surface texture")
        }

        mEglSurface = mEgl!!.eglCreateWindowSurface(mEglDisplay, mEglConfig[0], textureView.surfaceTexture, null)

        val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
        mEglContext = mEgl!!.eglCreateContext(mEglDisplay, mEglConfig[0], EGL10.EGL_NO_CONTEXT, contextAttributes)

        if (mEglDisplay === EGL10.EGL_NO_DISPLAY || mEglContext === EGL10.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext fail failed! " + mEgl!!.eglGetError())
        }

        if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw RuntimeException("eglMakeCurrent failed! " + mEgl!!.eglGetError())
        }

        mOesTextureId = OpenGlUtils.createOESTextureObject()
        mCameraRender = CameraRender(mContext)
        mParticleRender = ParticlesRender(mContext)
        synchronized(mLock) {
            mLock.notify()
        }
        Log.d(TAG, "init egl context")
    }

    private fun drawFrame() {
        if (mInputSurfaceTexture == null) {
            throw RuntimeException("input surface-texture not set, pls call getInputSurfaceTexture()")
        }

        mInputSurfaceTexture!!.updateTexImage()
        mInputSurfaceTexture!!.getTransformMatrix(mTransformMatrix)

        mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        mCameraRender!!.drawTexture(mTransformMatrix, mOesTextureId)
        if (mDrawParticles) {
            mParticleRender?.drawSelf()
        }
        mEgl!!.eglSwapBuffers(mEglDisplay, mEglSurface)
    }
}