package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionSnapshot
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderPixelBounds
import ee.schimke.composeai.uibuilder.renderer.sdk.bottom
import ee.schimke.composeai.uibuilder.renderer.sdk.right
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * The Visual Editor's surface on real Remote Compose (live variant only): every authored node of
 * the golden designs gets bounds from the player, nested as the design nests them.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3AuthoringSurfaceUiTest {

  @Test
  fun `every authored node of the golden designs is measured by the player`() {
    val surface = remoteAuthoringSurface ?: return // capture variant: no remote authoring surface
    listOf("golden-tiles-news", "golden-tiles-timer-1", "golden-tiles-goal").forEach { id ->
      val design = fixture(id)
      runDesktopComposeUiTest(width = 480, height = 320) {
        var last: UiBuilderInspectionSnapshot? = null
        setContent { surface(design, 240f, 240f) { last = it } }
        try {
          waitUntil(timeoutMillis = 10_000) {
            last?.generation?.measuredNodeIds?.size == design.nodes.size
          }
        } catch (e: Throwable) {
          val measured = last?.generation?.measuredNodeIds.orEmpty()
          throw AssertionError(
            "$id: unmeasured " +
              design.nodes.values
                .filter { it.id !in measured }
                .map { "${it.componentId} ${it.id}" },
            e,
          )
        }

        val snapshot = requireNotNull(last)
        val bounds =
          snapshot.nodes.associate { it.nodeId to requireNotNull(it.bounds) { it.nodeId } }
        design.nodes.values.forEach { parent ->
          parent.slots.values.flatten().forEach { childId ->
            assertTrue(
              bounds.getValue(parent.id).contains(bounds.getValue(childId)),
              "$id: ${design.nodes.getValue(childId).componentId} $childId " +
                "${bounds.getValue(childId)} outside ${parent.componentId} ${bounds.getValue(parent.id)}",
            )
          }
        }
      }
    }
  }

  @Test
  fun `a new revision is measured again to the same geometry`() {
    val surface = remoteAuthoringSurface ?: return
    val design = fixture("golden-tiles-timer-1")
    runDesktopComposeUiTest(width = 480, height = 320) {
      var current by mutableStateOf(design)
      var last: UiBuilderInspectionSnapshot? = null
      setContent { surface(current, 240f, 240f) { last = it } }
      waitUntil(timeoutMillis = 10_000) {
        last?.generation?.measuredNodeIds?.size == design.nodes.size
      }
      val before = last!!.nodes.associate { it.nodeId to it.bounds }
      val text = design.nodes.values.first { it.componentId == "remote-m3/remote-text" }

      current = design.copy(revision = design.revision + 1)
      // Same design, new revision: a fresh generation, measured again, the same geometry.
      waitUntil(timeoutMillis = 10_000) {
        last?.documentRevision == design.revision + 1 &&
          last?.generation?.measuredNodeIds?.size == design.nodes.size
      }
      assertEquals(before, last!!.nodes.associate { it.nodeId to it.bounds })
      assertNotEquals(null, before[text.id])
    }
  }

  private fun UiBuilderPixelBounds.contains(other: UiBuilderPixelBounds): Boolean {
    val slack = 1f
    return other.x >= x - slack &&
      other.y >= y - slack &&
      other.right <= right + slack &&
      other.bottom <= bottom + slack
  }

  private val lenient = Json { ignoreUnknownKeys = true }

  private fun fixture(id: String): UiBuilderDocument =
    lenient.decodeFromString(
      requireNotNull(javaClass.getResource("/remote-m3/$id.json")).readText()
    )
}
