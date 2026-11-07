package ee.schimke.remote.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ee.schimke.composeai.rcplayer.compose.RcPlayerTheme
import ee.schimke.composeai.rcplayer.embedded.RcEmbeddedPlayer
import ee.schimke.composeai.rcplayer.runtime.RcPlayerEvent
import kotlin.math.roundToInt

/** Where the calculator's logic runs. The layout is the same document either way. */
enum class Backend(val title: String) {
  /** Named host values and host actions: the app process calculates ([CalculatorSession]). */
  HostActions("Host actions"),

  /** Remote state and value-change expressions: the document calculates by itself. */
  DocumentExpressions("Document expressions"),
}

/** The Pixel Watch (squircle) large widget host: the container, its padding and its corners. */
internal object LargeWidgetHost {
  const val WIDTH_DP = 216f
  const val HEIGHT_DP = 124f
  const val PADDING_DP = 8f
  const val CORNER_DP = 26f
  const val CONTENT_WIDTH_DP = WIDTH_DP - 2 * PADDING_DP
  const val CONTENT_HEIGHT_DP = HEIGHT_DP - 2 * PADDING_DP
}

internal fun widgetTestTag(backend: Backend) = "calculator-widget-${backend.name}"

/**
 * The widget as a launcher would show it: [CalculatorLayout] played live by rc-players' embedded
 * player inside the large container's frame, with its logic in [backend].
 *
 * [session] is the host-actions backend's state. The document-expressions backend never touches it,
 * and nothing on this side reads the document's own state.
 */
@Composable
fun CalculatorWidgetHost(
  backend: Backend,
  modifier: Modifier = Modifier,
  session: CalculatorSession = remember { CalculatorSession() },
  /** Every event the player raises, for whoever is watching (the UI test). */
  onPlayerEvent: (RcPlayerEvent) -> Unit = {},
) {
  val density = LocalDensity.current
  val display =
    remember(density) {
      RemoteCreationDisplayInfo(
        (LargeWidgetHost.CONTENT_WIDTH_DP * density.density).roundToInt(),
        (LargeWidgetHost.CONTENT_HEIGHT_DP * density.density).roundToInt(),
        (160 * density.density).roundToInt(),
      )
    }
  val widthPx = display.size.width
  Box(
    modifier
      .testTag(widgetTestTag(backend))
      .size(LargeWidgetHost.WIDTH_DP.dp, LargeWidgetHost.HEIGHT_DP.dp)
      .clip(RoundedCornerShape(LargeWidgetHost.CORNER_DP.dp))
      .background(Palette.background)
      .padding(LargeWidgetHost.PADDING_DP.dp)
  ) {
    RcEmbeddedPlayer(
      display = display,
      modifier = Modifier.fillMaxSize(),
      theme = RcPlayerTheme.Dark,
      onEvent = { event ->
        onPlayerEvent(event)
        if (backend == Backend.HostActions) session.onPlayerEvent(event, widthPx)
      },
      onFailure = { it.printStackTrace() },
      key = backend to session,
    ) {
      val bindings =
        when (backend) {
          Backend.HostActions -> rememberHostBindings(session)
          Backend.DocumentExpressions -> rememberDocumentBindings(widthPx * SWIPE_FRACTION)
        }
      CalculatorLayout(bindings)
    }
  }
}

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Wear widget calculator",
    state = rememberWindowState(size = DpSize(600.dp, 320.dp)),
  ) {
    Column(
      Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Backend.entries.forEach { backend ->
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CalculatorWidgetHost(backend)
            BasicText(backend.title, style = TextStyle(color = Color.White, fontSize = 13.sp))
          }
        }
      }
      BasicText(
        "Tap the keys. Swipe across the display to clear. f(x) swaps in ± % √ x².",
        style = TextStyle(color = Color(0xFFC4C6D0), fontSize = 12.sp),
      )
    }
  }
}
