package io.toolbox.ui.protection

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
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
import io.toolbox.ui.EdgeToEdgeBarType
import io.toolbox.ui.MainActivity
import io.toolbox.ui.protection.actions.ActionsActivity
import kotlinx.coroutines.launch
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SimpleAlertDialog
import ru.morozovit.android.utils.ui.SwitchCard
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.clearFocusOnKeyboardDismiss
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll

@Composable
fun UnlockProtectionScreen(EdgeToEdgeBar: EdgeToEdgeBarType) {
    with (LocalContext() as MainActivity) {
        WindowInsetsHandler {
            EdgeToEdgeBar { innerPadding ->
                Column(
                    Modifier
                        .verticalScroll()
                        .padding(innerPadding)
                ) {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val admComponent = ComponentName(this@with, DeviceAdmin::class.java)
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
                                        resources.getString(R.string.devadmin_ed)
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


                    SwitchCard(
                        text = stringResource(R.string.enable),
                        checked = mainSwitch,
                        onCheckedChange = sw@{
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
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var unlockAttempts by rememberSaveable {
                        mutableStateOf(
                            "${Settings.UnlockProtection.unlockAttempts}"
                        )
                    }
                    var isError by remember { mutableStateOf(false) }
                    fun validate() {
                        isError = runCatching { unlockAttempts.toInt() }.isFailure
                    }

                    LaunchedEffect(Unit) {
                        validate()
                    }

                    Category {
                        ListItem(
                            headline = stringResource(R.string.unlock_attempts),
                            supportingText = stringResource(R.string.unlock_attempts_d),
                            materialDivider = true,
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
                                isError = isError,
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
                                startActivity(
                                    Intent(
                                        this@with,
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
}