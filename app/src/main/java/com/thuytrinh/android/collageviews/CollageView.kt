package com.thuytrinh.android.collageviews

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


class CollageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attrs, defStyle
) {
    data class ImageCoords(val x: Float, val y: Float, val width: Int, val height: Int)

    val imageCoords: ImageCoords get() {
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
}