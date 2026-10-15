package ee.schimke.catalog.typed

import ee.schimke.composeai.discovery.UiBuilderAuthoredComponent
import ee.schimke.composeai.discovery.UiBuilderBuiltin
import ee.schimke.composeai.discovery.UiBuilderBuiltinSvg
import ee.schimke.composeai.discovery.UiBuilderBuiltinWasm
import ee.schimke.composeai.discovery.UiBuilderComposeSourceExport
import ee.schimke.composeai.discovery.UiBuilderMenu
import ee.schimke.composeai.discovery.UiBuilderPolicyFile
import ee.schimke.composeai.discovery.UiBuilderTemplateEntry
import ee.schimke.composeai.uibuilder.protocol.JsonValueTypeV1
import ee.schimke.composeai.uibuilder.protocol.PropertyCapabilityV1
import ee.schimke.composeai.uibuilder.protocol.SlotCapabilityV1
import ee.schimke.composeai.uibuilder.protocol.SlotCardinalityV1
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Parallel authored policy. Inventory membership remains annotation-owned.

fun remoteM3Policy(): UiBuilderPolicyFile =
  UiBuilderPolicyFile.Builder("compose-ui-builder-policy/v1", "remote-compose")
    .apply {
      jsonSchema =
        "https://raw.githubusercontent.com/yschimke/compose-ai-tools/main/scripts/design-artifacts/ui-builder.policy.schema.json"
      platformLabel = "RemoteCompose"
      previewSurfaces = policyPreviewSurfaces()
      frame = policyFrame()
      composeSourceExport =
        UiBuilderComposeSourceExport.Builder("remote-compose", 1).apply {}.build()

      colorTokens = policyColorTokens()
      menu =
        UiBuilderMenu.Builder()
          .apply {
            groupOrder =
              listOf(
                "Widget Container",
                "Scaffold templates",
                "Containment",
                "Buttons",
                "Selection buttons",
                "Edge-hugging buttons",
                "Sliders",
                "Steppers",
                "Text",
                "Iconography",
                "Content",
                "Communication",
                "Position indicators",
                "Shapes",
                "Shaders",
                "Typeface",
                "Theme",
                "Wear M3",
                "Confetti",
              )
          }
          .build()
      components =
        linkedMapOf(
          "remote-m3/remote-app-card" to componentRemoteAppCard(),
          "remote-m3/remote-button-group" to componentRemoteButtonGroup(),
          "remote-m3/remote-text" to componentRemoteText(),
          "remote-m3/remote-icon" to componentRemoteIcon(),
          "remote-m3/remote-horizontal-page-indicator" to componentRemoteHorizontalPageIndicator(),
          "remote-m3/remote-vertical-page-indicator" to componentRemoteVerticalPageIndicator(),
          "remote-m3/remote-circular-progress-indicator" to
            componentRemoteCircularProgressIndicator(),
          "remote-m3/remote-linear-progress-indicator" to componentRemoteLinearProgressIndicator(),
          "remote-m3/remote-curved-progress-indicator" to componentRemoteCurvedProgressIndicator(),
          "remote-m3/remote-button" to componentRemoteButton(),
          "remote-m3/remote-compact-button" to componentRemoteCompactButton(),
          "remote-m3/remote-card" to componentRemoteCard(),
          "remote-m3/remote-outlined-card" to componentRemoteOutlinedCard(),
          "remote-m3/remote-checkbox-button" to componentRemoteCheckboxButton(),
          "remote-m3/remote-switch-button" to componentRemoteSwitchButton(),
          "remote-m3/remote-radio-button" to componentRemoteRadioButton(),
          "remote-m3/remote-split-checkbox-button" to componentRemoteSplitCheckboxButton(),
          "remote-m3/remote-split-switch-button" to componentRemoteSplitSwitchButton(),
          "remote-m3/remote-split-radio-button" to componentRemoteSplitRadioButton(),
          "remote-m3/remote-slider" to componentRemoteSlider(),
          "remote-m3/remote-stepper" to componentRemoteStepper(),
          "remote-m3/remote-edge-button" to componentRemoteEdgeButton(),
          "remote-m3/remote-icon-button" to componentRemoteIconButton(),
          "remote-m3/remote-text-button" to componentRemoteTextButton(),
          "remote-m3/remote-title-card" to componentRemoteTitleCard(),
          "remote-m3/theme-specimen" to componentThemeSpecimen(),
          "remote-m3/remote-sticker" to componentRemoteSticker(),
          "remote-m3/capturing-wear-widget-preview" to componentCapturingWearWidgetPreview(),
        )
      builtins =
        linkedMapOf(
          "remote-m3/remote-label-button" to builtinRemoteLabelButton(),
          "remote-m3/remote-time-text" to builtinRemoteTimeText(),
          "remote-m3/remote-material-theme" to builtinRemoteMaterialTheme(),
          "remote-m3/lottie" to builtinLottie(),
          "remote-m3/widget-container-small" to builtinWidgetContainerSmall(),
          "remote-m3/widget-container-large" to builtinWidgetContainerLarge(),
          "remote-m3/widget-container-adaptive" to builtinWidgetContainerAdaptive(),
        )
      templates =
        listOf(
          UiBuilderTemplateEntry.Builder("ui-builder/designs/wear-widget-small.json").build(),
          UiBuilderTemplateEntry.Builder("ui-builder/designs/wear-widget-large.json").build(),
          UiBuilderTemplateEntry.Builder("ui-builder/designs/wear-widget-adaptive.json").build(),
          UiBuilderTemplateEntry.Builder("ui-builder/designs/hello-widget.json").build(),
          UiBuilderTemplateEntry.Builder("ui-builder/designs/weather-widget.json").build(),
        )
      supersedes = policySupersedes()
    }
    .build()

