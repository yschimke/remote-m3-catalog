@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.SizeF
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * [DestinationsContent] as a launcher widget. A widget the person can resize is recorded with every
 * breakpoint and chooses during playback; one whose provider info forbids resizing has exactly one
 * size, so only that branch is recorded — see [fixedWidgetSize]. Not registered in the manifest:
 * this is a preview-only module, as [CounterWidget] is.
 */
class DestinationsWidget : RemoteComposeWidget() {

  @RemoteComposable
  @Composable
  override fun Content(context: Context, widgetId: Int) {
    CompositionLocalProvider(LocalFixedWidgetSize provides fixedWidgetSize(context, widgetId)) {
      DestinationsContent()
    }
  }
}

/**
 * The size of widget [widgetId] when it has exactly one: its provider declares no resize mode AND
 * the launcher reports a single rectangle for it. `RESIZE_NONE` alone is not enough — it only stops
 * the person resizing it, and a launcher may still give it a different portrait and landscape size
 * — so a widget is fixed only when its reported sizes (API 31's `OPTION_APPWIDGET_SIZES`) are one,
 * or, before that, when its min and max agree in both axes. Null otherwise, and the widget keeps
 * every breakpoint.
 */
fun fixedWidgetSize(context: Context, widgetId: Int): DpSize? {
  val manager = AppWidgetManager.getInstance(context)
  val info = manager.getAppWidgetInfo(widgetId) ?: return null
  if (info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) return null
  val options = manager.getAppWidgetOptions(widgetId)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // The untyped getter: the typed one is API 33, and OPTION_APPWIDGET_SIZES is API 31.
    @Suppress("DEPRECATION")
    val reported = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    if (!reported.isNullOrEmpty()) {
      val distinct = reported.distinct()
      return distinct.singleOrNull()?.let { DpSize(it.width.dp, it.height.dp) }
    }
  }
  val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
  val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
  val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
  val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
  if (minWidth <= 0 || minHeight <= 0) return null
  if (minWidth != maxWidth || minHeight != maxHeight) return null
  return DpSize(minWidth.dp, minHeight.dp)
}

/**
 * The breakpoints of [DestinationsContent]: Glance's `SizeMode.Responsive` sample
 * (developer.android.com/develop/ui/compose/glance/build-ui) moved onto the launcher grid. Its
 * 100×100, 250×100 and 250×250dp become the cell counts nearest them.
 */
val DestinationsSizes: Set<DpSize> =
  setOf(WidgetSize.S2x1.dpSize, WidgetSize.S4x1.dpSize, WidgetSize.S4x2.dpSize)

/**
 * Glance's "Where to?" sample as an [AdaptiveWidget]: one destination when narrow, two when wide,
 * and a title and a credit line once it is tall too. It branches on [LocalWidgetSize] the way the
 * Glance sample branches on `LocalSize`, and nothing else about it knows how it is resized.
 */
@RemoteComposable
@Composable
fun DestinationsContent(onDestination: ((String) -> Unit)? = null) {
  WidgetSurface {
    AdaptiveWidget(DestinationsSizes) {
      val size = LocalWidgetSize.current
      val wide = size.width >= WidgetSize.S4x1.dpSize.width
      val tall = size.height >= WidgetSize.S4x2.dpSize.height
      RemoteColumn(
        RemoteModifier.fillMaxSize(),
        verticalArrangement = RemoteArrangement.SpaceEvenly,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
      ) {
        if (tall) WidgetTitle("Where to?")
        RemoteRow(
          horizontalArrangement = RemoteArrangement.Center,
          verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
          val destinations = if (wide) listOf("Home", "Work") else listOf("Home")
          destinations.forEach { destination ->
            WidgetButton(destination, onClick = onDestination?.let { { it(destination) } })
          }
        }
        if (tall) WidgetLabel("Travel times by Example Transit")
      }
    }
  }
}
