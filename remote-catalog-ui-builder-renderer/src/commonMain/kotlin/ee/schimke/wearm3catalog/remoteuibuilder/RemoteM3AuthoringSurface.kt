// The Visual Editor's surface on real Remote Compose, played live, instead of Compose UI stand-ins.
// Used only under `PreviewEngine.Live`; see PreviewEngine.kt.
package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import ee.schimke.composeai.rccreation.bridge.RcComponentIds
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionCollector
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionSnapshot

/**
 * The authoring surface: the design recorded and played as the device would, with every authored
 * node tagged with a component id, so the player's geometry becomes the editor's per-node bounds.
 *
 * Bounds are taken as `CanvasDocumentHost` takes a stand-in's: the component's position in the root
 * and its size scaled to it. Selection, hit-testing and drop planning on the editor side read them
 * unchanged.
 *
 * Not yet carried over from the stand-in surface: text layout records (baselines), semantic actions
 * (`activate`, `scrollBy`), horizontal unrolling, and placeholders for empty slots.
 */
@Composable
internal fun RemoteM3AuthoringSurface(
  document: UiBuilderDocument,
  widthDp: Float,
  heightDp: Float,
  onInspectionSnapshot: (UiBuilderInspectionSnapshot) -> Unit,
) {
  val currentOnSnapshot by rememberUpdatedState(onInspectionSnapshot)
  // One registry for the surface's life: a node keeps its component id across edits.
  val ids = remember { RcComponentIds<String>() }
  // Every bounds report invalidates; snapshots are published once per settled batch.
  val invalidations = remember(document) { mutableIntStateOf(0) }
  val collector =
    remember(document) {
      UiBuilderInspectionCollector(document, onInvalidated = { invalidations.intValue++ })
    }
  val inspection =
    remember(collector) {
      AuthoringInspection(ids) { nodeId, coordinates ->
        if (nodeId !in document.nodes) return@AuthoringInspection
        val unit = coordinates.localToRoot(Offset(1f, 1f)) - coordinates.localToRoot(Offset.Zero)
        val origin = coordinates.positionInRoot()
        collector.recordNodeBounds(
          nodeId,
          origin.x,
          origin.y,
          origin.x + coordinates.size.width * unit.x,
          origin.y + coordinates.size.height * unit.y,
        )
      }
    }
  LaunchedEffect(collector) {
    snapshotFlow { invalidations.intValue }.collect { currentOnSnapshot(collector.snapshot()) }
  }
  LiveDevicePreview(
    document = document,
    widthDp = widthDp,
    heightDp = heightDp,
    authoring = inspection,
    onReady = { collector.publishSnapshot() },
  )
}
