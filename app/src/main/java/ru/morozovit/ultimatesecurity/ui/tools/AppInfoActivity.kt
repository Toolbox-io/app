package ru.morozovit.ultimatesecurity.ui.tools

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.AppTheme
import ru.morozovit.ultimatesecurity.ui.PhonePreview
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AppInfoActivity: BaseActivity() {
    private val appPackage by lazy { intent.getStringExtra("appPackage")!! }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @PhonePreview
    fun AppInfoScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.appinfo),
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val packageInfo = remember { packageManager.getPackageInfo(appPackage, 0) }
                    val launchIntent by lazy { packageManager.getLaunchIntentForPackage(appPackage) }

                    Image(
                        bitmap =
                            packageInfo
                                .applicationInfo!!
                                .loadIcon(packageManager)
                                .toBitmap()
                                .asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(94.dp)
                            .padding(bottom = 8.dp, top = 16.dp)
                    )
                    Text(
                        text = packageInfo
                            .applicationInfo!!
                            .loadLabel(packageManager)
                            .toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        @Composable
                        fun RowScope.HeaderItem(
                            title: String,
                            icon: ImageVector,
                            enabled: Boolean = true,
                            onClick: () -> Unit
                        ) {
                            Column(
                                modifier =
                                if (enabled) {
                                    Modifier.clickable(onClick = onClick)
                                } else {
                                    Modifier
                                }
                                    .padding(vertical = 16.dp)
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val color =
                                    if (enabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                    color = color
                                )
                            }
                        }

                        Row(Modifier.height(IntrinsicSize.Max)) {
                            HeaderItem(
                                title = stringResource(R.string.launch),
                                icon = Icons.AutoMirrored.Filled.Launch,
                                onClick = {
                                    startActivity(launchIntent!!)
                                },
                                enabled = launchIntent != null
                            )
                            VerticalDivider(
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.surface
                            )
                            HeaderItem(
                                title = stringResource(R.string.delete),
                                icon = Icons.Filled.Delete,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DELETE)
                                    intent.data = Uri.parse("package:$appPackage")
                                    startActivity(intent)
                                },
                                enabled = packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM == 0
                            )
                            VerticalDivider(
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.surface
                            )
                            HeaderItem(
                                title = stringResource(R.string.share),
                                icon = Icons.Filled.Share,
                                onClick = {
                                    try {
                                        val file = File(packageInfo.applicationInfo!!.publicSourceDir)
                                        val inputStream = FileInputStream(file)
                                        val outputFile = File(
                                            cacheDir,
                                            "${
                                                packageInfo
                                                    .applicationInfo!!
                                                    .loadLabel(packageManager)
                                            }.apk"
                                        )
                                        val outputStream = FileOutputStream(outputFile)
                                        inputStream.copyTo(outputStream)
                                        inputStream.close()
                                        outputStream.close()

                                        val uri = FileProvider.getUriForFile(
                                            this@AppInfoActivity,
                                            applicationContext.packageName + ".provider",
                                            outputFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND)
                                        shareIntent.type = "application/vnd.android.package-archive"
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                                        startActivity(Intent.createChooser(shareIntent, null))
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@AppInfoActivity,
                                            "Failed to share APK, please try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = true
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppInfoScreen()
        }
    }
}