package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A circular indicator's ring is as wide as the design says, and otherwise sized to its window as
 * Wear Material 3 sizes its own: `largeStrokeWidth` of the window, 8dp under 225dp wide. The
 * library's flat 12dp default reads as heavy on a widget-sized ring.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3ProgressStrokeUiTest {

  @Test
  fun `the default stroke follows the window, and an authored one replaces it`() {
    val default = ring(strokeWidthDp = null)
    // The goal widget's content window is narrower than 225dp.
    assertContentEquals(default, ring(strokeWidthDp = 8f), "default is not 8dp in a 216dp widget")
    assertFalse(default.contentEquals(ring(strokeWidthDp = 12f)), "default drew the flat 12dp")
  }

  private fun ring(strokeWidthDp: Float?): IntArray {
    val design = goal(strokeWidthDp)
    var pixels: IntArray? = null
    runDesktopComposeUiTest(width = 480, height = 320) {
      var ready = 0
      setContent { RemoteM3DevicePreview(design, widthDp = 224f, heightDp = 84f) { ready++ } }
      waitUntil(timeoutMillis = 15_000) { ready == 1 }
      val map = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage().toPixelMap()
      pixels = IntArray(map.width * map.height) { map[it % map.width, it / map.width].hashCode() }
    }
    return requireNotNull(pixels)
  }

  private fun goal(strokeWidthDp: Float?): UiBuilderDocument {
    val design: UiBuilderDocument =
      lenient.decodeFromString(
        requireNotNull(javaClass.getResource("/remote-m3/golden-tiles-goal.json")).readText()
      )
    if (strokeWidthDp == null) return design
    val ring = design.nodes.getValue(RING)
    val strokeWidth =
      JsonObject(mapOf("type" to JsonPrimitive("float"), "value" to JsonPrimitive(strokeWidthDp)))
    return design.copy(
      nodes =
        design.nodes +
          (RING to
            ring.copy(properties = JsonObject(ring.properties + ("strokeWidth" to strokeWidth))))
    )
  }

  private val lenient = Json { ignoreUnknownKeys = true }

  private companion object {
    const val RING = "goal-progress-s4"
  }
}
