#!/usr/bin/env python3
"""Writes the circular progress indicator's documents.

  python3 uid-stickers/tools/progress.py

The determinate ring takes its progress, enabled state and stroke width as arguments: all three are
the shell's knobs, and the stroke width is Wear's screen-size token, which no literal matches on
every display. The indeterminate ring is the other overload, with its own component.
"""
from uid import *


def fill():
    return {"type": "fillMaxSize"}


write(
    "circular-progress",
    document("circular-progress", "KitCircularProgressRemote", "ring", [
        node("ring", "remote-m3/remote-circular-progress-indicator", {
            "progress": binding("progress"),
            "enabled": binding("enabled"),
            "strokeWidth": binding("strokeWidth"),
        }, modifiers=[fill()]),
    ], {
        "progress": {"type": "float", "value": 0.6},
        "enabled": {"type": "bool", "value": True},
        "strokeWidth": {"type": "int", "value": 8},
    }),
    {},
    {"CircularProgressRemote": "CircularProgressRemote"},
)

write(
    "circular-progress-indeterminate",
    document("circular-progress-indeterminate", "KitIndeterminateCircularProgressRemote", "ring", [
        node("ring", "remote-m3/remote-circular-progress-indicator", {
            "indeterminate": {"type": "bool", "value": True},
            "strokeWidth": binding("strokeWidth"),
        }, modifiers=[fill()]),
    ], {
        "strokeWidth": {"type": "int", "value": 8},
    }),
    {},
    {},
)
