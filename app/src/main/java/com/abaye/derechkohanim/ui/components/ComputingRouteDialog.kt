package com.abaye.derechkohanim.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Blocking modal shown while a route is being computed. It cannot be dismissed by back or by
 * tapping outside - the only way out is Cancel, which the caller wires to an actual
 * cancellation of the in-flight request. Everything is token-driven and theme-aware.
 */
@Composable
fun ComputingRouteDialog(onCancel: () -> Unit, destinationName: String? = null) {
    Dialog(
        onDismissRequest = { /* blocking: dismissal only through Cancel */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val computing = stringResource(R.string.cd_route_computing)
        DkCard(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = computing
                    liveRegion = LiveRegionMode.Polite
                },
            color = DkTheme.colors.surfaceOverlay,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DkTheme.spacing.s4),
            ) {
                AppLoader(size = AppLoaderSize.Medium)
                Text(
                    text = if (destinationName.isNullOrBlank()) {
                        stringResource(R.string.route_computing)
                    } else {
                        stringResource(R.string.route_computing_to, destinationName)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = DkTheme.colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = DkTheme.spacing.s4),
                )
            }
            DkButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancel,
                variant = DkButtonVariant.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DkTheme.spacing.s2),
            )
        }
    }
}



@Preview(name = "Computing route", showBackground = true, locale = "he")
@Composable
private fun ComputingRoutePreview() {
    DerechKohanimTheme {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            // The dialog itself cannot host in a preview, so the content is previewed inline.
            DkCard(color = DkTheme.colors.surfaceOverlay) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DkTheme.spacing.s4),
                ) {
                    AppLoader(size = AppLoaderSize.Medium)
                    Text(
                        text = stringResource(R.string.route_computing),
                        style = MaterialTheme.typography.titleMedium,
                        color = DkTheme.colors.text,
                        modifier = Modifier.padding(top = DkTheme.spacing.s4),
                    )
                }
                DkButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = {},
                    variant = DkButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = DkTheme.spacing.s2),
                )
            }
        }
    }
}

@Preview(name = "Computing route dark", showBackground = true, locale = "en")
@Composable
private fun ComputingRouteDarkPreview() {
    DerechKohanimTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(DkTheme.spacing.s4)) {
            DkCard(color = DkTheme.colors.surfaceOverlay) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DkTheme.spacing.s4),
                ) {
                    AppLoader(size = AppLoaderSize.Medium)
                    Text(
                        text = stringResource(R.string.route_computing),
                        style = MaterialTheme.typography.titleMedium,
                        color = DkTheme.colors.text,
                        modifier = Modifier.padding(top = DkTheme.spacing.s4),
                    )
                }
                DkButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = {},
                    variant = DkButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth().padding(top = DkTheme.spacing.s2),
                )
            }
        }
    }
}
