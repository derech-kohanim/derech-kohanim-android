package com.abaye.derechkohanim.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.service.rememberLocationPermissionState
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    OnboardingScreen(
        state = state,
        onStringencySelected = viewModel::onStringencySelected,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onNext = viewModel::next,
        onBack = viewModel::back,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onStringencySelected: (Stringency) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val permissions = rememberLocationPermissionState(context)

    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
            ) {
                when (state.step) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.Stringency -> StringencyStep(state.stringency, onStringencySelected)
                    OnboardingStep.Terms -> TermsStep(state.termsAccepted, onTermsAcceptedChange)
                    OnboardingStep.Permissions -> PermissionsStep(
                        hasForeground = permissions.hasForeground,
                        hasBackground = permissions.hasBackground,
                        onAskForeground = permissions::askForeground,
                        onAskBackground = permissions::askBackground,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.step != OnboardingStep.Welcome) {
                    DkButton(
                        text = stringResource(R.string.action_back),
                        onClick = onBack,
                        variant = DkButtonVariant.Secondary,
                    )
                }
                DkButton(
                    text = stringResource(
                        if (state.step == OnboardingStep.Permissions) {
                            R.string.onboarding_finish
                        } else {
                            R.string.action_continue
                        }
                    ),
                    onClick = onNext,
                    enabled = state.step != OnboardingStep.Terms || state.termsAccepted,
                    large = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.displayMedium,
        color = DkTheme.colors.text,
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = DkTheme.colors.textMuted,
    )
    DkCard {
        Text(
            text = stringResource(R.string.onboarding_promise_title),
            style = MaterialTheme.typography.titleMedium,
            color = DkTheme.colors.text,
        )
        Text(
            text = stringResource(R.string.onboarding_promise_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
    }
    DkCard {
        Text(
            text = stringResource(R.string.onboarding_limits_title),
            style = MaterialTheme.typography.titleMedium,
            color = DkTheme.colors.text,
        )
        Text(
            text = stringResource(R.string.onboarding_limits_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
    }
}

@Composable
private fun StringencyStep(selected: Stringency, onSelect: (Stringency) -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_stringency_title),
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    Text(
        text = stringResource(R.string.onboarding_stringency_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
    Stringency.entries.forEach { option ->
        StringencyOption(
            option = option,
            selected = option == selected,
            onSelect = { onSelect(option) },
        )
    }
}

@Composable
fun StringencyOption(option: Stringency, selected: Boolean, onSelect: () -> Unit) {
    DkCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        borderColor = if (selected) DkTheme.colors.primary else DkTheme.colors.border,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.padding(start = DkTheme.spacing.s3)) {
                Text(
                    text = stringResource(stringencyTitleRes(option)),
                    style = MaterialTheme.typography.titleMedium,
                    color = DkTheme.colors.text,
                )
                Text(
                    text = stringResource(stringencyDescriptionRes(option)),
                    style = MaterialTheme.typography.bodySmall,
                    color = DkTheme.colors.textMuted,
                )
            }
        }
    }
}

fun stringencyTitleRes(stringency: Stringency): Int = when (stringency) {
    Stringency.BASIC -> R.string.stringency_basic
    Stringency.TAHARAT -> R.string.stringency_taharat
    Stringency.MACHMIRIM -> R.string.stringency_machmirim
}

fun stringencyDescriptionRes(stringency: Stringency): Int = when (stringency) {
    Stringency.BASIC -> R.string.stringency_basic_description
    Stringency.TAHARAT -> R.string.stringency_taharat_description
    Stringency.MACHMIRIM -> R.string.stringency_machmirim_description
}

@Composable
private fun TermsStep(accepted: Boolean, onAcceptedChange: (Boolean) -> Unit) {
    Text(
        text = stringResource(R.string.onboarding_terms_title),
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    Text(
        text = stringResource(R.string.onboarding_limits_body),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = accepted, role = Role.Checkbox, onValueChange = onAcceptedChange)
            .padding(vertical = DkTheme.spacing.s2),
    ) {
        Checkbox(checked = accepted, onCheckedChange = null)
        Text(
            text = stringResource(R.string.onboarding_terms_accept),
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.text,
            modifier = Modifier.padding(start = DkTheme.spacing.s3),
        )
    }
}

@Composable
private fun PermissionsStep(
    hasForeground: Boolean,
    hasBackground: Boolean,
    onAskForeground: () -> Unit,
    onAskBackground: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_permission_title),
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    Text(
        text = stringResource(R.string.onboarding_permission_body),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
    DkButton(
        text = stringResource(R.string.onboarding_permission_grant),
        onClick = onAskForeground,
        enabled = !hasForeground,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = stringResource(R.string.onboarding_background_title),
        style = MaterialTheme.typography.titleMedium,
        color = DkTheme.colors.text,
        modifier = Modifier.padding(top = DkTheme.spacing.s4),
    )
    Text(
        text = stringResource(R.string.onboarding_background_body),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
    DkButton(
        text = stringResource(R.string.onboarding_background_grant),
        onClick = onAskBackground,
        variant = DkButtonVariant.Secondary,
        enabled = hasForeground && !hasBackground,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = stringResource(R.string.onboarding_downloads_title),
        style = MaterialTheme.typography.titleMedium,
        color = DkTheme.colors.text,
        modifier = Modifier.padding(top = DkTheme.spacing.s4),
    )
    Text(
        text = stringResource(R.string.onboarding_downloads_body),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
}

@Preview(name = "Onboarding, stringency", showBackground = true, locale = "he")
@Composable
private fun OnboardingStringencyPreview() {
    DerechKohanimTheme {
        OnboardingScreen(
            state = OnboardingState(step = OnboardingStep.Stringency, stringency = Stringency.TAHARAT),
            onStringencySelected = {},
            onTermsAcceptedChange = {},
            onNext = {},
            onBack = {},
        )
    }
}

@Preview(name = "Onboarding, welcome English", showBackground = true, locale = "en")
@Composable
private fun OnboardingWelcomePreview() {
    DerechKohanimTheme {
        OnboardingScreen(
            state = OnboardingState(),
            onStringencySelected = {},
            onTermsAcceptedChange = {},
            onNext = {},
            onBack = {},
        )
    }
}
