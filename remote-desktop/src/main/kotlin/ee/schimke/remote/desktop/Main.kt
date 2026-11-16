package ee.schimke.remote.desktop

import ee.schimke.composeai.rccreation.compose.capture.RemoteCreationDisplayInfo
import ee.schimke.composeai.rccreation.compose.capture.RemoteDensityBehavior
import ee.schimke.composeai.rccreation.compose.capture.captureCommonRemoteDocument
import ee.schimke.composeai.rccreation.compose.layout.RemoteColumn
import ee.schimke.composeai.rccreation.compose.layout.RemoteText
import ee.schimke.composeai.rccreation.compose.state.rs
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

internal suspend fun createDesktopSample(): ByteArray =
  captureCommonRemoteDocument(
    creationDisplayInfo =
      RemoteCreationDisplayInfo(
        width = 192,
        height = 192,
        densityDpi = 160,
        densityBehavior = RemoteDensityBehavior.Dp,
      ),
  ) {
    RemoteColumn { RemoteText("Remote Compose on Desktop".rs) }
  }

fun main(args: Array<String>) = runBlocking {
  val document = createDesktopSample()
  check(document.isNotEmpty()) { "Desktop capture produced an empty Remote Compose document" }
  val output = Path.of(args.firstOrNull() ?: "build/desktop-sample.rc")
  output.parent?.let(Files::createDirectories)
  Files.write(output, document)
  println("Wrote ${document.size} bytes to ${output.toAbsolutePath()}")
}
