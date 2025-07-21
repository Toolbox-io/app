@file:Suppress("NOTHING_TO_INLINE", "ArrayInDataClass")

package io.toolbox.ui.main

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.toolbox.R
import io.toolbox.api.AuthAPI
import io.toolbox.api.errorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SecureTextField
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.clearFocusOnKeyboardDismiss
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll
import kotlin.math.roundToInt

private const val ROUTE_LOADING = "loading"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_CODE_ENTRY = "code_entry/{email}"
private const val ROUTE_ACCOUNT = "account"

@Serializable
data class PydanticError(
    val detail: Array<PydanticError0>
) {
    inline val error get() =
        detail[0]
            .msg
            .replaceFirst("Value error, ", "")
}

@Serializable
data class PydanticError0(
    val type: String,
    val loc: Array<String>,
    val msg: String,
    val input: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private inline fun LoginScreen(
    crossinline onLoginSuccess: () -> Unit,
    crossinline onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login),
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val scope = rememberCoroutineScope()

        val internalError = stringResource(R.string.internal_error)

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordHidden by remember { mutableStateOf(true) }

        var isLoggingIn by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        suspend fun showError(message: String) {
            errorMessage = message
            delay(3000)
            errorMessage = ""
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SecureTextField(
            value = password,
            onValueChange = { password = it },
            hidden = passwordHidden,
            onHiddenChange = { passwordHidden = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoggingIn = true
                        try {
                            AuthAPI.login(username, password)
                            onLoginSuccess()
                        } catch (e: ResponseException) {
                            showError(e.errorMessage())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showError(internalError)
                        } finally {
                            isLoggingIn = false
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.login_btn))
            }
            AnimatedVisibility(isLoggingIn) {
                CircularProgressIndicator(Modifier.padding(start = 16.dp))
            }
        }

        AnimatedVisibility(errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = Color.Red
            )
        }

        TextButton(
            onClick = { onRegister() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(stringResource(R.string.register))
        }
    }
}

