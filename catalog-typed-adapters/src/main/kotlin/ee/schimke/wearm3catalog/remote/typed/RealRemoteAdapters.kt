package ee.schimke.wearm3catalog.remote.typed

import ee.schimke.composeai.discovery.AdapterValueCodec
import ee.schimke.composeai.discovery.AdapterValueCodecs
import ee.schimke.composeai.discovery.ComponentRecord
import ee.schimke.composeai.discovery.ExportProjection
import ee.schimke.composeai.discovery.TypedComponentAdapter
import ee.schimke.composeai.discovery.WireColor
import kotlinx.serialization.json.JsonPrimitive

// The pilot's definitions on the real Remote Compose composables (`RealRemoteRecords`), each with
// the `ExportProjection` compose-ui-builder's generic export lowers a design through. They state
// what the builder's hand-written `RemoteContentEmitter` writes for the same components, as far as
// the projection vocabulary of compose-ai-tools 2.52.0 can say it; every property it cannot say is
// `exportRefuses`d by name rather than written differently. docs/design/TYPED_REMOTE_PILOT.md
// lists each refusal and the tools or builder API it waits on.

private const val MATERIAL_THEME = "${RealRemoteRecords.MATERIAL3}.RemoteMaterialTheme"
private const val TEXT_STYLE = "androidx.compose.remote.creation.compose.text.RemoteTextStyle"

/**
 * A string the builder stores, empty for unset, exported through [export]. The catalog's existing
 * wire values stay exactly as they are; only the export is typed.
 */
private fun exported(
  export: ExportProjection.Codec,
  allowed: Set<String>? = null,
): AdapterValueCodec<String> =
  object : AdapterValueCodec<String> by AdapterValueCodecs.String {
    override val stateType: String? = null
    override val exportCodec = export
    override val allowedValues = allowed
  }

/** `RemoteTypography`'s eighteen roles: the Wear scale, with no Material 3 `headline…`. */
val REMOTE_TYPOGRAPHY: Set<String> =
  setOf(
    "displayLarge",
    "displayMedium",
    "displaySmall",
    "titleLarge",
    "titleMedium",
    "titleSmall",
    "labelLarge",
    "labelMedium",
    "labelSmall",
    "bodyLarge",
    "bodyMedium",
    "bodySmall",
    "bodyExtraSmall",
    "numeralExtraLarge",
    "numeralLarge",
    "numeralMedium",
    "numeralSmall",
    "numeralExtraSmall",
  )

/** `style = RemoteMaterialTheme.typography.<role>`: a `RemoteTextStyle`, as the record takes. */
private val STYLE =
  exported(
    ExportProjection.Codec.Token(
      MATERIAL_THEME,
      listOf("typography"),
      TEXT_STYLE,
      REMOTE_TYPOGRAPHY,
    ),
    REMOTE_TYPOGRAPHY,
  )

private val TEXT_ALIGN =
  "androidx.compose.ui.text.style.TextAlign"
    .let { type ->
      exported(
        ExportProjection.Codec.Enum(
          type,
          listOf("start", "center", "end", "justify").associateWith { wire ->
            "$type.${wire.replaceFirstChar(Char::uppercaseChar)}"
          },
        ),
        setOf("start", "center", "end", "justify"),
      )
    }

/**
 * A theme role through `RemoteMaterialTheme.colorScheme`, which is already a `RemoteColor`. A
 * literal is written `Color(0x…)` by this codec, which is NOT a `RemoteColor` (the hand emitter
 * writes `Color(0x…).rc`); see the design doc's "Remote value types".
 */
val REMOTE_COLOR: ExportProjection.Codec.Color = ExportProjection.Codec.Color(MATERIAL_THEME)

data class RealRemoteTextProperties(
  val text: String,
  val style: String,
  val fontSize: Float,
  val color: WireColor,
  val textAlign: String,
  val maxLines: Int,
  val fontVariationSettings: String,
  val fontFeatureSettings: String,
)

/**
 * `remote-m3/remote-text` as Wear Remote Material 3's `RemoteText(text: RemoteString, …)`.
 *
 * `style`, `textAlign` and `maxLines` are written exactly as the hand emitter writes them. The
 * three Remote values are refused: `text` is a `RemoteString` (`"…".rs`), `color` a `RemoteColor`
 * (`RemoteMaterialTheme.colorScheme.<role>` or `Color(0x…).rc`) and `fontSize` a `RemoteTextUnit`
 * (`20.rsp`), and no codec writes any of the three. `text` is the record's required parameter, so
 * the definition does not validate and no `RemoteText` exports until a codec can write it.
 */
