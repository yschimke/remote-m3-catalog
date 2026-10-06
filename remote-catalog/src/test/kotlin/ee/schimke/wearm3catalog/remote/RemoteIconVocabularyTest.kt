package ee.schimke.wearm3catalog.remote

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * `remote-m3/remote-icon` lets a design choose its glyph.
 *
 * `RemoteIcon`'s `imageVector` is an `ImageVector`, which the derived vocabulary drops, so a
 * published icon could only ever draw its default and a launcher widget's icon buttons could not be
 * expressed (https://github.com/yschimke/remote-m3-catalog/issues/12). The policy states the
 * vocabulary instead, and because a stated vocabulary replaces the derivation entirely, losing a
 * name from it silently drops that argument from the shelf; this holds all three to the names the
 * canvas mapping, the Browser Preview and the export read.
 */
class RemoteIconVocabularyTest {

  @Test
  fun `the icon states the glyph, the description and the tint`() {
    val policy = Json.parseToJsonElement(File("ui-builder.policy.json").readText()).jsonObject
    val icon = policy.getValue("components").jsonObject.getValue("remote-m3/remote-icon").jsonObject
    val names =
      icon.getValue("propertyCapabilities").jsonArray.map {
        it.jsonObject.getValue("name").jsonPrimitive.contentOrNull
      }

    assertThat(names).containsExactly("imageVector", "contentDescription", "tint")
    // The canvas reads the glyph through this mapping, so the stated name has to be its source.
    val mapped =
      icon
        .getValue("canvasMapping")
        .jsonObject
        .getValue("properties")
        .jsonObject
        .getValue("iconKey")
        .jsonPrimitive
        .contentOrNull
    assertThat(mapped).isEqualTo("imageVector")
  }
}
