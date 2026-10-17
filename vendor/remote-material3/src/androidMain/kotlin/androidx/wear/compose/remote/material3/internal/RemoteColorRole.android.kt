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

import android.R
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.ui.graphics.Color

internal actual fun themedRemoteColor(role: RemoteColorRole, fallback: Color): RemoteColor =
    RemoteColor.createThemedRemoteColor(
        lightMode = role.lightToken.resId,
        darkMode = role.darkToken.resId,
        lightFallback = fallback,
        darkFallback = fallback,
    )

private val SystemColorToken.resId: Int
    get() =
        when (this) {
            SystemColorToken.SYSTEM_BACKGROUND_DARK -> R.color.system_background_dark
            SystemColorToken.SYSTEM_ERROR_300 -> R.color.system_error_300
            SystemColorToken.SYSTEM_ERROR_CONTAINER_DARK -> R.color.system_error_container_dark
            SystemColorToken.SYSTEM_ERROR_DARK -> R.color.system_error_dark
            SystemColorToken.SYSTEM_ON_BACKGROUND_DARK -> R.color.system_on_background_dark
            SystemColorToken.SYSTEM_ON_ERROR_CONTAINER_DARK ->
                R.color.system_on_error_container_dark
            SystemColorToken.SYSTEM_ON_ERROR_DARK -> R.color.system_on_error_dark
            SystemColorToken.SYSTEM_ON_PRIMARY_CONTAINER_DARK ->
                R.color.system_on_primary_container_dark
            SystemColorToken.SYSTEM_ON_PRIMARY_DARK -> R.color.system_on_primary_dark
            SystemColorToken.SYSTEM_ON_SECONDARY_CONTAINER_DARK ->
                R.color.system_on_secondary_container_dark
            SystemColorToken.SYSTEM_ON_SECONDARY_DARK -> R.color.system_on_secondary_dark
            SystemColorToken.SYSTEM_ON_SURFACE_DARK -> R.color.system_on_surface_dark
            SystemColorToken.SYSTEM_ON_SURFACE_VARIANT_DARK ->
                R.color.system_on_surface_variant_dark
            SystemColorToken.SYSTEM_ON_TERTIARY_CONTAINER_DARK ->
                R.color.system_on_tertiary_container_dark
            SystemColorToken.SYSTEM_ON_TERTIARY_DARK -> R.color.system_on_tertiary_dark
            SystemColorToken.SYSTEM_OUTLINE_DARK -> R.color.system_outline_dark
            SystemColorToken.SYSTEM_OUTLINE_VARIANT_DARK -> R.color.system_outline_variant_dark
            SystemColorToken.SYSTEM_PRIMARY_CONTAINER_DARK -> R.color.system_primary_container_dark
            SystemColorToken.SYSTEM_PRIMARY_DARK -> R.color.system_primary_dark
            SystemColorToken.SYSTEM_PRIMARY_FIXED_DIM -> R.color.system_primary_fixed_dim
            SystemColorToken.SYSTEM_SECONDARY_CONTAINER_DARK ->
                R.color.system_secondary_container_dark
            SystemColorToken.SYSTEM_SECONDARY_DARK -> R.color.system_secondary_dark
            SystemColorToken.SYSTEM_SECONDARY_FIXED_DIM -> R.color.system_secondary_fixed_dim
            SystemColorToken.SYSTEM_SURFACE_CONTAINER_DARK -> R.color.system_surface_container_dark
            SystemColorToken.SYSTEM_SURFACE_CONTAINER_HIGH_DARK ->
                R.color.system_surface_container_high_dark
            SystemColorToken.SYSTEM_SURFACE_CONTAINER_LOW_DARK ->
                R.color.system_surface_container_low_dark
            SystemColorToken.SYSTEM_TERTIARY_CONTAINER_DARK ->
                R.color.system_tertiary_container_dark
            SystemColorToken.SYSTEM_TERTIARY_DARK -> R.color.system_tertiary_dark
            SystemColorToken.SYSTEM_TERTIARY_FIXED_DIM -> R.color.system_tertiary_fixed_dim
        }
