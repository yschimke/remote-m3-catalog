package ee.schimke.wearm3catalog.remote

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Guards that the widget-container stickers carry their **encoded RemoteCompose document**, not
 * just pixels.
 *
 * Every other sticker in this sheet captures through `RemoteOverridablePreview`, which offers the
 * document to `IrSidecarChannel` so the render lands a `<stem>.rc` and `BundlePreviewTask
 * .resolvePreviewIr` packs it as the sticker's IR. The widget-container stickers went through
 * upstream's `WearWidgetPreview` instead, which captures the document internally and keeps the
 * bytes to itself — so they rendered fine while silently riding the bundle as compiled `@Preview`
 * bytecode. `CapturingWearWidgetPreview` closed that gap; this test is what keeps it closed.
 *
 * It is a *sidecar* assertion rather than a pixel one on purpose: the failure mode being guarded
 * against is invisible in the PNG. The capture is best-effort inside the composable (an IR failure
 * must not break the raster), so without this test a regression — a coroutines version skew, an
 * upstream signature change — would show up as nothing more than a missing file and a green build.
 */
class WidgetContainerIrCaptureTest {

  private val rendersDir = File("build/compose-previews/renders")

  private val widgetStickers =
    listOf(
      "WidgetContainerSmallRemote_width_216dp_height_76dp_dpi_320",
      "WidgetContainerLargeRemote_width_216dp_height_124dp_dpi_320",
      "WidgetContainerGradientRemote_width_216dp_height_76dp_dpi_320",
      // The round (Samsung) hosts — same gate as the squircle ones: the capture wrapper is what
      // carries the encoded document, and nothing about the round footprint exempts it.
      "WidgetContainerRoundSmallRemote_width_230dp_height_76dp_dpi_320",
      "WidgetContainerRoundLargeRemote_width_230dp_height_168dp_dpi_320",
      // Every frame is a `device =` spec (it pins dpi=320), so the frame is part of each stem.
      "WidgetContainerTileRemote_width_240dp_height_240dp_dpi_320",
    )

  @Test
  fun `every widget container sticker renders`() {
    for (stem in widgetStickers) {
      assertThat(renderFile(rendersDir, stem).exists()).isTrue()
    }
  }

  @Test
  fun `every widget container sticker emits its encoded RemoteCompose document as a rc sidecar`() {
    for (stem in widgetStickers) {
      val rc = renderFile(rendersDir, stem, ext = "rc")
      assertThat(rc.exists()).isTrue()
      // A real encoded document, not an empty placeholder.
      assertThat(rc.length()).isGreaterThan(0L)
    }
  }

  /**
   * The document must carry the sticker's **content**, not just its container.
   *
   * A widget's fill rides the document's `WearWidgetBrush` while its text rides `TEXT_LAYOUT` ops,
   * and the two fail independently: a capture that lost the content subtree would still emit a
   * plausible, non-empty `.rc` that renders as a correctly-shaped but empty squircle. Size alone
   * can't tell those apart, so assert on the strings themselves — they are stored as UTF-8 in the
   * encoded document.
   */
  @Test
  fun `each widget container document carries its text content`() {
    val expected =
      mapOf(
        "WidgetContainerSmallRemote_width_216dp_height_76dp_dpi_320" to
          listOf("Next: Standup 10:30"),
        "WidgetContainerLargeRemote_width_216dp_height_124dp_dpi_320" to
          listOf("Morning run", "28 min"),
        "WidgetContainerGradientRemote_width_216dp_height_76dp_dpi_320" to listOf("Gradient"),
        "WidgetContainerRoundSmallRemote_width_230dp_height_76dp_dpi_320" to
          listOf("Next: Standup 10:30"),
        "WidgetContainerRoundLargeRemote_width_230dp_height_168dp_dpi_320" to
          listOf("Morning run", "28 min"),
        "WidgetContainerTileRemote_width_240dp_height_240dp_dpi_320" to
          listOf("Morning run", "28 min"),
      )
    for ((stem, strings) in expected) {
      val bytes = renderFile(rendersDir, stem, ext = "rc").readBytes().toString(Charsets.UTF_8)
      for (s in strings) {
        assertThat(bytes).contains(s)
      }
    }
  }
}
