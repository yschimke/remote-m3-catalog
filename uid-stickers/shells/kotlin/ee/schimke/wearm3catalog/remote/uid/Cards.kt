@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote.uid

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.runtime.Composable
import ee.schimke.composeai.preview.CatalogComponent
import ee.schimke.wearm3catalog.remote.CatalogRemoteLarge
import ee.schimke.wearm3catalog.remote.KitCopy
import ee.schimke.wearm3catalog.remote.RemoteSticker
import ee.schimke.wearm3catalog.remote.countedRemote
import generated.uibuilder.remote.KitCardRemote
import generated.uibuilder.remote.KitOutlinedCardRemote

// Shells: the hand-written stickers' annotations and names, with bodies generated from
// `stickers/<name>.uid`. The click is the component's `Action` parameter: the shell passes the same
// counting action the hand-written sticker does.

@CatalogComponent(
  id = "Card",
  group = "Containment",
  parallel = "Card",
  noReference =
    "The kit's `Card` set publishes no title-less cell — all 45 cells are App Card or Title " +
      "Card layouts. This is Remote Material 3's plain one-slot card; the mapped title-card " +
      "cells are published by `TitleCard` and `AppCard`.",
  caption = "Remote Material 3's plain one-slot card.",
)
@CatalogRemoteLarge
@Composable
fun CardRemote() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.CARD_CONTENT)
  KitCardRemote(RemoteModifier, label, onClick)
}

@CatalogComponent(
  id = "Card/Outlined",
  group = "Containment",
  parallel = "Card/Outlined",
  noReference =
    "The kit's `Card` set publishes no title-less outlined cell. Its Outline cells are App Card " +
      "or Title Card layouts; this is Remote Material 3's plain `RemoteOutlinedCard`.",
  caption = "Remote Material 3's plain outlined one-slot card.",
)
@CatalogRemoteLarge
@Composable
fun OutlinedCardRemote() = RemoteSticker {
  val (label, onClick) = countedRemote(KitCopy.CARD_CONTENT)
  KitOutlinedCardRemote(RemoteModifier, label, onClick)
}
