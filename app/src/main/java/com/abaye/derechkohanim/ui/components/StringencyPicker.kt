package com.abaye.derechkohanim.ui.components

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
        color = colors.primarySoft,
        contentColor = colors.primary,
        border = BorderStroke(Dp.Hairline, colors.primary.copy(alpha = 0.25f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
            modifier = Modifier.padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s2),
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(DkTheme.spacing.s4))
            Text(
                text = stringResource(R.string.avoidance_level_chip, stringResource(stringencyTitleRes(stringency))),
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

/** A planning sheet to change the avoidance level from the map or a route screen. */
@Composable
fun StringencyPickerSheet(
    selected: Stringency,
    onSelect: (Stringency) -> Unit,
    onDismiss: () -> Unit,
) {
    DkBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.avoidance_level_title),
            style = MaterialTheme.typography.titleLarge,
            color = DkTheme.colors.text,
        )
        Text(
            text = stringResource(R.string.avoidance_level_hint),
            style = MaterialTheme.typography.bodySmall,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s1, bottom = DkTheme.spacing.s4),
        )
        StringencyOptions(
            selected = selected,
            onSelect = {
                onSelect(it)
                onDismiss()
            },
        )
    }
}
