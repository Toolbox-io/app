package ru.morozovit.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

@SuppressLint("AppCompatCustomView")
class ZoomImageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr), OnTouchListener {
    init {
        init()
    }

    companion object {
        /**
         * Calculate the distance between zero fingers
         * @param event
         * @return
         */
        fun getDoubleFingerDistance(event: MotionEvent): Float {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return sqrt((x * x + y * y).toDouble()).toFloat()
        }
    }

    object ZoomMode {
        const val ORDINARY: Int = 0
        const val ZOOM_IN: Int = 1
        const val TWO_FINGER_ZOOM: Int = 2
    }

    private var matrix: Matrix? = null

    //imageView size
    private var viewSize: PointF? = null

    //The size of the picture
    private var imageSize: PointF? = null

    //The size of the picture after scaling
    private val scaleSize = PointF()

    //The initial zoom ratio of width and height
    private val originScale = PointF()

    //xy real-time coordinates of bitmap in imageview
    private val bitmapOriginPoint = PointF()

    //Clicked point
    private val clickPoint = PointF()

    //Set the double-click check time limit
    private val doubleClickTimeSpan: Long = 250

    //Last click time
    private var lastClickTime: Long = 0

    //Double click to zoom in
    private val doubleClickZoom = 2

    //Current zoom mode
    private var zoomInMode = ZoomMode.ORDINARY

    //Temporary coordinate scale data
    private val tempPoint = PointF()

    //Maximum zoom ratio
    private val maxScrole = 4f

    //The distance between two points
    private var doublePointDistance = 0f

    //The center point when two fingers zoom
    private val doublePointCenter = PointF()

    //Two-finger zoom ratio
    private var doubleFingerScrole = 0f

    //Number of fingers touched last time
    private var lastFingerNum = 0

    data class ImageCoords(val x: Float, val y: Float, val width: Int, val height: Int)

    @Suppress("unused")
    val imageCoords: ImageCoords
        get() {
        val matrix = Matrix()
        imageMatrix.invert(matrix)

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        val drawableCorners = floatArrayOf(
            0f,
            0f,
            drawableWidth.toFloat(),
            0f,
            drawableWidth.toFloat(),
            drawableHeight.toFloat(),
            0f,
            drawableHeight.toFloat()
        )
        matrix.mapPoints(drawableCorners)

        val x = drawableCorners[0]
        val y = drawableCorners[1]
        return ImageCoords(x, y, drawableWidth, drawableHeight)
    }

    private fun init() {
        setOnTouchListener(this)
        scaleType = ScaleType.MATRIX
        matrix = Matrix()
    }


    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        viewSize = PointF(width.toFloat(), height.toFloat())

