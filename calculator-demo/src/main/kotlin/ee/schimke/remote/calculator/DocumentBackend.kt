package ee.schimke.remote.calculator

import androidx.compose.remote.creation.common.RemoteContext
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.DecimalFormat
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.pow
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.runtime.Composable

/**
 * Backend 2, **document expressions**: the calculator runs inside the document. No host action is
 * fired and the host keeps no state; a document recorded from this content would calculate on any
 * player, with its app process asleep.
 *
 * The state is a handful of remote floats ([Register]). Each key is a list of value-change actions
 * whose new values are expressions the player evaluates when the key is pressed. The arithmetic is
 * the same immediate-execution calculator as [Calculator], within what a document can hold:
 * - **32-bit floats.** A typed number is limited to [DOCUMENT_DIGITS] digits, results are shown to
 *   6 decimal places, and anything beyond ±[DOCUMENT_LIMIT] is an error.
 * - **No branching between actions.** An action list is fixed when the document is recorded, so a
 *   key that does different things in different states (an operator key in the `f(x)` set, `=` with
 *   or without a pending operation) assigns `condition.select(a, b)` to every register.
 *
 * ### Two banks of registers
 *
 * A key press assigns several registers from the *old* values of several registers: an operator
 * sets both the accumulator and the entry from both of them. The player runs a list of
 * value-changes in order, and how a later one sees an earlier write depends on how the expression
 * was recorded: a mutable float it reads directly is the new value, but a shared sub-expression
 * recorded as its own variable is still the value it had at the last frame. Rather than depend on
 * which one a given expression got, every key computes into a second bank ([next]) reading only the
 * committed bank ([current]), and then copies `next` into `current`. Phase one reads nothing it
 * writes, and phase two reads each `next` register directly, so both readings agree.
 */
private enum class Register(val initial: Float) {
  Entry(0f),
  /** Decimal places typed: -1 before the point, 0 straight after it. */
  Fraction(-1f),
  /** Digits typed, against [DOCUMENT_DIGITS]. */
  Digits(0f),
  Accumulator(0f),
  /** The pending operator: 0 for none, then 1…4 for + − × ÷ ([Operator] order). */
  Operator(0f),
  /** 1 when the next digit starts a new number. */
  Fresh(1f),
  /** 1 once a right operand for the pending operator is given: typed, or made by a unary key. */
  Operand(0f),
  RepeatOperator(0f),
  RepeatOperand(0f),
  Error(0f),
}

/** A typed number can be this many digits: what a 32-bit float carries exactly. */
const val DOCUMENT_DIGITS = 7

/** A result past this magnitude is an error rather than a number the display cannot show. */
const val DOCUMENT_LIMIT = 999_999_999f

private typealias Registers = Map<Register, MutableRemoteFloat>

/** New values for some registers. A register not named keeps its value. */
private typealias Step = Map<Register, RemoteFloat>

private class DocumentCalculator(val current: Registers, val next: Registers) {
  private operator fun Register.invoke(): RemoteFloat = current.getValue(this)

  private val fresh = Register.Fresh().isEqualTo(1f.rf)
  private val pending = Register.Operator().isNotEqualTo(0f.rf)

  /** The number a digit or the point extends: zero, before the point, if this is a new number. */
  private val baseEntry = fresh.select(0f.rf, Register.Entry())
  private val baseFraction = fresh.select((-1f).rf, Register.Fraction())
  private val baseDigits = fresh.select(0f.rf, Register.Digits())
  private val beforePoint = baseFraction.isLessThan(0f.rf)

  fun digit(d: Int): Step {
    val digit = d.toFloat().rf
    val negative = baseEntry.isLessThan(0f.rf)
    val whole = negative.select(baseEntry * 10f - digit, baseEntry * 10f + digit)
    val place = pow(10f, -(baseFraction + 1f))
    val decimal = negative.select(baseEntry - digit * place, baseEntry + digit * place)
    val entry = beforePoint.select(whole, decimal)
    // A leading zero is not a digit: "007" is one digit long.
    val digits = (beforePoint and entry.isEqualTo(0f.rf)).select(0f.rf, baseDigits + 1f)
    val allowed = baseDigits.isLessThan(DOCUMENT_DIGITS.toFloat().rf)
    return mapOf(
      Register.Entry to allowed.select(entry, Register.Entry()),
      Register.Fraction to
        allowed.select(beforePoint.select((-1f).rf, baseFraction + 1f), Register.Fraction()),
      Register.Digits to allowed.select(digits, Register.Digits()),
      Register.Fresh to 0f.rf,
      Register.Operand to 1f.rf,
      Register.Error to 0f.rf,
    )
  }

  fun point(): Step =
    mapOf(
      Register.Entry to baseEntry,
      Register.Fraction to beforePoint.select(0f.rf, baseFraction),
      Register.Digits to baseDigits,
      Register.Fresh to 0f.rf,
      Register.Operand to 1f.rf,
      Register.Error to 0f.rf,
    )

