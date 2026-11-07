package ee.schimke.remote.calculator

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.sqrt

/** The four binary operators, spelled as the keys spell them. */
enum class Operator(val symbol: String) {
  Add("+"),
  Subtract("−"),
  Multiply("×"),
  Divide("÷"),
}

/** Everything a key, or a gesture, can ask of the calculator. */
sealed interface Input {
  data class Digit(val value: Int) : Input {
    init {
      require(value in 0..9) { "not a digit: $value" }
    }
  }

  data object Point : Input

  data class Binary(val operator: Operator) : Input

  data object Equals : Input

  /** All clear. On the widget it is a swipe across the display, not a key. */
  data object Clear : Input

  data object Negate : Input

  data object Percent : Input

  data object SquareRoot : Input

  data object Square : Input
}

/**
 * An immediate-execution calculator, the kind on a phone: `2 + 3 ×` shows 5 before the `×`, and `=`
 * again repeats the last operation.
 *
 * Immutable; [reduce] returns the next state. What the widget shows is [display] and [expression];
 * [pending] is what the operator key highlights.
 */
data class Calculator(
  /** The number being typed, or the last result, as shown. */
  val entry: String = "0",
  /** The left operand of [pending]. */
  val accumulator: Double? = null,
  val pending: Operator? = null,
  /** The next digit starts a new number rather than extending [entry]. */
  val fresh: Boolean = true,
  /** What `=` repeats once [pending] has been resolved. */
  val repeat: Pair<Operator, Double>? = null,
  val error: Boolean = false,
) {
  val display: String
    get() = if (error) "Error" else entry

  /** The pending operation, `12 ×`, or nothing. */
  val expression: String
    get() =
      if (error || pending == null || accumulator == null) ""
      else "${format(accumulator)} ${pending.symbol}"

  private val value: Double
    get() = entry.toDouble()

  fun reduce(input: Input): Calculator {
    if (input == Input.Clear) return Calculator()
    // Anything after an error starts over, then applies.
    if (error) return Calculator().reduce(input)
    return when (input) {
      is Input.Digit -> typed(input.value.toString())
      Input.Point -> typed(".")
      is Input.Binary -> operator(input.operator)
      Input.Equals -> equals()
      Input.Negate -> unary { -it }
      Input.Percent ->
        unary {
          when (pending) {
            // 200 + 10% is 220: the percentage is of the left operand.
            Operator.Add,
            Operator.Subtract -> (accumulator ?: 0.0) * it / 100
            else -> it / 100
          }
        }
      Input.SquareRoot -> unary { if (it < 0) Double.NaN else sqrt(it) }
      Input.Square -> unary { it * it }
      Input.Clear -> error("handled above")
    }
  }

  private fun typed(key: String): Calculator {
    val start = if (fresh) "0" else entry
    val next =
      when {
        key == "." && "." in start -> start
        key == "." -> "$start."
        start == "0" -> key
        start == "-0" -> "-$key"
        else -> start + key
      }
    if (next.count(Char::isDigit) > MAX_DIGITS) return this
    return copy(entry = next, fresh = false)
  }

  private fun operator(operator: Operator): Calculator {
    // Changing your mind: `5 + ×` is `5 ×`.
    if (pending != null && fresh) return copy(pending = operator)
    val left =
      if (pending != null && accumulator != null) apply(accumulator, pending, value) else value
    return result(left)?.copy(accumulator = left, pending = operator, repeat = null) ?: failed()
  }

  private fun equals(): Calculator {
    val (operator, right) =
      when {
        // `5 × =` squares, as a phone does: the missing operand is the one on show.
        pending != null -> pending to value
        repeat != null -> repeat
        else -> return copy(fresh = true)
      }
    val left = if (pending != null) accumulator ?: value else value
    val answer = apply(left, operator, right)
    return result(answer)?.copy(accumulator = null, pending = null, repeat = operator to right)
      ?: failed()
  }

  private fun unary(f: (Double) -> Double): Calculator = result(f(value)) ?: failed()

  /** [answer] on show, ready to be operated on or replaced; null if it is not a number. */
  private fun result(answer: Double): Calculator? {
    if (answer.isNaN() || answer.isInfinite()) return null
    return copy(entry = format(answer), fresh = true)
  }

  private fun failed() = Calculator(error = true)

  companion object {
    /** As many digits as the display row holds at its text size. */
    const val MAX_DIGITS = 9

    fun apply(left: Double, operator: Operator, right: Double): Double =
      when (operator) {
        Operator.Add -> left + right
        Operator.Subtract -> left - right
        Operator.Multiply -> left * right
        Operator.Divide -> if (right == 0.0) Double.NaN else left / right
      }

    /**
     * [value] in at most [MAX_DIGITS] significant digits: plain while that fits the display, else
     * in scientific notation. Binary noise is rounded away, so `0.1 + 0.2` shows `0.3`.
     */
    fun format(value: Double): String {
      if (value == 0.0) return "0"
      val rounded = BigDecimal(value).round(MathContext(MAX_DIGITS)).stripTrailingZeros()
      val plain = rounded.toPlainString()
      val digits = plain.count(Char::isDigit)
      if (digits <= MAX_DIGITS + 1 && (abs(value) >= 1e-6)) {
        // A leading "0." costs one digit that the significant count does not.
        return plain
      }
      val scientific = BigDecimal(value).round(MathContext(4)).stripTrailingZeros()
      val exponent = scientific.precision() - scientific.scale() - 1
      val mantissa = scientific.movePointLeft(exponent).stripTrailingZeros().toPlainString()
      return "${mantissa}e$exponent"
    }
  }
}
