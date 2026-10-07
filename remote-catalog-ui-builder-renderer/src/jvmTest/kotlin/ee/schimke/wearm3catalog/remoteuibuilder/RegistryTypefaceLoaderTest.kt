package ee.schimke.wearm3catalog.remoteuibuilder

import ee.schimke.composeai.rcplayer.compose.RcFontAxis
import ee.schimke.composeai.rcplayer.compose.RcFontVariations
import ee.schimke.composeai.uibuilder.UiBuilderFontRegistry
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

  private companion object {
    const val ROBOTO_FLEX = "Roboto Flex"
    const val INTER = "Inter"
  }
}
