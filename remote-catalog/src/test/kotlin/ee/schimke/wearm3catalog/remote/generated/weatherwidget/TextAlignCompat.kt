@file:Suppress("RestrictedApi")

package ee.schimke.wearm3catalog.remote.generated.weatherwidget

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.remote.material3.RemoteText as LibraryRemoteText

/**
 * Temporary test-only shim while `compose-preview-ui-builder-export` still emits
 * `androidx.compose.ui.text.style.TextAlign` into `WidgetTemplate_weather-widget.kt` instead of
 * `RemoteTextAlign`. Remove once the exporter upgrades to `RemoteTextAlign` and the golden is
 * regenerated.
 */
@RemoteComposable
@Composable
internal fun RemoteText(
  text: RemoteString,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor? = null,
  fontSize: RemoteTextUnit? = null,
  textAlign: TextAlign?,
) {
  val remoteAlign =
    when (textAlign) {
      TextAlign.Left -> RemoteTextAlign.Left
      TextAlign.Right -> RemoteTextAlign.Right
      TextAlign.Center -> RemoteTextAlign.Center
      TextAlign.Justify -> RemoteTextAlign.Justify
      TextAlign.Start -> RemoteTextAlign.Start
      TextAlign.End -> RemoteTextAlign.End
      else -> null
    }
  LibraryRemoteText(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    textAlign = remoteAlign,
  )
}
