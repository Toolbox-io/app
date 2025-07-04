@file:Suppress("NOTHING_TO_INLINE", "ArrayInDataClass")

package io.toolbox.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.hazeSource
import io.ktor.client.plugins.ResponseException
import io.toolbox.R
import io.toolbox.Settings.Account.token
import io.toolbox.api.AuthAPI
import io.toolbox.api.errorMessage
import io.toolbox.ui.LocalHazeState
import io.toolbox.ui.WindowInsetsHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.android.ui.SecureTextField
import ru.morozovit.android.verticalScroll

private var page by mutableIntStateOf(0)

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
private inline fun LoginScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(LocalHazeState())
            .verticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val scope = rememberCoroutineScope()

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
            label = { Text("Username") },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SecureTextField(
            value = password,
            onValueChange = { password = it },
            hidden = passwordHidden,
            onHiddenChange = { passwordHidden = it },
            label = { Text("Password") },
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
                            token = AuthAPI.login(username, password)
                            page = 3
                        } catch (e: ResponseException) {
                            showError(e.errorMessage())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showError("Internal error")
                        } finally {
                            isLoggingIn = false
                        }
                    }
                }
            ) {
                Text("Log in")
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
    }
}

@Composable
private inline fun RegisterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(LocalHazeState())
            .verticalScroll(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Register",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TODO register screen
    }
}

@Composable
private inline fun AccountScreen(snackbarHostState: SnackbarHostState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(LocalHazeState())
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
                divider = true,
                dividerColor = MaterialTheme.colorScheme.surface,
                dividerThickness = 2.dp,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null
                    )
                },
                //placeholder = loading
            )
            ListItem(
                headline = stringResource(R.string.email),
                supportingText = email,
                divider = true,
                dividerColor = MaterialTheme.colorScheme.surface,
                dividerThickness = 2.dp,
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null
                    )
                },
                //placeholder = loading
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
                //placeholder = loading
            )
        }

        Category(title = "Account actions") {
            ListItem(
                headline = stringResource(R.string.logout),
                onClick = {
                    token = "" // Clear the token
                    page = 1
                },
                divider = true,
                dividerColor = MaterialTheme.colorScheme.surface,
                dividerThickness = 2.dp,
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                },
                //placeholder = loading
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
                //placeholder = loading
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(topBar: @Composable (TopAppBarScrollBehavior) -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = { topBar(scrollBehavior) }
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                AnimatedContent(page) {
                    when (it) {
                        0 /* loading */ -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LoadingIndicator(Modifier.size(75.dp))
                            }
                        }
                        1 -> LoginScreen()
                        2 -> RegisterScreen()
                        3 -> AccountScreen(snackbarHostState)
                    }
                }

                LaunchedEffect(Unit) {
                    try {
                        page = if (AuthAPI.isLoggedIn()) 3 else 1
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar(
                            "Unknown error while loading"
                        )
                    }
                }
            }
        }
    }
}