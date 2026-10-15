package ee.schimke.remote.embedded

import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureCommonRemoteDocument
import androidx.compose.remote.creation.compose.capture.captureCommonRemoteDocumentInto
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ee.schimke.composeai.rcplayer.protocol.RcDocument
import ee.schimke.composeai.rcplayer.protocol.RcDocumentCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking

/** A small editor-like screen: nested layouts, text, padding, background, sizes. */
@Composable
@RemoteComposable
internal fun FreshScreen(title: String) {
  RemoteColumn(RemoteModifier.fillMaxWidth().padding(8.rf).background(Color(0xFF202124).rc)) {
    RemoteText(title.rs)
    RemoteRow(RemoteModifier.padding(4.rf)) {
      RemoteBox(RemoteModifier.size(24.rdp).background(Color.Red.rc))
      RemoteText("Second".rs)
    }
    RemoteBox(RemoteModifier.size(48.rdp))
  }
}

class EmbeddedDocumentTest {
  private val display = RemoteCreationDisplayInfo(450, 450, 320)

  private suspend fun bytes(title: String): ByteArray =
    captureCommonRemoteDocument(display) { FreshScreen(title) }

  private suspend fun typed(title: String): RcDocument {
    val writer = RcOperationWriter(display)
    captureCommonRemoteDocumentInto(writer, display) { FreshScreen(title) }
    return writer.document()
  }

  @Test
  fun typedCaptureIsTheDecodedByteCapture() = runBlocking {
    val bytes = bytes("Hello")
    val document = typed("Hello")

    // The model the player consumes is identical whichever way it was produced...
    assertEquals(RcDocumentCodec.decode(bytes), document)
    // ...and encoding the typed model reproduces the byte writer exactly, so a typed writer plus
    // RcDocumentCodec.encode could replace RemoteDocumentWriter outright.
    assertContentEquals(bytes, RcDocumentCodec.encode(document))
  }

  @Test
  fun timing() = runBlocking {
    val n = 200
    var sink = 0
    val sample = bytes("t0")
    // Three interleaved rounds; the last is reported, so JIT warm-up does not favour a path.
    lateinit var report: String
    repeat(3) {
      val toBytes = measureTime { repeat(n) { sink += bytes("t$it").size } }
      val toBytesDecoded = measureTime {
        repeat(n) { sink += RcDocumentCodec.decode(bytes("t$it")).operations.size }
      }
      val toTyped = measureTime { repeat(n) { sink += typed("t$it").operations.size } }
      val decodeOnly = measureTime {
        repeat(n) { sink += RcDocumentCodec.decode(sample).operations.size }
      }
      report =
        """
        |embedded-document timing, mean of $n fresh captures (sink=$sink)
        |  capture -> bytes                 ${toBytes / n}
        |  capture -> bytes -> decode       ${toBytesDecoded / n}
        |  capture -> RcDocument (typed)    ${toTyped / n}
        |  decode only                      ${decodeOnly / n}
        |  document: ${sample.size} bytes, ${RcDocumentCodec.decode(sample).operations.size} operations
        """
          .trimMargin()
    }
    println(report)
  }
}