private fun policyPreviewSurfaces(): JsonElement = buildJsonObject {
  put(
    "wasm",
    buildJsonObject {
      put("fidelity", JsonPrimitive("approximate"))
      put(
        "reason",
        JsonPrimitive(
          "The selectable Visual Editor draws Wear Compose CMP stand-ins. The read-only Browser Preview is a separate DEVICE surface: CMP Remote M3 records a real .rc document in Wasm and the pinned CMP RcComposePlayer plays it in each selected host frame. Approximate describes authoring only, never the Browser Preview."
        ),
      )
    },
  )
  put(
    "native",
    buildJsonObject {
      put("fidelity", JsonPrimitive("authoritative"))
      put("backend", JsonPrimitive("android"))
      put(
        "reason",
        JsonPrimitive(
          "The generated creation-DSL Kotlin compiled against this module's own alpha Remote Compose trio and rasterised by the real player — the same lane that produces every sticker in this catalog."
        ),
      )
    },
  )
}

private fun policyFrame(): JsonElement = buildJsonObject {
  put("adapter", JsonPrimitive("frame/widget-host"))
  put(
    "geometry",
    buildJsonObject {
      put(
        "sizesDp",
        JsonArray(
          listOf(
            buildJsonObject {
              put("widthDp", JsonPrimitive(216))
              put("heightDp", JsonPrimitive(76))
              put("label", JsonPrimitive("Small container"))
            },
            buildJsonObject {
              put("widthDp", JsonPrimitive(216))
              put("heightDp", JsonPrimitive(124))
              put("label", JsonPrimitive("Large container"))
            },
          )
        ),
      )
    },
  )
}

private fun policyColorTokens(): JsonElement = buildJsonObject {
  put(
    "roles",
    JsonArray(
      listOf(
        JsonPrimitive("background"),
        JsonPrimitive("surface"),
        JsonPrimitive("surfaceContainer"),
        JsonPrimitive("surfaceContainerLow"),
        JsonPrimitive("surfaceContainerHigh"),
        JsonPrimitive("surfaceContainerHighest"),
        JsonPrimitive("primary"),
        JsonPrimitive("onPrimary"),
        JsonPrimitive("tertiary"),
        JsonPrimitive("onTertiary"),
        JsonPrimitive("onSurface"),
        JsonPrimitive("onSurfaceVariant"),
        JsonPrimitive("outlineVariant"),
        JsonPrimitive("transparent"),
      )
    ),
  )
}

private fun componentRemoteAppCard(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          slots =
            mapOf(
              "appName" to listOf(seedText("App")),
              "title" to listOf(seedText("Title")),
              "content" to listOf(seedText("Card")),
            )
        )
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteAppCardKt.RemoteAppCard"
      group = "Containment"
      canvas = "wear-m3/card"
      canvasMapping = buildJsonObject {
        put(
          "slots",
          buildJsonObject { put("content", JsonPrimitive("title")) },
        )
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("app"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteButtonGroup(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(slots = mapOf("content" to listOf(seedButton("Button"), seedButton("Button"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteButtonGroupKt.RemoteButtonGroup"
      group = "Buttons"
      canvas = "wear-m3/button-group"
      canvasMapping = buildJsonObject {
        put(
          "slots",
          buildJsonObject { put("children", JsonPrimitive("content")) },
        )
      }
    }
    .build()

private fun componentRemoteText(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("text" to seedValue("string", JsonPrimitive("Text"))))
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteTextKt.RemoteText"
      group = "Text"
      canvas = "wear-m3/text"
      canvasMapping = buildJsonObject {
        put(
          "properties",
          buildJsonObject { put("fontSizeSp", JsonPrimitive("fontSize")) },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "text",
            JsonValueTypeV1.STRING,
            JsonValueTypeV1.OBJECT,
            required = true,
            notes =
              "The string the component draws. `RemoteText` takes a `RemoteString`, so a design's plain string is written as `\"…\".rs` and a binding or a state value as the remote expression it names — what the design carries is the same JSON either way.",
          ),
          property(
            "style",
            JsonValueTypeV1.STRING,
            required = false,
            allowedValues =
              listOf(
                JsonPrimitive("displayLarge"),
                JsonPrimitive("displayMedium"),
                JsonPrimitive("displaySmall"),
                JsonPrimitive("titleLarge"),
                JsonPrimitive("titleMedium"),
                JsonPrimitive("titleSmall"),
                JsonPrimitive("labelLarge"),
                JsonPrimitive("labelMedium"),
                JsonPrimitive("labelSmall"),
                JsonPrimitive("bodyLarge"),
                JsonPrimitive("bodyMedium"),
                JsonPrimitive("bodySmall"),
                JsonPrimitive("bodyExtraSmall"),
                JsonPrimitive("numeralExtraLarge"),
                JsonPrimitive("numeralLarge"),
                JsonPrimitive("numeralMedium"),
                JsonPrimitive("numeralSmall"),
                JsonPrimitive("numeralExtraSmall"),
              ),
            notes =
              "A role on this catalog's type scale, written as `style = RemoteMaterialTheme.typography.<role>`. This is the way to set type here: a role carries size, weight, line height and letter spacing together through `RemoteTextStyle`, which is why the catalog's own stickers pass a role OR an explicit size and never both. The eighteen are `RemoteTypography`'s own and are NOT the Material 3 fifteen — there is no `headline` on a watch, and there are five `numeral` roles a time or a metric is set in. A role this class does not have would export as a call that does not compile, so the list is exactly what it publishes.",
          ),
          property(
            "fontSize",
            JsonValueTypeV1.NUMBER,
            required = false,
            notes =
              "Size in sp, as an override on top of `style` rather than a substitute for it — set it alone and everything else about the type comes from the ambient style. Spelled the parameter's own way; a design authored against the borrowed `m3/text` says `fontSizeSp` and the writer reads both. A whole number writes `22.rsp`; a fractional one writes `14.5f.sp.asRemoteTextUnit()`, because `RemoteTextUnitKt` publishes no `Float.rsp`.",
          ),
          property(
            "color",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "A colour: a `#RRGGBB` literal, which exports as `Color(0x…).rc`, or one of `colorTokens.roles`, which exports as `RemoteMaterialTheme.colorScheme.<role>`.",
          ),
          property(
            "textAlign",
            JsonValueTypeV1.STRING,
            required = false,
            allowedValues =
              listOf(
                JsonPrimitive("start"),
                JsonPrimitive("center"),
                JsonPrimitive("end"),
                JsonPrimitive("justify"),
              ),
          ),
          property("maxLines", JsonValueTypeV1.INTEGER, required = false),
          property(
            "fontVariationSettings",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "Variable-font axis coordinates, CSS `font-variation-settings` style with bare tags: `wght 650, wdth 87.5, GRAD -50`. Written as `RemoteText(fontVariationSettings = FontVariation.Settings(…))`, so the document carries the axes and the player applies them to the face it resolves; an axis that face does not have is ignored. `wght` set here overrides `fontWeight`.",
          ),
          property(
            "fontFeatureSettings",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "OpenType layout features, CSS `font-feature-settings` style with bare tags: `tnum, ss01, liga 0`. Written as `style = ….merge(fontFeatureSettings = …)`, since `RemoteText` has no feature parameter; Remote Compose carries them in the document beside the axes. A feature the typeface does not have is ignored.",
          ),
        )
    }
    .build()

private fun componentRemoteIcon(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteIconKt.RemoteIcon"
      group = "Iconography"
      canvas = "wear-m3/icon"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "iconKey",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("addCircle"))
              },
            )
          },
        )
        put(
          "properties",
          buildJsonObject {
            put("iconKey", JsonPrimitive("imageVector"))
            put("color", JsonPrimitive("tint"))
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "imageVector",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "A Material icon key (`home`, `search`, `addCircle`, …), written as `Icons.<Style>.<Name>.toRemoteImageVector()` — `RemoteIcon` takes a `RemoteImageVector` captured from the `androidx.compose.material.icons` vector. Unset draws `addCircle`, the default the canvas mapping above gives.",
          ),
          property(
            "contentDescription",
            JsonValueTypeV1.STRING,
            JsonValueTypeV1.OBJECT,
            required = false,
            notes =
              "What a screen reader says for the icon. Leave it empty beside a label; set it when the icon is the whole button.",
          ),
          property(
            "tint",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "A colour: a `#RRGGBB` literal or one of `colorTokens.roles`. Unset inherits the enclosing content colour, which is how a button's colour reaches its icon.",
          ),
        )
    }
    .build()

