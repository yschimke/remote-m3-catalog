package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.renderer.sdk.withTimeRunning
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/**
 * A device pane plays the design's clock only when the editor lets time run. Settled, it is a still
 * frame — animation time held at its first frame — like every other render of the design.
 *
 * The bar's left edge is `10 + time.animation * 40` dp, so it moves only with the player's own
 * clock; a `tween` beside it proves the animated-value lowering records.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3DevicePreviewTimeUiTest {
  private val settled: UiBuilderDocument = Json.decodeFromString(DOCUMENT)

  @Test
  fun `a settled pane holds animation time at its first frame`() =
    runDesktopComposeUiTest(width = 432, height = 248, testTimeout = 90.seconds) {
      val edges = barEdges(settled)
      assertEquals(edges[0], edges[1], "a settled pane moved with animation time: $edges")
    }

  @Test
  fun `a running pane plays animation time`() =
    runDesktopComposeUiTest(width = 432, height = 248, testTimeout = 90.seconds) {
      val edges = barEdges(settled.withTimeRunning(true))
      assertNotEquals(edges[0], edges[1], "a running pane held still: $edges")
    }

  /** The bar's left edge, half a second of frames apart. */
  private fun ComposeUiTest.barEdges(document: UiBuilderDocument): List<Int> {
    var ready = 0
    setContent { RemoteM3DevicePreview(document, widthDp = 216f, heightDp = 124f) { ready++ } }
    waitUntil(timeoutMillis = 15_000) { ready == 1 }
    // A running clock asks for a frame every frame, so from here frames are advanced by hand.
    mainClock.autoAdvance = false
    onAllNodesWithText("Remote M3 preview failed", substring = true).fetchSemanticsNodes().let {
      assertEquals(0, it.size, "the preview failed to record")
    }
    return (0 until 2).map {
      repeat(30) { mainClock.advanceTimeByFrame() }
      val pixels = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
      val y = pixels.height / 2
      (0 until pixels.width).firstOrNull { x ->
        val pixel = pixels[x, y]
        abs(pixel.red - 1f) < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f
      } ?: -1
    }
  }

  private companion object {
    val DOCUMENT =
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "device-time",
        "title": "Device time",
        "revision": 1,
        "catalogPin": {"systemId": "remote-m3", "catalogRevision": "test",
          "capabilityDigest": "test", "nativeRuntimeId": "remote-m3-test-runtime"},
        "environment": {"widthDp": 216, "heightDp": 124, "density": 1, "theme": "dark",
          "fontScale": 1, "layoutDirection": "ltr", "fixedTime": "2024-05-16T10:10:30Z",
          "animations": "settled"},
        "stateVariables": {"on": {"type": "value", "valueType": "bool", "initialValue": false,
          "nullable": false, "persistence": "session"}},
        "roots": ["root"],
        "nodes": {
          "root": {"id": "root", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [],
            "slots": {"background": [], "content": ["canvas"]}},
          "canvas": {"id": "canvas", "componentId": "draw/canvas", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}],
            "slots": {"ops": ["bar", "dot"]}},
          "bar": {"id": "bar", "componentId": "draw/rect", "properties": {
            "xDp": {"type": "expr", "op": "add", "args": [{"type": "int", "value": 10},
              {"type": "expr", "op": "mul", "args": [
                {"type": "system", "value": "time.animation"}, {"type": "int", "value": 40}]}]},
            "yDp": {"type": "float", "value": 0},
            "widthDp": {"type": "float", "value": 8}, "heightDp": {"type": "float", "value": 124},
            "color": {"type": "color", "value": "#FFFF0000"}},
            "modifiers": [], "slots": {}},
          "dot": {"id": "dot", "componentId": "draw/circle", "properties": {
            "radiusDp": {"type": "expr", "op": "tween", "args": [
              {"type": "expr", "op": "select", "args": [{"type": "state", "variable": "on"},
                {"type": "int", "value": 20}, {"type": "int", "value": 6}]},
              {"type": "int", "value": 300}, {"type": "string", "value": "overshoot"}]},
            "color": {"type": "color", "value": "#FF00FF00"}},
            "modifiers": [], "slots": {}}
        }
      }
      """
        .trimIndent()
  }
}
