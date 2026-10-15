// The Browser Preview as it ships: each edit records a fresh document
// (`captureCommonRemoteDocument`)
// and plays it. The default; `src/embeddedPreview` is the live alternative selected by
// `-PrcPlayersDir` (see build.gradle.kts).
package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background as scrimBackground
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding as spinnerPadding
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureCommonRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import ee.schimke.composeai.rcplayer.compose.RcComposePlayer
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.rcplayer.protocol.RcDocument
import ee.schimke.composeai.rcplayer.protocol.RcDocumentCodec
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.FontSettings
import ee.schimke.composeai.uibuilder.export.ThemeTypefaces
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.hostSpec
import ee.schimke.composeai.uibuilder.renderer.sdk.GoogleMaterialIconVectors
import ee.schimke.wearm3catalog.uibuilder.WearWidgetContainerFrame
import ee.schimke.wearm3catalog.uibuilder.wearWidgetHostShape
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Real Remote M3 creation and playback for the read-only browser device surface.
 *
 * [typefaceFallback] answers for every family the runtime's font registry does not: the player's
 * own default face unless a test stands in for the browser's.
 */
@Composable
internal fun RemoteM3DevicePreview(
  document: UiBuilderDocument,
  widthDp: Float,
  heightDp: Float,
  typefaceFallback: RcTypefaceLoader = RcTypefaceLoader.Default,
  onReady: () -> Unit,
) {
  val root = document.roots.singleOrNull()?.let(document.nodes::get)
  val widgetSize = root?.widgetSize()
  val hostSpec = widgetSize?.hostSpec(document.wearWidgetHostShape())
  // The frame is the host's: the launcher hands a widget its padding and corner radius, so a
  // design never authors them and a document that still carries them from before is not read.
  val horizontalPadding = hostSpec?.horizontalPaddingDp ?: 0f
  val verticalPadding = hostSpec?.verticalPaddingDp ?: 0f
  val cornerRadius = hostSpec?.cornerRadiusDp ?: 0f
  val contentWidth =
    hostSpec?.let { (it.frameWidthDp - 2f * horizontalPadding).coerceAtLeast(0f) } ?: widthDp
  val contentHeight =
    hostSpec?.let { (it.frameHeightDp - 2f * verticalPadding).coerceAtLeast(0f) } ?: heightDp
  // Recorded at the density the pane plays at. A document captured at 160dpi and played on a 2x
  // screen had its dp layout scaled up and its sp text not, so every word drew at half size.
  val density = LocalDensity.current
  // The widget's typefaces: recorded into the document by name, as the generated widget writes
  // them, and resolved for the player from the runtime's font registry.
  val roleNames = remember(root) { root?.let(ThemeTypefaces::families).orEmpty().remoteRoleNames() }
  // The role a text with no `style` is set in, as the generated widget's `ProvideRemoteTextStyle`.
  val textRole = root?.string("themeTextStyle").orEmpty()
  val registry = LocalUiBuilderFontRegistry.current
  LaunchedEffect(registry, roleNames) { roleNames.values.toSet().forEach { registry?.request(it) } }
  // Axes on a text with no family are drawn in Roboto Flex
  // ([RegistryTypefaceLoader.DEVICE_FAMILY]),
  // which the registry only holds once something asks for it.
  val declaredAxes =
    remember(document) {
      document.nodes.values.mapNotNullTo(mutableSetOf()) { node ->
        FontSettings.parseVariations(node.string(FontSettings.VARIATION_PROPERTY)).ifEmpty { null }
      }
    }
  val hasAxes = declaredAxes.isNotEmpty()
  LaunchedEffect(registry, hasAxes) {
    if (hasAxes) registry?.request(RegistryTypefaceLoader.DEVICE_FAMILY)
  }
  val fonts =
    remember(registry, declaredAxes, typefaceFallback) {
      RegistryTypefaceLoader(registry, declaredAxes, typefaceFallback)
    }
  // Kept across edits. Keyed on the document, every edit dropped the last drawing and showed a
  // placeholder until the new one was recorded; the previous frame stays up under a scrim instead.
  var captured by
    remember(contentWidth, contentHeight, density) {
      mutableStateOf<Result<CapturedRemoteDocuments>?>(null)
    }
  var refreshing by remember { mutableStateOf(true) }
  LaunchedEffect(document, contentWidth, contentHeight, density, roleNames, textRole) {
    refreshing = true
    val next = runCatching {
      // The capture records once and cannot wait for an icon, and in the browser a vector arrives
      // with its data file; fetch every icon the document names first, so none is recorded as "?".
      document.nodes.values
        .filter { it.componentId == "remote-m3/remote-icon" }
        .mapTo(mutableSetOf()) { it.remoteIconKey() }
        .forEach { GoogleMaterialIconVectors.load(it) }
      val content =
        captureDocument(contentWidth, contentHeight, density, roleNames, textRole) {
          RemoteDocumentTree(document).Render(widgetSize != null)
        }
      val background = hostSpec?.let { spec ->
        root
          ?.takeIf { it.slots["background"].orEmpty().isNotEmpty() }
          ?.let {
            captureDocument(
              spec.frameWidthDp.toFloat(),
              spec.frameHeightDp.toFloat(),
              density,
              roleNames,
              textRole,
            ) {
              RemoteDocumentTree(document).RenderRootSlot("background")
            }
          }
      }
      CapturedRemoteDocuments(content = content, background = background)
    }
    // A newer edit cancels this capture; `runCatching` would turn that into a failure that replaced
    // the kept drawing and cleared the newer edit's indicator. Only the live capture lands.
    currentCoroutineContext().ensureActive()
    captured = next
    refreshing = false
    onReady()
  }

  val result = captured
  val state =
    when {
      result == null || refreshing -> "Building"
      result.isSuccess -> "Ready"
      else -> "Failed"
    }
  Box(
    Modifier.fillMaxSize().testTag(REMOTE_M3_DEVICE_PREVIEW_TEST_TAG).semantics {
      stateDescription = state
    }
  ) {
    ProvideDevicePreviewTime(document) {
      when (result) {
        null -> Unit
        else ->
          result.fold(
            onSuccess = { documents ->
              if (hostSpec == null || root == null) {
                RcComposePlayer(
                  document = documents.content,
                  theme = document.playerTheme(),
                  typefaces = fonts,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                val background = root.color("background") ?: Color(39, 36, 48)
                WearWidgetContainerFrame(
                  modifier = Modifier.testTag(REMOTE_M3_WIDGET_HOST_TEST_TAG),
                  contentWidthDp = contentWidth,
                  contentHeightDp = contentHeight,
                  horizontalPaddingDp = horizontalPadding,
                  verticalPaddingDp = verticalPadding,
                  cornerRadiusDp = cornerRadius,
                  background = background,
                  backgroundContent = { shape ->
                    documents.background?.let {
                      RcComposePlayer(
                        document = it,
                        theme = document.playerTheme(),
                        typefaces = fonts,
                        modifier =
                          Modifier.fillMaxSize()
                            .clip(shape)
                            .testTag(REMOTE_M3_WIDGET_BACKGROUND_TEST_TAG),
                      )
                    }
                  },
                ) {
                  RcComposePlayer(
                    document = documents.content,
                    theme = document.playerTheme(),
                    typefaces = fonts,
                    modifier = Modifier.fillMaxSize().testTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG),
                  )
                }
              }
            },
            onFailure = { Text("Remote M3 preview failed: ${it.message ?: it::class.simpleName}") },
          )
      }
    }
    if (refreshing) RefreshingIndicator(dimmed = result != null)
  }
}

