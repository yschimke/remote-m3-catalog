// Generated from a Compose UI builder design. Do not edit by hand.
@file:Suppress("RestrictedApi")

package ee.schimke.remotewidgets.generated

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ee.schimke.remotewidgets.WidgetButton

class CounterWidget : RemoteComposeWidget() {
    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color(0xFFF3EDF7).rc)) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxSize(),
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                WidgetButton(text = "-", modifier = RemoteModifier.weight(1.rf))
                RemoteText(text = "0".rs, color = Color(0xFF1D1B20).rc, fontSize = 48.rsp)
                WidgetButton(text = "+", modifier = RemoteModifier.weight(1.rf))
            }
        }
    }
}

@Preview(name = "3x2", widthDp = 203, heightDp = 220)
@Composable
fun CounterWidgetPreview() =
    RemoteContentPreview(profile = RcPlatformProfiles.WIDGETS_V6) {
        CounterWidget().Content(LocalContext.current, 0)
    }
