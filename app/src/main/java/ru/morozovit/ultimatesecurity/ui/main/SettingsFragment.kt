package ru.morozovit.ultimatesecurity.ui.main

import android.app.Activity.RESULT_OK
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.ListItem
import ru.morozovit.android.SeparatedSwitchListItem
import ru.morozovit.android.SwitchListItem
import ru.morozovit.android.alertDialog
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.allowBiometric
import ru.morozovit.ultimatesecurity.Settings.deleteGlobalPasswordDsa
import ru.morozovit.ultimatesecurity.Settings.dontShowInRecents
import ru.morozovit.ultimatesecurity.Settings.globalPassword
import ru.morozovit.ultimatesecurity.Settings.globalPasswordEnabled
import ru.morozovit.ultimatesecurity.databinding.SettingsBinding
import ru.morozovit.ultimatesecurity.services.DeviceAdmin
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.AuthActivity
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview


class SettingsFragment : Fragment() {
    private lateinit var binding: SettingsBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    private var resumeHandler: (() -> Unit)? = null

    @Composable
    @PhonePreview
    fun SettingsScreen() {
        val isPreview = LocalInspectionMode.current
        fun valueOrFalse(value: () -> Boolean) = if (isPreview) false else value()
        fun runOrNoop(block: () -> Unit) {
            if (!isPreview) block()
        }
        AppTheme {
            Column {
                var allowBiometricSwitchEnabled by remember {
                    mutableStateOf(
                        valueOrFalse {
                            globalPassword != "" &&
                                    globalPasswordEnabled &&
                                    BiometricManager.from(requireActivity()).canAuthenticate(
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
                        alertDialog {
                            title(R.string.delete_app)
                            message(R.string.delete_app_d)
                            neutralButton(R.string.cancel)
                            positiveButton(R.string.yes) {
                                binding.sI1Sw.isChecked = false
                                startActivity(Intent(
                                    Intent.ACTION_DELETE, Uri.fromParts(
                                        "package",
                                        requireActivity().packageName,
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
                            requireActivity(),
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
                            alertDialog {
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
                    (requireActivity() as MainActivity).updateLock()
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
                resumeHandler = {
                    globalPassword != "" && globalPasswordEnabled
                }

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
                    }
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        binding = SettingsBinding.inflate(inflater, container, false)
//        return binding.root
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        activityLauncher = BetterActivityResult.registerActivityForResult(this)
        dpm = requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(requireActivity(), DeviceAdmin::class.java)

        (view as ComposeView).setContent {
            SettingsScreen()
        }

//        makeSwitchCard(binding.sDeviceadmin, binding.sI1Sw)
//
//        binding.sI1Sw.isChecked = dpm.isAdminActive(adminComponentName)
//
//        var listener = true
//
//        binding.sI1Sw.setOnCheckedChangeListener { v, isChecked ->
//            if (listener) {
//                if (isChecked) {
//                    v.isChecked = false
//                    activityLauncher.launch(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
//                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
//                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, """
//                        This permission is needed for the following features:
//                        - Protect this app from being removed by an intruder
//                        - Detect failed unlock attempts to take the required actions
//                        This app NEVER uses this permission for anything not listed above.
//                        """.trimIndent())
//                    }) { result ->
//                        listener = false
//                        v.isChecked = result.resultCode == RESULT_OK
//                        listener = true
//                    }
//                } else {
//                    dpm.removeActiveAdmin(adminComponentName)
//                    Settings.UnlockProtection.enabled = false
//                }
//            }
//        }

//        binding.sDelete.setOnClickListener {
//            alertDialog {
//                title(R.string.delete_app)
//                message(R.string.delete_app_d)
//                neutralButton(R.string.cancel)
//                positiveButton(R.string.yes) {
//                    binding.sI1Sw.isChecked = false
//                    startActivity(Intent(
//                        Intent.ACTION_DELETE, Uri.fromParts(
//                            "package",
//                            requireActivity().packageName,
//                            null
//                        )
//                    ))
//                }
//            }
//        }

//        var listener2 = true
//
//        val setPassword = { _: View? ->
//            activityLauncher.launch(
//                Intent(
//                    requireActivity(),
//                    AuthActivity::class.java
//                ).apply {
//                    putExtra("mode", 1)
//                }
//            ) {
//                if (it.resultCode == RESULT_OK) {
//                    listener2 = false
//                    binding.sI3Sw.isChecked = true
//                    globalPasswordEnabled = true
//                    listener2 = true
//                }
//            }
//        }
//
//        binding.sI3Sw.isChecked = globalPasswordEnabled && globalPassword != ""
//        if (globalPassword == "") globalPasswordEnabled = false
//
//        binding.sI3Sw.setOnCheckedChangeListener pw@ { v, isChecked ->
//            if (listener2) {
//                if (isChecked) {
//                    if (globalPassword == "") {
//                        v.isChecked = false
//                        setPassword(binding.sI3Hc)
//                        return@pw
//                    }
//                } else {
//                    if (!deleteGlobalPasswordDsa) {
//                        listener2 = false
//                        v.isChecked = true
//                        listener2 = true
//                        alertDialog {
//                            title(R.string.dpw)
//                            message(R.string.dpw_d)
//                            negativeButton(R.string.no) {
//                                listener2 = false
//                                v.isChecked = false
//                                listener2 = true
//                                binding.sI4Sw.isEnabled =
//                                    globalPassword != "" && globalPasswordEnabled
//                            }
//                            neutralButton(R.string.dsa) {
//                                listener2 = false
//                                v.isChecked = false
//                                listener2 = true
//                                deleteGlobalPasswordDsa = true
//                                binding.sI4Sw.isEnabled =
//                                    globalPassword != "" && globalPasswordEnabled
//                            }
//                            positiveButton(R.string.yes) {
//                                listener2 = false
//                                v.isChecked = false
//                                listener2 = true
//                                globalPassword = ""
//                                binding.sI4Sw.isEnabled =
//                                    globalPassword != "" && globalPasswordEnabled
//                            }
//                        }
//                        return@pw
//                    }
//                }
//            }
//            globalPasswordEnabled = isChecked
//            (requireActivity() as MainActivity).updateLock()
//            binding.sI4Sw.isEnabled = globalPassword != "" && globalPasswordEnabled
//        }
//
//        binding.sI3Hc.setOnClickListener(setPassword)

//        makeSwitchCard(binding.sAllowbiometric, binding.sI4Sw)
//        binding.sI4Sw.isChecked = allowBiometric
//        binding.sI4Sw.isEnabled =
//            globalPassword != "" &&
//            globalPasswordEnabled &&
//            BiometricManager.from(requireActivity()).canAuthenticate(
//                BiometricManager.Authenticators.BIOMETRIC_STRONG
//            ) == BIOMETRIC_SUCCESS
//        binding.sI4Sw.setOnCheckedChangeListener { _, isChecked ->
//            allowBiometric = isChecked
//        }

//        makeSwitchCard(binding.sDontshowinrecents, binding.sI5Sw)
//        binding.sI5Sw.isChecked = dontShowInRecents
//        binding.sI5Sw.setOnCheckedChangeListener { _, isChecked ->
//            dontShowInRecents = isChecked
//        }
    }

    override fun onResume() {
        super.onResume()
        resumeHandler?.invoke()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.clear()
    }
}