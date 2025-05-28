package io.toolbox.ui.protection.applocker

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.core.net.toUri
import androidx.core.view.postDelayed
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import io.toolbox.Settings.Applocker.UnlockMode.PRESS_TITLE
import io.toolbox.databinding.FakeCrashBinding
import ru.morozovit.android.appName
import ru.morozovit.android.screenWidth

class FakeCrashActivity: androidx.appcompat.app.AppCompatActivity() {
    private lateinit var binding: FakeCrashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FakeCrashBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        supportActionBar?.hide()
        actionBar?.hide()

        // Set proper width
        binding.alFcR.layoutParams.let {
            it.width = screenWidth
            binding.alFcR.layoutParams = it
        }

        binding.alFcL.text = String.format(resources.getString(R.string.app_keeps_stopping), "null")

        var packageName = ""

        with(intent.extras) {
            if (this != null) {
                packageName = getString("appPackage") ?: return@with
                val appName = appName(this@FakeCrashActivity, packageName) ?: return@with
                binding.alFcL.text = String.format(resources.getString(R.string.app_keeps_stopping), appName)
            }
        }

        binding.root.postDelayed(15000) {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.alFcAiB.setOnClickListener {
            if (packageName != "") {
                val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
                setResult(RESULT_OK)
                finish()
            }
        }

        binding.alFcOaaB?.setOnClickListener {
            if (packageName != "") {
                val intent = applicationContext
                    .packageManager
                    .getLaunchIntentForPackage(packageName)
                startActivity(intent)
                setResult(RESULT_OK)
                finish()
            }
        }

        val listener: (View?) -> Boolean = {
            setResult(RESULT_OK)
            finish()
            PasswordInputActivity.start(this, packageName)
            false
        }

        when (Settings.Applocker.unlockMode) {
            LONG_PRESS_APP_INFO -> binding.alFcAiB.setOnLongClickListener(listener)
            LONG_PRESS_CLOSE -> binding.alFcCaB.setOnLongClickListener(listener)
            LONG_PRESS_TITLE -> binding.alFcL.setOnLongClickListener(listener)
            PRESS_TITLE -> binding.alFcL.setOnClickListener { listener(binding.alFcL) }
            LONG_PRESS_OPEN_APP_AGAIN -> binding.alFcOaaB?.setOnLongClickListener { listener(binding.alFcOaaB) }
        }

        binding.alFcCaB.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        // Exit because we don't want the potential intruder to see that the dialog is fake
        setResult(RESULT_OK)
        finish()
    }
}