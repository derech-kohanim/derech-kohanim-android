package com.abaye.derechkohanim.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.ErrorCode
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.mock.MockData
import com.abaye.derechkohanim.data.model.LatLng
import com.abaye.derechkohanim.data.model.PlaceCategory
import com.abaye.derechkohanim.data.model.PlaceSuggestion
import com.abaye.derechkohanim.data.model.toSuggestion
import com.abaye.derechkohanim.ui.components.DkBadge
import com.abaye.derechkohanim.ui.components.DkBadgeTone
import com.abaye.derechkohanim.ui.components.DkButton
import com.abaye.derechkohanim.ui.components.DkButtonVariant
import com.abaye.derechkohanim.ui.components.DkIconButton
import com.abaye.derechkohanim.ui.components.DkListItem
import com.abaye.derechkohanim.ui.components.DkListItemSkeleton
import com.abaye.derechkohanim.ui.components.DkSearchField
import com.abaye.derechkohanim.ui.icons.categoryIcon
import com.abaye.derechkohanim.ui.icons.categoryLabel
import com.abaye.derechkohanim.ui.theme.DerechKohanimTheme
import com.abaye.derechkohanim.ui.theme.DkTheme

@Composable
fun DestinationSearchRoute(
    onBack: () -> Unit,
    onDestinationChosen: (PlaceSuggestion) -> Unit,
    origin: LatLng? = null,
    onRequireLogin: () -> Unit = {},
    viewModel: DestinationSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var savedMessage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(origin) { viewModel.setOrigin(origin) }

    // A guest tried an account-only action: tell them why, then send them to sign in. The pending
    // action is replayed by the view model once they return signed in.
    LaunchedEffect(Unit) {
        viewModel.requireLogin.collect {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.sign_in_required),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            onRequireLogin()
        }
    }

    LaunchedEffect(selection) {
        selection?.let {
            viewModel.clearSelection()
            onDestinationChosen(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedConfirmation.collect { category ->
            savedMessage = when (category) {
                PlaceCategory.HOME -> R.string.saved_home_confirmation
                PlaceCategory.WORK -> R.string.saved_work_confirmation
                else -> R.string.saved_place_confirmation
            }
            kotlinx.coroutines.delay(2200)
            savedMessage = null
        }
    }

    DestinationSearchScreen(
        state = state,
        savedMessageRes = savedMessage,
        onQueryChange = viewModel::onQueryChange,
        onSelect = viewModel::onSelect,
        onStartAssign = viewModel::startAssign,
        onCancelAssign = viewModel::cancelAssign,
        onRetry = viewModel::retry,
        onBack = onBack,
    )
}

@Composable
fun DestinationSearchScreen(
    state: DestinationSearchState,
    onQueryChange: (String) -> Unit,
    onSelect: (PlaceSuggestion) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    savedMessageRes: Int? = null,
    onStartAssign: (PlaceCategory) -> Unit = {},
    onCancelAssign: () -> Unit = {},
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) focusRequester.requestFocus() }

    Scaffold(containerColor = DkTheme.colors.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = DkTheme.spacing.s2,
                    end = DkTheme.spacing.s4,
                    top = DkTheme.spacing.s2,
                    bottom = DkTheme.spacing.s2,
                ),
            ) {
                DkIconButton(
                    icon = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = { if (state.assignCategory != null) onCancelAssign() else onBack() },
                    tint = DkTheme.colors.textMuted,
                    modifier = Modifier.rotateForBack(),
                )
                DkSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = DkTheme.spacing.s2),
                )
            }

            AnimatedVisibility(visible = state.assignCategory != null) {
                AssignBanner(category = state.assignCategory, onCancel = onCancelAssign)
            }

            AnimatedVisibility(visible = savedMessageRes != null) {
                savedMessageRes?.let {
                    Row(modifier = Modifier.padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s2)) {
                        DkBadge(
                            text = stringResource(it),
                            tone = DkBadgeTone.Success,
                            icon = Icons.Filled.CheckCircle,
                        )
                    }
                }
            }

            SuggestionList(
                state = state,
                onSelect = onSelect,
                onStartAssign = onStartAssign,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AssignBanner(category: PlaceCategory?, onCancel: () -> Unit) {
    val text = when (category) {
        PlaceCategory.WORK -> stringResource(R.string.assign_work_banner)
        else -> stringResource(R.string.assign_home_banner)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DkTheme.spacing.s4, vertical = DkTheme.spacing.s2),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DkTheme.colors.primary,
            modifier = Modifier.weight(1f),
        )
        DkButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            variant = DkButtonVariant.Ghost,
        )
    }
}

