# Calculator widget demo

A local, interactive build of the UI builder design
[`cranky-bandit`](https://ui.coo.ee/ui-builder/remote-m3/cranky-bandit): a calculator in the
Glance Wear **large** widget container (216×124dp), as Remote Compose, with real calculator logic
behind it.

```bash
./gradlew -PcalculatorDemo=true :calculator-demo:run
```

![The widget, from the UI test's captures](../docs/evidence/calculator-demo-widget.png)

It is opt-in (`-PcalculatorDemo=true` in `settings.gradle.kts`), so nothing in the catalog, its
committed records or CI builds it.

## How it is put together

It is shaped the way a Glance Wear widget has to be. The widget document draws and nothing more.
The app process that published it keeps the state and does the arithmetic.

| File | What it is |
| --- | --- |
| `Calculator.kt` | The engine: an immutable, immediate-execution calculator (`2 + 3 ×` shows 5; `=` again repeats). No UI. |
| `CalculatorWidget.kt` | The widget as `@RemoteComposable` content, and `CalculatorSession`, the host-side state. |
| `Main.kt` | A Compose Desktop window that plays the widget in the container's frame. |

The two sides talk only by **name**, through rc-players' `RcHostBridge`:

- **Values.** The display, the pending expression, the operator labels and the highlight colours are
  named values (`display`, `op-add-label`, `op-add-container`, …). A key press changes them without
  building a new document, so the player keeps its state.
- **Actions.** Every key is a named host action (`key-7`, `op-multiply`, `equals`, `more`), handled
  by `CalculatorSession`.
- **Swipe to clear.** The display carries `onTouchDown` and `onTouchUp` host actions whose value is
  the player's touch x (`RemoteContext.FLOAT_TOUCH_POS_X`). The host clears when the two are more
  than 30% of the widget's width apart. The keys sit outside that surface, so a tap is never also a
  swipe.

It plays through `RcEmbeddedPlayer`, the same live player the remote-m3 UI builder uses for its
Browser Preview, with this repository's published `remote-material3` port.

## Layout: from the tile to the widget

The design started from a round calculator tile, with glyphs floating in a 5×4 grid. In a
200×108dp content slot, that left a third of the space empty and gave no visible tap targets.

- **Four columns, not five.** The digit block is 3×4 (`.` `0` `=` on the last row). Every operator
  shares the right-hand column, `−` included, so each key gets a quarter of the width (~50dp)
  rather than a fifth.
- **Pills.** Each key is a tonal pill that fills its cell, so the whole slot is a tap target. `=`
  is filled, and the pending operator lights up until the next one.
- **The display row does three jobs.** The `f(x)` key, the pending operation (`106500 ×`) in
  small type, and the number, right-aligned.
- **No `C` key.** A swipe across the display clears, which frees a cell for `=`.
- **`f(x)` swaps the operator column** to `±` `%` `√` `x²` for one step, then back.

Known limits:

- The container is the Pixel Watch squircle host (8dp padding, 26dp corners). The round host's
  stadium has not been checked.
- On a real watch, a horizontal swipe may be taken by the launcher's carousel before the widget
  sees it.

## Tests

```bash
./gradlew -PcalculatorDemo=true :calculator-demo:test
```

- `CalculatorTest` covers the engine.
- `CalculatorWidgetUiTest` plays the real document and drives it with pointer input: key taps at
  their positions, a swipe across the display, and a tap that must not clear. Its captures land in
  `build/screenshots/`.
