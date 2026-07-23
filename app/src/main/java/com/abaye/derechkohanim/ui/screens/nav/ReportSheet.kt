package com.abaye.derechkohanim.ui.screens.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.REPORT_TYPES
import com.abaye.derechkohanim.data.model.RouteEventType
import com.abaye.derechkohanim.ui.components.DkBottomSheet
import com.abaye.derechkohanim.ui.icons.eventIcon
import com.abaye.derechkohanim.ui.icons.eventLabelRes
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

/** The report-type picker. Large tiles, one tap, dismissible, matching the web report flow. */
@Composable
fun ReportSheet(
    onPick: (RouteEventType) -> Unit,
    onDismiss: () -> Unit,
) {
    DkBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.report_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DkTheme.colors.text,
            modifier = Modifier.padding(bottom = DkTheme.spacing.s3),
        )
        Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
            REPORT_TYPES.forEach { kind ->
                ReportTile(kind = kind, onClick = { onPick(kind) })
            }
        }
    }
}

@Composable
private fun ReportTile(kind: RouteEventType, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DkTheme.radius.md),
        color = DkTheme.colors.surface,
        contentColor = DkTheme.colors.text,
        border = androidx.compose.foundation.BorderStroke(
            androidx.compose.ui.unit.Dp.Hairline,
            DkTheme.colors.border,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(DkTheme.spacing.s3),
        ) {
            Surface(
                shape = CircleShape,
                color = DkTheme.colors.surfaceMuted,
                contentColor = DkTheme.colors.danger,
                modifier = Modifier.size(DkTheme.touch.minTarget),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = eventIcon(kind), contentDescription = null)
                }
            }
            Text(
                text = stringResource(eventLabelRes(kind)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
    }
}

@Preview(name = "Report picker", showBackground = true, locale = "he")
@Composable
private fun ReportSheetPreview() {
    DerechKohanimTheme {
        Column(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            REPORT_TYPES.forEach { kind ->
                ReportTile(kind = kind, onClick = {})
            }
        }
    }
}
