package ee.schimke.remote.embedded

import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDocumentSession
import androidx.compose.remote.creation.compose.capture.captureCommonRemoteDocument
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import ee.schimke.composeai.rcplayer.protocol.RcDocument
import ee.schimke.composeai.rcplayer.protocol.RcDocumentCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * One remote composition kept alive under a host recomposer; each "edit" mutates authored state,
 * recomposes, and renders straight to an [RcDocument] — the shape a UI-builder canvas would take.
 */
class LiveSessionTest {
  private val display = RemoteCreationDisplayInfo(450, 450, 320)

  @Test
  fun liveSessionEditsMatchFreshCapturesAndAreCheap() =
    runBlocking(Dispatchers.Default.limitedParallelism(1)) {
      val clock = BroadcastFrameClock()
      val recomposer = Recomposer(coroutineContext + clock)
      val runner = launch(clock) { recomposer.runRecomposeAndApplyChanges() }

      var title by mutableStateOf("Hello")
      val session = RemoteDocumentSession(display, recomposer)
      session.setContent { FreshScreen(title) }

      suspend fun settle() {
        Snapshot.sendApplyNotifications()
        yield()
        while (recomposer.hasPendingWork) {
          clock.sendFrame(System.nanoTime())
          yield()
        }
      }

      fun document(): RcDocument = session.render(RcOperationWriter(display)).document()

      settle()
      assertEquals(
        RcDocumentCodec.decode(captureCommonRemoteDocument(display) { FreshScreen("Hello") }),
        document(),
      )

      val n = 200
      var recompose = Duration.ZERO
      var render = Duration.ZERO
      repeat(3) { round ->
        recompose = Duration.ZERO
        render = Duration.ZERO
        repeat(n) { i ->
          val start = TimeSource.Monotonic.markNow()
          title = "Title $round.$i"
          settle()
          val recomposed = TimeSource.Monotonic.markNow()
          document()
          recompose += start.elapsedNow() - recomposed.elapsedNow()
          render += recomposed.elapsedNow()
        }
      }
      title = "Final"
      settle()
      assertEquals(
        RcDocumentCodec.decode(captureCommonRemoteDocument(display) { FreshScreen("Final") }),
        document(),
      )
      println(
        """
        |embedded-document live session, mean of $n edits (last of 3 rounds)
        |  recompose                        ${recompose / n}
        |  render -> RcDocument (typed)     ${render / n}
        |  edit total                       ${(recompose + render) / n}
        """
          .trimMargin()
      )

      session.dispose()
      recomposer.cancel()
      runner.join()
    }
}
