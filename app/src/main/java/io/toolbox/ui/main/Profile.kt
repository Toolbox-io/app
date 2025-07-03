@file:Suppress("NOTHING_TO_INLINE")

package io.toolbox.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.hazeSource
import io.toolbox.ui.LocalHazeState
import io.toolbox.ui.WindowInsetsHandler
import io.toolbox.ui.account.AuthAPI
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.SecureTextField
import ru.morozovit.android.verticalScroll

@Composable
inline fun LoginScreen() {
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

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordHidden by remember { mutableStateOf(true) }

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

        Button(onClick = {}) {
            Text("Log in")
        }
    }
}

@Composable
inline fun RegisterScreen() {}

@Composable
inline fun AccountScreen() {

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                var page by remember { mutableIntStateOf(0) }

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
                        3 -> AccountScreen()
                    }
                }

                LaunchedEffect(Unit) {
                    page = if (AuthAPI.isLoggedIn()) 3 else 1
                }
            }
        }
    }
}