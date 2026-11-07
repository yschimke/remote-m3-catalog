package ee.schimke.remote.calculator

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorTest {
  private fun run(keys: String): Calculator =
    keys.split(' ').filter(String::isNotEmpty).fold(Calculator()) { calculator, key ->
      calculator.reduce(
        when (key) {
          "+" -> Input.Binary(Operator.Add)
          "-" -> Input.Binary(Operator.Subtract)
          "*" -> Input.Binary(Operator.Multiply)
          "/" -> Input.Binary(Operator.Divide)
          "=" -> Input.Equals
          "." -> Input.Point
          "C" -> Input.Clear
          "neg" -> Input.Negate
          "%" -> Input.Percent
          "sqrt" -> Input.SquareRoot
          "sq" -> Input.Square
          else -> return@fold key.fold(calculator) { c, d -> c.reduce(Input.Digit(d.digitToInt())) }
        }
      )
    }

  private fun shows(expected: String, keys: String) =
    assertEquals(expected, run(keys).display, keys)

  @Test fun typesNumbers() = shows("106500", "106500")

  @Test fun leadingZeroIsReplaced() = shows("7", "0 0 7")

  @Test fun decimals() = shows("0.25", ". 25")

  @Test fun secondPointIgnored() = shows("1.5", "1 . 5 .")

  @Test fun digitLimit() = shows("123456789", "1234567890")

  @Test fun adds() = shows("5", "2 + 3 =")

  @Test
  fun chainsImmediately() {
    // `2 + 3 ×` shows 5 before the multiplication.
    assertEquals("5", run("2 + 3 *").display)
    assertEquals("5 ×", run("2 + 3 *").expression)
    shows("20", "2 + 3 * 4 =")
  }

  @Test fun changingOperator() = shows("2", "5 + * - 3 =")

  @Test fun repeatsEquals() = shows("11", "5 + 3 = =")

  @Test fun operandDefaultsToDisplay() = shows("25", "5 * =")

  @Test fun subtractsBelowZero() = shows("-7", "3 - 10 =")

  @Test fun divides() = shows("0.333333333", "1 / 3 =")

  @Test fun roundsBinaryNoise() = shows("0.3", ". 1 + . 2 =")

  @Test fun divideByZeroIsAnError() = shows("Error", "1 / 0 =")

  @Test fun typingAfterAnErrorStartsOver() = shows("4", "1 / 0 = 4")

  @Test
  fun clears() {
    val cleared = run("12 + 3 C")
    assertEquals("0", cleared.display)
    assertEquals("", cleared.expression)
    assertEquals(null, cleared.pending)
  }

  @Test fun negates() = shows("-12", "12 neg")

  @Test fun percentOfLeftOperand() = shows("220", "200 + 10 % =")

  @Test fun plainPercent() = shows("0.5", "50 %")

  @Test fun squareRoot() = shows("12", "144 sqrt")

  @Test fun squareRootOfNegativeIsAnError() = shows("Error", "4 neg sqrt")

  @Test fun squares() = shows("144", "12 sq")

  @Test fun bigNumbersGoScientific() = shows("1e12", "1000000 * 1000000 =")

  @Test fun resultIsReplacedByTyping() = shows("9", "2 + 3 = 9")

  @Test fun unaryOperandIsKeptForTheNextOperator() = shows("15", "2 + 3 sq + 4 =")

  @Test fun unaryOperandIsKeptForEquals() = shows("11", "2 + 3 sq =")
}
