package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * The destination field. Deliberately not an OutlinedTextField, so the height, radius and
 * colours all come from tokens and match the web client.
 */
@Composable
fun DkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_hint),
    onSearch: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
) {
    val colors = DkTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    // Interactive input: a full pill, per the product design language.
    val shape = RoundedCornerShape(DkTheme.radius.full)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboard?.hide()
                onSearch()
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = DkTheme.touch.comfortableTarget)
                    .clip(shape)
                    .background(colors.surface)
                    .border(
                        border = BorderStroke(
                            width = Dp.Hairline,
                            color = if (focused) colors.focus else colors.border,
                        ),
                        shape = shape,
                    )
                    .padding(horizontal = DkTheme.spacing.s4),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = colors.textMuted,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = DkTheme.spacing.s3),
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textMuted,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    DkIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_clear),
                        onClick = { onValueChange("") },
                        tint = colors.textMuted,
                    )
                }
            }
        },
    )
}
