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
    private var top: Int? = null

    init {
        fitsSystemWindows = true
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val pT = top ?: 0
        val result = super.onApplyWindowInsets(insets)

        setPadding(
            paddingLeft,
            if (paddingTop != 0) paddingTop else pT,
            paddingRight,
            0
        )
        top = paddingTop
        return result
    }
}