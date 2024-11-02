package com.thuytrinh.android.collageviews

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import kotlin.math.max
import kotlin.math.min

class MultiTouchListener : OnTouchListener {
    var isRotateEnabled = false
    var isTranslateEnabled = true
    var isScaleEnabled = true
    var minimumScale = 1f
    var maximumScale = 10f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX = 0f
    private var mPrevY = 0f
    private val mScaleGestureDetector = ScaleGestureDetector(ScaleGestureListener())

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(view, event)
        view.performClick()

        if (!isTranslateEnabled) {
            return true
        }

        val action = event.action
        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y

                // Save the ID of this pointer.
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Find the index of the active pointer and fetch its position.
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex != -1) {
                    val currX = event.getX(pointerIndex)
                    val currY = event.getY(pointerIndex)

                    // Only move if the ScaleGestureDetector isn't processing a
                    // gesture.
                    if (!mScaleGestureDetector.isInProgress && view.scaleX > 1) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY)
                    }
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mActivePointerId =
                    INVALID_POINTER_ID
                if (view.scaleY <= 1) {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationX(0f)
                        .translationY(0f)
                        .duration = 500
                }
                val imgView = view as CollageView
                val coords = imgView.imageCoords
                val x = imgView.left + coords.x + imgView.translationX + imgView.width / 2 * imgView.scaleX
                val y = imgView.left + coords.y + imgView.translationY + imgView.height / 2 * imgView.scaleY
                val width = coords.width * imgView.scaleX
                val height = coords.height * imgView.scaleY
                if (x > width) {
                    imgView.translationX -= x
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Extract the index of the pointer that left the touch sensor.
                val pointerIndex =
                    (action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mPrevX = event.getX(newPointerIndex)
                    mPrevY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    private fun move(view: View, info: TransformInfo) {
        // Assume that scaling still maintains aspect ratio.
        var scale = view.scaleX * info.deltaScale
        scale = max(
            info.minimumScale.toDouble(),
            min(info.maximumScale.toDouble(), scale.toDouble())
        ).toFloat()
        view.scaleX = scale
        view.scaleY = scale

        val rotation = adjustAngle(view.rotation + info.deltaAngle)
        view.rotation = rotation
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var mPivotX = 0f
        private var mPivotY = 0f
        private val mPrevSpanVector = Vector2D()

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            mPivotX = detector.focusX
            mPivotY = detector.focusY
            mPrevSpanVector.set(detector.currentSpanVector)
            return true
        }

        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            move(view, TransformInfo().apply {
                deltaScale = if (isScaleEnabled) detector.scaleFactor else 1.0f
                deltaAngle = if (isRotateEnabled) Vector2D.getAngle(
                    mPrevSpanVector,
                    detector.currentSpanVector
                ) else 0.0f
                deltaX = 0.0f
                deltaY = 0.0f
                pivotX = mPivotX
                pivotY = mPivotY
                minimumScale = 0.1f
                maximumScale = 20f
            })
            return false
        }
    }

    private class TransformInfo {
        var deltaX = 0f
        var deltaY = 0f
        var deltaScale = 0f
        var deltaAngle = 0f
        var pivotX = 0f
        var pivotY = 0f
        var minimumScale = 0f
        var maximumScale = 0f
    }

    companion object {
        const val INVALID_POINTER_ID = -1
        fun adjustAngle(degrees: Float): Float {
            var degrees1 = degrees
            if (degrees1 > 180.0f) {
                degrees1 -= 360.0f
            } else if (degrees < -180.0f) {
                degrees1 += 360.0f
            }

            return degrees
        }

        private fun isImageCompletelyVisibleOnScreen(view: View): Boolean {
            val x = view.x
            val y = view.y

            val parent = view.parent as ViewGroup

            val sw = parent.width
            val sh = parent.height

            val vw = view.width * view.scaleX
            val vh = view.height * view.scaleY

            return x >= 0 && y >= 0 && vw <= sw && vh <= sh
        }

        fun adjustTranslation(view: View, deltaX: Float, deltaY: Float) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            view.translationX += deltaVector[0]
            view.translationY += deltaVector[1]
        }

        fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float) {
            if (view.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }

            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(prevPoint)
            val currPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(currPoint)

            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]

            view.pivotX = pivotX
            view.pivotY = pivotY

            view.translationX -= offsetX
            view.translationY -= offsetY
        }
    }
}