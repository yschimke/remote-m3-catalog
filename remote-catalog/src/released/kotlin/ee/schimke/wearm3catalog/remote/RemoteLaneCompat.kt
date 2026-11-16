@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.MutableRemoteState
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteDp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.remote.material3.LocalRemoteTextStyle
import ee.schimke.composeai.daemon.rememberOverridableRemoteColor
import androidx.wear.compose.remote.material3.RemoteText as LibraryRemoteText

// The released alphas' spelling of what the shared sources write in the snapshot's.
//
// The shared sources are written against snapshot 16942710 (`remote-creation-compose` after
// alpha23, `remote-material3` after alpha15). Every declaration below is that spelling bridged onto
// the released line, so the call sites stay one source and this file is what goes when alpha24 /
// alpha16 ship. Two ways in, depending on whether the shared file already names the library's
// declaration:
// - an OVERLOAD of a name the shared file imports explicitly (`RemoteText`, `RemoteImage`,
//   `rememberNamedRemoteDp`). An explicit import outranks this package, so these are found only
//   when the library's own overloads do not apply — that is, only for the snapshot's argument
//   types;
// - a declaration the released line has no counterpart for (`valueChangeAction`, `lerp`,
//   `RemoteFontVariation`). The shared files reach those through a star import of the library
//   package, which the snapshot satisfies and which this package outranks on the released lane.

/** A colour the connector can reseed live: its `rememberOverridableRemoteColor`. */
@Composable
internal fun catalogOverridableRemoteColor(name: String, defaultValue: Color): RemoteColor =
  rememberOverridableRemoteColor(name, defaultValue)

/** The snapshot's `valueChangeAction`: alpha23 calls it `valueChange`. */
internal fun <T> valueChangeAction(
  remoteState: MutableRemoteState<T>,
  updatedValue: RemoteState<T>,
): RemoteAction = valueChange(remoteState, updatedValue)

/** The snapshot's colour `lerp`: alpha23 calls it `tween`. */
internal fun lerp(from: RemoteColor, to: RemoteColor, fraction: RemoteFloat): RemoteColor =
  tween(from, to, fraction)

/**
 * The snapshot's `rememberNamedRemoteDp(name, defaultValue)`: alpha23 takes the default as a
 * trailing lambda.
 */
@Composable
@RemoteComposable
internal fun rememberNamedRemoteDp(name: String, defaultValue: RemoteDp): RemoteDp =
  rememberNamedRemoteDp(name) { defaultValue }

/**
 * The snapshot's `RemoteImage` with a [RemoteContentScale] and a default alpha: alpha23's
 * [RemoteContentScale] overload has no defaults, and its defaulted one takes a [ContentScale].
 */
@Composable
@RemoteComposable
internal fun RemoteImage(
  bitmap: RemoteImageBitmap,
  contentDescription: RemoteString?,
  modifier: RemoteModifier = RemoteModifier,
  contentScale: RemoteContentScale = RemoteContentScale.Fit,
  alpha: RemoteFloat = 1f.rf,
) {
  RemoteImage(bitmap, contentDescription, modifier, contentScale = contentScale, alpha = alpha)
}

/**
 * The snapshot's `RemoteFontVariation`, which alpha23 does not publish: axis settings whose values
 * are [RemoteFloat]s. Only the two axes the specimens use; the released `RemoteText` below writes
 * them as `FontVariation.Settings`, so each value has to be a constant.
 */
internal object RemoteFontVariation {
  class Setting(val axisName: String, val value: RemoteFloat)

  class Settings(vararg val settings: Setting)

  fun weight(weight: RemoteFloat): Setting = Setting("wght", weight)

  fun width(width: RemoteFloat): Setting = Setting("wdth", width)
}

private fun RemoteFontVariation.Settings.toCompose(): FontVariation.Settings =
  FontVariation.Settings(
    *settings
      .map { FontVariation.Setting(it.axisName, checkNotNull(it.value.constantValueOrNull)) }
      .toTypedArray()
  )

private fun RemoteTextAlign.toCompose(): TextAlign =
  when (this) {
    RemoteTextAlign.Left -> TextAlign.Left
    RemoteTextAlign.Right -> TextAlign.Right
    RemoteTextAlign.Center -> TextAlign.Center
    RemoteTextAlign.Justify -> TextAlign.Justify
    RemoteTextAlign.Start -> TextAlign.Start
    RemoteTextAlign.End -> TextAlign.End
    else -> TextAlign.Unspecified
  }

private fun RemoteTextOverflow.toCompose(): TextOverflow =
  when (this) {
    RemoteTextOverflow.Clip -> TextOverflow.Clip
    RemoteTextOverflow.Ellipsis -> TextOverflow.Ellipsis
    RemoteTextOverflow.Visible -> TextOverflow.Visible
    RemoteTextOverflow.StartEllipsis -> TextOverflow.StartEllipsis
    RemoteTextOverflow.MiddleEllipsis -> TextOverflow.MiddleEllipsis
    else -> error("No TextOverflow for $this")
  }

private fun RemoteFontWeight.toCompose(): FontWeight =
  FontWeight(checkNotNull(weight.constantValueOrNull).toInt())

/**
 * The snapshot's `RemoteText`, whose text attributes are the `Remote*` types: alpha15 takes Compose
 * UI's [TextAlign], [TextOverflow], [FontWeight] and [FontVariation.Settings]. Defaults are
 * alpha15's own, so an attribute the call site leaves out resolves exactly as it did.
 */
@Composable
@RemoteComposable
internal fun RemoteText(
  text: RemoteString,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor? = null,
  fontSize: RemoteTextUnit? = null,
  fontWeight: RemoteFontWeight? = null,
  fontFamily: RemoteFontFamily? = null,
  textAlign: RemoteTextAlign? = null,
  overflow: RemoteTextOverflow? = null,
  maxLines: Int = LocalTextConfiguration.current.maxLines,
  fontVariationSettings: RemoteFontVariation.Settings? = null,
  style: RemoteTextStyle = LocalRemoteTextStyle.current,
) {
  LibraryRemoteText(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight?.toCompose(),
    fontFamily = fontFamily,
    textAlign = textAlign?.toCompose() ?: LocalTextConfiguration.current.textAlign,
    overflow = overflow?.toCompose() ?: LocalTextConfiguration.current.overflow,
    maxLines = maxLines,
    fontVariationSettings = fontVariationSettings?.toCompose(),
    style = style,
  )
}
