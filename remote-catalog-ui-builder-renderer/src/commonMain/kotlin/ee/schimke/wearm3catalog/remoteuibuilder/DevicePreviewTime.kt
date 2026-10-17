package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import ee.schimke.composeai.rcplayer.compose.ExperimentalRcPlayerApi
import ee.schimke.composeai.rcplayer.compose.LocalRcInfiniteAnimationPolicy
import ee.schimke.composeai.rcplayer.compose.LocalRcTimeSource
import ee.schimke.composeai.rcplayer.runtime.RcTimeSnapshot
import ee.schimke.composeai.rcplayer.runtime.RcTimeSource
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import ee.schimke.composeai.uibuilder.export.UiExpressions
import ee.schimke.composeai.uibuilder.renderer.sdk.timeRuns
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * [content]'s players on the clock the editor asked for.
 *
 * The editor hands a device pane a document whose `environment.animations` is `running` only while
 * its preview's time toggle is on. Otherwise its wall clock is pinned at the document's `fixedTime`
 * (read as the canvas reads it, its own fields rather than the host's zone).
 *
 * A settled pane also pauses the player's never-ending frame loops
 * ([LocalRcInfiniteAnimationPolicy]): a design that reads `time.animation` or the continuous clock
 * draws its first frame, as the editor's canvas draws it, and the pane idles instead of redrawing
 * every frame. Tweens and springs end, so they still run. Holding animation time with
 * `LocalRcAnimationClock` instead would make the player redraw every frame for any document that
 * declares a float animation (every Remote M3 button carries a press spring), so the pane would
 * never idle.
 */
@OptIn(ExperimentalRcPlayerApi::class)
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
    LocalRcInfiniteAnimationPolicy provides SettledPane,
    content = content,
  )
}

/** Pauses the player's never-ending frame loops for as long as the pane is settled. */
private object SettledPane : InfiniteAnimationPolicy {
  override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R = awaitCancellation()
}

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
