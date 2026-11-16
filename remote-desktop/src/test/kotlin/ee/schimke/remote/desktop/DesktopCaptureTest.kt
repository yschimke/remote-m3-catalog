package ee.schimke.remote.desktop

import ee.schimke.composeai.rccreation.common.CoreDocument
import ee.schimke.composeai.rccreation.compose.capture.RemoteCreationDisplayInfo
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopCaptureTest {
  @Test
  fun capturesDeterministicRemoteDocumentWithoutAndroid() = runBlocking {
    val first = createDesktopSample()
    val second = createDesktopSample()

    assertTrue(first.isNotEmpty())
    assertContentEquals(first, second)
  }

  /**
   * The capture and the writer are rc-players' fork, in its own package, over its own common write
   * core: none of AndroidX's Java modules (`remote-core`, `remote-creation`) or its JSON importer
   * comes along.
   */
  @Test
  fun capturesThroughTheForkAlone() {
    assertTrue(
      CoreDocument::class.java.protectionDomain.codeSource.location.path.contains("rc-creation-"),
    )
    assertTrue(
      RemoteCreationDisplayInfo::class
        .java
        .protectionDomain
        .codeSource
        .location
        .path
        .contains("rc-creation-compose"),
    )
    for (absent in
      listOf(
        "androidx.compose.remote.core.CoreDocument",
        "androidx.compose.remote.creation.RemoteComposeWriter",
        "androidx.compose.remote.creation.json.RemoteComposeJsonParser",
      )) {
      assertFailsWith<ClassNotFoundException>(absent) { Class.forName(absent) }
    }
  }
}
