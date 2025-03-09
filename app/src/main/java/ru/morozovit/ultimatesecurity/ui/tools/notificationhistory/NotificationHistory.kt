package ru.morozovit.ultimatesecurity.ui.tools.notificationhistory

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ru.morozovit.android.invoke
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(actions: @Composable RowScope.() -> Unit, navigation: @Composable () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    WindowInsetsHandler {
        with (LocalContext()) {
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.notification_history),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = navigation,
                        actions = {
                            // TODO add actions
                            actions()
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { innerPadding ->
                // TODO implement
            }
        }
    }
}