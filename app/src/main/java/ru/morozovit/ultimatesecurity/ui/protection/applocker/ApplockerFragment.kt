package ru.morozovit.ultimatesecurity.ui.protection.applocker

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.ListItem
import ru.morozovit.android.SwitchCard
import ru.morozovit.android.alertDialog
import ru.morozovit.android.homeScreen
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.Applocker.getUnlockModeDescription
import ru.morozovit.ultimatesecurity.Settings.Applocker.unlockMode
import ru.morozovit.ultimatesecurity.Settings.accessibility
import ru.morozovit.ultimatesecurity.services.Accessibility
import ru.morozovit.ultimatesecurity.services.Accessibility.Companion.waitingForAccessibility
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import java.lang.Thread.sleep


class ApplockerFragment : Fragment() {
//    private lateinit var binding: ApplockerBinding
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>
//    private var checkListener = true

    private var resumeHandler: (() -> Unit)? = null

    @Composable
    @PhonePreview
    fun ApplockerScreen() {
        AppTheme {
            val (valueOrFalse, runOrNoop, isPreview) = previewUtils()
            var openSetPasswordDialog by remember { mutableStateOf(isPreview) }
            if (openSetPasswordDialog) {
                Dialog(
                    onDismissRequest = {
                        openSetPasswordDialog = false
                    }
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

                            TextField(
                                value = oldPassword,
                                onValueChange = { oldPassword = it },
                                visualTransformation =
                                if (!oldPasswordHidden)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                label = { Text("Old password") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { oldPasswordHidden = !oldPasswordHidden }
                                    ) {
                                        val visibilityIcon =
                                            if (oldPasswordHidden) Icons.Filled.Visibility else
                                                Icons.Filled.VisibilityOff
                                        // Provide localized description for accessibility services
                                        val description =
                                            if (oldPasswordHidden)
                                                stringResource(R.string.show_pw)
                                            else
                                                stringResource(R.string.hide_pw)
                                        Icon(imageVector = visibilityIcon, contentDescription = description)
                                    }
                                },
                                modifier = Modifier.padding().padding(bottom = 10.dp)
                            )
                            var newPasswordHidden by rememberSaveable { mutableStateOf(true) }
                            var newPassword by rememberSaveable { mutableStateOf("") }

                            TextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                visualTransformation =
                                if (!newPasswordHidden)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                label = { Text("New password") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { newPasswordHidden = !newPasswordHidden }
                                    ) {
                                        val visibilityIcon =
                                            if (newPasswordHidden) Icons.Filled.Visibility else
                                                Icons.Filled.VisibilityOff
                                        // Provide localized description for accessibility services
                                        val description =
                                            if (newPasswordHidden)
                                                stringResource(R.string.show_pw)
                                            else
                                                stringResource(R.string.hide_pw)
                                        Icon(imageVector = visibilityIcon, contentDescription = description)
                                    }
                                },
                                modifier = Modifier.padding().padding(bottom = 10.dp)
                            )

                            var confirmPasswordHidden by rememberSaveable { mutableStateOf(true) }
                            var confirmPassword by rememberSaveable { mutableStateOf("") }

                            TextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                visualTransformation =
                                if (!confirmPasswordHidden)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                label = { Text("Confirm password") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { confirmPasswordHidden = !confirmPasswordHidden }
                                    ) {
                                        val visibilityIcon =
                                            if (confirmPasswordHidden) Icons.Filled.Visibility else
                                                Icons.Filled.VisibilityOff
                                        // Provide localized description for accessibility services
                                        val description =
                                            if (confirmPasswordHidden)
                                                stringResource(R.string.show_pw)
                                            else
                                                stringResource(R.string.hide_pw)
                                        Icon(imageVector = visibilityIcon, contentDescription = description)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Column {
                var mainSwitch by remember {
                    mutableStateOf(
                        valueOrFalse { accessibility }
                    )
                }
                val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@ {
                    if (!isPreview) {
                        if (it) {
                            alertDialog {
                                title(R.string.permissions_required)
                                message(R.string.al_permissions)
                                negativeButton(R.string.cancel)
                                positiveButton(R.string.ok) {
                                    val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
                                    resumeHandler = {
                                        if (accessibility) {
                                            mainSwitch = true
                                        }
                                        waitingForAccessibility = false
                                        resumeHandler = null
                                    }
                                    waitingForAccessibility = true
                                    startActivity(intent)
                                }
                            }
                            return@sw
                        } else {
                            var error = false
                            try {
                                Accessibility.instance!!.disable()
                            } catch (e: Exception) {
                                error = true
                            }

                            if (accessibility || error) {
//                            Snackbar.make(
//                                root,
//                                R.string.error_disabling_service,
//                                LENGTH_SHORT
//                            )
//                                .setAction(R.string.settings) {
//                                    val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
//                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
//                                    startActivity(intent)
//                                }
//                                .show()
                                return@sw
                            }
                        }
                    }
                    mainSwitch = it
                }

                SwitchCard(
                    text = stringResource(R.string.enable),
                    checked = mainSwitch,
                    onCheckedChange = mainSwitchOnCheckedChange,
                    cardOnClick = {
                        mainSwitchOnCheckedChange(!mainSwitch)
                    }
                )
                HorizontalDivider()
                ListItem(
                    headline = stringResource(R.string.select_apps),
                    supportingText = stringResource(R.string.select_apps_d),
                    onClick = {
                        runOrNoop {
                            startActivity(
                                Intent(
                                    requireActivity(),
                                    SelectAppsActivity::class.java
                                )
                            )
                        }
                    },
                    divider = true
                )
                ListItem(
                    headline = stringResource(R.string.test_crash),
                    supportingText = stringResource(R.string.test_crash_d),
                    onClick = {
                        runOrNoop {
                            @Suppress("DIVISION_BY_ZERO")
                            0 / 0
                        }
                    },
                    divider = true
                )
                ListItem(
                    headline = stringResource(R.string.test_fake_crash),
                    supportingText = stringResource(R.string.test_fake_crash_d),
                    onClick = {
                        runOrNoop {
                            homeScreen()

                            sleep(500)

                            requireActivity().finish()

                            val intent = Intent(activity, FakeCrashActivity::class.java)
                            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                            val b = Bundle()
                            b.putString("appPackage", requireActivity().packageName)
                            startActivity(intent)
                        }
                    },
                    divider = true
                )
                ListItem(
                    headline = stringResource(R.string.setpassword),
                    supportingText = stringResource(R.string.setpassword_d),
                    onClick = {
                        runOrNoop {
                            val intent = Intent(
                                requireActivity(),
                                PasswordInputActivity::class.java
                            )
                            val b = Bundle()
                            b.putBoolean("setPassword", true)
                            intent.putExtras(b)
                            activityLauncher.launch(intent) { result ->
                                if (result.resultCode == RESULT_OK) {
                                    try {
                                        Settings.Applocker.password =
                                            result.data?.extras?.getString("password")!!
                                    } catch (e: NullPointerException) {
                                        Toast.makeText(
                                            requireActivity(),
                                            R.string.failed_setting_password,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    divider = true
                )
                val default = stringResource(R.string.lp_ai)
                var unlockMethodText by remember {
                    mutableStateOf(
                        if (!isPreview)
                            getUnlockModeDescription(unlockMode, resources)
                        else
                            default
                    )
                }
                
                ListItem(
                    headline = stringResource(R.string.unlockmethod),
                    supportingText = unlockMethodText,
                    onClick = {
                        runOrNoop {
                            activityLauncher.launch(
                                Intent(
                                    requireActivity(),
                                    UnlockModeActivity::class.java
                                )
                            ) {
                                unlockMethodText = getUnlockModeDescription(unlockMode, resources)
                            }
                        }
                    },
                    divider = true
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) /* : View {
        binding = ApplockerBinding.inflate(inflater, container, false)
        return binding.root
    } */ = ComposeView(requireActivity())

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityLauncher = BetterActivityResult.registerActivityForResult(this)
        (view as ComposeView).setContent {
            ApplockerScreen()
        }

//        with(binding) {
//            makeSwitchCard(applockerSwitchCard, applockerSwitch)
//            if (accessibility)
//                applockerSwitch.isChecked = true
//
//            applockerSwitch.setOnCheckedChangeListener { v, checked ->
//                if (checkListener) {
//                    if (checked) {
//                        checkListener = false
//                        v.isChecked = false
//                        checkListener = true
//                        alertDialog {
//                            title(R.string.permissions_required)
//                            message(R.string.al_permissions)
//                            negativeButton(R.string.cancel)
//                            positiveButton(R.string.ok) {
//                                val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
//                                intent.flags = FLAG_ACTIVITY_NEW_TASK
//                                waitingForAccessibility = true
//                                startActivity(intent)
//                            }
//                        }
//                    } else {
//                        var error = false
//                        try {
//                            Accessibility.instance!!.disable()
//                        } catch (e: Exception) {
//                            error = true
//                        }
//
//                        if (accessibility || error) {
//                            applockerSwitch.isChecked = true
//                            Snackbar.make(
//                                root,
//                                R.string.error_disabling_service,
//                                LENGTH_SHORT
//                            )
//                                .setAction(R.string.settings) {
//                                    val intent = Intent(ACTION_ACCESSIBILITY_SETTINGS)
//                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
//                                    startActivity(intent)
//                                }
//                                .show()
//                        }
//                    }
//                }
//            }

//            binding.alTfc.setOnClickListener {
//                homeScreen()
//
//                sleep(500)
//
//                requireActivity().finish()
//
//                val intent = Intent(activity, FakeCrashActivity::class.java)
//                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
//                val b = Bundle()
//                b.putString("appPackage", requireActivity().packageName)
//                startActivity(intent)
//            }

//            binding.alApps.setOnClickListener {
//                val intent = Intent(requireActivity(), SelectAppsActivity::class.java)
//                startActivity(intent)
//            }

//            binding.alTc.setOnClickListener {
//                @Suppress("DIVISION_BY_ZERO")
//                1 / 0 // Make an exception for the app to crash
//            }

//            binding.alPw.setOnClickListener {
//                val intent = Intent(activity, PasswordInputActivity::class.java)
//                val b = Bundle()
//                b.putBoolean("setPassword", true)
//                intent.putExtras(b)
//                activityLauncher.launch(intent) { result ->
//                    if (result.resultCode == RESULT_OK) {
//                        try {
//                            Settings.Applocker.password =
//                                result.data?.extras?.getString("password")!!
//                        } catch (e: NullPointerException) {
//                            Toast.makeText(activity, R.string.failed_setting_password, Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }
//            }
//
//            binding.alUm.setOnClickListener {
//                val intent = Intent(activity, UnlockModeActivity::class.java)
//                activityLauncher.launch(intent) {
//                    binding.alI5C.text = getUnlockModeDescription(unlockMode, resources)
//                }
//            }
//
//            binding.alI5C.text = getUnlockModeDescription(unlockMode, resources)
//        }
    }

    override fun onResume() {
        super.onResume()
        resumeHandler?.invoke()
//        if (accessibility && waitingForAccessibility) {
//            checkListener = false
//            binding.applockerSwitch.isChecked = true
//            checkListener = true
//            waitingForAccessibility = false
//        }
    }
}