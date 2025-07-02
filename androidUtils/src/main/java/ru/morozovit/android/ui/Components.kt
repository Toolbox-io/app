@file:Suppress(
    "NOTHING_TO_INLINE",
    "unused",
    "MemberVisibilityCanBePrivate",
    "UnusedReceiverParameter",
    "INVISIBLE_REFERENCE",
    "NOTHING_TO_INLINE",
    "ERROR_SUPPRESSION"
)

package ru.morozovit.android.ui

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.morozovit.android.R
import ru.morozovit.android.asAndroidScaleType
import ru.morozovit.android.clearFocusOnKeyboardDismiss
import ru.morozovit.android.invoke
import ru.morozovit.android.left
import ru.morozovit.android.link
import ru.morozovit.android.plus
import ru.morozovit.android.right
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.reflect.KClass

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
    headline: String,
    supportingText: String? = null,
    leadingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    trailingContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null,
    enabled: Boolean = true,
    divider: Boolean = false,
    dividerColor: Color = DividerDefaults.color,
    dividerThickness: Dp = DividerDefaults.Thickness,
    dividerAnimated: Boolean = false,
    onClick: (() -> Unit)? = null,
    bodyOnClick: (() -> Unit)? = null,
    leadingAndBodyShared: Boolean = false,
    bottomContent: (@Composable ConstraintLayoutScope.() -> Unit)? = null
) {
    Column {
        @Composable
        fun ProvideStyle(
            content: @Composable BoxScope?.() -> Unit
        ) {
            if (!enabled) {
                CompositionLocalProvider(
                    value = LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(
                        modifier = Modifier.alpha(0.38f),
                        content = content
                    )
                }
            } else {
                content(null)
            }
        }

        ProvideStyle {
            ConstraintLayout(
                modifier = Modifier.let {
                    var mod = it.fillMaxWidth()
                    if (onClick != null) {
                        mod += Modifier.clickable(onClick = onClick)
                    }
                    mod + modifier
                }
            ) {
                val (leading, listItem, trailing, btm) = createRefs()

                @Composable
                fun leadingBody() {
                    if (leadingContent != null) {
                        ConstraintLayout(
                            modifier = Modifier
                                .constrainAs(leading) {
                                    top link parent.top
                                    bottom link
                                            if (bottomContent != null) btm.top
                                            else parent.bottom
                                    left link parent.left
                                    right link listItem.left
                                }
                                .padding(
                                    start = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
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
                                if (bodyOnClick != null && !leadingAndBodyShared) {
                                    mod += Modifier.clickable(onClick = bodyOnClick)
                                }
                                mod + bodyModifier
                            },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }

                if (leadingAndBodyShared) {
                    Row(
                        modifier = Modifier
                            .constrainAs(listItem) {
                                top link parent.top
                                bottom link
                                        if (bottomContent != null) btm.top
                                        else parent.bottom
                                left link parent.left
                                right link
                                        if (trailingContent != null) trailing.left
                                        else parent.right
                                width = Dimension.fillToConstraints
                            }
                                +
                                if (bodyOnClick != null)
                                    Modifier.clickable(onClick = bodyOnClick)
                                else
                                    Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leadingBody()
                    }
                } else {
                    leadingBody()
                }

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
                            .padding(
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            ),
                        content = trailingContent
                    )
                }
                if (bottomContent != null) {
                    ConstraintLayout(
                        modifier = Modifier
                            .constrainAs(btm) {
                                bottom link parent.bottom
                                left link parent.left
                                right link parent.right
                                width = Dimension.fillToConstraints
                            }
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        content = bottomContent
                    )
                }
            }
        }
        if (dividerAnimated) {
            AnimatedVisibility(
                visible = divider,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = dividerThickness
                )
            }
        } else if (divider) {
            HorizontalDivider(
                color = dividerColor,
                thickness = dividerThickness
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
    crossinline listItemOnClick: () -> Unit = { onCheckedChange(!checked) },
    divider: Boolean = false,
    dividerColor: Color = DividerDefaults.color,
    dividerThickness: Dp = DividerDefaults.Thickness,
    dividerAnimated: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    ListItem(
        modifier = if (enabled) Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication(),
            onClick = { listItemOnClick() }
        ) + modifier else modifier,
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
        divider = divider,
        dividerColor = dividerColor,
        dividerThickness = dividerThickness,
        dividerAnimated = dividerAnimated,
        enabled = enabled
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
    dividerColor: Color = DividerDefaults.color,
    dividerThickness: Dp = DividerDefaults.Thickness,
    dividerAnimated: Boolean = false,
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
        divider = divider,
        dividerColor = dividerColor,
        dividerThickness = dividerThickness,
        dividerAnimated = dividerAnimated,
        leadingAndBodyShared = true,
        enabled = enabled
    )
}

@Composable
fun SwitchCard(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    cardOnClick: () -> Unit = { onCheckedChange(!checked) }
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                indication = LocalIndication(),
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
fun AnimatedCrosslineIcon(
    icon: ImageVector,
    crossline: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val lineDrawProgress by animateFloatAsState(
        targetValue = if (crossline) 0f else 1f, // 0f for hidden line, 1f for drawn line
        animationSpec = tween(300),
        label = "line_draw_progress"
    )

    val iconColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size(24.dp)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        val vectorPainter = rememberVectorPainter(icon)

        Canvas(modifier = Modifier.matchParentSize()) {
            val colorFilter = ColorFilter.tint(iconColor)

            // Draw the base Visibility icon directly to the canvas
            drawIntoCanvas {
                with(vectorPainter) {
                    draw(size, 1f, colorFilter)
                }
            }

            // Draw the animating main cross line and its 'background' strip on top
            if (!crossline || lineDrawProgress > 0f) {
                // Coordinates for the diagonal line (from top-left to bottom-right)
                val x1 = size.width * 0.05f
                val y1 = size.height * 0.15f
                val x2 = size.width * 0.825f
                val y2 = size.height * 0.925f

                // Current end point of the drawing line based on animation progress
                val currentX = x1 + (x2 - x1) * lineDrawProgress
                val currentY = y1 + (y2 - y1) * lineDrawProgress

                val mainLineThickness = 2.dp.toPx()
                val backgroundStripThickness = 2.dp.toPx() // Thickness of the 'blank background' strip

                // Calculate perpendicular vector for thickness and offset for the second line
                val dx = x2 - x1
                val dy = y2 - y1
                val length = sqrt(dx * dx + dy * dy)
                // Perpendicular vector normalized (points to the 'left' side relative to line direction)
                val nx = -dy / length
                val ny = dx / length

                // Global offset to shift both lines further to the left
                val globalLineOffset = mainLineThickness / 2

                // --- Draw the 'Blank background' strip path (filled rectangle) first, attached to the left/top-left of the main line ---
                // This path will now erase pixels
                drawPath(
                    path = Path().apply {
                        val offsetFromMainLineCenter = mainLineThickness / 2 + backgroundStripThickness / 2 - 1.dp.toPx()

                        // Points for the strip, offset to the 'top-left' side by subtracting nx and ny
                        // Apply globalLineOffset to shift both lines further left
                        val p1x = x1 - nx * (offsetFromMainLineCenter + globalLineOffset)
                        val p1y = y1 - ny * (offsetFromMainLineCenter + globalLineOffset)
                        val p2x = x1 - nx * (offsetFromMainLineCenter + backgroundStripThickness + globalLineOffset)
                        val p2y = y1 - ny * (offsetFromMainLineCenter + backgroundStripThickness + globalLineOffset)

                        val cp1x = currentX - nx * (offsetFromMainLineCenter + globalLineOffset)
                        val cp1y = currentY - ny * (offsetFromMainLineCenter + globalLineOffset)
                        val cp2x = currentX - nx * (offsetFromMainLineCenter + backgroundStripThickness + globalLineOffset)
                        val cp2y = currentY - ny * (offsetFromMainLineCenter + backgroundStripThickness + globalLineOffset)

                        moveTo(p1x, p1y)
                        lineTo(p2x, p2y)
                        lineTo(cp2x, cp2y)
                        lineTo(cp1x, cp1y)
                        close()
                    },
                    color = Color.Transparent, // This color will be used for blending calculation
                    style = Fill,
                    blendMode = BlendMode.Clear // This makes it erase pixels underneath
                )

                // --- Draw the Main cross line path (filled rectangle) second ---
                drawPath(
                    path = Path().apply {
                        // Apply globalLineOffset to shift both lines further left
                        val p1x = x1 - nx * (mainLineThickness / 2 + globalLineOffset)
                        val p1y = y1 - ny * (mainLineThickness / 2 + globalLineOffset)
                        val p2x = x1 + nx * (mainLineThickness / 2 - globalLineOffset)
                        val p2y = y1 + ny * (mainLineThickness / 2 - globalLineOffset)

                        val cp1x = currentX - nx * (mainLineThickness / 2 + globalLineOffset)
                        val cp1y = currentY - ny * (mainLineThickness / 2 + globalLineOffset)
                        val cp2x = currentX + nx * (mainLineThickness / 2 - globalLineOffset)
                        val cp2y = currentY + ny * (mainLineThickness / 2 - globalLineOffset)

                        moveTo(p1x, p1y)
                        lineTo(p2x, p2y)
                        lineTo(cp2x, cp2y)
                        lineTo(cp1x, cp1y)
                        close()
                    },
                    color = iconColor,
                    style = Fill
                )
            }
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight
                    )
                    .clearFocusOnKeyboardDismiss(),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    visualTransformation = visualTransformation,
                    innerTextField = {
                        AnimatedContent(
                            targetState = visualTransformation
                        ) {
                            innerTextField()
                        }
                    },
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape,
                        )
                    }
                )
            }
        )
    }
}

