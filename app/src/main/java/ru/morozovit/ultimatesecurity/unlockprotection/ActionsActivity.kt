package ru.morozovit.ultimatesecurity.unlockprotection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.databinding.UpActionsBinding

class ActionsActivity: AppCompatActivity() {
    private lateinit var binding: UpActionsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        savedInstanceState?.clear()
        binding = UpActionsBinding.inflate(layoutInflater)
        activityLauncher = registerActivityForResult(this)
        setContentView(binding.root)

        binding.upActionsTb.setNavigationOnClickListener {
            finish()
        }

        makeSwitchCard(binding.upActionsNotification, binding.upActionsI3Sw)

        binding.upActionsI1Sw.isChecked = Settings.UnlockProtection.Actions.alarm
        binding.upActionsI1Sw.setOnCheckedChangeListener { _, isChecked ->
            Settings.UnlockProtection.Actions.alarm = isChecked
        }
        binding.upActionsI1Hc.setOnClickListener {
            val intent = Intent(this, AlarmSettingsActivity::class.java)
            activityLauncher.launch(intent) {
                binding.upActionsI1Sw.isChecked = it.resultCode == 1
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.clear()
    }
}