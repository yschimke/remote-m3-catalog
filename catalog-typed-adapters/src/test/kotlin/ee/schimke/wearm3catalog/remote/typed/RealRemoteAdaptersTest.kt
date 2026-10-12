package ee.schimke.wearm3catalog.remote.typed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Each definition against the record it targets. The layouts validate; the two shelf components do
 * not, and the findings are pinned: each names a required parameter whose Remote value no
 * projection can write at compose-ai-tools 2.52.0. A tools release that closes the gap is adopted
 * by writing the parameter, which empties the finding and fails this test until it is updated.
 */
class RealRemoteAdaptersTest {
  private val real = RealRemotePublication()

  @Test
  fun `the layouts validate against RemoteBox and RemoteColumn`() {
    assertEquals(emptyList(), real.box.validateAgainst(real.box.component))
    assertEquals(emptyList(), real.column.validateAgainst(real.column.component))
  }

  @Test
  fun `RemoteText cannot be called without the RemoteString it is refused`() {
    assertEquals(
      listOf("remote-m3/remote-text -> RemoteText: required parameter text is not written"),
      real.text.validateAgainst(real.text.component),
    )
  }

  @Test
  fun `RemoteButton cannot be called without the Action it is refused`() {
    val findings = real.button.validateAgainst(real.button.component)
    assertEquals(1, findings.size, findings.toString())
    // Every one of the three overloads is reported, each for its `onClick`.
    assertEquals(
      3,
      Regex("required parameter onClick is not written").findAll(findings.single()).count(),
    )
  }

  @Test
  fun `every refused property is refused by name`() {
    val refused = { adapter: ee.schimke.composeai.discovery.TypedComponentAdapter<*> ->
      adapter.properties.map { it.name }.toSet() - requireNotNull(adapter.projection).properties
    }
    assertEquals(
      setOf("text", "color", "fontSize", "fontVariationSettings", "fontFeatureSettings"),
      refused(real.text),
    )
    assertEquals(setOf("containerImageKey"), refused(real.button))
    assertEquals(setOf("verticalSpacingDp", "alignment"), refused(real.column))
    assertTrue(refused(real.box).isEmpty())
  }
}
