package com.abaye.derechkohanim.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.data.model.Language
import com.abaye.derechkohanim.data.model.MapProvider
import com.abaye.derechkohanim.data.model.Stringency
import com.abaye.derechkohanim.data.model.ThemePreference
import com.abaye.derechkohanim.data.repository.ZoneLayerState
import com.abaye.derechkohanim.data.repository.ZoneLayerStatus
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkCard
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkSwitchRow
import com.abaye.derechkohanim.ui.screens.onboarding.StringencyOption
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme
import com.abaye.derechkohanim.voice.VoicePackState
import com.abaye.derechkohanim.voice.VoicePackStatus

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onSignIn = onSignIn,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
        onDeleteAccount = onDeleteAccount,
        onLanguage = viewModel::setLanguage,
        onTheme = viewModel::setTheme,
        onStringency = viewModel::setStringency,
        onTrafficAware = viewModel::setTrafficAware,
        onVoiceEnabled = viewModel::setVoiceEnabled,
        onAvoidDangerZones = viewModel::setAvoidDangerZones,
        onAvoidTolls = viewModel::setAvoidTolls,
        onMapProvider = viewModel::setMapProvider,
        onDownloadVoice = viewModel::downloadVoicePack,
        onDownloadZones = viewModel::downloadZoneLayers,
        onSignOut = viewModel::signOut,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onLanguage: (Language) -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onStringency: (Stringency) -> Unit,
    onTrafficAware: (Boolean) -> Unit,
    onVoiceEnabled: (Boolean) -> Unit,
    onAvoidDangerZones: (Boolean) -> Unit,
    onAvoidTolls: (Boolean) -> Unit,
    onMapProvider: (MapProvider) -> Unit = {},
    onDownloadVoice: () -> Unit = {},
    onDownloadZones: () -> Unit = {},
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
                DkSwitchRow(
                    label = stringResource(R.string.settings_avoid_danger_zones),
                    supportingText = stringResource(R.string.settings_avoid_danger_zones_hint),
                    checked = state.avoidDangerZones,
                    onCheckedChange = onAvoidDangerZones,
                )
                DkSwitchRow(
                    label = stringResource(R.string.settings_avoid_tolls),
                    supportingText = stringResource(R.string.settings_avoid_tolls_hint),
                    checked = state.avoidTolls,
                    onCheckedChange = onAvoidTolls,
                )
            }

            Section(stringResource(R.string.settings_section_map)) {
                ChoiceRow(
                    label = stringResource(R.string.settings_map_provider),
                    options = listOf(
                        MapProvider.AUTOMATIC to stringResource(R.string.settings_map_provider_auto),
                        MapProvider.GOOGLE to stringResource(R.string.settings_map_provider_google),
                        MapProvider.OPENFREEMAP to stringResource(R.string.settings_map_provider_openfreemap),
                    ),
                    selected = state.mapProvider,
                    onSelect = onMapProvider,
                )
                Text(
                    text = stringResource(R.string.settings_map_provider_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = DkTheme.colors.textMuted,
                    modifier = Modifier.padding(
                        horizontal = DkTheme.spacing.s4,
                        vertical = DkTheme.spacing.s1,
                    ),
                )
            }

            Section(stringResource(R.string.settings_section_downloads)) {
                DownloadRow(
                    label = stringResource(R.string.settings_download_voice),
                    status = voicePackStatusText(state.voicePack),
                    actionEnabled = state.voicePack.state != VoicePackState.DOWNLOADING,
                    onAction = onDownloadVoice,
                )
                DownloadRow(
                    label = stringResource(R.string.settings_download_zones),
                    status = zoneLayerStatusText(state.zoneLayer),
                    actionEnabled = state.zoneLayer.state != ZoneLayerState.UPDATING,
                    onAction = onDownloadZones,
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

            Section(stringResource(R.string.settings_section_legal)) {
                LinkRow(stringResource(R.string.legal_terms_title), onOpenTerms)
                LinkRow(stringResource(R.string.legal_privacy_title), onOpenPrivacy)
                LinkRow(stringResource(R.string.legal_delete_account_action), onDeleteAccount)
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
private fun voicePackStatusText(status: VoicePackStatus): String = when (status.state) {
    VoicePackState.DOWNLOADING -> stringResource(R.string.settings_download_voice_status_downloading)
    VoicePackState.READY -> stringResource(
        R.string.settings_download_voice_status_ready,
        status.version,
        status.clipCount,
    )
    VoicePackState.ERROR -> stringResource(R.string.settings_download_voice_status_error)
    VoicePackState.UNKNOWN -> stringResource(R.string.settings_download_voice_status_none)
}

@Composable
private fun zoneLayerStatusText(status: ZoneLayerStatus): String = when (status.state) {
    ZoneLayerState.UPDATING -> stringResource(R.string.settings_download_zones_status_downloading)
    ZoneLayerState.READY -> stringResource(
        R.string.settings_download_zones_status_ready,
        status.zoneCount,
        status.dangerCount,
    )
    ZoneLayerState.ERROR -> stringResource(R.string.settings_download_zones_status_error)
    ZoneLayerState.UNKNOWN -> stringResource(R.string.settings_download_zones_status_none)
}

@Composable
private fun DownloadRow(
    label: String,
    status: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    DkCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DkTheme.colors.text,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DkTheme.colors.textMuted,
                    modifier = Modifier.padding(top = DkTheme.spacing.s1),
                )
            }
            if (actionEnabled) {
                DkButton(
                    text = stringResource(R.string.settings_download_action),
                    onClick = onAction,
                    variant = DkButtonVariant.Secondary,
                )
            }
        }
    }
}

/** A settings row that opens another screen. The chevron is the affordance, so no button chrome. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    DkCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s1)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = DkTheme.colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DkTheme.colors.textMuted,
            )
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
            modifier = Modifier
                .padding(top = DkTheme.spacing.s2)
                .horizontalScroll(rememberScrollState()),
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
            onAvoidDangerZones = {},
            onAvoidTolls = {},
            onMapProvider = {},
            onSignOut = {},
        )
    }
}
