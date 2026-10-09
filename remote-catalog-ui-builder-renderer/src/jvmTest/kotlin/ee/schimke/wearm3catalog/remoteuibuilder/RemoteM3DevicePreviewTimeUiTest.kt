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
 * A device pane plays the design's wall clock only when the editor lets time run. Settled, the
 * clock is pinned at `fixedTime`, like every other render of the design.
 *
 * The red bar's left edge is `10 + (time.continuousSecond % 1) * 100` dp, so it moves only with the
 * wall clock. The green bar's is `10 + time.animation * 40` dp, the player's own clock, which a
 * running pane plays; a `tween` beside them proves the animated-value lowering records.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3DevicePreviewTimeUiTest {
  private val settled: UiBuilderDocument = Json.decodeFromString(DOCUMENT)

  @Test
  fun `a settled pane holds the wall clock at its fixed time`() =
    runDesktopComposeUiTest(width = 432, height = 248, testTimeout = 90.seconds) {
      val (wall, _) = edges(settled)
      assertEquals(wall[0], wall[1], "a settled pane moved with the wall clock: $wall")
    }

  @Test
  fun `a running pane plays the wall clock and animation time`() =
    runDesktopComposeUiTest(width = 432, height = 248, testTimeout = 90.seconds) {
      val (wall, animation) = edges(settled.withTimeRunning(true))
      assertNotEquals(wall[0], wall[1], "a running pane held the wall clock: $wall")
      assertNotEquals(animation[0], animation[1], "a running pane held animation time: $animation")
    }

  /** Each bar's left edge, sampled twice a quarter of a second of real and frame time apart. */
  private fun ComposeUiTest.edges(document: UiBuilderDocument): Pair<List<Int>, List<Int>> {
    var ready = 0
    setContent { RemoteM3DevicePreview(document, widthDp = 216f, heightDp = 124f) { ready++ } }
    waitUntil(timeoutMillis = 15_000) { ready == 1 }
    // A running clock asks for a frame every frame, so from here frames are advanced by hand.
    mainClock.autoAdvance = false
    onAllNodesWithText("Remote M3 preview failed", substring = true).fetchSemanticsNodes().let {
      assertEquals(0, it.size, "the preview failed to record")
    }
    val samples =
      (0 until 2).map {
        Thread.sleep(250)
        repeat(15) { mainClock.advanceTimeByFrame() }
        val pixels = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
        fun edge(row: Int, red: Boolean) =
          (0 until pixels.width).firstOrNull { x ->
            val pixel = pixels[x, row]
            if (red) abs(pixel.red - 1f) < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f
            else pixel.red < 0.1f && abs(pixel.green - 1f) < 0.1f && pixel.blue < 0.1f
          } ?: -1
        edge(pixels.height / 4, red = true) to edge(pixels.height * 3 / 4, red = false)
      }
    return samples.map { it.first } to samples.map { it.second }
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
            "slots": {"ops": ["bar", "clock", "dot"]}},
          "bar": {"id": "bar", "componentId": "draw/rect", "properties": {
            "xDp": {"type": "expr", "op": "add", "args": [{"type": "int", "value": 10},
              {"type": "expr", "op": "mul", "args": [
                {"type": "expr", "op": "mod", "args": [
                  {"type": "system", "value": "time.continuousSecond"},
                  {"type": "int", "value": 1}]},
                {"type": "int", "value": 100}]}]},
            "yDp": {"type": "float", "value": 0},
            "widthDp": {"type": "float", "value": 8}, "heightDp": {"type": "float", "value": 62},
            "color": {"type": "color", "value": "#FFFF0000"}},
            "modifiers": [], "slots": {}},
          "clock": {"id": "clock", "componentId": "draw/rect", "properties": {
            "xDp": {"type": "expr", "op": "add", "args": [{"type": "int", "value": 10},
              {"type": "expr", "op": "mul", "args": [
                {"type": "system", "value": "time.animation"}, {"type": "int", "value": 40}]}]},
            "yDp": {"type": "float", "value": 62},
            "widthDp": {"type": "float", "value": 8}, "heightDp": {"type": "float", "value": 62},
            "color": {"type": "color", "value": "#FF00FF00"}},
            "modifiers": [], "slots": {}},
          "dot": {"id": "dot", "componentId": "draw/circle", "properties": {
            "radiusDp": {"type": "expr", "op": "tween", "args": [
              {"type": "expr", "op": "select", "args": [{"type": "state", "variable": "on"},
                {"type": "int", "value": 20}, {"type": "int", "value": 6}]},
              {"type": "int", "value": 300}, {"type": "string", "value": "overshoot"}]},
            "color": {"type": "color", "value": "#FF0000FF"}},
            "modifiers": [], "slots": {}}
        }
      }
      """
        .trimIndent()
  }
}
