package ru.morozovit.ultimatesecurity.ui.main

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.morozovit.android.ListItem
import ru.morozovit.android.SeparatedSwitchListItem
import ru.morozovit.android.SimpleAlertDialog
import ru.morozovit.android.SwitchListItem
import ru.morozovit.android.alertDialog
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.allowBiometric
import ru.morozovit.ultimatesecurity.Settings.appTheme
import ru.morozovit.ultimatesecurity.Settings.deleteGlobalPasswordDsa
import ru.morozovit.ultimatesecurity.Settings.dontShowInRecents
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.AuthActivity
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.Theme
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.ultimatesecurity.ui.dynamicThemeEnabled
import ru.morozovit.ultimatesecurity.ui.theme

@OptIn(ExperimentalLayoutApi::class)
@Composable
@PhonePreview
fun SettingsScreen() {
    val (valueOrFalse, runOrNoop) = previewUtils()

    val c = LocalContext()
    val context by lazy { c as MainActivity }
    val dpm by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    val adminComponentName by lazy {
        ComponentName(context, DeviceAdmin::class.java)
    }
    val activityLauncher by lazy {
        context.activityLauncher
    }

    var deleteAppDialogOpen by remember { mutableStateOf(false) }

    SimpleAlertDialog(
        open = deleteAppDialogOpen,
        onDismissRequest = { deleteAppDialogOpen = false },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.delete_app)
            )
        },
        title = stringResource(R.string.delete_app),
        body = stringResource(R.string.delete_app_d),
        // TODO finish
    )

    WindowInsetsHandler {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
        ) {
            var allowBiometricSwitchEnabled by remember {
                mutableStateOf(
                    valueOrFalse {
                        globalPassword != "" &&
                                globalPasswordEnabled &&
                                BiometricManager.from(context).canAuthenticate(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                                ) == BIOMETRIC_SUCCESS
                    }
                )
            }

            runOrNoop {
                if (globalPassword == "") globalPasswordEnabled = false
            }

            // Device admin
            var devAdmSwitch by remember {
                mutableStateOf(
                    valueOrFalse {
                        dpm.isAdminActive(adminComponentName)
                    }
                )
            }
            val devAdmOnCheckedChanged: (Boolean) -> Unit = {
                runOrNoop {
                    if (it) {
                        activityLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(
                                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                adminComponentName
                            )
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                """
                                    This permission is needed for the following features:
                                    - Protect this app from being removed by an intruder
                                    - Detect failed unlock attempts to take the required actions
                                    This app NEVER uses this permission for anything not listed above.
                                    """.trimIndent()
                            )
                        }) { result ->
                            devAdmSwitch = result.resultCode == RESULT_OK
                        }
                    } else {
                        dpm.removeActiveAdmin(adminComponentName)
                        Settings.UnlockProtection.enabled = false
                        devAdmSwitch = false
                    }
                }
            }
            SwitchListItem(
                headline = stringResource(R.string.devadmin),
                supportingText = stringResource(R.string.devadmin_d),
                checked = devAdmSwitch,
                onCheckedChange = devAdmOnCheckedChanged,
                listItemOnClick = {
                    devAdmOnCheckedChanged(!devAdmSwitch)
                },
                divider = true
            )

            // Delete
            ListItem(
                headline = stringResource(R.string.delete),
                supportingText = stringResource(R.string.delete_d),
                modifier = Modifier.clickable {
                    // TODO rewrite in Jetpack Compose
                    context.alertDialog {
                        title(R.string.delete_app)
                        message(R.string.delete_app_d)
                        neutralButton(R.string.cancel)
                        positiveButton(R.string.yes) {
                            devAdmOnCheckedChanged(false)
                            context.startActivity(Intent(
                                Intent.ACTION_DELETE, Uri.fromParts(
                                    "package",
                                    context.packageName,
                                    null
                                )
                            ))
                        }
                    }
                },
                divider = true
            )

            // Password lock
            var passwordSwitch by remember {
                mutableStateOf(
                    valueOrFalse {
                        globalPasswordEnabled && globalPassword != ""
                    }
                )
            }
            fun setPassword() {
                activityLauncher.launch(
                    Intent(
                        context,
                        AuthActivity::class.java
                    ).apply {
                        putExtra("mode", 1)
                    }
                ) {
                    if (it.resultCode == RESULT_OK) {
                        passwordSwitch = true
                        globalPasswordEnabled = true
                    }
                }
            }
            val passwordOnCheckedChanged: (Boolean) -> Unit = pw@ {
                // passwordSwitch = it
                if (it) {
                    if (globalPassword == "") {
                        setPassword()
                        passwordSwitch = true
                        return@pw
                    }
                } else {
                    if (!deleteGlobalPasswordDsa) {
                        passwordSwitch = false
                        // TODO rewrite in Jetpack Compose
                        context.alertDialog {
                            title(R.string.dpw)
                            message(R.string.dpw_d)
                            negativeButton(R.string.no) {
                                allowBiometricSwitchEnabled = globalPassword != "" && globalPasswordEnabled
                            }
                            neutralButton(R.string.dsa) {
                                deleteGlobalPasswordDsa = true
                                allowBiometricSwitchEnabled = globalPassword != "" && globalPasswordEnabled
                            }
                            positiveButton(R.string.yes) {
                                globalPassword = ""
                                allowBiometricSwitchEnabled = globalPassword != "" && globalPasswordEnabled
                            }
                        }
                        return@pw
                    }
                }
                globalPasswordEnabled = it
                context.updateLock()
                allowBiometricSwitchEnabled = globalPassword != "" && globalPasswordEnabled
                passwordSwitch = it
            }

            SeparatedSwitchListItem(
                headline = stringResource(R.string.lockapp),
                supportingText = stringResource(R.string.lockapp_d),
                checked = passwordSwitch,
                onCheckedChange = passwordOnCheckedChanged,
                bodyOnClick = ::setPassword,
                divider = true
            )

            // Allow biometric
            var allowBiometricSwitch by remember {
                mutableStateOf(
                    valueOrFalse { allowBiometric }
                )
            }

            val allowBiometricSwitchOnCheckedChanged: (Boolean) -> Unit = {
                allowBiometricSwitch = it
                allowBiometric = it
            }

            SwitchListItem(
                headline = stringResource(R.string.allow_biometric),
                supportingText = stringResource(R.string.allow_biometric_d),
                checked = allowBiometricSwitch,
                onCheckedChange = allowBiometricSwitchOnCheckedChanged,
                listItemOnClick = {
                    allowBiometricSwitchOnCheckedChanged(!allowBiometricSwitch)
                },
                divider = true
            )

            // Don't show in recents
            var dontShowInRecentsSwitch by remember {
                mutableStateOf(
                    valueOrFalse { dontShowInRecents }
                )
            }
            val dontShowInRecentsOnCheckedChanged: (Boolean) -> Unit = {
                dontShowInRecentsSwitch = it
                dontShowInRecents = it
            }
            SwitchListItem(
                headline = stringResource(R.string.dont_show_in_recents),
                supportingText = stringResource(R.string.dont_show_in_recents_d),
                checked = dontShowInRecentsSwitch,
                onCheckedChange = dontShowInRecentsOnCheckedChanged,
                listItemOnClick = {
                    dontShowInRecentsOnCheckedChanged(!dontShowInRecentsSwitch)
                },
                divider = true
            )

            runOrNoop {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    materialYouEnabled = false
            }

            var materialYouSwitch by remember {
                mutableStateOf(
                    valueOrFalse {
                        materialYouEnabled
                    }
                )
            }
            val materialYouOnCheckedChanged: (Boolean) -> Unit = {
                materialYouSwitch = it
                runOrNoop {
                    materialYouEnabled = it
                    dynamicThemeEnabled = it
                    context.configureTheme()
                }
            }

            SwitchListItem(
                headline = stringResource(R.string.materialYou),
                supportingText = stringResource(R.string.materialYou_d),
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                checked = materialYouSwitch,
                onCheckedChange = materialYouOnCheckedChanged,
                listItemOnClick = { materialYouOnCheckedChanged(!materialYouSwitch) },
                divider = true
            )

            ListItem(
                headline = stringResource(R.string.theme),
                divider = true,
                bottomContent = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var selectedIndex by remember { mutableIntStateOf(appTheme.ordinal) }
                        val options = listOf(
                            stringResource(R.string.as_system),
                            stringResource(R.string.light),
                            stringResource(R.string.dark)
                        )
                        options.forEachIndexed { index, label ->
                            FilterChip(
                                onClick = {
                                    selectedIndex = index
                                    appTheme = Theme.entries[index]
                                    theme = Theme.entries[index]
                                    context.configureTheme()
                                },
                                selected = index == selectedIndex,
                                leadingIcon = {
                                    if (index == 0) {
                                        Spacer(Modifier.width(16.dp))
                                    }
                                    when (index) {
                                        0 -> Icon(Icons.Filled.Settings, null)
                                        1 -> Icon(Icons.Filled.LightMode, null)
                                        2 -> Icon(Icons.Filled.DarkMode, null)
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}