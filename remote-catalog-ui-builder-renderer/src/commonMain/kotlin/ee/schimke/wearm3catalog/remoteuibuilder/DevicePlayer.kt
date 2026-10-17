// The Browser Preview and the Visual Editor's surface, on rc-players' live player
// (`RcEmbeddedPlayer`).
package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.wear.compose.material3.Text
import ee.schimke.composeai.rccreation.bridge.RcComponentIds
import ee.schimke.composeai.rcplayer.compose.LocalRcComponentCoordinates
import ee.schimke.composeai.rcplayer.compose.RcComponentCoordinatesListener
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.rcplayer.embedded.RcEmbeddedPlayer
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.FontSettings
import ee.schimke.composeai.uibuilder.export.ThemeTypefaces
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiBuilderNode
import ee.schimke.composeai.uibuilder.export.hostSpec
import ee.schimke.composeai.uibuilder.renderer.sdk.GoogleMaterialIconVectors
import ee.schimke.wearm3catalog.uibuilder.WearWidgetContainerFrame
import ee.schimke.wearm3catalog.uibuilder.wearWidgetHostShape
import kotlin.math.roundToInt

/**
 * Real Remote M3 creation and playback for the browser device surface, played live.
 *
 * The design's remote composition is kept alive under the preview: an edit recomposes only what
 * changed and re-renders the retained tree, rather than recording a fresh document and playing it.
 * There is no recording step to wait for, so there is no refreshing scrim either. With [authoring]
 * it is the Visual Editor's surface too ([RemoteM3AuthoringSurface]).
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
  authoring: AuthoringInspection? = null,
  onReady: () -> Unit,
) {
  val root = document.roots.singleOrNull()?.let(document.nodes::get)
  val widgetSize = root?.widgetSize()
  val hostSpec = widgetSize?.hostSpec(document.wearWidgetHostShape())
  // The frame is the host's: the launcher hands a widget its padding and corner radius.
  val horizontalPadding = hostSpec?.horizontalPaddingDp ?: 0f
  val verticalPadding = hostSpec?.verticalPaddingDp ?: 0f
  val cornerRadius = hostSpec?.cornerRadiusDp ?: 0f
  val contentWidth =
    hostSpec?.let { (it.frameWidthDp - 2f * horizontalPadding).coerceAtLeast(0f) } ?: widthDp
  val contentHeight =
    hostSpec?.let { (it.frameHeightDp - 2f * verticalPadding).coerceAtLeast(0f) } ?: heightDp
  // Recorded at the density the pane plays at, so dp layout and sp text scale together.
  val density = LocalDensity.current
  val roleNames = remember(root) { root?.let(ThemeTypefaces::families).orEmpty().remoteRoleNames() }
  val textRole = root?.string("themeTextStyle").orEmpty()
  val registry = LocalUiBuilderFontRegistry.current
  LaunchedEffect(registry, roleNames) { roleNames.values.toSet().forEach { registry?.request(it) } }
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

  // A rendered icon is looked up synchronously, and in the browser a vector arrives with its data
  // file. Every icon the design names is fetched before the design is shown, and again whenever an
  // edit names a new one; until then the last design stays up.
  val iconKeys =
    remember(document) {
      document.nodes.values
        .filter { it.componentId == "remote-m3/remote-icon" }
        .mapTo(mutableSetOf()) { it.remoteIconKey() }
    }
  var loadedIcons by remember { mutableStateOf(emptySet<String>()) }
  LaunchedEffect(iconKeys) {
    iconKeys.forEach { GoogleMaterialIconVectors.load(it) }
    loadedIcons = loadedIcons + iconKeys
  }
  var shown by remember { mutableStateOf<UiBuilderDocument?>(null) }
  if (loadedIcons.containsAll(iconKeys)) shown = document
  val design = shown

  val hasBackground = hostSpec != null && root?.slots?.get("background").orEmpty().isNotEmpty()
  // Which design each player has rendered. The pane is Ready once both show the current one, and
  // [onReady] is told once per design.
  var contentRendered by remember { mutableStateOf<UiBuilderDocument?>(null) }
  var backgroundRendered by remember { mutableStateOf<UiBuilderDocument?>(null) }
  // Each player's own: one player rendering must not clear the other's failure.
  var contentFailure by remember { mutableStateOf<Throwable?>(null) }
  var backgroundFailure by remember { mutableStateOf<Throwable?>(null) }
  val failure = contentFailure ?: backgroundFailure.takeIf { hasBackground }
  val ready =
    design != null &&
      contentRendered === design &&
      (!hasBackground || backgroundRendered === design)
  val state =
    when {
      failure != null -> "Failed"
      ready -> "Ready"
      else -> "Building"
    }
  val currentOnReady by rememberUpdatedState(onReady)
  LaunchedEffect(ready, design) { if (ready) currentOnReady() }

  Box(
    Modifier.fillMaxSize().testTag(REMOTE_M3_DEVICE_PREVIEW_TEST_TAG).semantics {
      stateDescription = state
    }
  ) {
    if (design == null) return@Box
    @Composable
    fun Player(
      display: RemoteCreationDisplayInfo,
      widthDp: Float,
      heightDp: Float,
      modifier: Modifier,
      onSettled: (failure: Throwable?) -> Unit,
      content: @Composable () -> Unit,
    ) {
      CompositionLocalProvider(LocalRcComponentCoordinates provides authoring?.listener) {
        RcEmbeddedPlayer(
          display = display,
          modifier = modifier,
          theme = design.playerTheme(),
          typefaces = fonts,
          encodePng = ::encodeDesignAssetPng,
          onRendered = { onSettled(null) },
          // A failed render still settles the design: the pane reports Failed, and [onReady] is
          // told once.
          onFailure = { onSettled(it) },
          key = design,
        ) {
          RemotePreviewContent(widthDp, heightDp, density, roleNames, textRole, content)
        }
      }
    }

    ProvideDevicePreviewTime(design) {
      val tree = remember(design, authoring) { RemoteDocumentTree(design, authoring) }
      val contentDisplay =
        remember(contentWidth, contentHeight, density) {
          displayInfo(contentWidth, contentHeight, density)
        }
      if (hostSpec == null || root == null) {
        Player(
          contentDisplay,
          contentWidth,
          contentHeight,
          Modifier.fillMaxSize(),
          onSettled = {
            contentFailure = it
            contentRendered = design
          },
        ) {
          tree.Render(widgetSize != null)
        }
      } else {
        val background = root.color("background") ?: Color(39, 36, 48)
        WearWidgetContainerFrame(
          // The container draws no remote component of its own; in authoring it is the frame.
          modifier =
            Modifier.testTag(REMOTE_M3_WIDGET_HOST_TEST_TAG).let { frame ->
              if (authoring == null) frame
              else frame.onGloballyPositioned { authoring.onBounds(root.id, it) }
            },
          contentWidthDp = contentWidth,
          contentHeightDp = contentHeight,
          horizontalPaddingDp = horizontalPadding,
          verticalPaddingDp = verticalPadding,
          cornerRadiusDp = cornerRadius,
          background = background,
          backgroundContent = { shape ->
            if (hasBackground) {
              val frameWidth = hostSpec.frameWidthDp.toFloat()
              val frameHeight = hostSpec.frameHeightDp.toFloat()
              val backgroundDisplay =
                remember(frameWidth, frameHeight, density) {
                  displayInfo(frameWidth, frameHeight, density)
                }
              Player(
                backgroundDisplay,
                frameWidth,
                frameHeight,
                Modifier.fillMaxSize().clip(shape).testTag(REMOTE_M3_WIDGET_BACKGROUND_TEST_TAG),
                onSettled = {
                  backgroundFailure = it
                  backgroundRendered = design
                },
              ) {
                tree.RenderRootSlot("background")
              }
            }
          },
        ) {
          Player(
            contentDisplay,
            contentWidth,
            contentHeight,
            Modifier.fillMaxSize().testTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG),
            onSettled = {
              contentFailure = it
              contentRendered = design
            },
          ) {
            tree.Render(widgetSize != null)
          }
        }
      }
    }
    failure?.let { Text("Remote M3 preview failed: ${it.message ?: it::class.simpleName}") }
  }
}

private fun displayInfo(widthDp: Float, heightDp: Float, density: Density) =
  RemoteCreationDisplayInfo(
    (widthDp * density.density).roundToInt(),
    (heightDp * density.density).roundToInt(),
    (160 * density.density).roundToInt(),
  )

/**
 * What an authoring surface needs from the preview: each authored node tagged with a component id
 * from [ids], and what the player then reports for it, by node id — its geometry, a text's layout,
 * a click the editor can run — and every node the preview could only draw as a diagnostic.
 */
