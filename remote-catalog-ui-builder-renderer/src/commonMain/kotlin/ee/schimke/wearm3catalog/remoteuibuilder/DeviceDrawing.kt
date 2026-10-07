package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.path.RemotePath
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.radialGradient
import androidx.compose.remote.creation.compose.shaders.sweepGradient
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.PathParser
import ee.schimke.composeai.uibuilder.export.UiBuilderNode
import ee.schimke.composeai.uibuilder.export.UiDrawing
import ee.schimke.composeai.uibuilder.renderer.sdk.CanvasRenderNode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

/**
 * A `draw/canvas` recorded as the `RemoteCanvas` the exported widget writes, call for call, so the
 * device preview plays the drawing — computed geometry included — rather than a picture of it.
 *
 * Colours are read in composition first ([resolveColor] reaches the theme, which the draw lambda
 * cannot), then every operation is drawn in order under the export's rules: dp from the canvas's
 * top-left, the stated canvas size when there is one, a defaulted stroked box inset by half its
 * stroke.
 */
@Composable
@RemoteComposable
internal fun DeviceDrawCanvas(
  entry: CanvasRenderNode,
  modifier: RemoteModifier,
  values: DocumentValues,
  resolveColor: @Composable (String) -> RemoteColor?,
) {
  val operations = collect(entry.slot(UiDrawing.OPS_SLOT), values, resolveColor)
  val stated = entry.node.statedSizeDp()
  RemoteCanvas(modifier = modifier) {
    val extent =
      Extent(
        stated.first?.let { it.rdp.toPx() } ?: width,
        stated.second?.let { it.rdp.toPx() } ?: height,
      )
    operations.forEach { it.draw(this, extent, values) }
  }
}

private class Extent(val width: RemoteFloat, val height: RemoteFloat) {
  val centerX: RemoteFloat
    get() = width / 2f.rf

  val centerY: RemoteFloat
    get() = height / 2f.rf
}

