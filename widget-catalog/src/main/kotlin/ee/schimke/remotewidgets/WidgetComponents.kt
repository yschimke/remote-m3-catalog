@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.remotewidgets

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.widgets.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight

// ---------------------------------------------------------------------------------------------
// The Mobile Launcher Widgets component set.
//
// Built from `remote-creation-compose` and `remote-foundation` ONLY. There is no
// `remote-material3` here, and that is the reason this is a design system of its own rather than
// a mode of `remote-m3`: Remote Material 3 is Wear's component library, themed through
// `RemoteMaterialTheme`, and a phone launcher widget neither has nor needs it. What a widget body
// can call is the creation DSL's layouts and text plus the foundation's basic text, image, icon
// and spacer, so this set is a thin vocabulary over exactly those — the same shape as the
// `MyWidget` demo in AndroidX's `player-view-demos`, whose `Button` is reproduced as
// [WidgetButton] below.
//
// Colours are literals from [WidgetColors], not theme roles: there is no Remote theme to resolve a
// role against, so a role a design wrote would be a name with nothing behind it.
// ---------------------------------------------------------------------------------------------

/**
 * The palette every sticker in this sheet draws with. Literal values, deliberately — see the file
 * comment — taken from the baseline Material 3 light scheme so a widget reads as at home on a
 * default launcher. Mirrored in `ui-builder.policy.json`'s builtin notes.
 */
object WidgetColors {
  val Surface = Color(0xFFF3EDF7)
  val OnSurface = Color(0xFF1D1B20)
  val OnSurfaceVariant = Color(0xFF49454F)
  val Accent = Color(0xFF6750A4)
  val OnAccent = Color(0xFFFFFFFF)
  val AccentContainer = Color(0xFFE8DEF8)
  val OnAccentContainer = Color(0xFF4A4458)
}

/**
 * The widget's background fills the launcher bounds, with padding inside the surface. The launcher
 * owns its outer corner radius; the document must not impose a second rounded clip.
 */
@RemoteComposable
@Composable
fun WidgetSurface(
  modifier: RemoteModifier = RemoteModifier,
  color: Color = WidgetColors.Surface,
  contentAlignment: RemoteAlignment = RemoteAlignment.Center,
  content: @Composable @RemoteComposable () -> Unit,
) {
  RemoteBox(
    modifier.fillMaxSize().background(color.rc).padding(12.rdp),
    contentAlignment = contentAlignment,
    content = content,
  )
}

/** A widget's headline: one line, 20sp medium, on the surface colour. */
@RemoteComposable
@Composable
fun WidgetTitle(
  text: String,
  modifier: RemoteModifier = RemoteModifier,
  color: Color = WidgetColors.OnSurface,
) {
  RemoteText(
    text.rs,
    modifier = modifier,
    color = RemoteColor(color),
    fontSize = 20.rsp,
    fontWeight = FontWeight.Medium,
    maxLines = 1,
  )
}

/** A widget's supporting line: 14sp, the variant colour. */
@RemoteComposable
@Composable
fun WidgetLabel(
  text: String,
  modifier: RemoteModifier = RemoteModifier,
  color: Color = WidgetColors.OnSurfaceVariant,
) {
  RemoteText(text.rs, modifier = modifier, color = RemoteColor(color), fontSize = 14.rsp)
}

/**
 * A tappable pill, the `Button` of AndroidX's `MyWidget` demo: a rounded accent container around a
 * centred label.
 *
 * [onClick] is a widget LAMBDA action (`androidx.compose.remote.creation.compose.widgets.onClick`),
 * which the launcher routes back to the `RemoteComposeWidget` that recorded it. It is optional
 * because a sticker must not record one: each lambda action takes the next id from a process-wide
 * counter, so a capture that registered one would bake a different document depending on which
 * previews rendered before it — and renders here must be deterministic.
 */
@RemoteComposable
@Composable
fun WidgetButton(
  text: String,
  modifier: RemoteModifier = RemoteModifier,
  containerColor: Color = WidgetColors.Accent,
  contentColor: Color = WidgetColors.OnAccent,
  contentDescription: String = text,
  onClick: (() -> Unit)? = null,
) {
  val shaped =
    modifier
      .widthIn(min = 48.rdp)
      .heightIn(min = 48.rdp)
      .clip(RemoteRoundedCornerShape(24.rdp))
      .background(containerColor.rc)
      .semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription.rs
        role = Role.Button
      }
      .padding(start = 16.rdp, top = 8.rdp, end = 16.rdp, bottom = 8.rdp)
  RemoteBox(
    if (onClick != null) shaped.onClick(onClick) else shaped,
    contentAlignment = RemoteAlignment.Center,
  ) {
    RemoteText(text.rs, color = RemoteColor(contentColor), fontSize = 16.rsp, maxLines = 1)
  }
}
