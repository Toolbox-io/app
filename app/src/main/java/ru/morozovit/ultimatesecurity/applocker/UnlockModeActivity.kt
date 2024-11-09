package ru.morozovit.ultimatesecurity.applocker

import android.os.Bundle
import android.view.Window
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.NOTHING_SELECTED
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.unlockMode
import ru.morozovit.ultimatesecurity.databinding.UnlockModeBinding
import ru.morozovit.android.screenWidth

class UnlockModeActivity: BaseActivity() {
    private lateinit var binding: UnlockModeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = UnlockModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        actionBar?.hide()

        // Set proper width
        binding.alUmR.layoutParams.let {
            it.width = screenWidth
            binding.alUmR.layoutParams = it
        }

        // Cancel
        binding.alUmC.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // OK
        binding.alUmOk.setOnClickListener {
            val value = when (binding.alUmO.checkedRadioButtonId) {
                R.id.al_um_lp_ai -> LONG_PRESS_APP_INFO
                R.id.al_um_lp_c -> LONG_PRESS_CLOSE
                R.id.al_um_lp_t -> LONG_PRESS_TITLE
                R.id.al_um_p_t -> PRESS_TITLE
                else -> NOTHING_SELECTED
            }
            if (value != NOTHING_SELECTED) {
                unlockMode = value
            }
            setResult(RESULT_OK)
            finish()
        }

        val button = when (unlockMode) {
            LONG_PRESS_APP_INFO -> binding.alUmLpAi
            LONG_PRESS_CLOSE -> binding.alUmLpC
            LONG_PRESS_TITLE -> binding.alUmLpT
            PRESS_TITLE -> binding.alUmPT
            else -> throw IllegalArgumentException()
        }
        button.isChecked = true
    }
}
