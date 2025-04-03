package io.toolbox.ui.customization.shortcuts

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.toolbox.R
import ru.morozovit.android.launchFiles

class FilesShortcut: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            installSplashScreen()
        } catch (_: Exception) {}
        if (!launchFiles()) {
            Toast.makeText(this, R.string.failed_to_launch_files, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}