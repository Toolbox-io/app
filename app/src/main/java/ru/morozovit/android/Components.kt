@file:Suppress("NOTHING_TO_INLINE")

package ru.morozovit.android

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import ru.morozovit.ultimatesecurity.R

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
    bodyOnClick: (() -> Unit)? = null,
    bottomContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null
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
        val (leading, listItem, trailing, div, btm) = createRefs()
        if (leadingContent != null) {
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(leading) {
                        top link
                                if (bottomContent != null) btm.top
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
                                if (bottomContent != null) btm.top
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
                                if (bottomContent != null) btm.top
                                else parent.bottom
                        right link parent.right
                        left link listItem.right
                    }
                    .padding(end = 16.dp),
                content = trailingContent
            )
        }
        if (bottomContent != null) {
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(btm) {
                        bottom link
                                if (divider) div.top
                                else parent.bottom
                        left link parent.left
                        right link parent.right
                        width = Dimension.fillToConstraints
                    }
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                content = bottomContent
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

@Composable
inline fun SecureTextField(
    value: String,
    noinline onValueChange: (String) -> Unit,
    passwordHidden: Boolean,
    noinline visibilityOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    noinline label: @Composable (() -> Unit)? = null,
    noinline leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        visualTransformation =
        if (!passwordHidden)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        label = label,
        trailingIcon = {
            IconButton(
                onClick = visibilityOnClick
            ) {
                val visibilityIcon =
                    if (passwordHidden) Icons.Filled.Visibility else
                        Icons.Filled.VisibilityOff
                // Provide localized description for accessibility services
                val description =
                    if (passwordHidden)
                        stringResource(R.string.show_pw)
                    else
                        stringResource(R.string.hide_pw)
                Icon(imageVector = visibilityIcon, contentDescription = description)
            }
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false
        ),
        maxLines = 1,
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        leadingIcon = leadingIcon,
        isError = isError,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}