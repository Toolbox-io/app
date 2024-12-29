package ru.morozovit.ultimatesecurity.ui.customization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.morozovit.android.ToggleIconButton
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.Tiles.sleep
import ru.morozovit.ultimatesecurity.services.tiles.SleepTile
import ru.morozovit.ultimatesecurity.ui.AppThemeIfNessecary
import ru.morozovit.ultimatesecurity.ui.PhonePreview

@OptIn(ExperimentalLayoutApi::class)
@PhonePreview
@Composable
fun TilesScreen() {
    AppThemeIfNessecary {
        val (_, _, _, valueOrTrue) = previewUtils()

        FlowRow(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var checked by remember {
                        mutableStateOf(
                            valueOrTrue {
                                SleepTile.instance?.enabled ?: true
                            }
                        )
                    }
                    var enabled by remember {
                        mutableStateOf(
                            valueOrTrue { sleep }
                        )
                    }

                    Box(
                        Modifier
                            .padding(bottom = 10.dp)
                            .onFocusChanged {
                                checked = valueOrTrue {
                                    SleepTile.instance?.enabled ?: true
                                }
                            }
                    ) {
                        ToggleIconButton(
                            checked = checked,
                            onCheckedChange = { },
                            enabled = enabled
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = stringResource(R.string.sleep),
//                        modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(stringResource(R.string.sleep))

                    var switchChecked by remember {
                        mutableStateOf(
                            valueOrTrue { sleep }
                        )
                    }

                    Switch(
                        checked = switchChecked,
                        onCheckedChange = {
                            switchChecked = it
                            sleep = it
                            checked = true
                            enabled = it
                        }
                    )
                }
            }
        }
    }
}