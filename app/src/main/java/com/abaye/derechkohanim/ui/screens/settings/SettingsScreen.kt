package com.abaye.derechkohanim.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.abaye.derechkohanim.ui.components.DkChipTone
import com.abaye.derechkohanim.ui.components.DkConfirmDialog
import com.abaye.derechkohanim.ui.components.DkListBox
import com.abaye.derechkohanim.ui.components.DkListDivider
import com.abaye.derechkohanim.ui.components.DkListRow
import com.abaye.derechkohanim.ui.components.DkSwitchRow
import com.abaye.derechkohanim.ui.components.DkTopBar
import com.abaye.derechkohanim.ui.components.StringencyDialog
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyDescriptionRes
import com.abaye.derechkohanim.ui.screens.onboarding.stringencyTitleRes
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme
import com.abaye.derechkohanim.voice.VoicePackState
import com.abaye.derechkohanim.voice.VoicePackStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The settings sub-screens, in menu order. The value is the route segment. */
enum class SettingsSection(val route: String) {
    AVOIDANCE("avoidance"),
    VOICE("voice"),
    DISPLAY("display"),
    ZONE_DATA("zones"),
    ACCOUNT("account"),
    ABOUT("about"),
    ;

    companion object {
        fun fromRoute(route: String?): SettingsSection? = entries.firstOrNull { it.route == route }
    }
}

/** The callbacks every settings screen may need, so the sub-screens share one wiring. */
class SettingsActions(
    val onBack: () -> Unit,
    val onOpenSection: (SettingsSection) -> Unit,
    val onSignIn: () -> Unit,
    val onOpenTerms: () -> Unit,
    val onOpenPrivacy: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onLanguage: (Language) -> Unit,
    val onTheme: (ThemePreference) -> Unit,
    val onStringency: (Stringency) -> Unit,
    val onTrafficAware: (Boolean) -> Unit,
    val onVoiceEnabled: (Boolean) -> Unit,
    val onVoiceFemale: (Boolean) -> Unit,
    val onAvoidDangerZones: (Boolean) -> Unit,
    val onAvoidTolls: (Boolean) -> Unit,
    val onMapProvider: (MapProvider) -> Unit,
    val onDownloadVoice: () -> Unit,
    val onDownloadZones: () -> Unit,
    val onSignOut: () -> Unit,
) {
    companion object {
        val Preview = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/**
 * Settings, as a Civic menu of categories each opening its own screen, instead of one long
 * page. [section] null shows the menu.
 */
@Composable
fun SettingsRoute(
    section: SettingsSection?,
    onBack: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = SettingsActions(
        onBack = onBack,
        onOpenSection = onOpenSection,
        onSignIn = onSignIn,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
        onDeleteAccount = onDeleteAccount,
        onLanguage = viewModel::setLanguage,
        onTheme = viewModel::setTheme,
        onStringency = viewModel::setStringency,
        onTrafficAware = viewModel::setTrafficAware,
        onVoiceEnabled = viewModel::setVoiceEnabled,
        onVoiceFemale = viewModel::setVoiceFemale,
        onAvoidDangerZones = viewModel::setAvoidDangerZones,
        onAvoidTolls = viewModel::setAvoidTolls,
        onMapProvider = viewModel::setMapProvider,
        onDownloadVoice = viewModel::downloadVoicePack,
        onDownloadZones = viewModel::downloadZoneLayers,
        onSignOut = viewModel::signOut,
    )
    when (section) {
        null -> SettingsMenuScreen(state, actions)
        SettingsSection.AVOIDANCE -> AvoidanceSettingsScreen(state, actions)
        SettingsSection.VOICE -> VoiceSettingsScreen(state, actions)
        SettingsSection.DISPLAY -> DisplaySettingsScreen(state, actions)
        SettingsSection.ZONE_DATA -> ZoneDataSettingsScreen(state, actions)
        SettingsSection.ACCOUNT -> AccountSettingsScreen(state, actions)
        SettingsSection.ABOUT -> AboutScreen(actions)
    }
}

// -- shell -----------------------------------------------------------------------------------------

@Composable
private fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s6),
            content = content,
        )
    }
}

/** A titled group: the label above, the content (usually a [DkListBox]) under it. */
@Composable
private fun SettingsGroup(title: String?, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = DkTheme.colors.textMuted)
        }
        content()
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = DkTheme.colors.textMuted)
        }
    }
}

// -- menu ------------------------------------------------------------------------------------------

