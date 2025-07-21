package io.toolbox.ui.protection.actions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.LaunchedEffect
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
import io.toolbox.Settings
import io.toolbox.ui.AppTheme
import io.toolbox.ui.protection.actions.intruderphoto.IntruderPhotoSettingsActivity
import ru.morozovit.android.utils.ActivityLauncher
import ru.morozovit.android.utils.activityResultLauncher
import ru.morozovit.android.utils.ui.copy
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.CategoryDefaults
import ru.morozovit.android.utils.ui.SeparatedSwitchListItem
import ru.morozovit.android.utils.ui.Siren
import ru.morozovit.android.utils.ui.verticalScroll

class ActionsActivity: BaseActivity() {
    private lateinit var activityLauncher: ActivityLauncher

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun ActionsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.actions),
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
                        var alarm by remember {
                            mutableStateOf(
                                Settings.UnlockProtection.Alarm.enabled
                            )
                        }
                        SeparatedSwitchListItem(
                            headline = stringResource(R.string.alarm),
                            supportingText = stringResource(R.string.alarm_d),
                            checked = alarm,
                            onCheckedChange = {
                                alarm = it
                                Settings.UnlockProtection.Alarm.enabled = it
                            },
                            bodyOnClick = {
                                activityLauncher.launch(
                                    Intent(
                                        this@ActionsActivity,
                                        AlarmSettingsActivity::class.java
                                    )
                                ) {
                                    alarm = Settings.UnlockProtection.Alarm.enabled
                                }
                            },
                            materialDivider = true,
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Siren,
                                    contentDescription = null
                                )
                            }
                        )

                        var intruderPhoto by remember {
                            mutableStateOf(
                                Settings.UnlockProtection.IntruderPhoto.enabled
                            )
                        }

                        LaunchedEffect(Unit) {
                            if (checkSelfPermission(Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
                                intruderPhoto = false
                                Settings.UnlockProtection.IntruderPhoto.enabled = false
                            }
                        }

                        SeparatedSwitchListItem(
                            headline = stringResource(R.string.intruderphoto),
                            supportingText = stringResource(R.string.intruderphoto_d),
                            checked = intruderPhoto,
                            onCheckedChange = {
                                if (it) {
                                    if (checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
                                        intruderPhoto = true
                                    } else {
                                        requestPermission(Manifest.permission.CAMERA) { granted ->
                                            if (granted) {
                                                intruderPhoto = true
                                            }
                                        }
                                    }
                                } else {
                                    intruderPhoto = false
                                }
                                Settings.UnlockProtection.IntruderPhoto.enabled = it
                            },
                            bodyOnClick = {
                                activityLauncher.launch(
                                    Intent(
                                        this@ActionsActivity,
                                        IntruderPhotoSettingsActivity::class.java
                                    )
                                ) {
                                    intruderPhoto = Settings.UnlockProtection.IntruderPhoto.enabled
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
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
        super.onCreate(null)
        activityLauncher = activityResultLauncher
        enableEdgeToEdge()
        setContent {
            ActionsScreen()
        }
    }
}