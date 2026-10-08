package ee.schimke.wearm3catalog.uibuilder

import ee.schimke.composeai.uibuilder.renderer.sdk.CanvasNodeScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A control's own change callback, two-way bound: write [value] back to the variable [property]
 * reads, then run [event]'s actions.
 *
 * compose-ui-builder's `CanvasNodeScope.changeBoundState`, with the same rule, for the SDK this
 * catalog builds against when it predates it: only a bare `{"type": "state"}` read is written back,
 * and not when [event]'s own actions already write that variable — a hand-authored `toggle` — so a
 * tap changes it once. Replace with `changeBoundState` once every checkout this builds against has
 * it; named apart so the SDK member cannot shadow it meanwhile.
 */
internal fun CanvasNodeScope.writeBoundChange(
  property: String,
  value: String?,
  event: String = "click",
) {
  val wrapper = node.properties[property] as? JsonObject
  val variable =
    if ((wrapper?.get("type") as? JsonPrimitive)?.contentOrNull == "state")
      (wrapper?.get("variable") as? JsonPrimitive)?.contentOrNull
    else null
  val written =
    variable != null &&
      (node.eventBindings[event] as? JsonArray).orEmpty().any { element ->
        val action = element as? JsonObject ?: return@any false
        (action["type"] as? JsonPrimitive)?.contentOrNull in WRITING_ACTIONS &&
          (action["variable"] as? JsonPrimitive)?.contentOrNull == variable
      }
  if (variable != null && !written) updateBoundState(property, value)
  dispatch(event)
}

private val WRITING_ACTIONS =
  setOf("set", "select", "selectOrClear", "setText", "toggle", "increment")

/**
 * The event this node's change runs: the first of [events] it binds, else `click`.
 *
 * These adapters also draw the `remote-m3` selection rows (`ui-builder.policy.json` maps
 * `remote-checkbox-button` and its split and radio twins onto them), whose documents key the change
 * as `checkedChange`, `select` or `selectionClick`, as `RemoteM3DevicePreview` plays them. A Wear
 * design keys it as `click`, which stays the fallback.
 */
internal fun CanvasNodeScope.boundEvent(vararg events: String): String =
  events.firstOrNull { node.eventBindings[it] != null } ?: "click"