private class Operation(
  val node: UiBuilderNode,
  val color: RemoteColor,
  /** Where a gradient paint ends, resolved in composition beside [color]; null for none. */
  val gradientColor: RemoteColor?,
  val children: List<Operation>,
) {
  private fun RemoteDrawScope.drawChildren(extent: Extent, values: DocumentValues) =
    children.forEach {
      it.draw(this, extent, values)
    }

  fun draw(scope: RemoteDrawScope, extent: Extent, values: DocumentValues) {
    with(scope) {
      fun px(name: String): RemoteFloat? = values.float(node.properties[name])?.asRemoteDp()?.toPx()
      fun float(name: String): RemoteFloat? = values.float(node.properties[name])
      when (node.componentId) {
        UiDrawing.GROUP -> {
          val pivot =
            RemoteOffset(px("pivotXDp") ?: extent.centerX, px("pivotYDp") ?: extent.centerY)
          withTransform({
            translate(px("translateXDp") ?: 0f.rf, px("translateYDp") ?: 0f.rf)
            float("rotate")?.let { rotate(it, pivot) }
            float("scale")?.let { scale(it, it, pivot) }
          }) {
            drawChildren(extent, values)
          }
          return
        }
        UiDrawing.CLIP -> {
          val clipOp = if (node.flag("exclude")) ClipOp.Difference else ClipOp.Intersect
          val outline = node.text("pathData")?.let(::remotePath)
          if (outline != null) {
            // As the export writes it: into the viewport, clip, and back out.
            val sx = extent.width / (node.number("viewportWidth")?.takeIf { it > 0f } ?: 24f).rf
            val sy = extent.height / (node.number("viewportHeight")?.takeIf { it > 0f } ?: 24f).rf
            withTransform({
              scale(sx, sy)
              clipPath(outline, clipOp)
              scale(1f.rf / sx, 1f.rf / sy)
            }) {
              drawChildren(extent, values)
            }
          } else {
            val x = px("xDp") ?: 0f.rf
            val y = px("yDp") ?: 0f.rf
            clipRect(
              x,
              y,
              px("widthDp")?.let { x + it } ?: extent.width,
              px("heightDp")?.let { y + it } ?: extent.height,
              clipOp,
            ) {
              drawChildren(extent, values)
            }
          }
          return
        }
        UiDrawing.IF -> {
          val condition = values.bool(node.properties["condition"]) ?: return
          drawConditionally(condition) { drawChildren(extent, values) }
          return
        }
        UiDrawing.REPEAT -> {
          val name = UiDrawing.indexName(node) ?: return
          val until = float("until") ?: return
          loop(float("from") ?: 0f.rf, until, float("step") ?: 1f.rf) { index ->
            children.forEach { it.draw(this, extent, values.withBindings(mapOf(name to index))) }
          }
          return
        }
      }
      val stroked = node.text("style") == "stroke" || node.componentId == "draw/line"
      val strokeWidth = px("strokeWidthDp") ?: 1f.rdp.toPx()
      val paint = RemotePaint {
        color = this@Operation.color
        if (stroked) {
          style = PaintingStyle.Stroke
          this.strokeWidth = strokeWidth
        }
        when (node.text("strokeCap")) {
          "round" -> strokeCap = StrokeCap.Round
          "square" -> strokeCap = StrokeCap.Square
        }
        when (node.componentId) {
          "draw/text",
          UiDrawing.TEXT_CIRCLE -> textSize = px("textSizeSp") ?: 14f.rdp.toPx()
          // Drawn inside a one-dp scale, so the size is divided back out.
          UiDrawing.TEXT_PATH -> textSize = (px("textSizeSp") ?: 14f.rdp.toPx()) / 1f.rdp.toPx()
        }
        val end = gradientColor
        val kind = node.text("gradient")
        if (end != null && kind != null) {
          val brush =
            when (kind) {
              "horizontal" -> RemoteBrush.horizontalGradient(listOf(this@Operation.color, end))
              "vertical" -> RemoteBrush.verticalGradient(listOf(this@Operation.color, end))
              "radial" -> RemoteBrush.radialGradient(listOf(this@Operation.color, end))
              else -> RemoteBrush.sweepGradient(listOf(this@Operation.color, end))
            }
          with(brush) { applyTo(this@RemotePaint, RemoteSize(extent.width, extent.height)) }
        }
      }
      val boxStated = listOf("xDp", "yDp", "widthDp", "heightDp").any { it in node.properties }
      val inset = if (stroked && !boxStated) strokeWidth / 2f.rf else null
      val x = px("xDp") ?: inset ?: 0f.rf
      val y = px("yDp") ?: inset ?: 0f.rf
      val boxWidth = px("widthDp") ?: inset?.let { extent.width - it * 2f.rf } ?: (extent.width - x)
      val boxHeight =
        px("heightDp") ?: inset?.let { extent.height - it * 2f.rf } ?: (extent.height - y)
      val topLeft = RemoteOffset(x, y)
      val size = RemoteSize(boxWidth, boxHeight)
      when (node.componentId) {
        "draw/rect" -> {
          val radius = px("cornerRadiusDp")
          if (radius == null) drawRect(paint, topLeft, size)
          else drawRoundRect(paint, topLeft, size, RemoteOffset(radius, radius))
        }
        "draw/oval" -> drawOval(paint, topLeft, size)
        "draw/arc" ->
          drawArc(
            paint,
            startAngle = float("startAngle") ?: 0f.rf,
            sweepAngle = float("sweepAngle") ?: 360f.rf,
            useCenter = node.flag("useCenter"),
            topLeft = topLeft,
            size = size,
          )
        "draw/circle" ->
          drawCircle(
            paint,
            radius =
              px("radiusDp")
                ?: (min(extent.width, extent.height) / 2f.rf).let { r ->
                  inset?.let { r - it } ?: r
                },
            center =
              RemoteOffset(px("centerXDp") ?: extent.centerX, px("centerYDp") ?: extent.centerY),
          )
        "draw/line" ->
          drawLine(
            paint,
            start = RemoteOffset(px("startXDp") ?: 0f.rf, px("startYDp") ?: 0f.rf),
            end = RemoteOffset(px("endXDp") ?: extent.width, px("endYDp") ?: extent.height),
          )
        "draw/path" ->
          node.text("pathData")?.let(::remotePath)?.let { path ->
            val viewportWidth = node.number("viewportWidth")?.takeIf { it > 0f } ?: 24f
            val viewportHeight = node.number("viewportHeight")?.takeIf { it > 0f } ?: 24f
            withTransform({
              scale(
                extent.width / viewportWidth.rf,
                extent.height / viewportHeight.rf,
                RemoteOffset(0f, 0f),
              )
            }) {
              drawPath(path, paint)
            }
          }
        UiDrawing.MORPH -> {
          val from = node.text("pathData")?.let(::remotePath) ?: return
          val to = node.text("toPathData")?.let(::remotePath) ?: return
          val viewportWidth = node.number("viewportWidth")?.takeIf { it > 0f } ?: 24f
          val viewportHeight = node.number("viewportHeight")?.takeIf { it > 0f } ?: 24f
          withTransform({
            scale(
              extent.width / viewportWidth.rf,
              extent.height / viewportHeight.rf,
              RemoteOffset(0f, 0f),
            )
          }) {
            drawTweenPath(
              from,
              to,
              tween = clamp(float("progress") ?: 0f.rf, 0f, 1f),
              paint = paint,
            )
          }
        }
        UiDrawing.TEXT_CIRCLE ->
          drawTextOnCircle(
            values.string(node.properties["text"]) ?: "".rs,
            px("centerXDp") ?: extent.centerX,
            px("centerYDp") ?: extent.centerY,
            px("radiusDp")
              ?: (min(extent.width, extent.height) / 2f.rf - (px("textSizeSp") ?: 14f.rdp.toPx())),
            float("angle") ?: 270f.rf,
            0f.rf,
            paint,
          )
        UiDrawing.TEXT_PATH -> {
          val path = node.text("pathData")?.let(::remotePath) ?: return
          val density = 1f.rdp.toPx()
          withTransform({ scale(density, density, RemoteOffset(0f, 0f)) }) {
            drawTextOnPath(
              values.string(node.properties["text"]) ?: "".rs,
              path,
              hOffset = float("startDp") ?: 0f.rf,
              vOffset = float("offsetDp") ?: 0f.rf,
              paint = paint,
            )
          }
        }
        "draw/text" -> {
          val pan =
            when (node.text("align")) {
              "start" -> -1f
              "end" -> 1f
              else -> 0f
            }
          drawAnchoredText(
            values.string(node.properties["text"]) ?: "".rs,
            px("xDp") ?: extent.centerX,
            px("yDp") ?: extent.centerY,
            paint,
            panX = pan.rf,
            panY = 0f.rf,
          )
        }
      }
    }
  }
}

