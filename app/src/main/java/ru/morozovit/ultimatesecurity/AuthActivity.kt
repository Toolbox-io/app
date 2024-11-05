package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.TextView
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.databinding.AuthActivityBinding


class AuthActivity: BaseActivity() {
    companion object {
        const val PASSWORD_DOT = "●"
        const val PASSWORD_TEXT_SIZE = 30f
    }

    init {
        authEnabled = false
    }

    private lateinit var binding: AuthActivityBinding
    private val symbols = mutableMapOf<TextView, Char>()
    private val maxLength = 6

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            preSplashScreen()
        } catch (_: Exception) {}
        super.onCreate(savedInstanceState)
        if (globalPassword == "" || authenticated) {
            finish()
            return
        }
        binding = AuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val clear: (View) -> Unit = {
            for (symbol in symbols) {
                symbol.key
                    .animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(125)
                    .withEndAction {
                        binding.authPassword.removeView(symbol.key)
                        symbols.remove(symbol.key)
                    }
                    .start()
            }
        }
        val listener: (View) -> Unit = { v: View ->
            if (symbols.size < maxLength) {
                v as Button
                val char = v.text[0]

                val textView = TextView(this)
                textView.text = PASSWORD_DOT
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PASSWORD_TEXT_SIZE);
                binding.authPassword.addView(textView)
                textView.layoutParams.apply {
                    width = WRAP_CONTENT
                    height = WRAP_CONTENT
                    textView.layoutParams = this
                    textView.requestLayout()
                }
                textView.scaleX = 0f
                textView.scaleY = 0f

                textView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(32)
                    .start()

                symbols[textView] = char
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            100, VibrationEffect
                                .DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
                if (symbols.size == maxLength) {
                    val password = symbols.values.joinToString("")
                    if (password == globalPassword) {
                        authenticated = true
                        finish()
                    } else {
                        clear(binding.authClear)
                    }
                }
            }
        }

        binding.auth1.setOnClickListener(listener)
        binding.auth2.setOnClickListener(listener)
        binding.auth3.setOnClickListener(listener)
        binding.auth4.setOnClickListener(listener)
        binding.auth5.setOnClickListener(listener)
        binding.auth6.setOnClickListener(listener)
        binding.auth7.setOnClickListener(listener)
        binding.auth8.setOnClickListener(listener)
        binding.auth9.setOnClickListener(listener)

        binding.authClear.setOnClickListener(clear)

        binding.authErase.setOnClickListener {
            val key = symbols.entries.lastOrNull()?.key
            key
                ?.animate()
                ?.scaleX(0f)
                ?.scaleY(0f)
                ?.setDuration(125)
                ?.withEndAction {
                    binding.authPassword.removeView(key)
                    symbols.remove(key)
                }
                ?.start()
        }
    }
}