@Composable
fun SettingsMenuScreen(state: SettingsState, actions: SettingsActions) {
    SettingsPage(title = stringResource(R.string.settings_title), onBack = actions.onBack) {
        DkListBox {
            DkListRow(
                title = stringResource(R.string.settings_menu_avoidance),
                supporting = stringResource(stringencyTitleRes(state.stringency)),
                icon = Icons.Outlined.Shield,
                onClick = { actions.onOpenSection(SettingsSection.AVOIDANCE) },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.settings_menu_voice),
                supporting = voiceSummary(state),
                icon = Icons.Outlined.RecordVoiceOver,
                onClick = { actions.onOpenSection(SettingsSection.VOICE) },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.settings_menu_display),
                supporting = displaySummary(state),
                icon = Icons.Outlined.Palette,
                onClick = { actions.onOpenSection(SettingsSection.DISPLAY) },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.settings_menu_zone_data),
                supporting = zoneDataSummary(state.zoneLayer),
                icon = Icons.Outlined.Layers,
                onClick = { actions.onOpenSection(SettingsSection.ZONE_DATA) },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.settings_menu_account),
                supporting = stringResource(
                    if (state.signedIn) R.string.settings_account_signed_in else R.string.settings_account_guest,
                ),
                icon = Icons.Outlined.Person,
                onClick = { actions.onOpenSection(SettingsSection.ACCOUNT) },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.settings_menu_about),
                supporting = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                icon = Icons.Outlined.Info,
                onClick = { actions.onOpenSection(SettingsSection.ABOUT) },
            )
        }
    }
}

@Composable
private fun voiceSummary(state: SettingsState): String {
    if (!state.voiceEnabled) return stringResource(R.string.settings_voice_off)
    if (state.language != Language.HE) return stringResource(R.string.settings_voice_on)
    return stringResource(R.string.settings_voice_on) + " · " +
        stringResource(if (state.voiceFemale) R.string.settings_voice_female else R.string.settings_voice_male)
}

@Composable
private fun displaySummary(state: SettingsState): String =
    stringResource(if (state.language == Language.HE) R.string.settings_language_he else R.string.settings_language_en) +
        " · " + stringResource(themeLabelRes(state.theme))

private fun themeLabelRes(theme: ThemePreference): Int = when (theme) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

@Composable
private fun zoneDataSummary(status: ZoneLayerStatus): String {
    val time = status.updatedAtMillis
    return if (status.state == ZoneLayerState.READY && time != null) {
        stringResource(R.string.settings_zones_updated_at, formatDateTime(time))
    } else {
        zoneLayerStatusText(status)
    }
}

private val DATE_TIME = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm")

private fun formatDateTime(millis: Long): String =
    DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

// -- avoidance -------------------------------------------------------------------------------------

@Composable
fun AvoidanceSettingsScreen(state: SettingsState, actions: SettingsActions) {
    var pickLevel by remember { mutableStateOf(false) }
    SettingsPage(title = stringResource(R.string.settings_menu_avoidance), onBack = actions.onBack) {
        SettingsGroup(title = stringResource(R.string.avoidance_level_title)) {
            DkListBox {
                DkListRow(
                    title = stringResource(stringencyTitleRes(state.stringency)),
                    supporting = stringResource(stringencyDescriptionRes(state.stringency)),
                    icon = Icons.Outlined.Shield,
                    onClick = { pickLevel = true },
                )
            }
        }
        SettingsGroup(title = stringResource(R.string.settings_group_route)) {
            DkListBox {
                DkSwitchRow(
                    label = stringResource(R.string.settings_avoid_danger_zones),
                    supportingText = stringResource(R.string.settings_avoid_danger_zones_hint),
                    checked = state.avoidDangerZones,
                    onCheckedChange = actions.onAvoidDangerZones,
                )
                DkListDivider()
                DkSwitchRow(
                    label = stringResource(R.string.settings_avoid_tolls),
                    supportingText = stringResource(R.string.settings_avoid_tolls_hint),
                    checked = state.avoidTolls,
                    onCheckedChange = actions.onAvoidTolls,
                )
                DkListDivider()
                DkSwitchRow(
                    label = stringResource(R.string.settings_traffic_aware),
                    supportingText = stringResource(R.string.settings_traffic_aware_hint),
                    checked = state.trafficAware,
                    onCheckedChange = actions.onTrafficAware,
                )
            }
        }
    }
    if (pickLevel) {
        StringencyDialog(
            current = state.stringency,
            onSave = {
                actions.onStringency(it)
                pickLevel = false
            },
            onDismiss = { pickLevel = false },
        )
    }
}

// -- voice -----------------------------------------------------------------------------------------

