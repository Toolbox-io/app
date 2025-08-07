package io.toolbox.ui.main

import android.content.Intent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.toolbox.BuildConfig
import io.toolbox.R
import io.toolbox.ui.AppIcon
import io.toolbox.ui.EdgeToEdgeBarType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import ru.morozovit.android.utils.openUrl
import ru.morozovit.android.utils.ui.Button
import ru.morozovit.android.utils.ui.License
import ru.morozovit.android.utils.ui.Website
import ru.morozovit.android.utils.ui.WindowInsetsHandler
import ru.morozovit.android.utils.ui.invoke
import ru.morozovit.android.utils.ui.verticalScroll

@Composable
fun AboutScreen(EdgeToEdgeBar: EdgeToEdgeBarType) {
    with (LocalContext()) {
        WindowInsetsHandler {
            EdgeToEdgeBar { innerPadding ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(innerPadding)
                        .verticalScroll()
                ) {
                    Box(Modifier.padding(top = 20.dp)) {
                        AppIcon(modifier = Modifier.size(150.dp))
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    @Suppress("KotlinConstantConditions", "RedundantSuppression")
                    Text(
                        text = "${stringResource(R.string.version_app)}${BuildConfig.VERSION_NAME}${if (BuildConfig.DEBUG) " (DEBUG)" else ""}",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.app_desc_l),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 10.dp),
                        textAlign = TextAlign.Center
                    )

                    val osl = stringResource(R.string.osl)

                    Column(Modifier.width(IntrinsicSize.Max)) {
                        Button(
                            onClick = {
                                startActivity(
                                    Intent(this@with, OSSLicensesActivity::class.java)
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.License,
                                    contentDescription = osl
                                )
                            },
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                        ) {
                            Text(osl)
                        }

                        Button(
                            onClick = {
                                openUrl("toolbox-io.ru")
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Website,
                                    contentDescription = stringResource(R.string.website)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.website))
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        val viewConfiguration = LocalViewConfiguration.current
                        LaunchedEffect(interactionSource) {
                            var isLongClick = false

                            interactionSource.interactions.collectLatest { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> {
                                        isLongClick = false
                                        delay(viewConfiguration.longPressTimeoutMillis)
                                        isLongClick = true
                                        startActivity(
                                            Intent(
                                                this@with,
                                                DeveloperOptionsActivity::class.java
                                            )
                                        )
                                    }

                                    is PressInteraction.Release -> {
                                        if (isLongClick.not()) {
                                            openUrl(
                                                "https://github.com/denis0001-dev/Toolbox-io/issues/new" +
                                                "?assignees=denis0001-dev&labels=app%2C+bug" +
                                                "&projects=&template=application-bug-report.md&title="
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {},
                            interactionSource = interactionSource,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = stringResource(R.string.report_error)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.report_error))
                        }
                    }
                }
            }
        }
    }
}