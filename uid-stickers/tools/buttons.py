#!/usr/bin/env python3
"""Writes the buttons' documents: `Button/Filled`, `/Tonal`, `/FilledVariant`, `/Child`, `/Outlined`.

  python3 uid-stickers/tools/buttons.py

A label-only button is `RemoteButton` at the kit's row width over `buttonSizeModifier()`'s minimums
(12 x 52dp); its `Alignment=Left` is a cell. The kit's `Icon=Yes` column is the label overload, with
a leading `Add` icon and the second label the kit couples to it; its icon sizes are cells. Enabled is
an argument either way, read from the shell's knob.

The emphases are colour roles through `RemoteButtonDefaults.buttonColors`, transcribed from the
released lane's palette (`RemoteButtonPalette.kt`). The outlined style adds a 1dp border whose colour
is an argument: Wear dims it to `onSurface` at 20% when disabled, and a document cannot yet spell a
theme role at an opacity, so the shell supplies `kitOutlinedBorderColor(enabled)`.
"""
from uid import *

MIN_SIZE = [{"type": "widthIn", "minDp": 12}, {"type": "heightIn", "minDp": 52}]


def button_documents(key, name, sticker, extra=None):
    props = {"enabled": binding("enabled"), **(extra or {})}
    arguments = {"label": string("Primary label"), "enabled": {"type": "bool", "value": True}}
    if "borderColor" in props:
        arguments["borderColor"] = {"type": "color", "value": "#FF938F99"}
    write(
        key,
        document(key, name, "button", [
            node("button", "remote-m3/remote-button", props, slots={"content": ["label"]},
                 modifiers=MIN_SIZE + [width(172)], events=["click"]),
            node("label", "remote-m3/remote-text", {"text": binding("label")}),
        ], arguments),
        {"Left": [
            set_property("label", "textAlign", enum("start")),
            {"type": "setModifiers", "nodeId": "label", "modifiers": [{"type": "fillMaxWidth"}]},
        ]},
        {sticker: sticker},
    )
    write(
        f"{key}-icon",
        document(f"{key}-icon", f"{name}Icon", "button", [
            node("button", "remote-m3/remote-label-button", props,
                 slots={"label": ["label"], "secondaryLabel": ["secondary"], "icon": ["icon"]},
                 events=["click"]),
            node("label", "remote-m3/remote-text", {"text": binding("label")}),
            node("secondary", "remote-m3/remote-text", {"text": string("Secondary label")}),
            node("icon", "remote-m3/remote-icon", {"imageVector": enum("add")}, modifiers=[size(26)]),
        ], arguments),
        {
            "Large": [{"type": "setModifiers", "nodeId": "icon", "modifiers": [size(32)]}],
            "Extra large": [{"type": "setModifiers", "nodeId": "icon", "modifiers": [size(36)]}],
        },
        {},
    )


def role(name):
    return {"type": "colorToken", "value": name}


def palette(container, content, secondary, icon):
    return {
        "containerColor": container,
        "contentColor": role(content),
        "secondaryContentColor": role(secondary),
        "iconColor": role(icon),
    }


TRANSPARENT = {"type": "color", "value": "transparent"}

button_documents("button-filled", "KitFilledButtonRemote", "FilledRemoteButton")
button_documents("button-tonal", "KitTonalButtonRemote", "TonalRemoteButton",
                 palette(role("surfaceContainer"), "onSurface", "onSurfaceVariant", "primary"))
button_documents("button-filled-variant", "KitFilledVariantButtonRemote", "FilledVariantRemoteButton",
                 palette(role("primaryContainer"), "onPrimaryContainer", "onPrimaryContainer",
                         "onPrimaryContainer"))
button_documents("button-child", "KitChildButtonRemote", "ChildRemoteButton",
                 palette(TRANSPARENT, "onSurface", "onSurfaceVariant", "primary"))
button_documents("button-outlined", "KitOutlinedButtonRemote", "OutlinedRemoteButton",
                 {**palette(TRANSPARENT, "onSurface", "onSurfaceVariant", "primary"),
                  "border": {"type": "int", "value": 1}, "borderColor": binding("borderColor")})