@Composable
inline fun SecureTextField(
    value: String,
    noinline onValueChange: (String) -> Unit,
    hidden: Boolean,
    noinline onHiddenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle(),
    noinline label: @Composable (() -> Unit)? = null,
    noinline leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    TweakedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        visualTransformation =
            if (hidden)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
        label = label,
        trailingIcon = {
            IconButton(
                onClick = { onHiddenChange(!hidden) }
            ) {
                AnimatedCrosslineIcon(
                    icon = Icons.Filled.Visibility,
                    crossline = hidden,
                    contentDescription = if (hidden)
                        stringResource(R.string.show_pw)
                    else
                        stringResource(R.string.hide_pw)
                )
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

@Composable
inline fun ToggleIconButton(
    checked: Boolean,
    crossinline onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    checkedColors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    uncheckedColors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    noinline content: @Composable () -> Unit
) {
    if (checked) {
        FilledIconButton(
            onClick = { onCheckedChange(false) },
            modifier = Modifier.size(60.dp, 60.dp) + modifier,
            enabled = enabled,
            shape = shape,
            colors = checkedColors,
            interactionSource = interactionSource,
            content = content
        )
    } else {
        FilledTonalIconButton(
            onClick = { onCheckedChange(true) },
            modifier = Modifier.size(60.dp, 60.dp) + modifier,
            enabled = enabled,
            shape = shape,
            colors = uncheckedColors,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
inline fun Mipmap(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val update: (ImageView) -> Unit = {
        it.setImageResource(id)
        it.contentDescription = contentDescription
        it.alpha = alpha
        it.colorFilter = colorFilter?.asAndroidColorFilter()
        it.scaleType = contentScale.asAndroidScaleType()
    }

    AndroidView(
        modifier = modifier,
        factory = {
            ImageView(it).apply {
                update(this)
            }
        },
        update = update
    )
}

@Composable
inline fun ElevatedButton(
    noinline onClick: () -> Unit,
    crossinline icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    crossinline content: @Composable RowScope.() -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.size(18.dp)
        ) {
            icon()
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
inline fun Button(
    noinline onClick: () -> Unit,
    crossinline icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    crossinline content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.size(18.dp)
        ) {
            icon()
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
inline fun FilledTonalButton(
    noinline onClick: () -> Unit,
    crossinline icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    crossinline content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.size(18.dp)
        ) {
            icon()
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
inline fun TextButton(
    noinline onClick: () -> Unit,
    crossinline icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonWithIconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    crossinline content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.size(18.dp)
        ) {
            icon()
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

class RadioButtonControllerScope<T> @PublishedApi internal constructor() {
    private val radioButtons = mutableMapOf<T, MutableState<Boolean>>()

    val checkedItemAsState: MutableState<T?> = mutableStateOf(null)

    private fun processCheckedChange(id: T) {
        val toModify = mutableListOf<T>()

        radioButtons.forEach { (i) ->
            if (i != id) {
                toModify.add(i)
            }
        }

        toModify.forEach { i ->
            radioButtons[i]!!.value = false
        }
    }

    fun addRadioButton(id: T, checked: MutableState<Boolean>, coroutineScope: CoroutineScope) {
        radioButtons[id] = checked
        coroutineScope.launch {
            snapshotFlow { checked.value }.collect {
                if (it) {
                    processCheckedChange(id)
                    checkedItemAsState.value = checkedItem
                }
            }
        }
    }

    fun addRadioButtons(
        vararg radioButtons: Pair<T, MutableState<Boolean>>,
        coroutineScope: CoroutineScope
    ) =
        radioButtons.forEach { (id, checked) ->
            addRadioButton(id, checked, coroutineScope)
        }

    fun removeRadioButton(id: T) = radioButtons.remove(id)

    fun isChecked(id: T) = radioButtons[id]!!.value
    val checkedItem: T?
        get() = radioButtons.firstNotNullOfOrNull { if (it.value.value) it.key else null }

    fun clearCheckedItems() = radioButtons.values.forEach { it.value = false }

    companion object {
        inline fun <reified IdT: Id<T>, T> Ids() = Ids(IdT::class)
    }

    interface Id<T> {
        val id: T
    }

    class RandomIntId internal constructor(): Id<Int> {
        override val id = Random.Default.nextInt()
    }

    class OrderedIntId internal constructor(override val id: Int): Id<Int>

    open class Ids<IdT: Id<T>, T> @PublishedApi internal constructor(
        private val clazz: KClass<IdT>?
    ) {
        private val newId: () -> IdT = {
            clazz!!.java.getConstructor().newInstance()
        }

        open operator fun component1() = newId()
        open operator fun component2() = newId()
        open operator fun component3() = newId()
        open operator fun component4() = newId()
        open operator fun component5() = newId()
        open operator fun component6() = newId()
        open operator fun component7() = newId()
        open operator fun component8() = newId()
        open operator fun component9() = newId()
        open operator fun component10() = newId()
        open operator fun component11() = newId()
        open operator fun component12() = newId()
        open operator fun component13() = newId()
        open operator fun component14() = newId()
        open operator fun component15() = newId()
        open operator fun component16() = newId()
    }

    class OrderedIntIds internal constructor(): Ids<OrderedIntId, Int>(null) {
        override fun component1() = OrderedIntId(0)
        override fun component2() = OrderedIntId(1)
        override fun component3() = OrderedIntId(2)
        override fun component4() = OrderedIntId(3)
        override fun component5() = OrderedIntId(4)
        override fun component6() = OrderedIntId(5)
        override fun component7() = OrderedIntId(6)
        override fun component8() = OrderedIntId(7)
        override fun component9() = OrderedIntId(8)
        override fun component10() = OrderedIntId(9)
        override fun component11() = OrderedIntId(10)
        override fun component12() = OrderedIntId(11)
        override fun component13() = OrderedIntId(12)
        override fun component14() = OrderedIntId(13)
        override fun component15() = OrderedIntId(14)
        override fun component16() = OrderedIntId(15)
    }

    @Suppress("SameReturnValue")
    class Ints internal constructor() {
        operator fun component1() = 0
        operator fun component2() = 1
        operator fun component3() = 2
        operator fun component4() = 3
        operator fun component5() = 4
        operator fun component6() = 5
        operator fun component7() = 6
        operator fun component8() = 7
        operator fun component9() = 8
        operator fun component10() = 9
        operator fun component11() = 10
        operator fun component12() = 11
        operator fun component13() = 12
        operator fun component14() = 13
        operator fun component15() = 14
        operator fun component16() = 15
    }

    fun createRandomId() = RandomIntId()
    fun createRandomIds() = Ids<RandomIntId, Int>()

    fun createOrderedId(index: Int) = OrderedIntId(index)
    fun createOrderedIds() = OrderedIntIds()

    fun createIntIds() = Ints()
}

@Composable
inline fun <T> rememberRadioButtonController() = rememberSaveable { RadioButtonControllerScope<T>() }

@Composable
inline fun <T> RadioButtonController(
    content: @Composable RadioButtonControllerScope<T>.() -> Unit
) {
    content(RadioButtonControllerScope())
}

@Composable
inline fun CheckboxWithText(
    checked: Boolean,
    crossinline onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(contentPadding)
            +
            if (enabled)
                Modifier.toggleable(
                    value = checked,
                    onValueChange = { onCheckedChange(!checked) },
                    role = Role.Checkbox
                )
            else Modifier
            + modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = colors,
        )
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

@Composable
inline fun SwitchWithText(
    checked: Boolean,
    crossinline onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(contentPadding)
            +
            if (enabled)
                Modifier.toggleable(
                    value = checked,
                    onValueChange = { onCheckedChange(!checked) },
                    role = Role.Switch
                )
            else Modifier
            + modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = colors,
        )
    }
}


@Composable
inline fun RadioButtonWithText(
    modifier: Modifier = Modifier,
    selected: Boolean,
    crossinline onSelectedChange: () -> Unit,
    enabled: Boolean = true,
    crossinline content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .selectable(
                selected = selected,
                onClick = {
                    if (enabled) onSelectedChange()
                },
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp)
            + modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        ProvideTextStyle(value = MaterialTheme.typography.bodyLarge) {
            content()
        }
    }
}

@Composable
inline fun SimpleAlertDialog(
    open: Boolean,
    crossinline onDismissRequest: () -> Unit,
    crossinline onPositiveButtonClick: (() -> Unit) = {},
    crossinline onNegativeButtonClick: (() -> Unit) = {},
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    title: String,
    body: String? = null,
    dsaString: String? = null,
    crossinline onDsa: (() -> Unit) = {},
    noinline icon: @Composable (() -> Unit) = {},
) {
    if (open) {
        var dsaChecked by remember { mutableStateOf(false) }

        AlertDialog(
            icon = icon,
            title = {
                Text(text = title)
            },
            text =
            if (body != null || dsaString != null) {
                {
                    if (body != null) {
                        Text(text = body)
                    }
                    if (dsaString != null) {
                        CheckboxWithText(
                            checked = dsaChecked,
                            onCheckedChange = { dsaChecked = it },
                            contentPadding = PaddingValues(top = 10.dp)
                        ) {
                            Text(text = dsaString)
                        }
                    }
                }
            } else null,
            onDismissRequest = { onDismissRequest() },
            confirmButton = {
                if (!positiveButtonText.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onPositiveButtonClick()
                            if (dsaChecked) onDsa()
                            onDismissRequest()
                        }
                    ) {
                        Text(positiveButtonText)
                    }
                }
            },
            dismissButton = if (!negativeButtonText.isNullOrBlank()) {
                {
                    TextButton(
                        onClick = {
                            onNegativeButtonClick()
                            if (dsaChecked) onDsa()
                            onDismissRequest()
                        }
                    ) {
                        Text(negativeButtonText)
                    }
                }
            } else null
        )
    }
}

@Composable
inline fun RadioGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.selectableGroup() + modifier) {
        content()
    }
}

@Composable
inline fun SwipeToDismissBackground(
    dismissState: SwipeToDismissBoxState,
    startToEndColor: Color = Color(0xFFFF1744),
    startToEndIcon: @Composable () -> Unit = {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete"
        )
    },
    endToStartColor: Color = Color(0xFF1DE9B6),
    endToStartIcon: @Composable () -> Unit = {
        Icon(
            Icons.Filled.Archive,
            contentDescription = "Archive"
        )
    }
) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> startToEndColor
        SwipeToDismissBoxValue.EndToStart -> endToStartColor
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        startToEndIcon()
        Spacer(modifier = Modifier)
        endToStartIcon()
    }
}

object CategoryDefaults {
    val margin = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp)
    val dividerColor @Composable get() = MaterialTheme.colorScheme.surface
    val dividerThickness: Dp = 2.dp
}

@Composable
inline fun ColumnScope.Category(
    modifier: Modifier = Modifier,
    title: String? = null,
    margin: PaddingValues = CategoryDefaults.margin,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    noinline content: @Composable ColumnScope.() -> Unit
) {
    if (title != null) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 10.dp, start = 32.dp, end = 32.dp)
        )
    }
    Card(
        modifier = modifier
            .padding(margin)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = cardColors(containerColor = containerColor),
        content = content
    )
}