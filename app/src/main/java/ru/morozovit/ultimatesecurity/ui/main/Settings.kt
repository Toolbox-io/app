package ru.morozovit.ultimatesecurity.ui.main

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.morozovit.android.ListItem
import ru.morozovit.android.SeparatedSwitchListItem
import ru.morozovit.android.SwitchListItem
import ru.morozovit.android.alertDialog
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.allowBiometric
import ru.morozovit.ultimatesecurity.Settings.deleteGlobalPasswordDsa
import ru.morozovit.ultimatesecurity.Settings.dontShowInRecents
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.AuthActivity
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview

@Composable
@PhonePreview
fun SettingsScreen() {
    val (valueOrFalse, runOrNoop) = previewUtils()

    val context = LocalContext() as MainActivity
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponentName = ComponentName(context, DeviceAdmin::class.java)
    val activityLauncher = context.activityLauncher
    AppTheme {
        Column(Modifier.verticalScroll(rememberScrollState())) {
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
        }
    }
}