@Composable
fun VoiceSettingsScreen(state: SettingsState, actions: SettingsActions) {
    SettingsPage(title = stringResource(R.string.settings_menu_voice), onBack = actions.onBack) {
        DkListBox {
            DkSwitchRow(
                label = stringResource(R.string.settings_voice),
                supportingText = stringResource(R.string.settings_voice_hint),
                checked = state.voiceEnabled,
                onCheckedChange = actions.onVoiceEnabled,
            )
        }
        // Hebrew has a male and a female speaker; English has a single voice.
        if (state.language == Language.HE) {
            SettingsGroup(
                title = stringResource(R.string.settings_voice_gender),
                hint = stringResource(R.string.settings_voice_gender_hint),
            ) {
                RadioListBox(
                    options = listOf(
                        false to stringResource(R.string.settings_voice_male),
                        true to stringResource(R.string.settings_voice_female),
                    ),
                    selected = state.voiceFemale,
                    onSelect = actions.onVoiceFemale,
                )
            }
        }
        SettingsGroup(title = stringResource(R.string.settings_download_voice)) {
            StatusActionBox(
                status = voicePackStatusText(state.voicePack),
                actionEnabled = state.voicePack.state != VoicePackState.DOWNLOADING,
                onAction = actions.onDownloadVoice,
            )
        }
    }
}

// -- display and language --------------------------------------------------------------------------

@Composable
fun DisplaySettingsScreen(state: SettingsState, actions: SettingsActions) {
    SettingsPage(title = stringResource(R.string.settings_menu_display), onBack = actions.onBack) {
        SettingsGroup(title = stringResource(R.string.settings_language)) {
            RadioListBox(
                options = listOf(
                    Language.HE to stringResource(R.string.settings_language_he),
                    Language.EN to stringResource(R.string.settings_language_en),
                ),
                selected = state.language,
                onSelect = actions.onLanguage,
            )
        }
        SettingsGroup(title = stringResource(R.string.settings_theme)) {
            RadioListBox(
                options = listOf(
                    ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
                ),
                selected = state.theme,
                onSelect = actions.onTheme,
            )
        }
        SettingsGroup(
            title = stringResource(R.string.settings_map_provider),
            hint = stringResource(R.string.settings_map_provider_hint),
        ) {
            RadioListBox(
                options = listOf(
                    MapProvider.AUTOMATIC to stringResource(R.string.settings_map_provider_auto),
                    MapProvider.GOOGLE to stringResource(R.string.settings_map_provider_google),
                    MapProvider.OPENFREEMAP to stringResource(R.string.settings_map_provider_openfreemap),
                ),
                selected = state.mapProvider,
                onSelect = actions.onMapProvider,
            )
        }
    }
}

// -- zone data -------------------------------------------------------------------------------------

