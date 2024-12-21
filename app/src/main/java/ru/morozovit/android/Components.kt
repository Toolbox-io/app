@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
    headline: String,
    supportingText: String? = null,
    leadingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    trailingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    divider: Boolean = false,
    onClick: (() -> Unit)? = null,
    bodyOnClick: (() -> Unit)? = null
) {
    ConstraintLayout(
        modifier = Modifier.let {
            var mod = it.fillMaxWidth()
            if (onClick != null) {
                mod += Modifier.clickable(onClick = onClick)
            }
            mod + modifier
        }
    ) {
        val (leading, listItem, trailing, div) = createRefs()
        if (leadingContent != null) {
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(leading) {
                        top link
                                if (divider) div.top
                                else parent.bottom
                        bottom link parent.bottom
                        left link parent.left
                        right link listItem.left
                    }
                    .padding(end = 16.dp),
                content = leadingContent
            )
        }
        androidx.compose.material3.ListItem(
            headlineContent = { Text(headline) },
            supportingContent = { if (supportingText != null) Text(supportingText) },
            modifier = Modifier
                .let {
                    var mod = it.constrainAs(listItem) {
                        top link parent.top
                        bottom link
                                if (divider) div.top
                                else parent.bottom

                        left link
                                if (leadingContent != null) leading.right
                                else parent.left
                        right link
                                if (trailingContent != null) trailing.left
                                else parent.right
                        width = Dimension.fillToConstraints
                    }
                    if (bodyOnClick != null) {
                        mod += Modifier.clickable(onClick = bodyOnClick)
                    }
                    mod + bodyModifier
                }
        )
        if (trailingContent != null) {
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(trailing) {
                        top link parent.top
                        bottom link
                                if (divider) div.top
                                else parent.bottom
                        right link parent.right
                        left link listItem.right
                    }
                    .padding(end = 16.dp),
                content = trailingContent
            )
        }
        if (divider) {
            HorizontalDivider(
//                thickness = 0.5.dp,
                modifier = Modifier.constrainAs(div) {
                    bottom link parent.bottom
                    left link parent.left
                    right link parent.right
                }
            )
        }
    }
}

@Composable
inline fun SwitchListItem(
    modifier: Modifier = Modifier,
    headline: String,
    supportingText: String? = null,
    noinline leadingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    checked: Boolean,
    noinline onCheckedChange: (Boolean) -> Unit,
    noinline listItemOnClick: () -> Unit,
    divider: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    ListItem(
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = listItemOnClick
        ) + modifier,
        headline = headline,
        supportingText = supportingText,
        leadingContent = leadingContent,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource,
                enabled = enabled
            )
        },
        divider = divider
    )
}

@Composable
inline fun SeparatedSwitchListItem(
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
    headline: String,
    supportingText: String? = null,
    noinline leadingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    checked: Boolean,
    noinline onCheckedChange: (Boolean) -> Unit,
    noinline bodyOnClick: () -> Unit,
    divider: Boolean = false,
    enabled: Boolean = true
) {
    ListItem(
        modifier = modifier,
        bodyModifier = bodyModifier,
        bodyOnClick = bodyOnClick,
        headline = headline,
        supportingText = supportingText,
        leadingContent = leadingContent,
        trailingContent = {
            val (div, switch) = createRefs()
            VerticalDivider(
                modifier = Modifier
                    .padding()
                    .padding(end = 16.dp)
                    .constrainAs(div) {
                        top link parent.top
                        bottom link parent.bottom
                        left link parent.left
                        right link switch.left
                        height = Dimension.fillToConstraints
                    }
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.constrainAs(switch) {
                    top link parent.top
                    bottom link parent.bottom
                    right link parent.right
                    left link div.right
                },
                enabled = enabled
            )
        },
        divider = divider
    )
}

@Composable
fun SwitchCard(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    cardOnClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            + modifier,
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(25.dp)
    ) {
        Box(
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = cardOnClick
            )
        ) {
            ConstraintLayout(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                val (txt, switch) = createRefs()
                Box(
                    modifier = Modifier.constrainAs(txt) {
                        top link parent.top
                        bottom link parent.bottom
                        left link parent.left
                        right link switch.left
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = text,
                        fontSize = 20.sp
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.constrainAs(switch) {
                        top link parent.top
                        bottom link parent.bottom
                        right link parent.right
                    },
                    interactionSource = interactionSource
                )
            }
        }
    }
}