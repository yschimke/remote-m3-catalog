package ee.schimke.wearm3catalog.remote.typed

import ee.schimke.composeai.discovery.ComponentRecord
import ee.schimke.composeai.discovery.ComponentRecordFile
import ee.schimke.composeai.discovery.ExportProjectionJson
import ee.schimke.composeai.discovery.TypedComponentAdapter
import ee.schimke.composeai.discovery.UiBuilderAuthoredComponent
import ee.schimke.composeai.discovery.UiBuilderCode
import ee.schimke.composeai.discovery.UiBuilderComposeSourceExport
import ee.schimke.composeai.discovery.UiBuilderPolicyFile

/** The pilot's catalog id. Production keeps `remote-m3` and its committed JSON policy. */
const val TYPED_REMOTE_CATALOG_ID: String = "remote-m3-typed-pilot"

/**
 * The real-record definitions the pilot exports through, built on what discovery read. Component
 * ids are production's, so the committed designs address them unchanged.
 */
class RealRemotePublication(
  val records: Map<String, ComponentRecord> = RealRemoteRecords.records()
) {
  val text = RealRemoteTextAdapter(records.getValue("RemoteText"))
  val button = RealRemoteButtonAdapter(records.getValue("RemoteButton"))
  val box = RealRemoteBoxAdapter(records.getValue("RemoteBox"))
  val column = RealRemoteColumnAdapter(records.getValue("RemoteColumn"))

  /** Shelf components: their production policy entry gains the projection. */
  val shelf: List<TypedComponentAdapter<*>> = listOf(text, button)

  /**
   * The builder's shared `layout/…` components, which production writes as `RemoteBox` /
   * `RemoteColumn` and a host would otherwise join to Compose's `Box` / `Column`.
   */
  val layouts: List<TypedComponentAdapter<*>> = listOf(box, column)

  val adapters: List<TypedComponentAdapter<*>>
    get() = shelf + layouts
}

/**
 * The pilot pair's policy and record: production's authored policy under [TYPED_REMOTE_CATALOG_ID],
 * each shelf component a real definition covers carrying that definition's projection, the real
 * records (and the layouts') published as export targets kept off the shelf, the `code` block the
 * frame is stated in, and `compose-component-records` selected as the Kotlin export.
 */
object TypedRemotePilot {
  /** compose-ui-builder's catalog-neutral component-record export adapter. */
  const val COMPONENT_RECORDS: String = "compose-component-records"
  const val COMPONENT_RECORDS_V1: Int = 1

  /** Policy ids of the real records published only as export targets. */
  const val EXPORT_TARGET_PREFIX: String = "remote-m3/export-"

  fun policy(base: UiBuilderPolicyFile, real: RealRemotePublication): UiBuilderPolicyFile {
    val byId = real.shelf.associateBy { it.id }
    val components =
      base.components.mapValues { (id, component) ->
        val adapter = byId[id] ?: return@mapValues component
        component.newBuilder().also { it.exportProjection = projectionJson(adapter) }.build()
      } +
        // The real records a shelf component exports to, kept off the shelf.
        real.shelf.associate { adapter ->
          "$EXPORT_TARGET_PREFIX${adapter.component.symbol.name.lowercase()}" to
            UiBuilderAuthoredComponent.Builder()
              .also {
                it.record = adapter.component.canonicalId
                it.excluded = "an export target of ${adapter.id}"
              }
              .build()
        } +
        // The builder's shared layouts, keyed by their own `layout/…` ids so the generic export
        // lowers them through these projections (onto `RemoteBox` / `RemoteColumn`) instead of
        // its built-in Compose `Box` / `Column` lowering. Off the shelf: the editor keeps placing
        // its own foundation components.
        real.layouts.associate { adapter ->
          adapter.id to
            UiBuilderAuthoredComponent.Builder()
              .also {
                it.record = adapter.component.canonicalId
                it.excluded = "the builder's shared ${adapter.id}, exported as Remote Compose's"
                it.exportProjection = projectionJson(adapter)
              }
              .build()
        }
    return base
      .newBuilder()
      .apply {
        catalogId = TYPED_REMOTE_CATALOG_ID
        this.components = components
        code = REMOTE_WIDGET_CODE
        composeSourceExport =
          UiBuilderComposeSourceExport.Builder(COMPONENT_RECORDS, COMPONENT_RECORDS_V1).build()
      }
      .build()
  }

  /**
   * [discovered] (production's discovery of `:remote-catalog`, or empty) with the real export
   * targets beside it, each carrying the component ids a design names it by.
   */
  fun record(discovered: ComponentRecordFile?, real: RealRemotePublication): ComponentRecordFile {
    val targets =
      real.adapters.map { adapter ->
        adapter.component
          .newBuilder()
          .also { it.componentIds = (adapter.component.componentIds + adapter.id).distinct() }
          .build()
      }
    val base = discovered?.components.orEmpty()
    return ComponentRecordFile.Builder(
        discovered?.module ?: RealRemoteRecords.MODULE,
        discovered?.variant ?: "release",
        base + targets.filter { t -> base.none { it.canonicalId == t.canonicalId } },
      )
      .build()
  }

  private fun projectionJson(adapter: TypedComponentAdapter<*>) =
    ExportProjectionJson.encode(
      requireNotNull(adapter.projection) { "${adapter.id}: no projection" }
    )

  /**
   * The frame around a widget's content. compose-ui-builder 3.109.0 writes the generated root
   * inside `@Composable fun <Name>() { … }`, so the frame can hold only the content's own
   * structure: the `GlanceWearWidget` class, its `WearWidgetDocument(background = …)` and the
   * per-host previews the hand exporter writes around that function have nowhere to go. See the
   * design doc's "The widget wrapper".
   */
  val REMOTE_WIDGET_CODE: UiBuilderCode =
    UiBuilderCode.Builder()
      .also {
        it.strategy = "templates"
        it.templates = mapOf("screen-root" to "${'$'}{content}")
      }
      .build()
}
