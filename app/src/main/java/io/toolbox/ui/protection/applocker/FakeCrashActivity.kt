package io.toolbox.ui.protection.applocker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.toolbox.R
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_APP_INFO
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_CLOSE
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_OPEN_APP_AGAIN
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import io.toolbox.Settings.Applocker.UnlockMode.PRESS_TITLE
import io.toolbox.Settings.Applocker.unlockMode
import io.toolbox.ui.OverlayAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.morozovit.android.utils.appName
import ru.morozovit.android.utils.homeScreen
import ru.morozovit.android.utils.screenWidth
import ru.morozovit.android.utils.ui.ComposeView

class FakeCrashActivity : AppCompatActivity() {
    @Composable
    private fun FakeCrashScreen(intentAppPackage: String?) {
        OverlayAppTheme(Modifier.fillMaxSize()) {
            val context = this@FakeCrashActivity
            val isNight = isSystemInDarkTheme()
            val api = Build.VERSION.SDK_INT
            val scope = rememberCoroutineScope()

            // Per-API layout values from XMLs
            data class LayoutSpec(
                val cardCornerRadius: Int,
                val buttonSpacing: Int,
                val buttonTextSize: Int,
                val isBold: Boolean
            ) {
                val cardHorizontalMargin = 30
                val cardMaxWidth = 500
                val columnTop = 20
                val titleStart = 20
                val titleEnd = 20
                val columnBottom = 10
                val titleBottom = 20
                val buttonStart = 20
                val buttonEnd = 20
                val buttonBottom = 15
                val buttonTop = 15
            }

            val layout = LayoutSpec(
                cardCornerRadius = when {
                    api >= 31 -> 30
                    api >= 29 -> 10
                    else -> 0
                },
                buttonTextSize = if (api >= 31) 17 else 16,
                buttonSpacing = if (api >= 31) 16 else 32,
                isBold = api < 29
            )

            fun exit() {
                scope.launch {
                    homeScreen()
                    delay(1000)
                    setResult(RESULT_OK)
                    finish()
                }
            }

            val androidPrimary = if (isNight) Color(0xFF80cbc4) else Color(0xFF008577)

            // Button visibility by API
            val showOpenAppAgain = api in 24..27
            val showCloseApp = api >= 28
            val showAppInfo = api >= 28

            var packageName by remember { mutableStateOf("") }
            var appDisplayName by remember { mutableStateOf("null") }

            LaunchedEffect(intentAppPackage) {
                if (intentAppPackage != null) {
                    packageName = intentAppPackage
                    appDisplayName = appName(packageName).toString()
                }
            }

            // Auto-close after 15 seconds
            LaunchedEffect(Unit) {
                delay(15000)
                exit()
            }

            fun unlock(modeRequired: Int? = null) {
                if (modeRequired in listOf(null, unlockMode)) {
                    setResult(RESULT_OK)
                    finish()
                    PasswordInputActivity.start(context, packageName)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = layout.cardHorizontalMargin.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .widthIn(max = layout.cardMaxWidth.dp),
                    shape = RoundedCornerShape(layout.cardCornerRadius.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        Modifier.padding(
                            top = layout.columnTop.dp,
                            bottom = layout.columnBottom.dp
                        )
                    ) {
                        // Title
                        Text(
                            text = stringResource(R.string.app_keeps_stopping, appDisplayName),
                            fontSize = 20.sp,
                            color = when {
                                api >= 31 && isNight -> MaterialTheme.colorScheme.onSurface
                                api >= 29 && isNight -> Color.White
                                api >= 29 -> Color.Black
                                else -> Color.Black
                            },
                            fontWeight = FontWeight.W600.takeIf { layout.isBold },
                            modifier = Modifier
                                .padding(
                                    start = layout.titleStart.dp,
                                    end = layout.titleEnd.dp,
                                    bottom = layout.titleBottom.dp
                                )
                                .combinedClickable(
                                    onClick = { unlock(PRESS_TITLE) },
                                    onLongClick = { unlock(LONG_PRESS_TITLE) },
                                    indication = null,
                                    interactionSource = null,
                                )
                        )

                        @Composable
                        fun ActionButton(
                            icon: ImageVector,
                            text: String,
                            modeRequired: Int,
                            action: () -> Unit
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = action,
                                        onLongClick = { unlock(modeRequired) }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = layout.buttonStart.dp,
                                            end = layout.buttonEnd.dp,
                                            bottom = layout.buttonBottom.dp,
                                            top = layout.buttonTop.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = androidPrimary,
                                        modifier = Modifier.padding(end = layout.buttonSpacing.dp)
                                    )
                                    Text(
                                        text = text,
                                        fontSize = layout.buttonTextSize.sp,
                                        color = when {
                                            api >= 31 && isNight -> MaterialTheme.colorScheme.onSurface
                                            api >= 29 && isNight -> Color.White
                                            api >= 29 -> Color.Black
                                            else -> Color.Black
                                        }
                                    )
                                }
                            }
                        }

                        // App Info Button (API 28+)
                        if (showAppInfo) {
                            ActionButton(
                                icon = Icons.Outlined.Info,
                                text = stringResource(R.string.appinfo),
                                modeRequired = LONG_PRESS_APP_INFO
                            ) {
                                startActivity(
                                    Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                                        data = "package:$packageName".toUri()
                                    }
                                )
                                setResult(RESULT_OK)
                                finish()
                            }
                        }

                        // Close App Button (API 28+)
                        if (showCloseApp) {
                            ActionButton(
                                icon = Icons.Filled.Close,
                                text = stringResource(R.string.closeapp),
                                modeRequired = LONG_PRESS_CLOSE
                            ) {
                                exit()
                            }
                        }

                        // Open App Again Button (API 24–27)
                        if (showOpenAppAgain) {
                            ActionButton(
                                icon = Icons.Filled.Refresh,
                                text = stringResource(R.string.open_app_again),
                                modeRequired = LONG_PRESS_OPEN_APP_AGAIN
                            ) {
                                if (packageName.isNotBlank()) {
                                    val intent = applicationContext
                                        .packageManager
                                        .getLaunchIntentForPackage(packageName)
                                    startActivity(intent)
                                    setResult(RESULT_OK)
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(
            ComposeView {
                FakeCrashScreen(intentAppPackage = intent.extras?.getString("appPackage"))
            },
            ViewGroup.LayoutParams(
                screenWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onPause() {
        super.onPause()
        setResult(RESULT_OK)
        finish()
    }
}