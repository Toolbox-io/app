package io.toolbox.ui.main

import android.app.Activity.RESULT_OK
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
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
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.Settings.ACTIONS_LABEL
import io.toolbox.Settings.CURRENT_CUSTOM_ALARM_LABEL
import io.toolbox.Settings.CUSTOM_ALARMS_LABEL
import io.toolbox.Settings.KEYS_LABEL
import io.toolbox.Settings.allowBiometric
import io.toolbox.Settings.appTheme
import io.toolbox.Settings.dontShowInRecents
import io.toolbox.Settings.materialYouEnabled
import io.toolbox.services.DeviceAdmin
import io.toolbox.ui.AuthActivity
import io.toolbox.ui.MainActivity
import io.toolbox.ui.dynamicThemeEnabled
import io.toolbox.ui.protection.actions.ActionsActivity
import io.toolbox.ui.theme
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SeparatedSwitchListItem
import ru.morozovit.android.utils.ui.SimpleAlertDialog
import ru.morozovit.android.utils.ui.SwitchListItem
import ru.morozovit.android.utils.ui.ThemeSetting
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll
import java.io.BufferedInputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    val c = LocalContext()
    val context by lazy { c as MainActivity }
    val dpm by lazy { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponentName by lazy { ComponentName(context, DeviceAdmin::class.java) }
    val activityLauncher by lazy { context.activityLauncher }

    // Device admin
    var devAdmSwitch by remember {
        mutableStateOf(dpm.isAdminActive(adminComponentName))
    }
    val devAdmOnCheckedChanged = { it: Boolean ->
        if (it) {
            activityLauncher.launch(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        adminComponentName
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.resources.getString(R.string.devadmin_ed)
                    )
                }
            ) { result ->
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
    var allowBiometricSwitch by remember { mutableStateOf(allowBiometric) }

    // Password lock
    var passwordSwitch by remember { mutableStateOf(Settings.Keys.App.isSet) }
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

    // Don't show in recents
    var dontShowInRecentsSwitch by remember { mutableStateOf(dontShowInRecents) }

    // Material You
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) materialYouEnabled = false
    var materialYouSwitch by remember { mutableStateOf(materialYouEnabled) }

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

    // Check for updates
    var checkForUpdatesSwitch by remember { mutableStateOf(!Settings.update_dsa) }

    // Main content
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(
                Modifier
                    .verticalScroll()
                    .padding(innerPadding)
            ) {
                Category(title = stringResource(R.string.security)) {
                    // Device admin
                    SwitchListItem(
                        headline = stringResource(R.string.devadmin),
                        supportingText = stringResource(R.string.devadmin_d),
                        checked = devAdmSwitch,
                        onCheckedChange = devAdmOnCheckedChanged,
                        materialDivider = true,
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
                        onCheckedChange = pw@ {
                            if (it) {
                                if (!Settings.Keys.App.isSet) setPassword()
                            } else {
                                passwordSwitch = false
                                allowBiometricSwitchEnabled = false
                                Settings.Keys.App.clear()
                            }
                        },
                        bodyOnClick = ::setPassword,
                        materialDivider = true,
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
                        onCheckedChange = {
                            allowBiometricSwitch = it
                            allowBiometric = it
                        },
                        enabled = allowBiometricSwitchEnabled,
                        materialDivider = true,
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
                        onCheckedChange = {
                            dontShowInRecentsSwitch = it
                            dontShowInRecents = it
                        },
                        materialDivider = true,
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
                        onCheckedChange = {
                            materialYouSwitch = it
                            materialYouEnabled = it
                            dynamicThemeEnabled = it
                            context.configureTheme()
                        },
                        materialDivider = true,
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
                        materialDivider = true,
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
                                            appTheme = ThemeSetting.entries[index]
                                            theme = ThemeSetting.entries[index]
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
                    SwitchListItem(
                        headline = stringResource(R.string.check_for_updates),
                        supportingText = stringResource(R.string.check_for_updates_d),
                        checked = checkForUpdatesSwitch,
                        onCheckedChange = {
                            checkForUpdatesSwitch = it
                            Settings.update_dsa = !it
                        },
                        materialDivider = true,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null
                            )
                        }
                    )
                    ListItem(
                        headline = stringResource(R.string.export_settings),
                        supportingText = stringResource(R.string.export_settings_d),
                        onClick = {
                            fun onError() = Toast.makeText(context, R.string.smthwentwrong, Toast.LENGTH_SHORT).show()

                            try {
                                File("${context.cacheDir.absolutePath}/shared_prefs").let { cache ->
                                    File("${context.dataDir.absolutePath}/shared_prefs").copyRecursively(
                                        target = cache,
                                        overwrite = true
                                    )

                                    activityLauncher.launch(
                                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "application/zip"
                                            putExtra(
                                                Intent.EXTRA_TITLE,
                                                if (Build.VERSION.SDK_INT >= 26) {
                                                    "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm"))}.tiosettings"
                                                } else {
                                                    "settings.tiosettings"
                                                }
                                            )
                                        }
                                    ) { result ->
                                        try {
                                            val intent = result.data
                                            val uri = intent?.data

                                            if (result.resultCode == RESULT_OK && uri != null) {
                                                ZipOutputStream(
                                                    context
                                                        .contentResolver
                                                        .openOutputStream(uri)!!
                                                        .buffered()
                                                ).use { out ->
                                                    cache.listFiles()?.forEach {
                                                        if (it.isFile) {
                                                            when (it.nameWithoutExtension) {
                                                                KEYS_LABEL -> it.delete()
                                                                ACTIONS_LABEL -> {
                                                                    // Remove custom alarms
                                                                    it.writeText(
                                                                        it.readText()
                                                                            .replace(
                                                                                "<set\\s*name=\"${CUSTOM_ALARMS_LABEL}\"\\s*>(?>.|\\s)*</set>",
                                                                                ""
                                                                            )
                                                                            .replace(
                                                                                "<string\\s+name=\\\"${CURRENT_CUSTOM_ALARM_LABEL}\\\"\\s*>(?>.|\\s)*</string>",
                                                                                ""
                                                                            )
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        it.inputStream().buffered().use { origin ->
                                                            out.putNextEntry(ZipEntry(it.name))
                                                            origin.copyTo(out, 1024)
                                                        }
                                                    }
                                                }

                                                Toast.makeText(
                                                    context,
                                                    R.string.settings_exported_successfully,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else error("")
                                        } catch (_: Exception) {
                                            onError()
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                onError()
                            }
                        },
                        materialDivider = true,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null
                            )
                        }
                    )
                    ListItem(
                        headline = stringResource(R.string.import_settings),
                        supportingText = stringResource(R.string.import_settings_d),
                        onClick = {
                            fun onError(e: Exception) {
                                Log.e("Settings", "An error occurred", e)
                                Toast.makeText(context, R.string.smthwentwrong, Toast.LENGTH_SHORT).show()
                            }

                            try {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/zip"
                                }
                                activityLauncher.launch(intent) {
                                    try {
                                        Log.d("Settings", "Importing settings")
                                        if (it.resultCode == RESULT_OK && it.data != null && it.data!!.data != null) {
                                            Log.d("Settings", "Data not null")
                                            val uri = it.data!!.data!!
                                            Log.d("Settings", "Caching imported settings into app dir")
                                            val cachedZip = File(context.cacheDir.absolutePath + "/settings.zip")
                                            cachedZip.outputStream().use { zip ->
                                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                                    BufferedInputStream(inputStream).use { srcZip ->
                                                        srcZip.copyTo(zip)
                                                    }
                                                }
                                            }
                                            Log.d("Settings", "Unzipping settings to the shared_prefs folder")
                                            ZipFile(cachedZip).use { zip ->
                                                zip.entries().asSequence().forEach { entry ->
                                                    zip.getInputStream(entry).use { input ->
                                                        File(
                                                            "${context.dataDir.absolutePath}/shared_prefs/${entry.name}"
                                                        ).outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                }
                                            }
                                            Log.d("Settings", "Scheduling start of MainActivity")
                                            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)[
                                                AlarmManager.RTC,
                                                System.currentTimeMillis() + 1000
                                            ] = PendingIntent.getActivity(
                                                context,
                                                123456,
                                                Intent(context, MainActivity::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                },
                                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                            )
                                            Toast.makeText(
                                                context,
                                                R.string.settings_imported_successfully,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.d("Settings", "Exiting")
                                            exitProcess(0)
                                        } else {
                                            Log.e("Settings", "Data is null")
                                            throw NullPointerException("data is null")
                                        }
                                    } catch (e: Exception) {
                                        onError(e)
                                    }
                                }
                            } catch (e: Exception) {
                                onError(e)
                            }
                        },
                        materialDivider = true,
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null
                            )
                        }
                    )
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