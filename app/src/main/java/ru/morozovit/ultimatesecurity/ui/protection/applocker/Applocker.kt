package ru.morozovit.ultimatesecurity.ui.protection.applocker

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.postDelayed
import kotlinx.coroutines.launch
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.homeScreen
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SecureTextField
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.android.ui.SwitchCard
import ru.morozovit.android.ui.SwitchListItem
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.getUnlockModeDescription
import ru.morozovit.ultimatesecurity.Settings.Applocker.unlockMode
import ru.morozovit.ultimatesecurity.Settings.accessibility
import ru.morozovit.ultimatesecurity.crashreporter.IssueReporter
import ru.morozovit.ultimatesecurity.services.Accessibility
import ru.morozovit.ultimatesecurity.services.Accessibility.Companion.waitingForAccessibility
import ru.morozovit.ultimatesecurity.services.AccessibilityKeeperService
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import java.lang.Thread.sleep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplockerScreen(topBar: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = topBar
        ) { innerPadding ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                val context = LocalContext() as MainActivity

                val default = stringResource(R.string.lp_ai)
                var unlockMethodText by remember {
                    mutableStateOf(
                        if (!false)
                            getUnlockModeDescription(unlockMode, context.resources)
                        else
                            default
                    )
                }

                val errorDisablingService = stringResource(R.string.error_disabling_service)
                val settings = stringResource(R.string.settings)

                // Main switch
                var mainSwitch by remember {
                    mutableStateOf(
                        accessibility
                    )
                }

                // Set password dialog
                var openSetPasswordDialog by remember { mutableStateOf(false) }
                if (openSetPasswordDialog) {
                    fun onDismissRequest() {
                        openSetPasswordDialog = false
                    }
                    Dialog(
                        onDismissRequest = ::onDismissRequest
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = stringResource(R.string.setpassword),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .padding()
                                        .padding(bottom = 16.dp)
                                )
                                var oldPasswordHidden by rememberSaveable { mutableStateOf(true) }
                                var oldPassword by rememberSaveable { mutableStateOf("") }
                                var oldPasswordIsError by rememberSaveable { mutableStateOf(true) }

                                if (Settings.Keys.Applocker.isSet) {
                                    fun validate() {
                                        oldPasswordIsError = oldPassword.isEmpty()
                                    }
                                    LaunchedEffect(Unit) {
                                        snapshotFlow { oldPassword }.collect { validate() }
                                    }

                                    SecureTextField(
                                        value = oldPassword,
                                        onValueChange = { oldPassword = it },
                                        label = { Text(stringResource(R.string.old_password)) },
                                        visibilityOnClick = {
                                            oldPasswordHidden = !oldPasswordHidden
                                        },
                                        modifier = Modifier
                                            .padding()
                                            .padding(bottom = 10.dp)
                                            .fillMaxWidth()
                                            .clearFocusOnKeyboardDismiss(),
                                        passwordHidden = oldPasswordHidden,
                                        isError = oldPasswordIsError
                                    )
                                }
                                var newPasswordHidden by rememberSaveable { mutableStateOf(true) }
                                var newPassword by rememberSaveable { mutableStateOf("") }

                                SecureTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text(stringResource(R.string.new_password)) },
                                    visibilityOnClick = {
                                        newPasswordHidden = !newPasswordHidden
                                    },
                                    modifier = Modifier
                                        .padding()
                                        .padding(bottom = 10.dp)
                                        .fillMaxWidth()
                                        .clearFocusOnKeyboardDismiss(),
                                    passwordHidden = newPasswordHidden
                                )

                                var confirmPasswordHidden by rememberSaveable {
                                    mutableStateOf(
                                        true
                                    )
                                }
                                var confirmPassword by rememberSaveable { mutableStateOf("") }

                                SecureTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text(stringResource(R.string.confirm_password)) },
                                    visibilityOnClick = {
                                        confirmPasswordHidden = !confirmPasswordHidden
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clearFocusOnKeyboardDismiss(),
                                    passwordHidden = confirmPasswordHidden
                                )
                                Row(Modifier.padding(top = 24.dp)) {
                                    TextButton(
                                        onClick = ::onDismissRequest
                                    ) {
                                        Text(text = stringResource(R.string.cancel))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            if (Settings.Keys.Applocker.check(oldPassword) && newPassword == confirmPassword) {
                                                Settings.Keys.Applocker.set(newPassword)
                                                onDismissRequest()
                                            } else if (oldPassword.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.old_password_cannot_be_empty,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    R.string.passwords_dont_match,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Text(text = stringResource(R.string.ok))
                                    }
                                }
                            }
                        }
                    }
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
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = stringResource(R.string.unlockmethod),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .padding()
                                        .padding(bottom = 16.dp)
                                )
                                val radioOptions = mutableListOf<Int>()
                                if (Build.VERSION.SDK_INT >= 28) {
                                    radioOptions += LONG_PRESS_APP_INFO
                                    radioOptions += LONG_PRESS_CLOSE
                                } else {
                                    radioOptions += LONG_PRESS_OPEN_APP_AGAIN
                                }
                                radioOptions += LONG_PRESS_TITLE
                                radioOptions += PRESS_TITLE
                                val (selectedOption, onOptionSelected) = remember {
                                    mutableIntStateOf(
                                        radioOptions[
                                            if (false)
                                                0
                                            else
                                                radioOptions.indexOf(unlockMode)
                                        ]
                                    )
                                }
                                // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                                Column(Modifier.selectableGroup()) {
                                    radioOptions.forEach { num ->
                                        val text =
                                            if (!false)
                                                getUnlockModeDescription(num, context.resources)
                                            else
                                                stringResource(R.string.lp_ai)
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .selectable(
                                                    selected = (num == selectedOption),
                                                    onClick = { onOptionSelected(num) },
                                                    role = Role.RadioButton
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (num == selectedOption),
                                                onClick = null // null recommended for accessibility with screenreaders
                                            )
                                            Text(
                                                text = text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        }
                                    }
                                }

                                Row(Modifier.padding(top = 24.dp)) {
                                    TextButton(
                                        onClick = ::onDismissRequest
                                    ) {
                                        Text(text = stringResource(R.string.cancel))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            unlockMode = selectedOption
                                            unlockMethodText = getUnlockModeDescription(
                                                selectedOption, context.resources
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
                        val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = FLAG_ACTIVITY_NEW_TASK
                        context.resumeHandlers.size
                        var handler: (() -> Unit)? = null
                        handler = {
                            if (accessibility) {
                                mainSwitch = true
                            }
                            waitingForAccessibility = false
                            context.resumeHandlers.remove(handler)
                        }
                        context.resumeHandlers.add(handler)
                        waitingForAccessibility = true
                        context.startActivity(intent)
                    },
                    negativeButtonText = stringResource(R.string.cancel),
                    onNegativeButtonClick = ::onPermissionDialogDismiss
                )

                // Main switch
                val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@{
                    if (!false) {
                        if (it) {
                            openPermissionDialog = true
                            return@sw
                        } else {
                            var error = false
                            try {
                                Accessibility.instance!!.disable()
                            } catch (e: Exception) {
                                error = true
                            }

                            if (accessibility || error) {
                                scope.launch {
                                    val result = snackbarHostState
                                        .showSnackbar(
                                            message = errorDisablingService,
                                            actionLabel = settings,
                                            duration = SnackbarDuration.Short
                                        )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            val intent =
                                                Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                            intent.flags = FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        }

                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                                return@sw
                            }
                        }
                    }
                    mainSwitch = it
                }

                // Foreground service switch
                var afsSwitch by remember { mutableStateOf(Settings.UnlockProtection.fgServiceEnabled) }

                Column {
                    SwitchCard(
                        text = stringResource(R.string.enable),
                        checked = mainSwitch,
                        onCheckedChange = mainSwitchOnCheckedChange,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Category(title = stringResource(R.string.settings)) {
                        ListItem(
                            headline = stringResource(R.string.select_apps),
                            supportingText = stringResource(R.string.select_apps_d),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        context,
                                        SelectAppsActivity::class.java
                                    )
                                )
                            },
                            divider = true,
                            dividerThickness = 2.dp,
                            dividerColor = MaterialTheme.colorScheme.surface,
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
                                AccessibilityKeeperService.instance?.stopSelf()
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
                                openSetPasswordDialog = true
                            },
                            divider = true,
                            dividerThickness = 2.dp,
                            dividerColor = MaterialTheme.colorScheme.surface,
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
                            divider = true,
                            dividerThickness = 2.dp,
                            dividerColor = MaterialTheme.colorScheme.surface,
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
                                context.homeScreen()

                                sleep(500)

                                context.finish()

                                val intent = Intent(context, FakeCrashActivity::class.java)
                                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                                val b = Bundle()
                                b.putString("appPackage", context.packageName)
                                context.startActivity(intent)
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