@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.foundation.layout.RemoteSpacer
import androidx.compose.remote.foundation.text.RemoteBasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import ee.schimke.composeai.preview.CatalogComponent

// ---------------------------------------------------------------------------------------------
// The `remote-widgets` sheet. Every sticker is a RemoteDocument recorded under the launcher
// widget profile (`WidgetSticker`) and framed at a launcher GRID size (`WidgetSizes.kt`), never at
// a device size.
//
// No component here has a design kit behind it — there is no published Figma kit for Remote
// Compose launcher widgets — so every one enters through the library's door with a stated
// `noReference`, the same door `remote-m3` uses for its shaders and specimens.
// ---------------------------------------------------------------------------------------------

private const val NO_KIT = "No published design kit covers Remote Compose launcher widgets."

// ── Widget ─────────────────────────────────────────────────────────────────────────────────────

@CatalogComponent(
  id = "LauncherWidget/Counter",
  group = "Widget",
  noReference = NO_KIT,
  caption =
    "The worked sample: AndroidX's MyWidget counter rebuilt from this sheet's WidgetSurface and " +
      "WidgetButton, drawn at 3x2 and resized across the grid as a launcher would.",
)
@WidgetSizes
@Composable
fun CounterWidgetSticker() = WidgetSticker { CounterContent(count = 3) }

/**
 * The launcher grid itself: an empty [WidgetSurface] at every footprint this sheet names, labelled
 * with its cell count and the dp the documented formula gives it. The label is read back from the
 * frame the preview was given, so each render states the size it was actually drawn at.
 */
@CatalogComponent(
  id = "LauncherWidget/Grid",
  group = "Widget",
  noReference = NO_KIT,
  caption =
    "The launcher grid sizes a widget design is authored in — 1x1 to 5x2 — at Android's " +
      "documented portrait cell size, (73n − 16) × (118m − 16) dp.",
)
@Widget1x1
@Widget2x1
@Widget2x2
@Widget3x1
@Widget3x2
@Widget4x1
@Widget4x2
@Widget4x3
@Widget5x2
@Composable
fun WidgetGridSticker() {
  val configuration = LocalConfiguration.current
  val size =
    WidgetSize.entries.firstOrNull {
      it.widthDp == configuration.screenWidthDp && it.heightDp == configuration.screenHeightDp
    }
  WidgetSticker {
    WidgetSurface(color = WidgetColors.AccentContainer) {
      RemoteText(
        (size?.label ?: "?").rs,
        color = RemoteColor(WidgetColors.OnAccentContainer),
        fontSize = 16.rsp,
      )
    }
  }
}

// ── Containment ────────────────────────────────────────────────────────────────────────────────

@CatalogComponent(
  id = "WidgetSurface",
  group = "Containment",
  noReference = NO_KIT,
  caption =
    "The widget background: fills the cell, rounds it to Android 12's 16dp widget radius and " +
      "pads its content by 12dp.",
)
@Widget2x1
@Composable
fun WidgetSurfaceSticker() = WidgetSticker { WidgetSurface { WidgetTitle("Surface") } }

// ── Buttons ────────────────────────────────────────────────────────────────────────────────────

@CatalogComponent(
  id = "WidgetButton",
  group = "Buttons",
  noReference = NO_KIT,
  caption =
    "A pill around a centred label — MyWidget's Button. Its onClick is a widget lambda action " +
      "routed back to the RemoteComposeWidget that recorded it.",
)
@Widget2x1
@Composable
fun WidgetButtonSticker() = WidgetSticker { WidgetButton("Open") }

// ── Text ───────────────────────────────────────────────────────────────────────────────────────

@CatalogComponent(
  id = "WidgetTitle",
  group = "Text",
  noReference = NO_KIT,
  caption = "A widget's headline: one line of RemoteText at 20sp medium.",
)
@Widget3x1
@Composable
fun WidgetTitleSticker() = WidgetSticker {
  WidgetSurface(contentAlignment = RemoteAlignment.CenterStart) { WidgetTitle("Next: Standup") }
}

