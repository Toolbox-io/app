package ru.morozovit.ultimatesecurity.unlockprotection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RadioButton
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.ui.makeSwitchCard
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.currentCustomAlarm
import ru.morozovit.ultimatesecurity.databinding.AlarmSettingsBinding
import java.io.File

class AlarmSettingsActivity: AppCompatActivity() {
    private lateinit var binding: AlarmSettingsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>
    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        binding = AlarmSettingsBinding.inflate(layoutInflater)
        activityLauncher = registerActivityForResult(this)
        setContentView(binding.root)

        makeSwitchCard(binding.upActionsAlarmSwitchCard, binding.upActionsAlarmSwitch)
        binding.upActionsAlarmTb.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.upActionsAlarmSwitch.isChecked = Settings.UnlockProtection.Actions.alarm
        binding.upActionsAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.UnlockProtection.Actions.alarm = isChecked
        }

        binding.upActionsAlarmAlarm.setOnClickListener {
            val afd: AssetFileDescriptor = assets.openFd("alarm.mp3")
            mediaPlayer.apply {
                if (mediaPlayer.isPlaying) stop()
                reset()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                start()
            }
            currentCustomAlarm = ""
        }
        binding.upActionsAlarmAlarm.isChecked = currentCustomAlarm == ""

        fun createRadiobutton(item: Uri): RadioButton {
            val radiobutton = RadioButton(this)
            radiobutton.text = File(item.path!!).absolutePath
            val padding = resources.getDimensionPixelSize(R.dimen.padding)
            radiobutton.setPadding(padding, 0, padding, 0)
            binding.upActionsAlarmAlarms.addView(radiobutton)
            radiobutton.layoutParams.let {
                it.width = MATCH_PARENT
                it.height = WRAP_CONTENT
                radiobutton.layoutParams = it
            }
            radiobutton.isChecked = "$item" == currentCustomAlarm

            radiobutton.setOnClickListener {
                mediaPlayer.apply {
                    if (mediaPlayer.isPlaying) stop()
                    reset()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setDataSource(applicationContext, item)
                    prepare()
                    start()
                }
                currentCustomAlarm = "$item"
            }
            binding.upActionsAlarmAlarms.requestLayout()
            return radiobutton
        }

        fun createRadiobutton(item: String) {
            createRadiobutton(Uri.parse(item))
        }

        binding.upActionsAlarmAdd.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("audio/*")
            activityLauncher.launch(intent) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val uri = it.data?.data
                    if (uri != null) {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        val set = Settings.UnlockProtection.Actions.customAlarms.toMutableSet()
                        if (set.add("$uri")) {
                            Settings.UnlockProtection.Actions.customAlarms = set.toSet()
                            createRadiobutton(uri).isChecked = true
                        }
                    }
                }
            }
        }
        for (item in Settings.UnlockProtection.Actions.customAlarms) {
            createRadiobutton(item)
        }

        binding.upActionsAlarmClear.setOnClickListener {
            binding.upActionsAlarmAlarm.isChecked = true
            binding.upActionsAlarmAlarms.removeAllViews()
            binding.upActionsAlarmAlarms.addView(binding.upActionsAlarmAlarm)
            Settings.UnlockProtection.Actions.customAlarms = emptySet()
        }
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        setResult(if (binding.upActionsAlarmSwitch.isChecked) 1 else 2)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.clear()
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.clear()
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
    }
}