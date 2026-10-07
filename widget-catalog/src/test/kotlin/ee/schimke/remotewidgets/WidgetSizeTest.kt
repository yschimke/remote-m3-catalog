package ee.schimke.remotewidgets

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * The launcher grid is stated four times — [WidgetSize], the `@Widget<n>x<m>` preview frames,
 * `ui-builder.policy.json`'s `frame.geometry.sizesDp` and `catalog.spec.json`'s `breakpoints` —
 * because annotations cannot compute their arguments and JSON cannot read Kotlin. This holds all
 * four to the one formula Android documents, so none of them can drift alone.
 */
class WidgetSizeTest {

  private val json = Json { ignoreUnknownKeys = true }

  private fun moduleFile(path: String): File {
    // Gradle runs a module's unit tests from the module directory; an IDE may run them from the
    // repository root.
    var directory: File? = File(".").absoluteFile
    while (directory != null) {
      val candidate =
        if (directory.name == "widget-catalog") File(directory, path)
        else File(directory, "widget-catalog/$path")
      if (candidate.exists()) return candidate
      directory = directory.parentFile
    }
    error("could not find widget-catalog/$path from ${File(".").absolutePath}")
  }

  @Test
  fun `every size is Android's documented portrait cell size`() {
    // The table in developer.android.com/develop/ui/views/appwidgets/layouts, row for row.
    assertThat(WidgetSize.S1x1.widthDp to WidgetSize.S1x1.heightDp).isEqualTo(57 to 102)
    assertThat(WidgetSize.S2x1.widthDp to WidgetSize.S2x1.heightDp).isEqualTo(130 to 102)
    assertThat(WidgetSize.S3x1.widthDp to WidgetSize.S3x1.heightDp).isEqualTo(203 to 102)
    assertThat(WidgetSize.S4x1.widthDp to WidgetSize.S4x1.heightDp).isEqualTo(276 to 102)
    assertThat(WidgetSize.S5x2.widthDp to WidgetSize.S5x2.heightDp).isEqualTo(349 to 220)
    assertThat(WidgetSize.fromLabel("3x2")).isEqualTo(WidgetSize.S3x2)
  }

  @Test
  fun `the preview frames spell the formula's dp`() {
    val source = moduleFile("src/main/kotlin/ee/schimke/remotewidgets/WidgetSizes.kt").readText()
    val frame =
      Regex(
        """name = "(\d+x\d+)",\s*showBackground = false,\s*""" +
          """device = "spec:width=(\d+)dp,height=(\d+)dp,dpi=(\d+)""""
      )
    val frames = frame.findAll(source).map { it.destructured }.toList()
    assertThat(frames).isNotEmpty()
    frames.forEach { (label, width, height, dpi) ->
      val size = WidgetSize.fromLabel(label)
      assertThat(size).named(label).isNotNull()
      assertThat(width.toInt()).named("$label width").isEqualTo(size!!.widthDp)
      assertThat(height.toInt()).named("$label height").isEqualTo(size.heightDp)
      assertThat(dpi.toInt()).named("$label dpi").isEqualTo(WidgetSize.DPI)
    }
    // Every size has its single-frame annotation.
    WidgetSize.entries.forEach { assertThat(source).contains("annotation class Widget${it.label}") }
  }

  @Test
  fun `the builder offers exactly the grid, by cell count`() {
    val policy = json.parseToJsonElement(moduleFile("ui-builder.policy.json").readText()).jsonObject
    val sizes =
      policy
        .getValue("frame")
        .jsonObject
        .getValue("geometry")
        .jsonObject
        .getValue("sizesDp")
        .jsonArray
        .map {
          val size = it.jsonObject
          Triple(
            size.getValue("label").jsonPrimitive.content,
            size.getValue("widthDp").jsonPrimitive.int,
            size.getValue("heightDp").jsonPrimitive.int,
          )
        }
    assertThat(sizes)
      .containsExactlyElementsIn(
        WidgetSize.entries.map { Triple(it.label, it.widthDp, it.heightDp) }
      )
      .inOrder()
  }

  @Test
  fun `the sheet's breakpoints are the grid`() {
    val spec = json.parseToJsonElement(moduleFile("catalog.spec.json").readText()).jsonObject
    val breakpoints =
      spec.getValue("breakpoints").jsonArray.map {
        val breakpoint = it.jsonObject
        breakpoint.getValue("size").jsonPrimitive.content to
          breakpoint.getValue("device").jsonPrimitive.content
      }
    assertThat(breakpoints)
      .containsExactlyElementsIn(
        WidgetSize.entries.map {
          it.label to "spec:width=${it.widthDp}dp,height=${it.heightDp}dp,dpi=${WidgetSize.DPI}"
        }
      )
      .inOrder()
  }
}
