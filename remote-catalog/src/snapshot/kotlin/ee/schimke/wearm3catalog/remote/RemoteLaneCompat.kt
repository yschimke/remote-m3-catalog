package ee.schimke.wearm3catalog.remote

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A named colour, as the released lane's connector `rememberOverridableRemoteColor` writes one, but
 * without the connector: it is compiled against the released alphas' `RemoteColorComposeKt`,
 * which the snapshot folded into `RemoteColorKt`, so calling it here fails at render time with
 * `ClassNotFoundException`. The document still carries the named value a player reseeds; what
 * the snapshot lane loses is the connector's knob declaration, until the connector is built
 * against a line that has the class where the snapshot keeps it.
 */
@Composable
internal fun catalogOverridableRemoteColor(name: String, defaultValue: Color): RemoteColor =
  rememberNamedRemoteColor(name, RemoteColor(defaultValue))
