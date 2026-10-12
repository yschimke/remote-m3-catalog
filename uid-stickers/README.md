# `uid-stickers` — the shadow catalog

Remote stickers whose body is a UI builder document: the method of
[m3-catalog's `uid-stickers`](https://github.com/yschimke/m3-catalog/tree/main/uid-stickers) and
[wear-m3-catalog's](https://github.com/yschimke/wear-m3-catalog/tree/main/uid-stickers), on the
Remote Compose vocabulary.

| File | Holds |
| --- | --- |
| `stickers/<name>.uid` | the body: one component definition (`components`) placed once (`roots`) |
| `stickers/<name>.json` | `variants` (cells: builder operations applied before export) and `pairs` (shell → hand-written sticker) |
| `shells/kotlin/…/remote/uid/<File>.kt` | the shells: the hand-written sticker's annotations and name, its knobs, and which cell to call |
| `tools/*.py` | the scripts that write the documents for a family of stickers |

The generated Kotlin is what the builder's Remote exporter writes for a design's components alone
(`InlineRemoteContentExporter.exportComponents`): `internal` `@RemoteComposable` functions whose
parameters are the document's bindings (`label: RemoteString`), with theme roles read from the
`RemoteMaterialTheme` the shell's `RemoteSticker` frame provides. Nothing in the generated body is
catalog code; the vocabulary is Remote Material 3's own records, which ship inside the exporter.

## Running it

Opt-in, and nothing ships from it: with `-PuidStickers=true`, `:remote-catalog` compiles the
generated bodies and the shells beside the stickers they shadow, so one render draws both.

```sh
scripts/agent-gradle.sh --exclusive -PuidStickers=true -PremoteSnapshot= \
  :remote-catalog:composePreviewRender
python3 uid-stickers/compare-local.py
```

`exportComponents` for Remote is not in a released exporter yet, so the generator resolves
`compose-preview-ui-builder-export-jvm:3.110.1-SNAPSHOT` from Maven local: publish
yschimke/compose-ui-builder's branch with `:ui-builder-export:publishToMavenLocal`
(`-PuidExportVersion=` picks another).

`compare-local.py` passes when every capture a hand-written sticker publishes (modes, override
variants) has a pixel-identical capture from its shell.
