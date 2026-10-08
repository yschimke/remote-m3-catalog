package ee.schimke.wearm3catalog.remoteuibuilder

import ee.schimke.composeai.rcplayer.compose.RcFontAxis
import ee.schimke.composeai.rcplayer.compose.RcFontVariations
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.uibuilder.UiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.FontSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * The document's axes reach the face the player draws a loaded family in: `RemoteText` writes a
 * text's `fontVariationSettings` into the document and the player hands them to the loader.
 */
class RegistryTypefaceLoaderTest {
  private val fonts = File(System.getProperty("uiBuilder.rcFonts"))

  // Unconfined, and reads that never suspend: every family has loaded by the time `request`
  // returns, so the assertions read a settled registry.
  private val registry =
    UiBuilderFontRegistry(
        CoroutineScope(Dispatchers.Unconfined),
        readManifest = { File(fonts, "fonts.json").readText() },
        readFont = { File(fonts, it).readBytes() },
      )
      .apply {
        request(ROBOTO_FLEX)
        request(INTER)
      }

  private val loader = RegistryTypefaceLoader(registry)

  private fun axes(vararg settings: Pair<String, Float>) =
    RcFontVariations(settings.map { (tag, value) -> RcFontAxis(tag, value) })

  @Test
  fun `a variable face is drawn at the document's axes`() {
    val plain = assertNotNull(loader.typeface(ROBOTO_FLEX, null))
    assertSame(registry.loaded[ROBOTO_FLEX], plain)
    val narrow = assertNotNull(loader.typeface(ROBOTO_FLEX, axes("wdth" to 25f)))
    assertNotSame(plain, narrow)
    // One instance per setting, so a recomposition does not lay the text out again.
    assertSame(narrow, loader.typeface(ROBOTO_FLEX, axes("wdth" to 25f)))
  }

  @Test
  fun `features in the same list and a static face leave the plain family`() {
    // Remote Compose carries a text's features beside its axes; they are not axes of any face.
    assertSame(registry.loaded[ROBOTO_FLEX], loader.typeface(ROBOTO_FLEX, axes("tnum" to 1f)))
    // Inter is static: it has no `wdth` to set.
    assertSame(registry.loaded[INTER], loader.typeface(INTER, axes("wdth" to 25f)))
  }

  @Test
  fun `axes on a text with no family draw in Roboto Flex, as the canvas does`() {
    val fallback = RcTypefaceLoader.Default
    val loader =
      RegistryTypefaceLoader(
        registry,
        declaredAxes =
          listOf(listOf(FontSettings.Axis("wght", 800f), FontSettings.Axis("wdth", 75f))),
      )
    // The player's own default for an unstyled run, and the loader's when there are no axes.
    assertSame(fallback.typeface("default", null), loader.typeface("default", null))
    // The declared axes, and the `ital` the player adds to an italic text's.
    val condensed =
      assertNotNull(loader.typeface("sans-serif", axes("wght" to 800f, "wdth" to 75f)))
    assertSame(
      registry.variant(
        ROBOTO_FLEX,
        listOf(FontSettings.Axis("wght", 800f), FontSettings.Axis("wdth", 75f)),
      ),
      condensed,
    )
    assertNotNull(loader.typeface("default", axes("wght" to 800f, "wdth" to 75f, "ital" to 1f)))
    // Features alone are no axes of Roboto Flex: the plain default stays.
    assertSame(fallback.typeface("default", null), loader.typeface("default", axes("tnum" to 1f)))
  }

  @Test
  fun `the weight the player adds to every text is not a declared axis`() {
    val fallback = RcTypefaceLoader.Default
    val loader =
      RegistryTypefaceLoader(
        registry,
        declaredAxes = listOf(listOf(FontSettings.Axis("wdth", 75f))),
      )
    // rc-players hands every text its style weight as `wght`. A text that declared nothing, in a
    // document where another text did, keeps the player's face and so its features: Roboto Flex
    // has no `frac` or `tnum`.
    assertSame(
      fallback.typeface("sans-serif", null),
      loader.typeface("sans-serif", axes("wght" to 450f)),
    )
    // The declared text, with the weight added beside its axis.
    assertSame(
      registry.variant(
        ROBOTO_FLEX,
        listOf(FontSettings.Axis("wdth", 75f), FontSettings.Axis("wght", 450f)),
      ),
      loader.typeface("sans-serif", axes("wdth" to 75f, "wght" to 450f)),
    )
    // No declared axes at all: nothing moves onto Roboto Flex.
    assertSame(
      fallback.typeface("default", null),
      RegistryTypefaceLoader(registry).typeface("default", axes("wght" to 800f)),
    )
  }

  @Test
  fun `a generic serif or monospace text keeps the player's face`() {
    val fallback = RcTypefaceLoader.Default
    assertSame(
      fallback.typeface("serif", axes("wght" to 800f)),
      loader.typeface("serif", axes("wght" to 800f)),
    )
  }

  private companion object {
    const val ROBOTO_FLEX = "Roboto Flex"
    const val INTER = "Inter"
  }
}
