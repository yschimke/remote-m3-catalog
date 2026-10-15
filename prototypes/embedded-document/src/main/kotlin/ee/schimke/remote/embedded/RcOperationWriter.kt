package ee.schimke.remote.embedded

import androidx.compose.remote.creation.common.NamedVariableType
import androidx.compose.remote.creation.common.RemoteModifierData
import androidx.compose.remote.creation.common.RemoteModifierOperation
import androidx.compose.remote.creation.common.RemoteTextData
import androidx.compose.remote.creation.common.RemoteWriter
import androidx.compose.remote.creation.common.Utils
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.wireValue
import ee.schimke.composeai.rcplayer.protocol.RcBackgroundModifier
import ee.schimke.composeai.rcplayer.protocol.RcBoxLayout
import ee.schimke.composeai.rcplayer.protocol.RcClipRectModifier
import ee.schimke.composeai.rcplayer.protocol.RcColorConstant
import ee.schimke.composeai.rcplayer.protocol.RcColumnLayout
import ee.schimke.composeai.rcplayer.protocol.RcCoreText
import ee.schimke.composeai.rcplayer.protocol.RcDocument
import ee.schimke.composeai.rcplayer.protocol.RcFloatExpression
import ee.schimke.composeai.rcplayer.protocol.RcFloatWord
import ee.schimke.composeai.rcplayer.protocol.RcHeader
import ee.schimke.composeai.rcplayer.protocol.RcHeaderProperty
import ee.schimke.composeai.rcplayer.protocol.RcHeaderValue
import ee.schimke.composeai.rcplayer.protocol.RcHeightInModifier
import ee.schimke.composeai.rcplayer.protocol.RcHeightModifier
import ee.schimke.composeai.rcplayer.protocol.RcLayoutContent
import ee.schimke.composeai.rcplayer.protocol.RcNamedVariable
import ee.schimke.composeai.rcplayer.protocol.RcNoArg
import ee.schimke.composeai.rcplayer.protocol.RcOpcodes
import ee.schimke.composeai.rcplayer.protocol.RcOperation
import ee.schimke.composeai.rcplayer.protocol.RcPaddingModifier
import ee.schimke.composeai.rcplayer.protocol.RcRootLayout
import ee.schimke.composeai.rcplayer.protocol.RcRoundedClipRectModifier
import ee.schimke.composeai.rcplayer.protocol.RcRowLayout
import ee.schimke.composeai.rcplayer.protocol.RcTextData
import ee.schimke.composeai.rcplayer.protocol.RcTextStyleProperty
import ee.schimke.composeai.rcplayer.protocol.RcVersion
import ee.schimke.composeai.rcplayer.protocol.RcWidthInModifier
import ee.schimke.composeai.rcplayer.protocol.RcWidthModifier
import java.lang.reflect.Proxy

/**
 * PROTOTYPE: a [RemoteWriter] that builds the rc-players wire model ([RcDocument]) directly,
 * instead of bytes.
 *
 * It mirrors `RemoteDocumentWriter` call for call, so the same composition gives the same
 * operations: the same id allocation (data ids from 42, component ids counting down from -1), the
 * same dedup of texts and float expressions, and the same split of declarations (hoisted after the
 * header) from the body. `RcDocumentCodec.encode(document())` is therefore byte-identical to
 * `RemoteDocumentWriter.encodeToByteArray()`, which is what the tests hold it to.
 *
 * Only the layout/text/modifier/data subset a typical editor screen needs is implemented. Every
 * other [RemoteWriter] call is routed to [unsupported], which throws naming the method, so a caller
 * can fall back to the byte path instead of rendering something silently wrong.
 */
