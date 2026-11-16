package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import ee.schimke.composeai.rccreation.common.RemoteContext
import ee.schimke.composeai.rccreation.compose.action.RemoteAction
import ee.schimke.composeai.rccreation.compose.action.combinedAction
import ee.schimke.composeai.rccreation.compose.action.valueChangeAction
import ee.schimke.composeai.rccreation.compose.layout.RemoteTime
import ee.schimke.composeai.rccreation.compose.state.MutableRemoteBoolean
import ee.schimke.composeai.rccreation.compose.state.MutableRemoteFloat
import ee.schimke.composeai.rccreation.compose.state.MutableRemoteInt
import ee.schimke.composeai.rccreation.compose.state.MutableRemoteString
import ee.schimke.composeai.rccreation.compose.state.RemoteBoolean
import ee.schimke.composeai.rccreation.compose.state.RemoteColor
import ee.schimke.composeai.rccreation.compose.state.RemoteEasing
import ee.schimke.composeai.rccreation.compose.state.RemoteFloat
import ee.schimke.composeai.rccreation.compose.state.RemoteInt
import ee.schimke.composeai.rccreation.compose.state.RemoteString
import ee.schimke.composeai.rccreation.compose.state.abs
import ee.schimke.composeai.rccreation.compose.state.animateRemoteFloatAsState
import ee.schimke.composeai.rccreation.compose.state.ceil
import ee.schimke.composeai.rccreation.compose.state.clamp
import ee.schimke.composeai.rccreation.compose.state.cos
import ee.schimke.composeai.rccreation.compose.state.floor
import ee.schimke.composeai.rccreation.compose.state.lerp
import ee.schimke.composeai.rccreation.compose.state.max
import ee.schimke.composeai.rccreation.compose.state.min
import ee.schimke.composeai.rccreation.compose.state.pow
import ee.schimke.composeai.rccreation.compose.state.rb
import ee.schimke.composeai.rccreation.compose.state.rc
import ee.schimke.composeai.rccreation.compose.state.remoteSpring
import ee.schimke.composeai.rccreation.compose.state.remoteTween
import ee.schimke.composeai.rccreation.compose.state.rf
import ee.schimke.composeai.rccreation.compose.state.ri
import ee.schimke.composeai.rccreation.compose.state.round
import ee.schimke.composeai.rccreation.compose.state.rs
import ee.schimke.composeai.rccreation.compose.state.sin
import ee.schimke.composeai.rccreation.compose.state.sqrt
import ee.schimke.composeai.rccreation.compose.state.tan
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiBuilderNode
import ee.schimke.composeai.uibuilder.export.UiExpressions
import ee.schimke.composeai.uibuilder.export.UiValueKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

/**
 * A design's state, values and actions as the player's own: every typed state variable a mutable
 * remote value, every computed property the remote expression the exported widget writes, and every
 * bound action a `valueChange` — so the device preview *plays* the document the way the widget
 * will, rather than drawing the value it has at the preview state.
 *
 * Mirrors `RemoteContentEmitter`'s lowering call for call, against rc-players' creation fork, with
 * the shared [UiExpressions] typing in front of both so the two cannot disagree about what a tree
 * means.
 */
