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
import ee.schimke.composeai.rccreation.bridge.RcComponentIds
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionCollector
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionSnapshot

/**
 * The authoring surface: the design recorded and played as the device would, with every authored
 * node tagged with a component id, so the player's geometry becomes the editor's per-node bounds.
 *
 * Bounds are the component's box in the root, from all four of its corners, so a scaled or rotated
 * node keeps a true axis-aligned box. Selection, hit-testing and drop planning on the editor side
 * read them unchanged.
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
        // All four corners: a rotated node's box is not its diagonal's. Mapped one by one, since
        // `localBoundingBoxOf` answers an empty box (a zero-size gap) with the root's origin.
        val width = coordinates.size.width.toFloat()
        val height = coordinates.size.height.toFloat()
        val corners =
          listOf(Offset.Zero, Offset(width, 0f), Offset(0f, height), Offset(width, height)).map {
            coordinates.localToRoot(it)
          }
        collector.recordNodeBounds(
          nodeId,
          corners.minOf { it.x },
          corners.minOf { it.y },
          corners.maxOf { it.x },
          corners.maxOf { it.y },
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
