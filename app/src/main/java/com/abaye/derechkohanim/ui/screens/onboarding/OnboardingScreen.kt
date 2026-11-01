package com.abaye.derechkohanim.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.abaye.derechkohanim.ui.components.DkListBox
import com.abaye.derechkohanim.ui.components.DkListDivider
import com.abaye.derechkohanim.ui.components.StringencyOptions
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
    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            StepDots(index = state.stepIndex, count = state.stepCount)

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
                        body = state.termsBody,
                        loading = state.termsLoading,
                        accepted = state.termsAccepted,
                        onAcceptedChange = onTermsAcceptedChange,
                        onOpenTerms = onOpenTerms,
                        onOpenPrivacy = onOpenPrivacy,
                    )
                    OnboardingStep.Stringency -> StringencyStep(state.stringency, onStringencySelected)
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
                        when {
                            state.isFirst -> R.string.onboarding_start
                            state.isLast -> R.string.onboarding_finish
                            else -> R.string.action_continue
                        },
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

/** Civic progress: one dot per screen, the ones reached filled in royal. */
@Composable
private fun StepDots(index: Int, count: Int) {
    val description = stringResource(R.string.onboarding_progress, index + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(DkTheme.radius.full))
                    .background(if (i <= index) DkTheme.colors.primary else DkTheme.colors.border),
            )
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = DkTheme.colors.text,
    )
}

@Composable
private fun Lead(text: String, muted: Boolean = false) {
    Text(
        text = text,
        style = if (muted) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        color = if (muted) DkTheme.colors.textMuted else DkTheme.colors.textBody,
    )
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
    ) {
        Spacer(Modifier.size(DkTheme.spacing.s12))
        Image(
            painter = painterResource(R.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp)),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = DkTheme.colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.textBody,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HowItWorksStep() {
    StepTitle(stringResource(R.string.onboarding_how_title))
    DkListBox {
        HowRow(Icons.Filled.Search, R.string.onboarding_how_step1_title, R.string.onboarding_how_step1_body)
        DkListDivider()
        HowRow(Icons.AutoMirrored.Filled.AltRoute, R.string.onboarding_how_step2_title, R.string.onboarding_how_step2_body)
        DkListDivider()
        HowRow(Icons.Filled.WarningAmber, R.string.onboarding_how_step3_title, R.string.onboarding_how_step3_body)
        DkListDivider()
        HowRow(Icons.Filled.RecordVoiceOver, R.string.onboarding_how_step4_title, R.string.onboarding_how_step4_body)
    }
}

@Composable
private fun HowRow(icon: ImageVector, titleRes: Int, bodyRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DkTheme.spacing.s8)
                .background(DkTheme.colors.primarySoft, CircleShape),
        ) {
            Icon(icon, contentDescription = null, tint = DkTheme.colors.primary, modifier = Modifier.size(DkTheme.spacing.s5))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleSmall, color = DkTheme.colors.text)
            Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodySmall, color = DkTheme.colors.textMuted)
        }
    }
}

@Composable
private fun DataSourceStep() {
    StepTitle(stringResource(R.string.onboarding_data_title))
    Lead(stringResource(R.string.onboarding_data_source_body))
    Lead(stringResource(R.string.onboarding_limits_body))
    Lead(stringResource(R.string.onboarding_privacy), muted = true)
}

@Composable
private fun StringencyStep(selected: Stringency, onSelect: (Stringency) -> Unit) {
    StepTitle(stringResource(R.string.onboarding_stringency_title))
    Lead(stringResource(R.string.onboarding_stringency_hint))
    StringencyOptions(selected = selected, onSelect = onSelect)
}

@Composable
private fun TermsStep(
    body: String,
    loading: Boolean,
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    StepTitle(stringResource(R.string.onboarding_terms_title))
    Lead(stringResource(R.string.onboarding_terms_intro))
    // The real terms document from the server, in a bounded scrolling box so the accept control
    // never leaves the screen. No legal text is authored here.
    DkListBox {
        val placeholder = when {
            loading -> stringResource(R.string.state_loading)
            body.isBlank() -> stringResource(R.string.onboarding_terms_placeholder)
            else -> null
        }
        val bodyModifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(DkTheme.spacing.s4)
        if (placeholder != null) {
            Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = DkTheme.colors.text, modifier = bodyModifier)
        } else {
            MarkdownText(
                markdown = body,
                modifier = bodyModifier,
                color = DkTheme.colors.textBody,
                numberHeadings = true,
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = accepted, role = Role.Checkbox, onValueChange = onAcceptedChange)
            .padding(vertical = DkTheme.spacing.s2),
    ) {
        Checkbox(
            checked = accepted,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = DkTheme.colors.primary, uncheckedColor = DkTheme.colors.borderStrong),
        )
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
