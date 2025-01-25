package ru.morozovit.ultimatesecurity.ui.protection.unlockprotection

import android.app.Activity
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.Button
import ru.morozovit.android.RadioButtonWithText
import ru.morozovit.android.RadioGroup
import ru.morozovit.android.SwipeToDismissBackground
import ru.morozovit.android.SwitchCard
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.currentCustomAlarm
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.Actions.customAlarms
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import java.io.File

class AlarmSettingsActivity: BaseActivity() {
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @PhonePreview
    fun AlarmSettingsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

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
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val mediaPlayer = remember { MediaPlayer() }
                    DisposableEffect(Unit) {
                        onDispose {
                            runCatching {
                                mediaPlayer.stop()
                                mediaPlayer.release()
                            }
                        }
                    }
                    addPauseCallback {
                        mediaPlayer.stop()
                    }

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

                    val toRemove = mutableListOf<String>()
                    data class Alarm(
                        val label: String,
                        val onClick: () -> Unit,
                        val onRemove: (() -> Unit)?
                    ) {
                        var visible = mutableStateOf(true)

                        operator fun component4() = visible
                    }

                    val alarm = stringResource(R.string.default_alarm)
                    val unknown = stringResource(R.string.unknown)
                    val alarm_deleted = stringResource(R.string.alarm_deleted)
                    val cancel = stringResource(R.string.cancel)

                    var isSizeAnimationEnabled by remember { mutableStateOf(true) }

                    val radioOptions = remember {
                        mutableStateListOf(
                            Alarm(
                                alarm,
                                {
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
                                },
                                null
                            )
                        )
                    }
                    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0].label) }

                    fun createRadioButton(
                        item: Uri,
                        checked: Boolean = "$item" == currentCustomAlarm
                    ) {
                        try {
                            contentResolver.openInputStream(item)!!.apply {
                                read()
                                close()
                            }
                        } catch (e: Exception) {
                            toRemove.add("$item")
                        }
                        val text =
                            if (item.path != null)
                                File(item.path!!).absolutePath
                            else
                                unknown

                        var alarmCreated: Alarm? = null
                        alarmCreated = Alarm(
                            text,
                            {
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
                            },
                            {
                                currentCustomAlarm = ""
                                onOptionSelected(alarm)
                                customAlarms =
                                    customAlarms
                                        .toMutableSet()
                                        .also {
                                            it.remove("$item")
                                        }
                                        .toSet()
                                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                scope.launch {
                                    val result = snackbarHostState
                                        .showSnackbar(
                                            message = alarm_deleted,
                                            actionLabel = cancel,
                                            // Defaults to SnackbarDuration.Short
                                            duration = SnackbarDuration.Long
                                        )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            customAlarms =
                                                customAlarms
                                                    .toMutableSet()
                                                    .also {
                                                        it.add("$item")
                                                    }
                                                    .toSet()
                                            currentCustomAlarm = "$item"
                                            onOptionSelected(text)
                                            isSizeAnimationEnabled = true
                                            alarmCreated!!.visible.value = true
                                        }

                                        SnackbarResult.Dismissed -> {
                                            radioOptions.remove(alarmCreated)
                                        }
                                    }
                                }
                                Unit
                            }
                        )

                        radioOptions.add(alarmCreated)
                        if (checked) {
                            onOptionSelected(text)
                        }
                    }

                    Column(
                        Modifier.let {
                            if (isSizeAnimationEnabled) it.animateContentSize()
                            else it
                        }
                    ) {
                        LaunchedEffect(Unit) {
                            for (i in customAlarms) {
                                createRadioButton(Uri.parse(i))
                            }
                            if (customAlarms.isEmpty()) {
                                currentCustomAlarm = ""
                                onOptionSelected(alarm)
                            }
                            if (toRemove.isNotEmpty()) {
                                val alarms = customAlarms.toMutableList()
                                for (item in toRemove) {
                                    if (currentCustomAlarm == item) {
                                        currentCustomAlarm = ""
                                        onOptionSelected(alarm)
                                    }
                                    alarms.remove(item)
                                }
                                customAlarms = alarms.toSet()
                            }
                        }

                        @Composable
                        fun SwipeToDismissRadioButton(
                            text: String,
                            selected: Boolean,
                            onSelectedChange: () -> Unit,
                            dismissCallback: (() -> Unit)?,
                            visible: MutableState<Boolean>
                        ) {
                            if (dismissCallback != null) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        when (it) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                dismissCallback()
                                            }

                                            SwipeToDismissBoxValue.EndToStart -> {
                                                dismissCallback()
                                            }

                                            SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
                                        }
                                        isSizeAnimationEnabled = false
                                        visible.value = false
                                        return@rememberSwipeToDismissBoxState true
                                    },
                                    // positional threshold of 25%
                                    positionalThreshold = { it * .25f }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        SwipeToDismissBackground(
                                            dismissState = dismissState,
                                            endToStartColor = Color(0xFFFF1744),
                                            endToStartIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete"
                                                )
                                            }
                                        )
                                    },
                                    content = {
                                        Surface {
                                            AnimatedVisibility(
                                                visible = visible.value,
                                                exit = shrinkVertically(),
                                                enter = slideInHorizontally() + fadeIn() + expandVertically()
                                            ) {
                                                RadioButtonWithText(
                                                    selected = selected,
                                                    onSelectedChange = onSelectedChange
                                                ) {
                                                    Text(text)
                                                }
                                            }
                                        }
                                    }
                                )
                            } else {
                                RadioButtonWithText(
                                    selected = selected,
                                    onSelectedChange = onSelectedChange
                                ) {
                                    Text(text)
                                }
                            }
                        }

                        RadioGroup {
                            radioOptions.forEach { (text, callback, dismissCallback, visible) ->
                                SwipeToDismissRadioButton(
                                    selected = (text == selectedOption),
                                    onSelectedChange = {
                                        onOptionSelected(text)
                                        callback()
                                    },
                                    text = text,
                                    dismissCallback = dismissCallback,
                                    visible = visible
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.setType("audio/*")
                                activityLauncher.launch(intent) {
                                    if (it.resultCode == Activity.RESULT_OK) {
                                        val uri = it.data?.data
                                        if (uri != null) {
                                            contentResolver.takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            )
                                            val set = customAlarms.toMutableSet()
                                            if (set.add("$uri")) {
                                                customAlarms = set.toSet()
                                                createRadioButton(uri, true)
                                            } else {
                                                Toast.makeText(
                                                    this@AlarmSettingsActivity,
                                                    R.string.smthwentwrong,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(Icons.Filled.Add, null)
                            }
                        ) {
                            Text(stringResource(R.string.add))
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLauncher = registerActivityForResult(this)
        enableEdgeToEdge()
        setContent {
            AlarmSettingsScreen()
        }
    }
}