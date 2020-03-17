package com.example.android.camera2basic.qrcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.android.camera2basic.R

class QrScannerView: View {

    companion object {
        private const val TAG = "QrScannerView"
        private const val MASK_COLOR = 0x50000000
        private const val SCANNER_DURATION = 2000
    }

    private var mRatioWidth = 0
    private var mRatioHeight = 0

    private var mStartTime = -1L
    private var mFrame = RectF()
    private var mFrameRect = Rect()
    private var mPaint = Paint()
    private var mScannerHeight = 0
    private var mFrameDrawable: Drawable? = null
    private var mScannerDrawable: Drawable? = null

    constructor(context: Context): super(context) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?):
            super(context, attributeSet) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int):
            super(context, attributeSet, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        mPaint.isAntiAlias = true
        mFrameDrawable = context.getDrawable(R.drawable.qrcode_scan_frame)
        mScannerDrawable = context.getDrawable(R.drawable.qrcode_scan_scaner)
    }

    fun isShowing(): Boolean {
        return visibility == VISIBLE && width > 0
    }

    fun getFrameRect(): Rect {
        return mFrameRect
    }

    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        mRatioWidth = width
        mRatioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        if (mRatioWidth == 0 || mRatioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                height = width * mRatioHeight / mRatioWidth
            } else {
                width = height * mRatioWidth / mRatioHeight
            }
            setMeasuredDimension(width, height)
        }

        val length = width * 0.6
        mFrame.left = (width / 2 - length / 2).toFloat()
        mFrame.right = (width / 2 + length / 2).toFloat()
        mFrame.top = (height / 2 - length / 2).toFloat()
        mFrame.bottom = (height / 2 + length / 2).toFloat()

        mFrameRect.set(mFrame.left.toInt(), mFrame.top.toInt(), mFrame.right.toInt(), mFrame.bottom.toInt())

        mFrameDrawable?.setBounds(
                mFrame.left.toInt() - 10,
                mFrame.top.toInt() - 10,
                mFrame.right.toInt() + 10,
                mFrame.bottom.toInt() + 10
        )
        mScannerHeight = mScannerDrawable!!.intrinsicHeight * mFrame.width().toInt() /
                mScannerDrawable!!.intrinsicWidth
        Log.d(TAG, "onMeasure: width = $width, height = $height, frame rect = ${mFrameRect.toShortString()}")
    }

    override fun onDraw(canvas: Canvas?) {
        // draw mask
        mPaint.color = MASK_COLOR
        mPaint.style = Paint.Style.FILL
        canvas?.drawRect(0f, 0f, width.toFloat(), mFrame.top, mPaint)
        canvas?.drawRect(0f, mFrame.top, mFrame.left, mFrame.bottom, mPaint)
        canvas?.drawRect(mFrame.right, mFrame.top, width.toFloat(), mFrame.bottom, mPaint)
        canvas?.drawRect(0f, mFrame.bottom, width.toFloat(), height.toFloat(), mPaint)

        // draw scanner
        val current = System.currentTimeMillis()
        if (mStartTime < 0) {
            mStartTime = current
        }
        val timeOffset = ((current - mStartTime) % SCANNER_DURATION).toInt()
        if (timeOffset >= 0 && timeOffset <= SCANNER_DURATION / 2) {
            val scannerShift = mFrame.height() * 2 * timeOffset / SCANNER_DURATION
            canvas?.save()
            canvas?.clipRect(mFrame)
            mScannerDrawable?.setBounds(
                    mFrame.left.toInt(),
                    (mFrame.top + scannerShift).toInt(),
                    mFrame.right.toInt(),
                    (mFrame.top + mScannerHeight + scannerShift).toInt()
            )
            mScannerDrawable?.draw(canvas!!)
            canvas?.restore()
        }

        // draw frame
        mFrameDrawable?.draw(canvas!!)
        invalidate()
    }
}