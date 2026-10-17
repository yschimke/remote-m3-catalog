package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.renderer.sdk.CatalogRuntimeAction
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionSnapshot
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderPixelBounds
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderSemanticActionController
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderSemanticActionResult
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderUndrawnNode
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderUndrawnNodes
import ee.schimke.composeai.uibuilder.renderer.sdk.bottom
import ee.schimke.composeai.uibuilder.renderer.sdk.right
import ee.schimke.composeai.uibuilder.renderer.sdk.undrawnGenerationKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * The Visual Editor's surface on real Remote Compose: every authored node of the golden designs
 * gets bounds from the player, nested as the design nests them.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3AuthoringSurfaceUiTest {

  @Test
  fun `every authored node of the golden designs is measured by the player`() {
    listOf("golden-tiles-news", "golden-tiles-timer-1", "golden-tiles-goal").forEach { id ->
      val design = fixture(id)
      runDesktopComposeUiTest(width = 480, height = 320) {
        var last: UiBuilderInspectionSnapshot? = null
        setContent { RemoteM3AuthoringSurface(design, 240f, 240f) { last = it } }
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
    val design = fixture("golden-tiles-timer-1")
    runDesktopComposeUiTest(width = 480, height = 320) {
      var current by mutableStateOf(design)
      var last: UiBuilderInspectionSnapshot? = null
      setContent { RemoteM3AuthoringSurface(current, 240f, 240f) { last = it } }
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

  @Test
  fun `every remote text reports its lines and baselines inside its bounds`() {
    val design = fixture("golden-tiles-timer-1")
    runDesktopComposeUiTest(width = 480, height = 320) {
      var last: UiBuilderInspectionSnapshot? = null
      setContent { RemoteM3AuthoringSurface(design, 240f, 240f) { last = it } }
      val texts = design.nodes.values.filter { it.componentId == "remote-m3/remote-text" }
      assertTrue(texts.isNotEmpty(), "the fixture has no remote text")
      waitUntil(timeoutMillis = 10_000) {
        val generation = last?.generation ?: return@waitUntil false
        generation.measuredTextNodeIds.toSet() == texts.map { it.id }.toSet()
      }
      val snapshot = requireNotNull(last)
      assertEquals(texts.map { it.id }.sorted(), snapshot.generation.expectedAuthoredTextNodeIds)
      snapshot.nodes
        .filter { it.nodeId in snapshot.generation.measuredTextNodeIds }
        .forEach { node ->
          val bounds = requireNotNull(node.bounds)
          val text = requireNotNull(node.text) { node.nodeId }
          assertTrue(text.lineCount >= 1, "${node.nodeId}: ${text.lineCount} lines")
          assertTrue(
            text.firstBaselineY > bounds.y && text.lastBaselineY <= bounds.bottom + 1f,
            "${node.nodeId}: baselines ${text.firstBaselineY}..${text.lastBaselineY} in $bounds",
          )
        }
    }
  }

  @Test
  fun `activating a node runs its click in the player`() {
    val design = interactive()
    val actions = UiBuilderSemanticActionController()
    runDesktopComposeUiTest(width = 480, height = 320) {
      var last: UiBuilderInspectionSnapshot? = null
      setContent { RemoteM3AuthoringSurface(design, 240f, 240f, actions = actions) { last = it } }
      waitUntil(timeoutMillis = 10_000) { last?.bounds("mode") != null }
      val before = requireNotNull(last!!.bounds("mode"))

      // The editor's message, as the runtime endpoint hands it on.
      fun activate(nodeId: String) =
        actions.dispatch(
          CatalogRuntimeAction(design.id, design.revision, nodeId, "activate"),
          last,
        )
      waitUntil(timeoutMillis = 10_000) {
        activate("toggle") is UiBuilderSemanticActionResult.Applied
      }
      // The click flipped `expanded`, so the switch shows its other, taller branch.
      waitUntil(timeoutMillis = 10_000) {
        last?.bounds("mode")?.let { it.height > before.height } == true
      }
      // A node with no click has nothing to run.
      assertTrue(activate("mode") is UiBuilderSemanticActionResult.Rejected)
    }
  }

  @Test
  fun `a component the tree cannot draw is reported undrawn for its generation`() {
    val design = interactive()
    val key = undrawnGenerationKey(design.id, design.revision)
    runDesktopComposeUiTest(width = 480, height = 320) {
      var last: UiBuilderInspectionSnapshot? = null
      var shown by mutableStateOf(true)
      setContent { if (shown) RemoteM3AuthoringSurface(design, 240f, 240f) { last = it } }
      waitUntil(timeoutMillis = 10_000) { last?.bounds("ghost") != null }
      assertEquals(
        listOf(UiBuilderUndrawnNode("ghost", "nowhere/mystery", UiBuilderUndrawnNode.UNSUPPORTED)),
        UiBuilderUndrawnNodes.forGeneration(key),
      )
      // Gone with the surface that drew it.
      shown = false
      waitUntil(timeoutMillis = 10_000) { UiBuilderUndrawnNodes.forGeneration(key).isEmpty() }
    }
  }

  @Test
  fun `an empty container keeps its own box, and a scrolling one plays as a plain one`() {
    val design = interactive()
    runDesktopComposeUiTest(width = 480, height = 320) {
      var last: UiBuilderInspectionSnapshot? = null
      setContent { RemoteM3AuthoringSurface(design, 240f, 240f) { last = it } }
      waitUntil(timeoutMillis = 10_000) {
        last?.generation?.measuredNodeIds?.containsAll(listOf("empty", "list", "item2")) == true
      }
      val snapshot = requireNotNull(last)
      // What the editor places an empty slot's drop placeholder in.
      val empty = requireNotNull(snapshot.bounds("empty"))
      assertTrue(empty.width > 0f && empty.height == 40f, "empty column $empty")
      assertEquals(null, snapshot.slots.single { it.parentNodeId == "empty" }.bounds)
      // The scroll is dropped on the authoring surface: every item is measured where a plain
      // column puts it, in order, rather than the preview failing on the modifier.
      val first = requireNotNull(snapshot.bounds("item0"))
      val third = requireNotNull(snapshot.bounds("item2"))
      assertTrue(third.y >= first.bottom, "item2 $third after item0 $first")
    }
  }

  private fun UiBuilderInspectionSnapshot.bounds(nodeId: String): UiBuilderPixelBounds? =
    nodes.singleOrNull { it.nodeId == nodeId }?.bounds

  /**
   * A small widget with a button that flips a show-by-state switch between a short and a tall
   * branch, a component no canvas knows, an empty column and a scrolling one.
   */
  private fun interactive(): UiBuilderDocument {
    fun text(id: String, value: String, size: Int) =
      """
      "$id": {
        "id": "$id", "componentId": "remote-m3/remote-text",
        "properties": {
          "text": { "type": "string", "value": "$value" },
          "fontSize": { "type": "float", "value": $size }
        },
        "modifiers": [], "slots": {}
      }
      """
    return lenient.decodeFromString(
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "remote-m3-authoring-interactive",
        "title": "Remote M3 authoring",
        "revision": 1,
        "catalogPin": {
          "systemId": "remote-m3", "catalogRevision": "test", "capabilityDigest": "test",
          "nativeRuntimeId": "remote-m3-test-runtime"
        },
        "environment": {
          "widthDp": 240, "heightDp": 240, "density": 1, "theme": "dark", "fontScale": 1,
          "layoutDirection": "ltr"
        },
        "stateVariables": { "expanded": { "initialValue": false, "valueType": "bool" } },
        "roots": ["root"],
        "nodes": {
          "root": {
            "id": "root", "componentId": "remote-m3/widget-container-large",
            "properties": {}, "modifiers": [], "slots": { "background": [], "content": ["column"] }
          },
          "column": {
            "id": "column", "componentId": "layout/column", "properties": {},
            "modifiers": [{ "type": "fillMaxSize" }],
            "slots": { "children": ["toggle", "mode", "ghost", "empty", "list"] }
          },
          "toggle": {
            "id": "toggle", "componentId": "remote-m3/remote-button", "properties": {},
            "modifiers": [{ "type": "height", "heightDp": 24 }],
            "slots": { "content": ["label"] },
            "eventBindings": { "click": [{ "type": "toggle", "variable": "expanded" }] }
          },
          ${text("label", "Toggle", 10)},
          "mode": {
            "id": "mode", "componentId": "layout/box",
            "properties": {
              "showByState": { "type": "object", "fields": {
                "selector": { "type": "state", "variable": "expanded" },
                "cases": { "type": "object", "fields": {
                  "short": { "type": "bool", "value": false },
                  "tall": { "type": "bool", "value": true }
                } }
              } }
            },
            "modifiers": [], "slots": { "children": ["short", "tall"] }
          },
          ${text("short", "Short", 8)},
          ${text("tall", "Tall", 24)},
          "ghost": {
            "id": "ghost", "componentId": "nowhere/mystery", "properties": {},
            "modifiers": [], "slots": {}
          },
          "empty": {
            "id": "empty", "componentId": "layout/column", "properties": {},
            "modifiers": [{ "type": "fillMaxWidth" }, { "type": "height", "heightDp": 40 }],
            "slots": { "children": [] }
          },
          "list": {
            "id": "list", "componentId": "layout/column", "properties": {},
            "modifiers": [{ "type": "height", "heightDp": 30 }, { "type": "verticalScroll" }],
            "slots": { "children": ["item0", "item1", "item2"] }
          },
          ${text("item0", "First", 12)},
          ${text("item1", "Second", 12)},
          ${text("item2", "Third", 12)}
        }
      }
      """
    )
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
