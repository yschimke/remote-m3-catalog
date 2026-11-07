package ee.schimke.remote.calculator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import ee.schimke.composeai.rcplayer.compose.LocalRcComponentCoordinates
import ee.schimke.composeai.rcplayer.compose.RcComponentCoordinatesListener
import ee.schimke.composeai.rcplayer.runtime.RcPlayerEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plays each backend's real document and drives it through the player's own touch handling: every
 * key press is a pointer event at a key's position, and every check reads the text the player laid
 * out. The same scenario runs against both backends and expects the same display, so the document's
 * expressions are held to the Kotlin engine.
 */
@OptIn(ExperimentalTestApi::class)
class CalculatorWidgetUiTest {
  @Test fun hostActions() = scenario(Backend.HostActions)

  @Test fun documentExpressions() = scenario(Backend.DocumentExpressions)

  /** What the player last laid out for each text component. */
  private class LaidOutText : RcComponentCoordinatesListener {
    private class Text(val text: String, val sizeSp: Float, val x: Float)

    private val texts = ConcurrentHashMap<Int, Text>()

    override fun onPositioned(componentId: Int, coordinates: LayoutCoordinates) = Unit

    override fun onTextLaidOut(
      componentId: Int,
      content: LayoutCoordinates,
      layout: TextLayoutResult,
    ) {
      texts[componentId] =
        Text(
          layout.layoutInput.text.text,
          layout.layoutInput.style.fontSize.value,
          content.localToRoot(Offset.Zero).x,
        )
    }

    /** The number: the one text at the display's size. */
    val display: String?
      get() = texts.values.singleOrNull { it.sizeSp == DISPLAY_SIZE.toFloat() }?.text

    /** The pending operation: the display row's small text, right of the `f(x)` key. */
    val expression: String?
      get() =
        texts.values
          .filter { it.sizeSp == EXPRESSION_SIZE.toFloat() && it.x > 40f * SCALE }
          .singleOrNull()
          ?.text
  }

  private fun keyCentre(row: Int, column: Int): Offset {
    val keyWidth = LargeWidgetHost.CONTENT_WIDTH_DP / 4
    val rowHeight = (LargeWidgetHost.CONTENT_HEIGHT_DP - DISPLAY_HEIGHT) / 4
    val pad = LargeWidgetHost.PADDING_DP
    return Offset(
      pad + column * keyWidth + keyWidth / 2,
      pad + DISPLAY_HEIGHT + row * rowHeight + rowHeight / 2,
    )
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

  private val displayRowY = LargeWidgetHost.PADDING_DP + DISPLAY_HEIGHT / 2

  private fun scenario(backend: Backend) = runComposeUiTest {
    val texts = LaidOutText()
    val events = java.util.concurrent.CopyOnWriteArrayList<RcPlayerEvent>()
    setContent {
      // A watch's density, so the captures read at the size a person sees them.
      CompositionLocalProvider(
        LocalDensity provides Density(SCALE),
        LocalRcComponentCoordinates provides texts,
      ) {
        CalculatorWidgetHost(backend, onPlayerEvent = { events += it })
      }
    }
    val widget = onNodeWithTag(widgetTestTag(backend))

    fun tapDp(at: Offset) {
      widget.performTouchInput { click(at * SCALE) }
      waitForIdle()
    }

    fun press(sequence: String) = sequence.forEach { key ->
      val (row, column) = keys.getValue(key)
      tapDp(keyCentre(row, column))
    }

    fun swipeDisplay() {
      widget.performTouchInput {
        swipe(Offset(60f, displayRowY) * SCALE, Offset(190f, displayRowY) * SCALE)
      }
      waitForIdle()
    }

    fun shows(expected: String, expression: String? = null) {
      // Not `waitUntil` alone: on a timeout the assertions below say what was drawn instead.
      runCatching {
        waitUntil(timeoutMillis = 5_000) {
          texts.display == expected && (expression == null || texts.expression == expression)
        }
      }
      assertEquals(expected, texts.display, "$backend display")
      if (expression != null) assertEquals(expression, texts.expression, "$backend expression")
    }

    fun snapshot(name: String) = widget.snapshot(backend, name)

    shows("0")
    snapshot("0-initial")

    press("106500")
    shows("106500")
    press("*")
    shows("106500", expression = "106500 ×")
    snapshot("1-pending-multiply")

    press("2=")
    shows("213000", expression = "")
    snapshot("2-result")

    // f(x), then the first key of the second set: ±. The set is a one-off.
    tapDp(Offset(LargeWidgetHost.PADDING_DP + 17f, displayRowY))
    waitForIdle()
    snapshot("3-more-operators")
    press("+")
    shows("-213000")
    press("+3=")
    shows("-212997")

    swipeDisplay()
    shows("0")
    snapshot("4-cleared-by-swipe")

    // A tap on the display is not a swipe.
    press("7")
    tapDp(Offset(LargeWidgetHost.PADDING_DP + 120f, displayRowY))
    shows("7")

    press("/0=")
    shows("Error")
    press("4")
    shows("4")

    press("+3==")
    shows("10")

    swipeDisplay()
    press(".25")
    shows("0.25")
    press("+1.5=")
    shows("1.75")

    press("2.50")
    shows("2.50")

    // A unary key's result is the pending operation's right operand, not a change of mind:
    // `2 + 3 x² + 4 =` is 15.
    swipeDisplay()
    press("2+3")
    tapDp(Offset(LargeWidgetHost.PADDING_DP + 17f, displayRowY))
    press("/")
    shows("9")
    press("+4=")
    shows("15")

    // An error from a unary key forgets the repeat too: `2 + 3 = ± √` is an error, and `4 =` after
    // it is 4, not 4 + 3.
    swipeDisplay()
    press("2+3=")
    tapDp(Offset(LargeWidgetHost.PADDING_DP + 17f, displayRowY))
    press("+")
    tapDp(Offset(LargeWidgetHost.PADDING_DP + 17f, displayRowY))
    press("*")
    shows("Error")
    press("4=")
    shows("4")

    // Every place a typed number can reach is shown.
    swipeDisplay()
    press(".0000001")
    shows("0.0000001")

    val hostActions = events.filter {
      it is RcPlayerEvent.HostNamedAction || it is RcPlayerEvent.HostAction
    }
    when (backend) {
      // The bridge handles the keys itself; only the swipe's two touch events reach the host.
      Backend.HostActions -> assertTrue(hostActions.isNotEmpty())
      // The whole scenario ran in the document.
      Backend.DocumentExpressions -> assertEquals(emptyList(), hostActions)
    }
  }

  private fun SemanticsNodeInteraction.snapshot(backend: Backend, name: String) {
    val directory =
      File(System.getProperty("calculator.screenshots") ?: "build/screenshots", backend.name)
    directory.mkdirs()
    ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "$name.png"))
  }
}

/** The density the widget is played at, so a dp position is this many pixels. */
private const val SCALE = 2f
