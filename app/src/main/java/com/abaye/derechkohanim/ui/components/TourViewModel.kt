package com.abaye.derechkohanim.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaye.derechkohanim.data.local.SessionStore
import com.abaye.derechkohanim.data.local.Tour
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The per-screen tour switch. A screen asks [pending] whether its tour should run (not seen yet
 * and tours not turned off), and reports how it ended through [finish]. Null means the stored
 * flags have not been read yet, so nothing starts on a stale default.
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val flows = Tour.entries.associateWith { tour ->
        sessionStore.snapshots
            .map { !it.toursNever && tour !in it.toursSeen }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    fun pending(tour: Tour): StateFlow<Boolean?> = flows.getValue(tour)

    /**
     * Closes [tour]. It is marked seen only if it showed at least one step other than
     * [onlyStepKey]; a run that showed nothing but that step marks [onlyStepTour] instead, so the
     * step is not repeated and the regular tour stays pending. "Never" turns every tour off.
     */
    fun finish(
        tour: Tour,
        result: TourResult,
        onlyStepKey: String? = null,
        onlyStepTour: Tour? = null,
    ) {
        viewModelScope.launch {
            val onlyThatStep = onlyStepKey != null && result.shownTargets == setOf(onlyStepKey)
            if (onlyThatStep && onlyStepTour != null) {
                sessionStore.markTourSeen(onlyStepTour)
            } else if (result.shownTargets.isNotEmpty()) {
                sessionStore.markTourSeen(tour)
            }
            if (result.never) sessionStore.setToursNever(true)
        }
    }
}
