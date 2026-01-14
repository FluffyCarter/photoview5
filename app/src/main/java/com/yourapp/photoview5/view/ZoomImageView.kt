package com.yourapp.photoview5.view
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val matrix = Matrix()
    private val viewRect = RectF()
    private val imageRect = RectF()
    private var minScale = 1.0f
    private var maxScale = 5.0f
    private var currentScale = 1.0f
    private var scaleGestureDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    init {
        scaleType = ScaleType.MATRIX
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewRect()
        if (drawable != null) {
            setupInitialScale()
        }
    }
    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null && width > 0 && height > 0) {
            setupInitialScale()
        }
    }
    private fun updateViewRect() {
        viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
    }
    private fun setupInitialScale() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        if (imageWidth <= 0 || imageHeight <= 0 || width <= 0 || height <= 0) {
            return
        }
        imageRect.set(0f, 0f, imageWidth, imageHeight)
        val scaleX = viewRect.width() / imageWidth
        val scaleY = viewRect.height() / imageHeight
        currentScale = min(scaleX, scaleY)
        minScale = currentScale
        matrix.reset()
        matrix.setScale(currentScale, currentScale)
        val scaledWidth = imageWidth * currentScale
        val scaledHeight = imageHeight * currentScale
        val translateX = (viewRect.width() - scaledWidth) / 2
        val translateY = (viewRect.height() - scaledHeight) / 2
        matrix.postTranslate(translateX, translateY)
        imageMatrix = matrix
        updateImageRect()
    }
    private fun updateImageRect() {
        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        imageRect.set(0f, 0f, imageWidth, imageHeight)
        matrix.mapRect(imageRect)
    }
    private fun canDrag(): Boolean {
        return imageRect.width() > viewRect.width() + 0.01f ||
                imageRect.height() > viewRect.height() + 0.01f
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && !isDragging && canDrag()) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (sqrt((dx * dx + dy * dy).toDouble()) > 10) {
                        isDragging = true
                    }
                }
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    var newLeft = imageRect.left + dx
                    var newTop = imageRect.top + dy
                    if (imageRect.width() <= viewRect.width()) {
                        newLeft = (viewRect.width() - imageRect.width()) / 2
                    } else {
                        if (newLeft > 0) newLeft = 0f
                        if (newLeft + imageRect.width() < viewRect.width()) {
                            newLeft = viewRect.width() - imageRect.width()
                        }
                    }
                    if (imageRect.height() <= viewRect.height()) {
                        newTop = (viewRect.height() - imageRect.height()) / 2
                    } else {
                        if (newTop > 0) newTop = 0f
                        if (newTop + imageRect.height() < viewRect.height()) {
                            newTop = viewRect.height() - imageRect.height()
                        }
                    }
                    matrix.postTranslate(newLeft - imageRect.left, newTop - imageRect.top)
                    imageMatrix = matrix
                    updateImageRect()
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY
            var newScale = currentScale * scaleFactor
            newScale = max(minScale, min(newScale, maxScale))
            if (currentScale != newScale) {
                val scale = newScale / currentScale
                matrix.postScale(scale, scale, focusX, focusY)
                imageMatrix = matrix
                currentScale = newScale
                updateImageRect()
                if (imageRect.width() < viewRect.width() || imageRect.height() < viewRect.height()) {
                    centerImage()
                }
            }
            return true
        }
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isDragging = false
            return true
        }
    }
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (currentScale > minScale * 1.1f) {
                resetZoom()
            } else {
                val targetScale = min(currentScale * 3.0f, maxScale)
                zoomTo(targetScale, e.x, e.y)
            }
            return true
        }
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!scaleGestureDetector.isInProgress && canDrag()) {
                matrix.postTranslate(-distanceX, -distanceY)
                imageMatrix = matrix
                updateImageRect()
                adjustBounds()
                return true
            }
            return false
        }
    }
    private fun zoomTo(scale: Float, focusX: Float, focusY: Float) {
        val targetScale = max(minScale, min(scale, maxScale))
        val scaleFactor = targetScale / currentScale
        matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
        imageMatrix = matrix
        currentScale = targetScale
        updateImageRect()
        if (imageRect.width() < viewRect.width() || imageRect.height() < viewRect.height()) {
            centerImage()
        } else {
            adjustBounds()
        }
    }
    private fun centerImage() {
        if (drawable == null) return
        val drawable = drawable!!
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val scaledWidth = imageWidth * currentScale
        val scaledHeight = imageHeight * currentScale
        var translateX = 0f
        var translateY = 0f
        if (scaledWidth < viewRect.width()) {
            translateX = (viewRect.width() - scaledWidth) / 2 - imageRect.left
        }
        if (scaledHeight < viewRect.height()) {
            translateY = (viewRect.height() - scaledHeight) / 2 - imageRect.top
        }
        if (translateX != 0f || translateY != 0f) {
            matrix.postTranslate(translateX, translateY)
            imageMatrix = matrix
            updateImageRect()
        }
    }
    private fun adjustBounds() {
        var adjusted = false
        if (imageRect.left > 0) {
            matrix.postTranslate(-imageRect.left, 0f)
            adjusted = true
        } else if (imageRect.right < viewRect.width()) {
            matrix.postTranslate(viewRect.width() - imageRect.right, 0f)
            adjusted = true
        }
        if (imageRect.top > 0) {
            matrix.postTranslate(0f, -imageRect.top)
            adjusted = true
        } else if (imageRect.bottom < viewRect.height()) {
            matrix.postTranslate(0f, viewRect.height() - imageRect.bottom)
            adjusted = true
        }
        if (adjusted) {
            imageMatrix = matrix
            updateImageRect()
        }
    }
    fun resetZoom() {
        setupInitialScale()
    }
}