package com.abaye.derechkohanim.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkSwitchRow
import com.abaye.derechkohanim.ui.screens.onboarding.StringencyOption
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onSignIn = onSignIn,
        onLanguage = viewModel::setLanguage,
        onTheme = viewModel::setTheme,
        onStringency = viewModel::setStringency,
        onTrafficAware = viewModel::setTrafficAware,
        onVoiceEnabled = viewModel::setVoiceEnabled,
        onSignOut = viewModel::signOut,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onLanguage: (Language) -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onStringency: (Stringency) -> Unit,
    onTrafficAware: (Boolean) -> Unit,
    onVoiceEnabled: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = DkTheme.spacing.s2,
                    vertical = DkTheme.spacing.s2,
                ),
            ) {
                DkIconButton(
                    icon = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                    modifier = Modifier.rotate(if (rtl) 90f else -90f),
                )
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DkTheme.colors.text,
                    modifier = Modifier.padding(start = DkTheme.spacing.s2),
                )
            }

            Section(stringResource(R.string.settings_section_general)) {
                ChoiceRow(
                    label = stringResource(R.string.settings_language),
                    options = listOf(
                        Language.HE to stringResource(R.string.settings_language_he),
                        Language.EN to stringResource(R.string.settings_language_en),
                    ),
                    selected = state.language,
                    onSelect = onLanguage,
                )
                ChoiceRow(
                    label = stringResource(R.string.settings_theme),
                    options = listOf(
                        ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
                    ),
                    selected = state.theme,
                    onSelect = onTheme,
                )
            }

            Section(stringResource(R.string.settings_stringency)) {
                Column(modifier = Modifier.selectableGroup()) {
                    Stringency.entries.forEach { option ->
                        StringencyOption(
                            option = option,
                            selected = option == state.stringency,
                            onSelect = { onStringency(option) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.settings_section_navigation)) {
                DkSwitchRow(
                    label = stringResource(R.string.settings_traffic_aware),
                    supportingText = stringResource(R.string.settings_traffic_aware_hint),
                    checked = state.trafficAware,
                    onCheckedChange = onTrafficAware,
                )
                DkSwitchRow(
                    label = stringResource(R.string.settings_voice),
                    supportingText = stringResource(R.string.settings_voice_hint),
                    checked = state.voiceEnabled,
                    onCheckedChange = onVoiceEnabled,
                )
            }

            Section(stringResource(R.string.settings_section_account)) {
                if (state.signedIn) {
                    DkButton(
                        text = stringResource(R.string.settings_sign_out),
                        onClick = onSignOut,
                        variant = DkButtonVariant.Secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DkTheme.spacing.s4),
                    )
                } else {
                    DkButton(
                        text = stringResource(R.string.settings_sign_in),
                        onClick = onSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DkTheme.spacing.s4),
                    )
                }
            }

            Section(stringResource(R.string.settings_section_about)) {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = DkTheme.spacing.s4,
                        vertical = DkTheme.spacing.s2,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = DkTheme.colors.textMuted,
        modifier = Modifier.padding(
            start = DkTheme.spacing.s4,
            end = DkTheme.spacing.s4,
            top = DkTheme.spacing.s5,
            bottom = DkTheme.spacing.s1,
        ),
    )
    content()
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    DkCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s1),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.text,
        )
        Row(
            modifier = Modifier.padding(top = DkTheme.spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            options.forEach { (value, text) ->
                DkButton(
                    text = text,
                    onClick = { onSelect(value) },
                    variant = if (value == selected) {
                        DkButtonVariant.Primary
                    } else {
                        DkButtonVariant.Secondary
                    },
                )
            }
        }
    }
}

@Preview(name = "Settings", showBackground = true, locale = "he")
@Composable
private fun SettingsPreview() {
    DerechKohanimTheme {
        SettingsScreen(
            state = SettingsState(stringency = Stringency.TAHARAT, signedIn = true),
            onBack = {},
            onSignIn = {},
            onLanguage = {},
            onTheme = {},
            onStringency = {},
            onTrafficAware = {},
            onVoiceEnabled = {},
            onSignOut = {},
        )
    }
}
