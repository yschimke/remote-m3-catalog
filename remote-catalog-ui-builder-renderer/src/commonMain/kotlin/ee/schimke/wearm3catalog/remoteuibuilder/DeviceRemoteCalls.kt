package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.basicMarquee
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.compose.remote.creation.compose.modifier.enabled
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.stateDescription
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.semantics.Role
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

/**
 * A `remoteCall` modifier recorded as the `RemoteModifier` call the exported widget writes, so the
 * device preview plays it: the layer, the accessibility, the visibility, the marquee.
 *
 * Calls without a case here record nothing rather than taking the preview down — they are the
 * export's to write and the Native / Live lanes' to play; the device preview is a convenience.
 */
internal fun RemoteModifier.remoteCall(
  modifier: JsonObject,
  values: DocumentValues,
): RemoteModifier {
  val name = (modifier["name"] as? JsonPrimitive)?.contentOrNull ?: return this
  val args = modifier["args"] as? JsonObject ?: JsonObject(emptyMap())
  fun float(key: String) = values.float(args[key])
  fun literal(key: String) = (args[key] as? JsonObject)?.get("value") as? JsonPrimitive
  return when (name) {
    "graphicsLayer" ->
      graphicsLayer(
        scaleX = float("scaleX") ?: 1f.rf,
        scaleY = float("scaleY") ?: 1f.rf,
        rotationX = float("rotationX") ?: 0f.rf,
        rotationY = float("rotationY") ?: 0f.rf,
        rotationZ = float("rotationZ") ?: 0f.rf,
        shadowElevation = float("shadowElevation") ?: 0f.rf,
        transformOriginX = float("transformOriginX") ?: 0.5f.rf,
        transformOriginY = float("transformOriginY") ?: 0.5f.rf,
        translationX = float("translationX") ?: 0f.rf,
        translationY = float("translationY") ?: 0f.rf,
        alpha = float("alpha") ?: 1f.rf,
        cameraDistance = float("cameraDistance") ?: 8f.rf,
      )
    "semantics" -> {
      val description = values.string(args["contentDescription"])
      val state = values.string(args["stateDescription"])
      val chosen =
        when (literal("role")?.contentOrNull) {
          "Button" -> Role.Button
          "Checkbox" -> Role.Checkbox
          "Switch" -> Role.Switch
          "RadioButton" -> Role.RadioButton
          "Tab" -> Role.Tab
          "Image" -> Role.Image
          "DropdownList" -> Role.DropdownList
          else -> null
        }
      val isEnabled = literal("enabled")?.booleanOrNull
      val apply:
        androidx.compose.remote.creation.compose.modifier.SemanticsPropertyReceiver.() -> Unit =
        {
          description?.let { contentDescription = it }
          state?.let { stateDescription = it }
          chosen?.let { role = it }
          isEnabled?.let { enabled = it }
        }
      if (literal("clear")?.booleanOrNull == true) clearAndSetSemantics(apply)
      else semantics(literal("mergeDescendants")?.booleanOrNull == true, apply)
    }
    "visibility" -> values.int(args["visible"])?.let { visibility(it) } ?: this
    "defaultMinSize" ->
      defaultMinSize(float("minWidth")?.asRemoteDp(), float("minHeight")?.asRemoteDp())
    "basicMarquee" -> {
      val repeatDelay = literal("repeatDelayMillis")?.floatOrNull ?: 1_200f
      basicMarquee(
        iterations = literal("iterations")?.floatOrNull?.toInt() ?: Int.MAX_VALUE,
        repeatDelayMillis = repeatDelay,
        initialDelayMillis = literal("initialDelayMillis")?.floatOrNull ?: repeatDelay,
        spacing = literal("spacing")?.floatOrNull ?: 0f,
        velocity = literal("velocity")?.floatOrNull ?: 20f,
      )
    }
    else -> this
  }
}
