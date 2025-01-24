package ru.morozovit.ultimatesecurity.ui.protection.unlockprotection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.RadioButtonWithText
import ru.morozovit.android.RadioGroup
import ru.morozovit.android.SwitchCard
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.currentCustomAlarm
import ru.morozovit.ultimatesecurity.databinding.AlarmSettingsBinding
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview

// TODO rewrite in Jetpack Compose
class AlarmSettingsActivity: BaseActivity() {
    private lateinit var binding: AlarmSettingsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @PhonePreview
    fun AlarmSettingsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.alarm),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Localized description"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val mediaPlayer = remember { MediaPlayer() }

                    fun cleanup() {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                    DisposableEffect(Unit) {
                        onDispose(::cleanup)
                    }
                    addPauseCallback(::cleanup)

                    val (valueOrFalse) = previewUtils()

                    var mainSwitch by remember {
                        mutableStateOf(
                            valueOrFalse {
                                Settings.UnlockProtection.Actions.alarm
                            }
                        )
                    }

                    val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@ {
                        mainSwitch = it
                        Settings.UnlockProtection.Actions.alarm = it
                    }

                    SwitchCard(
                        text = stringResource(R.string.enable),
                        checked = mainSwitch,
                        onCheckedChange = mainSwitchOnCheckedChange,
                        cardOnClick = {
                            mainSwitchOnCheckedChange(!mainSwitch)
                        }
                    )
                    HorizontalDivider()
                    val alarm = stringResource(R.string.default_alarm)

                    val radioOptions = remember {
                        mutableStateListOf(
                            alarm to {
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
                        )
                    }

                    // TODO load add user-added alarms

                    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0].first) }
                    RadioGroup {
                        radioOptions.forEach { (text, callback) ->
                            RadioButtonWithText(
                                selected = (text == selectedOption),
                                onSelectedChange = {
                                    onOptionSelected(text)
                                    callback()
                                }
                            ) {
                                Text(text)
                            }
                        }
                    }

                    // TODO add alarm button
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AlarmSettingsScreen()
        }

//        binding = AlarmSettingsBinding.inflate(layoutInflater)
//        activityLauncher = registerActivityForResult(this)
//        setContentView(binding.root)
//
//        makeSwitchCard(binding.upActionsAlarmSwitchCard, binding.upActionsAlarmSwitch)
//        binding.upActionsAlarmTb.setNavigationOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
//        }
//        binding.upActionsAlarmSwitch.isChecked = Settings.UnlockProtection.Actions.alarm
//        binding.upActionsAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
//            Settings.UnlockProtection.Actions.alarm = isChecked
//        }
//
//        binding.upActionsAlarmAlarm.setOnClickListener {
//            val afd: AssetFileDescriptor = assets.openFd("alarm.mp3")
//            mediaPlayer.apply {
//                if (mediaPlayer.isPlaying) stop()
//                reset()
//                setAudioAttributes(
//                    AudioAttributes.Builder()
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .setUsage(AudioAttributes.USAGE_ALARM)
//                        .build()
//                )
//                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
//                prepare()
//                start()
//            }
//            currentCustomAlarm = ""
//        }
//
//        fun createRadiobutton(item: Uri): RadioButton? {
//            try {
//                contentResolver.openInputStream(item)!!.apply {
//                    read()
//                    close()
//                }
//            } catch (e: Exception) {
//                return null
//            }
//            val radiobutton = RadioButton(this)
//            radiobutton.text = File(item.path!!).absolutePath
//            val padding = resources.getDimensionPixelSize(R.dimen.padding)
//            radiobutton.setPadding(padding, 0, padding, 0)
//            binding.upActionsAlarmAlarms.addView(radiobutton)
//            radiobutton.layoutParams.let {
//                it.width = MATCH_PARENT
//                it.height = WRAP_CONTENT
//                radiobutton.layoutParams = it
//            }
//            radiobutton.isChecked = "$item" == currentCustomAlarm
//
//            radiobutton.setOnClickListener {
//                mediaPlayer.apply {
//                    if (mediaPlayer.isPlaying) stop()
//                    reset()
//                    setAudioAttributes(
//                        AudioAttributes.Builder()
//                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                            .setUsage(AudioAttributes.USAGE_ALARM)
//                            .build()
//                    )
//                    setDataSource(applicationContext, item)
//                    prepare()
//                    start()
//                }
//                currentCustomAlarm = "$item"
//            }
//            binding.upActionsAlarmAlarms.requestLayout()
//            return radiobutton
//        }
//
//        fun createRadiobutton(item: String) = createRadiobutton(Uri.parse(item))
//
//        binding.upActionsAlarmAdd.setOnClickListener {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//            intent.addCategory(Intent.CATEGORY_OPENABLE)
//            intent.setType("audio/*")
//            activityLauncher.launch(intent) {
//                if (it.resultCode == Activity.RESULT_OK) {
//                    val uri = it.data?.data
//                    if (uri != null) {
//                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                        val set = customAlarms.toMutableSet()
//                        if (set.add("$uri")) {
//                            customAlarms = set.toSet()
//                            createRadiobutton(uri)!!.isChecked = true
//                        }
//                    }
//                }
//            }
//        }
//        val toRemove = mutableListOf<String>()
//        for (item in customAlarms) {
//            if (createRadiobutton(item) == null) {
//                toRemove.add(item)
//            }
//        }
//        if (customAlarms.isEmpty()) {
//            currentCustomAlarm = ""
//            binding.upActionsAlarmAlarm.isChecked = true
//        }
//        if (toRemove.isNotEmpty()) {
//            val alarms = customAlarms.toMutableList()
//            for (item in toRemove) {
//                if (currentCustomAlarm == item) {
//                    currentCustomAlarm = ""
//                    binding.upActionsAlarmAlarm.isChecked = true
//                }
//                alarms.remove(item)
//            }
//            customAlarms = alarms.toSet()
//        }
//
//        binding.upActionsAlarmClear.setOnClickListener {
//            binding.upActionsAlarmAlarm.isChecked = true
//            binding.upActionsAlarmAlarms.removeAllViews()
//            binding.upActionsAlarmAlarms.addView(binding.upActionsAlarmAlarm)
//            customAlarms = emptySet()
//        }
//        binding.upActionsAlarmAlarm.isChecked = currentCustomAlarm == ""
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        setResult(if (binding.upActionsAlarmSwitch.isChecked) 1 else 2)
        finish()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        if (mediaPlayer.isPlaying) mediaPlayer.stop()
//        mediaPlayer.release()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (mediaPlayer.isPlaying) mediaPlayer.stop()
//    }
}