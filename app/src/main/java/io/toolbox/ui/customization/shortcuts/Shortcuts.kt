package io.toolbox.ui.customization.shortcuts

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import io.toolbox.R
import io.toolbox.ui.LocalHazeState
import io.toolbox.ui.WindowInsetsHandler
import ru.morozovit.android.getSystemService
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Mipmap
import ru.morozovit.android.ui.TextButton
import ru.morozovit.android.verticalScroll

private const val FILES_SHORTCUT = "files-shortcut"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutsScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val context = LocalContext()

                Box(
                    Modifier
                        .verticalScroll()
                        .padding(innerPadding)
                        .hazeSource(LocalHazeState())
                ) {
                    FlowRow(
                        Modifier
                            .padding(10.dp)
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
                                Box(Modifier.padding(bottom = 5.dp)) {
                                    Mipmap(
                                        id = R.mipmap.files_icon,
                                        contentDescription = stringResource(R.string.files),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .size(60.dp)
                                    )
                                }
                                Text(stringResource(R.string.files))

                                TextButton(
                                    onClick = {
                                        val shortcutManager = context.getSystemService(ShortcutManager::class)!!
                                        if (shortcutManager.isRequestPinShortcutSupported) {
                                            val shortcutInfo = ShortcutInfo.Builder(
                                                context, FILES_SHORTCUT
                                            )
                                                .setShortLabel(context.resources.getString(R.string.files))
                                                .setLongLabel(context.resources.getString(R.string.files))
                                                .setIcon(Icon.createWithResource(context, R.mipmap.files_icon))
                                                .setIntent(
                                                    Intent(context, FilesShortcut::class.java).apply {
                                                        action = ACTION_MAIN
                                                    }
                                                )
                                                .build()
                                            shortcutManager.requestPinShortcut(shortcutInfo, null)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = stringResource(R.string.add)
                                        )
                                    }
                                ) {
                                    Text(stringResource(R.string.add))
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.shortcuts_unsuported))
                }
            }
        }
    }
}