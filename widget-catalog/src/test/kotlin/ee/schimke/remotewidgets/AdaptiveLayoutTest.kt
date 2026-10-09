package ee.schimke.remotewidgets

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * [AdaptiveLayout]'s breakpoints are written as cell counts — `"4x1"` in a design, in the builder's
 * property editor, in the exported call — so [launcherCellSize] is where a design's words become
 * the sizes [bestFitSize] chooses between.
 */
class AdaptiveLayoutTest {

  @Test
  fun `a cell count is Android's documented portrait size`() {
    WidgetSize.entries.forEach { assertThat(launcherCellSize(it.label)).isEqualTo(it.dpSize) }
  }

  @Test
  fun `a count the sheet does not name still has a size`() {
    assertThat(launcherCellSize("6x4")).isEqualTo(DpSize(422.dp, 456.dp))
  }

  @Test
  fun `anything else is refused by name`() {
    listOf("", "4", "4x", "0x1", "4 x 1", "four by one").forEach { label ->
      assertThrows(IllegalArgumentException::class.java) { launcherCellSize(label) }
    }
  }

  @Test
  fun `the builder offers the grid as each breakpoint's values`() {
    val policy = Json.parseToJsonElement(File("ui-builder.policy.json").readText()).jsonObject
    val component =
      policy.getValue("components").jsonObject.getValue("remote-widgets/adaptive-layout").jsonObject
    val grid =
      policy
        .getValue("frame")
        .jsonObject
        .getValue("geometry")
        .jsonObject
        .getValue("sizesDp")
        .jsonArray
        .map { it.jsonObject.getValue("label").jsonPrimitive.content }
    val properties = component.getValue("propertyCapabilities").jsonArray.map { it.jsonObject }
    assertThat(properties.map { it.getValue("name").jsonPrimitive.content })
      .containsExactly("compactSize", "mediumSize", "expandedSize")
    properties.forEach { property ->
      assertThat(property.getValue("allowedValues").jsonArray.map { it.jsonPrimitive.content })
        .containsExactlyElementsIn(grid)
        .inOrder()
    }
    assertThat(
        component.getValue("slotCapabilities").jsonArray.map {
          it.jsonObject.getValue("name").jsonPrimitive.content
        }
      )
      .containsExactly("compact", "medium", "expanded")
  }
}
