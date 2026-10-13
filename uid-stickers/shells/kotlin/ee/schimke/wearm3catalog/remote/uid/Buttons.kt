@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote.uid

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.runtime.Composable
import ee.schimke.composeai.overrides.previewOverrideBoolean
import ee.schimke.composeai.overrides.previewOverrideChoice
import ee.schimke.composeai.preview.CatalogComponent
import ee.schimke.wearm3catalog.remote.CatalogRemoteModes
import ee.schimke.wearm3catalog.remote.KitCopy
import ee.schimke.wearm3catalog.remote.RemoteButtonKitCells
import ee.schimke.wearm3catalog.remote.RemoteSticker
import ee.schimke.wearm3catalog.remote.countedRemote
import ee.schimke.wearm3catalog.remote.kitOutlinedBorderColor
import generated.uibuilder.remote.KitChildButtonRemote
import generated.uibuilder.remote.KitChildButtonRemoteIcon
import generated.uibuilder.remote.KitChildButtonRemoteIconExtraLarge
import generated.uibuilder.remote.KitChildButtonRemoteIconLarge
import generated.uibuilder.remote.KitChildButtonRemoteLeft
import generated.uibuilder.remote.KitFilledButtonRemote
import generated.uibuilder.remote.KitFilledButtonRemoteIcon
import generated.uibuilder.remote.KitFilledButtonRemoteIconExtraLarge
import generated.uibuilder.remote.KitFilledButtonRemoteIconLarge
import generated.uibuilder.remote.KitFilledButtonRemoteLeft
import generated.uibuilder.remote.KitFilledVariantButtonRemote
import generated.uibuilder.remote.KitFilledVariantButtonRemoteIcon
import generated.uibuilder.remote.KitFilledVariantButtonRemoteIconExtraLarge
import generated.uibuilder.remote.KitFilledVariantButtonRemoteIconLarge
import generated.uibuilder.remote.KitFilledVariantButtonRemoteLeft
import generated.uibuilder.remote.KitOutlinedButtonRemote
import generated.uibuilder.remote.KitOutlinedButtonRemoteIcon
import generated.uibuilder.remote.KitOutlinedButtonRemoteIconExtraLarge
import generated.uibuilder.remote.KitOutlinedButtonRemoteIconLarge
import generated.uibuilder.remote.KitOutlinedButtonRemoteLeft
import generated.uibuilder.remote.KitTonalButtonRemote
import generated.uibuilder.remote.KitTonalButtonRemoteIcon
import generated.uibuilder.remote.KitTonalButtonRemoteIconExtraLarge
import generated.uibuilder.remote.KitTonalButtonRemoteIconLarge
import generated.uibuilder.remote.KitTonalButtonRemoteLeft

// The shells read `RemoteKitButton`'s knobs in the same order and pick the cell: the label-only
// button (`Alignment` as a cell) or the kit's `Icon=Yes` overload (`Icon size` as a cell). The
// click and the enabled state are the components' arguments; the outlined style's border colour is
// one too, because Wear dims it to `onSurface` at 20% when disabled and a document cannot yet spell
// a theme role at an opacity.

@CatalogComponent(
  id = "Button/Filled",
  group = "Buttons",
  parallel = "Button/Filled",
  reference = "figma:B24oss2tTeXAFykyeyusz0/35239:93092",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/35239:93088",
  caption = "Remote Material 3 filled button — the primary action.",
)
@CatalogRemoteModes
@RemoteButtonKitCells
@Composable
fun FilledRemoteButton() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.PRIMARY_LABEL)
  val enabled = previewOverrideBoolean("enabled", true).rb
  if (!previewOverrideBoolean("icon", false)) {
    if (previewOverrideChoice("alignment", "center", listOf("center", "left")) == "left") {
      KitFilledButtonRemoteLeft(RemoteModifier, enabled, label, onClick)
    } else {
      KitFilledButtonRemote(RemoteModifier, enabled, label, onClick)
    }
  } else {
    when (previewOverrideChoice("iconSize", "default", listOf("default", "large", "extra-large"))) {
      "large" -> KitFilledButtonRemoteIconLarge(RemoteModifier, enabled, label, onClick)
      "extra-large" -> KitFilledButtonRemoteIconExtraLarge(RemoteModifier, enabled, label, onClick)
      else -> KitFilledButtonRemoteIcon(RemoteModifier, enabled, label, onClick)
    }
  }
}

