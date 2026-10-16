package ee.schimke.wearm3catalog.remoteuibuilder

internal actual val defaultPreviewEngine: PreviewEngine =
  PreviewEngine.entries.firstOrNull { it.id == System.getProperty("remoteM3.previewEngine") }
    ?: PreviewEngine.Capture
