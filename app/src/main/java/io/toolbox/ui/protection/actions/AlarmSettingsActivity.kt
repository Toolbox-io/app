package io.toolbox.ui.protection.actions

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.net.toUri
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.ui.AppTheme
import kotlinx.coroutines.launch
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.copy
import ru.morozovit.android.getFileName
import ru.morozovit.android.test
import ru.morozovit.android.ui.Button
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.RadioButtonWithText
import ru.morozovit.android.ui.RadioGroup
import ru.morozovit.android.ui.SwipeToDismissBackground
import ru.morozovit.android.ui.SwitchCard
import ru.morozovit.android.verticalScroll

class AlarmSettingsActivity: BaseActivity() {
    private lateinit var activityLauncher: ActivityLauncher

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AlarmSettingsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
                                    contentDescription = stringResource(R.string.back)
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
                        .verticalScroll(),
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

                    var mainSwitch by remember {
                        mutableStateOf(
                            Settings.Actions.Alarm.enabled
                        )
                    }

                    SwitchCard(
                        text = stringResource(R.string.enable),
                        checked = mainSwitch,
                        onCheckedChange = sw@ {
                            mainSwitch = it
                            Settings.Actions.Alarm.enabled = it
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val toRemove = mutableListOf<String>()

                    data class Alarm(
                        val label: String,
                        val onClick: () -> Unit,
                        val onRemove: (() -> Unit)?
                    ) {
                        var visible = mutableStateOf(true)

                        @Suppress("unused", "RedundantSuppression")
                        operator fun component4() = visible
                    }

                    val alarm = stringResource(R.string.default_alarm)
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
                                    Settings.Actions.Alarm.current = ""
                                },
                                null
                            )
                        )
                    }
                    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0].label) }

                    fun createRadioButton(
                        item: Uri,
                        checked: Boolean = "$item" == Settings.Actions.Alarm.current
                    ) {
                        if (!contentResolver.test(item)) {
                            toRemove.add("$item")
                        }
                        val text = getFileName(item)

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
                                Settings.Actions.Alarm.current = "$item"
                            },
                            {
                                Settings.Actions.Alarm.current = ""
                                onOptionSelected(alarm)
                                Settings.Actions.Alarm.customAlarms =
                                    Settings.Actions.Alarm.customAlarms
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
                                            Settings.Actions.Alarm.customAlarms =
                                                Settings.Actions.Alarm.customAlarms
                                                    .toMutableSet()
                                                    .also {
                                                        it.add("$item")
                                                    }
                                                    .toSet()
                                            Settings.Actions.Alarm.current = "$item"
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

                    Category(
                        margin = CategoryDefaults.margin.copy(bottom = 16.dp)
                    ) {
                        Column(
                            Modifier.let {
                                if (isSizeAnimationEnabled) it.animateContentSize()
                                else it
                            }
                        ) {
                            LaunchedEffect(Unit) {
                                for (i in Settings.Actions.Alarm.customAlarms) {
                                    createRadioButton(i.toUri())
                                }
                                if (Settings.Actions.Alarm.customAlarms.isEmpty()) {
                                    Settings.Actions.Alarm.current = ""
                                    onOptionSelected(alarm)
                                }
                                if (toRemove.isNotEmpty()) {
                                    val alarms = Settings.Actions.Alarm.customAlarms.toMutableList()
                                    for (item in toRemove) {
                                        if (Settings.Actions.Alarm.current == item) {
                                            Settings.Actions.Alarm.current = ""
                                            onOptionSelected(alarm)
                                        }
                                        alarms.remove(item)
                                    }
                                    Settings.Actions.Alarm.customAlarms = alarms.toSet()
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
                                    @Suppress("DEPRECATION")
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
                                                        contentDescription = stringResource(R.string.delete)
                                                    )
                                                }
                                            )
                                        },
                                        content = {
                                            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
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
                    }

                    Row(
                        Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 10.dp
                        )
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.type = "audio/*"
                                activityLauncher.launch(intent) {
                                    if (it.resultCode == RESULT_OK) {
                                        val uri = it.data?.data
                                        if (uri != null) {
                                            contentResolver.takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            )
                                            val set = Settings.Actions.Alarm.customAlarms.toMutableSet()
                                            if (set.add("$uri")) {
                                                Settings.Actions.Alarm.customAlarms = set.toSet()
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
        activityLauncher = activityResultLauncher
        enableEdgeToEdge()
        setContent {
            AlarmSettingsScreen()
        }
    }
}