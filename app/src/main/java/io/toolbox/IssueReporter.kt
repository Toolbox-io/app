package io.toolbox

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.postDelayed
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.toolbox.App.Companion.GITHUB_API_VERSION
import io.toolbox.App.Companion.GITHUB_TOKEN
import io.toolbox.App.Companion.context
import io.toolbox.App.Companion.githubRateLimitRemaining
import io.toolbox.ui.AppTheme
import io.toolbox.ui.MainActivity
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.copy
import ru.morozovit.android.encodeJSON
import ru.morozovit.android.getSerializableExtraAs
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.DialogActivity
import ru.morozovit.android.verticalScroll
import ru.morozovit.utils.EParser
import ru.morozovit.utils.shorten
import java.io.BufferedInputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object IssueReporter {
    var enabled = true
        set(value) {
            field = value
            Thread.setDefaultUncaughtExceptionHandler(if (value) DEFAULT_HANDLER else DISABLED_HANDLER)
        }
    private var crashes = 0
    private const val CRASHES_LIMIT = 2

    private val DEFAULT_HANDLER = { t: Thread, exception: Throwable ->
        crashes++
        if (crashes > CRASHES_LIMIT) {
            enabled = false
            DISABLED_HANDLER(t, exception)
        }
        runCatching {
            Log.e("App", "EXCEPTION CAUGHT:\n${EParser(exception)}")
        }
        startCrashedActivity(exception, context)
    }
    private val DISABLED_HANDLER = { _: Thread, _: Throwable ->
        runCatching {
            Toast.makeText(
                context,
                R.string.fatal_error,
                Toast.LENGTH_SHORT
            )
        }
        exitProcess(10)
    }

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(DEFAULT_HANDLER)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun startCrashedActivity(exception: Throwable, context: Context) {
        if (enabled) {
            val handler = Handler(Looper.getMainLooper())
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_MAIN
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            handler.postDelayed(250) {
                DialogActivity.show(
                    context = context,
                    title = context.resources.getString(R.string.im_sorry),
                    body = context.resources.getString(R.string.crash_d),
                    positiveButtonText = context.resources.getString(R.string.continue1),
                    positiveButtonOnClick = {
                        crashes--
                        finish()
                    },
                    negativeButtonText = context.resources.getString(R.string.details),
                    negativeButtonOnClick = {
                        crashes--
                        context.startActivity(
                            Intent(context, ExceptionDetailsActivity::class.java).apply {
                                putExtra("exception", exception)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                )
            }
        }
    }
    fun reportIssue(context: Context, exception: Throwable, message: String? = null) {
        if (enabled) {
            val handler = Handler(Looper.getMainLooper())
            thread {
                with(context) {
                    Log.d("IssueReporter", "Reporting an issue")
                    val request = URL("https://api.github.com/repos/Toolbox-io/Toolbox-io/issues")
                        .openConnection() as HttpsURLConnection
                    request.requestMethod = "POST"
                    request.setRequestProperty("Accept", "application/vnd.github.raw+json")
                    request.setRequestProperty("X-Github-Api-Version", GITHUB_API_VERSION)
                    request.setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")

                    try {
                        request.connect()
                        request.outputStream.use {
                            val title = "${exception::class.simpleName}: ${exception.message}".shorten(256).encodeJSON()
                            var body = """
                                |_Этот отчет об ошибке был автоматически отправлен через Toolbox.io._
                                |
                                |### Конфигурация
                                |**Версия Android:** ${Build.VERSION.RELEASE}
                                |**Производитель:** ${Build.MANUFACTURER}
                                |**Бренд:** ${Build.BRAND}
                                |**Модель:** ${Build.MODEL}
                                |**Версия Toolbox.io**: ${BuildConfig.VERSION_NAME}
                                |
                                |### Ошибка
                                |```
                                |${"${EParser(exception)}".trim()}
                                |```
                            """.trimMargin()
                            if (!message.isNullOrBlank()) {
                                val str = message
                                    .lines()
                                    .joinToString(separator = "\n") { line ->
                                        "> $line"
                                    }

                                body += """
                                    |
                                    |### Что делали?
                                    |$str
                                """.trimMargin()
                            }
                            body = body.encodeJSON()
                            val json = """
                                {
                                    "title": "$title",
                                    "body": "$body",
                                    "assignees": ["denis0001-dev"],
                                    "labels": ["приложение", "баг", "авто-отчет"]
                                }
                            """.trimIndent()

                            Log.d("IssueReporter", json)
                            it.write(json.toByteArray())
                        }
                        if (request.responseCode == 201) {
                            val input = BufferedInputStream(request.inputStream)
                            var c: Char

                            val chars: MutableList<Char> = mutableListOf()

                            while (true) {
                                c = input.read().toChar()
                                if (c == 0.toChar() || c == '\uFFFF') break
                                chars.add(c)
                            }
                            val response = String(chars.toCharArray())
                            val parsedResponse = JsonParser.parseString(response) as JsonObject
                            val number = parsedResponse["number"].asInt
                            handler.post {
                                Toast.makeText(
                                    this,
                                    resources.getString(R.string.issuecreated).format(number),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Log.d("IssueReporter", "Error. HTTP response code: ${request.responseCode}")
                            val errorInput = request.errorStream!!
                            var c: Char

                            val chars: MutableList<Char> = mutableListOf()

                            while (true) {
                                c = errorInput.read().toChar()
                                if (c == 0.toChar() || c == '\uFFFF') break
                                chars.add(c)
                            }
                            val response = String(chars.toCharArray())
                            Log.d("IssueReporter", "Error response: $response")
                            error("")
                        }
                    } catch (e: Exception) {
                        Log.d("IssueReporter", "Error. \n${EParser(e)}")
                        handler.post {
                            Toast.makeText(
                                this,
                                resources.getString(R.string.smthwentwrong),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        runCatching {
                            githubRateLimitRemaining = request.getHeaderField("x-ratelimit-remaining").toLong()
                        }
                        request.disconnect()
                    }
                }
            }
        }
    }

    abstract class IssueReporterActivity: BaseActivity(authEnabled = false) {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (!enabled) {
                finish()
                return
            }
        }
    }
    class ExceptionDetailsActivity: IssueReporterActivity() {
        private val exception by lazy { intent.getSerializableExtraAs<Throwable>("exception") }
        private lateinit var activityLauncher: ActivityLauncher

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun ExceptionDetailsScreen() {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            AppTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MediumTopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.error_info),
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
                            .verticalScroll(),
                    ) {
                        val exceptionText = remember {
                            "${EParser(exception)}"
                        }

                        Category(margin = CategoryDefaults.margin.copy(bottom = 16.dp)) {
                            Text(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                                text = exceptionText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                softWrap = false
                            )
                        }

                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.report_d),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = {
                                activityLauncher.launch(
                                    Intent(this@ExceptionDetailsActivity, OneQuestionActivity::class.java).apply {
                                        putExtra("exception", exception)
                                    }
                                ) {
                                    if (it.resultCode == RESULT_OK) {
                                        finish()
                                    }
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = stringResource(R.string.report))
                        }
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            activityLauncher = activityResultLauncher
            enableEdgeToEdge()
            setContent {
                ExceptionDetailsScreen()
            }
        }
    }
    class OneQuestionActivity: IssueReporterActivity() {
        private val exception: Throwable by lazy { intent.getSerializableExtraAs<Throwable>("exception") }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun OneQuestionScreen() {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            AppTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MediumTopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.one_question),
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
                            .verticalScroll(),
                    ) {
                        var text by remember { mutableStateOf("") }

                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(R.string.one_question_d)
                        )
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                reportIssue(this@OneQuestionActivity, exception, text)
                                setResult(RESULT_OK)
                                finish()
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = stringResource(R.string.report))
                        }
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContent {
                OneQuestionScreen()
            }
        }

        override fun finish() {
            super.finish()
            setResult(RESULT_CANCELED)
        }
    }
}