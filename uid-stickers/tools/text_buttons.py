#!/usr/bin/env python3
"""Writes the text button's documents: `TextButton`, one per kit `Style`, its `Size` as cells.

  python3 uid-stickers/tools/text_buttons.py

`RemoteTextButton` with the kit's `MMM` glyph run. Its colours tween across `on`, the 0->1 float the
shell's `toggledRemote()` flips on a tap: `lerp(a, b, @on)` is the player's colour `tween(a, b, on)`,
so a baked capture (on = 0) draws `a` and a live tap moves toward `b`. Enabled is an argument; the
outlined style's border colour is too, because Wear dims it to `onSurface` at 20% when disabled and
a document cannot yet spell a theme role at an opacity.
"""
from uid import *


def role(name):
    return {"type": "colorToken", "value": name}


TRANSPARENT = {"type": "color", "value": "#00000000"}


def tween(a, b):
    return {"type": "expr", "op": "lerp", "args": [a, b, binding("on")]}


STYLES = {
    # Filled is the base: `primary`, darkening to `primaryDim` on a tap.
    "filled": {"containerColor": tween(role("primary"), role("primaryDim")),
               "contentColor": role("onPrimary")},
    "filled-variant": {"containerColor": tween(role("primaryContainer"), role("primaryDim")),
                       "contentColor": role("onPrimaryContainer")},
    "tonal": {"containerColor": tween(role("surfaceContainer"), role("primaryDim")),
              "contentColor": role("onSurface")},
    # The container-less styles carry the content colour with the container, so a light fill
    # never lands under the stock light label.
    "child": {"containerColor": tween(TRANSPARENT, role("primary")),
              "contentColor": tween(role("onSurface"), role("onPrimary"))},
    "outlined": {"containerColor": tween(TRANSPARENT, role("primary")),
                 "contentColor": tween(role("onSurface"), role("onPrimary")),
                 "border": {"type": "int", "value": 1}, "borderColor": binding("borderColor")},
}


def name(style):
    return "KitTextButton" + "".join(w.capitalize() for w in style.split("-")) + "Remote"


for style, colours in STYLES.items():
    props = {"enabled": binding("enabled"), **colours}
    arguments = {"enabled": {"type": "bool", "value": True}, "on": {"type": "float", "value": 0}}
    if "borderColor" in props:
        arguments["borderColor"] = {"type": "color", "value": "#FF938F99"}
    key = f"text-button-{style}"
    write(
        key,
        document(key, name(style), "button", [
            node("button", "remote-m3/remote-text-button", props, slots={"content": ["label"]},
                 events=["click"]),
            node("label", "remote-m3/remote-text", {"text": string("MMM"), "style": enum("labelMedium")}),
        ], arguments),
        {
            "Small": [{"type": "setModifiers", "nodeId": "button", "modifiers": [size(48)]}],
            "Large": [{"type": "setModifiers", "nodeId": "button", "modifiers": [size(60)]},
                      set_property("label", "style", enum("labelLarge"))],
        },
        {"TextRemoteButton": "TextRemoteButton"} if style == "filled" else {},
    )
