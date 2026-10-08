package ee.schimke.remotewidgets

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * A `canvasMapping` renames an authored name to the one its canvas adapter reads. One whose source
 * name the component does not have maps nothing, and the editing canvas silently draws the default
 * — a 48sp count drawn at body size, a surface drawn empty.
 */
class WidgetCanvasMappingTest {

  private val policy: JsonObject by lazy {
    var directory: File? = File(".").absoluteFile
    while (directory != null) {
      listOf(
          File(directory, "ui-builder.policy.json"),
          File(directory, "widget-catalog/ui-builder.policy.json"),
        )
        .firstOrNull { it.isFile && it.parentFile.name == "widget-catalog" }
        ?.let {
          return@lazy Json.parseToJsonElement(it.readText()).jsonObject
        }
      directory = directory.parentFile
    }
    error("could not find widget-catalog/ui-builder.policy.json")
  }

  @Test
  fun `a mapped property is one the component declares`() {
    policy.getValue("components").jsonObject.forEach { (id, value) ->
      val component = value.jsonObject
      val mapping = component["canvasMapping"]?.jsonObject ?: return@forEach
      val declared =
        component["propertyCapabilities"]?.jsonArray?.map {
          it.jsonObject.getValue("name").jsonPrimitive.content
        } ?: return@forEach
      mapping["properties"]?.jsonObject?.values?.forEach { source ->
        assertWithMessage(id).that(declared).contains(source.jsonPrimitive.content)
      }
    }
  }

  @Test
  fun `the text count keeps its size on the canvas`() {
    val text =
      policy.getValue("components").jsonObject.getValue("remote-widgets/remote-text").jsonObject
    assertWithMessage("remote-text fontSize")
      .that(
        text
          .getValue("canvasMapping")
          .jsonObject
          .getValue("properties")
          .jsonObject
          .getValue("fontSizeSp")
          .jsonPrimitive
          .content
      )
      .isEqualTo("fontSize")
  }

  @Test
  fun `the surface's content is the box's children`() {
    val surface =
      policy.getValue("components").jsonObject.getValue("remote-widgets/widget-surface").jsonObject
    assertWithMessage("widget-surface content")
      .that(
        surface
          .getValue("canvasMapping")
          .jsonObject
          .getValue("slots")
          .jsonObject
          .getValue("children")
          .jsonPrimitive
          .content
      )
      .isEqualTo("content")
  }
}