@Composable
private fun RegisterScreen(
    onRegisterSuccess: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val afar = stringResource(R.string.afar)
    val invalid_email = stringResource(R.string.invalid_email)
    val passwords_dont_match = stringResource(R.string.passwords_dont_match)
    val internal_error = stringResource(R.string.internal_error)

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordHidden by remember { mutableStateOf(true) }
    var confirmPasswordHidden by remember { mutableStateOf(true) }

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val showError = errorMessage.isNotBlank()
    val shakeTrigger = remember { mutableIntStateOf(0) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeTrigger.intValue > 0) 12f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0f at 0
            -12f at 50
            12f at 100
            -12f at 150
            12f at 200
            0f at 300
        }, label = "shakeOffset"
    )

    suspend fun showErrorMsg(msg: String) {
        errorMessage = msg
        shakeTrigger.intValue++
        delay(2000)
        errorMessage = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.register),
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SecureTextField(
            value = password,
            onValueChange = { password = it },
            hidden = passwordHidden,
            onHiddenChange = { passwordHidden = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SecureTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            hidden = confirmPasswordHidden,
            onHiddenChange = { confirmPasswordHidden = it },
            label = { Text(stringResource(R.string.confirm_password)) },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier
                .offset { IntOffset(shakeOffset.roundToInt(), 0) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    scope.launch {
                        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            showErrorMsg(afar)
                            return@launch
                        }
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            showErrorMsg(invalid_email)
                            return@launch
                        }
                        if (password != confirmPassword) {
                            showErrorMsg(passwords_dont_match)
                            return@launch
                        }
                        loading = true
                        errorMessage = ""
                        try {
                            AuthAPI.register(username, email, password)
                            delay(800)
                            onRegisterSuccess(email, username, password)
                        } catch (e: ResponseException) {
                            showErrorMsg(e.errorMessage())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showErrorMsg(internal_error)
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading
            ) {
                Text("Register")
            }
            AnimatedVisibility(loading) {
                CircularProgressIndicator(Modifier.padding(start = 16.dp))
            }
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back")
        }
        AnimatedVisibility(showError) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AccountScreen(
    snackbarHostState: SnackbarHostState,
    onLogoutSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll()
    ) {
        val context = LocalContext()
        val scope = rememberCoroutineScope()

        var username by remember { mutableStateOf("Loading...") }
        var email by remember { mutableStateOf("Loading...") }
        var memberSince by remember { mutableStateOf("Loading...") }

        var loading by remember { mutableStateOf(true) }

        var openChangePasswordDialog by remember { mutableStateOf(false) }
        if (openChangePasswordDialog) {
            fun onDismissRequest() {
                openChangePasswordDialog = false
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


                        fun validate() {
                            oldPasswordIsError = oldPassword.isEmpty()
                        }
                        LaunchedEffect(oldPassword) {
                            validate()
                        }

                        SecureTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text(stringResource(R.string.old_password)) },
                            onHiddenChange = {
                                oldPasswordHidden = !oldPasswordHidden
                            },
                            modifier = Modifier
                                .padding()
                                .padding(bottom = 10.dp)
                                .fillMaxWidth()
                                .clearFocusOnKeyboardDismiss(),
                            hidden = oldPasswordHidden,
                            isError = oldPasswordIsError
                        )

                        var newPasswordHidden by rememberSaveable { mutableStateOf(true) }
                        var newPassword by rememberSaveable { mutableStateOf("") }

                        SecureTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(stringResource(R.string.new_password)) },
                            onHiddenChange = {
                                newPasswordHidden = !newPasswordHidden
                            },
                            modifier = Modifier
                                .padding()
                                .padding(bottom = 10.dp)
                                .fillMaxWidth()
                                .clearFocusOnKeyboardDismiss(),
                            hidden = newPasswordHidden
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
                            label = { Text(stringResource(R.string.confirm_password)) },
                            onHiddenChange = {
                                confirmPasswordHidden = !confirmPasswordHidden
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clearFocusOnKeyboardDismiss(),
                            hidden = confirmPasswordHidden
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
                                    scope.launch {
                                        if (oldPassword.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                R.string.old_password_cannot_be_empty,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }

                                        if (newPassword != confirmPassword) {
                                            Toast.makeText(
                                                context,
                                                R.string.passwords_dont_match,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }

                                        try {
                                            AuthAPI.changePassword(oldPassword, newPassword)
                                            onDismissRequest()
                                            Toast.makeText(
                                                context,
                                                "Password changed successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: ResponseException) {
                                            Toast.makeText(
                                                context,
                                                e.errorMessage(),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                context,
                                                "Unknown error",
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

        LaunchedEffect(Unit) {
            try {
                val userInfo = AuthAPI.userInfo()
                username = userInfo.username
                email = userInfo.email
                memberSince = userInfo.created_at

                loading = false
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarHostState.showSnackbar("An error occurred while loading data")
            }
        }

        Category(title = "User info") {
            ListItem(
                headline = stringResource(R.string.username),
                supportingText = username,
                materialDivider = true,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null
                    )
                },
                placeholder = loading
            )
            ListItem(
                headline = stringResource(R.string.email),
                supportingText = email,
                materialDivider = true,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null
                    )
                },
                placeholder = loading
            )
            ListItem(
                headline = stringResource(R.string.member_since),
                supportingText = memberSince,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Cake,
                        contentDescription = null
                    )
                },
                placeholder = loading
            )
        }

        Category(title = "Account actions") {
            ListItem(
                headline = stringResource(R.string.logout),
                onClick = {
                    scope.launch {
                        AuthAPI.logout()
                        onLogoutSuccess()
                    }
                },
                materialDivider = true,
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                },
                placeholder = loading
            )
            ListItem(
                headline = stringResource(R.string.change_password),
                onClick = {
                    openChangePasswordDialog = true
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Password,
                        contentDescription = null
                    )
                },
                placeholder = loading
            )
        }
    }
}

