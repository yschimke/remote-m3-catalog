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
  fun `loops, clips, conditionals, morphs, curved text, gradients and Remote calls play`() =
    runDesktopComposeUiTest(width = 432, height = 248) {
      var ready = 0
      setContent {
        RemoteM3DevicePreview(document(CONTROL), widthDp = 216f, heightDp = 124f) { ready++ }
      }
      waitUntil(timeoutMillis = 15_000) { ready == 1 }
      onAllNodesWithText("Remote M3 preview failed", substring = true).assertCountEqualsZero()
      onAllNodesWithText("Unsupported", substring = true).assertCountEqualsZero()

      val pixels = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
      var red = 0
      for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
        val pixel = pixels[x, y]
        if (abs(pixel.red - 1f) < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f) red++
      }
      // Three squares from the loop, each read from its own index, so not stacked on one another.
      assertTrue(red > 3 * 8 * 8, "the repeated squares are missing ($red red pixels)")
    }

  @Test
  fun `a theme node re-skins its child, and a label button records`() =
    runDesktopComposeUiTest(width = 432, height = 248) {
      var ready = 0
      setContent {
        RemoteM3DevicePreview(document(THEMED), widthDp = 216f, heightDp = 124f) { ready++ }
      }
      waitUntil(timeoutMillis = 15_000) { ready == 1 }
      onAllNodesWithText("Remote M3 preview failed", substring = true).assertCountEqualsZero()
      onAllNodesWithText("Unsupported", substring = true).assertCountEqualsZero()

      val pixels = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
      var red = 0
      for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
        val pixel = pixels[x, y]
        if (abs(pixel.red - 1f) < 0.1f && pixel.green < 0.1f && pixel.blue < 0.1f) red++
      }
      // The button fills with `primary`, which the theme sets to red.
      assertTrue(red > 1_000, "the themed button is not red ($red red pixels)")
    }

  @Test
  fun `every published Remote Material 3 component records`() =
    runDesktopComposeUiTest(width = 432, height = 248) {
      var ready = 0
      val components = RemoteMaterial3.components
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

    val CONTROL =
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "remote-m3-control",
        "title": "Remote M3 control",
        "revision": 1,
        "catalogPin": {"systemId": "remote-m3", "catalogRevision": "test",
          "capabilityDigest": "test", "nativeRuntimeId": "remote-m3-test-runtime"},
        "environment": {"widthDp": 216, "heightDp": 124, "density": 1, "theme": "dark",
          "fontScale": 1, "layoutDirection": "ltr"},
        "stateVariables": {"shown": {"type": "value", "valueType": "bool", "initialValue": true,
          "nullable": false, "persistence": "session"}},
        "roots": ["root"],
        "nodes": {
          "root": {"id": "root", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [],
            "slots": {"background": [], "content": ["box"]}},
          "box": {"id": "box", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "fillMaxSize"},
              {"type": "remoteCall", "name": "graphicsLayer",
                "args": {"alpha": {"type": "float", "value": 1}}},
              {"type": "remoteCall", "name": "semantics",
                "args": {"contentDescription": {"type": "string", "value": "Dial"}}}],
            "slots": {"children": ["canvas", "time"]}},
          "canvas": {"id": "canvas", "componentId": "draw/canvas", "properties": {},
            "modifiers": [{"type": "size", "widthDp": 120, "heightDp": 60}],
            "slots": {"ops": ["squares", "lens", "alarm", "grow", "arc-label", "path-label"]}},
          "squares": {"id": "squares", "componentId": "draw/repeat", "properties": {
            "until": {"type": "float", "value": 3}}, "modifiers": [], "slots": {"ops": ["square"]}},
          "square": {"id": "square", "componentId": "draw/rect", "properties": {
            "xDp": {"type": "expr", "op": "mul", "args": [
              {"type": "binding", "value": "i"}, {"type": "int", "value": 20}]},
            "yDp": {"type": "float", "value": 0},
            "widthDp": {"type": "float", "value": 12}, "heightDp": {"type": "float", "value": 12},
            "color": {"type": "color", "value": "#FFFF0000"}}, "modifiers": [], "slots": {}},
          "lens": {"id": "lens", "componentId": "draw/clip", "properties": {
            "pathData": {"type": "string", "value": "M0 0 L24 0 L24 24 Z"}},
            "modifiers": [], "slots": {"ops": ["fill"]}},
          "fill": {"id": "fill", "componentId": "draw/rect", "properties": {
            "yDp": {"type": "float", "value": 20},
            "color": {"type": "color", "value": "#FF00FF00"},
            "gradient": {"type": "enum", "value": "horizontal"},
            "gradientColor": {"type": "color", "value": "#FF0000FF"}},
            "modifiers": [], "slots": {}},
          "alarm": {"id": "alarm", "componentId": "draw/if", "properties": {
            "condition": {"type": "state", "variable": "shown"}},
            "modifiers": [], "slots": {"ops": ["dot"]}},
          "dot": {"id": "dot", "componentId": "draw/circle", "properties": {
            "radiusDp": {"type": "float", "value": 4},
            "color": {"type": "color", "value": "#FF00FFFF"}}, "modifiers": [], "slots": {}},
          "grow": {"id": "grow", "componentId": "draw/morph", "properties": {
            "pathData": {"type": "string", "value": "M11 13 L13 13 L12 12 Z"},
            "toPathData": {"type": "string", "value": "M0 24 L24 24 L12 12 Z"},
            "progress": {"type": "float", "value": 0.5},
            "color": {"type": "color", "value": "#FFFFFF00"}}, "modifiers": [], "slots": {}},
          "arc-label": {"id": "arc-label", "componentId": "draw/text-circle", "properties": {
            "text": {"type": "string", "value": "ROUND"},
            "color": {"type": "color", "value": "#FFFFFFFF"}}, "modifiers": [], "slots": {}},
          "path-label": {"id": "path-label", "componentId": "draw/text-path", "properties": {
            "text": {"type": "string", "value": "ALONG"},
            "pathData": {"type": "string", "value": "M4 50 Q60 30 116 50"},
            "color": {"type": "color", "value": "#FFFFFFFF"}}, "modifiers": [], "slots": {}},
          "time": {"id": "time", "componentId": "remote-m3/remote-time-text", "properties": {
            "trailingText": {"type": "string", "value": "WED"}},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {}}
        }
      }
      """
        .trimIndent()

    val THEMED =
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "remote-m3-themed", "title": "Themed", "revision": 1,
        "catalogPin": {"systemId": "remote-m3", "catalogRevision": "test",
          "capabilityDigest": "test", "nativeRuntimeId": "remote-m3-test-runtime"},
        "environment": {"widthDp": 216, "heightDp": 124, "density": 1, "theme": "dark",
          "fontScale": 1, "layoutDirection": "ltr"},
        "stateVariables": {},
        "roots": ["root"],
        "nodes": {
          "root": {"id": "root", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [],
            "slots": {"background": [], "content": ["theme"]}},
          "theme": {"id": "theme", "componentId": "remote-m3/remote-material-theme",
            "properties": {"themePrimaryColor": {"type": "color", "value": "#FFFF0000"},
              "themeOnPrimaryColor": {"type": "colorToken", "value": "onSurface"}},
            "modifiers": [], "slots": {"children": ["button"]}},
          "button": {"id": "button", "componentId": "remote-m3/remote-label-button",
            "properties": {}, "modifiers": [{"type": "fillMaxSize"}],
            "slots": {"label": ["title"], "secondaryLabel": ["detail"]}},
          "title": {"id": "title", "componentId": "remote-m3/remote-text",
            "properties": {"text": {"type": "string", "value": "Start"}},
            "modifiers": [], "slots": {}},
          "detail": {"id": "detail", "componentId": "remote-m3/remote-text",
            "properties": {"text": {"type": "string", "value": "5 km"}},
            "modifiers": [], "slots": {}}
        }
      }
      """

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