private fun componentRemoteHorizontalPageIndicator(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemotePageIndicatorKt.RemoteHorizontalPageIndicator"
      group = "Position indicators"
      canvas = "wear-m3/page-indicator"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("horizontal"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "pageCount",
            JsonValueTypeV1.INTEGER,
            required = false,
            notes =
              "How many pages there are; 4 when absent. `state` is a `RemotePageIndicatorState`, which no design value becomes, so the emitter builds it from this and `selectedPage`.",
          ),
          property(
            "selectedPage",
            JsonValueTypeV1.INTEGER,
            JsonValueTypeV1.OBJECT,
            required = false,
            notes =
              "The page shown, from zero: a literal or the design's Int state, which is how a pager drives the dots. The first when absent.",
          ),
        )
    }
    .build()

private fun componentRemoteVerticalPageIndicator(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemotePageIndicatorKt.RemoteVerticalPageIndicator"
      group = "Position indicators"
      canvas = "wear-m3/page-indicator"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("vertical"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "pageCount",
            JsonValueTypeV1.INTEGER,
            required = false,
            notes =
              "How many pages there are; 4 when absent. `state` is a `RemotePageIndicatorState`, which no design value becomes, so the emitter builds it from this and `selectedPage`.",
          ),
          property(
            "selectedPage",
            JsonValueTypeV1.INTEGER,
            JsonValueTypeV1.OBJECT,
            required = false,
            notes =
              "The page shown, from zero: a literal or the design's Int state, which is how a pager drives the dots. The first when absent.",
          ),
        )
    }
    .build()

private fun componentRemoteCircularProgressIndicator(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("progress" to seedValue("float", JsonPrimitive(0.6))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteCircularProgressIndicatorKt.RemoteCircularProgressIndicator"
      group = "Communication"
      canvas = "wear-m3/progress-indicator"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("circular"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "indeterminate",
            JsonValueTypeV1.BOOLEAN,
            required = false,
            notes =
              "Draws the indeterminate `RemoteCircularProgressIndicator`, the overload without `progress`: the record carries only the determinate one, so this is stated rather than derived. Takes no `progress` or `enabled`.",
          )
        )
    }
    .build()

