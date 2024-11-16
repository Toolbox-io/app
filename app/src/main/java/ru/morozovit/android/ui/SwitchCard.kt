package ru.morozovit.android.ui

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton

@SuppressLint("ClickableViewAccessibility")
fun makeSwitchCard(card: ViewGroup, switch: CompoundButton) {
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