internal class DocumentValues(
  private val scope: UiExpressions.Scope,
  private val ints: Map<String, MutableRemoteInt>,
  private val floats: Map<String, MutableRemoteFloat>,
  private val bools: Map<String, MutableRemoteBoolean>,
  private val strings: Map<String, MutableRemoteString>,
  /** Loop indices in scope, by the name a formula reads them as: a `draw/repeat`'s lambda value. */
  private val bindings: Map<String, RemoteFloat> = emptyMap(),
) {
  /** These values with [extra] loop indices in scope, as a `draw/repeat` hands its operations. */
  fun withBindings(extra: Map<String, RemoteFloat>): DocumentValues {
    val all = bindings + extra
    return DocumentValues(
      UiExpressions.Scope(scope.stateKinds) { key ->
        if (key in all) UiValueKind.FLOAT else scope.bindingKinds(key)
      },
      ints,
      floats,
      bools,
      strings,
      all,
    )
  }

  /** A property as a `RemoteFloat`: a number, an Int or Float state read, or an expression. */
  fun float(value: JsonElement?): RemoteFloat? =
    when (val read = read(value, UiValueKind.FLOAT)) {
      is RemoteFloat -> read
      is RemoteInt -> read.toRemoteFloat()
      else -> null
    }

  fun int(value: JsonElement?): RemoteInt? = read(value, UiValueKind.INT) as? RemoteInt

  fun bool(value: JsonElement?): RemoteBoolean? = read(value, UiValueKind.BOOL) as? RemoteBoolean

  fun color(value: JsonElement?): RemoteColor? = read(value, UiValueKind.COLOR) as? RemoteColor

  /** A property as text: a string, a state read, or any printable expression. */
  fun string(value: JsonElement?): RemoteString? =
    read(value, UiValueKind.STRING)?.let { asString(it) }

  private fun read(value: JsonElement?, expected: UiValueKind): Any? {
    val wrapper = value as? JsonObject ?: return null
    val type = (wrapper["type"] as? JsonPrimitive)?.contentOrNull
    if (type == "state" || type == "binding" || UiExpressions.isComputed(wrapper)) {
      val checked = UiExpressions.check(wrapper, scope) as? UiExpressions.Checked.Ok ?: return null
      return lower(checked.expr)
    }
    val literal = wrapper["value"] as? JsonPrimitive ?: return null
    return when (expected) {
      UiValueKind.FLOAT -> literal.doubleOrNull?.toFloat()?.rf
      UiValueKind.INT -> literal.doubleOrNull?.toInt()?.ri
      UiValueKind.BOOL -> literal.booleanOrNull?.rb
      UiValueKind.STRING -> literal.contentOrNull?.rs
      UiValueKind.COLOR -> literal.contentOrNull?.let(::argb)?.let { Color(it).rc }
    }
  }

  private fun lower(expr: UiExpressions.Expr): Any =
    when (expr) {
      is UiExpressions.Expr.Literal ->
        when (expr.kind) {
          UiValueKind.FLOAT -> expr.value.content.toFloat().rf
          UiValueKind.INT -> expr.value.content.toInt().ri
          UiValueKind.BOOL -> expr.value.content.toBoolean().rb
          UiValueKind.STRING -> expr.value.content.rs
          UiValueKind.COLOR -> Color(argb(expr.value.content) ?: 0).rc
        }
      is UiExpressions.Expr.State ->
        when (expr.kind) {
          UiValueKind.INT -> ints.getValue(expr.variable)
          UiValueKind.FLOAT -> floats.getValue(expr.variable)
          UiValueKind.BOOL -> bools.getValue(expr.variable)
          else -> strings.getValue(expr.variable)
        }
      is UiExpressions.Expr.Binding -> bindings[expr.key] ?: 0f.rf
      is UiExpressions.Expr.System -> system(expr.value.id)
      is UiExpressions.Expr.Call -> call(expr)
    }

  private fun system(id: String): RemoteFloat =
    with(RemoteTime()) {
      when (id) {
        "time.hour" -> Hour()
        "time.minuteOfDay" -> Minutes()
        "time.secondOfHour" -> Seconds()
        "time.continuousSecond" -> ContinuousSec()
        "time.dayOfWeek" -> DayOfWeek()
        "time.dayOfMonth" -> DayOfMonth()
        "time.utcOffset" -> UtcOffset()
        // The player's own clock: seconds since it started the document.
        "time.animation" -> RemoteFloat(RemoteContext.FLOAT_ANIMATION_TIME)
        // `UiExpressions.check` refuses an id it does not know, so this is a value it learned
        // after this mapping was written: fail here rather than play it as some other clock.
        else -> error("system value `$id` has no remote mapping")
      }
    }

  private fun call(expr: UiExpressions.Expr.Call): Any {
    val args = expr.args.map(::lower)
    fun f(i: Int): RemoteFloat =
      when (val value = args[i]) {
        is RemoteInt -> value.toRemoteFloat()
        else -> value as RemoteFloat
      }
    fun i(index: Int): RemoteInt = args[index] as RemoteInt
    fun b(index: Int): RemoteBoolean = args[index] as RemoteBoolean
    val integral = expr.kind == UiValueKind.INT
    val allInts = expr.args.all { it.kind == UiValueKind.INT }
    val allBools = expr.args.all { it.kind == UiValueKind.BOOL }
    return when (expr.op) {
      UiExpressions.Op.ADD -> if (integral) i(0) + i(1) else f(0) + f(1)
      UiExpressions.Op.SUB -> if (integral) i(0) - i(1) else f(0) - f(1)
      UiExpressions.Op.MUL -> if (integral) i(0) * i(1) else f(0) * f(1)
      UiExpressions.Op.DIV -> if (integral) i(0) / i(1) else f(0) / f(1)
      UiExpressions.Op.MOD -> if (integral) i(0) % i(1) else f(0) % f(1)
      UiExpressions.Op.NEG -> if (integral) -i(0) else -f(0)
      UiExpressions.Op.MIN -> if (integral) min(i(0), i(1)) else min(f(0), f(1))
      UiExpressions.Op.MAX -> if (integral) max(i(0), i(1)) else max(f(0), f(1))
      UiExpressions.Op.CLAMP -> if (integral) clamp(i(1), i(2), i(0)) else clamp(f(0), f(1), f(2))
      UiExpressions.Op.ABS -> if (integral) i(0).absoluteValue else abs(f(0))
      UiExpressions.Op.FLOOR -> floor(f(0))
      UiExpressions.Op.CEIL -> ceil(f(0))
      UiExpressions.Op.ROUND -> round(f(0))
      UiExpressions.Op.SQRT -> sqrt(f(0))
      UiExpressions.Op.SIN -> sin(f(0))
      UiExpressions.Op.COS -> cos(f(0))
      UiExpressions.Op.TAN -> tan(f(0))
      UiExpressions.Op.POW -> pow(f(0), f(1))
      UiExpressions.Op.LERP -> lerp(f(0), f(1), f(2))
      UiExpressions.Op.TO_INT ->
        if (expr.args[0].kind == UiValueKind.INT) i(0) else f(0).toRemoteInt()
      UiExpressions.Op.TO_FLOAT -> f(0)
      UiExpressions.Op.EQ ->
        when {
          allBools -> b(0).isEqualTo(b(1))
          allInts -> i(0).isEqualTo(i(1))
          else -> f(0).isEqualTo(f(1))
        }
      UiExpressions.Op.NE ->
        when {
          allBools -> b(0).isNotEqualTo(b(1))
          allInts -> i(0).isNotEqualTo(i(1))
          else -> f(0).isNotEqualTo(f(1))
        }
      UiExpressions.Op.LT -> if (allInts) i(0).isLessThan(i(1)) else f(0).isLessThan(f(1))
      UiExpressions.Op.LE ->
        if (allInts) i(0).isLessThanOrEqualTo(i(1)) else f(0).isLessThanOrEqualTo(f(1))
      UiExpressions.Op.GT -> if (allInts) i(0).isGreaterThan(i(1)) else f(0).isGreaterThan(f(1))
      UiExpressions.Op.GE ->
        if (allInts) i(0).isGreaterThanOrEqualTo(i(1)) else f(0).isGreaterThanOrEqualTo(f(1))
      UiExpressions.Op.AND -> b(0) and b(1)
      UiExpressions.Op.OR -> b(0) or b(1)
      UiExpressions.Op.NOT -> !b(0)
      UiExpressions.Op.SELECT ->
        when (expr.kind) {
          UiValueKind.FLOAT -> b(0).select(f(1), f(2))
          UiValueKind.INT -> b(0).select(i(1), i(2))
          UiValueKind.BOOL -> b(0).select(b(1), b(2))
          UiValueKind.STRING -> b(0).select(asString(args[1]), asString(args[2]))
          UiValueKind.COLOR -> b(0).select(args[1] as RemoteColor, args[2] as RemoteColor)
        }
      UiExpressions.Op.CONCAT -> args.map(::asString).reduce { a, b -> a + b }
      UiExpressions.Op.TO_STRING -> asString(args[0])
      UiExpressions.Op.TWEEN,
      UiExpressions.Op.SPRING -> animated(expr, f(0))
    }
  }

  /**
   * `tween`/`spring`, as `RemoteContentEmitter` writes them: the player animates toward [target].
   */
  private fun animated(expr: UiExpressions.Expr.Call, target: RemoteFloat): RemoteFloat {
    fun literal(index: Int): String? =
      (expr.args.getOrNull(index) as? UiExpressions.Expr.Literal)?.value?.content
    val spec =
      if (expr.op == UiExpressions.Op.TWEEN) {
        remoteTween(literal(1)!!.toDouble().toInt(), easing(literal(2) ?: "standard"))
      } else {
        remoteSpring(
          stiffness = literal(1)?.toFloat() ?: 50f,
          dampingRatio = literal(2)?.toFloat() ?: 1f,
        )
      }
    return animateRemoteFloatAsState(target, spec)
  }

  private fun easing(name: String): RemoteEasing = remoteEasing(name)

  private fun asString(value: Any): RemoteString =
    when (value) {
      is RemoteString -> value
      is RemoteInt -> value.toRemoteString()
      is RemoteFloat -> value.toRemoteString()
      is RemoteBoolean -> value.select("true".rs, "false".rs)
      else -> "".rs
    }

  /**
   * The ordered actions bound to [event] on [node], as one `Action`: each state write a
   * `valueChange`, as the export writes them. Navigation and unknown writes play nothing.
   *
   * A two-way bound flag leads them: `checkedChange` with `checked` read whole from a flag the
   * authored actions do not already write negates it first, as compose-ui-builder's
   * `RemoteContentEmitter` exports it — so a bound `RemoteCheckboxButton` ticks with nothing
   * authored on its change.
   */
  fun action(node: UiBuilderNode, event: String): RemoteAction {
    val writeBack = writeBack(node, event)?.let { valueChangeAction(it, !it) }
    val actions =
      listOfNotNull(writeBack) +
        (node.eventBindings[event] as? JsonArray).orEmpty().mapNotNull { element ->
          val action = element as? JsonObject ?: return@mapNotNull null
          val variable =
            (action["variable"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
          val value = action["value"] as? JsonPrimitive
          when ((action["type"] as? JsonPrimitive)?.contentOrNull) {
            "toggle" -> bools[variable]?.let { valueChangeAction(it, !it) }
            // Summed in the player, as the export writes it: `valueChangeAction(count, count + 1)`.
            "increment" -> {
              val amount = (action["amount"] as? JsonPrimitive)?.takeUnless { it.isString }
              ints[variable]?.let { target ->
                (amount?.intOrNull ?: if (amount == null) 1 else null)?.let {
                  valueChangeAction(target, target + it)
                }
              }
                ?: floats[variable]?.let { target ->
                  (amount?.floatOrNull ?: if (amount == null) 1f else null)?.let {
                    valueChangeAction(target, target + it)
                  }
                }
            }
            "set",
            "select",
            "setText" ->
              ints[variable]?.let { target ->
                value?.doubleOrNull?.let { valueChangeAction(target, it.toInt().ri) }
              }
                ?: floats[variable]?.let { target ->
                  value?.doubleOrNull?.let { valueChangeAction(target, it.toFloat().rf) }
                }
                ?: bools[variable]?.let { target ->
                  value?.booleanOrNull?.let { valueChangeAction(target, it.rb) }
                }
                ?: strings[variable]?.let { target ->
                  value?.contentOrNull?.let { valueChangeAction(target, it.rs) }
                }
            else -> null
          }
        }
    return when (actions.size) {
      0 -> RemoteAction.None
      1 -> actions.single()
      else -> combinedAction(*actions.toTypedArray())
    }
  }

  fun hasAction(node: UiBuilderNode, event: String): Boolean =
    (node.eventBindings[event] as? JsonArray)?.isNotEmpty() == true ||
      writeBack(node, event) != null

  /** The flag [event]'s change writes back, when the authored actions leave it unwritten. */
  private fun writeBack(node: UiBuilderNode, event: String): MutableRemoteBoolean? {
    if (!event.endsWith("Change")) return null
    val wrapper = node.properties[event.removeSuffix("Change")] as? JsonObject ?: return null
    if ((wrapper["type"] as? JsonPrimitive)?.contentOrNull != "state") return null
    val variable = (wrapper["variable"] as? JsonPrimitive)?.contentOrNull ?: return null
    val written =
      (node.eventBindings[event] as? JsonArray).orEmpty().any { element ->
        val action = element as? JsonObject ?: return@any false
        (action["type"] as? JsonPrimitive)?.contentOrNull in WRITING_ACTIONS &&
          (action["variable"] as? JsonPrimitive)?.contentOrNull == variable
      }
    return if (written) null else bools[variable]
  }

  companion object {
    private val WRITING_ACTIONS =
      setOf("set", "select", "selectOrClear", "setText", "toggle", "increment")

    private fun argb(value: String): Int? {
      val hex = value.removePrefix("#")
      val packed = hex.toLongOrNull(16) ?: return null
      return when (hex.length) {
        6 -> (0xFF000000 or packed).toInt()
        8 -> packed.toInt()
        else -> null
      }
    }
  }
}

/** The design's typed state as remembered mutable remote values, one per variable. */
@Composable
internal fun rememberDocumentValues(document: UiBuilderDocument): DocumentValues {
  val scope = UiExpressions.Scope.of(document)
  val ints = mutableMapOf<String, MutableRemoteInt>()
  val floats = mutableMapOf<String, MutableRemoteFloat>()
  val bools = mutableMapOf<String, MutableRemoteBoolean>()
  val strings = mutableMapOf<String, MutableRemoteString>()
  scope.stateKinds.entries
    .sortedBy { it.key }
    .forEach { (name, kind) ->
      val initial =
        ((document.stateVariables[name] as? JsonObject)?.get("initialValue") as? JsonPrimitive)
      key(name) {
        when (kind) {
          UiValueKind.INT -> ints[name] = remember {
              MutableRemoteInt(initial?.doubleOrNull?.toInt() ?: 0)
            }
          UiValueKind.FLOAT -> floats[name] = remember {
              MutableRemoteFloat(initial?.doubleOrNull?.toFloat() ?: 0f)
            }
          UiValueKind.BOOL -> bools[name] = remember {
              MutableRemoteBoolean(initial?.booleanOrNull ?: false)
            }
          UiValueKind.STRING -> strings[name] = remember {
              MutableRemoteString(initial?.contentOrNull.orEmpty())
            }
          UiValueKind.COLOR -> Unit
        }
      }
    }
  return DocumentValues(scope, ints, floats, bools, strings)
}