class RealRemoteTextAdapter(record: ComponentRecord) :
  TypedComponentAdapter<RealRemoteTextProperties>("remote-m3/remote-text", record) {
  val text = wireProperty(RealRemoteTextProperties::text, AdapterValueCodecs.String, "")
  val style = wireProperty(RealRemoteTextProperties::style, STYLE, "")
  val fontSize =
    wireProperty(
      RealRemoteTextProperties::fontSize,
      AdapterValueCodecs.unit("sp", positive = true),
      0f,
    )
  val color =
    wireProperty(
      RealRemoteTextProperties::color,
      AdapterValueCodecs.color(REMOTE_COLOR),
      WireColor.Unset,
    )
  val textAlign = wireProperty(RealRemoteTextProperties::textAlign, TEXT_ALIGN, "")
  val maxLines = wireProperty(RealRemoteTextProperties::maxLines, AdapterValueCodecs.Int, 0)
  val fontVariationSettings =
    wireProperty(RealRemoteTextProperties::fontVariationSettings, AdapterValueCodecs.String, "")
  val fontFeatureSettings =
    wireProperty(RealRemoteTextProperties::fontFeatureSettings, AdapterValueCodecs.String, "")

  init {
    exportAs(
      target(
        record,
        mapOf(
          "style" to from(style),
          "textAlign" to from(textAlign),
          "maxLines" to from(maxLines, omitWhen = JsonPrimitive(0)),
        ),
      )
    )
    exportRefuses(text, color, fontSize, fontVariationSettings, fontFeatureSettings)
  }
}

data class RealRemoteButtonProperties(val containerImageKey: String)

/**
 * `remote-m3/remote-button` as `RemoteButton(onClick: Action, …) { content }`, the content
 * overload. Its required `onClick` is a Remote `Action`, which the hand emitter writes as
 * `lambdaAction {}` when the design binds nothing; a projection has no fixed value the generator
 * accepts for it (an empty `ActionLambda` is refused as "binds a handler with no actions"), so the
 * definition does not validate. `containerImageKey` is the painter overload, which needs a
 * caller-supplied bitmap.
 */
class RealRemoteButtonAdapter(record: ComponentRecord) :
  TypedComponentAdapter<RealRemoteButtonProperties>("remote-m3/remote-button", record) {
  val containerImageKey =
    wireProperty(RealRemoteButtonProperties::containerImageKey, AdapterValueCodecs.String, "")
  val content = slot("content", required = true)

  init {
    exportAs(target(record, emptyMap()))
    exportRefuses(containerImageKey)
  }
}

data class RealRemoteBoxProperties(val contentAlignment: String)

/**
 * `layout/box` as `RemoteBox(modifier, contentAlignment) { content }`. The box's own
 * `contentAlignment` is written onto `RemoteAlignment`'s nine members, as the hand emitter writes
 * it. What the hand emitter ALSO does is hoist a child's `align` modifier into that argument, since
 * `RemoteBox`'s content has no scope to align from; a projection cannot read a child, so a design
 * that aligns a child is refused by the builder (see the design doc's "Parent-decided alignment").
 */
class RealRemoteBoxAdapter(record: ComponentRecord) :
  TypedComponentAdapter<RealRemoteBoxProperties>("layout/box", record) {
  val contentAlignment =
    wireProperty(
      RealRemoteBoxProperties::contentAlignment,
      "${RealRemoteRecords.LAYOUT}.RemoteAlignment"
        .let { type ->
          exported(
            ExportProjection.Codec.Enum(
              type,
              BOX_ALIGNMENTS.associateWith { wire ->
                "$type.${wire.replaceFirstChar(Char::uppercaseChar)}"
              },
            ),
            BOX_ALIGNMENTS,
          )
        },
      "",
    )
  val children = slot("children")

  init {
    exportAs(
      target(
        record,
        mapOf("contentAlignment" to from(contentAlignment)),
        slots = mapOf("children" to "content"),
      )
    )
  }

  private companion object {
    /** The builder's nine box alignments, each `RemoteAlignment`'s member of the same name. */
    val BOX_ALIGNMENTS: Set<String> =
      setOf(
        "topStart",
        "topCenter",
        "topEnd",
        "centerStart",
        "center",
        "centerEnd",
        "bottomStart",
        "bottomCenter",
        "bottomEnd",
      )
  }
}

data class RealRemoteColumnProperties(val verticalSpacingDp: Float, val alignment: String)

/**
 * `layout/column` as `RemoteColumn(modifier) { content }`. Neither property the committed designs
 * set can be written: `verticalSpacingDp` is `RemoteArrangement.spacedBy(space: RemoteDp)`, which a
 * `dp` unit codec writes as a `Dp`; `alignment` is how the column sits in its PARENT, which the
 * hand emitter hoists into the parent `RemoteBox`'s `contentAlignment`.
 */
class RealRemoteColumnAdapter(record: ComponentRecord) :
  TypedComponentAdapter<RealRemoteColumnProperties>("layout/column", record) {
  val verticalSpacingDp =
    wireProperty(RealRemoteColumnProperties::verticalSpacingDp, AdapterValueCodecs.unit("dp"), 0f)
  val alignment = wireProperty(RealRemoteColumnProperties::alignment, AdapterValueCodecs.String, "")
  val children = slot("children")

  init {
    exportAs(target(record, emptyMap(), slots = mapOf("children" to "content")))
    exportRefuses(verticalSpacingDp, alignment)
  }
}
