package ru.morozovit.ultimatesecurity.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.morozovit.android.plus
import ru.morozovit.ultimatesecurity.R

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun AppIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.app_icon),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.clip(RoundedCornerShape(30.dp)) + modifier
    )
}