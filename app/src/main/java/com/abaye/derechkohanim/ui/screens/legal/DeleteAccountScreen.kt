package com.abaye.derechkohanim.ui.screens.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkTextField
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun DeleteAccountRoute(
    onBack: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeleteAccountScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onReasonChange = viewModel::onReasonChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}

/**
 * The deletion-request form, mirroring the web app's /delete-account page. Submitting records a
 * request and nothing more: an operator verifies the requester before anything is removed, which
 * is the only honest thing a form with no proof of identity can do.
 */
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountState,
    onEmailChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(DkTheme.spacing.s2),
            ) {
                DkIconButton(
                    icon = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                    modifier = Modifier.rotate(if (rtl) 90f else -90f),
                )
                Text(
                    text = stringResource(R.string.delete_account_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DkTheme.colors.text,
                    modifier = Modifier.padding(start = DkTheme.spacing.s2),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = DkTheme.spacing.s4,
                        end = DkTheme.spacing.s4,
                        bottom = DkTheme.spacing.s4,
                    ),
                verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
            ) {
                if (state.sent) {
                    SentCard()
                    DkButton(
                        text = stringResource(R.string.action_back),
                        onClick = onBack,
                        variant = DkButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    return@Column
                }

                Text(
                    text = stringResource(R.string.delete_account_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DkTheme.colors.textMuted,
                )

                if (state.emailFromAccount) {
                    // The server records the signed-in account's own address whatever the field
                    // says, so showing it read-only is the truthful presentation.
                    DkCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.delete_account_email_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = DkTheme.colors.textMuted,
                        )
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DkTheme.colors.text,
                            modifier = Modifier.padding(top = DkTheme.spacing.s1),
                        )
                    }
                } else {
                    DkTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = stringResource(R.string.delete_account_email_label),
                        supportingText = stringResource(R.string.delete_account_email_hint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                }

                DkTextField(
                    value = state.reason,
                    onValueChange = onReasonChange,
                    label = stringResource(R.string.delete_account_reason_label),
                    supportingText = stringResource(R.string.delete_account_reason_hint),
                )

                Text(
                    text = stringResource(
                        if (state.signedIn) {
                            R.string.delete_account_signed_in_note
                        } else {
                            R.string.delete_account_guest_note
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = DkTheme.colors.textMuted,
                )

                state.errorCode?.let { code ->
                    Text(
                        text = stringResource(deleteErrorRes(code)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DkTheme.colors.danger,
                    )
                }

                DkButton(
                    text = stringResource(R.string.delete_account_submit),
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    loading = state.busy,
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                DkCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.delete_account_what_happens_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = DkTheme.colors.text,
                    )
                    listOf(
                        R.string.delete_account_step_recorded,
                        R.string.delete_account_step_verified,
                        R.string.delete_account_step_removed,
                        R.string.delete_account_step_irreversible,
                    ).forEach { step ->
                        Row(modifier = Modifier.padding(top = DkTheme.spacing.s2)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DkTheme.colors.textMuted,
                            )
                            Text(
                                text = stringResource(step),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DkTheme.colors.textMuted,
                                modifier = Modifier.padding(start = DkTheme.spacing.s2),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentCard() {
    DkCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = DkTheme.colors.primary,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = stringResource(R.string.delete_account_sent_title),
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
        Text(
            text = stringResource(R.string.delete_account_sent_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
    }
}

private fun deleteErrorRes(code: String): Int = when (code) {
    ErrorCode.NETWORK -> R.string.error_network
    else -> R.string.delete_account_failed
}

@Preview(name = "Delete account", showBackground = true, locale = "he")
@Composable
private fun DeleteAccountPreview() {
    DerechKohanimTheme {
        DeleteAccountScreen(
            state = DeleteAccountState(signedIn = false),
            onEmailChange = {},
            onReasonChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
