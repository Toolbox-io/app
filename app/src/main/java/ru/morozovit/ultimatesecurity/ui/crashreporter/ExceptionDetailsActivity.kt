package ru.morozovit.ultimatesecurity.ui.crashreporter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.morozovit.android.copy
import ru.morozovit.android.getSerializableExtraAs
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.CategoryDefaults
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.utils.EParser

class ExceptionDetailsActivity: BaseActivity(authEnabled = false) {
    private val exception by lazy { intent.getSerializableExtraAs<Throwable>("exception") }

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
                        .verticalScroll(rememberScrollState()),
                ) {
                    val exceptionText = remember {
                        try {
                            "${EParser(exception)}"
                        } catch (e: Exception) {
                            "Cannot show exception details, because another exception occurred."
                        }
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
                            reportIssue(this@ExceptionDetailsActivity, exception)
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
            ExceptionDetailsScreen()
        }
    }
}