/** A light scrim over the last drawing and a small spinner, while the next one is recorded. */
@Composable
private fun RefreshingIndicator(dimmed: Boolean) {
  val turn by
    rememberInfiniteTransition(label = "refreshing")
      .animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "turn",
      )
  Box(
    Modifier.fillMaxSize()
      .testTag(REMOTE_M3_REFRESHING_TEST_TAG)
      .then(if (dimmed) Modifier.scrimBackground(Color.Black.copy(alpha = 0.12f)) else Modifier)
  ) {
    Canvas(Modifier.align(Alignment.TopEnd).spinnerPadding(6.dp).size(12.dp)) {
      drawArc(
        color = Color.White.copy(alpha = 0.8f),
        startAngle = turn,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = 1.5.dp.toPx()),
      )
    }
  }
}

private data class CapturedRemoteDocuments(
  val content: RcDocument,
  val background: RcDocument?,
)

private suspend fun captureDocument(
  widthDp: Float,
  heightDp: Float,
  density: Density,
  roleNames: Map<String, String>,
  textRole: String,
  content: @Composable @RemoteComposable () -> Unit,
): RcDocument {
  val bytes =
    captureCommonRemoteDocument(
      RemoteCreationDisplayInfo(
        (widthDp * density.density).roundToInt(),
        (heightDp * density.density).roundToInt(),
        (160 * density.density).roundToInt(),
      ),
      // Pictures travel inside the document: without an encoder the common writer drops them.
      encodePng = ::encodeDesignAssetPng,
    ) {
      RemotePreviewContent(widthDp, heightDp, density, roleNames, textRole, content)
    }
  return RcDocumentCodec.decode(bytes)
}
