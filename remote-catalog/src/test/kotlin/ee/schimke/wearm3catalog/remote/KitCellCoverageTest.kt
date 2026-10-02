package ee.schimke.wearm3catalog.remote

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Holds [kit-cells.json] — how much of each kit set this sheet draws — against the kit walk it
 * counts and the reasons `kit-sets.json` states.
 *
 * Moved here with the sheet when it split out of yschimke/wear-m3-catalog, where the same test
 * holds the Wear sheet's own record. A set counting as reproduced says nothing about how much of it
 * is drawn, which is how this sheet came to draw 15 of the `Card` set's 45 cells with the whole
 * suite green ([wear-m3-catalog#158](https://github.com/yschimke/wear-m3-catalog/issues/158)).
 *
 * The numbers themselves are not asserted here and could not be: they are an OUTPUT, regenerated
 * from the resolved design map by `scripts/kit-cells.sh` and reconciled by CI, so a cell that stops
 * being drawn moves a number in a reviewable diff. What this test adds is everything that cannot be
 * regenerated:
 * - the record and the kit walk agree on what the kit publishes,
 * - the record's own arithmetic holds, and the sheet claims no node the set no longer publishes,
 * - a reason stated for a gap cannot outlive it, and
 * - a gap cannot go unexplained.
 *
 * WHY THE REASONS LIVE IN `kit-sets.json` and the counts here: one is prose a person writes, the
 * other is a number a script derives, and putting a hand-written sentence in a generated file makes
 * it a merge conflict every time a cell is added.
 */
class KitCellCoverageTest {

  /** The sheet key `scripts/kit-cells.sh` records this module under. */
  private val sheet = "remote-catalog"

  private val root = repositoryRoot()

  private fun json(name: String): JsonObject =
    Json.parseToJsonElement(File(root, name).readText()).jsonObject

  private val rows = json("kit-cells.json").getValue("sets").jsonArray.map { it.jsonObject }

  private val index = json("figma-kit-index.json").getValue("sets").jsonObject

  private val setRows = json("kit-sets.json").getValue("sets").jsonArray.map { it.jsonObject }

  private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

  private fun JsonObject.optString(key: String): String? = get(key)?.jsonPrimitive?.content

  private fun countsOf(row: JsonObject): JsonObject? =
    row.getValue("sheets").jsonObject[sheet]?.jsonObject

  private fun JsonObject.uncovered(): Int = getValue("uncovered").jsonArray.size

  /**
   * A record that generated nothing would pass every assertion below by having nothing to check.
   */
  @Test
  fun `the record covers this sheet and only this sheet`() {
    assertTrue(
      "kit-cells.json names no sets — regenerate with scripts/kit-cells.sh",
      rows.isNotEmpty(),
    )
    val sheets = rows.flatMap { it.getValue("sheets").jsonObject.keys }.toSet()
    assertEquals(setOf(sheet), sheets)
  }

  /**
   * The denominator is the committed kit walk, so the two files move together or the record is
   * counting against a kit that has changed underneath it.
   */
  @Test
  fun `every row counts the cells the kit walk saw`() {
    for (row in rows) {
      val node = row.string("node")
      val set = index[node]?.jsonObject
      assertTrue(
        "${row.string("set")} is a row of kit-cells.json but figma-kit-index.json publishes no " +
          "set $node — regenerate both, in that order",
        set != null,
      )
      assertEquals(
        "${row.string("set")} counts against ${row.string("published")} cells, the kit walk " +
          "saw a different number — regenerate with scripts/kit-cells.sh",
        set!!.getValue("variants").jsonArray.size,
        row.getValue("published").jsonPrimitive.int,
      )
    }
  }

  /**
   * Every published cell is either drawn or named as uncovered, and `stray` — the sheet claiming a
   * node this set does not publish — means a reference has rotted and is being compared to nothing.
   */
  @Test
  fun `drawn and uncovered account for every published cell`() {
    for (row in rows) {
      val counts = countsOf(row) ?: continue
      val where = "${row.string("set")} / $sheet"
      assertEquals(
        "$where draws ${counts.string("drawn")} and names ${counts.uncovered()} uncovered, which " +
          "is not the ${row.string("published")} cells the set publishes",
        row.getValue("published").jsonPrimitive.int,
        counts.getValue("drawn").jsonPrimitive.int + counts.uncovered(),
      )
      assertTrue(
        "$where names ${counts["stray"]} as cells of this set, which the kit does not publish " +
          "under it — the reference has rotted and is being compared to nothing",
        "stray" !in counts,
      )
    }
  }

  /** A stated reason cannot outlive the gap that earned it. */
  @Test
  fun `no stated reason outlives its gap`() {
    val byNode = rows.associateBy { it.string("node") }
    for (setRow in setRows) {
      val reason = setRow["cells"]?.jsonObject?.optString(sheet) ?: continue
      val set = setRow.string("set")
      val counts = byNode[setRow.string("node")]?.let(::countsOf)
      assertTrue(
        "$set states why $sheet falls short of it, and $sheet reproduces none of it — that is " +
          "an absence at the level of the set, not of a cell",
        counts != null,
      )
      assertTrue(
        "$set / $sheet states why it falls short and now draws every cell the kit publishes — " +
          "delete the reason",
        counts!!.uncovered() > 0,
      )
      assertTrue(
        "$set / $sheet has an empty reason, which says nothing a missing key would not",
        reason.isNotBlank(),
      )
    }
  }

  /**
   * A gap has to say why — the direction that makes silence fail
   * ([wear-m3-catalog#160](https://github.com/yschimke/wear-m3-catalog/issues/160)).
   */
  @Test
  fun `every gap says why`() {
    val reasons = setRows.associate { it.string("node") to it["cells"]?.jsonObject }
    for (row in rows) {
      val counts = countsOf(row) ?: continue
      if (counts.uncovered() == 0) continue
      assertTrue(
        "${row.string("set")} / $sheet draws ${counts.string("drawn")} of " +
          "${row.string("published")} cells and says nothing about the rest — add a `cells." +
          "$sheet` reason to its kit-sets.json row, or draw them",
        !reasons[row.string("node")]?.optString(sheet).isNullOrBlank(),
      )
    }
  }

  private fun repositoryRoot(): File {
    var directory: File? = File(".").absoluteFile
    while (directory != null) {
      if (File(directory, "kit-cells.json").isFile) return directory
      directory = directory.parentFile
    }
    error("could not find kit-cells.json from ${File(".").absolutePath}")
  }
}
