package io.toolbox.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.morozovit.android.plus
import ru.morozovit.android.ui.Mipmap
import io.toolbox.R

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun AppIcon(modifier: Modifier = Modifier) {
    Mipmap(
        id = R.mipmap.app_icon,
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.size(80.dp) + modifier
    )
}