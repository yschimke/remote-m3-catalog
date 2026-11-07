package ee.schimke.remote.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import ee.schimke.composeai.rcplayer.runtime.RcHostActionValue
import ee.schimke.composeai.rcplayer.runtime.RcPlayerEvent
import kotlin.math.roundToInt

/** The Pixel Watch (squircle) large widget host: the container, its padding and its corners. */
internal object LargeWidgetHost {
  const val WIDTH_DP = 216f
  const val HEIGHT_DP = 124f
  const val PADDING_DP = 8f
  const val CORNER_DP = 26f
  const val CONTENT_WIDTH_DP = WIDTH_DP - 2 * PADDING_DP
  const val CONTENT_HEIGHT_DP = HEIGHT_DP - 2 * PADDING_DP
}

internal const val WIDGET_TEST_TAG = "calculator-widget"

/**
 * The widget as a launcher would show it: [CalculatorWidget] played live by rc-players' embedded
 * player inside the large container's frame.
 *
 * Key presses arrive as the bridge's named actions and go straight to [session]; the swipe's two
 * touch events carry a value, which the bridge does not route, so they arrive here.
 */
@Composable
fun CalculatorWidgetHost(session: CalculatorSession, modifier: Modifier = Modifier) {
  val density = LocalDensity.current
  val display =
    remember(density) {
      RemoteCreationDisplayInfo(
        (LargeWidgetHost.CONTENT_WIDTH_DP * density.density).roundToInt(),
        (LargeWidgetHost.CONTENT_HEIGHT_DP * density.density).roundToInt(),
        (160 * density.density).roundToInt(),
      )
    }
  Box(
    modifier
      .testTag(WIDGET_TEST_TAG)
      .size(LargeWidgetHost.WIDTH_DP.dp, LargeWidgetHost.HEIGHT_DP.dp)
      .clip(RoundedCornerShape(LargeWidgetHost.CORNER_DP.dp))
      .background(Palette.background)
      .padding(LargeWidgetHost.PADDING_DP.dp)
  ) {
    RcEmbeddedPlayer(
      display = display,
      modifier = Modifier.fillMaxSize(),
      theme = RcPlayerTheme.Dark,
      onEvent = { event -> session.onTouch(event, display.size.width) },
      onFailure = { it.printStackTrace() },
      key = session,
    ) {
      CalculatorWidget(session)
    }
  }
}

private fun CalculatorSession.onTouch(event: RcPlayerEvent, widthPx: Float) {
  val action = event as? RcPlayerEvent.HostNamedAction ?: return
  val x = (action.value as? RcHostActionValue.FloatValue)?.value ?: return
  when (action.name) {
    CalculatorSession.SWIPE_START -> swipeStarted(x)
    CalculatorSession.SWIPE_END -> swipeEnded(x, widthPx)
  }
}

fun main() = application {
  val session = remember { CalculatorSession() }
  Window(
    onCloseRequest = ::exitApplication,
    title = "Wear widget calculator",
    state = rememberWindowState(size = DpSize(420.dp, 340.dp)),
  ) {
    Column(
      Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CalculatorWidgetHost(session)
      BasicText(
        "Tap the keys. Swipe across the display to clear. f(x) swaps in ± % √ x².",
        style = TextStyle(color = Color(0xFFC4C6D0), fontSize = 12.sp),
      )
    }
  }
}
