package ee.schimke.wearm3catalog.uibuilder

import ee.schimke.composeai.uibuilder.renderer.sdk.CanvasNodeScope

/**
 * The event this node's change runs: the first of [events] it binds, else `click`.
 *
 * These adapters also draw the `remote-m3` selection rows (`ui-builder.policy.json` maps
 * `remote-checkbox-button` and its split and radio twins onto them), whose documents key the change
 * as `checkedChange`, `select` or `selectionClick`, as `RemoteM3DevicePreview` plays them. A Wear
 * design keys it as `click`, which stays the fallback. Passed to the SDK's `changeBoundState`, it
 * is also the event whose authored writes take precedence over the write back.
 */
internal fun CanvasNodeScope.boundEvent(vararg events: String): String =
  events.firstOrNull { node.eventBindings[it] != null } ?: "click"