@Composable
private fun SuggestionList(
    state: DestinationSearchState,
    onSelect: (PlaceSuggestion) -> Unit,
    onStartAssign: (PlaceCategory) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        // Home and work shortcuts sit at the very top when the field is empty (spec 17.5).
        if (state.showShortcuts) {
            item("shortcuts-header") { SectionHeader(stringResource(R.string.search_section_shortcuts)) }
            item("shortcut-home") {
                QuickSlot(
                    category = PlaceCategory.HOME,
                    place = state.home,
                    onNavigate = onSelect,
                    onAssign = { onStartAssign(PlaceCategory.HOME) },
                )
            }
            item("shortcut-work") {
                QuickSlot(
                    category = PlaceCategory.WORK,
                    place = state.work,
                    onNavigate = onSelect,
                    onAssign = { onStartAssign(PlaceCategory.WORK) },
                )
            }
        }

        state.coordinate?.let { coordinate ->
            item("coordinates-header") {
                SectionHeader(stringResource(R.string.search_section_coordinates))
            }
            item("coordinates-row") {
                DkListItem(
                    title = coordinate.title,
                    subtitle = stringResource(R.string.search_navigate_to_point),
                    leadingIcon = categoryIcon(coordinate.category),
                    onClick = { onSelect(coordinate) },
                )
            }
        }

        if (state.saved.isNotEmpty()) {
            item("saved-header") { SectionHeader(stringResource(R.string.search_section_saved)) }
            items(state.saved, key = { "saved-" + it.id }) { suggestion ->
                SuggestionRow(suggestion, onSelect)
            }
        }

        if (state.recent.isNotEmpty()) {
            item("recent-header") { SectionHeader(stringResource(R.string.search_section_recent)) }
            items(state.recent, key = { "recent-" + it.id }) { suggestion ->
                SuggestionRow(suggestion, onSelect)
            }
        }

        if (state.results.isNotEmpty()) {
            item("results-header") { SectionHeader(stringResource(R.string.search_section_results)) }
            items(state.results, key = { "result-" + it.id }) { suggestion ->
                SuggestionRow(suggestion, onSelect)
            }
        }

        if (state.isSearching && state.results.isEmpty()) {
            items(SKELETON_ROWS) { DkListItemSkeleton() }
        }

        if (state.errorCode != null) {
            item("error") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DkTheme.spacing.s6),
                ) {
                    Text(
                        text = stringResource(
                            if (state.errorCode == ErrorCode.NETWORK) {
                                R.string.error_network
                            } else {
                                R.string.search_error
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DkTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    DkButton(
                        text = stringResource(R.string.action_retry),
                        onClick = onRetry,
                        variant = DkButtonVariant.Secondary,
                    )
                }
            }
        }

        if (state.isEmptyResult) {
            item("empty") {
                EmptyMessage(stringResource(R.string.search_empty, state.query))
            }
        }

        if (state.query.isBlank() && !state.hasLocalResults) {
            item("prompt") { EmptyMessage(stringResource(R.string.search_prompt)) }
        }
    }
}

/**
 * A home or work shortcut. When set, tapping navigates and a pencil re-assigns it; when
 * empty, the whole row starts the assign flow. The category, not the stored label, names it.
 */