  fun binary(operator: Operator): Step {
    val code = (operator.ordinal + 1).toFloat().rf
    // Changing your mind: `5 + ×` is `5 ×`.
    val changeOnly = pending and Register.Operand().isEqualTo(0f.rf)
    val left =
      pending.select(
        apply(Register.Accumulator(), Register.Operator(), Register.Entry()),
        Register.Entry(),
      )
    val good = inRange(left)
    return mapOf(
      Register.Operator to changeOnly.select(code, good.select(code, 0f.rf)),
      Register.Accumulator to changeOnly.select(Register.Accumulator(), good.select(left, 0f.rf)),
      Register.Entry to changeOnly.select(Register.Entry(), good.select(left, 0f.rf)),
      Register.Error to changeOnly.select(0f.rf, good.select(0f.rf, 1f.rf)),
      Register.RepeatOperator to 0f.rf,
      Register.Operand to 0f.rf,
    ) + newNumber
  }

  fun equals(): Step {
    val repeating = Register.RepeatOperator().isNotEqualTo(0f.rf)
    val active = pending or repeating
    val operator = pending.select(Register.Operator(), Register.RepeatOperator())
    val left = pending.select(Register.Accumulator(), Register.Entry())
    // `5 × =` squares: after an operator the entry still shows the left operand.
    val right = pending.select(Register.Entry(), Register.RepeatOperand())
    val answer = apply(left, operator, right)
    val good = inRange(answer)
    return mapOf(
      Register.Entry to active.select(good.select(answer, 0f.rf), Register.Entry()),
      Register.Error to (active and !good).select(1f.rf, 0f.rf),
      Register.Operator to 0f.rf,
      Register.Accumulator to 0f.rf,
      Register.RepeatOperator to (active and good).select(operator, 0f.rf),
      Register.RepeatOperand to right,
      Register.Operand to 0f.rf,
    ) + newNumber
  }

  /** ± % √ x², by row of the operator column. */
  fun unary(row: Int): Step {
    val entry = Register.Entry()
    val value =
      when (row) {
        0 -> -entry
        1 -> {
          // 200 + 10% is 220: the percentage is of the left operand.
          val additive =
            Register.Operator().isEqualTo(1f.rf) or Register.Operator().isEqualTo(2f.rf)
          additive.select(Register.Accumulator() * entry / 100f, entry / 100f)
        }
        2 -> sqrt(entry)
        else -> entry * entry
      }
    val good = inRange(value)
    return mapOf(
      Register.Entry to good.select(value, 0f.rf),
      Register.Error to good.select(0f.rf, 1f.rf),
      Register.Operator to good.select(Register.Operator(), 0f.rf),
      Register.Accumulator to good.select(Register.Accumulator(), 0f.rf),
      Register.Operand to 1f.rf,
    ) + newNumber
  }

  /** A key of the operator column: which step it takes is decided when it is pressed. */
  fun operatorKey(row: Int, more: RemoteBoolean): Step {
    val basic = binary(Operator.entries[row])
    val second = unary(row)
    return (basic.keys + second.keys).associateWith { register ->
      more.select(second[register] ?: register(), basic[register] ?: register())
    }
  }

  private val newNumber: Step =
    mapOf(Register.Fresh to 1f.rf, Register.Fraction to (-1f).rf, Register.Digits to 0f.rf)

  /** The action for [step]: compute every new value from the committed bank, then commit. */
  fun action(step: Step, vararg also: Action): Action =
    combinedAction(
      *step.map { (register, value) -> valueChange(next.getValue(register), value) }.toTypedArray(),
      *step.keys
        .map { register ->
          // `min` with the largest float is the identity, as an expression: a bare mutable
          // float as the new value would name its declaration (its initial value), not its
          // current one.
          valueChange(current.getValue(register), next.getValue(register).min(Float.MAX_VALUE))
        }
        .toTypedArray(),
      *also,
    )

  // What the widget shows, from the committed bank.

  val display: RemoteString = run {
    val entry = Register.Entry()
    val fraction = Register.Fraction()
    // Every place a typed number can reach: `.0000001` is seven digits, all after the point.
    var typed = entry.toRemoteString(fixed(DOCUMENT_DIGITS))
    for (places in DOCUMENT_DIGITS - 1 downTo 1) {
      typed =
        fraction.isEqualTo(places.toFloat().rf).select(entry.toRemoteString(fixed(places)), typed)
    }
    val whole = entry.toRemoteString(fixed(0))
    typed = fraction.isEqualTo(0f.rf).select(whole + ".", typed)
    typed = fraction.isLessThan(0f.rf).select(whole, typed)
    Register.Error().isEqualTo(1f.rf).select("Error".rs, fresh.select(result(entry), typed))
  }

  val expression: RemoteString = run {
    val operator = Register.Operator()
    val symbol =
      operator
        .isEqualTo(1f.rf)
        .select(
          Operator.Add.symbol.rs,
          operator
            .isEqualTo(2f.rf)
            .select(
              Operator.Subtract.symbol.rs,
              operator
                .isEqualTo(3f.rf)
                .select(Operator.Multiply.symbol.rs, Operator.Divide.symbol.rs),
            ),
        )
    pending.select(result(Register.Accumulator()) + " " + symbol, "".rs)
  }

