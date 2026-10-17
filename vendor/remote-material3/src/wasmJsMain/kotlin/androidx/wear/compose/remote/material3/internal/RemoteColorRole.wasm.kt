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

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColor.Companion.createNamedRemoteColor
import androidx.compose.ui.graphics.Color

// Android system colors are resolved by the Android player only, and a capture on this target has
// no profile that reports themed-color support, so the role falls back to its named color.
internal actual fun themedRemoteColor(role: RemoteColorRole, fallback: Color): RemoteColor =
    createNamedRemoteColor(role.name, fallback)
