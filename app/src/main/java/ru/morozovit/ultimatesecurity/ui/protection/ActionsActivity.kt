package ru.morozovit.ultimatesecurity.ui.protection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.morozovit.android.BetterActivityResult
import ru.morozovit.android.BetterActivityResult.registerActivityForResult
import ru.morozovit.android.copy
import ru.morozovit.android.previewUtils
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.SeparatedSwitchListItem
import ru.morozovit.android.ui.Siren
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.AlarmSettingsActivity
import ru.morozovit.ultimatesecurity.ui.protection.unlockprotection.intruderphoto.IntruderPhotoSettingsActivity

class ActionsActivity: BaseActivity() {
    private lateinit var activityLauncher: BetterActivityResult<Intent, ActivityResult>

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    @PhonePreview
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
                        .verticalScroll(rememberScrollState()),
                ) {
                    val (valueOrFalse, runOrNoop) = previewUtils()
                    val coroutineScope = rememberCoroutineScope()

                    Category(margin = CategoryDefaults.margin.copy(top = 16.dp)) {
                        var alarm by remember {
                            mutableStateOf(
                                valueOrFalse {
                                    Settings.UnlockProtection.Alarm.enabled
                                }
                            )
                        }
                        val alarmOnCheckedChange: (Boolean) -> Unit = {
                            alarm = it
                            coroutineScope.launch {
                                Settings.UnlockProtection.Alarm.enabled = it
                            }
                        }
                        SeparatedSwitchListItem(
                            headline = stringResource(R.string.alarm),
                            supportingText = stringResource(R.string.alarm_d),
                            checked = alarm,
                            onCheckedChange = alarmOnCheckedChange,
                            bodyOnClick = {
                                runOrNoop {
                                    activityLauncher.launch(
                                        Intent(
                                            this@ActionsActivity,
                                            AlarmSettingsActivity::class.java
                                        )
                                    ) {
                                        alarm = Settings.UnlockProtection.Alarm.enabled
                                    }
                                }
                            },
                            divider = true,
                            dividerThickness = 2.dp,
                            dividerColor = MaterialTheme.colorScheme.surface,
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Siren,
                                    contentDescription = null
                                )
                            }
                        )

                        var intruderPhoto by remember {
                            mutableStateOf(
                                valueOrFalse {
                                    Settings.UnlockProtection.IntruderPhoto.enabled
                                }
                            )
                        }
                        val intruderPhotoOnCheckedChange: (Boolean) -> Unit = {
                            intruderPhoto = it
                            coroutineScope.launch {
                                Settings.UnlockProtection.IntruderPhoto.enabled = it
                            }
                        }

                        SeparatedSwitchListItem(
                            headline = stringResource(R.string.intruderphoto),
                            supportingText = stringResource(R.string.intruderphoto_d),
                            checked = intruderPhoto,
                            onCheckedChange = intruderPhotoOnCheckedChange,
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
        activityLauncher = registerActivityForResult(this)
        enableEdgeToEdge()
        setContent {
            ActionsScreen()
        }
    }
}