package ru.morozovit.android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout


class StatusFitView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        fitsSystemWindows = true
    }

    override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
        val result = super.onApplyWindowInsets(insets)
        setPadding(
            paddingLeft,
            paddingTop,
            paddingRight,
            0)
        return result
    }
}