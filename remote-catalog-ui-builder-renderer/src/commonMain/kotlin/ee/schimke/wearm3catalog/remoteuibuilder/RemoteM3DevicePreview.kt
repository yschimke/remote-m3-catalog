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
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumnScope
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRowScope
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteColumnScope
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rotate
import androidx.compose.remote.creation.compose.modifier.scale
import androidx.compose.remote.creation.compose.modifier.sharedElement
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.modifier.zIndex
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import androidx.wear.compose.remote.material3.LocalRemoteContentColor
import androidx.wear.compose.remote.material3.RemoteAppCard
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteButtonGroup
import androidx.wear.compose.remote.material3.RemoteCard
import androidx.wear.compose.remote.material3.RemoteCheckboxButton
import androidx.wear.compose.remote.material3.RemoteCircularProgressIndicator
import androidx.wear.compose.remote.material3.RemoteCompactButton
import androidx.wear.compose.remote.material3.RemoteCurvedProgressIndicator
import androidx.wear.compose.remote.material3.RemoteEdgeButton
import androidx.wear.compose.remote.material3.RemoteHorizontalPageIndicator
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteIconButton
import androidx.wear.compose.remote.material3.RemoteLinearProgressIndicator
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteOutlinedCard
import androidx.wear.compose.remote.material3.RemoteRadioButton
import androidx.wear.compose.remote.material3.RemoteSlider
import androidx.wear.compose.remote.material3.RemoteSliderDefaults
import androidx.wear.compose.remote.material3.RemoteSplitCheckboxButton
import androidx.wear.compose.remote.material3.RemoteSplitRadioButton
import androidx.wear.compose.remote.material3.RemoteSplitSwitchButton
import androidx.wear.compose.remote.material3.RemoteStepper
import androidx.wear.compose.remote.material3.RemoteSwitchButton
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteTextButton
import androidx.wear.compose.remote.material3.RemoteTimeText
import androidx.wear.compose.remote.material3.RemoteTitleCard
import androidx.wear.compose.remote.material3.RemoteVerticalPageIndicator
import androidx.wear.compose.remote.material3.rememberRemotePageIndicatorState
import ee.schimke.composeai.rcplayer.compose.RcComposePlayer
import ee.schimke.composeai.rcplayer.compose.RcPlayerTheme
import ee.schimke.composeai.rcplayer.protocol.RcDocument
import ee.schimke.composeai.rcplayer.protocol.RcDocumentCodec
import ee.schimke.composeai.uibuilder.LocalUiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.FontSettings
import ee.schimke.composeai.uibuilder.export.RemoteModifierVocabulary
import ee.schimke.composeai.uibuilder.export.SHOW_BY_STATE
import ee.schimke.composeai.uibuilder.export.StateSelection
import ee.schimke.composeai.uibuilder.export.ThemeTypefaces
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiBuilderNode
import ee.schimke.composeai.uibuilder.export.UiDrawing
import ee.schimke.composeai.uibuilder.export.UiTimeText
import ee.schimke.composeai.uibuilder.export.WearWidgetScaffoldSize
import ee.schimke.composeai.uibuilder.export.hostSpec
import ee.schimke.composeai.uibuilder.export.stateSelection
import ee.schimke.composeai.uibuilder.renderer.sdk.CanvasRenderNode
import ee.schimke.composeai.uibuilder.renderer.sdk.CanvasRenderTree
import ee.schimke.composeai.uibuilder.renderer.sdk.googleMaterialIconImageVector
import ee.schimke.wearm3catalog.uibuilder.WearWidgetContainerFrame
import ee.schimke.wearm3catalog.uibuilder.wearWidgetHostShape
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

internal const val REMOTE_M3_DEVICE_PREVIEW_TEST_TAG = "remote-m3-device-preview"
internal const val REMOTE_M3_WIDGET_HOST_TEST_TAG = "remote-m3-widget-host"
internal const val REMOTE_M3_WIDGET_BACKGROUND_TEST_TAG = "remote-m3-widget-background"
internal const val REMOTE_M3_WIDGET_CONTENT_TEST_TAG = "remote-m3-widget-content"
internal const val REMOTE_M3_REFRESHING_TEST_TAG = "remote-m3-refreshing"

/** Real Remote M3 creation and playback for the read-only browser device surface. */
@Composable
internal fun RemoteM3DevicePreview(
  document: UiBuilderDocument,
  widthDp: Float,
  heightDp: Float,
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
  val hasAxes =
    remember(document) { document.nodes.values.any { it.fontVariationSettings() != null } }
  LaunchedEffect(registry, hasAxes) {
    if (hasAxes) registry?.request(RegistryTypefaceLoader.DEVICE_FAMILY)
  }
  val fonts = remember(registry) { RegistryTypefaceLoader(registry) }
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

/**
 * The round buttons' own circle, as a fixed radius: the default `RemoteCircleShape` is a 50% corner
 * that rc-player-compose 2.1.2 resolves from a component size it has not registered yet (it fails
 * the whole document). At the buttons' 52dp default this is the same circle.
 */
private val PLAYABLE_CIRCLE = RemoteRoundedCornerShape(26.rdp)

private class CaptureWindowInfo(override val containerSize: IntSize) : WindowInfo {
  override val isWindowFocused: Boolean = true
}

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
      // The capture runs outside any window; components that size to the screen (the stepper)
      // read the widget's own extent instead.
      val window =
        CaptureWindowInfo(
          IntSize(
            (widthDp * density.density).roundToInt(),
            (heightDp * density.density).roundToInt(),
          )
        )
      CompositionLocalProvider(LocalWindowInfo provides window) {
        RemoteMaterialTheme(typography = RemoteMaterialTheme.typography.withRoleNames(roleNames)) {
          CompositionLocalProvider(LocalThemeTextRole provides textRole, content = content)
        }
      }
    }
  return RcDocumentCodec.decode(bytes)
}

