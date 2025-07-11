package io.toolbox.ui.protection.actions.intruderphoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.toolbox.BaseActivity
import io.toolbox.R
import io.toolbox.Settings
import io.toolbox.ui.AppTheme
import ru.morozovit.android.ActivityLauncher
import ru.morozovit.android.activityResultLauncher
import ru.morozovit.android.invoke
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.SwitchCard
import ru.morozovit.android.ui.SwitchListItem
import java.io.File

class IntruderPhotoSettingsActivity: BaseActivity(authEnabled = false) {
    private lateinit var activityLauncher: ActivityLauncher
    private var resumeLock = true

    data class IntruderPhoto(
        val uri: Uri,
        val drawable: Drawable,
        val file: File
    ) {
        inline val lastModified get() = file.lastModified()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun IntruderPhotoScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            val context = LocalContext()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.intruderphoto),
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
                val drawables = remember { mutableStateListOf<IntruderPhoto>() }
                LaunchedEffect(Unit) {
                    fun isImage(file: File): Boolean {
                        val type = file.extension
                        return type.equals("jpg", ignoreCase = true) ||
                                type.equals("png", ignoreCase = true) ||
                                type.equals("jpeg", ignoreCase = true)
                    }

                    val frontDir = File(filesDir.absolutePath + "/front")
                    val frontFiles = frontDir.listFiles() ?: emptyArray()

                    frontFiles.forEach { file ->
                        if (!isImage(file)) {
                            file.delete()
                        }
                    }

                    val validFrontFiles = frontFiles.filter { isImage(it) }

                    validFrontFiles.forEach { file ->
                        val uri = FileProvider.getUriForFile(
                            this@IntruderPhotoSettingsActivity,
                            applicationContext.packageName + ".provider",
                            file
                        )
                        val stream = contentResolver.openInputStream(uri)
                        val drawable = Drawable.createFromStream(stream, null)!!
                        stream!!.close()
                        drawables.add(IntruderPhoto(uri, drawable, file))
                    }

                    drawables.sortWith { o1, o2 -> o2.lastModified.compareTo(o1.lastModified) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = innerPadding
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            var mainSwitch by remember { mutableStateOf(Settings.Actions.IntruderPhoto.enabled) }

                            LaunchedEffect(Unit) {
                                if (checkSelfPermission(Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
                                    mainSwitch = false
                                    Settings.UnlockProtection.IntruderPhoto.enabled = false
                                }
                            }

                            SwitchCard(
                                text = stringResource(R.string.enable),
                                checked = mainSwitch,
                                onCheckedChange = {
                                    if (it) {
                                        if (checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
                                            mainSwitch = true
                                        } else {
                                            requestPermission(Manifest.permission.CAMERA) { granted ->
                                                if (granted) {
                                                    mainSwitch = true
                                                }
                                            }
                                        }
                                    } else {
                                        mainSwitch = false
                                    }
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Category {
                                var noptChecked by remember { mutableStateOf(Settings.Actions.IntruderPhoto.nopt) }
                                SwitchListItem(
                                    headline = stringResource(R.string.notify_on_photo_taken),
                                    supportingText = stringResource(R.string.nopt_d),
                                    checked = noptChecked,
                                    onCheckedChange = {
                                        if (it) {
                                            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED
                                                || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                            ) {
                                                noptChecked = true
                                                Settings.UnlockProtection.IntruderPhoto.nopt = true
                                            } else {
                                                requestPermission(Manifest.permission.POST_NOTIFICATIONS) { granted ->
                                                    if (granted) {
                                                        noptChecked = true
                                                        Settings.UnlockProtection.IntruderPhoto.nopt = true
                                                    }
                                                }
                                            }
                                        } else {
                                            noptChecked = false
                                            Settings.UnlockProtection.IntruderPhoto.nopt = false
                                        }
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }

                            Text(
                                text = stringResource(R.string.intruderphotos),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            )
                        }
                    }

                    items(drawables) {
                        var menu by remember { mutableStateOf(false) }

                        Box {
                            Image(
                                painter = rememberDrawablePainter(drawable = it.drawable),
                                contentDescription = null,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(it.uri, "image/*")
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                            )
                                        },
                                        onLongClick = {
                                            menu = true
                                        }
                                    )
                            )

                            DropdownMenu(
                                expanded = menu,
                                onDismissRequest = { menu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = {
                                        it.file.delete()
                                        drawables.remove(it)
                                        menu = false
                                    }
                                )
                            }
                        }
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
            IntruderPhotoScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!resumeLock) recreate()
        resumeLock = false
    }
}