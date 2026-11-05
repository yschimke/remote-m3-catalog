package ee.schimke.wearm3catalog.remoteuibuilder

import ee.schimke.composeai.uibuilder.export.UiBuilderNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** A text's own Google Fonts family is requested from the registry, as the theme's are. */
class TextGoogleFamiliesTest {
  private fun text(id: String, family: String?) =
    UiBuilderNode(
      id = id,
      componentId = "remote-m3/remote-text",
      properties =
        buildJsonObject {
          family?.let {
            putJsonObject("fontFamily") {
              put("type", "string")
              put("value", it)
            }
          }
        },
    )

  @Test
  fun `only google families a text names are requested`() {
    val nodes =
      listOf(
        text("a", "google:Michroma"),
        text("b", " google:Inter "),
        text("c", "serif"),
        text("d", null),
        text("e", "google:"),
        text("f", "google:Michroma"),
      )
    assertEquals(setOf("google:Michroma", "google:Inter"), nodes.textGoogleFamilies())
  }
}
