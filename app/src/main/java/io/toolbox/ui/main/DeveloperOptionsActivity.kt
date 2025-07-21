package io.toolbox.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.toolbox.BaseActivity
import io.toolbox.BuildConfig
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.ui.AppTheme
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SwitchListItem
import ru.morozovit.android.utils.ui.verticalScroll

class DeveloperOptionsActivity: BaseActivity(authEnabled = false) {
    @Suppress("DIVISION_BY_ZERO")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DeveloperOptionsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.developer_options),
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
                        .verticalScroll()
                ) {
                    Category {
                        var replacePhotosWithIntruderSwitch by remember {
                            mutableStateOf(Settings.Developer.replacePhotosWithIntruder)
                        }

                        ListItem(
                            headline = "Crash the app",
                            supportingText = "Forcefully crash the app to test crash reports",
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null
                                )
                            },
                            onClick = { 0 / 0 },
                            materialDivider = true
                        )
                        ListItem(
                            headline = "Green screen",
                            supportingText = "Display a green screen",
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Screenshot,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                startActivity(
                                    Intent(this@DeveloperOptionsActivity, GreenScreenActivity::class.java)
                                )
                            },
                            materialDivider = true
                        )
                        SwitchListItem(
                            headline = "Replace photos with intruder photos",
                            supportingText = "All photos will be replaced with intruders",
                            checked = replacePhotosWithIntruderSwitch,
                            onCheckedChange = {
                                replacePhotosWithIntruderSwitch = it
                                Settings.Developer.replacePhotosWithIntruder = it
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Photo,
                                    contentDescription = null
                                )
                            },
                            enabled = BuildConfig.DEBUG
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
            DeveloperOptionsScreen()
        }
    }
}