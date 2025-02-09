package ru.morozovit.ultimatesecurity.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.App.Companion.githubRateLimitRemaining
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import java.io.BufferedInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class Story(
    val title: String,
    val image: ImageBitmap,
    val id: String
)

@Composable
fun HomeScreen(EdgeToEdgeBar: @Composable (@Composable () -> Unit) -> Unit) {
    WindowInsetsHandler {
        EdgeToEdgeBar {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val context = LocalContext() as MainActivity

                // Stories
                val stories = mutableListOf<Story>()
                LaunchedEffect(Unit) {
                    async {
                        with(context) {
                            fun download(url: String): String? {
                                TODO("implement!!")
                            }

                            fun getContents(dir: String): JsonObject? {
                                val request = URL("https://api.github.com/repos/denis0001-dev/Toolbox-io/contents/$dir")
                                    .openConnection() as HttpsURLConnection
                                request.requestMethod = "GET";
                                request.setRequestProperty("Accept", "application/vnd.github+json")
                                request.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                                request.setRequestProperty("Authorization", "Bearer ${App.GITHUB_TOKEN}")

                                try {
                                    request.connect()
                                    if (request.responseCode == 200) {
                                        val input = BufferedInputStream(request.inputStream)
                                        var c: Char;

                                        val chars: MutableList<Char> = mutableListOf()

                                        while (true) {
                                            c = input.read().toChar()
                                            if (c == 0.toChar() || c == '\uFFFF') break;
                                            chars.add(c)
                                        }
                                        val response = String(chars.toCharArray())
                                        return JsonParser.parseString(response) as JsonObject
                                    } else {
                                        Log.d("IssueReporter", "Error. HTTP response code: ${request.responseCode}")
                                        val errorInput = request.errorStream!!
                                        var c: Char;

                                        val chars: MutableList<Char> = mutableListOf()

                                        while (true) {
                                            c = errorInput.read().toChar()
                                            if (c == 0.toChar() || c == '\uFFFF') break;
                                            chars.add(c)
                                        }
                                        val response = String(chars.toCharArray())
                                        Log.d("IssueReporter", "Error response: $response")
                                        error("")
                                    }
                                } catch (e: Exception) {
                                    Log.d("IssueReporter", "Error. \n${ru.morozovit.utils.EParser(e)}")
                                    return null
                                } finally {
                                    runCatching {
                                        githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
                                    }
                                    request.disconnect()
                                }
                            }

                            Log.d("Stories", "Loading stories")
                            val contents = getContents("stories")
                            if (contents != null) {
                                contents["entries"].asJsonArray.forEach {
                                    val entry = it.asJsonObject
                                    val metadata = getContents(entry["url"].asString + "/metadata.json")

                                }
                            }
                        }
                    }
                }
                LazyRow {

                }

                // UPDATE
                if (!update_dsa) {
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

                // TODO add some content
            }
        }
    }
}