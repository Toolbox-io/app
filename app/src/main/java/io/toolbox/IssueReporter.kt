package io.toolbox

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.postDelayed
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.toolbox.App.Companion.context
import io.toolbox.api.IssuesAPI
import io.toolbox.ui.AppTheme
import io.toolbox.ui.MainActivity
import kotlinx.coroutines.launch
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.copy
import ru.morozovit.android.getSerializableExtraAs
import ru.morozovit.android.runMultiple
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.ui.DialogActivity
import ru.morozovit.android.verticalScroll
import ru.morozovit.utils.EParser
import kotlin.system.exitProcess

object IssueReporter {
    private var uncaughtHandler: Thread.UncaughtExceptionHandler? = null
    private var uncaughtHandlerIsSet = false

    fun setHandler() {
        if (!uncaughtHandlerIsSet) {
            uncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
            uncaughtHandlerIsSet = true
        }
    }

    var enabled = true
        set(value) {
            field = value

            setHandler()

            Thread.setDefaultUncaughtExceptionHandler(
                if (value) DEFAULT_HANDLER
                else uncaughtHandler ?: DISABLED_HANDLER
            )
        }
    private var crashes = 0
    private const val CRASHES_LIMIT = 2

    private val DEFAULT_HANDLER = Thread.UncaughtExceptionHandler { t: Thread, exception: Throwable ->
        crashes++
        if (crashes > CRASHES_LIMIT) {
            enabled = false
            (uncaughtHandler ?: DISABLED_HANDLER).uncaughtException(t, exception)
        }
        runCatching {
            Log.e("App", "EXCEPTION CAUGHT:\n${EParser(exception)}")
        }
        startCrashedActivity(exception, context)
    }
    private val DISABLED_HANDLER = Thread.UncaughtExceptionHandler { _: Thread, e: Throwable ->
        runMultiple(
            {
                Toast.makeText(
                    context,
                    R.string.fatal_error,
                    Toast.LENGTH_SHORT
                )
            },
            {
                Log.wtf("App", "FATAL EXCEPTION:", e)
            }
        )
        exitProcess(10)
    }

    fun init() {
        setHandler()
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
    suspend fun reportIssue(context: Context, exception: Throwable, message: String? = null) {
        if (enabled) {
            try {
                val number = IssuesAPI.reportCrash(exception, message)
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.issuecreated).format(number),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: ResponseException) {
                Log.e("IssueReporter", "Error. HTTP response code: ${e.response.status}")
                Log.e("IssueReporter", e.response.bodyAsText())
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
        private val exception by lazy { intent.getSerializableExtraAs<Throwable>("exception")!! }
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
        private val exception by lazy { intent.getSerializableExtraAs<Throwable>("exception")!! }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun OneQuestionScreen() {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            val scope = rememberCoroutineScope()

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
                                scope.launch {
                                    reportIssue(this@OneQuestionActivity, exception, text)
                                    setResult(RESULT_OK)
                                    finish()
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