@Composable
fun SixDigitCodeInput(
    code: List<String>,
    onCodeChange: (Int, String) -> Unit,
    enabled: Boolean = true,
    focusRequesters: List<androidx.compose.ui.focus.FocusRequester>,
    showSuccess: Boolean = false,
    showError: Boolean = false
) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0..5) {
            val targetColor = when {
                showSuccess -> Color(0xFFC8FFD4)
                showError -> Color(0xFFFFB4B4)
                else -> Color.Transparent
            }
            val bgColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 350),
                label = "digitBgColor"
            )
            OutlinedTextField(
                value = code[i],
                onValueChange = { value ->
                    if (value.length <= 1 && (value.isEmpty() || value[0].isDigit())) {
                        onCodeChange(i, value)
                        if (value.isNotEmpty() && i < 5) {
                            focusRequesters[i + 1].requestFocus()
                        }
                        if (value.isEmpty() && i > 0) {
                            focusRequesters[i - 1].requestFocus()
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .size(48.dp, 56.dp)
                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                    .focusRequester(focusRequesters[i]),
                enabled = enabled,
                textStyle = LocalTextStyle().copy(textAlign = TextAlign.Center, fontSize = 22.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
fun RegisterCodeEntryScreen(
    email: String,
    onVerified: () -> Unit,
    onResend: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(List(6) { "" }) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var triggerSubmit by remember { mutableStateOf(false) }
    val focusRequesters = List(6) { remember { androidx.compose.ui.focus.FocusRequester() } }
    val showSuccess = success
    var transientError by remember { mutableStateOf(false) }
    val shakeTrigger = remember { mutableIntStateOf(0) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeTrigger.intValue > 0) 12f else 0f,
        animationSpec = keyframes {
            durationMillis = 300
            0f at 0
            -12f at 50
            12f at 100
            -12f at 150
            12f at 200
            0f at 300
        }, label = "shakeOffset"
    )

    // Autofocus first input on mount
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    // Autosubmit when code is complete
    LaunchedEffect(code) {
        if (code.all { it.length == 1 } && code.joinToString("").matches(Regex("^[0-9]{6}$"))) {
            triggerSubmit = true
        }
    }

    // Animate on success/error
    LaunchedEffect(success, errorMessage) {
        if (success) {
            delay(1200)
            onVerified()
        } else if (errorMessage != null) {
            transientError = true
            shakeTrigger.intValue++ // Triggers shake
            delay(3000)
            transientError = false
        }
    }

    // Submit logic (manual or autosubmit)
    LaunchedEffect(triggerSubmit) {
        if (triggerSubmit) {
            loading = true
            errorMessage = null
            delay(800)
            try {
                AuthAPI.verifyEmail(code.joinToString("").toInt())
                success = true
            } catch (e: ClientRequestException) {
                e.printStackTrace()
                errorMessage = "Invalid code"
                success = false
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unknown error"
                success = false
            }
            loading = false
            triggerSubmit = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Email Verification", fontSize = 20.sp, modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
        Text("Enter the 6-digit code sent to $email", modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier
                .offset { IntOffset(shakeOffset.roundToInt(), 0) },
            horizontalArrangement = Arrangement.Center
        ) {
            SixDigitCodeInput(
                code = code,
                onCodeChange = { idx, value -> code = code.toMutableList().also { it[idx] = value } },
                enabled = !loading,
                focusRequesters = focusRequesters,
                showSuccess = showSuccess,
                showError = transientError
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { scope.launch { onResend() } },
                enabled = !loading
            ) { Text("Resend") }
        }
        if (errorMessage != null) {
            Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
        if (loading) {
            CircularProgressIndicator(Modifier.padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(topBar: @Composable (TopAppBarScrollBehavior) -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        val snackbarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        var currentEmail: String? by remember { mutableStateOf(null) }
        var currentUsername: String? by remember { mutableStateOf(null) }
        var currentPassword: String? by remember { mutableStateOf(null) }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = { topBar(scrollBehavior) }
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_LOADING
                ) {
                    composable(ROUTE_LOADING) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LoadingIndicator(Modifier.size(75.dp))
                        }
                        LaunchedEffect(Unit) {
                            try {
                                if (AuthAPI.isLoggedIn()) {
                                    navController.navigate(ROUTE_ACCOUNT) {
                                        popUpTo(ROUTE_LOADING) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(ROUTE_LOGIN) {
                                        popUpTo(ROUTE_LOADING) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                snackbarHostState.showSnackbar(
                                    "Unknown error while loading"
                                )
                                navController.navigate(ROUTE_LOGIN) {
                                    popUpTo(ROUTE_LOADING) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(ROUTE_LOGIN) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(ROUTE_ACCOUNT) {
                                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                                }
                            },
                            onRegister = {
                                navController.navigate(ROUTE_REGISTER)
                            }
                        )
                    }
                    composable(ROUTE_REGISTER) {
                        RegisterScreen(
                            onRegisterSuccess = { _email, _username, _password ->
                                currentEmail = _email
                                currentUsername = _username
                                currentPassword = _password
                                navController.navigate("code_entry/$currentEmail") {
                                    popUpTo(ROUTE_REGISTER) { inclusive = true }
                                }
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = ROUTE_CODE_ENTRY,
                        arguments = listOf(
                            navArgument("email") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        RegisterCodeEntryScreen(
                            email = email,
                            onVerified = {
                                scope.launch {
                                    try {
                                        AuthAPI.login(currentUsername!!, currentPassword!!)

                                        currentEmail = null
                                        currentUsername = null
                                        currentPassword = null

                                        navController.navigate(ROUTE_ACCOUNT) {
                                            popUpTo(ROUTE_CODE_ENTRY) { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        navController.navigate(ROUTE_LOGIN) {
                                            popUpTo(ROUTE_CODE_ENTRY) { inclusive = true }
                                        }
                                    }
                                }
                            },
                            onResend = {
                                try {
                                    AuthAPI.sendVerifyEmail(email)
                                    snackbarHostState.showSnackbar(
                                        "Resent successfully"
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar(
                                        "Couldn't resend"
                                    )
                                }
                            }
                        )
                    }
                    composable(ROUTE_ACCOUNT) {
                        AccountScreen(
                            snackbarHostState = snackbarHostState,
                            onLogoutSuccess = {
                                Log.d("Account", "Logged out")
                                navController.navigate(ROUTE_LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}