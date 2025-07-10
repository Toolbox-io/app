@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui.protection.applocker

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.Handler
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import io.toolbox.Settings
import io.toolbox.databinding.PasswordBinding
import io.toolbox.services.Accessibility
import ru.morozovit.android.screenWidth


class PasswordInputActivity: AppCompatActivity() {
    private lateinit var binding: PasswordBinding

    companion object {
        const val RESULT_INVALID_PASSWORD = 1

        inline fun start(context: Context, appPackage: String) {
            val intent = Intent(context, PasswordInputActivity::class.java)
            val b = Bundle()
            b.putString("appPackage", appPackage)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.addFlags(FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.putExtras(b)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PasswordBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        supportActionBar?.hide()
        actionBar?.hide()

        // Set proper width
        binding.pwR.layoutParams.let {
            it.width = screenWidth
            binding.pwR.layoutParams = it
        }

        var packageName = ""

        with (intent.extras) {
            if (this != null) {
                try {
                    packageName = getString("appPackage")!!
                } catch (_: NullPointerException) {}
            }
        }

        binding.pwIm.visibility = VISIBLE
        binding.pwSm.visibility = GONE

        binding.pwOk.setOnClickListener {
            val password = binding.pwEt.text.toString()
            if (Settings.Keys.Applocker.check(password)) {
                setResult(RESULT_OK, intent)
                finish()
                Accessibility.instance?.lock = true
                val intent = applicationContext
                    .packageManager
                    .getLaunchIntentForPackage(packageName)
                startActivity(intent)
                Handler(mainLooper).postDelayed(2000) {
                    Accessibility.instance?.lock = false
                }
            } else {
                setResult(RESULT_INVALID_PASSWORD)
                finish()
            }
        }

        binding.pwC.setOnClickListener {
            //homeScreen()
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
