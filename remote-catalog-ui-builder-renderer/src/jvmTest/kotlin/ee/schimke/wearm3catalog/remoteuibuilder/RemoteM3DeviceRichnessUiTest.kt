package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.RemoteMaterial3
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * The device preview plays what the builder can now author: a drawing recorded as the
 * `RemoteCanvas` the export writes, computed values as live expressions, and every published Remote
 * Material 3 component as itself rather than as red "Unsupported" text.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3DeviceRichnessUiTest {

  @Test
  fun `a canvas, a computed label and a page indicator are recorded and played`() =
    runDesktopComposeUiTest(width = 432, height = 248) {
      var ready = 0
      setContent {
        RemoteM3DevicePreview(document(DRAWING), widthDp = 216f, heightDp = 124f) { ready++ }
      }
      waitUntil(timeoutMillis = 15_000) { ready == 1 }
      onAllNodesWithText("Remote M3 preview failed", substring = true).assertCountEqualsZero()

      val pixels = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
      var red = 0
      for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
        val pixel = pixels[x, y]
        if (abs(pixel.red - 1f) < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f) red++
      }
      assertTrue(red > 200, "the canvas drew no red square ($red red pixels)")
    }

  @Test
  fun `every published Remote Material 3 component records`() =
    runDesktopComposeUiTest(width = 432, height = 248) {
      var ready = 0
      val components = RemoteMaterial3.components.filter { it.componentId !in PLAYER_GAPS }
      var current by mutableStateOf(document(single(components.first().componentId)))
      setContent { RemoteM3DevicePreview(current, widthDp = 216f, heightDp = 124f) { ready++ } }
      components.forEachIndexed { index, component ->
        if (index > 0) current = document(single(component.componentId, revision = index + 1))
        waitUntil(timeoutMillis = 15_000) {
          ready == index + 1 ||
            onAllNodesWithText("Remote M3 preview failed", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
        }
        val failures =
          onAllNodesWithText("Remote M3 preview failed", substring = true)
            .fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
        assertTrue(failures.isEmpty(), "${component.componentId} failed to record: $failures")
      }
    }

  private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEqualsZero() {
    assertTrue(fetchSemanticsNodes().isEmpty())
  }

  private fun document(json: String): UiBuilderDocument = Json.decodeFromString(json)

  private fun single(componentId: String, revision: Int = 1): String =
    DRAWING.replace("\"revision\": 1", "\"revision\": $revision")
      .replace(
        "\"content\": [\"column\"]",
        "\"content\": [\"subject\"]",
      )
      .replace(
        "\"nodes\": {",
        """
        "nodes": {
          "subject": {"id": "subject", "componentId": "$componentId",
            "properties": {}, "modifiers": [], "slots": {}},
        """
          .trimIndent(),
      )

  private companion object {
    /**
     * Recorded, but not yet playable by rc-player-compose 2.1.2 on this Compose line, so the
     * preview shows them as unsupported: the edge button's conic path needs a newer Skia
     * `Path.conicTo`, and the slider's weighted bar canvas reads a component value the player
     * rejects.
     */
    val PLAYER_GAPS = setOf("remote-m3/remote-edge-button", "remote-m3/remote-slider")

    val DRAWING =
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "remote-m3-richness",
        "title": "Remote M3 richness",
        "revision": 1,
        "catalogPin": {"systemId": "remote-m3", "catalogRevision": "test",
          "capabilityDigest": "test", "nativeRuntimeId": "remote-m3-test-runtime"},
        "environment": {"widthDp": 216, "heightDp": 124, "density": 1, "theme": "dark",
          "fontScale": 1, "layoutDirection": "ltr"},
        "stateVariables": {"count": {"type": "value", "valueType": "int", "initialValue": 3,
          "nullable": false, "persistence": "session"}},
        "roots": ["root"],
        "nodes": {
          "root": {"id": "root", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [],
            "slots": {"background": [], "content": ["column"]}},
          "column": {"id": "column", "componentId": "layout/column", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}],
            "slots": {"children": ["canvas", "label", "pages"]},
            "eventBindings": {"click": [{"type": "set", "variable": "count", "value": 4}]}},
          "canvas": {"id": "canvas", "componentId": "draw/canvas", "properties": {},
            "modifiers": [{"type": "size", "widthDp": 48, "heightDp": 48}],
            "slots": {"ops": ["square", "ring"]}},
          "square": {"id": "square", "componentId": "draw/rect", "properties": {
            "xDp": {"type": "float", "value": 8}, "yDp": {"type": "float", "value": 8},
            "widthDp": {"type": "float", "value": 32}, "heightDp": {"type": "float", "value": 32},
            "color": {"type": "color", "value": "#FFFF0000"}}, "modifiers": [], "slots": {}},
          "ring": {"id": "ring", "componentId": "draw/arc", "properties": {
            "style": {"type": "enum", "value": "stroke"},
            "strokeWidthDp": {"type": "float", "value": 4},
            "sweepAngle": {"type": "expr", "op": "mul", "args": [
              {"type": "state", "variable": "count"}, {"type": "int", "value": 60}]},
            "color": {"type": "colorToken", "value": "primary"}}, "modifiers": [], "slots": {}},
          "label": {"id": "label", "componentId": "remote-m3/remote-text", "properties": {
            "text": {"type": "expr", "op": "concat", "args": [
              {"type": "string", "value": "Count "}, {"type": "state", "variable": "count"}]}},
            "modifiers": [], "slots": {}},
          "pages": {"id": "pages", "componentId": "remote-m3/remote-horizontal-page-indicator",
            "properties": {"pageCount": {"type": "int", "value": 5},
              "selectedPage": {"type": "state", "variable": "count"}},
            "modifiers": [], "slots": {}}
        }
      }
      """
        .trimIndent()
  }
}
