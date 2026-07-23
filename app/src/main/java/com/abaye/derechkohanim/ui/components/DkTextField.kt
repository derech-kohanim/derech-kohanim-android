package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.theme.DkTheme

/** Labelled text input built on the same tokens as the search field. */
@Composable
fun DkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = DkTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // Interactive input: a full pill, per the product design language.
    val shape = RoundedCornerShape(DkTheme.radius.full)
    val borderColor = when {
        errorText != null -> colors.danger
        focused -> colors.focus
        else -> colors.border
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
            modifier = Modifier.padding(bottom = DkTheme.spacing.s1),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = DkTheme.touch.comfortableTarget)
                        .clip(shape)
                        .background(colors.surface)
                        .border(BorderStroke(Dp.Hairline, borderColor), shape)
                        .padding(horizontal = DkTheme.spacing.s4),
                ) { innerTextField() }
            },
        )
        val helper = errorText ?: supportingText
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = if (errorText != null) colors.danger else colors.textMuted,
                modifier = Modifier.padding(top = DkTheme.spacing.s1),
            )
        }
    }
}
