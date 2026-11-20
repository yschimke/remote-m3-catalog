@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.remote.material3.RemoteBorderStroke
import androidx.wear.compose.remote.material3.RemoteButtonColors
import androidx.wear.compose.remote.material3.RemoteButtonDefaults
import androidx.wear.compose.remote.material3.RemoteIconButtonColors
import androidx.wear.compose.remote.material3.RemoteIconButtonDefaults
import androidx.wear.compose.remote.material3.RemoteTextButtonColors
import androidx.wear.compose.remote.material3.RemoteTextButtonDefaults
import androidx.wear.compose.remote.material3.RemoteButton as LibraryRemoteButton
import androidx.wear.compose.remote.material3.RemoteCompactButton as LibraryRemoteCompactButton
import androidx.wear.compose.remote.material3.RemoteIconButton as LibraryRemoteIconButton
import androidx.wear.compose.remote.material3.RemoteTextButton as LibraryRemoteTextButton

/** A component's click action. */
internal typealias CatalogAction = RemoteAction

/**
 * A named colour, as the released lane's connector `rememberOverridableRemoteColor` writes one, but
 * without the connector: it is compiled against the released alphas' `RemoteColorComposeKt`, which
 * the snapshot folded into `RemoteColorKt`, so calling it here fails at render time with
 * `ClassNotFoundException`. The document still carries the named value a player reseeds; what the
 * snapshot lane loses is the connector's knob declaration, until the connector is built against a
 * line that has the class where the snapshot keeps it.
 */
@Composable
internal fun catalogOverridableRemoteColor(name: String, defaultValue: Color): RemoteColor =
  rememberNamedRemoteColor(name, RemoteColor(defaultValue))

private fun stroke(width: RemoteDp?, color: RemoteColor?): RemoteBorderStroke? =
  if (width == null || color == null) null else RemoteBorderStroke(width, color)

@Composable
@RemoteComposable
internal fun RemoteButton(
  onClick: RemoteAction,
  modifier: RemoteModifier = RemoteModifier,
  enabled: RemoteBoolean = true.rb,
  colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
  border: RemoteDp?,
  borderColor: RemoteColor?,
  content: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
  LibraryRemoteButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    border = stroke(border, borderColor),
    content = content,
  )
}

@Composable
@RemoteComposable
internal fun RemoteButton(
  onClick: RemoteAction,
  modifier: RemoteModifier = RemoteModifier,
  secondaryLabel: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
  icon: (@Composable @RemoteComposable () -> Unit)? = null,
  enabled: RemoteBoolean = true.rb,
  colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
  border: RemoteDp?,
  borderColor: RemoteColor?,
  label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
  LibraryRemoteButton(
    onClick = onClick,
    modifier = modifier,
    secondaryLabel = secondaryLabel,
    icon = icon,
    enabled = enabled,
    colors = colors,
    border = stroke(border, borderColor),
    label = label,
  )
}

@Composable
@RemoteComposable
internal fun RemoteCompactButton(
  onClick: RemoteAction,
  modifier: RemoteModifier = RemoteModifier,
  icon: (@Composable @RemoteComposable () -> Unit)? = null,
  enabled: RemoteBoolean = true.rb,
  colors: RemoteButtonColors = RemoteButtonDefaults.buttonColors(),
  border: RemoteDp?,
  borderColor: RemoteColor?,
  label: @Composable @RemoteComposable (RemoteRowScope.() -> Unit)? = null,
) {
  LibraryRemoteCompactButton(
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    enabled = enabled,
    colors = colors,
    border = stroke(border, borderColor),
    label = label,
  )
}

@Composable
@RemoteComposable
internal fun RemoteTextButton(
  onClick: RemoteAction,
  modifier: RemoteModifier = RemoteModifier,
  enabled: RemoteBoolean = true.rb,
  colors: RemoteTextButtonColors = RemoteTextButtonDefaults.textButtonColors(),
  border: RemoteDp?,
  borderColor: RemoteColor?,
  content: @Composable @RemoteComposable () -> Unit,
) {
  LibraryRemoteTextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    border = stroke(border, borderColor),
    content = content,
  )
}

@Composable
@RemoteComposable
internal fun RemoteIconButton(
  onClick: RemoteAction,
  modifier: RemoteModifier = RemoteModifier,
  enabled: RemoteBoolean = true.rb,
  colors: RemoteIconButtonColors = RemoteIconButtonDefaults.iconButtonColors(),
  border: RemoteDp?,
  borderColor: RemoteColor?,
  content: @Composable @RemoteComposable () -> Unit,
) {
  LibraryRemoteIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    border = stroke(border, borderColor),
    content = content,
  )
}
