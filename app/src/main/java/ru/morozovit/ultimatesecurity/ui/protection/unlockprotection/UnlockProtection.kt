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
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.android.ui.SwitchCard
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.UnlockProtection.enabled
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.ultimatesecurity.ui.protection.ActionsActivity

@Composable
fun UnlockProtectionScreen(EdgeToEdgeBar: @Composable (@Composable () -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar {
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

                var permissionDialogOpen by remember { mutableStateOf(false) }
                fun permissionDialogOnDismiss() {
                    permissionDialogOpen = false
                }
                SimpleAlertDialog(
                    open = permissionDialogOpen,
                    onDismissRequest = ::permissionDialogOnDismiss,
                    title = stringResource(R.string.permissions_required),
                    body = stringResource(R.string.up_permissions),
                    positiveButtonText = stringResource(R.string.ok),
                    onPositiveButtonClick = {
                        val intent =
                            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(
                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    admComponent
                                )
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    context.resources.getString(R.string.devadmin_ed)
                                )
                            }
                        activityLauncher.launch(intent) { result ->
                            mainSwitch = result.resultCode == RESULT_OK
                            enabled = result.resultCode == RESULT_OK
                        }
                    },
                    negativeButtonText = stringResource(R.string.cancel),
                    onNegativeButtonClick = ::permissionDialogOnDismiss
                )

                val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@{
                    if (!isPreview) {
                        if (it) {
                            if (dpm!!.isAdminActive(admComponent!!)) {
                                enabled = true
                                mainSwitch = true
                                return@sw
                            }
                            permissionDialogOpen = true
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
                    mutableStateOf(
                        "${
                            if (isPreview)
                                0
                            else
                                Settings.UnlockProtection.unlockAttempts
                        }"
                    )
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
}