@CatalogComponent(
  id = "WidgetLabel",
  group = "Text",
  noReference = NO_KIT,
  caption = "A widget's supporting line: RemoteText at 14sp in the variant colour.",
)
@Widget3x1
@Composable
fun WidgetLabelSticker() = WidgetSticker {
  WidgetSurface(contentAlignment = RemoteAlignment.CenterStart) {
    WidgetLabel("10:30 – 10:45 · Room 4")
  }
}

@CatalogComponent(
  id = "RemoteText",
  group = "Text",
  noReference = NO_KIT,
  caption =
    "remote-creation-compose's RemoteText — the text every component above is written in, " +
      "with no Material theme behind it.",
)
@Widget3x1
@Composable
fun RemoteTextSticker() = WidgetSticker {
  WidgetSurface {
    RemoteText("RemoteText".rs, color = RemoteColor(WidgetColors.OnSurface), fontSize = 24.rsp)
  }
}

@CatalogComponent(
  id = "RemoteBasicText",
  group = "Text",
  noReference = NO_KIT,
  caption = "remote-foundation's RemoteBasicText: the foundation layer's own text entry point.",
)
@Widget3x1
@Composable
fun RemoteBasicTextSticker() = WidgetSticker {
  WidgetSurface {
    RemoteBasicText(
      "RemoteBasicText".rs,
      color = RemoteColor(WidgetColors.OnSurface),
      fontSize = 24.rsp,
    )
  }
}

// ── Layout ─────────────────────────────────────────────────────────────────────────────────────

/** A filled square, so a layout specimen shows where its children went. */
@RemoteComposable
@Composable
private fun Swatch(color: Color, modifier: RemoteModifier = RemoteModifier) {
  RemoteBox(modifier.size(32.rdp).clip(RemoteRoundedCornerShape(8.rdp)).background(color.rc))
}

@CatalogComponent(
  id = "RemoteRow",
  group = "Layout",
  noReference = NO_KIT,
  caption = "RemoteRow, its children spaced evenly across a 3x1 widget.",
)
@Widget3x1
@Composable
fun RemoteRowSticker() = WidgetSticker {
  WidgetSurface {
    RemoteRow(
      RemoteModifier.fillMaxWidth(),
      horizontalArrangement = RemoteArrangement.SpaceEvenly,
      verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
      Swatch(WidgetColors.Accent)
      Swatch(WidgetColors.AccentContainer)
      Swatch(WidgetColors.OnSurfaceVariant)
    }
  }
}

@CatalogComponent(
  id = "RemoteColumn",
  group = "Layout",
  noReference = NO_KIT,
  caption = "RemoteColumn: a title over a supporting line, the commonest widget body.",
)
@Widget2x2
@Composable
fun RemoteColumnSticker() = WidgetSticker {
  WidgetSurface(contentAlignment = RemoteAlignment.TopStart) {
    RemoteColumn(RemoteModifier.fillMaxSize(), verticalArrangement = RemoteArrangement.Top) {
      WidgetTitle("Today")
      WidgetLabel("3 events")
      RemoteSpacer(RemoteModifier.size(8.rdp))
      Swatch(WidgetColors.Accent)
    }
  }
}

@CatalogComponent(
  id = "RemoteBox",
  group = "Layout",
  noReference = NO_KIT,
  caption = "RemoteBox stacking its children, the later drawn over the earlier.",
)
@Widget1x1
@Composable
fun RemoteBoxSticker() = WidgetSticker {
  WidgetSurface {
    RemoteBox(contentAlignment = RemoteAlignment.Center) {
      Swatch(WidgetColors.AccentContainer, RemoteModifier.size(40.rdp))
      Swatch(WidgetColors.Accent, RemoteModifier.size(20.rdp))
    }
  }
}

@CatalogComponent(
  id = "RemoteSpacer",
  group = "Layout",
  noReference = NO_KIT,
  caption = "remote-foundation's RemoteSpacer holding two children 24dp apart.",
)
@Widget2x1
@Composable
fun RemoteSpacerSticker() = WidgetSticker {
  WidgetSurface {
    RemoteRow(verticalAlignment = RemoteAlignment.CenterVertically) {
      Swatch(WidgetColors.Accent)
      RemoteSpacer(RemoteModifier.size(24.rdp))
      Swatch(WidgetColors.Accent)
    }
  }
}
