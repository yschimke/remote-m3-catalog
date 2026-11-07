package ee.schimke.remote.calculator

import androidx.compose.remote.creation.common.RemoteContext
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import ee.schimke.composeai.rccreation.bridge.LocalRcHostBridge
import kotlin.math.abs

/**
 * The calculator's host side: the state a Glance Wear widget's app process would keep, and what
 * each host action does to it.
 *
 * The widget never computes anything. It shows [Calculator.display] and [Calculator.expression] as
 * named values, and each key fires a named host action that lands in [press]. That is how a widget
 * document talks to the process that published it.
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

  /** Where a touch went down on the display, in the document's pixels. */
  fun swipeStarted(x: Float) {
    swipeStart = x
  }

  /** Where it came up: a horizontal swipe across the display clears. */
  fun swipeEnded(x: Float, displayWidth: Float) {
    val start = swipeStart ?: return
    swipeStart = null
    if (abs(x - start) >= displayWidth * SWIPE_FRACTION) press(Input.Clear)
  }

  companion object {
    /** How far across the display a swipe has to travel to clear it. */
    const val SWIPE_FRACTION = 0.3f
    const val SWIPE_START = "swipe-start"
    const val SWIPE_END = "swipe-end"
  }
}

/** One key of the operator column, by row: its label and what it does, in each set. */
private class OperatorKey(val basic: Operator, val moreLabel: String, val more: Input)

private val operatorColumn =
  listOf(
    OperatorKey(Operator.Add, "±", Input.Negate),
    OperatorKey(Operator.Subtract, "%", Input.Percent),
    OperatorKey(Operator.Multiply, "√", Input.SquareRoot),
    OperatorKey(Operator.Divide, "x²", Input.Square),
  )

/** The digit block, row by row; the fourth row's last cell is `=`. */
private val digitRows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))

/**
 * The calculator, as Remote Compose content for the Glance Wear **large** widget container.
 *
 * Laid out for the content slot the host leaves inside its padding (200×108dp in the 216×124dp
 * container): a display row with the more-operators key, then four rows of four keys. Every key is
 * a pill, so the tap target is visible and the whole slot is used, rather than glyphs floating in
 * empty space as on the round tile it came from.
 */
@RemoteComposable
@Composable
fun CalculatorWidget(session: CalculatorSession) {
  val bridge = LocalRcHostBridge.current
  val display = bridge.text("display") { session.calculator.display }
  val expression = bridge.text("expression") { session.calculator.expression }
  RemoteMaterialTheme {
    // No `spacedBy` anywhere: this player hands weighted children their share of the whole row
    // before the spacing is taken out, so four weighted keys with three gaps overflow the slot by
    // the gaps. Each key instead insets its own pill by half a gap.
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
      RemoteRow(
        modifier = RemoteModifier.fillMaxWidth().height(DISPLAY_HEIGHT.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically,
      ) {
        val moreLabel = bridge.text("more-label") { if (session.moreOperators) "+−" else "f(x)" }
        val moreColors =
          KeyColors(
            container =
              bridge.color("more-container") {
                if (session.moreOperators) Palette.operator else Palette.operatorContainer
              },
            content =
              bridge.color("more-content") {
                if (session.moreOperators) Palette.onOperator else Palette.operator
              },
          )
        Key(
          label = moreLabel,
          colors = moreColors,
          action = bridge.action("more") { session.toggleMoreOperators() },
          modifier = RemoteModifier.width(MORE_KEY_WIDTH.rdp),
          fontSize = 12,
        )
        // The swipe surface. Touch down and up each report where they were, and the host decides
        // whether that was a swipe; the keys are left out of it, so a tap is never also a swipe.
        val touchX = RemoteFloat(RemoteContext.FLOAT_TOUCH_POS_X)
        RemoteRow(
          modifier =
            RemoteModifier.weight(1f.rf)
              .fillMaxHeight()
              .onTouchDown(hostAction(CalculatorSession.SWIPE_START.rs, touchX))
              .onTouchUp(hostAction(CalculatorSession.SWIPE_END.rs, touchX))
              // The end inset keeps the last digit clear of the container's rounded corner.
              .padding(start = 4.rdp, end = 10.rdp),
          horizontalArrangement = RemoteArrangement.spacedBy(6.rdp),
          verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
          RemoteText(
            text = expression,
            color = Palette.secondary.rc,
            fontSize = 12.rsp,
            maxLines = 1,
          )
          RemoteText(
            text = display,
            modifier = RemoteModifier.weight(1f.rf),
            color = Palette.operator.rc,
            fontSize = 20.rsp,
            textAlign = TextAlign.End,
            maxLines = 1,
          )
        }
      }
      for (row in 0 until 4) {
        RemoteRow(modifier = RemoteModifier.fillMaxWidth().weight(1f.rf)) {
          if (row < 3) {
            digitRows[row].forEach { DigitKey(it, session) }
          } else {
            DigitKey(".", session)
            DigitKey("0", session)
            Key(
              label = "=".rs,
              colors = KeyColors(Palette.operator.rc, Palette.onOperator.rc),
              action = bridge.action("equals") { session.press(Input.Equals) },
              modifier = RemoteModifier.weight(1f.rf),
            )
          }
          OperatorKey(operatorColumn[row], session)
        }
      }
    }
  }
}

