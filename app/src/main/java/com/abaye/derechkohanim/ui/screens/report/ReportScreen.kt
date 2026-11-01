package com.abaye.derechkohanim.ui.screens.report

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.abaye.derechkohanim.BuildConfig
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.SUPPORT_MESSAGE_MAX
import com.abaye.derechkohanim.data.model.SupportKind
import com.abaye.derechkohanim.data.model.SupportReportRequest
import com.abaye.derechkohanim.data.repository.SupportRepository
import com.abaye.derechkohanim.nav.Routes
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkListBox
import com.abaye.derechkohanim.ui.components.DkListDivider
import com.abaye.derechkohanim.ui.components.DkTopBar
import com.abaye.derechkohanim.ui.components.TaharatContact
import com.abaye.derechkohanim.ui.screens.settings.SavedPlacesEmptyCard
import com.abaye.derechkohanim.ui.theme.DkTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    val kind: SupportKind? = null,
    val message: String = "",
    val replyByEmail: Boolean = false,
    val routeId: String? = null,
    val zoneId: Int? = null,
    val zoneName: String? = null,
    val location: LatLng? = null,
    /** True when the location is a mark the driver dropped, so the chip says so. */
    val markedLocation: Boolean = false,
    val sending: Boolean = false,
    /** Set once sent: the issue number, or null when the report was kept without one. */
    val sentReference: Int? = null,
    val sent: Boolean = false,
    /** A string resource for the last send failure. */
    val errorRes: Int? = null,
) {
    val canSend: Boolean get() = kind != null && message.isNotBlank() && !sending
}

