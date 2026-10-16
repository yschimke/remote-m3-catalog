package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.Composable
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * How the runtime turns a design into Remote Compose, chosen per render so both can be compared in
 * the same editor.
 * - [Capture] records a fresh document on every edit and plays it. The editor's Visual Editor keeps
 *   its Compose UI stand-ins.
 * - [Live] keeps the design's remote composition alive under rc-players' `RcEmbeddedPlayer`: an
 *   edit recomposes only what changed. The Visual Editor plays the same recording, so its per-node
 *   bounds come from the player ([RemoteM3AuthoringSurface]).
 *
 * The editor asks for one by putting [ENVIRONMENT_KEY] in the environment of the document it sends,
 * and only to a runtime that announced [CAPABILITY]. Absent or unknown, the runtime uses
 * [defaultPreviewEngine].
 */
internal enum class PreviewEngine(val id: String) {
  Capture("capture"),
  Live("live");

  companion object {
    /** Announced in `initialized`: this runtime honours [ENVIRONMENT_KEY]. */
    const val CAPABILITY = "previewEngines"

    /** The document environment key the editor sets, `capture` or `live`. */
    const val ENVIRONMENT_KEY = "previewEngine"

    fun of(document: UiBuilderDocument): PreviewEngine {
      val requested = runCatching {
        document.environment[ENVIRONMENT_KEY]?.jsonPrimitive?.contentOrNull
      }
        .getOrNull()
      return entries.firstOrNull { it.id == requested } ?: defaultPreviewEngine
    }
  }
}

/**
 * The engine a document that names none is drawn with: [PreviewEngine.Capture], as the runtime
 * shipped. JVM tests can run under the other with `-DremoteM3.previewEngine=live`.
 */
internal expect val defaultPreviewEngine: PreviewEngine

/**
 * Real Remote M3 creation and playback for the read-only browser device surface, by the [engine]
 * the document asks for.
 *
 * [typefaceFallback] answers for every family the runtime's font registry does not: the player's
 * own default face unless a test stands in for the browser's.
 */
@Composable
internal fun RemoteM3DevicePreview(
  document: UiBuilderDocument,
  widthDp: Float,
  heightDp: Float,
  typefaceFallback: RcTypefaceLoader = RcTypefaceLoader.Default,
  engine: PreviewEngine = PreviewEngine.of(document),
  onReady: () -> Unit,
) {
  when (engine) {
    PreviewEngine.Capture ->
      CaptureDevicePreview(document, widthDp, heightDp, typefaceFallback, onReady = onReady)
    PreviewEngine.Live ->
      LiveDevicePreview(document, widthDp, heightDp, typefaceFallback, onReady = onReady)
  }
}
