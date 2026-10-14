@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote.uid

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.runtime.Composable
import ee.schimke.composeai.overrides.previewOverrideBoolean
import ee.schimke.composeai.overrides.previewOverrideChoice
import ee.schimke.composeai.preview.CatalogComponent
import ee.schimke.wearm3catalog.remote.CatalogRemoteModes
import ee.schimke.wearm3catalog.remote.RemoteSticker
import ee.schimke.wearm3catalog.remote.RemoteTextButtonKitCells
import ee.schimke.wearm3catalog.remote.kitOutlinedBorderColor
import ee.schimke.wearm3catalog.remote.toggledRemote
import generated.uibuilder.remote.KitTextButtonChildRemote
import generated.uibuilder.remote.KitTextButtonChildRemoteLarge
import generated.uibuilder.remote.KitTextButtonChildRemoteSmall
import generated.uibuilder.remote.KitTextButtonFilledRemote
import generated.uibuilder.remote.KitTextButtonFilledRemoteLarge
import generated.uibuilder.remote.KitTextButtonFilledRemoteSmall
import generated.uibuilder.remote.KitTextButtonFilledVariantRemote
import generated.uibuilder.remote.KitTextButtonFilledVariantRemoteLarge
import generated.uibuilder.remote.KitTextButtonFilledVariantRemoteSmall
import generated.uibuilder.remote.KitTextButtonOutlinedRemote
import generated.uibuilder.remote.KitTextButtonOutlinedRemoteLarge
import generated.uibuilder.remote.KitTextButtonOutlinedRemoteSmall
import generated.uibuilder.remote.KitTextButtonTonalRemote
import generated.uibuilder.remote.KitTextButtonTonalRemoteLarge
import generated.uibuilder.remote.KitTextButtonTonalRemoteSmall

// The shell reads the hand-written sticker's knobs in the same order and picks the document (the
// kit's `Style`) and its cell (`Size`). The colours tween across `on`, the 0→1 float
// `toggledRemote()` flips on a tap: the document spells `lerp(theme.primary, theme.primaryDim, @on)`,
// which is the player's colour `tween`, so a baked capture draws the resting colour and a live tap
// moves it. The outlined style's border colour is an argument, as on `Button/Outlined`.

@CatalogComponent(
  id = "TextButton",
  group = "Buttons",
  parallel = "TextButton",
  reference = "figma:B24oss2tTeXAFykyeyusz0/34732:103081",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/34732:103080",
  caption =
    "Low-emphasis round text button (RemoteTextButton), with the kit's style and size " +
      "axes folded in.",
)
@CatalogRemoteModes
@RemoteTextButtonKitCells
@Composable
fun TextRemoteButton() = RemoteSticker {
  val (on, onClick) = toggledRemote()
  val style =
    previewOverrideChoice(
      "style",
      "filled",
      listOf("filled", "filled-variant", "tonal", "child", "outlined"),
    )
  val size = previewOverrideChoice("size", "default", listOf("default", "small", "large"))
  val enabled = previewOverrideBoolean("enabled", true).rb
  when (style to size) {
      "filled" to "default" ->
        KitTextButtonFilledRemote(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "filled" to "small" ->
        KitTextButtonFilledRemoteSmall(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "filled" to "large" ->
        KitTextButtonFilledRemoteLarge(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "filled-variant" to "default" ->
        KitTextButtonFilledVariantRemote(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "filled-variant" to "small" ->
        KitTextButtonFilledVariantRemoteSmall(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "filled-variant" to "large" ->
        KitTextButtonFilledVariantRemoteLarge(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "tonal" to "default" ->
        KitTextButtonTonalRemote(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "tonal" to "small" ->
        KitTextButtonTonalRemoteSmall(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "tonal" to "large" ->
        KitTextButtonTonalRemoteLarge(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "child" to "default" ->
        KitTextButtonChildRemote(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "child" to "small" ->
        KitTextButtonChildRemoteSmall(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "child" to "large" ->
        KitTextButtonChildRemoteLarge(RemoteModifier, on = on, enabled = enabled, onButtonClick = onClick)
      "outlined" to "default" ->
        KitTextButtonOutlinedRemote(RemoteModifier, on = on, enabled = enabled, borderColor = kitOutlinedBorderColor(enabled), onButtonClick = onClick)
      "outlined" to "small" ->
        KitTextButtonOutlinedRemoteSmall(RemoteModifier, on = on, enabled = enabled, borderColor = kitOutlinedBorderColor(enabled), onButtonClick = onClick)
      "outlined" to "large" ->
        KitTextButtonOutlinedRemoteLarge(RemoteModifier, on = on, enabled = enabled, borderColor = kitOutlinedBorderColor(enabled), onButtonClick = onClick)
    else -> error("unknown cell $style/$size")
  }
}
