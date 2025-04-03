package io.toolbox.ui.main

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


class GreenScreenActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(
            View(this).apply {
                setBackgroundColor("#00ff00".toColorInt())
            }
        )
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
    }
}