/**
 * The report form's state. The context (route, zone, location) arrives as navigation arguments
 * from where the form was opened; the app version and device are added on send.
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sessionStore: SessionStore,
    private val supportRepository: SupportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ReportState(
            kind = savedState.get<String>(Routes.REPORT_ARG_KIND)?.let { k -> SupportKind.entries.firstOrNull { it.name == k } },
            routeId = savedState.get<String>(Routes.REPORT_ARG_ROUTE)?.takeIf { it.isNotBlank() },
            zoneId = savedState.get<String>(Routes.REPORT_ARG_ZONE)?.toIntOrNull(),
            zoneName = savedState.get<String>(Routes.REPORT_ARG_ZONE_NAME)?.takeIf { it.isNotBlank() },
            location = run {
                val lat = savedState.get<String>(Routes.REPORT_ARG_LAT)?.toDoubleOrNull()
                val lng = savedState.get<String>(Routes.REPORT_ARG_LNG)?.toDoubleOrNull()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            },
            markedLocation = savedState.get<String>(Routes.REPORT_ARG_MARKED) == "1",
        ),
    )
    val state: StateFlow<ReportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = sessionStore.snapshot()
            _state.value = _state.value.copy(
                loading = false,
                signedIn = snapshot.isSignedIn,
                email = snapshot.profileEmail,
            )
        }
    }

    fun setKind(kind: SupportKind) {
        _state.value = _state.value.copy(kind = kind, errorRes = null)
    }

    fun setMessage(message: String) {
        _state.value = _state.value.copy(message = message.take(SUPPORT_MESSAGE_MAX), errorRes = null)
    }

    fun setReplyByEmail(reply: Boolean) {
        _state.value = _state.value.copy(replyByEmail = reply)
    }

    fun send() {
        val current = _state.value
        val kind = current.kind ?: return
        if (!current.canSend) return
        _state.value = current.copy(sending = true, errorRes = null)
        viewModelScope.launch {
            val request = SupportReportRequest(
                kind = kind,
                message = current.message.trim(),
                appVersion = BuildConfig.VERSION_NAME,
                device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
                routeId = current.routeId,
                zoneId = current.zoneId,
                location = current.location,
                replyByEmail = if (current.email != null) current.replyByEmail else null,
            )
            _state.value = when (val result = supportRepository.sendReport(request)) {
                is DkResult.Success -> _state.value.copy(
                    sending = false,
                    sent = true,
                    sentReference = result.value.reference,
                )
                is DkResult.Failure -> _state.value.copy(
                    sending = false,
                    errorRes = if (result.code == RATE_LIMITED) R.string.report_error_rate_limited else R.string.report_error_generic,
                )
            }
        }
    }

    private companion object {
        const val RATE_LIMITED = "support_rate_limited"
    }
}

@Composable
fun ReportRoute(
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DkTheme.colors.background),
    ) {
        DkTopBar(title = stringResource(R.string.report_title), onBack = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s5),
        ) {
            when {
                state.loading -> Unit
                !state.signedIn -> SavedPlacesEmptyCard(
                    title = stringResource(R.string.report_guest_title),
                    body = stringResource(R.string.report_guest_body),
                    onSignIn = onSignIn,
                    onContinueAsGuest = onClose,
                    secondaryLabel = stringResource(R.string.action_cancel),
                )
                state.sent -> SentCard(reference = state.sentReference, onClose = onClose)
                else -> ReportForm(
                    state = state,
                    onKind = viewModel::setKind,
                    onMessage = viewModel::setMessage,
                    onReplyByEmail = viewModel::setReplyByEmail,
                    onSend = viewModel::send,
                    onCancel = onClose,
                )
            }
        }
    }
}

private val KINDS = listOf(
    Triple(SupportKind.BUG, R.string.report_kind_bug, R.string.report_kind_bug_hint),
    Triple(SupportKind.ROUTE, R.string.report_kind_route, R.string.report_kind_route_hint),
    Triple(SupportKind.DATA, R.string.report_kind_data, R.string.report_kind_data_hint),
    Triple(SupportKind.SUGGESTION, R.string.report_kind_suggestion, R.string.report_kind_suggestion_hint),
)

private fun placeholderRes(kind: SupportKind?): Int = when (kind) {
    SupportKind.ROUTE -> R.string.report_placeholder_route
    SupportKind.DATA -> R.string.report_placeholder_data
    SupportKind.SUGGESTION -> R.string.report_placeholder_suggestion
    else -> R.string.report_placeholder_bug
}

@Composable
private fun ReportForm(
    state: ReportState,
    onKind: (SupportKind) -> Unit,
    onMessage: (String) -> Unit,
    onReplyByEmail: (Boolean) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = DkTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
        Text(stringResource(R.string.report_kind_title), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        DkListBox {
            KINDS.forEachIndexed { index, (kind, title, hint) ->
                if (index > 0) DkListDivider()
                val selected = state.kind == kind
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onKind(kind) })
                        .background(if (selected) colors.band else colors.surface)
                        .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s3),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.borderStrong),
                    )
                    Column(modifier = Modifier.padding(start = DkTheme.spacing.s3)) {
                        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, color = colors.text)
                        Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
            }
        }
    }

    if (state.kind == SupportKind.DATA) {
        Surface(
            shape = RoundedCornerShape(DkTheme.radius.md),
            color = colors.primarySoft,
            border = BorderStroke(Dp.Hairline, colors.primary.copy(alpha = 0.25f)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3), modifier = Modifier.padding(DkTheme.spacing.s3)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.report_data_note, TaharatContact.ltr(TaharatContact.PHONE_DISPLAY)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textBody,
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
        Text(stringResource(R.string.report_message_title), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessage,
            placeholder = { Text(stringResource(placeholderRes(state.kind)), color = colors.textMuted) },
            minLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            shape = RoundedCornerShape(DkTheme.radius.sm),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.borderStrong,
            ),
        )
        Text(
            text = "${state.message.length}/$SUPPORT_MESSAGE_MAX",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.align(Alignment.End),
        )
    }

    val chips = buildList {
        if (state.routeId != null) add(stringResource(R.string.report_attached_route))
        if (state.zoneName != null) add(stringResource(R.string.report_attached_zone, state.zoneName))
        if (state.location != null && state.markedLocation) add(stringResource(R.string.report_attached_mark))
    }
    if (chips.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s1)) {
            chips.forEach { chip ->
                Text(
                    text = chip,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                    modifier = Modifier
                        .background(colors.surfaceMuted, RoundedCornerShape(DkTheme.radius.full))
                        .padding(horizontal = DkTheme.spacing.s3, vertical = DkTheme.spacing.s1),
                )
            }
        }
    }

    if (state.email != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.replyByEmail, role = Role.Checkbox, onValueChange = onReplyByEmail),
        ) {
            Checkbox(
                checked = state.replyByEmail,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary, uncheckedColor = colors.borderStrong),
            )
            Text(
                text = stringResource(R.string.report_reply_by_email, TaharatContact.ltr(state.email)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                modifier = Modifier.padding(start = DkTheme.spacing.s2),
            )
        }
    }

    Text(stringResource(R.string.report_sent_with), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)

    state.errorRes?.let { error ->
        Text(stringResource(error), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2)) {
        DkButton(
            text = stringResource(R.string.report_send),
            onClick = onSend,
            enabled = state.canSend,
            loading = state.sending,
            large = true,
            modifier = Modifier.weight(1f),
        )
        DkButton(text = stringResource(R.string.action_cancel), onClick = onCancel, variant = DkButtonVariant.Secondary, large = true)
    }
}

@Composable
private fun SentCard(reference: Int?, onClose: () -> Unit) {
    val colors = DkTheme.colors
    DkListBox {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s6),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(colors.successSoft, CircleShape),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(36.dp))
            }
            Text(stringResource(R.string.report_sent_title), style = MaterialTheme.typography.titleMedium, color = colors.text, textAlign = TextAlign.Center)
            Text(
                text = if (reference != null) {
                    stringResource(R.string.report_sent_reference, TaharatContact.ltr("#$reference"))
                } else {
                    stringResource(R.string.report_sent_no_reference)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textBody,
                textAlign = TextAlign.Center,
            )
            DkButton(text = stringResource(R.string.action_close), onClick = onClose, variant = DkButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The flag icon used by every report entry point. */
val ReportIcon = Icons.Outlined.Flag
