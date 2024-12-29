package ru.morozovit.android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import ru.morozovit.android.R


@Suppress("MemberVisibilityCanBePrivate")
class RoundedCornerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {
    private var mTopRightCornerRadius = 0
    private var mBottomRightCornerRadius = 0
    private var mTopLeftCornerRadius = 0
    private var mBottomLeftCornerRadius = 0

    var topRightCornerRadius
        get() = mTopRightCornerRadius
        @UiThread
        set(value) {
            mTopRightCornerRadius = value
            invalidate()
        }

    var bottomRightCornerRadius
        get() = mBottomRightCornerRadius
        @UiThread
        set(value) {
            mBottomRightCornerRadius = value
            invalidate()
        }

    var topLeftCornerRadius
        get() = mTopLeftCornerRadius
        @UiThread
        set(value) {
            mTopLeftCornerRadius = value
            invalidate()
        }

    var bottomLeftCornerRadius
        get() = mBottomLeftCornerRadius
        @UiThread
        set(value) {
            mBottomLeftCornerRadius = value
            postInvalidate()
        }

    var cornerRadius: Int
        get() {
            if (mTopLeftCornerRadius == mTopRightCornerRadius) {
                if (mBottomLeftCornerRadius == mBottomRightCornerRadius) {
                    return mTopLeftCornerRadius
                }
            }
            throw IllegalStateException("Corner radius isn't the same")
        }
        @UiThread
        set(value) {
            mTopLeftCornerRadius = value
            mTopRightCornerRadius = value
            mBottomRightCornerRadius = value
            mBottomLeftCornerRadius = value
            invalidate()
        }

    init {
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedCornerLayout
        )
        val cornerRadius = attributes.getDimensionPixelSize(
            R.styleable.RoundedCornerLayout_cornerRadius,
            0
        )
        this.cornerRadius = cornerRadius

        try {
            mTopRightCornerRadius = attributes.getDimensionPixelSizeOrThrow(
                R.styleable.RoundedCornerLayout_topRightCornerRadius
            )
        } catch (_: IllegalArgumentException) {}
        try {
            mBottomRightCornerRadius = attributes.getDimensionPixelSizeOrThrow(
                R.styleable.RoundedCornerLayout_bottomRightCornerRadius,
            )
        } catch (_: IllegalArgumentException) {}
        try {
            mTopLeftCornerRadius = attributes.getDimensionPixelSizeOrThrow(
                R.styleable.RoundedCornerLayout_topLeftCornerRadius
            )
        } catch (_: IllegalArgumentException) {}
        try {
            mBottomLeftCornerRadius = attributes.getDimensionPixelSizeOrThrow(
                R.styleable.RoundedCornerLayout_bottomLeftCornerRadius
            )
        } catch (_: IllegalArgumentException) {}

        attributes.recycle()
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    override fun dispatchDraw(canvas: Canvas) {
        val count = canvas.save()

        val path = Path()

        val cornerDimensions = floatArrayOf(
            topLeftCornerRadius.toFloat(), topLeftCornerRadius.toFloat(),
            topRightCornerRadius.toFloat(), topRightCornerRadius.toFloat(),
            bottomRightCornerRadius.toFloat(), bottomRightCornerRadius.toFloat(),
            bottomLeftCornerRadius.toFloat(), bottomLeftCornerRadius.toFloat()
        )

        path.addRoundRect(
            RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()),
            cornerDimensions, Path.Direction.CW
        )

        canvas.clipPath(path)

        super.dispatchDraw(canvas)
        canvas.restoreToCount(count)
    }
}