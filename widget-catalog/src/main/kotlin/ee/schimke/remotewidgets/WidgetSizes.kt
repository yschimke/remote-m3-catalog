package ee.schimke.remotewidgets

import androidx.compose.ui.tooling.preview.Preview

// ---------------------------------------------------------------------------------------------
// Launcher grid sizes — the frame a Mobile Launcher Widget is drawn into.
//
// A launcher widget is not sized by device. The launcher hands it a rectangle measured in GRID
// CELLS (2x1, 3x2, …), and the dp behind a cell is the launcher's. So this sheet names its frames
// by cell count, and every frame below is the portrait size Android documents for that count:
//
//   width  = 73n − 16 dp      height = 118m − 16 dp
//
// from "Determine a size for your widget"
// (https://developer.android.com/develop/ui/views/appwidgets/layouts), measured on a Pixel 4's 5x4
// grid. Real launchers vary their cell size and margins, which is exactly why the CELL COUNT is
// what a design states and the dp is the reference rendering of it. `ui-builder.policy.json`'s
// `frame.geometry.sizesDp` and `catalog.spec.json`'s `breakpoints` carry the same table; the
// `WidgetSizeTest` holds all three to this enum.
//
// 440dpi is the Pixel 4's, the device the table was measured on, so a sticker reads at the
// density the published dp were taken at.
// ---------------------------------------------------------------------------------------------

/** One launcher grid footprint: [columns] × [rows] cells. */
enum class WidgetSize(val columns: Int, val rows: Int) {
  S1x1(1, 1),
  S2x1(2, 1),
  S2x2(2, 2),
  S3x1(3, 1),
  S3x2(3, 2),
  S4x1(4, 1),
  S4x2(4, 2),
  S4x3(4, 3),
  S5x2(5, 2);

  /** `3x2` — the name a person picks a widget by, and the preview name of its sticker. */
  val label: String
    get() = "${columns}x$rows"

  /** Portrait width in dp: `73n − 16`. */
  val widthDp: Int
    get() = 73 * columns - 16

  /** Portrait height in dp: `118m − 16`. */
  val heightDp: Int
    get() = 118 * rows - 16

  companion object {
    /** The launcher's portrait cell pitch, and the dp both formulas subtract for the margins. */
    const val CELL_WIDTH_DP = 73
    const val CELL_HEIGHT_DP = 118
    const val CELL_MARGIN_DP = 16

    /** The Pixel 4's density, which the documented table was measured on. */
    const val DPI = 440

    fun fromLabel(label: String): WidgetSize? = entries.firstOrNull { it.label == label }
  }
}

// Kotlin annotations cannot compute their arguments, so each frame spells its dp literally. The
// literal is the formula's value, and `WidgetSizeTest` fails the build if one drifts from it.

/** A 1x1 widget: 57×102dp. */
@Preview(name = "1x1", showBackground = false, device = "spec:width=57dp,height=102dp,dpi=440")
annotation class Widget1x1

/** A 2x1 widget: 130×102dp. */
@Preview(name = "2x1", showBackground = false, device = "spec:width=130dp,height=102dp,dpi=440")
annotation class Widget2x1

/** A 2x2 widget: 130×220dp. */
@Preview(name = "2x2", showBackground = false, device = "spec:width=130dp,height=220dp,dpi=440")
annotation class Widget2x2

/** A 3x1 widget: 203×102dp. */
@Preview(name = "3x1", showBackground = false, device = "spec:width=203dp,height=102dp,dpi=440")
annotation class Widget3x1

/** A 3x2 widget: 203×220dp. */
@Preview(name = "3x2", showBackground = false, device = "spec:width=203dp,height=220dp,dpi=440")
annotation class Widget3x2

/** A 4x1 widget: 276×102dp. */
@Preview(name = "4x1", showBackground = false, device = "spec:width=276dp,height=102dp,dpi=440")
annotation class Widget4x1

/** A 4x2 widget: 276×220dp. */
@Preview(name = "4x2", showBackground = false, device = "spec:width=276dp,height=220dp,dpi=440")
annotation class Widget4x2

/** A 4x3 widget: 276×338dp. */
@Preview(name = "4x3", showBackground = false, device = "spec:width=276dp,height=338dp,dpi=440")
annotation class Widget4x3

/** A 5x2 widget: 349×220dp. */
@Preview(name = "5x2", showBackground = false, device = "spec:width=349dp,height=220dp,dpi=440")
annotation class Widget5x2

/**
 * A widget that RESPONDS to its footprint, fanned across the sizes a person is most likely to give
 * it. 3x2 leads because it is the size the sample widget is authored at; the rest are the same
 * widget resized on the home screen, which is the question a responsive widget has to answer.
 */
@Preview(name = "3x2", showBackground = false, device = "spec:width=203dp,height=220dp,dpi=440")
@Preview(name = "2x1", showBackground = false, device = "spec:width=130dp,height=102dp,dpi=440")
@Preview(name = "2x2", showBackground = false, device = "spec:width=130dp,height=220dp,dpi=440")
@Preview(name = "4x1", showBackground = false, device = "spec:width=276dp,height=102dp,dpi=440")
@Preview(name = "4x2", showBackground = false, device = "spec:width=276dp,height=220dp,dpi=440")
annotation class WidgetSizes
