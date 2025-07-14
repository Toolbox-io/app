@file:Suppress("UnusedReceiverParameter", "unused")

package ru.morozovit.android.utils.ui

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

val Icons.Outlined.Siren
    @Composable get() = ImageVector.vectorResource(R.drawable.siren_outlined)
val Icons.Filled.Siren
    @Composable get() = ImageVector.vectorResource(R.drawable.siren_filled)

val Icons.Outlined.Charger
    @Composable get() = ImageVector.vectorResource(R.drawable.charger_outlined)
val Icons.Filled.Charger
    @Composable get() = ImageVector.vectorResource(R.drawable.charger_filled)