@RemoteComposable
@Composable
private fun RemoteRowScope.DigitKey(key: String, session: CalculatorSession) {
  val input = if (key == ".") Input.Point else Input.Digit(key.toInt())
  Key(
    label = key.rs,
    colors = KeyColors(Palette.digitContainer.rc, Palette.onDigit.rc),
    action = LocalRcHostBridge.current.action("key-$key") { session.press(input) },
    modifier = RemoteModifier.weight(1f.rf),
  )
}

/**
 * One key of the operator column. Its label and colours are host values rather than structure: the
 * set it shows and the pending-operator highlight change with every press, and as named values they
 * reach the player without a new document.
 */
@RemoteComposable
@Composable
private fun RemoteRowScope.OperatorKey(key: OperatorKey, session: CalculatorSession) {
  val bridge = LocalRcHostBridge.current
  val name = "op-${key.basic.name.lowercase()}"
  fun highlighted() = !session.moreOperators && session.calculator.pending == key.basic
  val label =
    bridge.text("$name-label") { if (session.moreOperators) key.moreLabel else key.basic.symbol }
  val colors =
    KeyColors(
      container =
        bridge.color("$name-container") {
          if (highlighted()) Palette.operator else Palette.operatorContainer
        },
      content =
        bridge.color("$name-content") {
          if (highlighted()) Palette.onOperator else Palette.operator
        },
    )
  Key(
    label = label,
    colors = colors,
    action =
      bridge.action(name) {
        session.press(if (session.moreOperators) key.more else Input.Binary(key.basic))
      },
    modifier = RemoteModifier.weight(1f.rf),
  )
}

private class KeyColors(val container: RemoteColor, val content: RemoteColor)

@RemoteComposable
@Composable
private fun Key(
  label: RemoteString,
  colors: KeyColors,
  action: androidx.compose.remote.creation.compose.action.Action,
  modifier: RemoteModifier,
  fontSize: Int = 15,
) {
  RemoteBox(modifier = modifier.fillMaxHeight().padding((KEY_GAP / 2).rdp)) {
    RemoteBox(
      modifier =
        RemoteModifier.fillMaxSize()
          .clip(RemoteRoundedCornerShape(KEY_RADIUS.rdp))
          .background(colors.container)
          .clickable(action),
      contentAlignment = RemoteAlignment.Center,
    ) {
      RemoteText(text = label, color = colors.content, fontSize = fontSize.rsp, maxLines = 1)
    }
  }
}

/** Dark-first, on the lighter widget ground the design settled on (#303038). */
internal object Palette {
  val background = Color(0xFF303038)
  val digitContainer = Color(0xFF41414B)
  val onDigit = Color(0xFFE3E3E3)
  val operatorContainer = Color(0xFF2B3A52)
  val operator = Color(0xFFA8C7FA)
  val onOperator = Color(0xFF0B1D33)
  val secondary = Color(0xFFC4C6D0)
}

private const val KEY_GAP = 3f
private const val KEY_RADIUS = 10f
private const val DISPLAY_HEIGHT = 26f
private const val MORE_KEY_WIDTH = 34f
