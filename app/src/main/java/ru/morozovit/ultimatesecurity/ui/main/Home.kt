package ru.morozovit.ultimatesecurity.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.launch
import ru.morozovit.android.async
import ru.morozovit.android.invoke
import ru.morozovit.android.runOrLog
import ru.morozovit.ultimatesecurity.App
import ru.morozovit.ultimatesecurity.App.Companion.githubRateLimitRemaining
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DOWNLOAD_BROADCAST
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.DownloadBroadcastReceiver
import ru.morozovit.ultimatesecurity.services.UpdateChecker.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.MainActivity
import ru.morozovit.ultimatesecurity.ui.WindowInsetsHandler
import java.io.BufferedInputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URL
import javax.net.ssl.HttpsURLConnection


data class Story(
    val title: String,
    val type: String,
    val image: ImageBitmap?,
    val parts: List<Part>,
    val time: Long,
    val id: String
): Serializable {
    companion object {
        const val TYPE_TUTORIAL = "tutorial"
        const val TYPE_LIFEHACK = "lifehack"

        @Suppress("MayBeConstant")
        @JvmField
        val serialVersionUID = 1230581135718153350L

        fun read(file: File): Story {
            return ObjectInputStream(file.inputStream()).use {
                it.readObject() as Story
            }
        }
    }

    fun write(file: File) {
        file.parentFile!!.mkdirs()
        file.createNewFile()
        ObjectOutputStream(file.outputStream()).use {
            it.writeObject(this)
        }
    }

    data class Part(
        val file: URL,
        val caption: String
    ) : Serializable
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(EdgeToEdgeBar: @Composable (@Composable () -> Unit) -> Unit) {
    SharedTransitionLayout {
        val navController = rememberNavController()
        val context = LocalContext() as MainActivity
        val configuration = LocalConfiguration()
        val coroutineScope = rememberCoroutineScope()

        val stories = remember { mutableStateMapOf<String, Story>() }

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                WindowInsetsHandler {
                    EdgeToEdgeBar {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            // Stories
                            LaunchedEffect(Unit) {
                                async {
                                    runOrLog("Stories") {
                                        with(context) {
                                            fun resolveLocalizedString(s: JsonObject): String {
                                                return if (configuration.locales[0].language == "ru") {
                                                    String(
                                                        s["ru"]
                                                            .asString
                                                            .toByteArray(Charsets.ISO_8859_1),
                                                        Charsets.UTF_8
                                                    )
                                                } else {
                                                    s["en"].asString
                                                }
                                            }

                                            fun download(url: String): String? {
                                                val request = URL(url).openConnection() as HttpsURLConnection
                                                request.requestMethod = "GET";

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
                                                        return String(chars.toCharArray())
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
                                                        Log.d("Download", "Error response: $response")
                                                        error("")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.d("Download", "Error. \n${ru.morozovit.utils.EParser(e)}")
                                                    return null
                                                } finally {
                                                    request.disconnect()
                                                }
                                            }

                                            fun getContents(
                                                dir: String,
                                                isFullUrl: Boolean = false
                                            ): JsonElement? {
                                                val request = URL(
                                                    if (isFullUrl) {
                                                        dir
                                                    } else {
                                                        "https://api.github.com/repos/denis0001-dev/Toolbox-io/contents/$dir"
                                                    }
                                                ).openConnection() as HttpsURLConnection
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
                                                        return JsonParser.parseString(response)
                                                    } else {
                                                        Log.d("Stories", "Error. HTTP response code: ${request.responseCode}")
                                                        val errorInput = request.errorStream!!
                                                        var c: Char;

                                                        val chars: MutableList<Char> = mutableListOf()

                                                        while (true) {
                                                            c = errorInput.read().toChar()
                                                            if (c == 0.toChar() || c == '\uFFFF') break;
                                                            chars.add(c)
                                                        }
                                                        val response = String(chars.toCharArray())
                                                        Log.d("Stories", "Error response: $response")
                                                        error("")
                                                    }
                                                } catch (e: Exception) {
                                                    Log.d("Stories", "Error. \n${ru.morozovit.utils.EParser(e)}")
                                                    return null
                                                } finally {
                                                    runCatching {
                                                        githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
                                                    }
                                                    request.disconnect()
                                                }
                                            }

                                            Log.d("Stories", "Loading stories")
                                            // get cached stories
                                            File(cacheDir.absolutePath + "/stories").listFiles()?.forEach {
                                                runOrLog("Stories") {
                                                    val story = Story.read(it)
                                                    stories[story.id] = story
                                                    Log.i("Stories", "Story loaded from cache:\n$story")
                                                }
                                            }

                                            // download new stories
                                            val contents = getContents("stories")
                                            contents?.asJsonArray?.forEach {
                                                runOrLog("Stories") {
                                                    val entry = it.asJsonObject
                                                    if (entry["type"].asString != "file") {
                                                        val id =
                                                            entry["url"]
                                                                .asString
                                                                .let { s ->
                                                                    s.substring(s.lastIndexOf('/') + 1)
                                                                }
                                                                .let { s ->
                                                                    s.substring(0, s.lastIndexOf('?'))
                                                                }
                                                        val metadata = getContents(
                                                            dir = entry["url"]
                                                                .asString
                                                                .let { s ->
                                                                    s.substring(0, s.lastIndexOf('?'))
                                                                }
                                                                    + "/metadata.json",
                                                            isFullUrl = true
                                                        )?.asJsonObject
                                                        if (metadata != null) {
                                                            val jsonStr = download(metadata["download_url"].asString)
                                                            if (jsonStr != null) {
                                                                val json = JsonParser.parseString(jsonStr) as JsonObject
                                                                val title = resolveLocalizedString(json["title"].asJsonObject)
                                                                val type = json["type"].asString
                                                                val time = json["time"].asLong
                                                                val parts = mutableListOf<Story.Part>()
                                                                json["timestamps"].asJsonArray.forEach { p ->
                                                                    val part = p.asJsonObject
                                                                    val file = URL(
                                                                        getContents(
                                                                            dir = "${
                                                                            entry["url"].asString.let { s ->
                                                                                s.substring(0, s.lastIndexOf('?'))
                                                                            }
                                                                        }/${
                                                                            part["file"].asString
                                                                        }",
                                                                            isFullUrl = true
                                                                        )!!.asJsonObject["download_url"].asString
                                                                    )
                                                                    val caption = resolveLocalizedString(part["caption"].asJsonObject)
                                                                    parts.add(Story.Part(file, caption))
                                                                }
                                                                val story = Story(
                                                                    id = id,
                                                                    title = title,
                                                                    type = type,
                                                                    parts = parts,
                                                                    time = time,
                                                                    image = null
                                                                )
                                                                stories[story.id] = story
                                                                Log.i("Stories", "Story loaded from internet:\n$story")

                                                                runOrLog("Stories") {
                                                                    story.write(File(cacheDir.absolutePath + "/stories/$id.dat"))
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
                            LazyRow(
                                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val sortedStories = stories.values.sortedBy { it.time }

                                items(sortedStories) {
                                    with(this@SharedTransitionLayout) {
                                        Box(
                                            modifier = Modifier
                                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(30.dp))
                                                .size(100.dp)
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(key = "story-${it.id}"),
                                                    animatedVisibilityScope = this@composable
                                                )
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(30.dp))
                                                    .clickable {
                                                        navController.navigate("story/${it.id}")
                                                    }
                                            ) {
                                                Box(Modifier.padding(16.dp)) {
                                                    Text(
                                                        text = it.title,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis,
                                                        fontSize = 14.sp,
                                                        lineHeight = 17.sp,
                                                        modifier = Modifier.align(Alignment.BottomStart)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
            composable(
                route = "story/{id}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                with(this@SharedTransitionLayout) {
                    val id = backStackEntry.arguments!!.getString("id")
                    val story = stories[id]!!
                    AppTheme(
                        darkTheme = true,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "story-$id"),
                            animatedVisibilityScope = this@composable
                        )
                    ) {
                        Surface(color = Color.Black) {
                            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                                val pagerState = rememberPagerState {
                                    story.parts.size
                                }
                                var player: ExoPlayer? by remember { mutableStateOf(null) }
                                HorizontalPager(state = pagerState) { page ->
                                    VideoPlayer(
                                        mediaItems = listOf(
                                            VideoPlayerMediaItem.NetworkMediaItem(
                                                url = story.parts[page].file.toString(),
                                                mimeType = MimeTypes.APPLICATION_MP4,
                                            )
                                        ),
                                        handleLifecycle = true,
                                        autoPlay = true,
                                        usePlayerController = true,
                                        enablePip = false,
                                        handleAudioFocus = false,
                                        controllerConfig = VideoPlayerControllerConfig(
                                            showSpeedAndPitchOverlay = false,
                                            showSubtitleButton = false,
                                            showCurrentTimeAndTotalTime = false,
                                            showBufferingProgress = false,
                                            showForwardIncrementButton = false,
                                            showBackwardIncrementButton = false,
                                            showBackTrackButton = false,
                                            showNextTrackButton = false,
                                            showRepeatModeButton = false,
                                            controllerShowTimeMilliSeconds = 5_000,
                                            controllerAutoShow = false,
                                            showFullScreenButton = false
                                        ),
                                        volume = 1f,
                                        repeatMode = RepeatMode.NONE,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .align(Alignment.Center),
                                        playerInstance = {
                                            player = this
                                            addListener(
                                                object: Player.Listener {
                                                    override fun onPlaybackStateChanged(state: Int) {
                                                        super.onPlaybackStateChanged(playbackState)
                                                        if (state == ExoPlayer.STATE_ENDED && page == pagerState.currentPage) {
                                                            if (pagerState.currentPage + 1 < story.parts.size) {
                                                                coroutineScope.launch {
                                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                                }
                                                            } else {
                                                                navController.navigateUp()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    story.parts.forEachIndexed { i, _ ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .height(8.dp)
                                                .weight(1f),
                                            onClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(i)
                                                }
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(
                                                        when {
                                                            pagerState.currentPage > i -> 1f
                                                            player != null -> try {
                                                                player!!.duration / player!!.currentPosition
                                                            } catch (e: ArithmeticException) {
                                                                0
                                                            }.toFloat()
                                                            else -> 0f
                                                        }
                                                    )
                                                    .animateContentSize()
                                                    .background(MaterialTheme.colorScheme.onSurface)
                                            )
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
}