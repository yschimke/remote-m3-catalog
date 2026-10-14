#!/usr/bin/env python3
"""Writes the cards' documents: `Card` and `Card/Outlined`.

  python3 uid-stickers/tools/cards.py
"""
from uid import *

# Card: Remote Material 3's plain one-slot card, the kit's row width, its copy in the slot.
write(
    "card",
    document("card", "KitCardRemote", "card", [
        node("card", "remote-m3/remote-card", slots={"content": ["content"]}, modifiers=[width(172)], events=["click"]),
        node("content", "remote-m3/remote-text", {"text": binding("content")}),
    ], {"content": string("Content")}),
    {},
    {"CardRemote": "CardRemote"},
)

# Card/Outlined: the same slot in Remote Material 3's outlined card. Its container is transparent,
# so the label takes the theme's `onSurface`, as the hand-written sticker pins it, and the border is
# the theme's `outline` (Wear's `OutlinedCardTokens`), not the library's lighter default.
write(
    "outlined-card",
    document("outlined-card", "KitOutlinedCardRemote", "card", [
        node("card", "remote-m3/remote-outlined-card", {"borderColor": {"type": "colorToken", "value": "outline"}}, slots={"content": ["content"]}, modifiers=[width(172)], events=["click"]),
        node("content", "remote-m3/remote-text", {"text": binding("content"), "color": {"type": "colorToken", "value": "onSurface"}}),
    ], {"content": string("Content")}),
    {},
    {"OutlinedCardRemote": "OutlinedCardRemote"},
)
