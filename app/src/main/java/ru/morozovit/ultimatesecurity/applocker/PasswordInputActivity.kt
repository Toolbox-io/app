package ru.morozovit.ultimatesecurity.applocker

import android.os.Bundle
import android.os.Handler
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Service
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.databinding.PasswordBinding
import ru.morozovit.ultimatesecurity.screenWidth


class PasswordInputActivity: AppCompatActivity() {
    private lateinit var binding: PasswordBinding

    companion object {
        const val RESULT_INVALID_PASSWORD = 1
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

        var setPassword = false

        var packageName = ""

        with (intent.extras) {
            if (this != null) {
                try {
                    packageName = getString("appPackage")!!
                } catch (_: NullPointerException) {}
                setPassword = getBoolean("setPassword")
            }
        }

        when (setPassword) {
            true -> {
                binding.pwIm.visibility = GONE
                binding.pwSm.visibility = VISIBLE
            }
            false -> {
                binding.pwIm.visibility = VISIBLE
                binding.pwSm.visibility = GONE
            }
        }

        binding.pwOk.setOnClickListener {
            when (setPassword) {
                false -> {
                    val password = binding.pwEt.text.toString()
                    if (password == Settings.Applocker.password) {
                        setResult(RESULT_OK, intent)
                        finish()
                        Service.instance?.lock = true
                        val intent = applicationContext
                            .packageManager
                            .getLaunchIntentForPackage(packageName)
                        startActivity(intent)
                        Handler(mainLooper).postDelayed(2000) {
                            Service.instance?.lock = false
                        }
                    } else {
                        setResult(RESULT_INVALID_PASSWORD)
                        finish()
                    }
                }
                true -> {
                    val oldPassword = binding.pwOpw.text.toString()
                    val newPassword = binding.pwNpw.text.toString()
                    val confirmPassword = binding.pwCpw.text.toString()
                    if (oldPassword == Settings.Applocker.password && newPassword == confirmPassword) {
                        setResult(RESULT_OK, intent.putExtra("password", newPassword))
                        finish()
                    } else {
                        Toast.makeText(this, R.string.passwords_dont_match, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.pwC.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
