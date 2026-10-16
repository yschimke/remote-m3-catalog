package ee.schimke.wearm3catalog.remoteuibuilder

import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PreviewEngineTest {
  private val design: UiBuilderDocument =
    Json.decodeFromString(
      """
      {"schema": "compose-ui-builder-document/v1-candidate", "id": "engine", "title": "Engine",
       "revision": 1, "catalogPin": {"systemId": "remote-m3"}, "environment": {},
       "roots": [], "nodes": {}}
      """
    )

  private fun asking(value: String) =
    design.copy(
      environment = JsonObject(mapOf(PreviewEngine.ENVIRONMENT_KEY to JsonPrimitive(value)))
    )

  @Test
  fun `the editor picks the engine through the document environment`() {
    assertEquals(PreviewEngine.Live, PreviewEngine.of(asking("live")))
    assertEquals(PreviewEngine.Capture, PreviewEngine.of(asking("capture")))
  }

  @Test
  fun `a document that names no known engine gets the default`() {
    assertEquals(defaultPreviewEngine, PreviewEngine.of(design))
    assertEquals(defaultPreviewEngine, PreviewEngine.of(asking("fast")))
  }
}