private fun componentRemoteLinearProgressIndicator(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("progress" to seedValue("float", JsonPrimitive(0.6))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteLinearProgressIndicatorKt.RemoteLinearProgressIndicator"
      group = "Communication"
      canvas = "wear-m3/progress-indicator"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("linear"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteCurvedProgressIndicator(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("progress" to seedValue("float", JsonPrimitive(0.6))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteCurvedProgressIndicatorKt.RemoteCurvedProgressIndicator"
      group = "Communication"
      canvas = "wear-m3/progress-indicator"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("arc"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("Button"))))
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteButtonKt.RemoteButton"
      group = "Buttons"
      canvas = "wear-m3/button"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("filled"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "containerImageKey",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The key of an asset to fill the container with, as `asset/image`'s `assetKey`: written as the `containerPainter` overload, `containerPainter = painterRemoteImageBitmap(…)`. A plain container when absent. The record carries the overload without a painter, so this is stated rather than derived.",
          )
        ) + buttonColorRoles()
    }
    .build()

private fun componentRemoteCompactButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("label" to listOf(seedText("Compact"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteButtonKt.RemoteCompactButton"
      group = "Buttons"
      canvas = "wear-m3/button"
      canvasMapping = buildJsonObject {
        put(
          "slots",
          buildJsonObject { put("content", JsonPrimitive("label")) },
        )
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("filled"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteCard(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("Card"))))
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteCardKt.RemoteCard"
      group = "Containment"
      canvas = "wear-m3/card"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("filled"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "containerImageKey",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The key of an asset to fill the container with, as `asset/image`'s `assetKey`: written as the `containerPainter` overload, `containerPainter = painterRemoteImageBitmap(…)`. A plain container when absent. The record carries the overload without a painter, so this is stated rather than derived.",
          )
        )
    }
    .build()

private fun componentRemoteOutlinedCard(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("Card"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteCardKt.RemoteOutlinedCard"
      group = "Containment"
      canvas = "wear-m3/card"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("outlined"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        listOf(
          property(
            "borderColor",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The outline's colour, written as `border = RemoteBorderStroke(borderWidthDp.rdp, …)`: a theme role (`outline`), `transparent` or `#AARRGGBB`. Absent, the card keeps `RemoteCardDefaults.outlinedCardBorder()`.",
          ),
          property(
            "borderWidthDp",
            JsonValueTypeV1.NUMBER,
            required = false,
            notes = "The outline's width in dp, with `borderColor`; 1dp when absent.",
          ),
        )
    }
    .build()

private fun componentRemoteCheckboxButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("checked" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Checkbox"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteCheckboxButtonKt.RemoteCheckboxButton"
      group = "Selection buttons"
      canvas = "wear-m3/checkbox-button"
    }
    .build()

private fun componentRemoteSwitchButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("checked" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Switch"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteSwitchButtonKt.RemoteSwitchButton"
      group = "Selection buttons"
      canvas = "wear-m3/switch-button"
    }
    .build()

private fun componentRemoteRadioButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("selected" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Option"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteRadioButtonKt.RemoteRadioButton"
      group = "Selection buttons"
      canvas = "wear-m3/radio-button"
    }
    .build()

private fun componentRemoteSplitCheckboxButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("checked" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Checkbox"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteSplitCheckboxButtonKt.RemoteSplitCheckboxButton"
      group = "Selection buttons"
      canvas = "wear-m3/checkbox-button"
    }
    .build()

private fun componentRemoteSplitSwitchButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("checked" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Switch"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteSplitSwitchButtonKt.RemoteSplitSwitchButton"
      group = "Selection buttons"
      canvas = "wear-m3/switch-button"
    }
    .build()

private fun componentRemoteSplitRadioButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          properties = mapOf("selected" to seedValue("bool", JsonPrimitive(true))),
          slots = mapOf("label" to listOf(seedText("Option"))),
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteSplitRadioButtonKt.RemoteSplitRadioButton"
      group = "Selection buttons"
      canvas = "wear-m3/radio-button"
    }
    .build()

private fun componentRemoteSlider(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("value" to seedValue("float", JsonPrimitive(0.5))))
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteSliderKt.RemoteSlider"
      group = "Sliders"
      canvas = "wear-m3/slider"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "valueFrom",
              buildJsonObject {
                put("type", JsonPrimitive("float"))
                put("value", JsonPrimitive(0.0))
              },
            )
            put(
              "valueTo",
              buildJsonObject {
                put("type", JsonPrimitive("float"))
                put("value", JsonPrimitive(1.0))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteStepper(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(properties = mapOf("value" to seedValue("float", JsonPrimitive(0.5))))
      record = "remote-catalog/androidx.wear.compose.remote.material3.RemoteStepperKt.RemoteStepper"
      group = "Steppers"
      canvas = "wear-m3/stepper"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "valueFrom",
              buildJsonObject {
                put("type", JsonPrimitive("float"))
                put("value", JsonPrimitive(0.0))
              },
            )
            put(
              "valueTo",
              buildJsonObject {
                put("type", JsonPrimitive("float"))
                put("value", JsonPrimitive(1.0))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteEdgeButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("Done"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteEdgeButtonKt.RemoteEdgeButton"
      group = "Edge-hugging buttons"
      canvas = "wear-m3/edge-button"
      canvasMapping = buildJsonObject {
        put(
          "properties",
          buildJsonObject { put("size", JsonPrimitive("buttonSize")) },
        )
      }
    }
    .build()

private fun componentRemoteIconButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("+"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteIconButtonKt.RemoteIconButton"
      group = "Buttons"
      canvas = "wear-m3/icon-button"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("filled"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentRemoteTextButton(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent = insertSeeds(slots = mapOf("content" to listOf(seedText("OK"))))
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteTextButtonKt.RemoteTextButton"
      group = "Buttons"
      canvas = "wear-m3/text-button"
      canvasMapping = buildJsonObject {
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("standard"))
              },
            )
          },
        )
      }
      propertyCapabilities =
        colorRoles(
          "RemoteTextButtonDefaults.textButtonColors",
          listOf(
            "containerColor",
            "contentColor",
            "disabledContainerColor",
            "disabledContentColor",
          ),
        )
    }
    .build()

private fun componentRemoteTitleCard(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      insertContent =
        insertSeeds(
          slots = mapOf("title" to listOf(seedText("Title")), "content" to listOf(seedText("Card")))
        )
      record =
        "remote-catalog/androidx.wear.compose.remote.material3.RemoteTitleCardKt.RemoteTitleCard"
      group = "Containment"
      canvas = "wear-m3/card"
      canvasMapping = buildJsonObject {
        put(
          "slots",
          buildJsonObject { put("content", JsonPrimitive("title")) },
        )
        put(
          "defaults",
          buildJsonObject {
            put(
              "variant",
              buildJsonObject {
                put("type", JsonPrimitive("enum"))
                put("value", JsonPrimitive("title"))
              },
            )
          },
        )
      }
    }
    .build()

private fun componentThemeSpecimen(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record = "remote-catalog/ee.schimke.wearm3catalog.remote.RemoteThemeSpecimensKt.ThemeSpecimen"
      excluded =
        "the catalog's theme showcase, not an authorable Remote Compose component: it is a preview specimen that the design exporter cannot write."
    }
    .build()

private fun componentRemoteSticker(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record = "remote-catalog/ee.schimke.wearm3catalog.remote.CatalogThemeKt.RemoteSticker"
      excluded =
        "the catalog's own preview frame, not a Remote Compose component: every sticker is drawn inside one and nobody places one in a design."
    }
    .build()

private fun componentCapturingWearWidgetPreview(): UiBuilderAuthoredComponent =
  UiBuilderAuthoredComponent.Builder()
    .apply {
      record =
        "remote-catalog/ee.schimke.composeai.wear.preview.CapturingWearWidgetPreviewKt.CapturingWearWidgetPreview"
      excluded =
        "compose-ai-tools' `wear-preview-runtime` capture wrapper, not a Remote Compose component: it exists only to hand the widget's encoded document to `IrSidecarChannel` so the render lands a `.rc` sidecar beside the PNG. Same exclusion, and the same reason, as `remote-m3/remote-sticker` above: nobody places a capture frame in a design. The local copy this wrapper replaced published on the `Widget Container` shelf, beside the two widget-container builtins it wraps."
    }
    .build()

private fun builtinRemoteLabelButton(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("container")
    .apply {
      shelfRole = "Container"
      displayName = "Label button"
      group = "Buttons"
      canvas = "wear-m3/button"
      traits = listOf("RemoteContent", "RemoteAuthorable")
      slots =
        linkedMapOf(
          "label" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
              put("ordered", JsonPrimitive(true))
            },
          "secondaryLabel" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
              put("ordered", JsonPrimitive(true))
            },
          "icon" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
              put("ordered", JsonPrimitive(true))
            },
        )
      modifierCapabilities =
        listOf(
          "size",
          "fillMaxSize",
          "fillMaxWidth",
          "padding",
          "clip",
          "alpha",
          "offset",
          "rotate",
          "scale",
          "zIndex",
          "width",
          "height",
          "widthIn",
          "heightIn",
          "fillMaxHeight",
          "background",
          "border",
          "wrapContentSize",
          "verticalScroll",
          "horizontalScroll",
          "align",
          "alignHorizontal",
          "alignVertical",
          "weight",
          "animateEnterExit",
          "collapsiblePriority",
          "remoteCall",
          "sharedElement",
        )
      properties =
        listOf(
          property(
            "enabled",
            JsonValueTypeV1.BOOLEAN,
            JsonValueTypeV1.OBJECT,
            notes = "`RemoteButton`'s `enabled: RemoteBoolean`.",
          ),
          property(
            "borderColor",
            JsonValueTypeV1.STRING,
            notes = "`RemoteButton`'s `borderColor: RemoteColor?`.",
          ),
          property(
            "containerImageKey",
            JsonValueTypeV1.STRING,
            notes =
              "The key of an asset to fill the container with, as `asset/image`'s `assetKey`: written as the `containerPainter` overload, `containerPainter = painterRemoteImageBitmap(…)`. A plain container when absent.",
          ),
          property(
            "border",
            JsonValueTypeV1.NUMBER,
            notes = "`RemoteButton`'s `border: RemoteDp?`, the outline's width in dp.",
          ),
        ) + buttonColorRoles()
      wasm =
        UiBuilderBuiltinWasm.Builder()
          .apply {
            platformSupported = JsonPrimitive(true)
            adapterStatus = "supported"
            notes =
              "Drawn with Wear Material 3's `wear-m3/button`, the adapter the published catalog names; the widget plays `RemoteButton` itself."
          }
          .build()
      svg =
        UiBuilderBuiltinSvg.Builder("unsupported", "none")
          .apply {
            blocksExport = true
            notes = "Remote Material 3 inside a widget body has no structured SVG answer."
          }
          .build()
    }
    .build()

private fun builtinRemoteTimeText(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("list-item")
    .apply {
      shelfRole = "Leaf"
      displayName = "Time text"
      group = "Text"
      traits = listOf("RemoteAuthorable")
      modifierCapabilities =
        listOf(
          "size",
          "fillMaxSize",
          "fillMaxWidth",
          "padding",
          "clip",
          "alpha",
          "offset",
          "rotate",
          "scale",
          "zIndex",
          "width",
          "height",
          "widthIn",
          "heightIn",
          "fillMaxHeight",
          "background",
          "border",
          "wrapContentSize",
          "verticalScroll",
          "horizontalScroll",
          "align",
          "alignHorizontal",
          "alignVertical",
          "weight",
          "animateEnterExit",
          "collapsiblePriority",
          "remoteCall",
          "sharedElement",
        )
      properties =
        listOf(
          property(
            "leadingText",
            JsonValueTypeV1.STRING,
            notes = "Text before the time, e.g. a date. None when absent.",
          ),
          property(
            "trailingText",
            JsonValueTypeV1.STRING,
            notes = "Text after the time. None when absent.",
          ),
          property(
            "separator",
            JsonValueTypeV1.STRING,
            notes = "What sits between the time and the text beside it. `·` when absent.",
          ),
          property(
            "textSizeSp",
            JsonValueTypeV1.NUMBER,
            notes = "Text size. The time text style's when absent.",
          ),
          property(
            "color",
            JsonValueTypeV1.STRING,
            notes = "Text colour. The theme's onBackground when absent.",
          ),
        )
      wasm =
        UiBuilderBuiltinWasm.Builder()
          .apply {
            platformSupported = JsonPrimitive(true)
            adapterStatus = "planned"
            notes =
              "RemoteTimeText: the device's time curved along the top of its bounds, with optional text either side. The canvas shows the design's preview time."
          }
          .build()
      svg =
        UiBuilderBuiltinSvg.Builder("unsupported", "none")
          .apply {
            blocksExport = true
            notes = "A Remote Compose drawing has no structured SVG answer."
          }
          .build()
    }
    .build()

private fun builtinRemoteMaterialTheme(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("container")
    .apply {
      shelfRole = "Container"
      displayName = "Theme"
      group = "Theme"
      traits = listOf("RemoteAuthorable")
      slots =
        linkedMapOf(
          "children" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("AnyContent"))))
              put("ordered", JsonPrimitive(true))
              put("max", JsonPrimitive(1))
            }
        )
      modifierCapabilities = listOf()
      properties =
        listOf(
          property(
            "themePrimaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `primary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnPrimaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `onPrimary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themePrimaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `primaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnPrimaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `onPrimaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeSecondaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `secondary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnSecondaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `onSecondary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeSecondaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `secondaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnSecondaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `onSecondaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeTertiaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `tertiary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnTertiaryColor",
            JsonValueTypeV1.STRING,
            notes = "The `onTertiary` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeTertiaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `tertiaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnTertiaryContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `onTertiaryContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeSurfaceContainerLowColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `surfaceContainerLow` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeSurfaceContainerColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `surfaceContainer` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeSurfaceContainerHighColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `surfaceContainerHigh` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnSurfaceColor",
            JsonValueTypeV1.STRING,
            notes = "The `onSurface` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnSurfaceVariantColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `onSurfaceVariant` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOutlineColor",
            JsonValueTypeV1.STRING,
            notes = "The `outline` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOutlineVariantColor",
            JsonValueTypeV1.STRING,
            notes =
              "The `outlineVariant` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeBackgroundColor",
            JsonValueTypeV1.STRING,
            notes = "The `background` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnBackgroundColor",
            JsonValueTypeV1.STRING,
            notes = "The `onBackground` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeErrorColor",
            JsonValueTypeV1.STRING,
            notes = "The `error` role inside this theme. The enclosing theme's when absent.",
          ),
          property(
            "themeOnErrorColor",
            JsonValueTypeV1.STRING,
            notes = "The `onError` role inside this theme. The enclosing theme's when absent.",
          ),
        )
      wasm =
        UiBuilderBuiltinWasm.Builder()
          .apply {
            platformSupported = JsonPrimitive(true)
            adapterStatus = "planned"
            notes =
              "RemoteMaterialTheme: re-skins the subtree it holds with the colour roles it overrides, reading the enclosing theme for the rest."
          }
          .build()
      svg =
        UiBuilderBuiltinSvg.Builder("unsupported", "none")
          .apply {
            blocksExport = true
            notes = "A Remote Compose drawing has no structured SVG answer."
          }
          .build()
    }
    .build()

