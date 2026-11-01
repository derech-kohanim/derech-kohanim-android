package com.abaye.derechkohanim.ui.screens.settings

import com.abaye.derechkohanim.ui.components.AppLoader
import com.abaye.derechkohanim.ui.components.AppLoaderSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.DkResult
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.model.SavedPlace
import com.abaye.derechkohanim.data.model.toSuggestion
import com.abaye.derechkohanim.data.repository.PlacesRepository
import com.abaye.derechkohanim.nav.TripStore
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkConfirmDialog
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkListBox
import com.abaye.derechkohanim.ui.components.DkListDivider
import com.abaye.derechkohanim.ui.components.DkTopBar
import com.abaye.derechkohanim.ui.icons.categoryIcon
import com.abaye.derechkohanim.ui.theme.DkTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedPlacesState(
    val signedIn: Boolean = false,
    val loading: Boolean = true,
    val places: List<SavedPlace> = emptyList(),
    val failed: Boolean = false,
)

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val placesRepository: PlacesRepository,
    private val tripStore: TripStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedPlacesState())
    val state: StateFlow<SavedPlacesState> = _state.asStateFlow()

    /** Loads the list; a guest has no saved places, so nothing is requested. */
    fun load() {
        viewModelScope.launch {
            val signedIn = sessionStore.snapshot().isSignedIn
            if (!signedIn) {
                _state.value = SavedPlacesState(signedIn = false, loading = false)
                return@launch
            }
            _state.value = _state.value.copy(signedIn = true, loading = true, failed = false)
            when (val result = placesRepository.savedPlaces()) {
                is DkResult.Success -> _state.value = SavedPlacesState(true, false, result.value)
                is DkResult.Failure -> _state.value = SavedPlacesState(true, false, emptyList(), failed = true)
            }
        }
    }

    fun remove(place: SavedPlace) {
        viewModelScope.launch {
            if (placesRepository.removeSavedPlace(place.id) is DkResult.Success) {
                _state.value = _state.value.copy(places = _state.value.places.filterNot { it.id == place.id })
            }
        }
    }

    /** Makes the place the trip's destination, for the route review screen to compute. */
    fun choose(place: SavedPlace) {
        tripStore.setDestination(place.toSuggestion())
    }
}

/**
 * Saved places, reachable from the main settings menu whether or not the user is signed in. A
 * guest sees an illustrated card explaining that places are kept in an account; a signed-in user
 * with no places sees the same card telling where places are saved from.
 */
@Composable
fun SavedPlacesRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onPlaceChosen: () -> Unit,
    viewModel: SavedPlacesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Reloaded every time the screen is shown, so coming back from sign-in shows the list.
    LaunchedEffect(Unit) { viewModel.load() }
    var pendingRemove by remember { mutableStateOf<SavedPlace?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DkTheme.colors.background),
    ) {
        DkTopBar(title = stringResource(R.string.settings_menu_saved_places), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s4),
        ) {
            when {
                state.loading -> AppLoader(
                    size = AppLoaderSize.Large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DkTheme.spacing.s12),
                )
                !state.signedIn -> SavedPlacesEmptyCard(
                    title = stringResource(R.string.saved_places_guest_title),
                    body = stringResource(R.string.saved_places_guest_body),
                    onSignIn = onSignIn,
                    onContinueAsGuest = onBack,
                )
                state.places.isEmpty() -> SavedPlacesEmptyCard(
                    title = stringResource(
                        if (state.failed) R.string.saved_places_failed_title else R.string.saved_places_empty_title,
                    ),
                    body = stringResource(
                        if (state.failed) R.string.saved_places_failed_body else R.string.saved_places_empty_body,
                    ),
                )
                else -> DkListBox {
                    state.places.forEachIndexed { index, place ->
                        if (index > 0) DkListDivider()
                        SavedPlaceRow(
                            place = place,
                            onClick = {
                                viewModel.choose(place)
                                onPlaceChosen()
                            },
                            onRemove = { pendingRemove = place },
                        )
                    }
                }
            }
        }
    }

    pendingRemove?.let { place ->
        DkConfirmDialog(
            title = stringResource(R.string.saved_places_remove_title),
            body = stringResource(R.string.saved_places_remove_body, place.label),
            confirmText = stringResource(R.string.saved_places_remove),
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = {
                viewModel.remove(place)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }
}

@Composable
private fun SavedPlaceRow(place: SavedPlace, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = DkTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = DkTheme.spacing.s4, end = DkTheme.spacing.s1, top = DkTheme.spacing.s2, bottom = DkTheme.spacing.s2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DkTheme.spacing.s8)
                .background(colors.primarySoft, CircleShape),
        ) {
            Icon(categoryIcon(place.category), contentDescription = null, tint = colors.primary, modifier = Modifier.size(DkTheme.spacing.s5))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = place.label, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            place.address?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
            }
        }
        DkIconButton(
            icon = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.saved_places_remove),
            onClick = onRemove,
            tint = colors.textMuted,
        )
    }
}

/**
 * The illustrated empty state: a royal-soft disc with a map pin and a small star badge, drawn
 * from line icons (no emoji), then a title, a sentence and, for a guest, the two actions.
 */
@Composable
fun SavedPlacesEmptyCard(
    title: String,
    body: String,
    onSignIn: (() -> Unit)? = null,
    onContinueAsGuest: (() -> Unit)? = null,
    /** The secondary button's label; "continue as a guest" by default. */
    secondaryLabel: String? = null,
) {
    val colors = DkTheme.colors
    DkListBox {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s6),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(colors.primarySoft, CircleShape),
                )
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(56.dp),
                )
                // The star badge, on a white ring so it reads over the pin and the disc.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 30.dp, y = (-28).dp)
                        .size(36.dp)
                        .background(colors.surface, CircleShape)
                        .padding(3.dp)
                        .background(colors.primary, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = colors.primaryContrast,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textBody,
                textAlign = TextAlign.Center,
            )
            if (onSignIn != null) {
                DkButton(
                    text = stringResource(R.string.settings_sign_in),
                    onClick = onSignIn,
                    large = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DkTheme.spacing.s2),
                )
            }
            if (onContinueAsGuest != null) {
                DkButton(
                    text = secondaryLabel ?: stringResource(R.string.saved_places_continue_guest),
                    onClick = onContinueAsGuest,
                    variant = DkButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
