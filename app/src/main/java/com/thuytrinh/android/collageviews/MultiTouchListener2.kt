package com.thuytrinh.android.collageviews

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.max
import kotlin.math.min

class MultiTouchListener2 : OnTouchListener {
    var isRotateEnabled: Boolean = false
    var isTranslateEnabled: Boolean = true
    var isScaleEnabled: Boolean = true
    var minimumScale: Float = 0.5f
    var maximumScale: Float = 10.0f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX = 0f
    private var mPrevY = 0f
    private val mScaleGestureDetector: ScaleGestureDetector

    init {
        mScaleGestureDetector = ScaleGestureDetector(ScaleGestureListener())
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        view.performClick()
        mScaleGestureDetector.onTouchEvent(view, event)

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
                    if (!mScaleGestureDetector.isInProgress) {
                        adjustTranslation(view, currX - mPrevX, currY - mPrevY)
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
//                if (view.scaleX == 1f) {
//                    view.animate()
//                        .translationX(0f)
//                        .translationY(0f)
//                        .duration = 500
//                }
            }
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
//                if (view.scaleX == 1f) {
//                    view.animate()
//                        .translationX(0f)
//                        .translationY(0f)
//                        .duration = 500
//                }
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
            val info = TransformInfo()
            info.deltaScale = if (isScaleEnabled) detector.scaleFactor else 1.0f
            info.deltaAngle = if (isRotateEnabled) Vector2D.getAngle(
                mPrevSpanVector,
                detector.currentSpanVector
            ) else 0.0f
            info.deltaX = if (isTranslateEnabled) detector.focusX - mPivotX else 0.0f
            info.deltaY = if (isTranslateEnabled) detector.focusY - mPivotY else 0.0f
            info.pivotX = mPivotX
            info.pivotY = mPivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale

            move(view, info)
            return false
        }

        override fun onScaleEnd(view: View, detector: ScaleGestureDetector) {
            super.onScaleEnd(view, detector)
//            if (view.scaleX == 1f) {
//                view.animate()
//                    .translationX(0f)
//                    .translationY(0f)
//                    .duration = 500
//            }
        }
    }

    private inner class TransformInfo {
        var deltaX: Float = 0f
        var deltaY: Float = 0f
        var deltaScale: Float = 0f
        var deltaAngle: Float = 0f
        var pivotX: Float = 0f
        var pivotY: Float = 0f
        var minimumScale: Float = 0f
        var maximumScale: Float = 0f
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
        private fun adjustAngle(degrees: Float): Float {
            var degrees1 = degrees
            if (degrees1 > 180.0f) {
                degrees1 -= 360.0f
            } else if (degrees1 < -180.0f) {
                degrees1 += 360.0f
            }

            return degrees1
        }

        private fun move(view: View, info: TransformInfo) {
            if (view.scaleX == 1f) {
                view.animate()
                    .translationX(1f)
                    .translationY(1f)
                    .duration = 500
            } else {
                computeRenderOffset(view, info.pivotX, info.pivotY)
                adjustTranslation(view, info.deltaX, info.deltaY)
            }

            // Assume that scaling still maintains aspect ratio.
            var scale = view.scaleX * info.deltaScale
            scale = max(
                info.minimumScale.toDouble(),
                min(info.maximumScale.toDouble(), scale.toDouble())
            )
                .toFloat()
            view.scaleX = scale
            view.scaleY = scale

            val rotation = adjustAngle(view.rotation + info.deltaAngle)
            view.rotation = rotation
        }

        private fun adjustTranslation(view: View, deltaX: Float, deltaY: Float, animation: Boolean = false) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            if (animation) {
                view.animate()
                   .translationX(view.translationX + deltaVector[0])
                   .translationY(view.translationY + deltaVector[1])
                   .duration = 500
            } else {
                view.translationX += deltaVector[0]
                view.translationY += deltaVector[1]
            }
        }

        private fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float, animation: Boolean = false) {
            if (view.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }

            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(prevPoint)

            view.pivotX = pivotX
            view.pivotY = pivotY

            val currPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(currPoint)

            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]

            if (animation) {
                view.animate()
                   .translationX(view.translationX - offsetX)
                   .translationY(view.translationY - offsetY)
                   .duration = 500
            } else {
                view.translationX -= offsetX
                view.translationY -= offsetY
            }
        }
    }
}