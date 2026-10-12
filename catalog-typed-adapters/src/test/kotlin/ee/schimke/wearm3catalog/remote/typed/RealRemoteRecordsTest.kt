package ee.schimke.wearm3catalog.remote.typed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What [RealRemoteRecords] reads from the vendored Remote Compose port: the real composables and
 * the Remote value types their parameters take. Those types are the pilot's central finding — none
 * is a type a projection codec writes — so a library change that moves one fails here first.
 */
class RealRemoteRecordsTest {
  private val records = RealRemoteRecords.records()

  private fun type(component: String, parameter: String): String {
    val p = records.getValue(component).parameters.single { it.name == parameter }
    return p.typeFqn + if (p.nullable) "?" else ""
  }

  @Test
  fun `every real composable is discovered as a library record`() {
    assertEquals(RealRemoteRecords.callables.map { it.name }.toSet(), records.keys)
    assertTrue(records.values.all { it.signatureKnown })
  }

  @Test
  fun `every value RemoteText takes is a Remote type`() {
    val state = RealRemoteRecords.STATE
    assertEquals("$state.RemoteString", type("RemoteText", "text"))
    assertEquals("$state.RemoteColor?", type("RemoteText", "color"))
    assertEquals("$state.RemoteTextUnit?", type("RemoteText", "fontSize"))
    assertEquals(
      "androidx.compose.remote.creation.compose.text.RemoteTextStyle",
      type("RemoteText", "style"),
    )
  }

  @Test
  fun `every modifier is a RemoteModifier, not Compose's Modifier`() {
    for (name in records.keys) {
      assertEquals("${RealRemoteRecords.MODIFIER}.RemoteModifier", type(name, "modifier"), name)
    }
  }

  @Test
  fun `a button's click is a Remote Action, not a callback`() {
    assertEquals(
      "androidx.compose.remote.creation.compose.action.Action",
      type("RemoteButton", "onClick"),
    )
    // The content overload of three; the other two add a container painter.
    assertEquals(3, records.getValue("RemoteButton").overloads.size)
  }

  @Test
  fun `a box's content has no scope to align a child from`() {
    val content = records.getValue("RemoteBox").parameters.single { it.name == "content" }
    assertEquals(null, content.composableSlotReceiver)
  }

  @Test
  fun `a column's spacing takes a RemoteDp, not a Dp`() {
    val spacedBy =
      RealRemoteRecords.factories().filter {
        it.callableFqn == "${RealRemoteRecords.LAYOUT}.RemoteArrangement.spacedBy" &&
          it.parameters.size == 1
      }
    assertEquals(
      setOf("${RealRemoteRecords.STATE}.RemoteDp", "${RealRemoteRecords.STATE}.RemoteFloat"),
      spacedBy.map { it.parameters.single().typeFqn }.toSet(),
    )
  }

  @Test
  fun `the Remote value wrappers are companion invokes`() {
    val factories = RealRemoteRecords.factories().associateBy { it.callableFqn }
    // `Color(…).rc` and `"…".rs` are these calls; `RemoteString`'s is `@RestrictTo(LIBRARY_GROUP)`.
    val color = factories.getValue("${RealRemoteRecords.STATE}.RemoteColor.Companion.invoke")
    assertEquals("androidx.compose.ui.graphics.Color", color.parameters.single().typeFqn)
    val string = factories.getValue("${RealRemoteRecords.STATE}.RemoteString.Companion.invoke")
    assertEquals("kotlin.String", string.parameters.single().typeFqn)
  }
}
