package ru.morozovit.ultimatesecurity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.homeScreen
import ru.morozovit.ultimatesecurity.App.Companion.authenticated
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.databinding.AuthActivityBinding


class AuthActivity: BaseActivity(false) {
    companion object {
        private const val PASSWORD_DOT = "●"
        private const val PASSWORD_TEXT_SIZE = 30f
        const val MAX_PASSWORD_LENGTH = 6

        const val MODE_NONE = -1
        const val MODE_ENTER = 0
        const val MODE_SET = 1
        const val MODE_CONFIRM = 2
        const val MODE_ENTER_OLD_PW = 3

        var started = false
    }

    private lateinit var binding: AuthActivityBinding
    private val symbols = mutableMapOf<TextView, Char>()

    private var mode
        inline get() = intent.getIntExtra("mode", MODE_ENTER)
        inline set(value) {intent.putExtra("mode", value)}
    private val isSetOrConfirm inline get() = mode in 1..3
    private val enteredPassword inline get() =
        intent.getStringExtra("password") ?:
        throw NullPointerException("Password must be set")
    private val oldPwConfirmed inline get() = intent.getBooleanExtra("oldPwConfirmed", false)
    private val setStarted inline get() = intent.getBooleanExtra("setStarted", false)

    private var confirm = false

    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((globalPassword == "" || authenticated) && !isSetOrConfirm) {
            finish()
            return
        }
        if (setStarted) started = true

        if (!isSetOrConfirm && !isSplashScreenVisible)
            overridePendingTransition(R.anim.alpha_up, R.anim.scale_up)

        if (isSplashScreenVisible) overridePendingTransition()

        binding = AuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityLauncher = registerActivityForResult(this)

        if (isSetOrConfirm) {
            if (mode == MODE_SET && globalPassword != "" && !oldPwConfirmed) {
                intent.putExtra("mode", MODE_ENTER_OLD_PW)
                assert(mode == MODE_ENTER_OLD_PW)
            }

            binding.authLabel.text = resources.getString(
                if (mode == MODE_SET)
                    if (globalPassword == "")
                        R.string.setpassword
                    else
                        R.string.change_password
                else
                    if (mode == MODE_ENTER_OLD_PW)
                        R.string.enter_old_password
                    else
                        R.string.confirm_password
            )
            binding.authNav.visibility = VISIBLE
            binding.authNav.setNavigationOnClickListener {onBackPressed()}
        }

        val clear = { _: View ->
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
        val listener = { v: View ->
            if (symbols.size < MAX_PASSWORD_LENGTH) {
                v as Button
                val char = v.text[0]

                val textView = TextView(this)
                textView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        textView.apply {
                            scaleX = 0f
                            scaleY = 0f
                            pivotX = textView.width / 2f
                            pivotY = textView.height / 2f
                            animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(125)
                                .start()
                        }
                        textView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
                textView.text = PASSWORD_DOT
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PASSWORD_TEXT_SIZE);
                binding.authPassword.addView(textView)
                textView.layoutParams.apply {
                    width = WRAP_CONTENT
                    height = WRAP_CONTENT
                    textView.layoutParams = this
                    textView.requestLayout()
                }

                symbols[textView] = char

                vibrate(100)

                if (symbols.size == MAX_PASSWORD_LENGTH) {
                    val password = symbols.values.joinToString("")
                    if (password == globalPassword && !isSetOrConfirm) {
                        authenticated = true
                        finishAfterTransition(R.anim.scale_down, R.anim.alpha_down)
                    } else if (mode == MODE_ENTER_OLD_PW && password == globalPassword) {
                        startActivity(Intent(this, AuthActivity::class.java).apply {
                            putExtra("mode", MODE_SET)
                            putExtra("oldPwConfirmed", true)
                            putExtra("noAnim", true)
                        })
                        finish()
                    } else if (mode == MODE_SET) {
                        confirm = true
                        activityLauncher.launch(Intent(this, AuthActivity::class.java).apply {
                            putExtra("mode", MODE_CONFIRM)
                            putExtra("password", password)
                            putExtra("noAnim", true)
                        }) {
                            if (it.resultCode == RESULT_OK) {
                                setResult(RESULT_OK)
                                finish()
                            }
                        }
                        clear(binding.authClear)
                    } else if (
                        mode == MODE_CONFIRM &&
                        password == enteredPassword
                    ) {
                        globalPassword = password
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val duration = 70L
                        binding.authPassword
                            .animate()
                            .translationX(-10f)
                            .setDuration(duration)
                            .withEndAction {
                                binding.authPassword
                                    .animate()
                                    .translationX(15f)
                                    .setDuration(duration)
                                    .withEndAction {
                                        binding.authPassword
                                            .animate()
                                            .translationX(-20f)
                                            .setDuration(duration)
                                            .withEndAction {
                                                binding.authPassword
                                                    .animate()
                                                    .translationX(15f)
                                                    .setDuration(duration)
                                                    .withEndAction {
                                                        binding.authPassword
                                                            .animate()
                                                            .translationX(-10f)
                                                            .setDuration(duration)
                                                            .withEndAction {
                                                                binding.authPassword
                                                                    .animate()
                                                                    .translationX(10f)
                                                                    .setDuration(duration)
                                                                    .withEndAction {
                                                                        binding.authPassword
                                                                            .animate()
                                                                            .translationX(10f)
                                                                            .setDuration(duration)
                                                                            .withEndAction {
                                                                                clear(binding.authClear)
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                            }
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

        startEnterAnimation(binding.root)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (mode == MODE_ENTER) {
            homeScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun finish() {
        super.finish()
        mode = MODE_NONE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isSetOrConfirm) recreate()
    }
}