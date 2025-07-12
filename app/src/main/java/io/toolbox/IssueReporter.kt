package io.toolbox

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import cat.ereza.customactivityoncrash.config.CaocConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.toolbox.api.IssuesAPI
import io.toolbox.ui.AppTheme
import io.toolbox.ui.MainActivity
import kotlinx.coroutines.launch
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.copy
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.android.verticalScroll


object IssueReporter {
    var enabled = true
        set(value) {
            field = value
            config(enabled = value)
        }

    fun config(enabled: Boolean = true) {
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
            .enabled(enabled) //default: true
            .errorActivity(MainActivity::class.java) //default: null (default error activity)
            .trackActivities(true)
            .customCrashDataCollector {
                "crash_mainactivity"
            }
            .apply()
    }

    fun init() = config(enabled = true)

    suspend fun reportIssue(context: Context, exception: String, message: String? = null) {
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
        private val exception by lazy { intent.getStringExtra("exception")!! }
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
                        Category(margin = CategoryDefaults.margin.copy(bottom = 16.dp)) {
                            Text(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState()),
                                text = exception,
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
        private val exception by lazy { intent.getStringExtra("exception")!! }

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