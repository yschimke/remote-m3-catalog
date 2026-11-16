package ee.schimke.wearm3catalog.remote

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.wear.compose.remote.material3.RemoteButtonDefaults

/**
 * `remote-material3`'s own `buttonSizeModifier()`, the minimum a label-only [RemoteButton] measures
 * to: the kit's one-line cell height. Spelled out here because the released `remote-material3`
 * keeps it `internal` (the snapshot publishes it); the body is the library's, call for call.
 */
internal fun RemoteModifier.kitButtonSize(): RemoteModifier =
  defaultMinSize(minWidth = RemoteButtonDefaults.Width, minHeight = RemoteButtonDefaults.Height)
