/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.remote.material3.internal

import androidx.compose.remote.creation.compose.capture.currentProfileSupportsThemedColors
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColor.Companion.createNamedRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A color role in a remote design system's color scheme, such as primary or onSurface.
 *
 * This doesn't depend on any particular design system, so it can back the color schemes of other
 * remote design systems.
 *
 * @property name The named color used when the color is not themed, e.g. `"WearM3.primary"`. A host
 *   can override named colors.
 * @property lightToken The Android system color used in light mode, e.g.
 *   `android.R.color.system_primary_light`.
 * @property darkToken The Android system color used in dark mode, e.g.
 *   `android.R.color.system_primary_dark`. Design systems that are always dark can use the same
 *   token for both modes.
 */
// Port: upstream stores the `android.R.color` resource ids directly; commonMain cannot name
// `android.R`, so the roles carry a [SystemColorToken] and the Android actual of
// [themedRemoteColor] maps it to the resource id.
internal class RemoteColorRole(
    val name: String,
    val lightToken: SystemColorToken,
    val darkToken: SystemColorToken = lightToken,
)

/** How the [RemoteColor] of each [RemoteColorRole] in a color scheme is resolved. */
internal enum class RemoteColorSource {
    /** Named colors, which resolve to their fallback unless the host overrides them. */
    Named,

    /** Themed colors, which follow the player's Android system (dynamic) colors. */
    Themed;

    /** Returns the [RemoteColor] for [role], using [fallback] when it can't be resolved. */
    fun resolve(role: RemoteColorRole, fallback: Color): RemoteColor =
        when (this) {
            Named -> createNamedRemoteColor(role.name, fallback)
            Themed -> themedRemoteColor(role, fallback)
        }
}

/**
 * Returns the [RemoteColorSource] for a dynamic color scheme: [RemoteColorSource.Themed] if the
 * current creation profile supports themed colors, otherwise [RemoteColorSource.Named].
 *
 * Support is checked once for the whole scheme, so a scheme never mixes themed and named colors.
 */
@Composable
@RemoteComposable
internal fun dynamicRemoteColorSource(): RemoteColorSource =
    if (currentProfileSupportsThemedColors()) RemoteColorSource.Themed else RemoteColorSource.Named

/**
 * Returns a themed [RemoteColor] for [role] that follows the player's Android system colors, with
 * [fallback] for both modes. Only called when the creation profile supports themed colors.
 */
internal expect fun themedRemoteColor(role: RemoteColorRole, fallback: Color): RemoteColor

/** The Android system (dynamic) color resources the Wear Material 3 color roles map to. */
internal enum class SystemColorToken {
    SYSTEM_BACKGROUND_DARK,
    SYSTEM_ERROR_300,
    SYSTEM_ERROR_CONTAINER_DARK,
    SYSTEM_ERROR_DARK,
    SYSTEM_ON_BACKGROUND_DARK,
    SYSTEM_ON_ERROR_CONTAINER_DARK,
    SYSTEM_ON_ERROR_DARK,
    SYSTEM_ON_PRIMARY_CONTAINER_DARK,
    SYSTEM_ON_PRIMARY_DARK,
    SYSTEM_ON_SECONDARY_CONTAINER_DARK,
    SYSTEM_ON_SECONDARY_DARK,
    SYSTEM_ON_SURFACE_DARK,
    SYSTEM_ON_SURFACE_VARIANT_DARK,
    SYSTEM_ON_TERTIARY_CONTAINER_DARK,
    SYSTEM_ON_TERTIARY_DARK,
    SYSTEM_OUTLINE_DARK,
    SYSTEM_OUTLINE_VARIANT_DARK,
    SYSTEM_PRIMARY_CONTAINER_DARK,
    SYSTEM_PRIMARY_DARK,
    SYSTEM_PRIMARY_FIXED_DIM,
    SYSTEM_SECONDARY_CONTAINER_DARK,
    SYSTEM_SECONDARY_DARK,
    SYSTEM_SECONDARY_FIXED_DIM,
    SYSTEM_SURFACE_CONTAINER_DARK,
    SYSTEM_SURFACE_CONTAINER_HIGH_DARK,
    SYSTEM_SURFACE_CONTAINER_LOW_DARK,
    SYSTEM_TERTIARY_CONTAINER_DARK,
    SYSTEM_TERTIARY_DARK,
    SYSTEM_TERTIARY_FIXED_DIM,
}