@Composable
fun ZoneDataSettingsScreen(state: SettingsState, actions: SettingsActions) {
    val status = state.zoneLayer
    SettingsPage(title = stringResource(R.string.settings_menu_zone_data), onBack = actions.onBack) {
        SettingsGroup(
            title = stringResource(R.string.settings_download_zones),
            hint = stringResource(R.string.settings_zones_source),
        ) {
            DkListBox {
                InfoRow(stringResource(R.string.settings_zones_status), zoneLayerStatusText(status))
                status.updatedAtMillis?.let { time ->
                    DkListDivider()
                    InfoRow(stringResource(R.string.settings_zones_last_update), formatDateTime(time))
                }
                if (status.state == ZoneLayerState.READY) {
                    DkListDivider()
                    InfoRow(stringResource(R.string.settings_zones_versions), "${status.zoneVersion} / ${status.dangerVersion}")
                }
            }
        }
        DkButton(
            text = stringResource(R.string.settings_zones_update_now),
            onClick = actions.onDownloadZones,
            enabled = status.state != ZoneLayerState.UPDATING,
            loading = status.state == ZoneLayerState.UPDATING,
            variant = DkButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// -- account ---------------------------------------------------------------------------------------

@Composable
fun AccountSettingsScreen(state: SettingsState, actions: SettingsActions) {
    var confirmSignOut by remember { mutableStateOf(false) }
    SettingsPage(title = stringResource(R.string.settings_menu_account), onBack = actions.onBack) {
        Text(
            text = stringResource(
                if (state.signedIn) R.string.settings_account_signed_in_body else R.string.settings_account_guest_body,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.textBody,
        )
        DkListBox {
            if (state.signedIn) {
                DkListRow(
                    title = stringResource(R.string.settings_sign_out),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = { confirmSignOut = true },
                    showChevron = false,
                )
            } else {
                DkListRow(
                    title = stringResource(R.string.settings_sign_in),
                    icon = Icons.Filled.Login,
                    onClick = actions.onSignIn,
                )
            }
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.legal_delete_account_action),
                icon = Icons.Filled.DeleteOutline,
                chipTone = DkChipTone.Danger,
                titleColor = DkTheme.colors.danger,
                onClick = actions.onDeleteAccount,
            )
        }
    }
    if (confirmSignOut) {
        DkConfirmDialog(
            title = stringResource(R.string.settings_sign_out_confirm_title),
            body = stringResource(R.string.settings_sign_out_confirm_body),
            confirmText = stringResource(R.string.settings_sign_out),
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                confirmSignOut = false
                actions.onSignOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

// -- about -----------------------------------------------------------------------------------------

/**
 * About and legal: the app's identity (icon, name, version, one line on what it does), the
 * credits and the open-source link, then the documents.
 */
@Composable
fun AboutScreen(actions: SettingsActions) {
    val context = LocalContext.current
    SettingsPage(title = stringResource(R.string.settings_menu_about), onBack = actions.onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                // The launcher foreground is white; it sits on the royal fill like the launcher icon.
                shape = RoundedCornerShape(DkTheme.radius.xl),
                color = DkTheme.colors.primary,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(DkTheme.radius.xl)),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = DkTheme.colors.primary,
            )
            Text(
                // Isolated left to right, or a version like "1.2.0-beta" reads backwards in Hebrew.
                text = stringResource(R.string.settings_version, "⁦${BuildConfig.VERSION_NAME}⁩"),
                style = MaterialTheme.typography.bodySmall,
                color = DkTheme.colors.textMuted,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = DkTheme.colors.textBody,
                textAlign = TextAlign.Center,
            )
        }

        // The developer, as in the other abaye apps: logo, name and the site.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(R.drawable.developer_abaye),
                contentDescription = "abaye",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
            )
            Text(
                text = stringResource(R.string.about_developed_by),
                style = MaterialTheme.typography.titleMedium,
                color = DkTheme.colors.text,
            )
            DkButton(
                text = "abaye.co",
                variant = DkButtonVariant.Secondary,
                leadingIcon = Icons.Filled.Language,
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_URL)))
                    }
                },
            )
        }

        SettingsGroup(title = stringResource(R.string.about_credits_title)) {
            Text(
                text = stringResource(R.string.about_credits),
                style = MaterialTheme.typography.bodyMedium,
                color = DkTheme.colors.textBody,
            )
        }

        DkListBox {
            DkListRow(
                title = stringResource(R.string.about_open_source),
                supporting = OPEN_SOURCE_URL.removePrefix("https://"),
                icon = Icons.Filled.Code,
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OPEN_SOURCE_URL)))
                    }
                },
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.legal_terms_title),
                icon = Icons.Outlined.Description,
                onClick = actions.onOpenTerms,
            )
            DkListDivider()
            DkListRow(
                title = stringResource(R.string.legal_privacy_title),
                icon = Icons.Outlined.PrivacyTip,
                onClick = actions.onOpenPrivacy,
            )
        }
    }
}

private const val OPEN_SOURCE_URL = "https://github.com/derech-kohanim"
private const val DEVELOPER_URL = "https://abaye.co"

// -- shared rows -----------------------------------------------------------------------------------

/** A single choice as Civic radio rows in one bordered box. */
@Composable
private fun <T> RadioListBox(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = DkTheme.colors
    DkListBox {
        options.forEachIndexed { index, (value, label) ->
            if (index > 0) DkListDivider()
            val isSelected = value == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .background(if (isSelected) colors.band else colors.surface)
                    .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s2),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.borderStrong),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text,
                    modifier = Modifier.padding(start = DkTheme.spacing.s3),
                )
            }
        }
    }
}

/** A label and its value on one row. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s3),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = DkTheme.colors.text,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = DkTheme.colors.textMuted)
    }
}

/** A status line with a download/update action beside it. */
@Composable
private fun StatusActionBox(status: String, actionEnabled: Boolean, onAction: () -> Unit) {
    DkListBox {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s3),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = DkTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            DkButton(
                text = stringResource(R.string.settings_download_action),
                onClick = onAction,
                enabled = actionEnabled,
                variant = DkButtonVariant.Secondary,
            )
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

// -- previews --------------------------------------------------------------------------------------

@Preview(name = "Settings menu", showBackground = true, locale = "he")
@Composable
private fun SettingsMenuPreview() {
    DerechKohanimTheme {
        SettingsMenuScreen(SettingsState(stringency = Stringency.TAHARAT, signedIn = true, voiceFemale = true), SettingsActions.Preview)
    }
}

@Preview(name = "Avoidance settings", showBackground = true, locale = "he")
@Composable
private fun AvoidanceSettingsPreview() {
    DerechKohanimTheme {
        AvoidanceSettingsScreen(SettingsState(stringency = Stringency.TAHARAT), SettingsActions.Preview)
    }
}

@Preview(name = "About", showBackground = true, locale = "he")
@Composable
private fun AboutPreview() {
    DerechKohanimTheme {
        AboutScreen(SettingsActions.Preview)
    }
}
