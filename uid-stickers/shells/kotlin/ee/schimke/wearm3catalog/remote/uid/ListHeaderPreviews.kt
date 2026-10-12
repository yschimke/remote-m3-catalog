@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote.uid

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import ee.schimke.composeai.overrides.previewOverrideBoolean
import ee.schimke.composeai.overrides.previewOverrideChoice
import ee.schimke.composeai.preview.CatalogComponent
import ee.schimke.composeai.preview.OverrideVariant
import ee.schimke.wearm3catalog.remote.CatalogRemoteModes
import ee.schimke.wearm3catalog.remote.KitCopy
import ee.schimke.wearm3catalog.remote.RemoteSticker
import generated.uibuilder.remote.KitListHeaderRemote
import generated.uibuilder.remote.KitListHeaderRemoteLeft
import generated.uibuilder.remote.KitListSubHeaderRemote
import generated.uibuilder.remote.KitListSubHeaderRemoteCentred
import generated.uibuilder.remote.KitListSubHeaderRemoteIcon
import generated.uibuilder.remote.KitListSubHeaderRemoteIconCentred

// Shells: the hand-written stickers' annotations and names, with bodies generated from
// `stickers/<name>.uid`. The knobs are read here, as the hand-written stickers read them, and
// choose the cell.

@CatalogComponent(
  id = "ListHeader",
  group = "Text",
  parallel = "ListHeader",
  reference = "figma:B24oss2tTeXAFykyeyusz0/38977:66978",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/38977:66977",
  caption = "Titles the screen a list belongs to; the first thing above the list.",
)
@CatalogRemoteModes
// The kit's other axis, `Type = Page-Top | Page-Mid`, is NOT a cell on either column and cannot be
// one: the two cells differ in the space ABOVE them, which is a property of where the header sits
// rather than of the component — and a component sticker is cropped to what it draws, so that space
// is not in the picture either way. Same absence, same reason, as the `catalog` row states.
@OverrideVariant(
  name = "left-aligned",
  strings = ["align=left"],
  kitAxis = "Alignment",
  kitValue = "Left",
)
@Composable
fun ListHeaderRemote() = RemoteSticker {
  val label = KitCopy.TITLE.rs
  if (align() == "left") KitListHeaderRemoteLeft(RemoteModifier, label)
  else KitListHeaderRemote(RemoteModifier, label)
}

@CatalogComponent(
  id = "ListSubHeader",
  group = "Text",
  parallel = "ListSubHeader",
  reference = "figma:B24oss2tTeXAFykyeyusz0/38977:66983",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/38977:66982",
  caption = "Divides a long list into named runs, without leaving the screen's title behind.",
)
@CatalogRemoteModes
// The kit publishes `Icon=Yes` at `Alignment=Left` only — the icon leads the row, so there is
// nothing for a centred variant of it to be — which is why the icon cell names one axis and the
// centred one names the pair. Cell names are the Wear sibling's, so the two columns pair.
@OverrideVariant(
  name = "icon",
  booleans = ["icon=true"],
  kitProps = ["Icon=Yes", "Alignment=Left"],
)
@OverrideVariant(
  name = "centred",
  strings = ["align=centre"],
  kitProps = ["Icon=No", "Alignment=Centre"],
)
@Composable
fun ListSubHeaderRemote() = RemoteSticker {
  val icon = previewOverrideBoolean("icon", false)
  val label = KitCopy.SUBTITLE.rs
  val centred = align(default = "left") == "centre"
  when {
    icon && centred -> KitListSubHeaderRemoteIconCentred(RemoteModifier, label)
    icon -> KitListSubHeaderRemoteIcon(RemoteModifier, label)
    centred -> KitListSubHeaderRemoteCentred(RemoteModifier, label)
    else -> KitListSubHeaderRemote(RemoteModifier, label)
  }
}

@Composable
private fun align(default: String = "centre"): String =
  previewOverrideChoice("align", default, listOf("centre", "left"))
