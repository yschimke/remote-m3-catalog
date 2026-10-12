package ee.schimke.wearm3catalog.remote.typed

import ee.schimke.catalog.typed.remoteM3Policy
import ee.schimke.composeai.discovery.UiBuilderPolicyFile
import ee.schimke.composeai.uibuilder.export.CatalogExportProjections
import ee.schimke.composeai.uibuilder.export.CatalogScreenStructure
import ee.schimke.composeai.uibuilder.export.ScreenExportGate
import ee.schimke.composeai.uibuilder.export.WearWidgetCodeExporter
import ee.schimke.composeai.uibuilder.export.toUiBuilderDocument
import ee.schimke.composeai.uibuilder.protocol.DesignDocumentV1
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * The pilot exported end to end the way the editor would export it: the pilot policy's projections
 * and `code` block read back by compose-ui-builder 3.109.0's own readers, and every committed
 * remote design through `ScreenExportGate` — beside production's `WearWidgetCodeExporter`, which
 * writes all five.
 *
 * Every committed design is refused, and each refusal is pinned exactly: they are the port work
 * left, and each names the tools or builder API it waits on in docs/design/TYPED_REMOTE_PILOT.md. A
 * gap that closes changes a list here, which is the point.
 */
class RealRemoteExportTest {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val real = RealRemotePublication()

  private val policy = TypedRemotePilot.policy(remoteM3Policy(), real)

  /** The policy in the shape both readers accept: a `statusSemantics` block or a policy file. */
  private val semantics =
    json.encodeToJsonElement(UiBuilderPolicyFile.serializer(), policy).jsonObject

  /** The real records, each joined to the component id a design names it by. */
  private val record = TypedRemotePilot.record(null, real)

  private val root =
    generateSequence(File(".").absoluteFile) { it.parentFile }
      .first { File(it, "remote-catalog/ui-builder/designs").isDirectory }

  private fun design(name: String): DesignDocumentV1 =
    json.decodeFromString(File(root, "remote-catalog/ui-builder/designs/$name").readText())

  private fun export(document: DesignDocumentV1): ScreenExportGate.Outcome =
    ScreenExportGate.export(
      document,
      record,
      CatalogExportProjections.from(semantics),
      CatalogScreenStructure.from(semantics),
    )

  private fun refused(name: String): List<String> =
    assertIs<ScreenExportGate.Outcome.Refused>(export(design(name)), name).reasons

  private fun notWritten(node: String, vararg properties: String, by: String = TEXT) =
    properties.map {
      "node `$node`.`$it` is not written by `$by`'s export projection"
    }

  @Test
  fun `the pilot selects the generic export and publishes four projections`() {
    assertEquals(TYPED_REMOTE_CATALOG_ID, policy.catalogId)
    assertEquals(TypedRemotePilot.COMPONENT_RECORDS, policy.composeSourceExport?.adapter)
    assertEquals(1, policy.composeSourceExport?.version)
    assertEquals(
      setOf("remote-m3/remote-text", "remote-m3/remote-button", "layout/box", "layout/column"),
      CatalogExportProjections.from(semantics).keys,
    )
  }

  /**
   * The frame is never reached: the builder reads ONE `screen-root` builtin as the root a frame is
   * written around, and remote-m3 has three — one per widget container size.
   */
  @Test
  fun `three screen-root containers leave the frame without a root`() {
    assertEquals(
      setOf(
        "remote-m3/widget-container-small",
        "remote-m3/widget-container-large",
        "remote-m3/widget-container-adaptive",
      ),
      policy.builtins.filterValues { it.role == "screen-root" }.keys,
    )
    val structure = CatalogScreenStructure.from(semantics)
    assertEquals("\${content}", structure.frame)
    assertEquals(null, structure.rootComponent)
  }

  @Test
  fun `production's hand emitter writes every committed design`() {
    for (name in DESIGNS) {
      val source =
        assertIs<WearWidgetCodeExporter.Result.Emitted>(
            WearWidgetCodeExporter.export(design(name).toUiBuilderDocument()),
            name,
          )
          .source
      assertContains(source, ": GlanceWearWidget() {")
      assertContains(source, "@RemoteComposable")
    }
  }

  @Test
  fun `hello is refused for its Remote values, its alignment and its container`() {
    assertEquals(
      notWritten("hello-text", "text", "color", "fontSize") +
        ("node `hello-text` aligns itself, which `Modifier.align` supplies from a box's scope; " +
          "this node sits at the root, which has no receiver") +
        "no component `remote-m3/widget-container-small` in this catalog",
      refused("hello-widget.json"),
    )
  }

