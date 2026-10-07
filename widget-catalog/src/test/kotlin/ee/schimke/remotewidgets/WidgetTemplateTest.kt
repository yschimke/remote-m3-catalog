package ee.schimke.remotewidgets

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Every template the `remote-widgets` policy declares is a launcher widget this catalog can author.
 *
 * A template is the one document nobody authored, so nothing else catches a component the catalog
 * does not offer, a size off the launcher grid, or a theme role a launcher widget has no theme to
 * resolve. The Kotlin the `counter-widget` template becomes is `generated/CounterWidget.kt`, which
 * the unit-test compile builds against this module's classpath.
 */
@RunWith(Parameterized::class)
class WidgetTemplateTest(private val templatePath: String) {

  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    private fun moduleFile(path: String): File {
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

    private val policy: JsonObject by lazy {
      json.parseToJsonElement(moduleFile("ui-builder.policy.json").readText()).jsonObject
    }

    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun templates(): List<String> =
      policy.getValue("templates").jsonArray.map { it.jsonPrimitive.content }
  }

  private val document: JsonObject by lazy {
    json.parseToJsonElement(moduleFile(templatePath).readText()).jsonObject
  }

  private val nodes: Map<String, JsonObject> by lazy {
    document.getValue("nodes").jsonObject.mapValues { it.value.jsonObject }
  }

  @Test
  fun `it is pinned to this catalog`() {
    val pin = document.getValue("catalogPin").jsonObject
    assertThat(pin.getValue("systemId").jsonPrimitive.content).isEqualTo("remote-widgets")
  }

  @Test
  fun `its root is the launcher widget`() {
    val roots = document.getValue("roots").jsonArray.map { it.jsonPrimitive.content }
    assertThat(roots).hasSize(1)
    assertThat(nodes.getValue(roots.single()).getValue("componentId").jsonPrimitive.content)
      .isEqualTo("remote-widgets/launcher-widget")
  }

  @Test
  fun `it is sized on the launcher grid, and its title says which cell count`() {
    val environment = document.getValue("environment").jsonObject
    val width = environment.getValue("widthDp").jsonPrimitive.int
    val height = environment.getValue("heightDp").jsonPrimitive.int
    val size = WidgetSize.entries.firstOrNull { it.widthDp == width && it.heightDp == height }
    assertWithMessage("$width×$height dp").that(size).isNotNull()
    assertThat(document.getValue("title").jsonPrimitive.content).contains(size!!.label)
  }

  @Test
  fun `every component is one this catalog offers`() {
    val offered =
      policy.getValue("builtins").jsonObject.keys + policy.getValue("components").jsonObject.keys
    nodes.values.forEach { node ->
      val id = node.getValue("componentId").jsonPrimitive.content
      // `layout/*` is the builder's generic Remote Compose vocabulary, offered to every
      // `remote-compose` catalog without a declaration.
      if (!id.startsWith("layout/")) assertThat(offered).contains(id)
    }
  }

  @Test
  fun `every colour is a literal, because there is no theme to name a role in`() {
    nodes.values.forEach { node ->
      node.getValue("properties").jsonObject.forEach { (name, value) ->
        if (name == "color" || name == "background") {
          assertWithMessage("${node.getValue("id").jsonPrimitive.content}.$name")
            .that(value.jsonObject.getValue("value").jsonPrimitive.content)
            .matches("#[0-9A-Fa-f]{8}")
        }
      }
    }
  }
}
