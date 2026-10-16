package androidx.compose.remote.creation.common

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RemoteDocumentWriterTest {
  @Test
  fun writesDeclarationPreamble() {
    val document =
      RemoteDocumentWriter(width = 192, height = 192).run {
        root { column { text("Remote Compose on Desktop") } }
        encodeToByteArray()
      }

    assertContentEquals(ExpectedDocument, document)
  }

  @Test
  fun customLayoutContentHasItsOwnComponentId() {
    val writer = RemoteDocumentWriter(width = 192, height = 192)
    val headerSize = writer.encodeToByteArray().size
    writer.root {
      startCustom(RemoteModifierData(), configId = 42, properties = emptyList())
      endCustom()
    }
    val bytes = writer.encodeToByteArray()
    val body = bytes.copyOfRange(headerSize, bytes.size)

    // Root (opcode, id), then LAYOUT_CUSTOM (opcode, id, animation id, config id, property count),
    // then its LAYOUT_CONTENT (opcode, id).
    val custom = 1 + 4
    assertEquals(LayoutCustom, body[custom].toInt() and 0xff)
    val customId = body.intAt(custom + 1)
    val content = custom + 1 + 4 * 4
    assertEquals(LayoutContent, body[content].toInt() and 0xff)
    val contentId = body.intAt(content + 1)

    assertNotEquals(customId, contentId, "the custom layout and its content share component id $customId")
  }

  private fun ByteArray.intAt(index: Int): Int =
    (this[index].toInt() and 0xff shl 24) or
      (this[index + 1].toInt() and 0xff shl 16) or
      (this[index + 2].toInt() and 0xff shl 8) or
      (this[index + 3].toInt() and 0xff)

  private companion object {
    const val LayoutCustom = 93
    const val LayoutContent = 201

    val ExpectedDocument =
      """
      00 04 8c 00 01 00 00 00 01 00 00 00 00 00 00 00
      05 00 05 00 04 00 00 00 c0 00 06 00 04 00 00 00
      c0 0c 09 00 04 00 00 00 00 00 0e 00 04 00 00 00
      00 00 1b 00 04 00 00 00 02 66 00 00 00 2a 00 00
      00 19 52 65 6d 6f 74 65 20 43 6f 6d 70 6f 73 65
      20 6f 6e 20 44 65 73 6b 74 6f 70 c8 ff ff ff fe
      cc ff ff ff fd ff ff ff ff 00 00 00 01 00 00 00
      04 00 00 00 00 c9 ff ff ff fc ef 00 00 00 2a 00
      02 01 ff ff ff fb 05 41 40 00 00 c9 ff ff ff fa
      d6 d6 d6 d6 d6
      """.trimIndent().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()
  }
}
