@file:Suppress("DEPRECATION")

package ru.morozovit.ultimatesecurity.ui.main

import android.content.Intent
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Log
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ru.morozovit.android.alertDialog
import ru.morozovit.android.async
import ru.morozovit.android.canRequestPackageInstallsOrFalse
import ru.morozovit.android.invoke
import ru.morozovit.android.previewUtils
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.installPackage_dsa
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

@Composable
@PhonePreview
fun HomeScreen() {
    AppTheme {
        Box {
            var isDownloading by rememberSaveable { mutableStateOf(false) }
            var downloadProgress by rememberSaveable { mutableFloatStateOf(0f) }

            val context = LocalContext() as MainActivity
            if (isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { downloadProgress }
                )
            }

            Column(Modifier.verticalScroll(rememberScrollState())) {
                val (valueOrFalse, runOrNoop) = previewUtils()
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
                                            isDownloading = true
                                            async download@ {
                                                val file: File
                                                var count: Int
                                                try {
                                                    val url = URL(info.download)
                                                    val connection = url.openConnection()
                                                    connection.connect()

                                                    // this will be useful so that you can show a tipical 0-100%
                                                    // progress bar
                                                    val lengthOfFile = connection.contentLength

                                                    // download the file
                                                    val input: InputStream =
                                                        BufferedInputStream(
                                                            url.openStream(),
                                                            8192
                                                        )

                                                    // Output stream
                                                    file =
                                                        File(
                                                            context
                                                                .cacheDir
                                                                .absolutePath
                                                                    + "/update.apk"
                                                        )
                                                    if (file.exists()) {
                                                        file.delete()
                                                    }
                                                    file.createNewFile()

                                                    val output: OutputStream =
                                                        FileOutputStream(file)
                                                    val data = ByteArray(1024)
                                                    var total: Long = 0

                                                    while (
                                                        (
                                                                input.read(data).also {
                                                                    count = it
                                                                }
                                                                ) != -1
                                                    ) {
                                                        total += count.toLong()
                                                        // publishing the progress....
                                                        // After this onProgressUpdate will be called
                                                        downloadProgress = (
                                                                (total * 100) / lengthOfFile
                                                                ).toInt().toFloat()

                                                        // writing data to file
                                                        output.write(data, 0, count)
                                                    }

                                                    // flushing output
                                                    output.flush()

                                                    // closing streams
                                                    output.close()
                                                    input.close()
                                                } catch (e: java.lang.Exception) {
                                                    Log.e("Error: ", e.message!!)
                                                    return@download
                                                }
                                                Handler(getMainLooper()).post {
                                                    isDownloading = false
                                                    downloadProgress = 0f
                                                    fun install() {
                                                        val install =
                                                            Intent(Intent.ACTION_INSTALL_PACKAGE)
                                                        install.flags =
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        install.data =
                                                            FileProvider.getUriForFile(
                                                                context,
                                                                context.applicationContext.packageName + ".provider",
                                                                file
                                                            )
                                                        context.startActivity(install)
                                                    }

                                                    if (installPackage_dsa || context.packageManager.canRequestPackageInstallsOrFalse()) {
                                                        install()
                                                    } else {
                                                        // TODO rewrite in Jetpack Compose
                                                        context.alertDialog {
                                                            title(R.string.install_package)
                                                            message(R.string.install_package_d)
                                                            positiveButton(R.string.install, ::install)
                                                            negativeButton(R.string.cancel)
                                                            neutralButton(R.string.dsa) {
                                                                installPackage_dsa = true
                                                                install()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        isUpdateCardVisible = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}