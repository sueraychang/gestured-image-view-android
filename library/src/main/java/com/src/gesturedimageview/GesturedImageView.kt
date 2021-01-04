package com.src.gesturedimageview

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View.OnTouchListener
import androidx.appcompat.widget.AppCompatImageView

class GesturedImageView : AppCompatImageView {

    interface PanToEndListener {

        fun onPanToHorizontalEnd(toEnd: Boolean)

        fun onPanToVerticalEnd(toEnd: Boolean)
    }

    private var listener: PanToEndListener? = null

    constructor(context: Context) : super(context) {
        initialView(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initialView(context)
        initialAttrs(attributeSet)
    }

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var lastX = 0f
    private var lastY = 0f

    private var savedScale = 1f

    private val matrixValues = FloatArray(9)
    private var displayMatrix = Matrix()

    private var activePointerId = -1

    private var isPanToHorizontalEnd: Boolean = false
    private var isPanToVerticalEnd: Boolean = false

    private val scaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
//                Log.d(TAG, "onScaleBegin")
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                Log.d(TAG, "onScale")
                val prevScale = savedScale
                var scaleFactor = detector.scaleFactor
                savedScale *= detector.scaleFactor
                if (savedScale > MAX_SCALE) {
                    savedScale = MAX_SCALE
                    scaleFactor = MAX_SCALE / prevScale
                }
                if (savedScale < MIN_SCALE) {
                    savedScale = MIN_SCALE
                    scaleFactor = MIN_SCALE / prevScale
                }
                displayMatrix.postScale(
                    scaleFactor,
                    scaleFactor,
                    detector.focusX,
                    detector.focusY
                )
                imageMatrix = displayMatrix
                fixTranslation()
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
//                Log.d(TAG, "onScaleEnd")
            }
        }
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap")
            fitToScreen()
            return true
        }
    }
    private val touchListener = OnTouchListener { _, event ->
        Log.d(TAG, "onTouch $event")

        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                event.actionIndex.let {
                    lastX = event.getX(it)
                    lastY = event.getY(it)
                }
//                state = State.DRAG
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val (x: Float, y: Float) = event.findPointerIndex(activePointerId).let {
                    event.getX(it) to event.getY(it)
                }
                val dx = x - lastX
                val dy = y - lastY
                displayMatrix.postTranslate(dx, dy)
                imageMatrix = displayMatrix
                fixTranslation()

                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                event.actionIndex.also { pointerIndex ->
                    event.getPointerId(pointerIndex)
                        .takeIf { it == activePointerId }
                        ?.run {
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            lastX = event.getX(newPointerIndex)
                            lastY = event.getY(newPointerIndex)
                            activePointerId = event.getPointerId(newPointerIndex)
                        }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
            }
        }

        true
    }

    fun setOnPanToEndListener(listener: PanToEndListener) {
        this.listener = listener
    }

    private fun initialView(context: Context) {
        Log.d(TAG, "initialView")

        scaleType = ScaleType.MATRIX

        imageMatrix = displayMatrix

        scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)
        gestureDetector = GestureDetector(context, gestureListener)
        setOnTouchListener(touchListener)

    }

    private fun initialAttrs(attributeSet: AttributeSet) {

    }

    private fun fitToScreen() {
        val scaleFactor = 1 / savedScale
        displayMatrix.postScale(scaleFactor, scaleFactor)
        imageMatrix = displayMatrix

        savedScale = 1f
    }

    private fun fixTranslation() {
        imageMatrix.getValues(matrixValues)
        matrixValues.apply {
            val transX = this[Matrix.MTRANS_X]
            val transY = this[Matrix.MTRANS_Y]

            Log.d(TAG, "transX $transX transY $transY")

            val fixTransX = getFixTranslation(transX, width.toFloat(), width * savedScale)
            val fixTransY = getFixTranslation(transY, height.toFloat(), height * savedScale)
            Log.d(TAG, "fixTransX $fixTransX fixTransY $fixTransY")

            if (fixTransX != 0f || fixTransY != 0f) {
                displayMatrix.postTranslate(fixTransX, fixTransY)
                imageMatrix = displayMatrix
            }

            if (fixTransX != 0f && !isPanToHorizontalEnd) {
                isPanToHorizontalEnd = true
                listener?.onPanToHorizontalEnd(true)
            } else if (fixTransX == 0f && isPanToHorizontalEnd) {
                isPanToHorizontalEnd = false
                listener?.onPanToHorizontalEnd(false)
            }

            if (fixTransY != 0f && !isPanToVerticalEnd) {
                isPanToVerticalEnd = true
                listener?.onPanToVerticalEnd(true)
            } else if (fixTransY == 0f && isPanToVerticalEnd) {
                listener?.onPanToVerticalEnd(false)
            }
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        Log.d(TAG, "minTrans $minTrans maxTrans $maxTrans")
        if (trans < minTrans) {
            return minTrans - trans
        }
        if (trans > maxTrans) {
            return maxTrans - trans
        }
        return 0f
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        Log.d(TAG, "onMeasure width $width height $height")
//    }

    companion object {

        private const val TAG = "[IG]GesturalImageView"

        private const val MAX_SCALE = 4f
        private const val MIN_SCALE = 1f
    }
}