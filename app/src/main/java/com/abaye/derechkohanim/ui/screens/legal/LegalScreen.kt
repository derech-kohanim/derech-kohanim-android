package com.abaye.derechkohanim.ui.screens.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.Dp
import com.abaye.derechkohanim.ui.components.DkTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.MarkdownText
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * The two policy documents on their own destinations, so they can be opened from settings, from
 * the onboarding terms step and from the registration form without leaving the app.
 *
 * The terms are the versioned document a user accepts, so they come from the server and the
 * screen shows what was actually accepted; the copy bundled in the string resources is the
 * fallback for a device with no connection, and the screen says so when it falls back. The
 * privacy policy has no version and no acceptance, so it simply ships with the app.
 */
@Composable
fun TermsRoute(
    onBack: () -> Unit,
    viewModel: TermsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LegalScreen(
        title = state.title.ifBlank { stringResource(R.string.legal_terms_title) },
        body = state.body.ifBlank { stringResource(R.string.legal_terms_body) },
        loading = state.loading,
        bundled = state.usingBundledCopy,
        onBack = onBack,
    )
}

@Composable
fun PrivacyRoute(
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    LegalScreen(
        title = stringResource(R.string.legal_privacy_title),
        body = stringResource(R.string.legal_privacy_body),
        loading = false,
        bundled = false,
        onBack = onBack,
        footer = {
            DkButton(
                text = stringResource(R.string.legal_delete_account_action),
                onClick = onDeleteAccount,
                variant = DkButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * Inline links to the two documents, for the places that reference them in passing: the
 * onboarding terms step and the registration form. Both sit inside a flow, so these open the
 * document as its own destination and come back to exactly where the reader was.
 */
@Composable
fun LegalLinksRow(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DkButton(
            text = stringResource(R.string.legal_terms_title),
            onClick = onOpenTerms,
            variant = DkButtonVariant.Ghost,
        )
        DkButton(
            text = stringResource(R.string.legal_privacy_title),
            onClick = onOpenPrivacy,
            variant = DkButtonVariant.Ghost,
        )
    }
}

@Composable
fun LegalScreen(
    title: String,
    body: String,
    loading: Boolean,
    bundled: Boolean,
    onBack: () -> Unit,
    footer: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DkTheme.colors.background),
    ) {
        DkTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            if (bundled) {
                // The document on screen is the shipped copy, not the server's, and a reader
                // deciding whether to accept deserves to know that. A Civic notice on the band.
                Surface(
                    shape = RoundedCornerShape(DkTheme.radius.md),
                    color = DkTheme.colors.band,
                    border = BorderStroke(Dp.Hairline, DkTheme.colors.border),
                ) {
                    Text(
                        text = stringResource(R.string.legal_bundled_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = DkTheme.colors.textBody,
                        modifier = Modifier.padding(DkTheme.spacing.s3),
                    )
                }
            }
            // A document reads as running text on the page, not inside a card.
            if (loading) {
                Text(
                    text = stringResource(R.string.state_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.textMuted,
                )
            } else {
                MarkdownText(
                    markdown = body,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DkTheme.colors.textBody,
                    numberHeadings = true,
                )
            }
            footer?.invoke()
        }
    }
}

@Preview(name = "Privacy policy", showBackground = true, locale = "he")
@Composable
private fun PrivacyPreview() {
    DerechKohanimTheme {
        PrivacyRoute(onBack = {}, onDeleteAccount = {})
    }
}
