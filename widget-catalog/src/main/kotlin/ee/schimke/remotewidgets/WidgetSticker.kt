@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import ee.schimke.composeai.daemon.RemoteOverridablePreview

/**
 * The frame every sticker in this sheet is captured through: a real `RemoteDocument`, recorded
 * under the **launcher widget** profile and rasterised by the player at the grid size the
 * `@Widget<n>x<m>` annotation pins.
 *
 * `RcPlatformProfiles.WIDGETS_V6` is the profile `RemoteComposeWidget` records its documents with
 * on-device (`RCWidget`'s default), so an operation a launcher widget cannot carry fails here, at
 * capture, exactly as it would on a phone — rather than rendering in the sheet and failing in the
 * launcher. `remote-m3` records under `ANDROIDX` because its stickers are components, not widgets;
 * this sheet's whole subject is the widget.
 *
 * Captured through the connector's [RemoteOverridablePreview] rather than upstream's
 * `RemoteContentPreview` for the same reason `remote-m3`'s `RemoteSticker` is: the captured
 * document lands in the render's `.rc` sidecar, so the sheet ships the document and not only its
 * pixels.
 */
@Composable
fun WidgetSticker(content: @Composable @RemoteComposable () -> Unit) {
  RemoteOverridablePreview(profile = RcPlatformProfiles.WIDGETS_V6) {
    RemoteBox(
      modifier = RemoteModifier.fillMaxSize(),
      contentAlignment = RemoteAlignment.Center,
      content = content,
    )
  }
}
