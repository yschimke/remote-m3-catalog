package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.remote.material3.RemoteTypography
import ee.schimke.composeai.rcplayer.compose.RcFontVariations
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.uibuilder.UiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.FontSettings
import ee.schimke.composeai.uibuilder.export.ThemeTypefaces

/**
 * The family a widget container's theme names for each Remote Material 3 role, by role name, as the
 * `google:`-prefixed name the document carries.
 *
 * `RemoteFontFamily.Named("google:…")` is what the generated widget writes (compose-ui-builder's
 * `RemoteContentEmitter.themeTypography`), so the device preview records the same document the
 * widget will ship.
 */
internal fun Map<ThemeTypefaces.Group, String>.remoteRoleNames(): Map<String, String> =
  ThemeTypefaces.wearRoleFamilies(this).mapValues { (_, family) ->
    "google:${ThemeTypefaces.familyName(family)}"
  }

/**
 * This scale with each role in [names] set to its named family.
 *
 * Every role explicitly, for the reason `RemoteCatalogFonts.withFamilies` gives:
 * `RemoteTypography(defaultFontFamily = …)` fills a family in only where a style has none, and
 * every stock role declares one.
 */
internal fun RemoteTypography.withRoleNames(names: Map<String, String>): RemoteTypography {
  if (names.isEmpty()) return this
  fun f(role: String) = names[role]?.let(RemoteFontFamily::Named)
  return copy(
    displayLarge = f("displayLarge")?.let { displayLarge.copy(fontFamily = it) } ?: displayLarge,
    displayMedium =
      f("displayMedium")?.let { displayMedium.copy(fontFamily = it) } ?: displayMedium,
    displaySmall = f("displaySmall")?.let { displaySmall.copy(fontFamily = it) } ?: displaySmall,
    titleLarge = f("titleLarge")?.let { titleLarge.copy(fontFamily = it) } ?: titleLarge,
    titleMedium = f("titleMedium")?.let { titleMedium.copy(fontFamily = it) } ?: titleMedium,
    titleSmall = f("titleSmall")?.let { titleSmall.copy(fontFamily = it) } ?: titleSmall,
    labelLarge = f("labelLarge")?.let { labelLarge.copy(fontFamily = it) } ?: labelLarge,
    labelMedium = f("labelMedium")?.let { labelMedium.copy(fontFamily = it) } ?: labelMedium,
    labelSmall = f("labelSmall")?.let { labelSmall.copy(fontFamily = it) } ?: labelSmall,
    bodyLarge = f("bodyLarge")?.let { bodyLarge.copy(fontFamily = it) } ?: bodyLarge,
    bodyMedium = f("bodyMedium")?.let { bodyMedium.copy(fontFamily = it) } ?: bodyMedium,
    bodySmall = f("bodySmall")?.let { bodySmall.copy(fontFamily = it) } ?: bodySmall,
    bodyExtraSmall =
      f("bodyExtraSmall")?.let { bodyExtraSmall.copy(fontFamily = it) } ?: bodyExtraSmall,
    numeralExtraLarge =
      f("numeralExtraLarge")?.let { numeralExtraLarge.copy(fontFamily = it) } ?: numeralExtraLarge,
    numeralLarge = f("numeralLarge")?.let { numeralLarge.copy(fontFamily = it) } ?: numeralLarge,
    numeralMedium =
      f("numeralMedium")?.let { numeralMedium.copy(fontFamily = it) } ?: numeralMedium,
    numeralSmall = f("numeralSmall")?.let { numeralSmall.copy(fontFamily = it) } ?: numeralSmall,
    numeralExtraSmall =
      f("numeralExtraSmall")?.let { numeralExtraSmall.copy(fontFamily = it) } ?: numeralExtraSmall,
  )
}

/**
 * The player's faces: whatever the runtime's font registry has loaded, by the name the document
 * carries, over the player's own defaults for everything else.
 *
 * [UiBuilderFontRegistry.loaded] is snapshot state, so a player that asked for a family before it
 * arrived draws again in it once it does.
 *
 * The document's axes (a text's `fontVariationSettings`, which `RemoteText` writes into it) are
 * applied to a loaded family as a variable instance of it. Only the axes the face has are passed:
 * Remote Compose writes a text's features into the same list, and a static face has no axes at all,
 * so either would otherwise be a new instance that draws the plain face.
 *
 * A text with no family of its own (the player's `default` or `sans-serif`) that declares axes is
 * drawn in Wear's Roboto Flex at those axes, as the editor's canvas draws it: that is the face a
 * watch sets such a text in, and the player's own default is static, so its axes drew nothing.
 *
 * **Declared** is what [declaredAxes] says, not "the player passed axes". The player hands every
 * text a `wght` (rc-players' `withWeightAxis`, so a variable default face draws the style's
 * weight), so read that way every text in the document moved onto Roboto Flex — whose vendored
 * instance has no `frac` or `tnum`, so a features-only text lost its fraction. A text the document
 * gave no axes keeps the player's face, with its features, as it does on the canvas.
 *
 * @param declaredAxes each `fontVariationSettings` the document's texts declare. The player's
 *   variations for a text include all of one of these when that text declared it; it adds `wght`
 *   and `ital` to them, but never drops one.
 */
internal class RegistryTypefaceLoader(
  private val registry: UiBuilderFontRegistry?,
  private val declaredAxes: Collection<List<FontSettings.Axis>> = emptyList(),
  private val fallback: RcTypefaceLoader = RcTypefaceLoader.Default,
) : RcTypefaceLoader {
  override val families: Set<String>
    get() = fallback.families + registry?.loaded?.keys.orEmpty()

  override fun typeface(family: String, variations: RcFontVariations?): FontFamily? {
    val loaded =
      registry?.loaded?.get(family)
        ?: return deviceFace(family, variations) ?: fallback.typeface(family, variations)
    val axes = axes(family, variations)
    return if (axes.isEmpty()) loaded else registry.variant(family, axes) ?: loaded
  }

  /** Roboto Flex at [variations] for a text the player would set in its own default face. */
  private fun deviceFace(family: String, variations: RcFontVariations?): FontFamily? {
    if (family !in DEFAULT_FAMILIES || registry?.loaded?.get(DEVICE_FAMILY) == null) return null
    if (!declared(variations)) return null
    val axes = axes(DEVICE_FAMILY, variations)
    return if (axes.isEmpty()) null else registry.variant(DEVICE_FAMILY, axes)
  }

  /** Whether [variations] carry every axis of one of [declaredAxes]: the text asked for them. */
  private fun declared(variations: RcFontVariations?): Boolean {
    val passed =
      variations?.axes.orEmpty().mapTo(mutableSetOf()) { FontSettings.Axis(it.tag, it.value) }
    return declaredAxes.any { it.isNotEmpty() && passed.containsAll(it) }
  }

  private fun axes(family: String, variations: RcFontVariations?): List<FontSettings.Axis> =
    variations
      ?.axes
      .orEmpty()
      .filter { registry?.hasAxis(family, it.tag) == true }
      .map { FontSettings.Axis(it.tag, it.value) }

  internal companion object {
    /** The face a text with no family is drawn in on a watch, and on the editor's canvas. */
    const val DEVICE_FAMILY: String = FontSettings.WEAR_DEFAULT_FAMILY

    /** The names the player hands a loader for a text that names no family. */
    private val DEFAULT_FAMILIES = setOf("default", "sans-serif")
  }
}
