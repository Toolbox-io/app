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
import io.toolbox.Settings.Applocker.UnlockMode.LONG_PRESS_TITLE
import io.toolbox.Settings.Applocker.UnlockMode.PRESS_TITLE
import io.toolbox.ui.OverlayAppTheme
import ru.morozovit.android.ComposeView
import ru.morozovit.android.appName
import ru.morozovit.android.screenWidth

class FakeCrashActivity : AppCompatActivity() {
    @Composable
    private fun FakeCrashScreen(intentAppPackage: String?) {
        OverlayAppTheme(Modifier.fillMaxSize()) {
            val context = this@FakeCrashActivity
            val isNight = isSystemInDarkTheme()
            val api = Build.VERSION.SDK_INT

            // Per-API layout values from XMLs
            data class LayoutSpec(
                val cardCornerRadius: Int,
                val cardHorizontalMargin: Int,
                val cardMaxWidth: Int,
                val columnTop: Int,
                val columnBottom: Int,
                val titleStart: Int,
                val titleEnd: Int,
                val titleBottom: Int,
                val buttonStart: Int,
                val buttonEnd: Int,
                val buttonBottom: Int,
                val buttonTop: Int,
                val buttonSpacing: Int,
                val buttonTextSize: Int
            )
            val layout = when {
                api >= 31 -> LayoutSpec(
                    cardCornerRadius = 30, cardHorizontalMargin = 30, cardMaxWidth = 500,
                    columnTop = 20, columnBottom = 10, titleStart = 20, titleEnd = 20,
                    titleBottom = 20, buttonStart = 20, buttonEnd = 20, buttonBottom = 15,
                    buttonTop = 15, buttonTextSize = 17, buttonSpacing = 16
                )
                api >= 29 -> LayoutSpec(
                    cardCornerRadius = 10, cardHorizontalMargin = 30, cardMaxWidth = 500,
                    columnTop = 20, columnBottom = 10, titleStart = 20, titleEnd = 20,
                    titleBottom = 20, buttonStart = 20, buttonEnd = 20, buttonBottom = 15,
                    buttonTop = 15, buttonTextSize = 16, buttonSpacing = 32
                )
                else -> LayoutSpec(
                    cardCornerRadius = 0, cardHorizontalMargin = 30, cardMaxWidth = 500,
                    columnTop = 20, columnBottom = 10, titleStart = 20, titleEnd = 20,
                    titleBottom = 20, buttonStart = 20, buttonEnd = 20, buttonBottom = 15,
                    buttonTop = 15, buttonTextSize = 16, buttonSpacing = 32
                )
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
                    appDisplayName = appName(context, packageName) ?: "null"
                }
            }

            // Auto-close after 15 seconds
            // TODO uncomment
            /*LaunchedEffect(Unit) {
                delay(15000)
                setResult(RESULT_CANCELED)
                finish()
            }*/

            fun unlock(modeRequired: Int? = null) {
//                if (modeRequired in listOf(null, unlockMode)) {
//                    setResult(RESULT_OK)
//                    finish()
//                    PasswordInputActivity.start(context, packageName)
//                }
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
                            fontWeight = FontWeight.W600.takeIf { api < 29 },
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
                            text: String
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            startActivity(
                                                Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                                                    data = "package:$packageName".toUri()
                                                }
                                            )
                                            setResult(RESULT_OK)
                                            finish()
                                        },
                                        onLongClick = { unlock(LONG_PRESS_APP_INFO) }
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
                        if (showAppInfo) ActionButton(Icons.Outlined.Info, stringResource(R.string.appinfo))

                        // Close App Button (API 28+)
                        if (showCloseApp) ActionButton(Icons.Filled.Close, stringResource(R.string.closeapp))

                        // Open App Again Button (API 24–27)
                        if (showOpenAppAgain) ActionButton(Icons.Filled.Refresh, stringResource(R.string.open_app_again))
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