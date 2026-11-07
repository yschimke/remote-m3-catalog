package ee.schimke.remote.calculator

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
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
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText

/** A key whose label and colours can change: the operator column and the `f(x)` key. */
class KeyBinding(
  val label: RemoteString,
  val container: RemoteColor,
  val content: RemoteColor,
  val action: Action,
)

/**
 * Everything the layout reads, from whichever backend runs the calculator.
 *
 * The layout is the same document structure for both backends. Only where these values and actions
 * come from differs: named host values and host actions ([rememberHostBindings]), or remote state
 * and value-change expressions evaluated by the player ([rememberDocumentBindings]).
 */
class CalculatorBindings(
  val display: RemoteString,
  val expression: RemoteString,
  val more: KeyBinding,
  /** `0`…`9` and `.`. */
  val digits: Map<String, Action>,
  val equals: Action,
  /** The operator column, top to bottom: + − × ÷, or ± % √ x² in the second set. */
  val operators: List<KeyBinding>,
  /** Fired where a touch goes down and comes up on the display: the swipe to clear. */
  val swipeStart: Action,
  val swipeEnd: Action,
) {
  init {
    require(operators.size == 4) { "the operator column has four keys" }
  }
}

/** The digit block, row by row; the fourth row is `.` `0` `=`. */
private val digitRows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))

/**
 * The calculator, as Remote Compose content for the Glance Wear **large** widget container.
 *
 * Laid out for the content slot the host leaves inside its padding (200×108dp in the 216×124dp
 * container): a display row with the `f(x)` key, then four rows of four keys. Every key is a pill,
 * so the tap target is visible and the whole slot is used, rather than glyphs floating in empty
 * space as on the round tile it came from.
 */
@RemoteComposable
@Composable
fun CalculatorLayout(bindings: CalculatorBindings) {
  RemoteMaterialTheme {
    // No `spacedBy` anywhere: this player hands weighted children their share of the whole row
    // before the spacing is taken out, so four weighted keys with three gaps overflow the slot by
    // the gaps. Each key instead insets its own pill by half a gap.
    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
      RemoteRow(
        modifier = RemoteModifier.fillMaxWidth().height(DISPLAY_HEIGHT.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically,
      ) {
        Key(bindings.more, RemoteModifier.width(MORE_KEY_WIDTH.rdp), fontSize = 12)
        // The swipe surface. The keys are left out of it, so a tap is never also a swipe.
        RemoteRow(
          modifier =
            RemoteModifier.weight(1f.rf)
              .fillMaxHeight()
              .onTouchDown(bindings.swipeStart)
              .onTouchUp(bindings.swipeEnd)
              // The end inset keeps the last digit clear of the container's rounded corner.
              .padding(start = 4.rdp, end = 10.rdp),
          horizontalArrangement = RemoteArrangement.spacedBy(6.rdp),
          verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
          RemoteText(
            text = bindings.expression,
            color = Palette.secondary.rc,
            fontSize = EXPRESSION_SIZE.rsp,
            maxLines = 1,
          )
          RemoteText(
            text = bindings.display,
            modifier = RemoteModifier.weight(1f.rf),
            color = Palette.operator.rc,
            fontSize = DISPLAY_SIZE.rsp,
            textAlign = TextAlign.End,
            maxLines = 1,
          )
        }
      }
      for (row in 0 until 4) {
        RemoteRow(modifier = RemoteModifier.fillMaxWidth().weight(1f.rf)) {
          val keys = if (row < 3) digitRows[row] else listOf(".", "0")
          keys.forEach { key ->
            Key(
              KeyBinding(
                key.rs,
                Palette.digitContainer.rc,
                Palette.onDigit.rc,
                bindings.digits.getValue(key),
              ),
              RemoteModifier.weight(1f.rf),
            )
          }
          if (row == 3) {
            Key(
              KeyBinding("=".rs, Palette.operator.rc, Palette.onOperator.rc, bindings.equals),
              RemoteModifier.weight(1f.rf),
            )
          }
          Key(bindings.operators[row], RemoteModifier.weight(1f.rf))
        }
      }
    }
  }
}

@RemoteComposable
@Composable
private fun Key(key: KeyBinding, modifier: RemoteModifier, fontSize: Int = KEY_SIZE) {
  RemoteBox(modifier = modifier.fillMaxHeight().padding((KEY_GAP / 2).rdp)) {
    RemoteBox(
      modifier =
        RemoteModifier.fillMaxSize()
          .clip(RemoteRoundedCornerShape(KEY_RADIUS.rdp))
          .background(key.container)
          .clickable(key.action),
      contentAlignment = RemoteAlignment.Center,
    ) {
      RemoteText(text = key.label, color = key.content, fontSize = fontSize.rsp, maxLines = 1)
    }
  }
}

/** The second operator set, by row of the operator column. */
internal val moreOperatorLabels = listOf("±", "%", "√", "x²")

internal const val MORE_LABEL = "f(x)"
internal const val BASIC_LABEL = "+−"

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

internal const val DISPLAY_SIZE = 20
internal const val EXPRESSION_SIZE = 12
private const val KEY_SIZE = 15
private const val KEY_GAP = 3f
private const val KEY_RADIUS = 10f
internal const val DISPLAY_HEIGHT = 26f
private const val MORE_KEY_WIDTH = 34f
