package io.toolbox.ui.protection.applocker

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ImagesearchRoller
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.postDelayed
import io.toolbox.IssueReporter
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import io.toolbox.Settings.Applocker.UnlockMode.PRESS_TITLE
import io.toolbox.Settings.Applocker.showMode
import io.toolbox.Settings.Applocker.unlockMode
import io.toolbox.services.Accessibility
import io.toolbox.services.Accessibility.Companion.returnBack
import io.toolbox.services.AccessibilityKeeperService
import io.toolbox.ui.AuthActivity
import io.toolbox.ui.MainActivity
import io.toolbox.ui.TopBarType
import ru.morozovit.android.utils.homeScreen
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SimpleAlertDialog
import ru.morozovit.android.utils.ui.SwitchCard
import ru.morozovit.android.utils.ui.SwitchListItem
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll
import java.lang.Thread.sleep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplockerScreen(topBar: TopBarType, scrollBehavior: TopAppBarScrollBehavior) {
    with (LocalContext() as MainActivity) {
        WindowInsetsHandler {
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                topBar = { topBar(scrollBehavior) }
            ) { innerPadding ->
                Column(
                    Modifier
                        .verticalScroll()
                        .padding(innerPadding)
                ) {
                    var unlockMethodText by remember {
                        mutableStateOf(
                            Settings.Applocker.getUnlockModeDescription(unlockMode, this@with.resources)
                        )
                    }

                    var showModeText by remember {
                        mutableStateOf(
                            Settings.Applocker.getShowModeDescription(showMode, this@with.resources)
                        )
                    }

                    if (!Accessibility.running) Settings.Applocker.enabled = false

                    @Composable
                    fun <T> RadioButtonList(
                        options: List<T>,
                        selectedOption: T,
                        onOptionSelected: (T) -> Unit,
                        optionText: (T) -> String
                    ) {
                        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                        Column(Modifier.selectableGroup()) {
                            options.forEach { option ->
                                val selected = option == selectedOption

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .selectable(
                                            selected = selected,
                                            onClick = { onOptionSelected(option) },
                                            role = Role.RadioButton,
                                        )
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null
                                    )
                                    Text(
                                        text = optionText(option),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main switch
                    var mainSwitch by remember {
                        mutableStateOf(
                            if (!Accessibility.running)
                                false
                            else
                                Settings.Applocker.enabled
                        )
                    }

                    // Unlock method dialog
                    var openUnlockMethodDialog by remember { mutableStateOf(false) }
                    if (openUnlockMethodDialog) {
                        fun onDismissRequest() {
                            openUnlockMethodDialog = false
                        }
                        Dialog(
                            onDismissRequest = ::onDismissRequest
                        ) {
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.unlockmethod),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp)
                                    )

                                    val (selectedOption, onOptionSelected) = remember {
                                        mutableIntStateOf(unlockMode)
                                    }

                                    RadioButtonList(
                                        options = buildList {
                                            if (Build.VERSION.SDK_INT >= 28) {
                                                add(LONG_PRESS_APP_INFO)
                                                add(LONG_PRESS_CLOSE)
                                            } else {
                                                add(LONG_PRESS_OPEN_APP_AGAIN)
                                            }
                                            add(LONG_PRESS_TITLE)
                                            add(PRESS_TITLE)
                                        },
                                        selectedOption = selectedOption,
                                        onOptionSelected = onOptionSelected,
                                        optionText = { Settings.Applocker.getUnlockModeDescription(it, resources) }
                                    )

                                    Row(Modifier.padding(24.dp, 16.dp, 24.dp, 24.dp)) {
                                        TextButton(onClick = ::onDismissRequest) {
                                            Text(text = stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                unlockMode = selectedOption
                                                unlockMethodText = Settings.Applocker.getUnlockModeDescription(
                                                    selectedOption, resources
                                                )
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(text = stringResource(R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Show mode dialog
                    var openShowModeDialog by remember { mutableStateOf(false) }
                    if (openShowModeDialog) {
                        fun onDismissRequest() {
                            openShowModeDialog = false
                        }
                        Dialog(
                            onDismissRequest = ::onDismissRequest
                        ) {
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.showmode),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 16.dp)
                                    )

                                    val (selectedOption, onOptionSelected) = remember {
                                        mutableStateOf(showMode)
                                    }

                                    RadioButtonList(
                                        options = Settings.Applocker.ShowMode.entries.toList(),
                                        selectedOption = selectedOption,
                                        onOptionSelected = onOptionSelected,
                                        optionText = { Settings.Applocker.getShowModeDescription(it, resources) }
                                    )

                                    Row(modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 24.dp)) {
                                        TextButton(
                                            onClick = ::onDismissRequest
                                        ) {
                                            Text(text = stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                showMode = selectedOption
                                                showModeText = Settings.Applocker.getShowModeDescription(
                                                    selectedOption, resources
                                                )
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(text = stringResource(R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Accessibility permission dialog
                    var openPermissionDialog by remember { mutableStateOf(false) }
                    fun onPermissionDialogDismiss() {
                        openPermissionDialog = false
                    }
                    SimpleAlertDialog(
                        open = openPermissionDialog,
                        onDismissRequest = ::onPermissionDialogDismiss,
                        title = stringResource(R.string.permissions_required),
                        body = stringResource(R.string.al_permissions),
                        positiveButtonText = stringResource(R.string.ok),
                        onPositiveButtonClick = {
                            startActivity(
                                Intent(ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = FLAG_ACTIVITY_NEW_TASK
                                    var handler: (() -> Unit)? = null
                                    handler = {
                                        if (Accessibility.running) {
                                            mainSwitch = true
                                            Settings.Applocker.enabled = true
                                            Settings.Applocker.used = true
                                        }
                                        returnBack = false
                                        resumeHandlers.remove(handler)
                                    }
                                    resumeHandlers.add(handler)
                                    returnBack = true
                                }
                            )
                        },
                        negativeButtonText = stringResource(R.string.cancel),
                        onNegativeButtonClick = ::onPermissionDialogDismiss
                    )

                    // Foreground service switch
                    var afsSwitch by remember { mutableStateOf(Settings.UnlockProtection.fgServiceEnabled) }

                    Column {
                        SwitchCard(
                            text = stringResource(R.string.enable),
                            checked = mainSwitch,
                            onCheckedChange = sw@ {
                                if (it && !Accessibility.running) {
                                    openPermissionDialog = true
                                } else {
                                    mainSwitch = it
                                    Settings.Applocker.enabled = it
                                    Settings.Applocker.used = false
                                }
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Category(title = stringResource(R.string.settings)) {
                            ListItem(
                                headline = stringResource(R.string.select_apps),
                                supportingText = stringResource(R.string.select_apps_d),
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@with,
                                            SelectAppsActivity::class.java
                                        )
                                    )
                                },
                                materialDivider = true,
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Apps,
                                        contentDescription = null
                                    )
                                }
                            )
                            SwitchListItem(
                                headline = stringResource(R.string.afs),
                                supportingText = stringResource(R.string.afs_d),
                                checked = afsSwitch,
                                onCheckedChange = {
                                    afsSwitch = it
                                    Settings.UnlockProtection.fgServiceEnabled = it
                                    if (it) {
                                        AccessibilityKeeperService.start(this@with)
                                    } else {
                                        AccessibilityKeeperService.stop()
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.SettingsApplications,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        Category(title = stringResource(R.string.security)) {
                            ListItem(
                                headline = stringResource(R.string.setpassword),
                                supportingText = stringResource(R.string.setpassword_d),
                                onClick = {
                                    startActivity(
                                        Intent(this@with, AuthActivity::class.java).apply {
                                            putExtra("setStarted", true)
                                            putExtra("mode", 1)
                                            putExtra("applocker", true)
                                            flags = FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                },
                                materialDivider = true,
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Password,
                                        contentDescription = null
                                    )
                                }
                            )
                            ListItem(
                                headline = stringResource(R.string.unlockmethod),
                                supportingText = unlockMethodText,
                                onClick = {
                                    openUnlockMethodDialog = true
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Key,
                                        contentDescription = null
                                    )
                                },
                                materialDivider = true
                            )
                            ListItem(
                                headline = stringResource(R.string.unlock_duration),
                                supportingText = stringResource(R.string.unlock_duration_d),
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null
                                    )
                                }
                            ) {
                                Column(Modifier.padding(horizontal = 16.dp)) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val tooltipState = rememberTooltipState(isPersistent = true)
                                    val sliderState =
                                        rememberSliderState(
                                            value = remember { Settings.Applocker.unlockDuration }.toFloat(),
                                            steps = 9,
                                            valueRange = 0f..10f
                                        ).also {
                                            it.onValueChangeFinished = {
                                                Settings.Applocker.unlockDuration = it.value.toInt()
                                            }
                                        }

                                    LaunchedEffect(sliderState.isDragging) {
                                        if (sliderState.isDragging) {
                                            tooltipState.show()
                                        } else {
                                            tooltipState.dismiss()
                                        }
                                    }

                                    Slider(
                                        state = sliderState,
                                        interactionSource = interactionSource,
                                        thumb = {
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                                    TooltipAnchorPosition.Above,
                                                    4.dp
                                                ),
                                                tooltip = {
                                                    PlainTooltip {
                                                        Text(
                                                            when (sliderState.value.toInt()) {
                                                                0 -> stringResource(R.string.instant)
                                                                10 -> stringResource(R.string.until_screen_locked)
                                                                else -> sliderState.value.toInt().let {
                                                                    pluralStringResource(R.plurals.minutes, it, it)
                                                                }
                                                            }
                                                        )
                                                    }
                                                },
                                                state = tooltipState
                                            ) {
                                                SliderDefaults.Thumb(interactionSource = interactionSource)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Category(title = stringResource(R.string.customization)) {
                            ListItem(
                                headline = stringResource(R.string.showmode),
                                supportingText = showModeText,
                                onClick = {
                                    openShowModeDialog = true
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.ImagesearchRoller,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        Category(title = stringResource(R.string.testing)) {
                            ListItem(
                                headline = stringResource(R.string.test_crash),
                                supportingText = stringResource(R.string.test_crash_d),
                                onClick = {
                                    IssueReporter.enabled = false
                                    Handler(Looper.getMainLooper()).postDelayed(500) {
                                        IssueReporter.enabled = true
                                    }
                                    @Suppress("DIVISION_BY_ZERO")
                                    0 / 0
                                },
                                materialDivider = true,
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = null
                                    )
                                }
                            )
                            ListItem(
                                headline = stringResource(R.string.test_fake_crash),
                                supportingText = stringResource(R.string.test_fake_crash_d),
                                onClick = {
                                    this@with.homeScreen()

                                    sleep(500)

                                    finish()

                                    val intent = Intent(this@with, FakeCrashActivity::class.java)
                                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                                    val b = Bundle()
                                    b.putString("appPackage", this@with.packageName)
                                    startActivity(intent)
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}