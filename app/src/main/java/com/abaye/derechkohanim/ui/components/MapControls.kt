package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * The map control cluster a driver expects, one-handed on the end edge. Every button is a
 * comfortable touch target from the tokens. Heading-up and overview are stateful toggles.
 */
@Composable
fun MapControls(
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    headingUp: Boolean? = null,
    onToggleOrientation: (() -> Unit)? = null,
    onOverview: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
    ) {
        if (headingUp != null && onToggleOrientation != null) {
            DkFloatingIconButton(
                icon = if (headingUp) Icons.Filled.Navigation else Icons.Filled.Explore,
                contentDescription = stringResource(R.string.cd_orientation),
                onClick = onToggleOrientation,
            )
        }
        if (onOverview != null) {
            DkFloatingIconButton(
                icon = Icons.Filled.ZoomOutMap,
                contentDescription = stringResource(R.string.cd_overview),
                onClick = onOverview,
            )
        }
        DkFloatingIconButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.cd_zoom_in),
            onClick = onZoomIn,
        )
        DkFloatingIconButton(
            icon = Icons.Filled.Remove,
            contentDescription = stringResource(R.string.cd_zoom_out),
            onClick = onZoomOut,
        )
        DkFloatingIconButton(
            icon = Icons.Filled.MyLocation,
            contentDescription = stringResource(R.string.cd_recenter),
            onClick = onRecenter,
        )
    }
}
