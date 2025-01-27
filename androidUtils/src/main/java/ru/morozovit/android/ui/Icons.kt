@file:Suppress("UnusedReceiverParameter", "unused", "unused", "unused")

package ru.morozovit.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ru.morozovit.android.R

val Icons.Outlined.Internet get() = Icons.Outlined.Language
val Icons.Filled.Internet get() = Icons.Filled.Language

val Icons.Outlined.Website get() = Icons.Outlined.Language
val Icons.Filled.Website get() = Icons.Filled.Language

val Icons.Outlined.License
    @Composable get() = ImageVector.vectorResource(R.drawable.license)
val Icons.Filled.License
    @Composable get() = Icons.Outlined.License