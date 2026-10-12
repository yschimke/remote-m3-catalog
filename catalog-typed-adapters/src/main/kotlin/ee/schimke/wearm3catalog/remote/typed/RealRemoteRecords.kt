package ee.schimke.wearm3catalog.remote.typed

import ee.schimke.composeai.discovery.ComponentRecord
import ee.schimke.composeai.discovery.ComponentRecordFile
import ee.schimke.composeai.discovery.FactorySignature
import ee.schimke.composeai.discovery.TypedAdapterDiscovery
import ee.schimke.composeai.discovery.TypedAdapterDiscovery.LibraryCallable

/**
 * The real Remote Compose composables the pilot's definitions export to, read from the library
 * metadata on this module's runtime classpath: the vendored CMP port under `vendor/`, which
 * publishes the same `androidx.compose.remote` / `androidx.wear.compose.remote` API the Android
 * export calls. Nothing is loaded or run.
 */
object RealRemoteRecords {
  const val MODULE: String = "remote-m3"
  const val MATERIAL3: String = "androidx.wear.compose.remote.material3"
  const val LAYOUT: String = "androidx.compose.remote.creation.compose.layout"
  const val STATE: String = "androidx.compose.remote.creation.compose.state"
  const val MODIFIER: String = "androidx.compose.remote.creation.compose.modifier"

  /** The builder's shared layouts, by the Remote composable the hand emitter writes for each. */
  val LAYOUTS: Map<String, String> =
    mapOf("layout/box" to "RemoteBox", "layout/column" to "RemoteColumn")

  val callables: List<LibraryCallable> =
    listOf(
      // Wear Remote Material 3's text, as production's record names it: one overload.
      LibraryCallable("$MATERIAL3.RemoteTextKt", "RemoteText"),
      // The content overload: the other two add a `containerPainter` (required, or nullable
      // beside a `label` slot).
      LibraryCallable(
        "$MATERIAL3.RemoteButtonKt",
        "RemoteButton",
        declaring = setOf("content"),
        excluding = setOf("containerPainter"),
      ),
      // The content overloads: the bare `RemoteBox(modifier)` is a spacer.
      LibraryCallable("$LAYOUT.RemoteBoxKt", "RemoteBox", declaring = setOf("content")),
      LibraryCallable("$LAYOUT.RemoteColumnKt", "RemoteColumn"),
    )

  /** The factories a projection calls: the column's `spacedBy`, and the colour wrapper. */
  val factoryCallables: List<TypedAdapterDiscovery.Callable> =
    listOf(
      TypedAdapterDiscovery.Callable("$LAYOUT.RemoteArrangement", "spacedBy"),
      // Companion `invoke`s are discovered on the companion's JVM class.
      TypedAdapterDiscovery.Callable("$STATE.RemoteColor\$Companion", "invoke"),
      TypedAdapterDiscovery.Callable("$STATE.RemoteString\$Companion", "invoke"),
    )

  fun discover(): ComponentRecordFile =
    TypedAdapterDiscovery.discoverLibrary(MODULE, "release", callables)

  fun factories(): List<FactorySignature> =
    TypedAdapterDiscovery.discoverFactories(factoryCallables)

  /** The discovered records by simple name. */
  fun records(): Map<String, ComponentRecord> = discover().components.associateBy { it.symbol.name }
}
