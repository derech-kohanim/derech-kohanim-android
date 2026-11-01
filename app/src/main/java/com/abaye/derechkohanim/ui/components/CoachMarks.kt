package com.abaye.derechkohanim.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.ui.theme.DkTheme

/**
 * Where each highlightable element sits on screen, by key, in window coordinates. Screens tag
 * their elements with [tourTarget]; the overlay reads the rectangles to cut its window out.
 */
@Stable
class TourTargets {
    internal val rects = mutableStateMapOf<String, Rect>()
    fun has(key: String): Boolean = rects[key]?.let { it.width > 0f && it.height > 0f } == true
}

@Composable
fun rememberTourTargets(): TourTargets = remember { TourTargets() }

/** Records this element's on-screen bounds under [key] for a coach-mark tour. */
fun Modifier.tourTarget(targets: TourTargets, key: String): Modifier =
    onGloballyPositioned { targets.rects[key] = it.boundsInWindow() }

/**
 * One step of a tour: what to highlight (null centres the card with no cut-out), the title and
 * the body. [tryLabelRes] turns the first press of the primary button into a demonstration
 * ([onTry]) before it advances, e.g. "נסה ללחוץ" opening a zone's card.
 */
data class TourStep(
    val target: String?,
    val titleRes: Int,
    val bodyRes: Int,
    val tryLabelRes: Int? = null,
    val onTry: (() -> Unit)? = null,
)

/**
 * The coach-mark overlay: a dark scrim with a rounded window around the current step's target,
 * and a Civic card above or below it with the text, progress dots, "דלג", "הקודם" and
 * "הבא"/"סיום". On the first step a checkbox turns all tours off. Taps outside the card do
 * nothing, so the tour is never closed by accident.
 *
 * [onDone] is called once with how the tour ended: finished (false) or skipped (true), and
 * whether "never show tours again" was ticked.
 */
@Composable
fun CoachMarkOverlay(
    steps: List<TourStep>,
    targets: TourTargets,
    onDone: (skipped: Boolean, never: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Called when the step changes, so a screen can close what a step's demo opened. */
    onStepChange: (Int) -> Unit = {},
) {
    if (steps.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOfCompat(0) }
    var tried by remember { mutableStateOf(false) }
    var never by rememberSaveable { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val isLast = index == steps.lastIndex
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val radius = with(density) { 16.dp.toPx() }
    val hole: Rect? = step.target?.let { targets.rects[it] }?.takeIf { it.width > 0f }
        ?.let { Rect(it.left - origin.x - pad, it.top - origin.y - pad, it.right - origin.x + pad, it.bottom - origin.y + pad) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // Swallow every touch on the scrim: only the card's own controls respond.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                }
            },
    ) {
        val heightPx = with(density) { maxHeight.toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (hole != null) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = hole.topLeft,
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(radius, radius),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        // The card goes on the side of the screen away from the target, right next to the
        // window, and never off screen. Without a target it is centred.
        val cardAbove = hole != null && hole.center.y > heightPx / 2f
        val gap = with(density) { 12.dp.roundToPx() }
        val margin = with(density) { DkTheme.spacing.s4.roundToPx() }
        val insets = with(density) { 48.dp.roundToPx() }
        Layout(
            content = {
                CoachCard(
                    step = step,
                    index = index,
                    count = steps.size,
                    isLast = isLast,
                    showNever = index == 0,
                    never = never,
                    tried = tried,
                    onNeverChange = { never = it },
                    onSkip = { onDone(true, never) },
                    onPrevious = {
                        tried = false
                        index = (index - 1).coerceAtLeast(0)
                        onStepChange(index)
                    },
                    onNext = {
                        val tryAction = step.onTry
                        if (tryAction != null && step.tryLabelRes != null && !tried) {
                            tried = true
                            tryAction()
                        } else if (isLast) {
                            onDone(false, never)
                        } else {
                            tried = false
                            index += 1
                            onStepChange(index)
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val card = measurables.first().measure(
                Constraints(maxWidth = (width - margin * 2).coerceAtLeast(0), maxHeight = height),
            )
            val top = when {
                hole == null -> (height - card.height) / 2
                cardAbove -> hole.top.toInt() - gap - card.height
                else -> hole.bottom.toInt() + gap
            }.coerceIn(insets, (height - card.height - insets).coerceAtLeast(insets))
            layout(width, height) { card.place(margin, top) }
        }
    }
}

/** rememberSaveable-friendly Int state without importing the primitive-state API everywhere. */
private fun mutableIntStateOfCompat(value: Int) = androidx.compose.runtime.mutableIntStateOf(value)

@Composable
private fun CoachCard(
    step: TourStep,
    index: Int,
    count: Int,
    isLast: Boolean,
    showNever: Boolean,
    never: Boolean,
    tried: Boolean,
    onNeverChange: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DkTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DkTheme.radius.lg),
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(DkTheme.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(DkTheme.spacing.s2),
        ) {
            Text(text = stringResource(step.titleRes), style = MaterialTheme.typography.titleMedium, color = colors.text)
            Text(text = stringResource(step.bodyRes), style = MaterialTheme.typography.bodyMedium, color = colors.textBody)
            if (showNever) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = never, role = Role.Checkbox, onValueChange = onNeverChange),
                ) {
                    Checkbox(
                        checked = never,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary, uncheckedColor = colors.borderStrong),
                    )
                    Text(
                        text = stringResource(R.string.tour_never),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(start = DkTheme.spacing.s2),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = DkTheme.spacing.s1)) {
                // Progress dots: the current step is a royal pill, the rest small dots.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(count) { i ->
                        Box(
                            modifier = Modifier
                                .size(width = if (i == index) 16.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (i == index) colors.primary else colors.border),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!isLast) {
                    DkButton(text = stringResource(R.string.tour_skip), onClick = onSkip, variant = DkButtonVariant.Ghost)
                }
                if (index > 0) {
                    DkButton(text = stringResource(R.string.tour_previous), onClick = onPrevious, variant = DkButtonVariant.Secondary)
                    Spacer(Modifier.width(DkTheme.spacing.s2))
                }
                DkButton(
                    text = stringResource(
                        when {
                            step.tryLabelRes != null && step.onTry != null && !tried -> step.tryLabelRes
                            isLast -> R.string.tour_finish
                            else -> R.string.tour_next
                        },
                    ),
                    onClick = onNext,
                )
            }
        }
    }
}