@Composable
private fun collect(
  operations: List<CanvasRenderNode>,
  values: DocumentValues,
  resolveColor: @Composable (String) -> RemoteColor?,
): List<Operation> = operations.mapNotNull { operation ->
  val node = operation.node
  if (node.componentId !in UiDrawing.BY_ID) return@mapNotNull null
  val base =
    values.color(node.properties["color"]?.takeIf { it.isComputedColour() })
      ?: node.text("color")?.let { resolveColor(it) }
      ?: Color.Black.rc
  val alpha = values.float(node.properties["alpha"])
  val color = alpha?.let { base.copy(alpha = it) } ?: base
  val gradientColor =
    node.text("gradient")?.let {
      val end =
        values.color(node.properties["gradientColor"]?.takeIf { it.isComputedColour() })
          ?: node.text("gradientColor")?.let { resolveColor(it) }
          ?: Color.Transparent.rc
      alpha?.let { end.copy(alpha = it) } ?: end
    }
  Operation(
    node,
    color,
    gradientColor,
    if (UiDrawing.BY_ID.getValue(node.componentId).container)
      collect(operation.slot(UiDrawing.OPS_SLOT), values, resolveColor)
    else emptyList(),
  )
}

private fun kotlinx.serialization.json.JsonElement.isComputedColour(): Boolean =
  (this as? JsonObject)?.get("type")?.let { (it as? JsonPrimitive)?.contentOrNull } in
    setOf("expr", "system", "state")

/** SVG path data as the port's `RemotePath`: arcs and smooth curves become cubics on the way. */
private fun remotePath(data: String): RemotePath? {
  val parsed =
    runCatching { PathParser().parsePathString(data).toPath() }.getOrNull() ?: return null
  val path = RemotePath()
  val iterator = parsed.iterator()
  while (iterator.hasNext()) {
    val segment = iterator.next()
    val p = segment.points
    when (segment.type) {
      PathSegment.Type.Move -> path.moveTo(p[0], p[1])
      PathSegment.Type.Line -> path.lineTo(p[2], p[3])
      PathSegment.Type.Quadratic -> path.quadTo(p[2], p[3], p[4], p[5])
      PathSegment.Type.Conic -> path.conicTo(p[2], p[3], p[4], p[5], segment.weight)
      PathSegment.Type.Cubic -> path.cubicTo(p[2], p[3], p[4], p[5], p[6], p[7])
      PathSegment.Type.Close -> path.close()
      PathSegment.Type.Done -> Unit
    }
  }
  return path
}

private fun UiBuilderNode.scalar(name: String): JsonPrimitive? =
  (properties[name] as? JsonObject)?.get("value") as? JsonPrimitive

private fun UiBuilderNode.number(name: String): Float? = scalar(name)?.floatOrNull

private fun UiBuilderNode.text(name: String): String? =
  scalar(name)?.contentOrNull?.takeIf(String::isNotEmpty)

private fun UiBuilderNode.flag(name: String): Boolean = scalar(name)?.booleanOrNull ?: false

private fun UiBuilderNode.statedSizeDp(): Pair<Float?, Float?> {
  var width: Float? = null
  var height: Float? = null
  modifiers.forEach { element ->
    val modifier = element as? JsonObject ?: return@forEach
    fun number(name: String) = (modifier[name] as? JsonPrimitive)?.floatOrNull
    when ((modifier["type"] as? JsonPrimitive)?.contentOrNull) {
      "size" -> {
        number("widthDp")?.let { width = it }
        number("heightDp")?.let { height = it }
      }
      "width" -> number("widthDp")?.let { width = it }
      "height" -> number("heightDp")?.let { height = it }
    }
  }
  return width to height
}
