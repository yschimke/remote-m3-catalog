package ee.schimke.wearm3catalog.remote.typed

import ee.schimke.catalog.typed.remoteM3Policy
import ee.schimke.composeai.discovery.ComponentRecordFile
import ee.schimke.composeai.discovery.UiBuilderCatalogFile
import ee.schimke.composeai.discovery.UiBuilderCatalogs
import ee.schimke.composeai.discovery.UiBuilderPolicyFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val artifactJson = Json {
  prettyPrint = true
  encodeDefaults = true
  ignoreUnknownKeys = true
}

/** The pilot pair, generated: what a host serves as [TYPED_REMOTE_CATALOG_ID]. */
class TypedRemotePair(
  val record: ComponentRecordFile,
  val policy: UiBuilderPolicyFile,
  val catalog: UiBuilderCatalogFile,
)

/**
 * The pilot pair over [discovered], production's discovery of `:remote-catalog`: its inventory and
 * signatures unchanged, the real export targets beside them, and the pilot policy. Generation
 * diagnostics are held to production's: the same discovery under the authored production policy
 * already reports some (a snapshot-lane component's policy with no record on the released lane, a
 * canvas no adapter claims), and the pilot may add only its own export targets and layouts kept off
 * the shelf, exactly as asked. Anything else refuses the pair.
 */
fun generateTypedPilot(
  discovered: ComponentRecordFile,
  real: RealRemotePublication = RealRemotePublication(),
): TypedRemotePair {
  val base = remoteM3Policy()
  val policy = TypedRemotePilot.policy(base, real)
  val record = TypedRemotePilot.record(discovered, real)
  val cover = UiBuilderCatalogs.CoverSheet(TYPED_REMOTE_CATALOG_ID, "Remote M3 — typed pilot")
  val catalog = requireNotNull(UiBuilderCatalogs.generate(record, cover, policy))
  val production =
    requireNotNull(
        UiBuilderCatalogs.generate(
          discovered,
          UiBuilderCatalogs.CoverSheet(TYPED_REMOTE_CATALOG_ID, "Remote M3 — production baseline"),
          base.newBuilder().apply { catalogId = TYPED_REMOTE_CATALOG_ID }.build(),
        )
      )
      .diagnostics
      .map { it.code to it.subject }
      .toSet()
  val offShelf = policy.components.filterValues { it.excluded != null }.keys
  val unexpected =
    catalog.diagnostics.filterNot {
      (it.code to it.subject) in production ||
        (it.code == UiBuilderCatalogs.Diagnostics.COMPONENT_EXCLUDED && it.subject in offShelf)
    }
  check(unexpected.isEmpty()) {
    "$TYPED_REMOTE_CATALOG_ID: " + unexpected.joinToString { "${it.code}: ${it.message}" }
  }
  return TypedRemotePair(record, policy, catalog)
}

/**
 * Write the pilot pair to `<output>/remote-m3-typed-pilot/`, read from `<root>`'s discovered
 * `:remote-catalog` (`./gradlew :remote-catalog:composePreviewDiscover` first). Nothing under
 * `<root>` is written.
 */
fun writeTypedPilot(root: Path, output: Path) {
  val discovery = root.resolve("remote-catalog/build/compose-previews/components.json")
  check(Files.isRegularFile(discovery)) {
    "Discover :remote-catalog:composePreviewDiscover before generating the pilot: $discovery"
  }
  val discovered =
    artifactJson.decodeFromString(ComponentRecordFile.serializer(), Files.readString(discovery))
  require(discovered.components.isNotEmpty()) { "Refusing an empty discovered inventory" }
  val pair = generateTypedPilot(discovered)
  val destination = output.resolve(TYPED_REMOTE_CATALOG_ID)
  Files.createDirectories(output)
  val staging = Files.createTempDirectory(output, ".pilot-")
  try {
    Files.writeString(
      staging.resolve("components.json"),
      artifactJson.encodeToString(ComponentRecordFile.serializer(), pair.record) + "\n",
    )
    Files.writeString(
      staging.resolve("ui-builder.json"),
      artifactJson.encodeToString(UiBuilderCatalogFile.serializer(), pair.catalog) + "\n",
    )
    Files.writeString(
      staging.resolve("ui-builder.policy.json"),
      artifactJson.encodeToString(UiBuilderPolicyFile.serializer(), pair.policy) + "\n",
    )
    for (template in pair.policy.templates) {
      val path = Path.of(template.path).normalize()
      require(!path.isAbsolute && !path.startsWith("..")) { "Template escapes the catalog: $path" }
      val source = root.resolve("remote-catalog").resolve(path)
      check(Files.isRegularFile(source)) { "Missing template: $path" }
      val target = staging.resolve(path)
      Files.createDirectories(target.parent)
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
    val readiness: JsonObject = buildJsonObject {
      put("catalogId", TYPED_REMOTE_CATALOG_ID)
      put("discoveredComponents", discovered.components.size)
      put("exportTargets", pair.record.components.size - discovered.components.size)
      put("readyForCutover", false)
      put(
        "remainingGate",
        "Every committed design is refused at compose-ai-tools 2.52.0 / compose-ui-builder " +
          "3.109.0; the tools and builder APIs it waits on are listed in " +
          "docs/design/TYPED_REMOTE_PILOT.md",
      )
    }
    Files.writeString(
      staging.resolve("readiness.json"),
      artifactJson.encodeToString(JsonObject.serializer(), readiness) + "\n",
    )
    if (Files.exists(destination)) destination.toFile().deleteRecursively()
    Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE)
    println(
      "$TYPED_REMOTE_CATALOG_ID: ${discovered.components.size} discovered records, " +
        "${pair.record.components.size - discovered.components.size} export targets, " +
        "${pair.policy.templates.size} templates"
    )
  } finally {
    staging.toFile().deleteRecursively()
  }
}

fun main(args: Array<String>) {
  require(args.size == 2) { "Expected repository root and task-owned output directory" }
  writeTypedPilot(Path.of(args[0]), Path.of(args[1]))
}