private fun builtinLottie(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("list-item")
    .apply {
      shelfRole = "Leaf"
      displayName = "Lottie animation"
      group = "Content"
      traits = listOf("RemoteContent", "RemoteAuthorable")
      modifierCapabilities =
        listOf(
          "size",
          "fillMaxSize",
          "fillMaxWidth",
          "clip",
          "alpha",
          "offset",
          "rotate",
          "scale",
          "zIndex",
          "width",
          "height",
          "widthIn",
          "heightIn",
          "fillMaxHeight",
          "align",
          "alignHorizontal",
          "alignVertical",
          "weight",
          "animateEnterExit",
          "collapsiblePriority",
          "remoteCall",
          "sharedElement",
        )
      properties =
        listOf(
          property(
            "url",
            JsonValueTypeV1.STRING,
            notes =
              "Where the animation was fetched from. Resolved into `json` once, in the builder; the generated widget never reaches the network.",
          ),
          property(
            "json",
            JsonValueTypeV1.STRING,
            notes =
              "The Lottie animation itself, as JSON text. This is what is compiled into the Remote Compose document, so it is what the export needs.",
          ),
          property(
            "progress",
            JsonValueTypeV1.NUMBER,
            JsonValueTypeV1.OBJECT,
            notes =
              "Pins the animation to one frame, 0 (first) to 1 (last). Unset — the default — lets the document's animation clock run it in a loop.",
          ),
        )
      wasm =
        UiBuilderBuiltinWasm.Builder()
          .apply {
            platformSupported = JsonPrimitive(true)
            adapterStatus = "supported"
            notes =
              "Drawn as a named placeholder carrying the animation's source and size. The canvas has no Lottie renderer, and compiling the animation the way the export does — into Remote Compose operations — is Horologist's Android-only creation API, which a Wasm build cannot link. A lookalike would be an impression of an animation nobody could check; the picture comes from the native lane, which builds this design's own generated document."
          }
          .build()
      svg =
        UiBuilderBuiltinSvg.Builder("unsupported", "none")
          .apply {
            blocksExport = true
            notes =
              "A placeholder on the canvas must not claim structured SVG parity with an animation."
          }
          .build()
    }
    .build()

private fun builtinWidgetContainerSmall(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("screen-root")
    .apply {
      displayName = "Wear widget · Small (216×76dp)"
      group = "Widget Container"
      canvas = "widget-container"
      traits = listOf("ScreenContent", "WearWidgetHost", "RemoteContentHost")
      slots =
        linkedMapOf(
          "background" to
            buildJsonObject {
              put("acceptedRoles", JsonArray(listOf(JsonPrimitive("Leaf"))))
              put(
                "acceptedTraits",
                JsonArray(listOf(JsonPrimitive("DrawLayer"), JsonPrimitive("ImageContent"))),
              )
            },
          "content" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
              put("max", JsonPrimitive(1))
            },
        )
      properties =
        listOf(
          property(
            "background",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The widget's own background, painted by the host inside the frame it draws. Defaults to #FF272430, the colour WearWidgetContainer applies to a widget that declares none. Padding and corner radius are not properties: WearWidgetParams hands them to the widget per host shape (0 rectangular, 26 squircle, 999 round), so the outermost box never authors a radius.",
          ),
          property(
            "themeDisplayTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the display roles (displayLarge, displayMedium, displaySmall, numeralExtraLarge, numeralLarge, numeralMedium, numeralSmall, numeralExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTitleTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the title roles (titleLarge, titleMedium, titleSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeBodyTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the body roles (bodyLarge, bodyMedium, bodySmall, bodyExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeLabelTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the label roles (labelLarge, labelMedium, labelSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTextStyle",
            JsonValueTypeV1.STRING,
            required = false,
            allowedValues =
              listOf(
                JsonPrimitive("displayLarge"),
                JsonPrimitive("displayMedium"),
                JsonPrimitive("displaySmall"),
                JsonPrimitive("titleLarge"),
                JsonPrimitive("titleMedium"),
                JsonPrimitive("titleSmall"),
                JsonPrimitive("labelLarge"),
                JsonPrimitive("labelMedium"),
                JsonPrimitive("labelSmall"),
                JsonPrimitive("bodyLarge"),
                JsonPrimitive("bodyMedium"),
                JsonPrimitive("bodySmall"),
                JsonPrimitive("bodyExtraSmall"),
                JsonPrimitive("numeralExtraLarge"),
                JsonPrimitive("numeralLarge"),
                JsonPrimitive("numeralMedium"),
                JsonPrimitive("numeralSmall"),
                JsonPrimitive("numeralExtraSmall"),
              ),
            notes =
              "The type role a text with no `style` of its own is set in, for everything this theme host contains — and with it that role's size and typeface. Both themes default to `bodyLarge`; choose `labelMedium` for a design whose plain text is mostly labels, or a display role for one that is mostly figures. Generated code provides it as `ProvideTextStyle(MaterialTheme.typography.<role>)` inside the theme.",
          ),
        )
      modifierCapabilities = listOf()
    }
    .build()

private fun builtinWidgetContainerLarge(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("screen-root")
    .apply {
      displayName = "Wear widget · Large (216×124dp)"
      group = "Widget Container"
      canvas = "widget-container"
      traits = listOf("ScreenContent", "WearWidgetHost", "RemoteContentHost")
      slots =
        linkedMapOf(
          "background" to
            buildJsonObject {
              put("acceptedRoles", JsonArray(listOf(JsonPrimitive("Leaf"))))
              put(
                "acceptedTraits",
                JsonArray(listOf(JsonPrimitive("DrawLayer"), JsonPrimitive("ImageContent"))),
              )
            },
          "content" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
              put("max", JsonPrimitive(1))
            },
        )
      properties =
        listOf(
          property(
            "background",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The widget's own background, painted by the host inside the frame it draws. Defaults to #FF272430, the colour WearWidgetContainer applies to a widget that declares none. Padding and corner radius are not properties: WearWidgetParams hands them to the widget per host shape (0 rectangular, 26 squircle, 999 round), so the outermost box never authors a radius.",
          ),
          property(
            "themeDisplayTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the display roles (displayLarge, displayMedium, displaySmall, numeralExtraLarge, numeralLarge, numeralMedium, numeralSmall, numeralExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTitleTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the title roles (titleLarge, titleMedium, titleSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeBodyTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the body roles (bodyLarge, bodyMedium, bodySmall, bodyExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeLabelTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the label roles (labelLarge, labelMedium, labelSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTextStyle",
            JsonValueTypeV1.STRING,
            required = false,
            allowedValues =
              listOf(
                JsonPrimitive("displayLarge"),
                JsonPrimitive("displayMedium"),
                JsonPrimitive("displaySmall"),
                JsonPrimitive("titleLarge"),
                JsonPrimitive("titleMedium"),
                JsonPrimitive("titleSmall"),
                JsonPrimitive("labelLarge"),
                JsonPrimitive("labelMedium"),
                JsonPrimitive("labelSmall"),
                JsonPrimitive("bodyLarge"),
                JsonPrimitive("bodyMedium"),
                JsonPrimitive("bodySmall"),
                JsonPrimitive("bodyExtraSmall"),
                JsonPrimitive("numeralExtraLarge"),
                JsonPrimitive("numeralLarge"),
                JsonPrimitive("numeralMedium"),
                JsonPrimitive("numeralSmall"),
                JsonPrimitive("numeralExtraSmall"),
              ),
            notes =
              "The type role a text with no `style` of its own is set in, for everything this theme host contains — and with it that role's size and typeface. Both themes default to `bodyLarge`; choose `labelMedium` for a design whose plain text is mostly labels, or a display role for one that is mostly figures. Generated code provides it as `ProvideTextStyle(MaterialTheme.typography.<role>)` inside the theme.",
          ),
        )
      modifierCapabilities = listOf()
    }
    .build()

private fun builtinWidgetContainerAdaptive(): UiBuilderBuiltin =
  UiBuilderBuiltin.Builder("screen-root")
    .apply {
      insertContent =
        insertSeeds(
          slots =
            mapOf(
              "headline" to listOf(seedText("Headline")),
              "supporting" to listOf(seedText("Supporting text")),
              "action" to listOf(seedButton("Open")),
            )
        )
      displayName = "Wear widget · Adaptive (experimental)"
      group = "Widget Container"
      canvas = "widget-container"
      traits = listOf("ScreenContent", "WearWidgetHost", "RemoteContentHost")
      slots =
        linkedMapOf(
          "background" to
            buildJsonObject {
              put("acceptedRoles", JsonArray(listOf(JsonPrimitive("Leaf"))))
              put(
                "acceptedTraits",
                JsonArray(listOf(JsonPrimitive("DrawLayer"), JsonPrimitive("ImageContent"))),
              )
            },
          "headline" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
            },
          "supporting" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
            },
          "action" to
            buildJsonObject {
              put(
                "acceptedRoles",
                JsonArray(
                  listOf(
                    JsonPrimitive("Scaffold"),
                    JsonPrimitive("Container"),
                    JsonPrimitive("Leaf"),
                  )
                ),
              )
              put("acceptedTraits", JsonArray(listOf(JsonPrimitive("RemoteAuthorable"))))
            },
        )
      properties =
        listOf(
          property(
            "background",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The widget's own background, painted by the host inside the frame it draws. Defaults to #FF272430, the colour WearWidgetContainer applies to a widget that declares none. Padding and corner radius are not properties: WearWidgetParams hands them to the widget per host shape (0 rectangular, 26 squircle, 999 round), so the outermost box never authors a radius.",
          ),
          property(
            "themeDisplayTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the display roles (displayLarge, displayMedium, displaySmall, numeralExtraLarge, numeralLarge, numeralMedium, numeralSmall, numeralExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTitleTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the title roles (titleLarge, titleMedium, titleSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeBodyTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the body roles (bodyLarge, bodyMedium, bodySmall, bodyExtraSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeLabelTypeface",
            JsonValueTypeV1.STRING,
            required = false,
            notes =
              "The font family for the label roles (labelLarge, labelMedium, labelSmall) of everything this theme host contains. A family name: one of the editor's vendored faces or any Google Fonts family (\"Space Grotesk\", \"Michroma\"); a `google:` prefix is accepted. Generated code sets it as a `GoogleFont` family on the theme's typography. Unset keeps the platform's own face for these roles.",
          ),
          property(
            "themeTextStyle",
            JsonValueTypeV1.STRING,
            required = false,
            allowedValues =
              listOf(
                JsonPrimitive("displayLarge"),
                JsonPrimitive("displayMedium"),
                JsonPrimitive("displaySmall"),
                JsonPrimitive("titleLarge"),
                JsonPrimitive("titleMedium"),
                JsonPrimitive("titleSmall"),
                JsonPrimitive("labelLarge"),
                JsonPrimitive("labelMedium"),
                JsonPrimitive("labelSmall"),
                JsonPrimitive("bodyLarge"),
                JsonPrimitive("bodyMedium"),
                JsonPrimitive("bodySmall"),
                JsonPrimitive("bodyExtraSmall"),
                JsonPrimitive("numeralExtraLarge"),
                JsonPrimitive("numeralLarge"),
                JsonPrimitive("numeralMedium"),
                JsonPrimitive("numeralSmall"),
                JsonPrimitive("numeralExtraSmall"),
              ),
            notes =
              "The type role a text with no `style` of its own is set in, for everything this theme host contains — and with it that role's size and typeface. Both themes default to `bodyLarge`; choose `labelMedium` for a design whose plain text is mostly labels, or a display role for one that is mostly figures. Generated code provides it as `ProvideTextStyle(MaterialTheme.typography.<role>)` inside the theme.",
          ),
        )
      modifierCapabilities = listOf()
    }
    .build()

