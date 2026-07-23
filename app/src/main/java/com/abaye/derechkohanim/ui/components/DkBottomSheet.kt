package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Sheets are for planning, never for driving: spec 17.2 forbids anything modal while
 * navigation is running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DkBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = state,
        modifier = modifier,
        containerColor = DkTheme.colors.surfaceOverlay,
        contentColor = DkTheme.colors.text,
        shape = RoundedCornerShape(
            topStart = DkTheme.radius.xl,
            topEnd = DkTheme.radius.xl,
            bottomStart = DkTheme.radius.xs,
            bottomEnd = DkTheme.radius.xs,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                start = DkTheme.spacing.s4,
                end = DkTheme.spacing.s4,
                bottom = DkTheme.spacing.s8,
            ),
            content = content,
        )
    }
}
