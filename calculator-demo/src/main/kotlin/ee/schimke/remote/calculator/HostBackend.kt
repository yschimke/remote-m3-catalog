package ee.schimke.remote.calculator

import androidx.compose.remote.creation.common.RemoteContext
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ee.schimke.composeai.rccreation.bridge.LocalRcHostBridge
import ee.schimke.composeai.rcplayer.runtime.RcHostActionValue
import ee.schimke.composeai.rcplayer.runtime.RcPlayerEvent
import kotlin.math.abs

/**
 * Backend 1, **host actions**: the state a Glance Wear widget's app process would keep, and what
 * each host action does to it.
 *
 * The document never computes anything. It shows [Calculator.display] and [Calculator.expression]
 * as named values, and each key fires a named host action that lands in [press]. That is how a
 * widget document talks to the process that published it.
 */
class CalculatorSession {
  var calculator by mutableStateOf(Calculator())
    private set

  /** The operator column shows the other operators (± % √ x²) instead of + − × ÷. */
  var moreOperators by mutableStateOf(false)
    private set

  fun press(input: Input) {
    calculator = calculator.reduce(input)
    // An operator from the second set is a one-off: back to + − × ÷ for the next step.
    if (input !is Input.Digit && input != Input.Point && moreOperators) moreOperators = false
  }

  fun toggleMoreOperators() {
    moreOperators = !moreOperators
  }

  private var swipeStart: Float? = null

  /**
   * The swipe's two touch events. They carry a value, the touch x in the document's pixels, which
   * the bridge does not route by name, so the player host hands them over here.
   */
  fun onPlayerEvent(event: RcPlayerEvent, widthPx: Float) {
    val action = event as? RcPlayerEvent.HostNamedAction ?: return
    val x = (action.value as? RcHostActionValue.FloatValue)?.value ?: return
    when (action.name) {
      SWIPE_START -> swipeStart = x
      SWIPE_END -> {
        val start = swipeStart ?: return
        swipeStart = null
        if (abs(x - start) >= widthPx * SWIPE_FRACTION) press(Input.Clear)
      }
    }
  }

  companion object {
    const val SWIPE_START = "swipe-start"
    const val SWIPE_END = "swipe-end"
  }
}

/** How far across the widget a swipe on the display has to travel to clear it. */
const val SWIPE_FRACTION = 0.3f

/** The host-actions backend's bindings: named values in, named actions out. */
@RemoteComposable
@Composable
fun rememberHostBindings(session: CalculatorSession): CalculatorBindings {
  val bridge = LocalRcHostBridge.current
  val touchX = RemoteFloat(RemoteContext.FLOAT_TOUCH_POS_X)
  val more =
    KeyBinding(
      label = bridge.text("more-label") { if (session.moreOperators) BASIC_LABEL else MORE_LABEL },
      container =
        bridge.color("more-container") {
          if (session.moreOperators) Palette.operator else Palette.operatorContainer
        },
      content =
        bridge.color("more-content") {
          if (session.moreOperators) Palette.onOperator else Palette.operator
        },
      action = bridge.action("more") { session.toggleMoreOperators() },
    )
  val digits =
    (0..9).associate { d ->
      d.toString() to bridge.action("key-$d") { session.press(Input.Digit(d)) }
    } + ("." to bridge.action("key-.") { session.press(Input.Point) })
  val moreInputs = listOf(Input.Negate, Input.Percent, Input.SquareRoot, Input.Square)
  val operators =
    Operator.entries.mapIndexed { row, operator ->
      val name = "op-${operator.name.lowercase()}"
      fun highlighted() = !session.moreOperators && session.calculator.pending == operator
      KeyBinding(
        label =
          bridge.text("$name-label") {
            if (session.moreOperators) moreOperatorLabels[row] else operator.symbol
          },
        container =
          bridge.color("$name-container") {
            if (highlighted()) Palette.operator else Palette.operatorContainer
          },
        content =
          bridge.color("$name-content") {
            if (highlighted()) Palette.onOperator else Palette.operator
          },
        action =
          bridge.action(name) {
            session.press(if (session.moreOperators) moreInputs[row] else Input.Binary(operator))
          },
      )
    }
  return CalculatorBindings(
    display = bridge.text("display") { session.calculator.display },
    expression = bridge.text("expression") { session.calculator.expression },
    more = more,
    digits = digits,
    equals = bridge.action("equals") { session.press(Input.Equals) },
    operators = operators,
    swipeStart = hostAction(CalculatorSession.SWIPE_START.rs, touchX),
    swipeEnd = hostAction(CalculatorSession.SWIPE_END.rs, touchX),
  )
}