  fun highlighted(operator: Operator): RemoteBoolean =
    Register.Operator().isEqualTo((operator.ordinal + 1).toFloat().rf)

  companion object {
    fun apply(left: RemoteFloat, operator: RemoteFloat, right: RemoteFloat): RemoteFloat =
      operator
        .isEqualTo(1f.rf)
        .select(
          left + right,
          operator
            .isEqualTo(2f.rf)
            .select(left - right, operator.isEqualTo(3f.rf).select(left * right, left / right)),
        )

    /** False for NaN and infinity too: a comparison with NaN is false. */
    fun inRange(value: RemoteFloat): RemoteBoolean =
      abs(value).isLessThanOrEqualTo(DOCUMENT_LIMIT.rf)

    /** A result: up to 6 decimal places, trailing zeros dropped, no grouping. */
    fun result(value: RemoteFloat): RemoteString =
      value.toRemoteString(
        DecimalFormat().apply {
          isGroupingUsed = false
          maximumFractionDigits = 6
        }
      )

    /** A number being typed: exactly [places] decimals, so a typed trailing zero shows. */
    fun fixed(places: Int): DecimalFormat =
      DecimalFormat().apply {
        isGroupingUsed = false
        maximumFractionDigits = places
        minimumFractionDigits = places
      }
  }
}

/**
 * A mutable remote float of its own, starting at [initial].
 *
 * Not `rememberMutableRemoteFloat(initial)`: the common document writer declares a mutable float as
 * a float expression and caches float expressions by content, so every mutable float with the same
 * initial value comes out as one variable (every register starting at 0 would be the same
 * register). The declaration here is an expression unique to [index] that still evaluates to
 * [initial]: the window width is positive, so `min(width, -(index + 1))` is `-(index + 1)`.
 */
@RemoteComposable
@Composable
internal fun rememberRegister(initial: Float, index: Int): MutableRemoteFloat =
  rememberMutableRemoteFloat {
    RemoteFloat(RemoteContext.FLOAT_WINDOW_WIDTH).min(-(index + 1f)) + (initial + index + 1f)
  }

/**
 * The document-expressions backend's bindings: remote state and value-change actions only.
 *
 * @param swipeThresholdPx how far, in the document's pixels, a touch has to travel across the
 *   display between down and up to clear it.
 */
@RemoteComposable
@Composable
fun rememberDocumentBindings(swipeThresholdPx: Float): CalculatorBindings {
  val registers = Register.entries.size
  val current = Register.entries.associateWith { rememberRegister(it.initial, it.ordinal) }
  val next = Register.entries.associateWith { rememberRegister(it.initial, registers + it.ordinal) }
  val moreRegister = rememberRegister(0f, 2 * registers)
  val swipeStart = rememberRegister(0f, 2 * registers + 1)
  val calculator = DocumentCalculator(current, next)
  val more = moreRegister.isEqualTo(1f.rf)
  val closeMore = valueChange(moreRegister, 0f.rf)

  val touchX = RemoteFloat(RemoteContext.FLOAT_TOUCH_POS_X)
  val swiped = abs(touchX - swipeStart).isGreaterThanOrEqualTo(swipeThresholdPx.rf)
  // Clearing writes each register from itself and the touch, which nothing here writes, so it
  // needs no second bank.
  val clear =
    combinedAction(
      *Register.entries
        .map { register ->
          valueChange(
            current.getValue(register),
            swiped.select(register.initial.rf, current.getValue(register).min(Float.MAX_VALUE)),
          )
        }
        .toTypedArray(),
      valueChange(moreRegister, swiped.select(0f.rf, moreRegister.min(Float.MAX_VALUE))),
    )

  val operators =
    Operator.entries.mapIndexed { row, operator ->
      val lit = !more and calculator.highlighted(operator)
      KeyBinding(
        label = more.select(moreOperatorLabels[row].rs, operator.symbol.rs),
        container = lit.select(Palette.operator.rc, Palette.operatorContainer.rc),
        content = lit.select(Palette.onOperator.rc, Palette.operator.rc),
        action = calculator.action(calculator.operatorKey(row, more), closeMore),
      )
    }
  return CalculatorBindings(
    display = calculator.display,
    expression = calculator.expression,
    more =
      KeyBinding(
        label = more.select(BASIC_LABEL.rs, MORE_LABEL.rs),
        container = more.select(Palette.operator.rc, Palette.operatorContainer.rc),
        content = more.select(Palette.onOperator.rc, Palette.operator.rc),
        action = valueChange(moreRegister, 1f.rf - moreRegister),
      ),
    digits =
      (0..9).associate { d -> d.toString() to calculator.action(calculator.digit(d)) } +
        ("." to calculator.action(calculator.point())),
    equals = calculator.action(calculator.equals(), closeMore),
    operators = operators,
    swipeStart = valueChange(swipeStart, touchX.min(Float.MAX_VALUE)),
    swipeEnd = clear,
  )
}
