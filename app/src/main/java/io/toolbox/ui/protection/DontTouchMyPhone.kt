package io.toolbox.ui.protection

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.DontTouchMyPhoneService
import io.toolbox.services.DontTouchMyPhoneService.Companion.working
import io.toolbox.ui.EdgeToEdgeBarType
import io.toolbox.ui.protection.actions.ActionsActivity
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.Charger
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.ui.SwitchListItem
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke

@Composable
fun DontTouchMyPhoneScreen(EdgeToEdgeBar: EdgeToEdgeBarType) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                val context = LocalContext()

                var useSensors by remember { mutableStateOf(Settings.DTMP.useSensors) }
                var triggerOnCharger by remember { mutableStateOf(Settings.DTMP.triggerOnCharger) }

                Category(title = stringResource(R.string.settings)) {
                    SwitchListItem(
                        headline = stringResource(R.string.use_sensors),
                        supportingText = stringResource(R.string.use_sensors_d),
                        checked = useSensors,
                        onCheckedChange = {
                            Settings.DTMP.useSensors = it
                            useSensors = it
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null
                            )
                        },
                        materialDivider = true
                    )
                    SwitchListItem(
                        headline = stringResource(R.string.trigger_on_charger),
                        supportingText = stringResource(R.string.trigger_on_charger_d),
                        checked = triggerOnCharger,
                        onCheckedChange = {
                            Settings.DTMP.triggerOnCharger = it
                            triggerOnCharger = it
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Charger,
                                contentDescription = null
                            )
                        }
                    )
                }

                Category {
                    ListItem(
                        headline = stringResource(R.string.actions),
                        supportingText = stringResource(R.string.actions_d),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    ActionsActivity::class.java
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null
                            )
                        }
                    )
                }

                Button(
                    onClick = {
                        if (working) {
                            DontTouchMyPhoneService.stop()
                        } else {
                            DontTouchMyPhoneService.start(context)
                        }
                    },
                    enabled = useSensors || triggerOnCharger,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (working)
                            stringResource(R.string.stop)
                        else
                            stringResource(R.string.start)
                    )
                }
            }
        }
    }
}