package ee.schimke.remotewidgets

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [bestFitSize] is the choice [AdaptiveWidget] records when it knows its size, and the expression
 * it plays back when it does not computes the same thing, so it is the one rule both paths answer
 * to: Glance's `SizeMode.Responsive` pick.
 */
class AdaptiveWidgetTest {

  private val small = WidgetSize.S2x1.dpSize
  private val wide = WidgetSize.S4x1.dpSize
  private val big = WidgetSize.S4x2.dpSize

  @Test
  fun `an exact breakpoint picks itself`() {
    DestinationsSizes.forEach { assertThat(bestFitSize(it, DestinationsSizes)).isEqualTo(it) }
  }

  @Test
  fun `between breakpoints, the closest one that fits`() {
    assertThat(bestFitSize(WidgetSize.S3x1.dpSize, DestinationsSizes)).isEqualTo(small)
    assertThat(bestFitSize(WidgetSize.S5x2.dpSize.copy(height = 150.dp), DestinationsSizes))
      .isEqualTo(wide)
    assertThat(bestFitSize(WidgetSize.S5x2.dpSize, DestinationsSizes)).isEqualTo(big)
  }

  @Test
  fun `tall but narrow fits only the smallest`() {
    assertThat(bestFitSize(WidgetSize.S2x2.dpSize, DestinationsSizes)).isEqualTo(small)
  }

  @Test
  fun `nothing fits, so the smallest`() {
    assertThat(bestFitSize(WidgetSize.S1x1.dpSize, DestinationsSizes)).isEqualTo(small)
  }

  @Test
  fun `a window a fraction of a dp short of a breakpoint still fits it`() {
    assertThat(bestFitSize(DpSize(275.7.dp, 101.8.dp), DestinationsSizes)).isEqualTo(wide)
  }

  @Test
  fun `the order the sizes are given in does not matter`() {
    assertThat(bestFitSize(big, listOf(big, wide, small))).isEqualTo(big)
    assertThat(bestFitSize(WidgetSize.S1x1.dpSize, listOf(big, wide, small))).isEqualTo(small)
  }
}