@CatalogComponent(
  id = "Button/Tonal",
  group = "Buttons",
  parallel = "Button/Tonal",
  reference = "figma:B24oss2tTeXAFykyeyusz0/35239:93104",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/35239:93088",
  caption = "Tonal button on the theme's surface-container emphasis.",
)
@CatalogRemoteModes
@RemoteButtonKitCells
// THE THREE `Disabled=Yes` CELLS ARE DRAWN, and they are the disabled FILLED button to the byte.
// `RemoteButtonDefaults.buttonColors` takes the disabled pair as its own arguments, so a style
// written out by passing `containerColor` and `contentColor` leaves them at their defaults:
//
//   c58fbb6c…  FilledRemoteButton_VARIANT_disabled   == TonalRemoteButton_VARIANT_disabled
//   e12368d1…  …_VARIANT_icon_disabled               == …_VARIANT_icon_disabled
//   4676a294…  …_VARIANT_icon_large_disabled         == …_VARIANT_icon_large_disabled
//
// The Wear column withdraws the equivalent cells for that reason (`EdgeButtonKitCells`). This
// column draws them, because the sheet's job here is to cover what the kit publishes: a reader
// looking for the disabled tonal button finds it, and what they find is the truth about the alpha
// surface — the emphasis does not survive being disabled. Recorded rather than hidden.
@Composable
fun TonalRemoteButton() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.PRIMARY_LABEL)
  val enabled = previewOverrideBoolean("enabled", true).rb
  if (!previewOverrideBoolean("icon", false)) {
    if (previewOverrideChoice("alignment", "center", listOf("center", "left")) == "left") {
      KitTonalButtonRemoteLeft(RemoteModifier, enabled, label, onClick)
    } else {
      KitTonalButtonRemote(RemoteModifier, enabled, label, onClick)
    }
  } else {
    when (previewOverrideChoice("iconSize", "default", listOf("default", "large", "extra-large"))) {
      "large" -> KitTonalButtonRemoteIconLarge(RemoteModifier, enabled, label, onClick)
      "extra-large" -> KitTonalButtonRemoteIconExtraLarge(RemoteModifier, enabled, label, onClick)
      else -> KitTonalButtonRemoteIcon(RemoteModifier, enabled, label, onClick)
    }
  }
}

@CatalogComponent(
  id = "Button/FilledVariant",
  group = "Buttons",
  parallel = "Button/FilledVariant",
  reference = "figma:B24oss2tTeXAFykyeyusz0/39577:895",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/35239:93088",
  caption = "The kit's highlighted style — a filled button in the variant palette.",
)
@CatalogRemoteModes
@RemoteButtonKitCells
// The kit's fifth `Button` style, and the last one this column did not reach: ten of its fifty
// cells had no component at all here, against a Wear column that draws all five styles
// ([#160](https://github.com/yschimke/wear-m3-catalog/issues/160)).
//
// Written out, like `Button/Tonal` beside it, because `RemoteButtonDefaults` publishes exactly one
// `buttonColors()` and no emphasis variants. The tokens come from the Wear function this pairs
// with: `ButtonDefaults.filledVariantButtonColors()` is `primaryContainer` / `onPrimaryContainer`.
@Composable
fun FilledVariantRemoteButton() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.PRIMARY_LABEL)
  val enabled = previewOverrideBoolean("enabled", true).rb
  if (!previewOverrideBoolean("icon", false)) {
    if (previewOverrideChoice("alignment", "center", listOf("center", "left")) == "left") {
      KitFilledVariantButtonRemoteLeft(RemoteModifier, enabled, label, onClick)
    } else {
      KitFilledVariantButtonRemote(RemoteModifier, enabled, label, onClick)
    }
  } else {
    when (previewOverrideChoice("iconSize", "default", listOf("default", "large", "extra-large"))) {
      "large" -> KitFilledVariantButtonRemoteIconLarge(RemoteModifier, enabled, label, onClick)
      "extra-large" -> KitFilledVariantButtonRemoteIconExtraLarge(
        RemoteModifier,
        enabled,
        label,
        onClick,
      )
      else -> KitFilledVariantButtonRemoteIcon(RemoteModifier, enabled, label, onClick)
    }
  }
}

