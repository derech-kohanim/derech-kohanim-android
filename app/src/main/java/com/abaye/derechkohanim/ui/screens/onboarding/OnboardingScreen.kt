package com.abaye.derechkohanim.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.service.rememberLocationPermissionState
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.MarkdownText
import com.abaye.derechkohanim.ui.screens.legal.LegalLinksRow
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
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
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onStringencySelected: (Stringency) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
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
            // Progress: which of the six steps we are on. The welcome screen stays clean, so the
            // bar only appears once the tour has begun.
            if (!state.isFirst) {
                StepProgress(index = state.stepIndex, count = state.stepCount)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
            ) {
                when (state.step) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.HowItWorks -> HowItWorksStep()
                    OnboardingStep.DataSource -> DataSourceStep()
                    OnboardingStep.Terms -> TermsStep(
                        title = state.termsTitle,
                        body = state.termsBody,
                        loading = state.termsLoading,
                        accepted = state.termsAccepted,
                        onAcceptedChange = onTermsAcceptedChange,
                        onOpenTerms = onOpenTerms,
                        onOpenPrivacy = onOpenPrivacy,
                    )
                    OnboardingStep.Stringency -> StringencyStep(state.stringency, onStringencySelected)
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
                if (!state.isFirst) {
                    DkButton(
                        text = stringResource(R.string.action_back),
                        onClick = onBack,
                        variant = DkButtonVariant.Secondary,
                    )
                }
                DkButton(
                    text = stringResource(
                        if (state.isLast) R.string.onboarding_finish else R.string.action_continue
                    ),
                    onClick = onNext,
                    // Terms acceptance gates the flow, matching the web client and the auth screens.
                    enabled = state.step != OnboardingStep.Terms || state.termsAccepted,
                    large = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StepProgress(index: Int, count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / count.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DkTheme.radius.full)),
            color = DkTheme.colors.primary,
            trackColor = DkTheme.colors.surfaceMuted,
        )
        Text(
            text = stringResource(R.string.onboarding_progress, index + 1, count),
            style = MaterialTheme.typography.labelSmall,
            color = DkTheme.colors.textMuted,
        )
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
    ) {
        Spacer(Modifier.size(DkTheme.spacing.s10))
        Surface(
            shape = RoundedCornerShape(DkTheme.radius.xl),
            color = DkTheme.colors.primary,
            contentColor = DkTheme.colors.primaryContrast,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = null,
                modifier = Modifier
                    .padding(DkTheme.spacing.s5)
                    .size(DkTheme.spacing.s10),
            )
        }
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displayMedium,
            color = DkTheme.colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HowItWorksStep() {
    Text(
        text = stringResource(R.string.onboarding_how_title),
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    HowItWorksCard(
        icon = Icons.Filled.Search,
        title = stringResource(R.string.onboarding_how_step1_title),
        body = stringResource(R.string.onboarding_how_step1_body),
    )
    HowItWorksCard(
        icon = Icons.AutoMirrored.Filled.AltRoute,
        title = stringResource(R.string.onboarding_how_step2_title),
        body = stringResource(R.string.onboarding_how_step2_body),
    )
    HowItWorksCard(
        icon = Icons.Filled.EventAvailable,
        title = stringResource(R.string.onboarding_how_step3_title),
        body = stringResource(R.string.onboarding_how_step3_body),
    )
    HowItWorksCard(
        icon = Icons.Filled.RecordVoiceOver,
        title = stringResource(R.string.onboarding_how_step4_title),
        body = stringResource(R.string.onboarding_how_step4_body),
    )
}

@Composable
private fun HowItWorksCard(icon: ImageVector, title: String, body: String) {
    DkCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(DkTheme.radius.md),
                color = DkTheme.colors.surfaceMuted,
                contentColor = DkTheme.colors.primary,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(DkTheme.spacing.s2)
                        .size(DkTheme.spacing.s6),
                )
            }
            Column(modifier = Modifier.padding(start = DkTheme.spacing.s3)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DkTheme.colors.text,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.textMuted,
                    modifier = Modifier.padding(top = DkTheme.spacing.s1),
                )
            }
        }
    }
}

@Composable
private fun DataSourceStep() {
    Text(
        text = stringResource(R.string.onboarding_data_title),
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    DkCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = DkTheme.colors.primary,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = stringResource(R.string.onboarding_data_source_title),
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
        Text(
            text = stringResource(R.string.onboarding_data_source_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
    }
    DkCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Gavel,
                contentDescription = null,
                tint = DkTheme.colors.warning,
                modifier = Modifier.size(DkTheme.spacing.s6),
            )
            Text(
                text = stringResource(R.string.onboarding_limits_title),
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s3),
            )
        }
        Text(
            text = stringResource(R.string.onboarding_limits_body),
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
        )
        Text(
            text = stringResource(R.string.onboarding_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(top = DkTheme.spacing.s3),
        )
    }
}

@Composable
private fun TermsStep(
    title: String,
    body: String,
    loading: Boolean,
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Text(
        text = title.ifBlank { stringResource(R.string.onboarding_terms_title) },
        style = MaterialTheme.typography.displaySmall,
        color = DkTheme.colors.text,
    )
    Text(
        text = stringResource(R.string.onboarding_terms_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
    )
    DkCard(modifier = Modifier.fillMaxWidth()) {
        // The document itself is scrollable inside a bounded box, so long terms never push the
        // accept control off the screen. The body comes from the server as Markdown; no legal
        // text is authored here.
        val placeholder = when {
            loading -> stringResource(R.string.state_loading)
            body.isBlank() -> stringResource(R.string.onboarding_terms_placeholder)
            else -> null
        }
        val bodyModifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
        if (placeholder != null) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = DkTheme.colors.text,
                modifier = bodyModifier,
            )
        } else {
            MarkdownText(markdown = body, modifier = bodyModifier)
        }
    }
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
    // The full documents are their own destinations, so the wizard keeps its place behind them.
    LegalLinksRow(onOpenTerms = onOpenTerms, onOpenPrivacy = onOpenPrivacy)
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

@Preview(name = "Onboarding, welcome Hebrew", showBackground = true, locale = "he")
@Composable
private fun OnboardingWelcomeHePreview() {
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

@Preview(name = "Onboarding, how it works", showBackground = true, locale = "he")
@Composable
private fun OnboardingHowPreview() {
    DerechKohanimTheme {
        OnboardingScreen(
            state = OnboardingState(step = OnboardingStep.HowItWorks),
            onStringencySelected = {},
            onTermsAcceptedChange = {},
            onNext = {},
            onBack = {},
        )
    }
}

@Preview(name = "Onboarding, data source English", showBackground = true, locale = "en")
@Composable
private fun OnboardingDataPreview() {
    DerechKohanimTheme {
        OnboardingScreen(
            state = OnboardingState(step = OnboardingStep.DataSource),
            onStringencySelected = {},
            onTermsAcceptedChange = {},
            onNext = {},
            onBack = {},
        )
    }
}
