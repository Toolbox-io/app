package ru.morozovit.ultimatesecurity.unlockprotection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.databinding.UpActionsBinding
import ru.morozovit.ultimatesecurity.unlockprotection.intruderphoto.IntruderPhotoSettingsActivity

class ActionsActivity: BaseActivity() {
    private lateinit var binding: UpActionsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        binding = UpActionsBinding.inflate(layoutInflater)
        activityLauncher = registerActivityForResult(this)
        setContentView(binding.root)

        binding.upActionsTb.setNavigationOnClickListener {
            finish()
        }

        makeSwitchCard(binding.upActionsNotification, binding.upActionsI3Sw)

        // Alarm
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
        // Intruder photo
        binding.upActionsI2Sw.isChecked = Settings.UnlockProtection.Actions.intruderPhoto
        binding.upActionsI2Sw.setOnCheckedChangeListener { _, isChecked ->
            Settings.UnlockProtection.Actions.intruderPhoto = isChecked
        }
        binding.upActionsI2Hc.setOnClickListener {
            val intent = Intent(this, IntruderPhotoSettingsActivity::class.java)
            activityLauncher.launch(intent) {
                binding.upActionsI2Sw.isChecked = it.resultCode == 1
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }
}