@Composable
private fun QuickSlot(
    category: PlaceCategory,
    place: PlaceSuggestion?,
    onNavigate: (PlaceSuggestion) -> Unit,
    onAssign: () -> Unit,
) {
    val context = LocalContext.current
    if (place == null) {
        DkListItem(
            title = stringResource(
                if (category == PlaceCategory.WORK) R.string.quick_slot_work_empty
                else R.string.quick_slot_home_empty
            ),
            subtitle = stringResource(
                if (category == PlaceCategory.WORK) R.string.quick_slot_work_hint
                else R.string.quick_slot_home_hint
            ),
            leadingIcon = categoryIcon(category),
            trailingIcon = Icons.Filled.Add,
            trailingIconDescription = null,
            onClick = onAssign,
        )
    } else {
        DkListItem(
            title = categoryLabel(category),
            subtitle = place.subtitle ?: place.title,
            leadingIcon = categoryIcon(category),
            trailingIcon = Icons.Filled.Edit,
            trailingIconDescription = stringResource(R.string.cd_edit_slot),
            onTrailingClick = onAssign,
            onClick = { onNavigate(place) },
        )
    }
}

@Composable
private fun SuggestionRow(suggestion: PlaceSuggestion, onSelect: (PlaceSuggestion) -> Unit) {
    val context = LocalContext.current
    DkListItem(
        title = suggestion.title,
        subtitle = suggestion.subtitle,
        matchedRanges = suggestion.matchedRanges,
        leadingIcon = categoryIcon(suggestion.category),
        trailingText = suggestion.distanceMeters?.let { Formatters.distance(context, it) },
        onClick = { onSelect(suggestion) },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        HorizontalDivider(color = DkTheme.colors.border)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = DkTheme.colors.textMuted,
            modifier = Modifier.padding(
                start = DkTheme.spacing.s4,
                end = DkTheme.spacing.s4,
                top = DkTheme.spacing.s4,
                bottom = DkTheme.spacing.s1,
            ),
        )
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = DkTheme.colors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(DkTheme.spacing.s6),
    )
}

/** The back arrow is chrome, so it does mirror with the layout direction. */
@Composable
private fun Modifier.rotateForBack(): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return this.rotate(if (rtl) 90f else -90f)
}

private const val SKELETON_ROWS = 4

@Preview(name = "Search, results", showBackground = true, locale = "he")
@Composable
private fun DestinationSearchPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(
                query = "בני",
                saved = MockData.savedPlaces.map { it.toSuggestion() },
                results = MockData.search("בני", MockData.currentLocation),
            ),
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}

@Preview(name = "Search, loading", showBackground = true, locale = "he")
@Composable
private fun DestinationSearchLoadingPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(query = "רחוב", isSearching = true),
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}

@Preview(name = "Search, empty English", showBackground = true, locale = "en")
@Composable
private fun DestinationSearchEmptyPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(query = "zzzz"),
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}

@Preview(name = "Search, shortcuts empty", showBackground = true, locale = "he")
@Composable
private fun DestinationSearchShortcutsEmptyPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(
                recent = MockData.recentPlaces.map { it.toSuggestion() },
            ),
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}

@Preview(name = "Search, shortcuts set", showBackground = true, locale = "he")
@Composable
private fun DestinationSearchShortcutsSetPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(
                home = MockData.savedPlaces.first { it.category == PlaceCategory.HOME }.toSuggestion(),
                work = MockData.savedPlaces.first { it.category == PlaceCategory.WORK }.toSuggestion(),
                recent = MockData.recentPlaces.map { it.toSuggestion() },
            ),
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}

@Preview(name = "Search, assign mode", showBackground = true, locale = "he")
@Composable
private fun DestinationSearchAssignPreview() {
    DerechKohanimTheme {
        DestinationSearchScreen(
            state = DestinationSearchState(
                query = "רחוב",
                assignCategory = PlaceCategory.HOME,
                results = MockData.search("רחוב", MockData.currentLocation),
            ),
            savedMessageRes = null,
            onQueryChange = {},
            onSelect = {},
            onRetry = {},
            onBack = {},
            autoFocus = false,
        )
    }
}
