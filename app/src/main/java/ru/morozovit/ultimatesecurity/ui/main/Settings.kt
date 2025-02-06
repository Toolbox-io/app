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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SeparatedSwitchListItem
import ru.morozovit.android.ui.SimpleAlertDialog
import ru.morozovit.android.ui.SwitchListItem
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.allowBiometric
import ru.morozovit.ultimatesecurity.Settings.appTheme
import ru.morozovit.ultimatesecurity.Settings.dontShowInRecents
import ru.morozovit.ultimatesecurity.Settings.materialYouEnabled
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.AuthActivity
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.Theme
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import ru.morozovit.ultimatesecurity.ui.dynamicThemeEnabled
import ru.morozovit.ultimatesecurity.ui.protection.ActionsActivity
import ru.morozovit.ultimatesecurity.ui.theme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(EdgeToEdgeBar: @Composable (@Composable () -> Unit) -> Unit) {
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

    // Device admin
    var devAdmSwitch by remember {
        mutableStateOf(dpm.isAdminActive(adminComponentName))
    }
    val devAdmOnCheckedChanged: (Boolean) -> Unit = {
        if (it) {
            activityLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    adminComponentName
                )
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.resources.getString(R.string.devadmin_ed)
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

    // Allow biometric
    var allowBiometricSwitchEnabled by remember {
        mutableStateOf(
            Settings.Keys.App.isSet &&
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BIOMETRIC_SUCCESS
        )
    }
    var allowBiometricSwitch by remember {
        mutableStateOf(
            allowBiometric
        )
    }
    val allowBiometricSwitchOnCheckedChanged: (Boolean) -> Unit = {
        allowBiometricSwitch = it
        allowBiometric = it
    }

    // Password lock
    var passwordSwitch by remember {
        mutableStateOf(Settings.Keys.App.isSet)
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
            }
        }
    }
    val passwordOnCheckedChanged: (Boolean) -> Unit = pw@ {
        if (it) {
            if (!Settings.Keys.App.isSet) {
                setPassword()
                passwordSwitch = true
                return@pw
            }
        } else {
            passwordSwitch = false
            Settings.Keys.App.set("")
            allowBiometricSwitchEnabled = Settings.Keys.App.isSet
            return@pw
        }
        context.updateLock()
        allowBiometricSwitchEnabled = Settings.Keys.App.isSet
        passwordSwitch = true
    }

    // Don't show in recents
    var dontShowInRecentsSwitch by remember {
        mutableStateOf(
            dontShowInRecents
        )
    }
    val dontShowInRecentsOnCheckedChanged: (Boolean) -> Unit = {
        dontShowInRecentsSwitch = it
        dontShowInRecents = it
    }

    // Material You
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        materialYouEnabled = false
    var materialYouSwitch by remember {
        mutableStateOf(
            materialYouEnabled
        )
    }
    val materialYouOnCheckedChanged: (Boolean) -> Unit = {
        materialYouSwitch = it
        materialYouEnabled = it
        dynamicThemeEnabled = it
        context.configureTheme()
    }

    // Delete app dialog
    var deleteAppDialogOpen by remember { mutableStateOf(false) }
    fun deleteAppDialogOnDismiss() {
        deleteAppDialogOpen = false
    }
    SimpleAlertDialog(
        open = deleteAppDialogOpen,
        onDismissRequest = ::deleteAppDialogOnDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.delete_app)
            )
        },
        title = stringResource(R.string.delete_app),
        body = stringResource(R.string.delete_app_d),
        positiveButtonText = stringResource(R.string.yes),
        onPositiveButtonClick = {
            devAdmOnCheckedChanged(false)
            context.startActivity(
                Intent(
                    Intent.ACTION_DELETE,
                    Uri.fromParts(
                        "package",
                        context.packageName,
                        null
                    )
                )
            )
            deleteAppDialogOnDismiss()
        },
        negativeButtonText = stringResource(R.string.no),
        onNegativeButtonClick = ::deleteAppDialogOnDismiss
    )

    // Main content
    WindowInsetsHandler {
        EdgeToEdgeBar {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Category(title = stringResource(R.string.security)) {
                    // Device admin
                    SwitchListItem(
                        headline = stringResource(R.string.devadmin),
                        supportingText = stringResource(R.string.devadmin_d),
                        checked = devAdmSwitch,
                        onCheckedChange = devAdmOnCheckedChanged,
                        divider = true,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        dividerThickness = 2.dp,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null
                            )
                        }
                    )
                    // Password lock
                    SeparatedSwitchListItem(
                        headline = stringResource(R.string.lockapp),
                        supportingText = stringResource(R.string.lockapp_d),
                        checked = passwordSwitch,
                        onCheckedChange = passwordOnCheckedChanged,
                        bodyOnClick = ::setPassword,
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

                    // Allow biometric
                    SwitchListItem(
                        headline = stringResource(R.string.allow_biometric),
                        supportingText = stringResource(R.string.allow_biometric_d),
                        checked = allowBiometricSwitch,
                        onCheckedChange = allowBiometricSwitchOnCheckedChanged,
                        divider = true,
                        dividerThickness = 2.dp,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null
                            )
                        }
                    )

                    // Don't show in recents
                    SwitchListItem(
                        headline = stringResource(R.string.dont_show_in_recents),
                        supportingText = stringResource(R.string.dont_show_in_recents_d),
                        checked = dontShowInRecentsSwitch,
                        onCheckedChange = dontShowInRecentsOnCheckedChanged,
                        divider = true,
                        dividerThickness = 2.dp,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    )

                    // Security actions
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

                Category(title = stringResource(R.string.customization)) {
                    // Material You
                    SwitchListItem(
                        headline = stringResource(R.string.materialYou),
                        supportingText = stringResource(R.string.materialYou_d),
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        checked = materialYouSwitch,
                        onCheckedChange = materialYouOnCheckedChanged,
                        divider = true,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        dividerThickness = 2.dp,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Wallpaper,
                                contentDescription = null
                            )
                        }
                    )

                    // Theme
                    ListItem(
                        headline = stringResource(R.string.theme),
                        divider = true,
                        dividerColor = MaterialTheme.colorScheme.surface,
                        dividerThickness = 2.dp,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = null
                            )
                        },
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

                Category {
                    // Delete app
                    ListItem(
                        headline = stringResource(R.string.delete),
                        supportingText = stringResource(R.string.delete_d),
                        onClick = {
                            deleteAppDialogOpen = true
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}