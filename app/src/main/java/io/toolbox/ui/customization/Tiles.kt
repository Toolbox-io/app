package io.toolbox.ui.customization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import dev.chrisbanes.haze.hazeSource
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.services.tiles.SleepTile
import io.toolbox.ui.LocalHazeState
import io.toolbox.ui.WindowInsetsHandler
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.ToggleIconButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TilesScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            FlowRow(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .hazeSource(LocalHazeState())
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
                                SleepTile.instance?.enabled != false
                            )
                        }
                        var enabled by remember {
                            mutableStateOf(
                                Settings.Tiles.sleep
                            )
                        }

                        Box(
                            Modifier
                                .padding(bottom = 10.dp)
                                .onFocusChanged {
                                    checked = SleepTile.instance?.enabled != false
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
                                )
                            }
                        }

                        Text(stringResource(R.string.sleep))

                        var switchChecked by remember {
                            mutableStateOf(
                                Settings.Tiles.sleep
                            )
                        }

                        Switch(
                            checked = switchChecked,
                            onCheckedChange = {
                                switchChecked = it
                                Settings.Tiles.sleep = it
                                checked = true
                                enabled = it
                            }
                        )
                    }
                }
            }
        }
    }
}