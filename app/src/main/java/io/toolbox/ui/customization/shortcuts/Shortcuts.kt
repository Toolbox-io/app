package io.toolbox.ui.customization.shortcuts

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.widget.Toast
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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.toolbox.R
import ru.morozovit.android.utils.ui.Mipmap
import ru.morozovit.android.utils.ui.TextButton
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll

private const val FILES_SHORTCUT = "files-shortcut"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortcutsScreen(EdgeToEdgeBar: @Composable (@Composable (PaddingValues) -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar { innerPadding ->
            val context = LocalContext()
            val isShortcutsSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

            if (isShortcutsSupported) {
                Box(
                    Modifier
                        .verticalScroll()
                        .padding(innerPadding)
                ) {
                    FlowRow(Modifier.padding(10.dp)) {
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
                                        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                                            ShortcutManagerCompat.requestPinShortcut(
                                                context,
                                                ShortcutInfoCompat.Builder(context, FILES_SHORTCUT)
                                                    .setShortLabel(context.resources.getString(R.string.files))
                                                    .setLongLabel(context.resources.getString(R.string.files))
                                                    .setIcon(IconCompat.createWithResource(context, R.mipmap.files_icon))
                                                    .setIntent(
                                                        Intent(context, FilesShortcut::class.java).apply {
                                                            action = ACTION_MAIN
                                                        }
                                                    )
                                                    .build(),
                                                null
                                            )
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.shortcuts_unsuported1,
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                    Text(stringResource(R.string.shortcuts_unsuported1))
                }
            }
        }
    }
}