@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteConfiguration
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.selectIfLt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Glance's responsive sizing, on Remote Compose.
//
// A Glance widget with `SizeMode.Responsive(sizes)` is composed once per breakpoint, and the host
// shows whichever layout fits the widget best (developer.android.com/develop/ui/compose/glance/
// build-ui). A Remote Compose widget is RECORDED once, as a document, and played back at whatever
// size the launcher gives it — so the choice cannot be made while recording. It is made during
// playback instead: every breakpoint's layout goes into one `RemoteStateLayout`, and the index it
// shows is an expression over the player's window size, the widget's own cell rectangle.
// Resizing on the home screen re-evaluates the expression, and StateLayout animates between the
// layouts.
//
// When the size is known while recording (a preview pinned to one grid size, or a widget whose
// metadata does not let it resize), there is nothing to choose at playback: [AdaptiveWidget]
// records only the branch [bestFitSize] picks, and the document carries no StateLayout at all.
// ---------------------------------------------------------------------------------------------

/**
 * The breakpoint the current branch was laid out for, as Glance's `LocalSize` is under
 * `SizeMode.Responsive`. Unspecified outside an [AdaptiveWidget].
 */
val LocalWidgetSize = staticCompositionLocalOf { DpSize.Unspecified }

/**
 * The size, when the recording already knows it, that [AdaptiveWidget] lays out for instead of
 * deciding at playback. Null — the default — means the widget may be resized.
 */
val LocalFixedWidgetSize = staticCompositionLocalOf<DpSize?> { null }

/**
 * Glance's rule for picking a responsive breakpoint: of the [sizes] that fit inside [available],
 * the one closest to it; when none fits, the smallest.
 *
 * "Closest" is the squared distance between the two rectangles' corners, as Glance measures it.
 * Ties go to the smaller size. A size counts as fitting within [FIT_TOLERANCE_DP], because the
 * player's window is whole pixels and a 203dp cell at 2.75x can come back as 202.9dp.
 */
fun bestFitSize(available: DpSize, sizes: Collection<DpSize>): DpSize {
  val ordered = smallestFirst(sizes)
  val width = available.width.value + FIT_TOLERANCE_DP
  val height = available.height.value + FIT_TOLERANCE_DP
  return ordered
    .filter { it.width.value <= width && it.height.value <= height }
    .minByOrNull { distanceSquared(available, it) } ?: ordered.first()
}

/**
 * A widget body that follows its size the way a Glance `SizeMode.Responsive` widget does.
 *
 * [content] is composed once per entry of [sizes], each time with [LocalWidgetSize] set to that
 * breakpoint, so it branches on `LocalWidgetSize.current` exactly as Glance content branches on
 * `LocalSize.current`. Which one is shown is decided:
 * - while playing, by a `RemoteStateLayout` keyed on the player's window size, when the size is not
 *   known while recording; or
 * - while recording, by [bestFitSize], when [fixedSize] (or [LocalFixedWidgetSize]) names it. Only
 *   that branch is recorded.
 */
@RemoteComposable
@Composable
fun AdaptiveWidget(
  sizes: Set<DpSize>,
  modifier: RemoteModifier = RemoteModifier,
  fixedSize: DpSize? = LocalFixedWidgetSize.current,
  content: @Composable @RemoteComposable () -> Unit,
) {
  require(sizes.isNotEmpty()) { "an adaptive widget needs at least one size" }
  if (fixedSize != null) {
    // The same container the StateLayout would have been, so [modifier] means the same thing
    // whichever way the widget was recorded.
    RemoteBox(modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalWidgetSize provides bestFitSize(fixedSize, sizes)) { content() }
    }
    return
  }
  val ordered = smallestFirst(sizes)
  RemoteStateLayout(
    currentState = breakpointIndex(ordered).toRemoteInt(),
    states = *ordered.indices.toList().toIntArray(),
    modifier = modifier.fillMaxSize(),
  ) { index ->
    CompositionLocalProvider(LocalWidgetSize provides ordered[index]) { content() }
  }
}

/**
 * [bestFitSize] as a playback expression: the index into [ordered] of the breakpoint that fits the
 * player's window best. Each size scores its squared distance from the window, or [NO_FIT] when it
 * does not fit, and the lowest score wins; a strict comparison keeps the earlier — smaller — size
 * on a tie, and when nothing fits every score is [NO_FIT] and index 0, the smallest, stands.
 */
private fun breakpointIndex(ordered: List<DpSize>): RemoteFloat {
  val density = RemoteConfiguration.density
  val width = RemoteConfiguration.windowWidthPx / density
  val height = RemoteConfiguration.windowHeightPx / density
  fun score(size: DpSize): RemoteFloat {
    val dx = width - size.width.value
    val dy = height - size.height.value
    return selectIfLt(
      width + FIT_TOLERANCE_DP,
      size.width.value.rf,
      NO_FIT.rf,
      selectIfLt(height + FIT_TOLERANCE_DP, size.height.value.rf, NO_FIT.rf, dx * dx + dy * dy),
    )
  }
  var bestIndex: RemoteFloat = 0f.rf
  var bestScore = score(ordered.first())
  for (index in 1 until ordered.size) {
    val candidate = score(ordered[index])
    bestIndex = selectIfLt(candidate, bestScore, index.toFloat().rf, bestIndex)
    bestScore = selectIfLt(candidate, bestScore, candidate, bestScore)
  }
  return bestIndex
}

private fun smallestFirst(sizes: Collection<DpSize>): List<DpSize> =
  sizes.sortedWith(
    compareBy({ it.width.value * it.height.value }, { it.width.value }, { it.height.value })
  )

private fun distanceSquared(a: DpSize, b: DpSize): Float {
  val dx = a.width.value - b.width.value
  val dy = a.height.value - b.height.value
  return dx * dx + dy * dy
}

/** Slack, in dp, for a window size that rounds to whole pixels just short of a breakpoint. */
const val FIT_TOLERANCE_DP: Float = 0.5f

/** A score larger than any real squared distance, for a size that does not fit. */
private const val NO_FIT: Float = 1e9f

/** A launcher grid size as the [DpSize] a breakpoint is written in. */
val WidgetSize.dpSize: DpSize
  get() = DpSize(widthDp.dp, heightDp.dp)
