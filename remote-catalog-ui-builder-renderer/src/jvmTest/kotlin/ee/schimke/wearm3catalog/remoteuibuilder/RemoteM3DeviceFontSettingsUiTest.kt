package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import ee.schimke.composeai.rcplayer.compose.RcFontVariations
import ee.schimke.composeai.rcplayer.compose.RcTypefaceLoader
import ee.schimke.composeai.uibuilder.ProvideUiBuilderFonts
import ee.schimke.composeai.uibuilder.UiBuilderFontRegistry
import ee.schimke.composeai.uibuilder.export.UiBuilderDocument
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

/**
 * A remote text's `fontFeatureSettings` and `fontVariationSettings` change what the device preview
 * draws, as they do on the editor's canvas.
 *
 * The player hands every `CoreText` to the loader with a `wght` axis (rc-players' `withWeightAxis`,
 * so a variable default face draws the style's weight), so "the variations carry an axis" is not
 * the same as "the document declared one". Read that way, every text in a document with any axes
 * moved onto Roboto Flex — whose vendored instance has no `frac` or `tnum` — and the features line
 * lost its fraction.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteM3DeviceFontSettingsUiTest {
  private val fonts = File(System.getProperty("uiBuilder.rcFonts"))

  private fun registry() =
    UiBuilderFontRegistry(
      CoroutineScope(Dispatchers.Unconfined),
      readManifest = { File(fonts, "fonts.json").readText() },
      readFont = { File(fonts, it).readBytes() },
    )

  /**
   * The face a text with no family falls to, standing in for the browser runtime's default: static
   * Roboto, which has `frac` and `tnum`. The JVM's own `FontFamily.SansSerif` is whatever the host
   * has installed (DejaVu Sans here), which has no `frac`, so it would show nothing either way.
   */
  private val platformDefault =
    object : RcTypefaceLoader {
      private val roboto =
        FontFamily(
          Font("test:Roboto", File(fonts, "Roboto-Regular.ttf").readBytes(), FontWeight.Normal)
        )

      override val families = setOf("default", "sans-serif")

      override fun typeface(family: String, variations: RcFontVariations?): FontFamily? =
        roboto.takeIf {
          family in families
        }
    }

  /** The rendered widget content, captured at density 2. */
  private fun render(document: UiBuilderDocument): ImageBitmap {
    var image: ImageBitmap? = null
    runDesktopComposeUiTest(width = 600, height = 300) {
      var readyCalls = 0
      val fontRegistry = registry()
      setContent {
        CompositionLocalProvider(LocalDensity provides Density(2f)) {
          ProvideUiBuilderFonts(fontRegistry) {
            RemoteM3DevicePreview(
              document,
              widthDp = 216f,
              heightDp = 76f,
              typefaceFallback = platformDefault,
            ) {
              readyCalls++
            }
          }
        }
      }
      waitUntil(timeoutMillis = 10_000) { readyCalls >= 1 }
      waitForIdle()
      onNodeWithTag(REMOTE_M3_DEVICE_PREVIEW_TEST_TAG)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ready"))
      image = onNodeWithTag(REMOTE_M3_WIDGET_CONTENT_TEST_TAG).captureToImage()
    }
    return requireNotNull(image)
  }

  @Test
  fun `a remote text keeps its features and its width axis`() {
    val image = render(document(PLAIN, FEATURES, AXES))
    save(image, "font-settings-three-lines.png")
    val lines = inkLines(image.toPixelMap())
    assertTrue(lines.size == 3, "three lines of ink, found ${lines.map { it.toList() }}")
    val (plain, features, axes) = lines.map { it[2] - it[0] + 1 }
    // The bold-only comparison: the same text at `wght 800` and nothing else.
    val bold = render(document(BOLD))
    save(bold, "font-settings-wght-800.png")
    val boldWidth = inkLines(bold.toPixelMap()).single().let { it[2] - it[0] + 1 }
    println(
      "font settings ink widths (px at 2x): plain=$plain features=$features axes=$axes " +
        "wght800=$boldWidth"
    )
    // A frac glyph is a reduced numerator and denominator around a fraction slash; the line
    // reads narrower than "1/2" set at full size.
    assertTrue(
      abs(features - plain) >= 4,
      "the features line draws its fraction: plain $plain px, features $features px",
    )
    // `wdth 75` reaches the face. The vendored Roboto Flex (3.200) narrows this string's advances
    // by only 3.2% there at `wght 800` (19406 to 18786 units, read off the font's own HVAR), so the
    // bound is that, less a pixel of rounding, not "clearly condensed".
    assertTrue(
      axes <= boldWidth * 0.985,
      "wdth 75 condenses the wght 800 line: wght 800 $boldWidth px, + wdth 75 $axes px",
    )
  }

  private fun save(image: ImageBitmap, name: String) {
    val directory =
      File(System.getProperty("java.io.tmpdir"), "rm3-font-settings").apply(File::mkdirs)
    ImageIO.write(image.toAwtImage(), "png", File(directory, name))
  }

  /**
   * Each horizontal band of ink, as `[left, top, right, bottom]`: pixels that differ from the
   * content's own background, read at its top-left corner.
   */
  private fun inkLines(pixels: PixelMap): List<IntArray> {
    val background = pixels[0, 0]
    fun ink(color: Color) =
      abs(color.red - background.red) +
        abs(color.green - background.green) +
        abs(color.blue - background.blue) > 0.3f
    val rows =
      (0 until pixels.height).map { y -> (0 until pixels.width).any { ink(pixels[it, y]) } }
    val bands = mutableListOf<IntRange>()
    var start = -1
    rows.forEachIndexed { y, has ->
      if (has && start < 0) start = y
      if (!has && start >= 0) {
        bands += start until y
        start = -1
      }
    }
    if (start >= 0) bands += start until pixels.height
    return bands.map { band ->
      var left = Int.MAX_VALUE
      var right = -1
      for (y in band) for (x in 0 until pixels.width) {
        if (ink(pixels[x, y])) {
          left = minOf(left, x)
          right = maxOf(right, x)
        }
      }
      intArrayOf(left, band.first, right, band.last)
    }
  }

  private fun document(vararg texts: String): UiBuilderDocument {
    val nodes = texts.mapIndexed { index, properties ->
      """
        "t$index": {
          "id": "t$index",
          "componentId": "remote-m3/remote-text",
          "properties": {
            "text": { "type": "string", "value": "Office 1/2 0123 1111" },
            "fontSize": { "type": "float", "value": 15 }$properties
          },
          "modifiers": [],
          "slots": {}
        }
        """
    }
    return Json.decodeFromString(
      """
      {
        "schema": "compose-ui-builder-document/v1-candidate",
        "id": "remote-m3-font-settings",
        "title": "Remote M3 font settings",
        "revision": 1,
        "catalogPin": {
          "systemId": "remote-m3",
          "catalogRevision": "test",
          "capabilityDigest": "test",
          "nativeRuntimeId": "remote-m3-test-runtime"
        },
        "environment": {
          "widthDp": 216, "heightDp": 76, "density": 2, "theme": "dark", "fontScale": 1,
          "layoutDirection": "ltr"
        },
        "stateVariables": {},
        "roots": ["root"],
        "nodes": {
          "root": {
            "id": "root",
            "componentId": "remote-m3/widget-container-small",
            "properties": {},
            "modifiers": [],
            "slots": { "content": ["column"] }
          },
          "column": {
            "id": "column",
            "componentId": "layout/column",
            "properties": {},
            "modifiers": [{ "type": "fillMaxSize" }],
            "slots": { "children": [${texts.indices.joinToString { "\"t$it\"" }}] }
          },
          ${nodes.joinToString(",")}
        }
      }
      """
    )
  }

  private companion object {
    const val PLAIN = ""
    const val FEATURES =
      """, "fontFeatureSettings": { "type": "string", "value": "tnum 1, frac 1, liga 0" }"""
    const val AXES =
      """, "fontVariationSettings": { "type": "string", "value": "wght 800, wdth 75" }"""
    const val BOLD = """, "fontVariationSettings": { "type": "string", "value": "wght 800" }"""
  }
}
