package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

/**
 * Motion tied to a toggle, played by the device preview: a switch button writes `on`, a "Show by
 * state" box shows a pane per value, and the panes share a dot (`sharedElement`, with its own
 * timing) and differ by a badge that slides in (`animateEnterExit`). Frames go to
 * `$RM3_MOTION_FRAMES` when it is set.
 *
 * The dot is the point: flipping the switch moves one dot across the pane rather than fading one
 * out on the left and another in on the right.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3DeviceMotionUiTest {
  private val document: UiBuilderDocument = Json.decodeFromString(DOCUMENT)

  @Test
  fun `flipping the switch moves the shared dot across and slides the badge in`() =
    runDesktopComposeUiTest(width = 216, height = 124, testTimeout = 90.seconds) {
      val frames = flip()
      val dots = frames.map { it.bounds(::isRed) }
      val first = checkNotNull(dots.first()) { "no dot before the flip" }
      val last = checkNotNull(dots.last()) { "no dot after the flip" }
      val width = frames.first().width
      assertTrue(first.centerX < width / 4, "the dot starts on the left: $first")
      assertTrue(last.centerX > width * 3 / 4, "and ends on the right: $last")
      // One dot in flight, not one at each end: every frame has a single red run on its row.
      val midway = dots.filterNotNull().filter { it.centerX in width / 3..width * 2 / 3 }
      assertTrue(midway.isNotEmpty(), "the dot travels: ${dots.map { it?.centerX }}")
      frames.forEachIndexed { index, frame ->
        assertTrue(frame.runs(::isRed) <= 1, "frame $index shows the dot twice")
      }
      // The badge slides down into place as its pane comes in (`slideInBottom` names the way it
      // travels), straight down: it never moves sideways.
      val badges = frames.map { it.bounds(::isGreen) }
      val settled = checkNotNull(badges.last()) { "no badge after the flip" }
      assertTrue(
        badges.filterNotNull().any { it.top < settled.top - 4 },
        "the badge slides in from above: ${badges.map { it?.top }}",
      )
      assertTrue(
        badges.filterNotNull().all { it.left == settled.left },
        "${badges.map { it?.left }}",
      )
    }

  /** The frames from just before the switch is tapped until the transition has settled. */
  private fun ComposeUiTest.flip(): List<Pixels> {
    var ready = 0
    setContent { RemoteM3DevicePreview(document, widthDp = 216f, heightDp = 124f) { ready++ } }
    waitUntil(timeoutMillis = 15_000) { ready == 1 }
    mainClock.autoAdvance = false
    assertEquals(
      0,
      onAllNodesWithText("Remote M3 preview failed", substring = true).fetchSemanticsNodes().size,
    )
    repeat(5) { mainClock.advanceTimeByFrame() }
    val content = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG)
    val frames = mutableListOf(Pixels(content.captureToImage()))
    content.performTouchInput { click(Offset(width / 2f, 20f)) }
    repeat(40) {
      mainClock.advanceTimeByFrame()
      frames += Pixels(content.captureToImage())
    }
    System.getenv("RM3_MOTION_FRAMES")?.let { dir ->
      java.io.File(dir).mkdirs()
      frames.forEachIndexed { index, frame ->
        val skia = frame.image.asSkiaBitmap()
        java.io
          .File(dir, "f%02d.png".format(index))
          .writeBytes(org.jetbrains.skia.Image.makeFromBitmap(skia).encodeToData()!!.bytes)
      }
    }
    return frames
  }

  private class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int
      get() = (left + right) / 2

    override fun toString() = "($left,$top)-($right,$bottom)"
  }

  private class Pixels(val image: ImageBitmap) {
    private val map = image.toPixelMap()
    val width: Int
      get() = map.width

    fun bounds(test: (Float, Float, Float) -> Boolean): Bounds? {
      var left = Int.MAX_VALUE
      var top = Int.MAX_VALUE
      var right = -1
      var bottom = -1
      for (y in 0 until map.height) for (x in 0 until map.width) {
        val pixel = map[x, y]
        if (test(pixel.red, pixel.green, pixel.blue)) {
          left = minOf(left, x)
          top = minOf(top, y)
          right = maxOf(right, x)
          bottom = maxOf(bottom, y)
        }
      }
      return if (right < 0) null else Bounds(left, top, right, bottom)
    }

    /** The separate runs of matching pixels along the row through the matches' middle. */
    fun runs(test: (Float, Float, Float) -> Boolean): Int {
      val bounds = bounds(test) ?: return 0
      val y = (bounds.top + bounds.bottom) / 2
      var runs = 0
      var inside = false
      for (x in 0 until map.width) {
        val pixel = map[x, y]
        val matches = test(pixel.red, pixel.green, pixel.blue)
        if (matches && !inside) runs++
        inside = matches
      }
      return runs
    }
  }

  private companion object {
    fun isRed(r: Float, g: Float, b: Float) = r > 0.6f && g < 0.25f && b < 0.25f

    fun isGreen(r: Float, g: Float, b: Float) =
      r < 0.25f && g > 0.6f && b < 0.25f && abs(r - b) < 0.2f

    val DOCUMENT =
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "toggle-motion",
        "title": "Toggle motion",
        "revision": 1,
        "catalogPin": {"systemId": "remote-m3", "catalogRevision": "test",
          "capabilityDigest": "test"},
        "environment": {"widthDp": 216, "heightDp": 124, "density": 1, "theme": "dark",
          "fontScale": 1, "layoutDirection": "ltr"},
        "stateVariables": {"on": {"type": "value", "valueType": "bool", "initialValue": false,
          "nullable": false, "persistence": "session"}},
        "roots": ["widget"],
        "nodes": {
          "widget": {"id": "widget", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [],
            "slots": {"background": [], "content": ["column"]}},
          "column": {"id": "column", "componentId": "layout/column", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {"children": ["switch", "mode"]}},
          "switch": {"id": "switch", "componentId": "remote-m3/remote-switch-button",
            "properties": {"checked": {"type": "state", "variable": "on"}},
            "modifiers": [{"type": "fillMaxWidth"}, {"type": "height", "heightDp": 40}],
            "slots": {"label": ["label"]}},
          "label": {"id": "label", "componentId": "remote-m3/remote-text",
            "properties": {"text": {"type": "string", "value": "Motion"}},
            "modifiers": [], "slots": {}},
          "mode": {"id": "mode", "componentId": "layout/box",
            "properties": {"showByState": {"type": "object", "fields": {
              "selector": {"type": "state", "variable": "on"},
              "cases": {"type": "object", "fields": {
                "off": {"type": "bool", "value": false},
                "on": {"type": "bool", "value": true}}}}}},
            "modifiers": [{"type": "fillMaxWidth"}, {"type": "height", "heightDp": 60}],
            "slots": {"children": ["off", "on"]}},
          "off": {"id": "off", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {"children": ["dotOff"]}},
          "on": {"id": "on", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {"children": ["dotLane", "badgeLane"]}},
          "dotLane": {"id": "dotLane", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {"children": ["dotOn"]}},
          "badgeLane": {"id": "badgeLane", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "fillMaxSize"}], "slots": {"children": ["badge"]}},
          "dotOff": {"id": "dotOff", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "align", "alignment": "centerStart"},
              {"type": "size", "widthDp": 24, "heightDp": 24},
              {"type": "background", "color": "#FFFF0000"},
              {"type": "sharedElement", "key": 1, "durationMs": 400, "easing": "linear"}],
            "slots": {}},
          "dotOn": {"id": "dotOn", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "align", "alignment": "centerEnd"},
              {"type": "size", "widthDp": 24, "heightDp": 24},
              {"type": "background", "color": "#FFFF0000"},
              {"type": "sharedElement", "key": 1, "durationMs": 400, "easing": "linear"}],
            "slots": {}},
          "badge": {"id": "badge", "componentId": "layout/box", "properties": {},
            "modifiers": [{"type": "align", "alignment": "center"},
              {"type": "size", "widthDp": 20, "heightDp": 20},
              {"type": "background", "color": "#FF00FF00"},
              {"type": "animateEnterExit", "enter": "slideInBottom", "exit": "fadeOut",
                "durationMs": 400, "easing": "linear"}],
            "slots": {}}
        }
      }
      """
        .trimIndent()
  }
}
