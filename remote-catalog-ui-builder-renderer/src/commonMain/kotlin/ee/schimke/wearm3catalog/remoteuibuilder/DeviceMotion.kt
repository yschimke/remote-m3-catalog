package ee.schimke.wearm3catalog.remoteuibuilder

import androidx.compose.remote.creation.compose.modifier.RemoteEnterTransition
import androidx.compose.remote.creation.compose.modifier.RemoteExitTransition
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.sharedElement
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteTweenSpec
import androidx.compose.remote.creation.compose.state.remoteTween
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

/** A `tween` or motion modifier's easing name as the player's constant; `standard` otherwise. */
internal fun remoteEasing(name: String?): RemoteEasing =
  when (name) {
    "linear" -> RemoteEasing.Linear
    "accelerate" -> RemoteEasing.Accelerate
    "decelerate" -> RemoteEasing.Decelerate
    "anticipate" -> RemoteEasing.Anticipate
    "overshoot" -> RemoteEasing.Overshoot
    "bounce" -> RemoteEasing.Bounce
    "elastic" -> RemoteEasing.Elastic
    else -> RemoteEasing.Standard
  }

/**
 * `sharedElement` as the export writes it: the bare key when that is all the design names, and
 * `sharedElement`'s own timing and ways in and out when it names more.
 */
internal fun RemoteModifier.sharedElementFrom(modifier: JsonObject): RemoteModifier {
  val key = modifier.number("key")?.toInt()?.takeIf { it >= 1 } ?: return this
  if (MOTION_FIELDS.none { modifier.text(it) != null }) return sharedElement(key = key)
  val motion = modifier.motion()
  return sharedElement(key, motion.spec, motion.enter, motion.exit)
}

/**
 * `animateEnterExit` as the export writes it: an enabled spec under the unset id, -1. The creation
 * library's own `animateEnterExit` writes id 0, which every player reads as "disabled".
 */
internal fun RemoteModifier.animateEnterExitFrom(modifier: JsonObject): RemoteModifier {
  val motion = modifier.motion()
  return animationSpec(-1, motion.spec, motion.spec, motion.enter, motion.exit)
}

private class Motion(
  val spec: RemoteTweenSpec,
  val enter: RemoteEnterTransition,
  val exit: RemoteExitTransition,
)

private fun JsonObject.motion(): Motion =
  Motion(
    spec = remoteTween((number("durationMs") ?: 300f).toInt(), remoteEasing(text("easing"))),
    enter =
      when (text("enter")) {
        "slideInLeft" -> RemoteEnterTransition.SlideInLeft
        "slideInRight" -> RemoteEnterTransition.SlideInRight
        "slideInTop" -> RemoteEnterTransition.SlideInTop
        "slideInBottom" -> RemoteEnterTransition.SlideInBottom
        "rotate" -> RemoteEnterTransition.Rotate
        else -> RemoteEnterTransition.FadeIn
      },
    exit =
      when (text("exit")) {
        "slideOutLeft" -> RemoteExitTransition.SlideOutLeft
        "slideOutRight" -> RemoteExitTransition.SlideOutRight
        "slideOutTop" -> RemoteExitTransition.SlideOutTop
        "slideOutBottom" -> RemoteExitTransition.SlideOutBottom
        "rotate" -> RemoteExitTransition.Rotate
        else -> RemoteExitTransition.FadeOut
      },
  )

private val MOTION_FIELDS = listOf("durationMs", "easing", "enter", "exit")

private fun JsonObject.number(name: String): Float? = (this[name] as? JsonPrimitive)?.floatOrNull

private fun JsonObject.text(name: String): String? =
  (this[name] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
