@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * [AdaptiveWidget] as a component a design can place: up to three layouts, each authored at a
 * launcher grid size, of which the widget shows the one that fits it best.
 *
 * This is the shape the UI builder edits. Glance content branches on `LocalSize.current` in code; a
 * design has no code to branch in, so each breakpoint is a slot of its own — [compact], [medium],
 * [expanded] — and its size is a property naming the cell count it is laid out for (`"2x1"`,
 * `"4x1"`, `"4x2"`). Which slot shows is [AdaptiveWidget]'s choice: during playback, through a
 * `RemoteStateLayout` keyed on the widget's size, or while recording when the size is already
 * known. A slot left out is not a breakpoint, so a widget with only [compact] is a widget with one
 * layout.
 */
@RemoteComposable
@Composable
fun AdaptiveLayout(
  modifier: RemoteModifier = RemoteModifier,
  compactSize: String = "2x1",
  mediumSize: String = "4x1",
  expandedSize: String = "4x2",
  compact: @Composable @RemoteComposable () -> Unit,
  medium: @Composable @RemoteComposable () -> Unit = NoLayout,
  expanded: @Composable @RemoteComposable () -> Unit = NoLayout,
) {
  val branches = buildList {
    add(launcherCellSize(compactSize) to compact)
    if (medium !== NoLayout) add(launcherCellSize(mediumSize) to medium)
    if (expanded !== NoLayout) add(launcherCellSize(expandedSize) to expanded)
  }
  AdaptiveWidget(branches.map { it.first }.toSet(), modifier) {
    val size = LocalWidgetSize.current
    // Two slots authored at one size are one breakpoint; the first of them is the one it shows.
    branches.first { it.first == size }.second()
  }
}

/**
 * The default of [AdaptiveLayout]'s optional slots, compared by identity: a slot still holding it
 * was never authored, so it is not a breakpoint. A sentinel rather than a nullable lambda, because
 * a nullable composable parameter is not one the component record recognises as a slot.
 */
val NoLayout: @Composable @RemoteComposable () -> Unit = {}

/**
 * The [DpSize] of a launcher grid size written as its cell count, `"<columns>x<rows>"`: the
 * portrait size Android documents for it, by the formula [WidgetSize] uses. Any count is accepted,
 * not only the sheet's named sizes, because a launcher's grid is not limited to them.
 */
fun launcherCellSize(label: String): DpSize {
  val match = CELL_COUNT.matchEntire(label.trim())
  requireNotNull(match) { "`$label` is not a launcher grid size such as 3x2" }
  val (columns, rows) = match.destructured
  return DpSize(
    (WidgetSize.CELL_WIDTH_DP * columns.toInt() - WidgetSize.CELL_MARGIN_DP).dp,
    (WidgetSize.CELL_HEIGHT_DP * rows.toInt() - WidgetSize.CELL_MARGIN_DP).dp,
  )
}

private val CELL_COUNT = Regex("""([1-9][0-9]?)x([1-9][0-9]?)""")
