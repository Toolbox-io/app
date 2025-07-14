package io.toolbox.ui.protection.applocker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import io.toolbox.ui.Auth
import ru.morozovit.android.utils.ComposeLifecycleOwner
import ru.morozovit.android.utils.ui.ComposeView
import ru.morozovit.android.utils.runOrLog
import java.lang.ref.WeakReference

class ApplockerAuthOverlay(val context: Context): Auth(
    context,
    Intent().apply {
        putExtra("setStarted", true)
        putExtra("applocker", true)
        putExtra("noAnim", true)
    }
) {
    init {
        if (instance != null) throw UnsupportedOperationException()
    }

    @SuppressLint("StaticFieldLeak")
    companion object {
        var shown = false
            private set

        private var _instance: WeakReference<ApplockerAuthOverlay>? = null
        val instance get() = _instance?.get()
    }

    private lateinit var view: ComposeView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
    private val lifecycle = ComposeLifecycleOwner()

    fun show() {
        if (!shown) {
            with(context) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    lifecycle.onCreate()
                    // set the layout parameters of the window
                    layoutParams = WindowManager.LayoutParams( // Shrink the window to wrap the content rather
                        // than filling the screen
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,  // Display it on top of other application windows
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Don't let it grab the input focus
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        // through any transparent parts
                        PixelFormat.TRANSLUCENT
                    )

                    view = ComposeView(lifecycle) {
                        AuthScreen(0)
                    }

                    // Define the position of the
                    // window within the screen
                    layoutParams.gravity = Gravity.CENTER

                    if (Build.VERSION.SDK_INT >= 28) {
                        layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    runOrLog("Applocker") {
                        // check if the view is already
                        // inflated or present in the window
                        if (view.windowToken == null && view.parent == null) {
                            windowManager.addView(view, layoutParams);
                            shown = true
                            _instance = WeakReference(this@ApplockerAuthOverlay)
                        }
                    }
                }
            }
        }
    }

    fun hide() {
        if (shown) {
            windowManager.removeView(view)
            shown = false
            _instance = null
            Handler(Looper.getMainLooper()).post(lifecycle::onDestroy)
        }
    }
}