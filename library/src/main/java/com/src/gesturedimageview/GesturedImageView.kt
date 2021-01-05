package com.src.gesturedimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View.OnTouchListener
import androidx.appcompat.widget.AppCompatImageView
import java.lang.Float.max
import java.lang.Float.min
import java.lang.Integer.min

class GesturedImageView : AppCompatImageView {

    interface ScrollToEndListener {

        fun onScrollToHorizontalEnd(direction: Int)

        fun onScrollToVerticalEnd(direction: Int)
    }

    private var listener: ScrollToEndListener? = null

    constructor(context: Context) : super(context) {
        initialView(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        initialView(context)
        initialAttrs(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initialView(context)
        initialAttrs(attributeSet)
        initialStyle(defStyleAttr)
    }

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    private var viewWidth = 0
    private var viewHeight = 0

    private var gesturedScaleType = ScaleType.FIT_CENTER

    private var lastTouch = PointF(0f, 0f)

    private var currentScale = 1f

    private val matrixValues = FloatArray(9)
    private var displayMatrix = Matrix()

    private var activePointerId = -1

    private var isScrolledToHorizontalEnd: Int = 0
    private var isScrolledToVerticalEnd: Int = 0

    private val scaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleImage(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
            }
        }
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap")
//            fitToScreen()
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
                    lastTouch.set(event.getX(it), event.getY(it))
                }
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val (x: Float, y: Float) = event.findPointerIndex(activePointerId).let {
                    event.getX(it) to event.getY(it)
                }
                translateImage(x - lastTouch.x, y - lastTouch.y)
                lastTouch.set(x, y)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                event.actionIndex.also { pointerIndex ->
                    event.getPointerId(pointerIndex)
                        .takeIf { it == activePointerId }
                        ?.run {
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            lastTouch.set(event.getX(newPointerIndex), event.getY(newPointerIndex))
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

    var maxScale: Float = 1f
    var minScale: Float = 1f

    fun setScrollToEndListener(listener: ScrollToEndListener) {
        this.listener = listener
    }

    override fun setScaleType(scaleType: ScaleType) {
        Log.d(TAG, "setScaleType $scaleType")
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType)
        } else {
            gesturedScaleType = scaleType
        }
    }

    override fun getScaleType() = gesturedScaleType

    override fun setImageResource(resId: Int) {
        Log.d(TAG, "setImageResource $resId")
        super.setImageResource(resId)
        initialImage()
    }

    override fun setImageBitmap(bm: Bitmap) {
        Log.d(TAG, "setImageBitmap $bm")
        super.setImageBitmap(bm)
        initialImage()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        Log.d(TAG, "setImageDrawable $drawable")
        super.setImageDrawable(drawable)
        initialImage()
    }