internal class AuthoringInspection(
  val ids: RcComponentIds<String>,
  val onBounds: (nodeId: String, coordinates: LayoutCoordinates) -> Unit,
  val onText: (nodeId: String, content: LayoutCoordinates, layout: TextLayoutResult) -> Unit =
    { _, _, _ ->
    },
  /** A click the player holds for a node, [added] or taken back. */
  val onClick: (nodeId: String, click: () -> Unit, added: Boolean) -> Unit = { _, _, _ -> },
  /** A node drawn as an "Unsupported" diagnostic ([drawn] false), or no longer ([drawn] true). */
  val onUndrawn: (node: UiBuilderNode, drawn: Boolean) -> Unit = { _, _ -> },
) {
  val listener =
    object : RcComponentCoordinatesListener {
      override fun onPositioned(componentId: Int, coordinates: LayoutCoordinates) {
        ids.keyFor(componentId)?.let { onBounds(it, coordinates) }
      }

      override fun onTextLaidOut(
        componentId: Int,
        content: LayoutCoordinates,
        layout: TextLayoutResult,
      ) {
        ids.keyFor(componentId)?.let { onText(it, content, layout) }
      }

      override fun onClickActionAdded(componentId: Int, click: () -> Unit) {
        ids.keyFor(componentId)?.let { onClick(it, click, true) }
      }

      override fun onClickActionRemoved(componentId: Int, click: () -> Unit) {
        ids.keyFor(componentId)?.let { onClick(it, click, false) }
      }
    }

  /** Called where the tree draws [node] as a diagnostic, for as long as it does. */
  @Composable
  fun ReportUndrawn(node: UiBuilderNode) {
    DisposableEffect(node.id, node.componentId) {
      onUndrawn(node, false)
      onDispose { onUndrawn(node, true) }
    }
  }
}
