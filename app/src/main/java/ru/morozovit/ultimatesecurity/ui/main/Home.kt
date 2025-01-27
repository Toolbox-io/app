package ru.morozovit.ultimatesecurity.ui.main

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler

@Composable
@PhonePreview
fun HomeScreen() {
    WindowInsetsHandler {
        Box {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val (valueOrFalse, runOrNoop) = previewUtils()
                val context = LocalContext() as MainActivity

                // UPDATE
                if (valueOrFalse { !update_dsa }) {
                    var isUpdateCardVisible by remember { mutableStateOf(false) }

                    val versionFormat = stringResource(R.string.update_version)
                    var version by remember { mutableStateOf("") }
                    var body by remember { mutableStateOf("") }
                    var downloadOnClick by remember { mutableStateOf({}) }

                    AnimatedVisibility(
                        visible = isUpdateCardVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.7f),
                        exit = fadeOut() + scaleOut(targetScale = 0.7f)
                    ) {
                        Card(
                            modifier = Modifier
                                .padding()
                                .padding(16.dp)
                                .fillMaxWidth(),
                            colors = cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.update),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                HorizontalDivider()
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                                )
                                Row {
                                    TextButton(onClick = downloadOnClick) {
                                        Text(text = stringResource(R.string.download))
                                    }
                                    TextButton(onClick = {
                                        update_dsa = true
                                        isUpdateCardVisible = false
                                    }) {
                                        Text(text = stringResource(R.string.dsa))
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        runOrNoop {
                            async {
                                runCatching {
                                    val info = checkForUpdates()!!
                                    if (info.available) {
                                        version = String.format(versionFormat, info.version)
                                        body = info.description
                                        downloadOnClick = {
                                            context.sendBroadcast(
                                                Intent(App.context, DownloadBroadcastReceiver::class.java).apply {
                                                    action = DOWNLOAD_BROADCAST
                                                    putExtra("updateInfo", info)
                                                }
                                            )
                                        }
                                        isUpdateCardVisible = true
                                    }
                                }
                            }
                        }
                    }
                }

                // TODO add some content
            }
        }
    }
}