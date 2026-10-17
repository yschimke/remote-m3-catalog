package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontFamilies
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontVariants
import ee.schimke.composeai.uibuilder.protocol.UiBuilderRendererSurfaceModeV2
import ee.schimke.composeai.uibuilder.renderer.sdk.CATALOG_RUNTIME_CAPABILITY_HORIZONTAL_UNROLL
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderInspectionCollector
import ee.schimke.composeai.uibuilder.renderer.sdk.UiBuilderSemanticActionController
import ee.schimke.composeai.uibuilder.renderer.sdk.catalogRuntimeFontRegistry
import ee.schimke.composeai.uibuilder.renderer.sdk.startCatalogRenderer
import ee.schimke.wearcmp.port.LocalWearDeviceConfiguration
import ee.schimke.wearcmp.port.WearDeviceConfiguration
import ee.schimke.wearm3catalog.uibuilder.LocalWearWidgetHostShape
import ee.schimke.wearm3catalog.uibuilder.wearWidgetHostShape
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko

@OptIn(InternalSkikoApi::class)
fun main() {
  val actions = UiBuilderSemanticActionController()
  // One registry for the page: a widget's theme typefaces, from the runtime's own `fonts/` and, for
  // anything it does not ship, the host's Google Fonts route.
  val fonts = catalogRuntimeFontRegistry()
  awaitSkiko.then(
    onFulfilled = {
      startCatalogRenderer(
        actions,
        capabilities = setOf(CATALOG_RUNTIME_CAPABILITY_HORIZONTAL_UNROLL),
      ) { document, surface, renderSessionId, onInspectionSnapshot ->
        val hostDensity = LocalDensity.current
        val density =
          Density(
            density = surface.density,
            fontScale =
              document.environment["fontScale"]
                ?.let { it as? JsonPrimitive }
                ?.contentOrNull
                ?.toFloatOrNull()
                ?.takeIf { it.isFinite() && it > 0f } ?: hostDensity.fontScale,
          )
        val layoutDirection =
          if (document.environment["layoutDirection"]?.jsonPrimitive?.contentOrNull == "rtl")
            LayoutDirection.Rtl
          else LayoutDirection.Ltr
        CompositionLocalProvider(
          LocalDensity provides density,
          LocalLayoutDirection provides layoutDirection,
          LocalWearDeviceConfiguration provides
            WearDeviceConfiguration(
              isScreenRound = true,
              screenWidthDp = surface.widthDp.toInt(),
              screenHeightDp = surface.heightDp.toInt(),
            ),
          LocalWearWidgetHostShape provides document.wearWidgetHostShape(),
          LocalUiBuilderFontRegistry provides fonts,
          LocalUiBuilderFontFamilies provides fonts.loaded,
          LocalUiBuilderFontVariants provides fonts,
        ) {
          MaterialTheme {
            // The viewport's canvas starts opaque white, and whatever the design leaves uncovered
            // (a widget frame's rounded corners, a pane wider than the frame) showed as a white box
            // in the editor. The editor draws its own backdrop behind this frame, so the runtime
            // clears to transparent and paints only the design.
            Box(
              Modifier.fillMaxSize().drawWithContent {
                drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                drawContent()
              }
            )
            Box(Modifier.requiredSize(surface.widthDp.dp, surface.heightDp.dp)) {
              if (surface.mode == UiBuilderRendererSurfaceModeV2.AUTHORING_UNROLLED) {
                // The editor's surface is the real recording, played; see
                // [RemoteM3AuthoringSurface].
                RemoteM3AuthoringSurface(
                  document,
                  surface.widthDp,
                  surface.heightDp,
                  renderSessionId,
                  actions,
                  onInspectionSnapshot,
                )
              } else {
                RemoteM3DevicePreview(
                  document = document,
                  widthDp = surface.widthDp,
                  heightDp = surface.heightDp,
                  onReady = {
                    // DEVICE is read-only, so the host only needs document identity to correlate
                    // the frame. Bounds and semantic actions belong to AUTHORING_UNROLLED and must
                    // not be fabricated from the player's pixels.
                    onInspectionSnapshot(UiBuilderInspectionCollector(document).snapshot())
                  },
                )
              }
            }
          }
        }
      }
      null
    },
    onRejected = { error("Skiko initialization failed: $it") },
  )
}
