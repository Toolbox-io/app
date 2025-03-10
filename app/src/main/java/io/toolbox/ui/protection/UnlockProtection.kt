package io.toolbox.ui.protection

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.DeviceAdmin
import io.toolbox.ui.MainActivity
import io.toolbox.ui.WindowInsetsHandler
import io.toolbox.ui.protection.actions.ActionsActivity
import kotlinx.coroutines.launch
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.android.ui.SwitchCard

@Composable
fun UnlockProtectionScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                val context = LocalContext() as MainActivity
                val activityLauncher = context.activityLauncher
                val dpm =
                    context
                        .getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admComponent = ComponentName(context, DeviceAdmin::class.java)
                if (!dpm.isAdminActive(admComponent)) Settings.UnlockProtection.enabled = false
                val coroutineScope = rememberCoroutineScope()

                var mainSwitch by remember {
                    mutableStateOf(
                        if (!dpm.isAdminActive(admComponent))
                            false
                        else
                            Settings.UnlockProtection.enabled
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
                            Settings.UnlockProtection.enabled = result.resultCode == RESULT_OK
                        }
                    },
                    negativeButtonText = stringResource(R.string.cancel),
                    onNegativeButtonClick = ::permissionDialogOnDismiss
                )

                val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@{
                    if (it) {
                        if (dpm.isAdminActive(admComponent)) {
                            Settings.UnlockProtection.enabled = true
                            mainSwitch = true
                            return@sw
                        }
                        permissionDialogOpen = true
                        return@sw
                    } else {
                        Settings.UnlockProtection.enabled = false
                    }
                    mainSwitch = false
                }

                SwitchCard(
                    text = stringResource(R.string.enable),
                    checked = mainSwitch,
                    onCheckedChange = mainSwitchOnCheckedChange,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var unlockAttempts by rememberSaveable {
                    mutableStateOf(
                        "${Settings.UnlockProtection.unlockAttempts}"
                    )
                }
                var isError by remember { mutableStateOf(false) }
                fun validate() {
                    isError = runCatching { unlockAttempts.toInt() }.isSuccess
                }
                LaunchedEffect(Unit) {
                    validate()
                }

                Category {
                    ListItem(
                        headline = stringResource(R.string.unlock_attempts),
                        supportingText = stringResource(R.string.unlock_attempts_d),
                        divider = true,
                        dividerThickness = 2.dp,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        onClick = {},
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Password,
                                contentDescription = null
                            )
                        }
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
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    ActionsActivity::class.java
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}