package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyDescriptionRes
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyTitleRes
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * The avoidance level chip shown over the map: "רמת הימנעות: טהרת הכהנים". It says what the
 * level is (not just its name) and is the shortcut to change it.
 */
@Composable
fun StringencyChip(
    stringency: Stringency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DkTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(DkTheme.radius.full),
        color = colors.surface,
        contentColor = colors.primary,
        border = BorderStroke(1.dp, colors.borderStrong),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
            modifier = Modifier.padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s2),
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(DkTheme.spacing.s4))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.textMuted)) {
                        append(stringResource(R.string.avoidance_level_label))
                    }
                    append(' ')
                    withStyle(SpanStyle(color = colors.text)) {
                        append(stringResource(stringencyTitleRes(stringency)))
                    }
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** The three avoidance levels as Civic list rows (radio, title, explanation) in one bordered box. */
@Composable
fun StringencyOptions(
    selected: Stringency,
    onSelect: (Stringency) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DkTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DkTheme.radius.md),
        color = colors.surface,
        border = BorderStroke(Dp.Hairline, colors.border),
    ) {
        Column {
            Stringency.entries.forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(thickness = Dp.Hairline, color = colors.border)
                val isSelected = option == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                        .background(if (isSelected) colors.band else colors.surface)
                        .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s3),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.primary,
                            unselectedColor = colors.borderStrong,
                        ),
                    )
                    Column(modifier = Modifier.padding(start = DkTheme.spacing.s3).weight(1f)) {
                        Text(
                            text = stringResource(stringencyTitleRes(option)),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.text,
                        )
                        Text(
                            text = stringResource(stringencyDescriptionRes(option)),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The one place the avoidance level is changed ("בחירת רמת הימנעות"), opened from the home pill
 * and from the avoidance settings row. The choice is a draft until Save. Saving a less strict
 * level does not apply at once: the dialog turns into a confirmation step first, because a lower
 * level lets routes through zones the user has avoided until now.
 */
@Composable
fun StringencyDialog(
    current: Stringency,
    onSave: (Stringency) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    var confirmLower by remember { mutableStateOf(false) }
    val colors = DkTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(DkTheme.radius.lg),
        title = {
            Text(
                text = stringResource(
                    if (confirmLower) R.string.avoidance_lower_confirm_title else R.string.avoidance_dialog_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text,
            )
        },
        text = {
            if (confirmLower) {
                Text(
                    text = stringResource(
                        R.string.avoidance_lower_confirm_body,
                        stringResource(stringencyTitleRes(current)),
                        stringResource(stringencyTitleRes(draft)),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textBody,
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
                ) {
                    Text(
                        text = stringResource(R.string.avoidance_level_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                    StringencyOptions(selected = draft, onSelect = { draft = it })
                }
            }
        },
        confirmButton = {
            if (confirmLower) {
                DkButton(
                    text = stringResource(R.string.avoidance_lower_confirm),
                    onClick = { onSave(draft) },
                    variant = DkButtonVariant.Danger,
                )
            } else {
                DkButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        when {
                            draft == current -> onDismiss()
                            draft.ordinal < current.ordinal -> confirmLower = true
                            else -> onSave(draft)
                        }
                    },
                )
            }
        },
        dismissButton = {
            if (confirmLower) {
                DkButton(
                    text = stringResource(R.string.action_back),
                    onClick = { confirmLower = false },
                    variant = DkButtonVariant.Ghost,
                )
            } else {
                DkButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = DkButtonVariant.Ghost,
                )
            }
        },
    )
}
