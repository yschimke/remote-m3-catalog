package ee.schimke.remote.calculator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Plays the real widget document and drives it through the player's own touch handling: every key
 * press below is a pointer event at a key's position, so it exercises the document's click actions
 * and the bridge's routing, not the session directly.
 */
@OptIn(ExperimentalTestApi::class)
class CalculatorWidgetUiTest {
  // The layout's grid, in dp from the widget's top-left: the content slot starts at the padding,
  // the display row is DISPLAY_HEIGHT tall, then four equal rows of four equal columns.
  private val content = LargeWidgetHost.CONTENT_WIDTH_DP to LargeWidgetHost.CONTENT_HEIGHT_DP
  private val pad = LargeWidgetHost.PADDING_DP
  private val displayHeight = 26f

  private fun keyCentre(row: Int, column: Int): Pair<Float, Float> {
    val keyWidth = content.first / 4
    val rowHeight = (content.second - displayHeight) / 4
    val x = pad + column * keyWidth + keyWidth / 2
    val y = pad + displayHeight + row * rowHeight + rowHeight / 2
    return x to y
  }

  private val keys =
    mapOf(
      '1' to (0 to 0),
      '2' to (0 to 1),
      '3' to (0 to 2),
      '+' to (0 to 3),
      '4' to (1 to 0),
      '5' to (1 to 1),
      '6' to (1 to 2),
      '-' to (1 to 3),
      '7' to (2 to 0),
      '8' to (2 to 1),
      '9' to (2 to 2),
      '*' to (2 to 3),
      '.' to (3 to 0),
      '0' to (3 to 1),
      '=' to (3 to 2),
      '/' to (3 to 3),
    )

  private fun ComposeUiTest.widget(): SemanticsNodeInteraction = onNodeWithTag(WIDGET_TEST_TAG)

  private fun ComposeUiTest.tap(x: Float, y: Float) {
    widget().performTouchInput { click(Offset(x * SCALE, y * SCALE)) }
    waitForIdle()
  }

  private fun ComposeUiTest.press(sequence: String) = sequence.forEach { key ->
    val (row, column) = keys.getValue(key)
    val (x, y) = keyCentre(row, column)
    tap(x, y)
  }

  private fun ComposeUiTest.pressMore() = tap(pad + 17f, pad + displayHeight / 2)

  private fun ComposeUiTest.swipeDisplay() {
    val y = pad + displayHeight / 2
    widget().performTouchInput {
      swipe(Offset((pad + 60f) * SCALE, y * SCALE), Offset((pad + 190f) * SCALE, y * SCALE))
    }
    waitForIdle()
  }

  private fun ComposeUiTest.awaitDisplay(session: CalculatorSession, expected: String) {
    waitUntil(timeoutMillis = 5_000) { session.calculator.display == expected }
    assertEquals(expected, session.calculator.display)
  }

  private fun ComposeUiTest.snapshot(name: String) {
    waitForIdle()
    val directory = File(System.getProperty("calculator.screenshots") ?: "build/screenshots")
    directory.mkdirs()
    ImageIO.write(widget().captureToImage().toAwtImage(), "png", File(directory, "$name.png"))
  }

  @Test
  fun keysSwipeAndMoreOperators() = runComposeUiTest {
    val session = CalculatorSession()
    setContent {
      // A watch's density, so the captures read at the size a person sees them.
      CompositionLocalProvider(LocalDensity provides Density(SCALE)) {
        CalculatorWidgetHost(session)
      }
    }
    waitUntil(timeoutMillis = 10_000) { runCatching { widget().assertExists() }.isSuccess }
    // The player renders asynchronously; give the first document time to arrive.
    Thread.sleep(1_500)
    waitForIdle()
    snapshot("0-initial")

    press("106500")
    awaitDisplay(session, "106500")
    press("*")
    awaitDisplay(session, "106500")
    assertEquals(Operator.Multiply, session.calculator.pending)
    snapshot("1-pending-multiply")

    press("2=")
    awaitDisplay(session, "213000")
    snapshot("2-result")

    pressMore()
    waitUntil { session.moreOperators }
    snapshot("3-more-operators")
    // In the second set, the third row of the operator column is √.
    press("*")
    awaitDisplay(session, "461.51923")
    assertEquals(false, session.moreOperators)

    swipeDisplay()
    awaitDisplay(session, "0")
    snapshot("4-cleared-by-swipe")

    // A tap on the display is not a swipe.
    press("7")
    tap(pad + 120f, pad + displayHeight / 2)
    awaitDisplay(session, "7")
  }
}

/** The density the widget is played at, so a dp position is this many pixels. */
private const val SCALE = 2f