@CatalogComponent(
  id = "Button/Child",
  group = "Buttons",
  parallel = "Button/Child",
  reference = "figma:B24oss2tTeXAFykyeyusz0/35239:93128",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/35239:93088",
  caption = "Lowest emphasis; no container at all, for a button inside another surface.",
)
@CatalogRemoteModes
@RemoteButtonKitCells
// The kit's `Child (No background)` column — a label on whatever is behind it. Transparent
// container plus `onSurface` content is how Wear's own `childButtonColors()` is built, and with no
// container to draw the whole style IS those two colours.
@Composable
fun ChildRemoteButton() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.PRIMARY_LABEL)
  val enabled = previewOverrideBoolean("enabled", true).rb
  if (!previewOverrideBoolean("icon", false)) {
    if (previewOverrideChoice("alignment", "center", listOf("center", "left")) == "left") {
      KitChildButtonRemoteLeft(RemoteModifier, enabled, label, onClick)
    } else {
      KitChildButtonRemote(RemoteModifier, enabled, label, onClick)
    }
  } else {
    when (previewOverrideChoice("iconSize", "default", listOf("default", "large", "extra-large"))) {
      "large" -> KitChildButtonRemoteIconLarge(RemoteModifier, enabled, label, onClick)
      "extra-large" -> KitChildButtonRemoteIconExtraLarge(RemoteModifier, enabled, label, onClick)
      else -> KitChildButtonRemoteIcon(RemoteModifier, enabled, label, onClick)
    }
  }
}

@CatalogComponent(
  id = "Button/Outlined",
  group = "Buttons",
  parallel = "Button/Outlined",
  reference = "figma:B24oss2tTeXAFykyeyusz0/35239:93116",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/35239:93088",
  caption =
    "Remote Material 3 outlined-emphasis button (RemoteButton with an explicit border + border " +
      "colour).",
)
@CatalogRemoteModes
@RemoteButtonKitCells
@Composable
fun OutlinedRemoteButton() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.PRIMARY_LABEL)
  val enabled = previewOverrideBoolean("enabled", true).rb
  val border = kitOutlinedBorderColor(enabled)
  if (!previewOverrideBoolean("icon", false)) {
    if (previewOverrideChoice("alignment", "center", listOf("center", "left")) == "left") {
      KitOutlinedButtonRemoteLeft(RemoteModifier, enabled, border, label, onClick)
    } else {
      KitOutlinedButtonRemote(RemoteModifier, enabled, border, label, onClick)
    }
  } else {
    when (previewOverrideChoice("iconSize", "default", listOf("default", "large", "extra-large"))) {
      "large" -> KitOutlinedButtonRemoteIconLarge(RemoteModifier, enabled, border, label, onClick)
      "extra-large" -> KitOutlinedButtonRemoteIconExtraLarge(
        RemoteModifier,
        enabled,
        border,
        label,
        onClick,
      )
      else -> KitOutlinedButtonRemoteIcon(RemoteModifier, enabled, border, label, onClick)
    }
  }
}
