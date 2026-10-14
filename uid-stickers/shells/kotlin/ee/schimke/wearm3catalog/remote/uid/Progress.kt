@file:Suppress("RestrictedApiAndroidX")

package ee.schimke.wearm3catalog.remote.uid

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import ee.schimke.composeai.daemon.rememberOverridableRemoteFloat
import ee.schimke.composeai.overrides.previewOverrideBoolean
import ee.schimke.composeai.overrides.previewOverrideChoice
import ee.schimke.composeai.overrides.previewOverrideFloat
import ee.schimke.composeai.preview.CatalogComponent
import ee.schimke.composeai.preview.OverrideVariant
import ee.schimke.wearm3catalog.remote.CatalogRemoteDisplay
import ee.schimke.wearm3catalog.remote.RemoteSticker
import generated.uibuilder.remote.KitCircularProgressRemote
import generated.uibuilder.remote.KitIndeterminateCircularProgressRemote

// The shell reads the hand-written sticker's knobs in the same order and passes them in: the
// progress (a named value the viewer reseeds live), the enabled state, and the stroke width, which
// is Wear's screen-size token rather than a literal. The indeterminate mode is the other overload.

@OverrideVariant(
  name = "disabled",
  booleans = ["enabled=false"],
  kitAxis = "Disabled",
  kitValue = "Yes",
)
// The kit's `Progress` axis, which is the `progress` knob this row already carries — three more of
// the set's cells for three seeded floats
// ([#160](https://github.com/yschimke/wear-m3-catalog/issues/160)). The cell names and the values
// are the Wear sibling's, so the two columns pair.
@OverrideVariant(
  name = "complete",
  floats = ["progress=1.0"],
  kitAxis = "Progress",
  kitValue = "Complete",
)
@OverrideVariant(
  name = "overflow",
  floats = ["progress=1.4"],
  kitAxis = "Progress",
  kitValue = "Overflow",
)
@OverrideVariant(
  name = "zero",
  floats = ["progress=0.0"],
  kitAxis = "Progress",
  kitValue = "Zero",
)
// The crossing of the two, and the last cell of this set that has a call site. It was withheld as
// an EMPTY FRAME — a disabled ring draws entirely in its disabled track colour, and at zero
// progress that used to leave nothing lit — which is the same reason the Wear sibling gave, and
// which turned out to have expired there
// ([#178](https://github.com/yschimke/wear-m3-catalog/issues/178)).
// A gap held open by a library limitation is worth re-rendering rather than re-reading, so this
// one is drawn and the renderer decides: `StickerBakeCoverageTest` fails it if it really is blank
// and nothing exempts it.
@OverrideVariant(
  name = "zero-disabled",
  booleans = ["enabled=false"],
  floats = ["progress=0.0"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Medium",
      "Progress=Zero",
      "Dot value=No",
      "Disabled=Yes",
    ],
  secondary = true,
)
// The kit publishes four determinate `Progress=` values and no indeterminate one, but the library
// ships both on the same name — so it folds in here as a cell rather than standing up a second
// card for the same component. It used to be `CircularProgressIndicator-Indeterminate`, a
// top-level component; the Wear sibling has carried it as this cell all along, and two sheets that
// spell one component two ways cannot be read side by side.
//
// The cell is a still of something that only reads as itself in motion, which is what
// `motionPreview` on the component below is for.
@OverrideVariant(name = "indeterminate", strings = ["mode=indeterminate"])
// The kit's `Stroke Width` axis, and the second half of every `Progress`/`Disabled` cell above.
// These six are the Wear sibling's `small-stroke*` cells under the same names and the same seeded
// values, so the two columns pair cell for cell and this set stops being one the Remote sheet is
// behind on.
//
// WHERE THE NUMBER COMES FROM, because that is the whole question here. The kit's `Stroke
// Width=Small | Medium` is a pair of dp values, and `RemoteProgressIndicatorDefaults` publishes
// neither — it carries `IndeterminateStrokeWidth` and the `CurvedIndicator*` family and nothing for
// the determinate ring, so the argument has no token on the Remote side to spell it with. Writing a
// literal here would be exactly what the Wear sibling refuses to do for the same set's `Type=Top
// Gap` cells: "an invented number under the kit's name is worse than an honest absence."
//
// It does not come to that. `androidx.wear.compose:compose-material3` publishes
// `CircularProgressIndicatorDefaults.smallStrokeWidth` / `largeStrokeWidth` — where the Wear
// sibling reads the same two values — and `remote-material3`'s POM already depends on it at compile
// scope — so this is not a number invented for the Remote sheet, it is the same named token both
// sheets draw, resolved by the same library. (Both tokens branch on `isSmallScreen`, so they are
// screen-size dependent rather than constant; the capture frame decides, as it does on Wear.)
// `remote-catalog/build.gradle.kts` declares that dependency outright rather than leaning on a
// transitive an upstream POM could quietly demote.
@OverrideVariant(
  name = "small-stroke",
  strings = ["stroke=small"],
  kitAxis = "Stroke Width",
  kitValue = "Small",
)
@OverrideVariant(
  name = "small-stroke-complete",
  strings = ["stroke=small"],
  floats = ["progress=1.0"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Small",
      "Progress=Complete",
      "Dot value=No",
      "Disabled=No",
    ],
  secondary = true,
)
@OverrideVariant(
  name = "small-stroke-overflow",
  strings = ["stroke=small"],
  floats = ["progress=1.4"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Small",
      "Progress=Overflow",
      "Dot value=No",
      "Disabled=No",
    ],
  secondary = true,
)
@OverrideVariant(
  name = "small-stroke-zero",
  strings = ["stroke=small"],
  floats = ["progress=0.0"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Small",
      "Progress=Zero",
      "Dot value=No",
      "Disabled=No",
    ],
  secondary = true,
)
@OverrideVariant(
  name = "small-stroke-disabled",
  booleans = ["enabled=false"],
  strings = ["stroke=small"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Small",
      "Progress=In progress",
      "Dot value=No",
      "Disabled=Yes",
    ],
  secondary = true,
)
@OverrideVariant(
  name = "small-stroke-zero-disabled",
  booleans = ["enabled=false"],
  strings = ["stroke=small"],
  floats = ["progress=0.0"],
  kitProps =
    [
      "Type=Full",
      "Segments=1",
      "Stroke Width=Small",
      "Progress=Zero",
      "Dot value=No",
      "Disabled=Yes",
    ],
  secondary = true,
)
@CatalogComponent(
  id = "CircularProgressIndicator",
  group = "Communication",
  parallel = "CircularProgressIndicator",
  reference = "figma:B24oss2tTeXAFykyeyusz0/41424:58637",
  referenceSet = "figma:B24oss2tTeXAFykyeyusz0/41424:58385",
  caption = "Determinate circular progress rail against the display edge, at a fixed 60%.",
  // The indeterminate sweep, recorded in ComponentVariantPreviews.kt. It cannot be annotated on
  // the variant itself: a motion annotation rides every `@OverrideVariant` cell and would publish
  // one recording under every progress name.
  motionPreview = "IndeterminateCircularProgressMotionRemote",
)
@CatalogRemoteDisplay
@Composable
fun CircularProgressRemote() = RemoteSticker {
  val progress = rememberOverridableRemoteFloat("progress", previewOverrideFloat("progress", 0.6f))
  val stroke =
    if (previewOverrideChoice("stroke", "medium", listOf("medium", "small")) == "small")
      CircularProgressIndicatorDefaults.smallStrokeWidth.asRdp()
    else CircularProgressIndicatorDefaults.largeStrokeWidth.asRdp()
  if (
    previewOverrideChoice("mode", "determinate", listOf("determinate", "indeterminate")) ==
      "indeterminate"
  ) {
    KitIndeterminateCircularProgressRemote(RemoteModifier, stroke)
  } else {
    KitCircularProgressRemote(
      RemoteModifier,
      progress,
      previewOverrideBoolean("enabled", true).rb,
      stroke,
    )
  }
}