private class RemoteDocumentTree(private val document: UiBuilderDocument) {
  // Computed values stay expressions: the recorded document plays them, through [DocumentValues],
  // rather than drawing the value they have at the preview state.
  private val tree =
    CanvasRenderTree(
      document = document,
      state = document.initialState(),
      adapterIds = emptyMap(),
      adapterMappings = emptyMap(),
      evaluateExpressions = false,
    )

  private var documentValues: DocumentValues? = null

  /** The design's live state, remembered by whichever entry point is composing. */
  private val values: DocumentValues
    get() = checkNotNull(documentValues) { "values read outside Render" }

  @Composable
  @RemoteComposable
  fun Render(skipWidgetFrame: Boolean) {
    documentValues = rememberDocumentValues(document)
    document.roots.forEach { id ->
      tree.root(id)?.let { root ->
        if (skipWidgetFrame && root.node.widgetSize() != null) {
          root.slot("content").forEach { RenderNode(it) }
        } else {
          RenderNode(root)
        }
      }
    }
  }

  @Composable
  @RemoteComposable
  fun RenderRootSlot(slotName: String) {
    documentValues = rememberDocumentValues(document)
    // Stacked in one full-frame box, the way the host layers a widget's background: each layer is
    // a brush over the whole frame, so it fills it without authoring any size of its own (the
    // export refuses a background node that carries one).
    RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
      document.roots.singleOrNull()?.let(tree::root)?.slot(slotName)?.forEach {
        RenderNode(it, fillFrame = true)
      }
    }
  }

  /**
   * "Show by state" on a box, played as the `RemoteStateLayout` the export writes.
   *
   * Every branch is recorded — one per case, in case order, then the fallback — and the layout
   * starts on the branch the design's initial state selects. Recording them all, rather than only
   * the selected child, is what lets a `sharedElement` pair across branches the way it does in the
   * exported widget, and what stops the preview stacking every branch on top of the others.
   */
  @Composable
  @RemoteComposable
  private fun StateSwitch(entry: CanvasRenderNode, modifier: RemoteModifier) {
    val node = entry.node
    val children = entry.slot("children").associateBy { it.node.id }
    val selection = node.stateSelection()
    val branches: List<String?> =
      selection?.let { it.cases.keys.toList() + it.fallback } ?: listOf(null)
    val selected =
      selection?.selectedNode(document.initialState(), document.stateVariables)?.let {
        branches.indexOf(it)
      }
    val initial = rememberMutableRemoteInt(selected?.takeIf { it >= 0 } ?: branches.lastIndex)
    // Follow the variable itself, so a click or toggle that sets it switches the branch.
    val current = selection?.let(::liveBranch) ?: initial
    RemoteBox(modifier = modifier, contentAlignment = node.boxAlignment()) {
      RemoteStateLayout(current, *IntArray(branches.size) { it }) { branch ->
        RemoteBox { branches.getOrNull(branch)?.let(children::get)?.let { RenderNode(it) } }
      }
    }
  }

  /**
   * The selected branch as the export writes it: the index of the case the live state variable
   * equals, else the fallback's (`cases.size`). Int cases compare 16-bit halves, as the export
   * does, so `Int.MIN_VALUE` cannot overflow the player's equality. Null for a selector that is not
   * an int or boolean state read; those keep the branch their initial value picks.
   */
  private fun liveBranch(selection: StateSelection): RemoteInt? {
    if (selection.selector["type"] != JsonPrimitive("state")) return null
    val asInt = values.int(selection.selector)
    val asBool = if (asInt == null) values.bool(selection.selector) ?: return null else null
    var ordinal: RemoteInt = selection.cases.size.ri
    selection.cases.values.withIndex().reversed().forEach { (index, literal) ->
      val match: RemoteBoolean =
        if (asInt != null) {
          val value = literal.intOrNull ?: return null
          (asInt and 65535.ri)
            .isEqualTo((value and 65535).ri)
            .and((asInt shr 16.ri).isEqualTo((value shr 16).ri))
        } else {
          asBool!!.isEqualTo((literal.booleanOrNull ?: return null).rb)
        }
      ordinal = match.select(index.ri, ordinal)
    }
    return ordinal
  }

  /** A slot's children as a composable lambda, or null for an empty slot (an absent overload). */
  private fun CanvasRenderNode.slotContent(
    name: String
  ): (@Composable @RemoteComposable () -> Unit)? =
    slot(name)
      .takeIf { it.isNotEmpty() }
      ?.let { children -> { children.forEach { RenderNode(it) } } }

  /** [slotContent] for a slot whose lambda receives the row it is laid out in. */
  private fun CanvasRenderNode.rowSlotContent(
    name: String
  ): (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? =
    slot(name)
      .takeIf { it.isNotEmpty() }
      ?.let { children -> { children.forEach { RenderNode(it, row = this) } } }

  /** `enabled`, which may read Boolean state or be computed. */
  private fun UiBuilderNode.enabled(): RemoteBoolean =
    values.bool(properties["enabled"]) ?: boolean("enabled", true).rb

  @Composable
  @RemoteComposable
  private fun DrawCanvas(entry: CanvasRenderNode, modifier: RemoteModifier) {
    DeviceDrawCanvas(entry, modifier, values) { it.remoteColor() }
  }

  /**
   * One node, entered from the container it sits in.
   *
   * [row] and [column] are that container's scope, because `weight` is a member of it rather than a
   * plain modifier — exactly as in the Kotlin this design exports to.
   */
  @Composable
  @RemoteComposable
  private fun RenderNode(
    entry: CanvasRenderNode,
    row: RemoteRowScope? = null,
    column: RemoteColumnScope? = null,
    collapsibleColumn: RemoteCollapsibleColumnScope? = null,
    collapsibleRow: RemoteCollapsibleRowScope? = null,
    fillFrame: Boolean = false,
  ) {
    val node = entry.node
    val modifier =
      node
        .remoteModifier(values, row, column, collapsibleColumn, collapsibleRow)
        .let { if (fillFrame) it.fillMaxSize() else it }
        .let { base ->
          // A layout's own click, long press and double tap, as the export writes them.
          val long = values.hasAction(node, "longClick")
          val double = values.hasAction(node, "doubleClick")
          when {
            node.componentId.startsWith("remote-m3/") -> base
            long || double ->
              base.combinedClickable(
                onClick = values.action(node, "click"),
                onLongClick = values.action(node, "longClick"),
                onDoubleClick = values.action(node, "doubleClick"),
              )
            values.hasAction(node, "click") -> base.clickable(values.action(node, "click"))
            else -> base
          }
        }
    if (node.componentId == "layout/box" && SHOW_BY_STATE in node.properties) {
      StateSwitch(entry, modifier)
      return
    }
    when (node.componentId) {
      "remote-m3/widget-container-small",
      "remote-m3/widget-container-large" -> entry.slot("content").forEach { RenderNode(it) }
      "layout/box" -> {
        val children = entry.slot("children")
        RemoteBox(
          modifier = modifier,
          contentAlignment =
            children.sharedAlignment("align")?.boxAlignment() ?: node.boxAlignment(),
        ) {
          children.forEach { RenderNode(it) }
        }
      }
      "layout/column" -> {
        val children = entry.slot("children")
        RemoteColumn(
          modifier = modifier,
          verticalArrangement = node.verticalArrangement(),
          horizontalAlignment =
            children.sharedAlignment("alignHorizontal")?.horizontal() ?: node.horizontalAlignment(),
        ) {
          children.forEach { RenderNode(it, column = this) }
        }
      }
      "layout/row" -> {
        val children = entry.slot("children")
        RemoteRow(
          modifier = modifier,
          horizontalArrangement = node.horizontalArrangement(),
          verticalAlignment =
            children.sharedAlignment("alignVertical")?.vertical() ?: node.verticalAlignment(),
        ) {
          children.forEach { RenderNode(it, row = this) }
        }
      }
      // Remote Compose's own layouts. The CMP writer records all four; only the fit box is in the
      // Glance Wear widget profile, so a widget using the other three plays here and fails on
      // Native / Live, which is the authoritative lane.
      "layout/flow-row" ->
        RemoteFlowRow(
          modifier = modifier,
          horizontalArrangement = node.horizontalArrangement(),
          verticalArrangement = node.verticalArrangement(),
          maxItemsInEachRow = node.integer("maxItemsInEachRow")?.takeIf { it > 0 } ?: Int.MAX_VALUE,
        ) {
          entry.slot("children").forEach { RenderNode(it) }
        }
      "layout/collapsible-column" -> {
        val children = entry.slot("children")
        RemoteCollapsibleColumn(
          modifier = modifier,
          verticalArrangement = node.verticalArrangement(),
          horizontalAlignment =
            children.sharedAlignment("alignHorizontal")?.horizontal() ?: node.horizontalAlignment(),
        ) {
          children.forEach { RenderNode(it, collapsibleColumn = this) }
        }
      }
      "layout/collapsible-row" -> {
        val children = entry.slot("children")
        RemoteCollapsibleRow(
          modifier = modifier,
          horizontalArrangement = node.horizontalArrangement(),
          verticalAlignment =
            children.sharedAlignment("alignVertical")?.vertical() ?: node.verticalAlignment(),
        ) {
          children.forEach { RenderNode(it, collapsibleRow = this) }
        }
      }
      "layout/fit-box" ->
        RemoteFitBox(
          modifier = modifier,
          horizontalAlignment =
            when (node.string("horizontalAlignment")) {
              "start" -> RemoteAlignment.Start
              "end" -> RemoteAlignment.End
              else -> RemoteAlignment.CenterHorizontally
            },
          verticalArrangement =
            when (node.string("verticalArrangement")) {
              "top" -> RemoteArrangement.Top
              "bottom" -> RemoteArrangement.Bottom
              else -> RemoteArrangement.Center
            },
        ) {
          entry.slot("children").forEach { RenderNode(it) }
        }
      "m3/text",
      "remote-m3/remote-text" ->
        RemoteText(
          text = values.string(node.properties["text"]) ?: node.string("text").rs,
          modifier = modifier,
          color = values.color(node.properties["color"]) ?: node.remoteColor("color"),
          fontSize =
            node.number("fontSize")?.sp?.asRemoteTextUnit()
              ?: node.number("fontSizeSp")?.sp?.asRemoteTextUnit(),
          fontWeight = node.fontWeight(),
          textAlign = node.textAlign(),
          overflow = node.textOverflow(),
          maxLines = node.integer("maxLines") ?: Int.MAX_VALUE,
          style = node.textStyle().withFontFeatures(node),
          fontVariationSettings = node.fontVariationSettings(),
        )
      "remote-m3/remote-button" ->
        RemoteButton(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      // The icon the published catalog maps onto the canvas's `wear-m3/icon`: its `imageVector` is
      // that adapter's `iconKey`, with the same `addCircle` default, so both surfaces draw the same
      // glyph. Without this branch the pane drew the red "Unsupported" text, which a round button
      // then wrapped a few letters to a line.
      "remote-m3/remote-icon" -> {
        val vector =
          googleMaterialIconImageVector(
            node.string("imageVector").ifEmpty { REMOTE_ICON_DEFAULT_KEY }
          )
        if (vector == null) {
          RemoteText(text = "?".rs, modifier = modifier)
        } else {
          RemoteIcon(
            imageVector = vector.toRemoteImageVector(),
            contentDescription = node.string("contentDescription").ifEmpty { null }?.rs,
            modifier = modifier,
            tint = node.remoteColor("tint") ?: LocalRemoteContentColor.current,
          )
        }
      }
      "remote-m3/remote-card" ->
        RemoteCard(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      "remote-m3/remote-circular-progress-indicator" ->
        RemoteCircularProgressIndicator(
          progress = values.float(node.properties["progress"]) ?: 0f.rf,
          modifier = modifier,
          startAngle = values.float(node.properties["startAngle"]) ?: 0f.rf,
          endAngle =
            values.float(node.properties["endAngle"])
              ?: values.float(node.properties["startAngle"])
              ?: 0f.rf,
        )
      "remote-m3/remote-linear-progress-indicator" ->
        RemoteLinearProgressIndicator(
          progress = values.float(node.properties["progress"]) ?: 0f.rf,
          modifier = modifier,
          enabled = node.enabled(),
        )
      "remote-m3/remote-curved-progress-indicator" ->
        RemoteCurvedProgressIndicator(
          progress = values.float(node.properties["progress"]) ?: 0f.rf,
          modifier = modifier,
          enabled = node.enabled(),
        )
      "remote-m3/remote-horizontal-page-indicator",
      "remote-m3/remote-vertical-page-indicator" -> {
        val state =
          rememberRemotePageIndicatorState(
            pageCount = node.integer("pageCount")?.coerceAtLeast(1) ?: 4,
            selectedPage = values.int(node.properties["selectedPage"]) ?: 0.ri,
          )
        if (node.componentId.endsWith("vertical-page-indicator"))
          RemoteVerticalPageIndicator(state = state, modifier = modifier)
        else RemoteHorizontalPageIndicator(state = state, modifier = modifier)
      }
      "remote-m3/remote-compact-button" ->
        RemoteCompactButton(
          onClick = values.action(node, "click"),
          modifier = modifier,
          icon = entry.slotContent("icon"),
          enabled = node.enabled(),
          label = entry.rowSlotContent("label"),
        )
      "remote-m3/remote-icon-button" ->
        RemoteIconButton(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
          shape = PLAYABLE_CIRCLE,
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      "remote-m3/remote-text-button" ->
        RemoteTextButton(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
          shape = PLAYABLE_CIRCLE,
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      "remote-m3/remote-outlined-card" ->
        RemoteOutlinedCard(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      "remote-m3/remote-title-card" ->
        RemoteTitleCard(
          onClick = values.action(node, "click"),
          title = { entry.slot("title").forEach { RenderNode(it) } },
          modifier = modifier,
          enabled = node.enabled(),
          time = entry.slotContent("time"),
          subtitle = entry.slotContent("subtitle"),
          content = entry.slotContent("content"),
        )
      "remote-m3/remote-app-card" ->
        RemoteAppCard(
          onClick = values.action(node, "click"),
          appName = { entry.slot("appName").forEach { RenderNode(it) } },
          title = { entry.slot("title").forEach { RenderNode(it) } },
          modifier = modifier,
          enabled = node.enabled(),
          appImage = entry.slotContent("appImage"),
          time = entry.slotContent("time"),
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      "remote-m3/remote-button-group" ->
        RemoteButtonGroup(modifier = modifier) {
          entry.slot("content").forEach { RenderNode(it, row = this) }
        }
      "remote-m3/remote-checkbox-button" ->
        RemoteCheckboxButton(
          checked = values.bool(node.properties["checked"]) ?: false.rb,
          onCheckedChange = values.action(node, "checkedChange"),
          modifier = modifier,
          enabled = node.enabled(),
          icon = entry.slotContent("icon"),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-switch-button" ->
        RemoteSwitchButton(
          checked = values.bool(node.properties["checked"]) ?: false.rb,
          onCheckedChange = values.action(node, "checkedChange"),
          modifier = modifier,
          enabled = node.enabled(),
          icon = entry.slotContent("icon"),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-radio-button" ->
        RemoteRadioButton(
          selected = values.bool(node.properties["selected"]) ?: false.rb,
          onSelect = values.action(node, "select"),
          modifier = modifier,
          enabled = node.enabled(),
          icon = entry.slotContent("icon"),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-split-checkbox-button" ->
        RemoteSplitCheckboxButton(
          checked = values.bool(node.properties["checked"]) ?: false.rb,
          onCheckedChange = values.action(node, "checkedChange"),
          toggleContentDescription = null,
          onContainerClick = values.action(node, "containerClick"),
          modifier = modifier,
          enabled = node.enabled(),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-split-switch-button" ->
        RemoteSplitSwitchButton(
          checked = values.bool(node.properties["checked"]) ?: false.rb,
          onCheckedChange = values.action(node, "checkedChange"),
          toggleContentDescription = null,
          onContainerClick = values.action(node, "containerClick"),
          modifier = modifier,
          enabled = node.enabled(),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-split-radio-button" ->
        RemoteSplitRadioButton(
          selected = values.bool(node.properties["selected"]) ?: false.rb,
          onSelectionClick = values.action(node, "selectionClick"),
          selectionContentDescription = null,
          onContainerClick = values.action(node, "containerClick"),
          modifier = modifier,
          enabled = node.enabled(),
          secondaryLabel = entry.rowSlotContent("secondaryLabel"),
          label = entry.rowSlotContent("label") ?: {},
        )
      "remote-m3/remote-edge-button" ->
        RemoteEdgeButton(
          onClick = values.action(node, "click"),
          modifier = modifier,
          enabled = node.enabled(),
        ) {
          entry.slot("content").forEach { RenderNode(it, row = this) }
        }
      "remote-m3/remote-slider" -> {
        val steps = node.integer("steps") ?: 0
        RemoteSlider(
          value = values.float(node.properties["value"]) ?: 0f.rf,
          steps = steps,
          modifier = modifier,
          decreaseAction = values.action(node, "decreaseAction"),
          increaseAction = values.action(node, "increaseAction"),
          enabled = node.enabled(),
          // The component's own default when the design does not say.
          segmented = node.boolean("segmented", steps <= RemoteSliderDefaults.MaxSegmentSteps),
        )
      }
      UiTimeText.ID ->
        RemoteTimeText(
          modifier = modifier,
          fontSize = node.number("textSizeSp")?.sp?.asRemoteTextUnit(),
          leadingText = values.string(node.properties["leadingText"]),
          trailingText = values.string(node.properties["trailingText"]),
          separator = values.string(node.properties["separator"]) ?: "·".rs,
          color = values.color(node.properties["color"]) ?: node.remoteColor("color"),
        )
      "remote-m3/remote-stepper" ->
        RemoteStepper(
          value = values.float(node.properties["value"]) ?: 0f.rf,
          steps = node.integer("steps") ?: 0,
          modifier = modifier,
          decreaseAction = values.action(node, "decreaseAction"),
          increaseAction = values.action(node, "increaseAction"),
          enabled = node.enabled(),
        ) {
          entry.slot("content").forEach { RenderNode(it) }
        }
      UiDrawing.CANVAS -> DrawCanvas(entry, modifier)
      "asset/image" -> {
        // The editor inlines the uploaded pictures it has fetched; until it has, a plain frame.
        val bytes = document.embeddedAssetBytes(node.assetKey())
        val bitmap = remember(bytes) { bytes?.let(::decodeDesignAssetBitmap) }
        if (bitmap == null) RemoteBox(modifier = modifier.background(Color(0x33808080).rc))
        else
          RemoteImage(
            remoteBitmap = bitmap.rb,
            contentDescription = node.string("contentDescription").ifEmpty { null }?.rs,
            modifier = modifier,
            contentScale = node.assetContentScale(),
          )
      }
      "shape/linear-gradient" -> {
        val colors = listOfNotNull(node.remoteColor("startColor"), node.remoteColor("endColor"))
        val brush =
          if (node.string("direction") == "horizontal") RemoteBrush.horizontalGradient(colors)
          else RemoteBrush.verticalGradient(colors)
        // A draw layer declares no size modifiers: it fills whatever it is placed in.
        RemoteBox(modifier = modifier.fillMaxSize().background(brush))
      }
      else ->
        RemoteText(
          text = "Unsupported: ${node.componentId}".rs,
          modifier = modifier,
          color = Color.Red.rc,
          fontSize = 10.sp.asRemoteTextUnit(),
        )
    }
  }
}

/**
 * The alignment every child asks its container for with a [modifierType] modifier, when they agree.
 *
 * `RemoteBox`, `RemoteRow` and `RemoteColumn` align their content as a group, so a child's own
 * alignment becomes the container's argument, which is how the exported Kotlin writes it too.
 */
private fun List<CanvasRenderNode>.sharedAlignment(modifierType: String): String? = map { child ->
  child.node.modifiers
    .mapNotNull { it as? JsonObject }
    .firstOrNull { (it["type"] as? JsonPrimitive)?.contentOrNull == modifierType }
    ?.let { (it["alignment"] as? JsonPrimitive)?.contentOrNull }
}
  .distinct()
  .singleOrNull()

private fun String.horizontal(): RemoteAlignment.Horizontal =
  when (this) {
    "center" -> RemoteAlignment.CenterHorizontally
    "end" -> RemoteAlignment.End
    else -> RemoteAlignment.Start
  }

private fun String.vertical(): RemoteAlignment.Vertical =
  when (this) {
    "top" -> RemoteAlignment.Top
    "bottom" -> RemoteAlignment.Bottom
    else -> RemoteAlignment.CenterVertically
  }

private fun UiBuilderDocument.initialState(): Map<String, String?> =
  stateVariables.mapValues { (_, declaration) ->
    ((declaration as? JsonObject)?.get("initialValue") as? JsonPrimitive)?.contentOrNull
  }

private fun UiBuilderNode.widgetSize(): WearWidgetScaffoldSize? =
  WearWidgetScaffoldSize.entries.firstOrNull { it.componentId == componentId }

private fun UiBuilderDocument.playerTheme(): RcPlayerTheme =
  when ((environment["theme"] as? JsonPrimitive)?.contentOrNull) {
    "light" -> RcPlayerTheme.Light
    "dark" -> RcPlayerTheme.Dark
    else -> RcPlayerTheme.System
  }

private fun UiBuilderNode.value(name: String): JsonPrimitive? =
  ((properties[name] as? JsonObject)?.get("value") as? JsonPrimitive)

private fun UiBuilderNode.string(name: String): String = value(name)?.contentOrNull.orEmpty()

private fun UiBuilderNode.number(name: String): Float? = value(name)?.floatOrNull

private fun UiBuilderNode.integer(name: String): Int? = value(name)?.intOrNull

private fun UiBuilderNode.boolean(name: String, fallback: Boolean): Boolean =
  value(name)?.booleanOrNull ?: fallback

@Composable
private fun UiBuilderNode.remoteModifier(
  values: DocumentValues,
  row: RemoteRowScope? = null,
  column: RemoteColumnScope? = null,
  collapsibleColumn: RemoteCollapsibleColumnScope? = null,
  collapsibleRow: RemoteCollapsibleRowScope? = null,
): RemoteModifier {
  var result: RemoteModifier = RemoteModifier
  modifiers.forEach { element ->
    val modifier = element as? JsonObject ?: return@forEach
    val type = (modifier["type"] as? JsonPrimitive)?.contentOrNull ?: return@forEach
    fun number(vararg names: String): Float? = names.firstNotNullOfOrNull {
      (modifier[it] as? JsonPrimitive)?.floatOrNull
    }
    // The shape a `clip` names, or a `background` draws in.
    fun shape(): RemoteRoundedCornerShape? =
      (modifier["shape"] as? JsonPrimitive)?.contentOrNull?.let {
        RemoteRoundedCornerShape(namedShapeRadiusDp(it).rdp)
      }
    result =
      when (type) {
        "fillMaxSize" -> result.fillMaxSize()
        "fillMaxWidth" -> result.fillMaxWidth()
        "fillMaxHeight" -> result.fillMaxHeight()
        "width" -> number("widthDp", "value")?.let { result.width(it.rdp) } ?: result
        "height" -> number("heightDp", "value")?.let { result.height(it.rdp) } ?: result
        "size" -> {
          val both = number("sizeDp", "value")
          val width = number("widthDp") ?: both
          val height = number("heightDp") ?: both
          var sized = result
          if (width != null) sized = sized.width(width.rdp)
          if (height != null) sized = sized.height(height.rdp)
          sized
        }
        "padding" -> {
          val all = number("allDp", "value")
          if (all != null) result.padding(all.rdp)
          else
            result.padding(
              start = (number("startDp", "horizontalDp") ?: 0f).rdp,
              top = (number("topDp", "verticalDp") ?: 0f).rdp,
              end = (number("endDp", "horizontalDp") ?: 0f).rdp,
              bottom = (number("bottomDp", "verticalDp") ?: 0f).rdp,
            )
        }
        // A member of the container's scope, as in the exported Kotlin; outside a row or column
        // it means nothing and is dropped rather than failing the preview.
        "weight" -> {
          val weight = number("weight", "value") ?: 1f
          row?.run { result.weight(weight.rf) }
            ?: column?.run { result.weight(weight.rf) }
            ?: collapsibleColumn?.run { result.weight(weight.rf) }
            ?: collapsibleRow?.run { result.weight(weight.rf) }
            ?: result
        }
        // Members of the collapsible scopes, like `weight`; meaningless anywhere else.
        "collapsiblePriority" -> {
          val priority = number("priority") ?: 0f
          collapsibleColumn?.run { result.collapsiblePriority(priority) }
            ?: collapsibleRow?.run { result.collapsiblePriority(priority) }
            ?: result
        }
        // The export writes `animationSpec(Int, Boolean)` because the native lane's
        // alpha19 predates `sharedElement`; the port this preview records with has both, and they
        // lower to the same AnimationSpec operation with the same default motion.
        "sharedElement" ->
          number("key")?.toInt()?.takeIf { it >= 1 }?.let { result.sharedElement(key = it) }
            ?: result
        // Each may be computed, and then plays as the expression the export writes.
        "alpha" -> result.alpha(values.float(modifier["alpha"].wrapped()) ?: 1f.rf)
        "rotate" -> result.rotate(values.float(modifier["degrees"].wrapped()) ?: 0f.rf)
        "scale" ->
          result.scale(
            values.float(modifier["scaleX"].wrapped()) ?: 1f.rf,
            values.float(modifier["scaleY"].wrapped()) ?: 1f.rf,
          )
        "zIndex" -> result.zIndex(values.float(modifier["zIndex"].wrapped()) ?: 0f.rf)
        "offset" -> result.offset((number("xDp") ?: 0f).rdp, (number("yDp") ?: 0f).rdp)
        "widthIn" -> result.widthIn(number("minDp")?.rdp, number("maxDp")?.rdp)
        "heightIn" -> result.heightIn(number("minDp")?.rdp, number("maxDp")?.rdp)
        "wrapContentSize" -> result.wrapContentSize()
        "border" -> {
          val color = modifier["color"].modifierColor()?.remoteColor() ?: Color.Transparent.rc
          result.border(
            (number("widthDp") ?: 1f).rdp,
            color,
            shape() ?: RemoteRoundedCornerShape(0f.rdp),
          )
        }
        "clip" -> result.clip(shape() ?: RemoteRoundedCornerShape(0f.rdp))
        "background" -> {
          val color = modifier["color"].modifierColor()?.remoteColor()
          val clipped = shape()?.let { result.clip(it) } ?: result
          color?.let { clipped.background(it) } ?: clipped
        }
        // Read by the parent: a remote container aligns its content as a group (`sharedAlignment`).
        "align",
        "alignVertical",
        "alignHorizontal" -> result
        RemoteModifierVocabulary.TYPE -> result.remoteCall(modifier, values)
        else -> error("Unsupported Remote Compose modifier '$type' on ${componentId}")
      }
  }
  return result
}

/**
 * The corner radius a stored shape names: a number of dp, or one of the size words.
 *
 * The words resolve to what the exported Kotlin writes for them (`RemoteContentEmitter`), because
 * that is the widget that ships; the authoring canvas uses the same table so switching between it
 * and a device preview never changes a component's geometry.
 */
internal fun namedShapeRadiusDp(declared: String?): Float =
  when (declared) {
    "large" -> 16f
    "medium" -> 12f
    "small" -> 8f
    else -> declared?.toFloatOrNull() ?: 0f
  }

/**
 * A modifier field — a bare number or a computed wrapper — as the wrapper [DocumentValues] reads.
 */
private fun kotlinx.serialization.json.JsonElement?.wrapped():
  kotlinx.serialization.json.JsonElement? =
  when (this) {
    is JsonPrimitive -> JsonObject(mapOf("type" to JsonPrimitive("float"), "value" to this))
    else -> this
  }

/** A modifier's colour, written either bare or as the `{"type":"color","value":…}` wrapper. */
private fun kotlinx.serialization.json.JsonElement?.modifierColor(): String? =
  when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> (this["value"] as? JsonPrimitive)?.contentOrNull
    else -> null
  }

@Composable
private fun UiBuilderNode.remoteColor(name: String): RemoteColor? =
  value(name)?.contentOrNull?.remoteColor()

@Composable
private fun String.remoteColor(): RemoteColor? =
  parseColor()?.rc
    ?: when (this) {
      "background" -> RemoteMaterialTheme.colorScheme.background
      "surface" -> RemoteMaterialTheme.colorScheme.surfaceContainer
      "surfaceContainer" -> RemoteMaterialTheme.colorScheme.surfaceContainer
      "surfaceContainerLow" -> RemoteMaterialTheme.colorScheme.surfaceContainerLow
      "surfaceContainerHigh" -> RemoteMaterialTheme.colorScheme.surfaceContainerHigh
      "surfaceContainerHighest" -> RemoteMaterialTheme.colorScheme.surfaceContainerHigh
      "primary" -> RemoteMaterialTheme.colorScheme.primary
      "onPrimary" -> RemoteMaterialTheme.colorScheme.onPrimary
      "tertiary" -> RemoteMaterialTheme.colorScheme.tertiary
      "onTertiary" -> RemoteMaterialTheme.colorScheme.onTertiary
      "onSurface" -> RemoteMaterialTheme.colorScheme.onSurface
      "onSurfaceVariant" -> RemoteMaterialTheme.colorScheme.onSurfaceVariant
      "outlineVariant" -> RemoteMaterialTheme.colorScheme.outlineVariant
      "transparent" -> Color.Transparent.rc
      else -> null
    }

private fun UiBuilderNode.color(name: String): Color? =
  value(name)?.contentOrNull?.let { word ->
    word.parseColor()
      ?: when (word) {
        "background" -> Color(0xFF1A1110)
        "surface" -> Color(0xFF1A1110)
        "surfaceContainer" -> Color(0xFF2A2220)
        "primary" -> Color(0xFFFFB4A8)
        "onPrimary" -> Color(0xFF561E18)
        "onSurface" -> Color(0xFFF1DFDB)
        "onSurfaceVariant" -> Color(0xFFD8C2BD)
        "tertiary" -> Color(0xFFE7C089)
        "onTertiary" -> Color(0xFF442B03)
        "transparent" -> Color.Transparent
        else -> null
      }
  }

private fun String.parseColor(): Color? = takeIf {
  it.startsWith("#")
}
  ?.removePrefix("#")
  ?.let { hex ->
    runCatching {
      when (hex.length) {
        6 -> (0xFF000000u or hex.toUInt(16)).toLong()
        8 -> hex.toUInt(16).toLong()
        else -> return null
      }
    }
      .getOrNull()
  }
  ?.let(::Color)

private fun UiBuilderNode.textAlign(): TextAlign? =
  when (string("textAlign").ifEmpty { string("alignment") }) {
    "center" -> TextAlign.Center
    "end" -> TextAlign.End
    "justify" -> TextAlign.Justify
    "start" -> TextAlign.Start
    else -> null
  }

/**
 * The role a widget container's `themeTextStyle` names, which a text with no `style` of its own is
 * set in. Empty keeps the theme's own, `bodyLarge`.
 */
private val LocalThemeTextRole = staticCompositionLocalOf { "" }

/**
 * The node's `style`, else its theme's default role, else `bodyLarge` — what `RemoteMaterialTheme`
 * provides, and so what the generated widget's unstyled `RemoteText` is set in on the device.
 */
@Composable
private fun UiBuilderNode.textStyle(): RemoteTextStyle =
  textRole(string("style"))
    ?: textRole(LocalThemeTextRole.current)
    ?: RemoteMaterialTheme.typography.bodyLarge

/** [role]'s style, with Material 3's headline roles as Wear's titles, or null for no Wear role. */
@Composable
private fun textRole(role: String): RemoteTextStyle? =
  when (role) {
    "displayLarge" -> RemoteMaterialTheme.typography.displayLarge
    "displayMedium" -> RemoteMaterialTheme.typography.displayMedium
    "displaySmall" -> RemoteMaterialTheme.typography.displaySmall
    "headlineLarge",
    "titleLarge" -> RemoteMaterialTheme.typography.titleLarge
    "headlineMedium",
    "titleMedium" -> RemoteMaterialTheme.typography.titleMedium
    "headlineSmall",
    "titleSmall" -> RemoteMaterialTheme.typography.titleSmall
    "labelLarge" -> RemoteMaterialTheme.typography.labelLarge
    "labelMedium" -> RemoteMaterialTheme.typography.labelMedium
    "labelSmall" -> RemoteMaterialTheme.typography.labelSmall
    "bodyLarge" -> RemoteMaterialTheme.typography.bodyLarge
    "bodyMedium" -> RemoteMaterialTheme.typography.bodyMedium
    "bodySmall" -> RemoteMaterialTheme.typography.bodySmall
    "bodyExtraSmall" -> RemoteMaterialTheme.typography.bodyExtraSmall
    "numeralExtraLarge" -> RemoteMaterialTheme.typography.numeralExtraLarge
    "numeralLarge" -> RemoteMaterialTheme.typography.numeralLarge
    "numeralMedium" -> RemoteMaterialTheme.typography.numeralMedium
    "numeralSmall" -> RemoteMaterialTheme.typography.numeralSmall
    "numeralExtraSmall" -> RemoteMaterialTheme.typography.numeralExtraSmall
    else -> null
  }

private fun UiBuilderNode.boxAlignment(): RemoteAlignment =
  string("contentAlignment").boxAlignment()

private fun String.boxAlignment(): RemoteAlignment =
  when (this) {
    "center" -> RemoteAlignment.Center
    "topCenter" -> RemoteAlignment.TopCenter
    "topEnd" -> RemoteAlignment.TopEnd
    "centerStart" -> RemoteAlignment.CenterStart
    "centerEnd" -> RemoteAlignment.CenterEnd
    "bottomStart" -> RemoteAlignment.BottomStart
    "bottomCenter" -> RemoteAlignment.BottomCenter
    "bottomEnd" -> RemoteAlignment.BottomEnd
    else -> RemoteAlignment.TopStart
  }

/**
 * The node's `fontVariationSettings`, as the generated widget writes them
 * (`RemoteText(fontVariationSettings = …)`): the document carries the axes and the player applies
 * them to the face it resolves ([RegistryTypefaceLoader]).
 */
private fun UiBuilderNode.fontVariationSettings(): FontVariation.Settings? =
  FontSettings.parseVariations(string(FontSettings.VARIATION_PROPERTY))
    .takeIf { it.isNotEmpty() }
    ?.let { axes ->
      FontVariation.Settings(*axes.map { FontVariation.Setting(it.tag, it.value) }.toTypedArray())
    }

/**
 * [this] with the node's `fontFeatureSettings` merged in, as the generated widget writes them:
 * `RemoteText` has no feature parameter, so they ride on the style into the document.
 */
private fun RemoteTextStyle.withFontFeatures(node: UiBuilderNode): RemoteTextStyle =
  FontSettings.formatFeatures(
      FontSettings.parseFeatures(node.string(FontSettings.FEATURE_PROPERTY))
    )
    .takeIf { it.isNotEmpty() }
    ?.let { merge(fontFeatureSettings = it) } ?: this

private fun UiBuilderNode.fontWeight(): FontWeight? =
  when (string("fontWeight")) {
    "thin" -> FontWeight.Thin
    "light" -> FontWeight.Light
    "normal" -> FontWeight.Normal
    "medium" -> FontWeight.Medium
    "semiBold" -> FontWeight.SemiBold
    "bold" -> FontWeight.Bold
    "black" -> FontWeight.Black
    else -> string("fontWeight").removePrefix("w").toIntOrNull()?.let { FontWeight(it) }
  }

private fun UiBuilderNode.textOverflow(): TextOverflow =
  when (string("overflow")) {
    "ellipsis" -> TextOverflow.Ellipsis
    "visible" -> TextOverflow.Visible
    else -> TextOverflow.Clip
  }

private fun UiBuilderNode.horizontalAlignment(): RemoteAlignment.Horizontal =
  when (string("horizontalAlignment")) {
    "center" -> RemoteAlignment.CenterHorizontally
    "end" -> RemoteAlignment.End
    else -> RemoteAlignment.Start
  }

private fun UiBuilderNode.verticalAlignment(): RemoteAlignment.Vertical =
  when (string("verticalAlignment")) {
    "top" -> RemoteAlignment.Top
    "bottom" -> RemoteAlignment.Bottom
    else -> RemoteAlignment.CenterVertically
  }

private fun UiBuilderNode.verticalArrangement(): RemoteArrangement.Vertical =
  when (string("verticalArrangement")) {
    "center" ->
      RemoteArrangement.spacedBy(
        (number("verticalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.CenterVertically,
      )
    "bottom" ->
      RemoteArrangement.spacedBy(
        (number("verticalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.Bottom,
      )
    "spaceBetween" -> RemoteArrangement.SpaceBetween
    "spaceAround" -> RemoteArrangement.SpaceAround
    "spaceEvenly" -> RemoteArrangement.SpaceEvenly
    else ->
      RemoteArrangement.spacedBy(
        (number("verticalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.Top,
      )
  }

private fun UiBuilderNode.horizontalArrangement(): RemoteArrangement.Horizontal =
  when (string("horizontalArrangement")) {
    "center" ->
      RemoteArrangement.spacedBy(
        (number("horizontalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.CenterHorizontally,
      )
    "end" ->
      RemoteArrangement.spacedBy(
        (number("horizontalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.End,
      )
    "spaceBetween" -> RemoteArrangement.SpaceBetween
    "spaceAround" -> RemoteArrangement.SpaceAround
    "spaceEvenly" -> RemoteArrangement.SpaceEvenly
    else ->
      RemoteArrangement.spacedBy(
        (number("horizontalSpacingDp") ?: 0f).rdp,
        RemoteAlignment.Start,
      )
  }

/**
 * `remote-catalog/ui-builder.policy.json`'s default for `remote-m3/remote-icon`'s `imageVector`.
 */
private const val REMOTE_ICON_DEFAULT_KEY = "addCircle"
