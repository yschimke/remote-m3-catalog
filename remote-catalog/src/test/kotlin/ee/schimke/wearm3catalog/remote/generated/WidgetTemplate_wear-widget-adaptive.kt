// Generated from a Compose UI builder design. Do not edit by hand.
@file:Suppress("RestrictedApi")

package ee.schimke.wearm3catalog.remote.generated.wearwidgetadaptive

import android.content.Context
import androidx.compose.remote.creation.compose.action.lambdaAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.tooling.preview.RectangularLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.RectangularSmallWidgetPreviewParams
import androidx.glance.wear.tooling.preview.RoundLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.RoundSmallWidgetPreviewParams
import androidx.glance.wear.tooling.preview.SquircleLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.SquircleSmallWidgetPreviewParams
import androidx.glance.wear.tooling.preview.WearWidgetPreview
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteColorScheme
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText

@RemoteComposable
@Composable
fun WearWidgetContent(large: Boolean) {
    RemoteMaterialTheme {
        if (large) {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize(),
                verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
            ) {
                RemoteColumn(
                    modifier = RemoteModifier.weight(1.rf),
                    verticalArrangement = RemoteArrangement.spacedBy(2.rdp),
                ) {
                    RemoteText(
                        text = "Next meeting".rs,
                        color = RemoteMaterialTheme.colorScheme.onSurface,
                        style = RemoteMaterialTheme.typography.titleMedium,
                    )
                    RemoteText(
                        text = "10:30 · Room 4".rs,
                        color = RemoteMaterialTheme.colorScheme.onSurfaceVariant,
                        style = RemoteMaterialTheme.typography.bodyMedium,
                    )
                }
                RemoteButton(onClick = lambdaAction {}) {
                    RemoteText(text = "Join".rs)
                }
            }
        } else {
            RemoteRow(
                modifier = RemoteModifier.fillMaxSize(),
                horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                RemoteColumn(
                    modifier = RemoteModifier.weight(1.rf),
                    verticalArrangement = RemoteArrangement.spacedBy(2.rdp),
                ) {
                    RemoteText(
                        text = "Next meeting".rs,
                        color = RemoteMaterialTheme.colorScheme.onSurface,
                        style = RemoteMaterialTheme.typography.titleMedium,
                    )
                }
                RemoteButton(onClick = lambdaAction {}) {
                    RemoteText(text = "Join".rs)
                }
            }
        }
    }
}

class WearWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData {
        val colorScheme = RemoteColorScheme()
        val background =
            WearWidgetBrush.color(colorScheme.surfaceContainer)
        return WearWidgetDocument(background = background) {
            WearWidgetContent(large = params.containerType == ContainerInfo.CONTAINER_TYPE_LARGE)
        }
    }
}

@Preview(name = "Rectangular Large Preview")
@Composable
fun WearWidgetRectangularLargePreview() =
    WearWidgetPreview(
        WearWidget(),
        RectangularLargeWidgetPreviewParams().values.maxBy { it.widthDp },
    )

@Preview(name = "Squircle Large Preview")
@Composable
fun WearWidgetSquircleLargePreview() =
    WearWidgetPreview(
        WearWidget(),
        SquircleLargeWidgetPreviewParams().values.maxBy { it.widthDp },
    )

@Preview(name = "Round Large Preview")
@Composable
fun WearWidgetRoundLargePreview() =
    WearWidgetPreview(
        WearWidget(),
        RoundLargeWidgetPreviewParams().values.maxBy { it.widthDp },
    )

@Preview(name = "Rectangular Small Preview")
@Composable
fun WearWidgetRectangularSmallPreview() =
    WearWidgetPreview(
        WearWidget(),
        RectangularSmallWidgetPreviewParams().values.maxBy { it.widthDp },
    )

@Preview(name = "Squircle Small Preview")
@Composable
fun WearWidgetSquircleSmallPreview() =
    WearWidgetPreview(
        WearWidget(),
        SquircleSmallWidgetPreviewParams().values.maxBy { it.widthDp },
    )

@Preview(name = "Round Small Preview")
@Composable
fun WearWidgetRoundSmallPreview() =
    WearWidgetPreview(
        WearWidget(),
        RoundSmallWidgetPreviewParams().values.maxBy { it.widthDp },
    )
