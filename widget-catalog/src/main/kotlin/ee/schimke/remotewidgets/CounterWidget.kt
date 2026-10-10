@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.remote.foundation.layout.RemoteSpacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign

/**
 * The worked sample: AndroidX's `MyWidget` demo (`compose/remote/integration-tests/
 * player-view-demos/…/widgets/MyWidget.kt`) rebuilt from this sheet's components. A counter with a
 * minus and a plus, each a widget lambda action the launcher routes back here.
 *
 * It is the shape the UI builder's generated code takes for this catalog — a `RemoteComposeWidget`
 * whose `Content(context, widgetId)` is the design's body — which is why it is a real provider
 * class rather than a bare composable. Not registered in the manifest: this is a preview-only
 * module, as `:remote-catalog` is.
 */
class CounterWidget : RemoteComposeWidget() {

  @RemoteComposable
  @Composable
  override fun Content(context: Context, widgetId: Int) {
    val counter = readCounter(context, widgetId)
    CounterContent(
      count = counter,
      onDecrement = { writeCounter(context, widgetId, -1) },
      onIncrement = { writeCounter(context, widgetId, 1) },
    )
  }

  private fun store(context: Context, widgetId: Int): SharedPreferences =
    context.getSharedPreferences("WIDGET_$widgetId", Context.MODE_PRIVATE)

  private fun readCounter(context: Context, widgetId: Int): Int =
    store(context, widgetId).getInt(COUNTER, 0)

  private fun writeCounter(context: Context, widgetId: Int, delta: Int) {
    val store = store(context, widgetId)
    store.edit().putInt(COUNTER, store.getInt(COUNTER, 0) + delta).apply()
  }

  private companion object {
    const val COUNTER = "counter"
  }
}

/**
 * The counter's body, split from [CounterWidget] so a sticker can draw it without registering the
 * lambda actions — see [WidgetButton] for why a capture must not.
 */
@RemoteComposable
@Composable
fun CounterContent(
  count: Int,
  onDecrement: (() -> Unit)? = null,
  onIncrement: (() -> Unit)? = null,
) {
  AdaptiveWidget(
    setOf(
      WidgetSize.S2x1.dpSize,
      WidgetSize.S2x2.dpSize,
      WidgetSize.S3x1.dpSize,
      WidgetSize.S3x2.dpSize,
    )
  ) {
    val size = LocalWidgetSize.current
    val narrow = size.width < WidgetSize.S3x1.dpSize.width
    val tall = size.height >= WidgetSize.S2x2.dpSize.height
    WidgetSurface {
      if (tall) {
        RemoteColumn(
          RemoteModifier.fillMaxSize(),
          verticalArrangement = RemoteArrangement.SpaceEvenly,
          horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
          WidgetTitle("Counter")
          RemoteText(
            "$count".rs,
            color = RemoteColor(WidgetColors.OnSurface),
            fontSize = 48.rsp,
            maxLines = 1,
          )
          RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
            WidgetButton("−", contentDescription = "Decrease count", onClick = onDecrement)
            RemoteSpacer(RemoteModifier.size(8.rdp))
            WidgetButton("+", contentDescription = "Increase count", onClick = onIncrement)
          }
        }
      } else {
        RemoteRow(
          RemoteModifier.fillMaxSize(),
          horizontalArrangement = RemoteArrangement.Center,
          verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
          // At 2x1, retain the primary action instead of squeezing two targets and the value.
          if (!narrow) {
            WidgetButton("−", contentDescription = "Decrease count", onClick = onDecrement)
            RemoteSpacer(RemoteModifier.size(8.rdp))
          }
          RemoteText(
            "$count".rs,
            modifier = RemoteModifier.weight(1f),
            color = RemoteColor(WidgetColors.OnSurface),
            fontSize = (if (narrow) 32 else 48).rsp,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
          RemoteSpacer(RemoteModifier.size(8.rdp))
          WidgetButton("+", contentDescription = "Increase count", onClick = onIncrement)
        }
      }
    }
  }
}
