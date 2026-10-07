@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.runtime.Composable

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
  WidgetSurface {
    RemoteRow(
      RemoteModifier.fillMaxSize(),
      horizontalArrangement = RemoteArrangement.Center,
      verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
      WidgetButton("-", RemoteModifier.weight(1f), onClick = onDecrement)
      RemoteText("$count".rs, color = RemoteColor(WidgetColors.OnSurface), fontSize = 48.rsp)
      WidgetButton("+", RemoteModifier.weight(1f), onClick = onIncrement)
    }
  }
}
