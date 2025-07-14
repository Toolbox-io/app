package io.toolbox.ui.tools.appmanager

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Shortcut
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.core.graphics.drawable.toBitmap
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.ui.AppTheme
import ru.morozovit.android.utils.ui.Category
import ru.morozovit.android.utils.ui.IntentActivity
import ru.morozovit.android.utils.ui.ListItem
import ru.morozovit.android.utils.getSystemService
import ru.morozovit.android.utils.isLauncher
import ru.morozovit.android.utils.launchIntent
import ru.morozovit.android.utils.ui.verticalScroll

class ChooseActivityActivity: BaseActivity() {
    private val appPackage by lazy { intent.getStringExtra("appPackage")!! }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChooseActivityScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.choose_activity),
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
                        .verticalScroll()
                ) {
                    val activities = packageManager.getPackageInfo(appPackage, PackageManager.GET_ACTIVITIES).activities!!

                    val launcherActivities = mutableListOf<ActivityInfo>()
                    val otherActivities = mutableListOf<ActivityInfo>()
                    activities.forEach { activity ->
                        if (activity.enabled || activity.exported) {
                            if (activity.isLauncher(this@ChooseActivityActivity)) {
                                launcherActivities.add(activity)
                            } else {
                                otherActivities.add(activity)
                            }
                        }
                    }

                    @Composable
                    fun ActivityListItem(
                        activity: ActivityInfo,
                        isLast: Boolean
                    ) {
                        val label = activity.loadLabel(packageManager).toString()
                        val leadingContent: @Composable ConstraintLayoutScope.() -> Unit = {
                            Image(
                                bitmap = activity.loadIcon(packageManager).toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(30.dp))
                            )
                        }
                        val trailingContent: @Composable ConstraintLayoutScope.() -> Unit = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                IconButton(
                                    onClick = {
                                        val shortcutManager = getSystemService(ShortcutManager::class)!!
                                        if (shortcutManager.isRequestPinShortcutSupported) {
                                            val shortcutInfo = ShortcutInfo.Builder(
                                                this@ChooseActivityActivity,
                                                "${activity.packageName}:${activity.name}"
                                            )
                                                .setShortLabel(activity.loadLabel(packageManager))
                                                .setLongLabel(activity.loadLabel(packageManager))
                                                .setIcon(
                                                    try {
                                                        android.graphics.drawable.Icon.createWithAdaptiveBitmap(
                                                            activity.loadIcon(packageManager).toBitmap()
                                                        )
                                                    } catch (_: Exception) {
                                                        android.graphics.drawable.Icon.createWithBitmap(
                                                            activity.loadIcon(packageManager).toBitmap()
                                                        )
                                                    }
                                                )
                                                .setIntent(
                                                    Intent(
                                                        this@ChooseActivityActivity,
                                                        IntentActivity::class.java
                                                    ).apply {
                                                        val component = ComponentName(activity.packageName, activity.name)
                                                        putExtra(IntentActivity.EXTRA_PACKAGE_NAME, component.packageName)
                                                        putExtra(IntentActivity.EXTRA_CLASS_NAME, component.className)
                                                        action = ACTION_MAIN
                                                    }
                                                )
                                                .build()
                                            shortcutManager.requestPinShortcut(shortcutInfo, null)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Shortcut,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        val smthwentwrong = stringResource(R.string.smthwentwrong)

                        fun onClick() {
                            try {
                                startActivity(activity.launchIntent)
                            } catch (_: Exception) {
                                Toast.makeText(this@ChooseActivityActivity, smthwentwrong, Toast.LENGTH_SHORT).show()
                            }
                        }

                        if (isLast) {
                            ListItem(
                                headline = label,
                                supportingText = activity.name,
                                divider = true,
                                dividerThickness = 2.dp,
                                dividerColor = MaterialTheme.colorScheme.surface,
                                leadingContent = leadingContent,
                                trailingContent = trailingContent,
                                onClick = ::onClick
                            )
                        } else {
                            ListItem(
                                headline = label,
                                supportingText = activity.name,
                                leadingContent = leadingContent,
                                trailingContent = trailingContent,
                                onClick = ::onClick
                            )
                        }
                    }

                    Category {
                        for (index in launcherActivities.indices) {
                            ActivityListItem(
                                activity = launcherActivities[index],
                                isLast = index == launcherActivities.size - 1
                            )
                        }
                    }
                    Category {
                        for (index in otherActivities.indices) {
                            ActivityListItem(
                                activity = otherActivities[index],
                                isLast = index == otherActivities.size - 1
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
            ChooseActivityScreen()
        }
    }
}