  @Test
  fun `weather is refused for its column's spacing and alignment, its Remote values and its container`() {
    assertEquals(
      notWritten("weather-column", "verticalSpacingDp", "alignment", by = "layout/column") +
        notWritten("weather-location", "text", "color", "fontSize") +
        notWritten("weather-reading", "text", "color", "fontSize") +
        "no component `remote-m3/widget-container-large` in this catalog",
      refused("weather-widget.json"),
    )
  }

  @Test
  fun `the adaptive widget is refused for its Remote values and its container`() {
    assertEquals(
      notWritten("adaptive-headline", "text", "color") +
        notWritten("adaptive-supporting", "text", "color") +
        notWritten("adaptive-action-label", "text") +
        "no component `remote-m3/widget-container-adaptive` in this catalog",
      refused("wear-widget-adaptive.json"),
    )
  }

  /** The starters: an empty box filling the container, refused for `Modifier` and the container. */
  @Test
  fun `the empty starters are refused for Compose's Modifier and their container`() {
    for ((name, size) in
      listOf("wear-widget-small.json" to "small", "wear-widget-large.json" to "large")) {
      assertEquals(
        listOf(
          "no component `remote-m3/widget-container-$size` in this catalog",
          MODIFIER_REFUSAL.format("RemoteBox"),
        ),
        refused(name),
        name,
      )
    }
  }

  /**
   * What does lower: the layouts onto the real `RemoteBox` / `RemoteColumn`, and a box's own
   * `contentAlignment` onto `RemoteAlignment`, written as the hand emitter writes them.
   */
  @Test
  fun `a box and column with nothing Remote-valued export onto the real layouts`() {
    val source = assertIs<ScreenExportGate.Outcome.Emitted>(export(probe(modifiers = "[]"))).source
    assertContains(source, "import androidx.compose.remote.creation.compose.layout.RemoteBox\n")
    assertContains(source, "import androidx.compose.remote.creation.compose.layout.RemoteColumn\n")
    assertContains(source, "RemoteBox(contentAlignment = RemoteAlignment.Center) {")
    assertContains(source, "RemoteColumn {")
  }

  /** The same box, given the `fillMaxSize` every committed design gives it. */
  @Test
  fun `an authored modifier is lowered as Compose's Modifier and refused`() {
    assertEquals(
      listOf(MODIFIER_REFUSAL.format("RemoteBox")),
      assertIs<ScreenExportGate.Outcome.Refused>(
          export(probe(modifiers = """[{ "type": "fillMaxSize" }]"""))
        )
        .reasons,
    )
  }

  /** A design rooted on a box (no container), its column empty. */
  private fun probe(modifiers: String): DesignDocumentV1 =
    json.decodeFromString(
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "probe",
        "title": "Probe",
        "revision": 0,
        "catalogPin": { "systemId": "$TYPED_REMOTE_CATALOG_ID", "catalogRevision": "candidate",
          "capabilityDigest": "candidate", "nativeRuntimeId": "candidate" },
        "environment": { "widthDp": 216, "heightDp": 124, "density": 2.0, "theme": "dark",
          "dynamicColor": false, "locale": "en-US", "fontScale": 1, "layoutDirection": "ltr",
          "windowPosture": "flat", "fixedTime": "2024-05-16T12:00:00Z", "animations": "settled",
          "networkAccess": false },
        "stateVariables": {},
        "roots": ["box"],
        "nodes": {
          "box": { "id": "box", "componentId": "layout/box",
            "properties": { "contentAlignment": { "type": "enum", "value": "center" } },
            "modifiers": $modifiers, "slots": { "children": ["column"] } },
          "column": { "id": "column", "componentId": "layout/column", "properties": {},
            "modifiers": [], "slots": { "children": [] } }
        }
      }
      """
    )

  private companion object {
    const val TEXT = "remote-m3/remote-text"

    val DESIGNS =
      listOf(
        "hello-widget.json",
        "weather-widget.json",
        "wear-widget-adaptive.json",
        "wear-widget-small.json",
        "wear-widget-large.json",
      )

    const val MODIFIER_REFUSAL =
      "`%s`.`modifier` is androidx.compose.remote.creation.compose.modifier.RemoteModifier, and " +
        "this value is a androidx.compose.ui.Modifier"
  }
}
