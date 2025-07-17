package io.toolbox.ui.customization.shortcuts

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.toolbox.R
import ru.morozovit.android.utils.launchFiles

class FilesShortcut: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!launchFiles()) {
            Toast.makeText(
                this,
                R.string.failed_to_launch_files,
                Toast.LENGTH_SHORT
            ).show()
        }
        finish()
    }
}