    override fun setImageURI(uri: Uri?) {
        Log.d(TAG, "setImageURI $uri")
        super.setImageURI(uri)
        initialImage()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG, "onMeasure $drawable ${drawable?.intrinsicWidth} ${drawable?.intrinsicHeight}")
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicWidth == 0) {
            setMeasuredDimension(0, 0)
            return
        }

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val totalWidth = getViewSize(widthMode, widthSize, drawable.intrinsicWidth)
        val totalHeight = getViewSize(heightMode, heightSize, drawable.intrinsicHeight)

        setMeasuredDimension(
            totalWidth - paddingLeft - paddingRight,
            totalHeight - paddingTop - paddingBottom
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onLayout $changed $left $top $right $bottom")
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG, "onDraw")
        super.onDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged $w $h $oldw $oldh")

        viewWidth = w
        viewHeight = h

        initialImage()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return isScrolledToHorizontalEnd == 0
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return isScrolledToVerticalEnd == 0
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

    private fun initialStyle(defStyleAttr: Int) {

    }

    private fun scaleImage(scaleFactor: Float, focusX: Float, focusY: Float) {
        val prevScale = currentScale
        currentScale *= scaleFactor
        var realScaleFactor = scaleFactor
        if (currentScale > maxScale) {
            currentScale = maxScale
            realScaleFactor = maxScale / prevScale
        }
        if (currentScale < minScale) {
            currentScale = minScale
            realScaleFactor = minScale / prevScale
        }
        displayMatrix.postScale(realScaleFactor, realScaleFactor, focusX, focusY)
        imageMatrix = displayMatrix
//        fixTranslation()
    }

    private fun translateImage(dx: Float, dy: Float) {
        displayMatrix.postTranslate(dx, dy)
        imageMatrix = displayMatrix
        correctBoundary()
    }

    private fun correctBoundary() {
        imageMatrix.getValues(matrixValues)
        matrixValues.apply {
            val transX = this[Matrix.MTRANS_X]
            val transY = this[Matrix.MTRANS_Y]

            Log.d(TAG, "transX $transX transY $transY")

            val correctionX =
                getCorrection(transX, viewWidth.toFloat(), drawable.intrinsicWidth * currentScale)
            val correctionY =
                getCorrection(transY, viewHeight.toFloat(), drawable.intrinsicHeight * currentScale)
            Log.d(TAG, "correctionX $correctionX correctionY $correctionY")

            if (correctionX != 0f || correctionY != 0f) {
                displayMatrix.postTranslate(correctionX, correctionY)
                imageMatrix = displayMatrix
            }

            Log.d(TAG, "$isScrolledToHorizontalEnd $isScrolledToVerticalEnd")
            if (correctionX != 0f && (correctionX * isScrolledToHorizontalEnd) == 0f) {
                isScrolledToHorizontalEnd = if (correctionX > 0) 1 else -1
                listener?.onScrollToHorizontalEnd(isScrolledToHorizontalEnd)
            } else if (correctionX == 0f && isScrolledToHorizontalEnd != 0) {
                isScrolledToHorizontalEnd = 0
                listener?.onScrollToHorizontalEnd(0)
            }

            if (correctionY != 0f && (correctionY * isScrolledToVerticalEnd) == 0f) {
                isScrolledToVerticalEnd = if (correctionY > 0) -1 else 1
                listener?.onScrollToVerticalEnd(isScrolledToVerticalEnd)
            } else if (correctionY == 0f && isScrolledToVerticalEnd != 0) {
                isScrolledToVerticalEnd = 0
                listener?.onScrollToVerticalEnd(isScrolledToVerticalEnd)
            }
        }
    }

    private fun getCorrection(trans: Float, viewSize: Float, contentSize: Float): Float {
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

    private fun initialImage() {
        Log.d(TAG, "transformImage")
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }

        var scaleWidth = viewWidth / drawable.intrinsicWidth.toFloat()
        var scaleHeight = viewHeight / drawable.intrinsicHeight.toFloat()
        Log.d(TAG, "$scaleWidth $scaleHeight")
        Log.d(TAG, "gesturedScaleType $gesturedScaleType")
        when (gesturedScaleType) {
            ScaleType.FIT_CENTER -> {
                min(scaleWidth, scaleHeight).apply {
                    scaleWidth = this
                    scaleHeight = this
                }
                displayMatrix.postScale(scaleWidth, scaleHeight)
            }
            ScaleType.CENTER_CROP -> {
                max(scaleWidth, scaleHeight).apply {
                    scaleWidth = this
                    scaleHeight = this
                }
                displayMatrix.postScale(scaleWidth, scaleHeight)
            }
            else -> {

            }
        }

        val diffWidth = viewWidth - scaleWidth * drawable.intrinsicWidth
        val diffHeight = viewHeight - scaleHeight * drawable.intrinsicHeight

        when (gesturedScaleType) {
            ScaleType.FIT_CENTER -> {
                displayMatrix.postTranslate(diffWidth / 2, diffHeight / 2)
            }
        }

        imageMatrix = displayMatrix
    }

    private fun getViewSize(mode: Int, size: Int, drawableSize: Int): Int {
        return when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> min(drawableSize, size)
            MeasureSpec.UNSPECIFIED -> drawableSize
            else -> size
        }
    }

    companion object {
        private const val TAG = "[GesturedImageView]"
    }
}