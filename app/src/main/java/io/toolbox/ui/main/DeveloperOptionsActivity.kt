package io.toolbox.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Screenshot
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.ui.AppTheme
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem

class DeveloperOptionsActivity: BaseActivity(authEnabled = false) {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Category {
                        ListItem(
                            headline = "Crash the app",
                            supportingText = "Forcefully crash the app to test crash reports",
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                // Forcefully crash the app
                                @Suppress("DIVISION_BY_ZERO")
                                0 / 0
                            },
                            divider = true,
                            dividerThickness = 2.dp,
                            dividerColor = MaterialTheme.colorScheme.surface
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
            DeveloperOptionsScreen()
        }
    }
}