        val drawable = drawable
        if (drawable != null) {
            imageSize = PointF(drawable.minimumWidth.toFloat(), drawable.minimumHeight.toFloat())
            showCenter()
        }
    }

    /**
     * Set the picture to be displayed in the middle ratio
     */
    private fun showCenter() {
        val scalex = viewSize!!.x / imageSize!!.x
        val scaley = viewSize!!.y / imageSize!!.y

        val scale = min(scalex.toDouble(), scaley.toDouble()).toFloat()
        scaleImage(PointF(scale, scale))

        //Move the picture and save the coordinates of the upper left corner (or origin) of the original picture
        if (scalex < scaley) {
            translationImage(PointF(0f, viewSize!!.y / 2 - scaleSize.y / 2))
            bitmapOriginPoint.x = 0f
            bitmapOriginPoint.y = viewSize!!.y / 2 - scaleSize.y / 2
        } else {
            translationImage(PointF(viewSize!!.x / 2 - scaleSize.x / 2, 0f))
            bitmapOriginPoint.x = viewSize!!.x / 2 - scaleSize.x / 2
            bitmapOriginPoint.y = 0f
        }
        //Save the initial zoom ratio
        originScale[scale] = scale
        doubleFingerScrole = scale
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //Finger press event
                //Record the coordinates of the clicked point
                clickPoint[event.x] = event.y
                //Determine the number of points held down on the screen at this time, trigger when only one point is clicked on the current screen
                if (event.pointerCount == 1) {
                    //Set a click interval to determine whether it is a double click
                    if (System.currentTimeMillis() - lastClickTime <= doubleClickTimeSpan) {
                        //If the zoom mode of the picture is normal at this time, it will trigger double-click to zoom in
                        if (zoomInMode == ZoomMode.ORDINARY) {
                            //Record the ratio of the distance between the clicked point and the upper left corner of the picture on the x, y axis and the side length of the picture on the x, y axis, respectively,
                            //It is convenient to calculate the coordinate point corresponding to this point after zooming
                            tempPoint[(clickPoint.x - bitmapOriginPoint.x) / scaleSize.x] =
                                (clickPoint.y - bitmapOriginPoint.y) / scaleSize.y
                            //To zoom
                            scaleImage(
                                PointF(
                                    originScale.x * doubleClickZoom,
                                    originScale.y * doubleClickZoom
                                )
                            )
                            //Get the xy coordinate of the upper left corner of the picture after zooming
                            bitmapOffset

                            //Translate the picture so that the position of the clicked point remains unchanged. Here is the xy coordinate that was clicked after zooming,
                            //Calculate the difference with the xy coordinate value of the original clicked position, and then do a translation operation
                            translationImage(
                                PointF(
                                    clickPoint.x - (bitmapOriginPoint.x + tempPoint.x * scaleSize.x),
                                    clickPoint.y - (bitmapOriginPoint.y + tempPoint.y * scaleSize.y)
                                )
                            )
                            zoomInMode = ZoomMode.ZOOM_IN
                            doubleFingerScrole = originScale.x * doubleClickZoom
                        } else {
                            //Double click to restore
                            showCenter()
                            zoomInMode = ZoomMode.ORDINARY
                        }
                    } else {
                        lastClickTime = System.currentTimeMillis()
                    }
                }
            }

            MotionEvent.ACTION_POINTER_DOWN ->                 //There is already a point on the screen to hold down and press a point to trigger the event
                //Calculate the distance between the first two fingers
                doublePointDistance = getDoubleFingerDistance(event)

            MotionEvent.ACTION_POINTER_UP -> {
                //There are already two points on the screen that will trigger the event when you press and release a point
                //When a finger leaves the screen, modify the state so that if you double-click the screen, you can restore to the original size
                zoomInMode = ZoomMode.ZOOM_IN
                //Record the two-finger zoom ratio at this time
                doubleFingerScrole = scaleSize.x / imageSize!!.x
                //Record the number of points touched on the screen at this time
                lastFingerNum = 1
                //Judging the scaled ratio, if it is less than the original ratio, it will be restored to the original size
                if (scaleSize.x < viewSize!!.x && scaleSize.y < viewSize!!.y) {
                    zoomInMode = ZoomMode.ORDINARY
                    showCenter()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                //The event is triggered when the finger moves
                if (zoomInMode != ZoomMode.ORDINARY) {
                    //If it is multi-finger, the calculated center point is the assumed clicked point
                    var currentX = 0f
                    var currentY = 0f
                    //Get how many points are touched on the screen at this time
                    val pointCount = event.pointerCount
                    //Calculate the coordinates of the intermediate point
                    var i = 0
                    while (i < pointCount) {
                        currentX += event.getX(i)
                        currentY += event.getY(i)
                        i++
                    }
                    currentX /= pointCount.toFloat()
                    currentY /= pointCount.toFloat()
                    //When the number of touched points on the screen changes, consider the latest calculated center point as the clicked point
                    if (lastFingerNum != event.pointerCount) {
                        clickPoint.x = currentX
                        clickPoint.y = currentY
                        lastFingerNum = event.pointerCount
                    }
                    //When the finger is moved, the coordinates of the center point calculated in real time are subtracted from the coordinates of the clicked point to get the distance to be moved
                    val moveX = currentX - clickPoint.x
                    val moveY = currentY - clickPoint.y
                    //Calculate the boundary so that it cannot be out of the boundary, but if it is moved when zooming with two fingers, because of the zoom effect,
                    //So the boundary judgment at this time is invalid
                    val moveFloat = moveBorderDistance(moveX, moveY)
                    //Handle events of moving pictures
                    translationImage(PointF(moveFloat[0], moveFloat[1]))
                    clickPoint[currentX] = currentY
                }
                //Judging that two fingers are currently touching the screen before processing the zoom event
                if (event.pointerCount == 2) {
                    //If the zoomed size at this time is greater than or equal to the set maximum zoomed size, it will not be processed
                    if (!(scaleSize.x / imageSize!!.x >= originScale.x * maxScrole
                                || scaleSize.y / imageSize!!.y >= originScale.y * maxScrole) || getDoubleFingerDistance(event) - doublePointDistance <= 0
                    ) {
                        //Set here when the distance change of the two-finger zoom is greater than 50, and the current is not in the two-finger zoom state, calculate the center point, and wait for some operations
                        if (abs((getDoubleFingerDistance(event) - doublePointDistance).toDouble()) > 50
                            && zoomInMode != ZoomMode.TWO_FINGER_ZOOM
                        ) {
                            //Calculate the center point between the two fingers as the center point of zooming
                            doublePointCenter[(event.getX(0) + event.getX(1)) / 2] =
                                (event.getY(0) + event.getY(1)) / 2
                            //The center point of the two fingers is assumed to be the clicked point
                            clickPoint.set(doublePointCenter)
                            //The following is basically the same as double-clicking to zoom in
                            bitmapOffset
                            //Record the ratio of the distance between the clicked point and the upper left corner of the picture on the x, y axis and the side length of the picture on the x, y axis, respectively,
                            //It is convenient to calculate the coordinate point corresponding to this point after zooming
                            tempPoint[(clickPoint.x - bitmapOriginPoint.x) / scaleSize.x] =
                                (clickPoint.y - bitmapOriginPoint.y) / scaleSize.y
                            //Set to enter the two-finger zoom state
                            zoomInMode = ZoomMode.TWO_FINGER_ZOOM
                        }
                        //If you have entered the two-finger zoom state, directly calculate the zoom ratio and perform displacement
                        if (zoomInMode == ZoomMode.TWO_FINGER_ZOOM) {
                            //Multiply the current zoom ratio by the zoom ratio of the distance between the two fingers at this time to get the corresponding image should be zoomed ratio
                            val scrole =
                                doubleFingerScrole * getDoubleFingerDistance(event) / doublePointDistance
                            //This is the same as when double-clicking to zoom in
                            scaleImage(PointF(scrole, scrole))
                            bitmapOffset
                            translationImage(
                                PointF(
                                    clickPoint.x - (bitmapOriginPoint.x + tempPoint.x * scaleSize.x),
                                    clickPoint.y - (bitmapOriginPoint.y + tempPoint.y * scaleSize.y)
                                )
                            )
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                //The event is triggered when the finger is released
                Log.e("kzg", "***********************ACTION_UP")
                lastFingerNum = 0
            }
        }
        return true
    }


    private fun scaleImage(scaleXY: PointF) {
        matrix!!.setScale(scaleXY.x, scaleXY.y)
        scaleSize[scaleXY.x * imageSize!!.x] = scaleXY.y * imageSize!!.y
        imageMatrix = matrix
    }

    /**
     * Translate the image in the x and y axis directions
     * @param pointF
     */
    private fun translationImage(pointF: PointF) {
        matrix!!.postTranslate(pointF.x, pointF.y)
        imageMatrix = matrix
    }


    /**
     * Prevent the moving picture from exceeding the boundary, calculate the boundary condition
     * @param moveX
     * @param moveY
     * @return
     */
    private fun moveBorderDistance(moveX: Float, moveY: Float): FloatArray {
        @Suppress("NAME_SHADOWING") var moveX = moveX
        @Suppress("NAME_SHADOWING") var moveY = moveY
        // Calculate the coordinates of the upper left corner of the bitmap
        bitmapOffset
        Log.e(
            "kzg",
            "**********************moveBorderDistance-- bitmapOriginPoint:$bitmapOriginPoint"
        )
        //Calculate the coordinates of the lower right corner of the bitmap
        val bitmapRightBottomX = bitmapOriginPoint.x + scaleSize.x
        val bitmapRightBottomY = bitmapOriginPoint.y + scaleSize.y

        if (moveY > 0) {
            // Slide down
            if (bitmapOriginPoint.y + moveY > 0) {
                moveY = if (bitmapOriginPoint.y < 0) {
                    -bitmapOriginPoint.y
                } else {
                    0f
                }
            }
        } else if (moveY < 0) {
            // Slide up
            if (bitmapRightBottomY + moveY < viewSize!!.y) {
                moveY = if (bitmapRightBottomY > viewSize!!.y) {
                    -(bitmapRightBottomY - viewSize!!.y)
                } else {
                    0f
                }
            }
        }

        if (moveX > 0) {
            //Swipe right
            if (bitmapOriginPoint.x + moveX > 0) {
                moveX = if (bitmapOriginPoint.x < 0) {
                    -bitmapOriginPoint.x
                } else {
                    0f
                }
            }
        } else if (moveX < 0) {
            //Slide left
            if (bitmapRightBottomX + moveX < viewSize!!.x) {
                moveX = if (bitmapRightBottomX > viewSize!!.x) {
                    -(bitmapRightBottomX - viewSize!!.x)
                } else {
                    0f
                }
            }
        }
        return floatArrayOf(moveX, moveY)
    }

    private val bitmapOffset: Unit
        /**
         * Get the coordinate point of the bitmap in the view
         */
        get() {
            val value = FloatArray(9)
            val offset = FloatArray(2)
            val imageMatrix = imageMatrix
            imageMatrix.getValues(value)
            offset[0] = value[2]
            offset[1] = value[5]
            bitmapOriginPoint[offset[0]] = offset[1]
        }
}
