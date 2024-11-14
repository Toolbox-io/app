package ru.morozovit.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.MainThread
import com.google.android.material.button.MaterialButton
import ru.morozovit.ultimatesecurity.R

class MaterialToggleIconButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = R.style.Widget_Material3_Button_IconButton_Filled_Toggleable_Active
): MaterialButton(ContextThemeWrapper(context, defStyleAttr), attributeSet, defStyleAttr) {
    var toggled = false
        set(value) {
            field = value
            invalidate()
        }

    var checkedBackgroundColor: Int = 0
        @MainThread
        set(value) {
            field = value
            invalidate()
        }

    var uncheckedBackgroundColor: Int = 0
        @MainThread
        set(value) {
            field = value
            invalidate()
        }

    init {
        val attributes = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.MaterialToggleIconButton
        )
        toggled = attributes.getBoolean(
            R.styleable.MaterialToggleIconButton_checked,
            false
        );

        val checkedColor = attributes.getColor(R.styleable.MaterialToggleIconButton_checkedColor, 0)
        if (checkedColor == 0) {
            TypedValue().apply {
                if (
                    context.theme.resolveAttribute(
                        com.google.android.material.R.attr.colorPrimary,
                        this,
                        true
                    )
                ) {
                    checkedBackgroundColor = context.getColor(resourceId)
                }
            }
        } else {
            checkedBackgroundColor = checkedColor
        }

        val uncheckedColor = attributes.getColor(R.styleable.MaterialToggleIconButton_uncheckedColor, 0)
        if (uncheckedColor == 0) {
            TypedValue().apply {
                if (
                    context.theme.resolveAttribute(
                        com.google.android.material.R.attr.colorOutline,
                        this,
                        true
                    )
                ) {
                    uncheckedBackgroundColor = context.getColor(resourceId)
                }
            }
        } else {
            uncheckedBackgroundColor = uncheckedColor
        }

        super.setBackgroundColor(if (toggled) checkedBackgroundColor else uncheckedBackgroundColor)
        iconSize = 25 * resources.displayMetrics.density.toInt()
        gravity = Gravity.CENTER
        iconGravity = ICON_GRAVITY_TEXT_START

        attributes.recycle()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        toggled = !toggled
        if (toggled) {
            setBackgroundColor(checkedBackgroundColor)
        } else {
            setBackgroundColor(uncheckedBackgroundColor)
        }
        return true
    }

    override fun setBackgroundColor(color: Int) {
        checkedBackgroundColor = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = widthMeasureSpec
        var height = heightMeasureSpec
        if (layoutParams.width == WRAP_CONTENT) {
            width = 60 * resources.displayMetrics.density.toInt()
        }
        if (layoutParams.height == WRAP_CONTENT) {
            height = 60 * resources.displayMetrics.density.toInt()
        }
        setMeasuredDimension(width, height)
        super.onMeasure(width, height)
    }
}