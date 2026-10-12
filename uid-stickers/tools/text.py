#!/usr/bin/env python3
"""Writes the list headers' documents: `ListHeader` and `ListSubHeader`.

Both are drawn from `RemoteText` exactly as the hand-written stickers draw them: a row or box of
the kit's width, the Wear header tokens' minimum height and content padding, and the label at a
named type role and content colour from `RemoteMaterialTheme`.

  python3 uid-stickers/tools/text.py
"""
from uid import *

HEADER = [width(180), {"type": "heightIn", "minDp": 48}]


def padding(start, top, end, bottom):
    return {"type": "padding", "startDp": start, "topDp": top, "endDp": end, "bottomDp": bottom}


# ListHeader: a centred `titleMedium` label over `onBackground`; `Alignment=Left` is a cell.
write(
    "list-header",
    document("list-header", "KitListHeaderRemote", "frame", [
        node("frame", "layout/box", {"contentAlignment": enum("center")}, slots={"children": ["title"]},
             modifiers=HEADER + [padding(14, 16, 14, 12)]),
        node("title", "remote-m3/remote-text", {
            "text": binding("label"), "style": enum("titleMedium"), "color": enum("onBackground"),
            "textAlign": enum("center"),
        }, modifiers=[{"type": "fillMaxWidth"}]),
    ], {"label": string("Title")}),
    {"Left": [set_property("title", "textAlign", enum("start"))]},
    {"ListHeaderRemote": "ListHeaderRemote"},
)

# ListSubHeader: a start-aligned `titleSmall` label over `onSurface` in a row; the kit's `Icon=Yes`
# leads with a 24dp `Add` icon and 6dp of space.
icon = node("icon", "remote-m3/remote-icon", {"imageVector": enum("add"), "tint": enum("onSurface")},
            modifiers=[size(24), padding(0, 0, 6, 0)])
write(
    "list-sub-header",
    document("list-sub-header", "KitListSubHeaderRemote", "row", [
        node("row", "layout/row", {"verticalAlignment": enum("center")}, slots={"children": ["label"]},
             modifiers=HEADER + [padding(14, 16, 14, 8)]),
        node("label", "remote-m3/remote-text", {
            "text": binding("label"), "style": enum("titleSmall"), "color": enum("onSurface"),
            "textAlign": enum("start"),
        }, modifiers=[{"type": "fillMaxWidth"}]),
    ], {"label": string("Subtitle")}),
    {
        "Centred": [set_property("label", "textAlign", enum("center"))],
        "Icon centred": [
            {"type": "deleteNode", "nodeId": "label"},
            add_node("row", "children", icon),
            add_node("row", "children", node("label", "remote-m3/remote-text", {
                "text": binding("label"), "style": enum("titleSmall"), "color": enum("onSurface"),
                "textAlign": enum("center"),
            }, modifiers=[{"type": "fillMaxWidth"}])),
        ],
        "Icon": [
            {"type": "deleteNode", "nodeId": "label"},
            add_node("row", "children", icon),
            add_node("row", "children", node("label", "remote-m3/remote-text", {
                "text": binding("label"), "style": enum("titleSmall"), "color": enum("onSurface"),
                "textAlign": enum("start"),
            }, modifiers=[{"type": "fillMaxWidth"}])),
        ],
    },
    {"ListSubHeaderRemote": "ListSubHeaderRemote"},
)
