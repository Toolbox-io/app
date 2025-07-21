package io.toolbox.ui.tools.notificationhistory

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.Settings.NotificationHistory.removeDuplicates
import io.toolbox.Settings.NotificationHistory.removeUselessNotifications
import io.toolbox.ui.AppTheme
import ru.morozovit.android.utils.ui.copy
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.CategoryDefaults
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SwitchListItem
import ru.morozovit.android.utils.ui.verticalScroll

class NotificationHistorySettingsActivity: BaseActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NotificationHistorySettingsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.settings),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(),
                ) {
                    Category(margin = CategoryDefaults.margin.copy(top = 16.dp)) {
                        var removeDuplicatesSwitch by remember { mutableStateOf(removeDuplicates) }

                        SwitchListItem(
                            headline = stringResource(R.string.remove_duplicates),
                            supportingText = stringResource(R.string.remove_duplicates_d),
                            checked = removeDuplicatesSwitch,
                            onCheckedChange = {
                                removeDuplicatesSwitch = it
                                removeDuplicates = it
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null
                                )
                            },
                            materialDivider = true
                        )

                        var removeUselessNotificationsSwitch by remember { mutableStateOf(removeUselessNotifications) }

                        SwitchListItem(
                            headline = stringResource(R.string.remove_useless_notifications),
                            supportingText = stringResource(R.string.remove_useless_notifications_d),
                            checked = removeUselessNotificationsSwitch,
                            onCheckedChange = {
                                removeUselessNotificationsSwitch = it
                                removeUselessNotifications = it
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null
                                )
                            },
                            materialDivider = true
                        )

                        ListItem(
                            headline = stringResource(R.string.select_apps),
                            supportingText = stringResource(R.string.select_apps_2_d),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@NotificationHistorySettingsActivity,
                                        SelectAppsActivity::class.java,
                                    )
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Apps,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotificationHistorySettingsScreen()
        }
    }
}