private fun policySupersedes(): JsonElement = buildJsonObject {
  put(
    "m3/text",
    buildJsonObject {
      put("componentId", JsonPrimitive("remote-m3/remote-text"))
      put(
        "properties",
        buildJsonObject { put("fontSizeSp", JsonPrimitive("fontSize")) },
      )
    },
  )
}

// Insert seeds: what the builder places inside a component dropped from the palette, read ahead of
// compose-ui-builder's `StarterContent` table. Labels are `remote-m3/remote-text`, which this
// catalog offers, never `m3/text`, which it supersedes.
private fun insertSeeds(
  properties: Map<String, JsonElement> = emptyMap(),
  slots: Map<String, List<JsonElement>> = emptyMap(),
): JsonElement = seed(componentId = null, properties, slots)

private fun seedText(text: String): JsonElement =
  seed(
    "remote-m3/remote-text",
    properties = mapOf("text" to seedValue("string", JsonPrimitive(text))),
  )

private fun seedButton(label: String): JsonElement =
  seed("remote-m3/remote-button", slots = mapOf("content" to listOf(seedText(label))))

private fun seedValue(type: String, value: JsonPrimitive): JsonElement = buildJsonObject {
  put("type", JsonPrimitive(type))
  put("value", value)
}

private fun seed(
  componentId: String?,
  properties: Map<String, JsonElement> = emptyMap(),
  slots: Map<String, List<JsonElement>> = emptyMap(),
): JsonElement = buildJsonObject {
  componentId?.let { put("componentId", JsonPrimitive(it)) }
  if (properties.isNotEmpty()) put("properties", JsonObject(properties))
  if (slots.isNotEmpty()) put("slots", JsonObject(slots.mapValues { JsonArray(it.value) }))
}

