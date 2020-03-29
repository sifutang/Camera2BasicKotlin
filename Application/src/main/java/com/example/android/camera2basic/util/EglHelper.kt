package com.example.android.camera2basic.util

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import javax.microedition.khronos.egl.*

class EglHelper(private var outputSurfaceTexture: SurfaceTexture) {

    companion object {
        private const val TAG = "MyGlSurfaceProvider"
        private const val M_INIT = 0
        private const val M_DRAW = 1
        private const val M_SIZE_CHANGE = 2
        private const val M_UN_INIT = 3
    }

    private var mGlThread: HandlerThread = HandlerThread("gl_thread")
    private var mGlHandler: Handler

    private var mEgl: EGL10? = null
    private var mEglDisplay = EGL10.EGL_NO_DISPLAY
    private var mEglContext = EGL10.EGL_NO_CONTEXT
    private var mEglConfig = arrayOfNulls<EGLConfig>(1)
    private var mEglSurface: EGLSurface? = null

    private var render: Render? = null
    private var width = -1
    private var height = -1

    interface Render {
        /**
         * Called when the surface is created or recreated.
         */
        fun onSurfaceCreated(egl: EGL10?)

        /**
         * Called when the surface changed size.
         */
        fun onSurfaceChanged(egl: EGL10?, width: Int, height: Int)

        /**
         * Called to draw the current frame.
         */
        fun onDrawFrame(egl: EGL10?)

        /**
         * Called when egl context release.
         */
        fun onSurfaceDestroy()
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
                    M_SIZE_CHANGE -> {
                        render?.onSurfaceChanged(mEgl, msg.arg1, msg.arg2)
                    }
                    M_UN_INIT -> {
                        unInitEGL()
                    }
                }
            }
        }
        mGlHandler.sendEmptyMessage(M_INIT)
    }

    fun setRender(render: Render) {
        this.render = render
    }

    fun setSize(width: Int, height: Int) {
        if (this.width != width || this.height != height) {
            mGlHandler.sendMessage(mGlHandler.obtainMessage(M_SIZE_CHANGE, width, height))
        }

        this.width = width
        this.height = height
    }

    fun requestDraw() {
        mGlHandler.sendMessage(Message.obtain(mGlHandler, M_DRAW))
    }

    fun release() {
        Log.d(TAG, "release: ")
        mGlHandler.sendEmptyMessage(M_UN_INIT)
        mGlThread.quitSafely()
    }

    private fun unInitEGL() {
        render?.onSurfaceDestroy()
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

        mEglSurface = mEgl!!.eglCreateWindowSurface(mEglDisplay, mEglConfig[0], outputSurfaceTexture, null)

        val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
        mEglContext = mEgl!!.eglCreateContext(mEglDisplay, mEglConfig[0], EGL10.EGL_NO_CONTEXT, contextAttributes)

        if (mEglDisplay === EGL10.EGL_NO_DISPLAY || mEglContext === EGL10.EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext fail failed! " + mEgl!!.eglGetError())
        }

        if (!mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw RuntimeException("eglMakeCurrent failed! " + mEgl!!.eglGetError())
        }
        Log.d(TAG, "init egl context")

        render?.onSurfaceCreated(mEgl)
    }

    private fun drawFrame() {
        mEgl!!.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
        render?.onDrawFrame(mEgl)
        mEgl!!.eglSwapBuffers(mEglDisplay, mEglSurface)
    }
}