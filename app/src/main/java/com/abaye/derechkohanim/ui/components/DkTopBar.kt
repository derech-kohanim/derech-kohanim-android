package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Civic top bar: a white bar with a hairline under it, a back arrow pointing to the inline
 * start, and the screen title.
 */
@Composable
fun DkTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = DkTheme.colors.surface, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = DkTheme.spacing.s1),
            ) {
                DkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DkTheme.colors.text,
                    modifier = Modifier.padding(start = DkTheme.spacing.s1),
                )
            }
            HorizontalDivider(thickness = Dp.Hairline, color = DkTheme.colors.border)
        }
    }
}
