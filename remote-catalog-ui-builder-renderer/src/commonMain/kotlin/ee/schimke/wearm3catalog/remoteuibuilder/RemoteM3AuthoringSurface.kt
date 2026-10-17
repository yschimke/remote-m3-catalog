// The Visual Editor's surface on real Remote Compose, played live.
package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import ee.schimke.composeai.rccreation.bridge.RcComponentIds
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiBuilderInstancePath
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionCollector
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionSnapshot
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderPixelBounds
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderSemanticActionController
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderSemanticActionEntry
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderUndrawnNode
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderUndrawnNodes
import ee.schimke.composeai.uibuilder.renderer.sdk.undrawnGenerationKey
import kotlinx.coroutines.flow.drop

/**
 * The Visual Editor's surface: the design recorded and played as the device would, with every
 * authored node tagged with a component id, so what the player reports becomes the editor's
 * inspection.
 * - **Bounds** are the component's box in the root, from all four of its corners, so a scaled or
 *   rotated node keeps a true axis-aligned box. Selection, hit-testing, drop planning and the
 *   editor's empty-slot placeholders read them unchanged.
 * - **Text layout**: each remote text's line count and baselines, in its own bounds.
 * - **Activate**: a node with a click binding can be activated through [actions], which runs the
 *   click the player holds for it, as a tap would.
 * - **Undrawn**: a component the tree can only draw as a diagnostic is reported for the generation
 *   (`UiBuilderUndrawnNodes`), so the editor notes it and the coverage gate fails on it.
 *
 * Scroll modifiers are dropped: a scrolling container plays as a plain one, and a popped-out
 * horizontal one widens with the editor's budget until it fits.
 *
 * Not reported: the inspection's `selected` follows the design's initial state, since the player
 * keeps the live state; and `scrollBy` and `revealNode`, which the editor sends only to the device
 * view.
 */
@Composable
internal fun RemoteM3AuthoringSurface(
  document: UiBuilderDocument,
  widthDp: Float,
  heightDp: Float,
  renderSessionId: String = "",
  actions: UiBuilderSemanticActionController? = null,
  onInspectionSnapshot: (UiBuilderInspectionSnapshot) -> Unit,
) {
  val currentOnSnapshot by rememberUpdatedState(onInspectionSnapshot)
  // One registry for the surface's life: a node keeps its component id across edits.
  val ids = remember { RcComponentIds<UiBuilderInstancePath>() }
  // Every report invalidates; snapshots are published once per settled batch.
  val invalidations = remember(document) { mutableIntStateOf(0) }
  val collector =
    remember(document, renderSessionId) {
      UiBuilderInspectionCollector(
        document,
        onInvalidated = { invalidations.intValue++ },
        canvasAdapterIds = TEXT_ADAPTERS,
      )
    }
  val viewport =
    with(LocalDensity.current) {
      UiBuilderPixelBounds(0f, 0f, widthDp * density, heightDp * density)
    }
  val inspection =
    remember(collector, actions, viewport) {
      LiveInspection(document, collector, ids, actions, viewport)
    }
  // Nothing until the player has reported: the runtime answers the editor's render with the first
  // snapshot that holds still, and an empty one sent before the player has laid anything out would
  // be that answer — a render that "measured no node".
  LaunchedEffect(collector) {
    snapshotFlow { invalidations.intValue }
      .drop(1)
      .collect { currentOnSnapshot(collector.snapshot()) }
  }
  RemoteM3DevicePreview(
    document = document,
    widthDp = widthDp,
    heightDp = heightDp,
    authoring = inspection.authoring,
    onReady = { collector.publishSnapshot() },
  )
}

/** The player's reports for one generation, turned into the collector's and the actions'. */
private class LiveInspection(
  private val document: UiBuilderDocument,
  private val collector: UiBuilderInspectionCollector,
  ids: RcComponentIds<UiBuilderInstancePath>,
  private val actions: UiBuilderSemanticActionController?,
  private val viewport: UiBuilderPixelBounds,
) {
  private val undrawnKey = undrawnGenerationKey(document.id, document.revision)

  /** Each measured node's top, in the root: a text's baselines are recorded against it. */
  private val tops = mutableMapOf<String, Float>()

  /** Each laid-out text's line count and first and last baselines, in the root. */
  private val texts = mutableMapOf<String, Triple<Int, Float, Float>>()

  /**
   * Every click the player holds for each node. It can compose a node twice for a moment, as a new
   * document replaces the old, so the most recent one still held is the node's click.
   */
  private val clicks = mutableMapOf<String, MutableList<() -> Unit>>()

  val authoring =
    AuthoringInspection(
      ids,
      onBounds = { nodeId, coordinates ->
        if (nodeId !in document.nodes) return@AuthoringInspection
        // All four corners: a rotated node's box is not its diagonal's. Mapped one by one, since
        // `localBoundingBoxOf` answers an empty box (a zero-size gap) with the root's origin.
        val width = coordinates.size.width.toFloat()
        val height = coordinates.size.height.toFloat()
        val corners =
          listOf(Offset.Zero, Offset(width, 0f), Offset(0f, height), Offset(width, height)).map {
            coordinates.localToRoot(it)
          }
        val top = corners.minOf { it.y }
        tops[nodeId] = top
        collector.recordNodeBounds(
          nodeId,
          corners.minOf { it.x },
          top,
          corners.maxOf { it.x },
          corners.maxOf { it.y },
        )
        recordText(nodeId)
      },
      onText = { nodeId, content, layout ->
        if (document.nodes[nodeId]?.componentId !in TEXT_ADAPTERS) return@AuthoringInspection
        val first = content.localToRoot(Offset(0f, layout.firstBaseline)).y
        val last = content.localToRoot(Offset(0f, layout.lastBaseline)).y
        texts[nodeId] = Triple(layout.lineCount, first, last)
        recordText(nodeId)
      },
      onClick = { nodeId, click, added ->
        if (nodeId !in document.nodes) return@AuthoringInspection
        val held = clicks.getOrPut(nodeId) { mutableListOf() }
        if (added) held += click else held -= click
        if (held.isEmpty()) clicks -= nodeId
        installActions()
      },
      onUndrawn = { node, drawn ->
        if (drawn) UiBuilderUndrawnNodes.forget(undrawnKey, node.id)
        else
          UiBuilderUndrawnNodes.record(
            undrawnKey,
            UiBuilderUndrawnNode(node.id, node.componentId, UiBuilderUndrawnNode.UNSUPPORTED),
          )
      },
    )

  init {
    // The previous generation's clicks are gone with its player; until this one reports, there
    // are none to run.
    installActions()
  }

  private fun recordText(nodeId: String) {
    val top = tops[nodeId] ?: return
    val (lines, first, last) = texts[nodeId] ?: return
    collector.recordTextLayout(nodeId, lines, first - top, last - top)
  }

  private fun installActions() {
    actions?.install(
      clicks.mapValues { (_, held) -> UiBuilderSemanticActionEntry(activate = held.last()) },
      viewport,
    )
  }
}

/**
 * The remote components the tree draws as one remote text, by a text adapter id the inspection
 * knows: what makes a node a text node to it.
 */
private val TEXT_ADAPTERS = mapOf("remote-m3/remote-text" to "wear-m3/text")
