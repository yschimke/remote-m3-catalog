package ee.schimke.remote.calculator

import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.state.DecimalFormat
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ee.schimke.composeai.rcplayer.compose.LocalRcComponentCoordinates
import ee.schimke.composeai.rcplayer.compose.RcComponentCoordinatesListener
import ee.schimke.composeai.rcplayer.embedded.RcEmbeddedPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins why [rememberRegister] exists. The common document writer caches float expressions by
 * content, and a mutable float is declared as one, so two `rememberMutableRemoteFloat(0f)` are one
 * variable: incrementing the first moves the second.
 *
 * Fails in the other direction once the writer gives each mutable float its own id: then delete
 * this test and [rememberRegister], and declare the registers with `rememberMutableRemoteFloat`.
 */
@OptIn(ExperimentalTestApi::class)
class MutableFloatIdentityTest {
  @Test
  fun sameInitialValueIsOneVariable() =
    assertEquals(listOf("1", "1"), afterIncrementingFirst { rememberMutableRemoteFloat(0f) })

  @Test
  fun registersAreSeparateVariables() {
    var index = 0
    assertEquals(listOf("1", "0"), afterIncrementingFirst { rememberRegister(0f, index++) })
  }

  /** Two floats from [declare], shown one per row; taps the first row's increment. */
  private fun afterIncrementingFirst(declare: @Composable () -> MutableRemoteFloat): List<String> {
    val texts = ConcurrentHashMap<Float, String>()
    val listener =
      object : RcComponentCoordinatesListener {
        override fun onPositioned(componentId: Int, coordinates: LayoutCoordinates) = Unit

        override fun onTextLaidOut(id: Int, content: LayoutCoordinates, layout: TextLayoutResult) {
          texts[content.localToRoot(Offset.Zero).y] = layout.layoutInput.text.text
        }
      }
    runComposeUiTest {
      setContent {
        CompositionLocalProvider(LocalRcComponentCoordinates provides listener) {
          RcEmbeddedPlayer(RemoteCreationDisplayInfo(100, 40, 160), Modifier.size(100.dp, 40.dp)) {
            val first = declare()
            val second = declare()
            RemoteColumn {
              listOf(first, second).forEach { value ->
                RemoteBox(
                  RemoteModifier.fillMaxWidth()
                    .height(20.rdp)
                    .clickable(valueChange(value, value + 1f))
                ) {
                  RemoteText(value.toRemoteString(wholeNumbers), fontSize = 10.rsp)
                }
              }
            }
          }
        }
      }
      waitUntil(timeoutMillis = 5_000) { texts.size == 2 }
      onRoot().performTouchInput { click(Offset(50f, 10f)) }
      waitUntil(timeoutMillis = 5_000) { texts.toSortedMap().values.first() == "1" }
      waitForIdle()
    }
    return texts.toSortedMap().values.toList()
  }
}

private val wholeNumbers =
  DecimalFormat().apply {
    isGroupingUsed = false
    maximumFractionDigits = 0
  }