/** The colour roles `RemoteButtonDefaults.buttonColors` takes, each a property a node may set. */
private fun buttonColorRoles(): List<JsonElement> =
  colorRoles(
    "RemoteButtonDefaults.buttonColors",
    listOf(
      "containerColor",
      "contentColor",
      "secondaryContentColor",
      "iconColor",
      "disabledContainerColor",
      "disabledContentColor",
      "disabledSecondaryContentColor",
      "disabledIconColor",
    ),
  )

/**
 * A colours factory's roles as properties: a literal colour or theme role, or a computed one (a
 * colour `lerp` across a component argument, the player's colour `tween`).
 */
private fun colorRoles(factory: String, roles: List<String>): List<JsonElement> =
  roles.map { role ->
    property(
      role,
      JsonValueTypeV1.STRING,
      JsonValueTypeV1.OBJECT,
      required = false,
      notes =
        "`$factory`' `$role`: a theme role (`primary`), `transparent`, `#AARRGGBB`, or a computed colour such as `lerp(theme.primary, theme.primaryDim, @on)`. The roles a node leaves out keep the factory's defaults; stated rather than derived, because `colors` is a bundle the record cannot type.",
    )
  }

private fun property(
  name: String,
  vararg types: JsonValueTypeV1,
  required: Boolean = false,
  allowedValues: List<JsonElement> = emptyList(),
  notes: String? = null,
): JsonElement =
  Json.encodeToJsonElement(
    PropertyCapabilityV1.serializer(),
    PropertyCapabilityV1.Builder(
        name,
        if (types.size == 1) JsonPrimitive(types.single().name.lowercase())
        else JsonArray(types.map { JsonPrimitive(it.name.lowercase()) }),
      )
      .apply {
        this.required = required
        this.allowedValues = allowedValues
        this.notes = notes
      }
      .build(),
  )

private fun slot(
  name: String,
  min: Int,
  max: Int?,
  ordered: Boolean,
  roles: List<String>,
  traits: List<String>,
): JsonElement =
  Json.encodeToJsonElement(
    SlotCapabilityV1.serializer(),
    SlotCapabilityV1.Builder(
        name,
        SlotCardinalityV1.Builder()
          .apply {
            this.min = min
            this.max = max
          }
          .build(),
        ordered,
      )
      .apply {
        acceptedRoles = roles
        acceptedTraits = traits
      }
      .build(),
  )
