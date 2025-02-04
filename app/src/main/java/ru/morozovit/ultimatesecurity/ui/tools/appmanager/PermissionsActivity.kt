package ru.morozovit.ultimatesecurity.ui.tools.appmanager

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.core.graphics.drawable.toBitmap
import ru.morozovit.android.ui.Category
import ru.morozovit.android.ui.ListItem
import ru.morozovit.ultimatesecurity.BaseActivity
import ru.morozovit.ultimatesecurity.R
import ru.morozovit.ultimatesecurity.ui.AppTheme

class PermissionsActivity : BaseActivity() {
    private val appPackage by lazy { intent.getStringExtra("appPackage")!! }

    @SuppressLint("NewApi")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PermissionsScreen() {
        AppTheme {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    MediumTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.permissions),
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
                    Spacer(Modifier.height(16.dp))
                    val permissions = packageManager.getPackageInfo(appPackage, PackageManager.GET_PERMISSIONS).requestedPermissions

                    val normalPermissions = mutableListOf<String>()
                    val specialPermissions = mutableListOf<String>()
                    val systemOnlyPermissions = mutableListOf<String>()

                    permissions?.forEach { permission ->
                        runCatching {
                            val permissionInfo = packageManager.getPermissionInfo(permission, 0)
                            val protectionLevel = permissionInfo.protection
                            val protectionFlags = permissionInfo.protectionFlags

                            when {
                                (
                                    protectionFlags and
                                        PermissionInfo.PROTECTION_FLAG_PRIVILEGED
                                        == PermissionInfo.PROTECTION_FLAG_PRIVILEGED
                                ) -> systemOnlyPermissions.add(permission)

                                protectionLevel == PermissionInfo.PROTECTION_NORMAL -> normalPermissions.add(permission)
                                protectionLevel == PermissionInfo.PROTECTION_DANGEROUS -> specialPermissions.add(permission)
                                protectionLevel == PermissionInfo.PROTECTION_SIGNATURE -> normalPermissions.add(permission)
                                else -> {}
                            }
                        }
                    }

                    @Composable
                    fun PermissionList(@StringRes title: Int, list: List<String>) {
                        if (list.isNotEmpty()) {
                            Category(title = stringResource(title)) {
                                list.forEachIndexed { index, perm ->
                                    val permissionInfo = packageManager.getPermissionInfo(perm, 0)
                                    val label = permissionInfo.loadLabel(packageManager)
                                    val headline =
                                        (
                                                if (label == permissionInfo.name)
                                                    label
                                                else
                                                    "$label (${permissionInfo.name})"
                                                ).toString()
                                    val supportingText = permissionInfo.loadDescription(packageManager)?.toString()
                                    val leadingContent: @Composable ConstraintLayoutScope.() -> Unit = {
                                        Icon(
                                            bitmap = permissionInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    if (index == normalPermissions.size - 1) {
                                        ListItem(
                                            headline = headline,
                                            supportingText = supportingText,
                                            leadingContent = leadingContent
                                        )
                                    } else {
                                        ListItem(
                                            headline = headline,
                                            supportingText = supportingText,
                                            leadingContent = leadingContent,
                                            divider = true,
                                            dividerThickness = 2.dp,
                                            dividerColor = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PermissionList(title = R.string.normalPermissions, list = normalPermissions)
                    PermissionList(title = R.string.specialPermissions, list = specialPermissions)
                    PermissionList(title = R.string.systemOnlyPermissions, list = systemOnlyPermissions)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PermissionsScreen()
        }
    }
}