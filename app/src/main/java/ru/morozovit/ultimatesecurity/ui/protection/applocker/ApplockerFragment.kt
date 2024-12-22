package ru.morozovit.ultimatesecurity.ui.protection.applocker

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import ru.morozovit.android.ListItem
import ru.morozovit.android.SecureTextField
import ru.morozovit.android.SwitchCard
import ru.morozovit.android.alertDialog
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.homeScreen
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.UnlockMode.PRESS_TITLE
import ru.morozovit.ultimatesecurity.Settings.Applocker.getUnlockModeDescription
import ru.morozovit.ultimatesecurity.Settings.Applocker.unlockMode
import ru.morozovit.ultimatesecurity.Settings.accessibility
import ru.morozovit.ultimatesecurity.services.Accessibility
import ru.morozovit.ultimatesecurity.services.Accessibility.Companion.waitingForAccessibility
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import java.lang.Thread.sleep

class ApplockerFragment : Fragment() {
    private var resumeHandler: (() -> Unit)? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    @PhonePreview
    fun ApplockerScreen() {
        AppTheme {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val (valueOrFalse, runOrNoop, isPreview, valueOrTrue) = previewUtils()

                    val default = stringResource(R.string.lp_ai)
                    var unlockMethodText by remember {
                        mutableStateOf(
                            if (!isPreview)
                                getUnlockModeDescription(unlockMode, resources)
                            else
                                default
                        )
                    }

                    val errorDisablingService = stringResource(R.string.error_disabling_service)
                    val settings = stringResource(R.string.settings)

                    var openSetPasswordDialog by remember { mutableStateOf(false) }
                    if (openSetPasswordDialog) {
                        fun onDismissRequest() {
                            openSetPasswordDialog = false
                        }
                        Dialog(
                            onDismissRequest = ::onDismissRequest
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
                                    var oldPasswordIsError by rememberSaveable { mutableStateOf(true) }

                                    if (
                                        valueOrTrue {
                                            Settings.Applocker.password != ""
                                        }
                                    ) {
                                        fun validate() {
                                            oldPasswordIsError = oldPassword.isEmpty()
                                        }
                                        LaunchedEffect(Unit) {
                                            snapshotFlow { oldPassword }.collect { validate() }
                                        }

                                        SecureTextField(
                                            value = oldPassword,
                                            onValueChange = { oldPassword = it },
                                            label = { Text("Old password") },
                                            visibilityOnClick = {
                                                oldPasswordHidden = !oldPasswordHidden
                                            },
                                            modifier = Modifier
                                                .padding()
                                                .padding(bottom = 10.dp)
                                                .fillMaxWidth()
                                                .clearFocusOnKeyboardDismiss(),
                                            passwordHidden = oldPasswordHidden,
                                            isError = oldPasswordIsError
                                        )
                                    }
                                    var newPasswordHidden by rememberSaveable { mutableStateOf(true) }
                                    var newPassword by rememberSaveable { mutableStateOf("") }

                                    SecureTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("New password") },
                                        visibilityOnClick = {
                                            newPasswordHidden = !newPasswordHidden
                                        },
                                        modifier = Modifier
                                            .padding()
                                            .padding(bottom = 10.dp)
                                            .fillMaxWidth()
                                            .clearFocusOnKeyboardDismiss(),
                                        passwordHidden = newPasswordHidden
                                    )

                                    var confirmPasswordHidden by rememberSaveable {
                                        mutableStateOf(
                                            true
                                        )
                                    }
                                    var confirmPassword by rememberSaveable { mutableStateOf("") }

                                    SecureTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("Confirm password") },
                                        visibilityOnClick = {
                                            confirmPasswordHidden = !confirmPasswordHidden
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clearFocusOnKeyboardDismiss(),
                                        passwordHidden = confirmPasswordHidden
                                    )
                                    Row(Modifier.padding(top = 24.dp)) {
                                        TextButton(
                                            onClick = ::onDismissRequest
                                        ) {
                                            Text(text = stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                if (oldPassword == Settings.Applocker.password && newPassword == confirmPassword) {
                                                    Settings.Applocker.password = newPassword
                                                    onDismissRequest()
                                                } else if (oldPassword.isEmpty()) {
                                                    runOrNoop {
                                                        Toast.makeText(
                                                            requireActivity(),
                                                            R.string.old_password_cannot_be_empty,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    runOrNoop {
                                                        Toast.makeText(
                                                            requireActivity(),
                                                            R.string.passwords_dont_match,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(text = stringResource(R.string.ok))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var openUnlockMethodDialog by remember { mutableStateOf(false) }
                    if (openUnlockMethodDialog) {
                        fun onDismissRequest() {
                            openUnlockMethodDialog = false
                        }
                        Dialog(
                            onDismissRequest = ::onDismissRequest
                        ) {
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        text = stringResource(R.string.unlockmethod),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier
                                            .padding()
                                            .padding(bottom = 16.dp)
                                    )
                                    val radioOptions = mutableListOf<Int>()
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        radioOptions += LONG_PRESS_APP_INFO
                                        radioOptions += LONG_PRESS_CLOSE
                                    } else {
                                        radioOptions += LONG_PRESS_OPEN_APP_AGAIN
                                    }
                                    radioOptions += LONG_PRESS_TITLE
                                    radioOptions += PRESS_TITLE
                                    val (selectedOption, onOptionSelected) = remember {
                                        mutableIntStateOf(
                                            radioOptions[
                                                if (isPreview)
                                                    0
                                                else
                                                    radioOptions.indexOf(unlockMode)
                                            ]
                                        )
                                    }
                                    // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
                                    Column(Modifier.selectableGroup()) {
                                        radioOptions.forEach { num ->
                                            val text =
                                                if (!isPreview)
                                                    getUnlockModeDescription(num, resources)
                                                else
                                                    stringResource(R.string.lp_ai)
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .selectable(
                                                        selected = (num == selectedOption),
                                                        onClick = { onOptionSelected(num) },
                                                        role = Role.RadioButton
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = (num == selectedOption),
                                                    onClick = null // null recommended for accessibility with screenreaders
                                                )
                                                Text(
                                                    text = text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.padding(start = 16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(Modifier.padding(top = 24.dp)) {
                                        TextButton(
                                            onClick = ::onDismissRequest
                                        ) {
                                            Text(text = stringResource(R.string.cancel))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                unlockMode = selectedOption
                                                unlockMethodText = getUnlockModeDescription(
                                                    selectedOption, resources
                                                )
                                                onDismissRequest()
                                            }
                                        ) {
                                            Text(text = stringResource(R.string.ok))
                                        }
                                    }
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

                        val mainSwitchOnCheckedChange: (Boolean) -> Unit = sw@{
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
                                        scope.launch {
                                            val result = snackbarHostState
                                                .showSnackbar(
                                                    message = errorDisablingService,
                                                    actionLabel = settings,
                                                    duration = SnackbarDuration.Short
                                                )
                                            when (result) {
                                                SnackbarResult.ActionPerformed -> {
                                                    val intent =
                                                        Intent(ACTION_ACCESSIBILITY_SETTINGS)
                                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
                                                    startActivity(intent)
                                                }

                                                SnackbarResult.Dismissed -> {}
                                            }
                                        }
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
                                openSetPasswordDialog = true
                            },
                            divider = true
                        )

                        ListItem(
                            headline = stringResource(R.string.unlockmethod),
                            supportingText = unlockMethodText,
                            onClick = {
                                openUnlockMethodDialog = true
                            },
                            divider = true
                        )
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireActivity()).apply {
        setContent { ApplockerScreen() }
    }

    override fun onResume() {
        super.onResume()
        resumeHandler?.invoke()
    }
}