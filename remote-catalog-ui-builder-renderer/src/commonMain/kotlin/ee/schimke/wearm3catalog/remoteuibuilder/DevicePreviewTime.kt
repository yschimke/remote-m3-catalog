package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import ee.schimke.composeai.rcplayer.compose.LocalRcAnimationClock
import ee.schimke.composeai.rcplayer.compose.LocalRcTimeSource
import ee.schimke.composeai.rcplayer.compose.RcAnimationClock
import ee.schimke.composeai.rcplayer.runtime.RcTimeSnapshot
import ee.schimke.composeai.rcplayer.runtime.RcTimeSource
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiExpressions
import ee.schimke.composeai.uibuilder.renderer.sdk.timeRuns
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * [content]'s players on the clock the editor asked for.
 *
 * The editor hands a device pane a document whose `environment.animations` is `running` only while
 * its preview's time toggle is on. Otherwise the pane is a still frame like every other render of
 * the design: the wall clock pinned at the document's `fixedTime` (read as the canvas reads it, its
 * own fields rather than the host's zone) and animation time held at its first frame.
 */
@Composable
internal fun ProvideDevicePreviewTime(
  document: UiBuilderDocument,
  content: @Composable () -> Unit,
) {
  if (document.timeRuns) {
    content()
    return
  }
  val fixedTime = (document.environment["fixedTime"] as? JsonPrimitive)?.contentOrNull
  val source = remember(fixedTime) { FixedTimeSource(UiExpressions.Clock.of(fixedTime)) }
  CompositionLocalProvider(
    LocalRcTimeSource provides source,
    LocalRcAnimationClock provides FirstFrame,
    content = content,
  )
}

private val FirstFrame = RcAnimationClock { 0f }

private class FixedTimeSource(private val clock: UiExpressions.Clock) : RcTimeSource {
  override fun currentTimeMillis(): Long = clock.epochMillis

  override fun snapshot(epochMillis: Long): RcTimeSnapshot =
    RcTimeSnapshot(
      epochMillis = clock.epochMillis,
      year = clock.year,
      month = clock.month,
      dayOfMonth = clock.dayOfMonth,
      dayOfYear = clock.dayOfYear,
      hour = clock.hour,
      minute = clock.minute,
      second = clock.second,
      isoDayOfWeek = clock.dayOfWeek,
      offsetSeconds = clock.utcOffsetSeconds,
    )
}