class RcOperationWriter(
  private val width: Int,
  private val height: Int,
  private val densityBehavior: Int,
  private val profiles: Int = 0,
) : RemoteWriter by unsupported() {
  private val declarations = ArrayList<RcOperation>()
  private val body = ArrayList<RcOperation>()

  private var nextComponentId = -1
  private var lastComponentId = -1
  private var nextDataId = 42
  private val textIds = HashMap<String, Int>()
  private val floatExpressionIds = HashMap<List<Int>, Int>()

  /** Sized for [display], as `captureCommonRemoteDocument` sizes its byte writer. */
  constructor(
    display: RemoteCreationDisplayInfo
  ) : this(
    display.size.width.toInt(),
    display.size.height.toInt(),
    display.densityBehavior.wireValue,
  )

  /** The document as the player consumes it; equal to `RcDocumentCodec.decode` of the bytes. */
  fun document(): RcDocument = RcDocument(header(), declarations + body)

  private fun header(): RcHeader =
    RcHeader(
      RcVersion(1, 1, 0),
      listOf(
        RcHeaderProperty(5, RcHeaderValue.IntValue(width)),
        RcHeaderProperty(6, RcHeaderValue.IntValue(height)),
        RcHeaderProperty(9, RcHeaderValue.StringValue("")),
        RcHeaderProperty(14, RcHeaderValue.IntValue(profiles)),
        RcHeaderProperty(27, RcHeaderValue.IntValue(densityBehavior)),
      ),
      modern = true,
    )

  // --- ids ---------------------------------------------------------------------------------

  override val componentIdForCache: Int
    get() = lastComponentId

  override fun createFloatId(): Float = Utils.asNan(nextDataId++)

  private fun componentId(): Int {
    lastComponentId = --nextComponentId
    return lastComponentId
  }

  private fun resolveComponentId(requested: Int): Int {
    lastComponentId = if (requested == -1) --nextComponentId else requested
    return lastComponentId
  }

  // --- structure ---------------------------------------------------------------------------

  override fun startRoot() {
    body += RcRootLayout(componentId())
  }

  override fun endRoot(): Unit = containerEnd()

  override fun startBox(modifier: RemoteModifierData, horizontal: Int, vertical: Int) {
    body += RcBoxLayout(resolveComponentId(modifier.componentId), -1, horizontal, vertical)
    modifiersThenContent(modifier)
  }

  override fun endBox(): Unit = endLayout()

  override fun startRow(modifier: RemoteModifierData, horizontal: Int, vertical: Int) {
    body +=
      RcRowLayout(
        resolveComponentId(modifier.componentId),
        -1,
        horizontal,
        vertical,
        RcFloatWord.literal(modifier.spacedBy),
      )
    modifiersThenContent(modifier)
  }

  override fun endRow(): Unit = endLayout()

  override fun startColumn(modifier: RemoteModifierData, horizontal: Int, vertical: Int) {
    body +=
      RcColumnLayout(
        resolveComponentId(modifier.componentId),
        -1,
        horizontal,
        vertical,
        RcFloatWord.literal(modifier.spacedBy),
      )
    modifiersThenContent(modifier)
  }

  override fun endColumn(): Unit = endLayout()

  override fun startText(data: RemoteTextData) {
    require(data.fontAxisIds.isEmpty()) { "prototype: font axes are not mirrored yet" }
    val properties = ArrayList<RcTextStyleProperty>()
    fun int(id: Int, value: Int, default: Int) {
      if (value != default) properties += RcTextStyleProperty.IntValue(id, value)
    }
    fun float(id: Int, value: Float, default: Float) {
      if (value != default) {
        properties += RcTextStyleProperty.FloatValue(id, RcFloatWord.literal(value))
      }
    }
    fun boolean(id: Int, value: Boolean) {
      if (value) properties += RcTextStyleProperty.BooleanValue(id, true)
    }
    // Same order and defaults as RemoteDocumentWriter.startText: the wire is order-sensitive.
    int(1, resolveComponentId(data.modifier.componentId), -1)
    int(3, data.color, 0xff000000.toInt())
    int(4, data.colorId, -1)
    float(5, data.fontSize, 36f)
    float(25, data.minFontSize, -1f)
    float(26, data.maxFontSize, -1f)
    int(6, data.fontStyle, 0)
    float(7, data.fontWeight, 400f)
    int(8, data.fontFamilyId, -1)
    int(9, data.textAlign, 1)
    int(10, data.overflow, 1)
    int(11, data.maxLines, Int.MAX_VALUE)
    float(12, data.letterSpacing, 0f)
    float(13, data.lineHeightAdd, 0f)
    float(14, data.lineHeightMultiplier, 1f)
    int(15, data.lineBreakStrategy, 0)
    int(16, data.hyphenationFrequency, 0)
    int(17, data.justificationMode, 0)
    boolean(18, data.underline)
    boolean(19, data.strikethrough)
    boolean(22, data.autosize)
    int(23, data.flags, 0)
    int(24, data.textStyleId, -1)
    body += RcCoreText(data.textId, properties)
    modifiersThenContent(data.modifier)
  }

  override fun endText(): Unit = endLayout()

  override fun writeModifier(operation: RemoteModifierOperation) {
    body +=
      when (operation) {
        is RemoteModifierOperation.Width -> RcWidthModifier(operation.type, word(operation.value))
        is RemoteModifierOperation.Height -> RcHeightModifier(operation.type, word(operation.value))
        is RemoteModifierOperation.Padding ->
          RcPaddingModifier(
            word(operation.left),
            word(operation.top),
            word(operation.right),
            word(operation.bottom),
          )
        is RemoteModifierOperation.Background ->
          RcBackgroundModifier(
            operation.flags,
            operation.colorId,
            0,
            0,
            word(operation.red),
            word(operation.green),
            word(operation.blue),
            word(operation.alpha),
            operation.shape,
          )
        RemoteModifierOperation.ClipRect -> RcClipRectModifier
        is RemoteModifierOperation.RoundedClipRect ->
          RcRoundedClipRectModifier(
            word(operation.topStart),
            word(operation.topEnd),
            word(operation.bottomStart),
            word(operation.bottomEnd),
          )
        is RemoteModifierOperation.WidthIn ->
          RcWidthInModifier(word(operation.min), word(operation.max))
        is RemoteModifierOperation.HeightIn ->
          RcHeightInModifier(word(operation.min), word(operation.max))
        else -> throw UnsupportedOperationException("prototype: operation $operation")
      }
  }

  private fun modifiersThenContent(modifier: RemoteModifierData) {
    modifier.writeTo(this)
    body += RcLayoutContent(componentId())
  }

  private fun containerEnd() {
    body += RcNoArg(RcOpcodes.CONTAINER_END)
  }

  private fun endLayout() {
    containerEnd()
    containerEnd()
  }

  // --- data (hoisted declarations) ---------------------------------------------------------

  override fun addText(value: String): Int =
    textIds.getOrPut(value) { nextDataId++.also { declarations += RcTextData(it, value) } }

  override fun addColor(argb: Int): Int =
    nextDataId++.also { declarations += RcColorConstant(it, argb) }

  override fun addNamedColor(name: String, argb: Int): Int =
    addColor(argb).also { setNamedVariable(it, name, NamedVariableType.COLOR) }

  override fun setNamedVariable(id: Int, name: String, type: Int) {
    declarations += RcNamedVariable(id, type, name)
  }

  override fun floatExpression(vararg values: Float): Float = floatExpression(values, null)

  override fun floatExpression(values: FloatArray, animation: FloatArray?): Float {
    val key = values.map { it.toRawBits() }
    val id =
      floatExpressionIds.getOrPut(key) {
        nextDataId++.also {
          declarations += RcFloatExpression(it, values.map(::word), animation?.map(::word))
        }
      }
    return Utils.asNan(id)
  }

  private fun word(value: Float): RcFloatWord = RcFloatWord(value.toRawBits())
}

/** Every [RemoteWriter] call the prototype has not mirrored yet fails loudly, naming itself. */
private fun unsupported(): RemoteWriter =
  Proxy.newProxyInstance(RemoteWriter::class.java.classLoader, arrayOf(RemoteWriter::class.java)) {
    _,
    method,
    _ ->
    throw UnsupportedOperationException("prototype: RemoteWriter.${method.name} is not mirrored")
  } as RemoteWriter
