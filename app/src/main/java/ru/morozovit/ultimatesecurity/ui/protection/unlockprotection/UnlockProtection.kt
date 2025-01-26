package ru.morozovit.ultimatesecurity.ui.protection.unlockprotection

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import ru.morozovit.android.ListItem
import ru.morozovit.android.SwitchCard
import ru.morozovit.android.alertDialog
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.enabled
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@Composable
@PhonePreview
fun UnlockProtectionScreen() {
    WindowInsetsHandler {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            val (valueOrFalse, runOrNoop, isPreview) = previewUtils()
            val context = LocalContext() as MainActivity
            val activityLauncher = context.activityLauncher

            val dpm =
                if (isPreview)
                    null
                else
                    context
                        .getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admComponent =
                if (isPreview)
                    null
                else
                    ComponentName(context, DeviceAdmin::class.java)

            runOrNoop {
                if (!dpm!!.isAdminActive(admComponent!!)) enabled = false
            }

            val coroutineScope = rememberCoroutineScope()

            var mainSwitch by remember {
                mutableStateOf(
                    valueOrFalse {
                        if (!dpm!!.isAdminActive(admComponent!!))
                            false
                        else
                            enabled
                    }
                )
            }

            val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@ {
                if (!isPreview) {
                    if (it) {
                        if (dpm!!.isAdminActive(admComponent!!)) {
                            enabled = true
                            mainSwitch = true
                            return@sw
                        }
                        // TODO rewrite in Jetpack Compose
                        context.alertDialog {
                            title(R.string.permissions_required)
                            message(R.string.up_permissions)
                            negativeButton(R.string.cancel)
                            positiveButton(R.string.ok) {
                                val intent =
                                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(
                                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                            admComponent
                                        )
                                        putExtra(
                                            DevicePolicyManager.EXTRA_ADD_EXPLANATION, """
                            This permission is needed for the following features:
                            - Protect this app from being removed by an intruder
                            - Detect failed unlock attempts to take the required actions
                            This app NEVER uses this permission for anything not listed above.
                            """.trimIndent()
                                        )
                                    }
                                activityLauncher.launch(intent) { result ->
                                    mainSwitch = result.resultCode == RESULT_OK
                                    enabled = result.resultCode == RESULT_OK
                                }
                            }
                        }
                        return@sw
                    } else {
                        enabled = false
                    }
                }
                mainSwitch = it
            }

            SwitchCard(
                text = stringResource(R.string.enable),
                checked = mainSwitch,
                onCheckedChange = mainSwitchOnCheckedChange
            )
            HorizontalDivider()

            var unlockAttempts by rememberSaveable {
                mutableStateOf("${
                    if (isPreview) 
                        0 
                    else 
                        Settings.UnlockProtection.unlockAttempts
                }")
            }
            var isError by remember { mutableStateOf(false) }
            fun validate() {
                isError = runCatching { unlockAttempts.toInt() }.isSuccess
            }
            LaunchedEffect(Unit) {
                validate()
            }

            ListItem(
                headline = stringResource(R.string.unlock_attempts),
                supportingText = stringResource(R.string.unlock_attempts_d),
                divider = true,
                onClick = {}
            ) {
                TextField(
                    value = unlockAttempts,
                    onValueChange = {
                        unlockAttempts = it
                        validate()
                        coroutineScope.launch {
                            runCatching {
                                Settings.UnlockProtection.unlockAttempts = unlockAttempts.toInt()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearFocusOnKeyboardDismiss(),
                    label = {
                        Text(stringResource(R.string.attempts))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            ListItem(
                headline = stringResource(R.string.actions),
                supportingText = stringResource(R.string.actions_d),
                divider = true,
                onClick = {
                    runOrNoop {
                        context.startActivity(
                            Intent(
                                context,
                                ActionsActivity::class.java
                            )
                        )
                    }
                }
            )
        }
    }
}