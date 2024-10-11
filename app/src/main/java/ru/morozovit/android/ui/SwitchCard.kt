package ru.morozovit.android.ui

import android.R.attr.x
import android.R.attr.y
import android.annotation.SuppressLint
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

@SuppressLint("ClickableViewAccessibility")
fun makeSwitchCard(card: MaterialCardView) {
    val switch = card.getChildAt(0) as MaterialSwitch

    var switchOnTouch: ((View, MotionEvent) -> Boolean)? = null
    val switchCardOnTouch: (View, MotionEvent) -> Boolean = { _: View, event: MotionEvent ->
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            val downTime = SystemClock.uptimeMillis();
            val eventTime = SystemClock.uptimeMillis() + 100;
            val x = 0.0f;
            val y = 0.0f;
            val metaState = 0;
            val motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                event.action,
                x,
                y,
                metaState
            )
            switch.setOnTouchListener(null)
            switch.dispatchTouchEvent(motionEvent)
            switch.setOnTouchListener(switchOnTouch)
        }
        false
    }
    switchOnTouch = { _, event ->
        var ret = false
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            val background = card.background
            if (background is RippleDrawable) {
                background.setHotspot(x.toFloat(), y.toFloat())
            }

            card.isPressed = event.action == MotionEvent.ACTION_DOWN
            if (event.action == MotionEvent.ACTION_UP) {
                switch.isChecked = !switch.isChecked
                ret = true
            }
        }
        ret
    }
    card.setOnTouchListener(switchCardOnTouch)
    switch.setOnTouchListener(switchOnTouch)
}