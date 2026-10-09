// Generated from a Compose UI builder design. Do not edit by hand.
@file:Suppress("RestrictedApi")

package ee.schimke.remotewidgets.generated

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ee.schimke.remotewidgets.AdaptiveLayout
import ee.schimke.remotewidgets.WidgetTitle

class AdaptiveWidget : RemoteComposeWidget() {
    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
            AdaptiveLayout(
                modifier = RemoteModifier.fillMaxSize(),
                mediumSize = "4x1",
                compact = {
                    WidgetTitle(text = "Compact")
                },
                medium = {
                    WidgetTitle(text = "Medium")
                },
            )
        }
    }
}

@Preview(name = "4x1", widthDp = 276, heightDp = 102)
@Composable
fun AdaptiveWidgetPreview() =
    RemoteContentPreview(profile = RcPlatformProfiles.WIDGETS_V6) {
        AdaptiveWidget().Content(LocalContext.current, 0)
    }
