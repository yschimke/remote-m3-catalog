package ee.schimke.wearm3catalog.remote.probe

import androidx.compose.remote.creation.compose.action.*
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.MutableRemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ee.schimke.wearm3catalog.remote.*

/**
 * What a design's ACTIONS have to become, compiled rather than guessed.
 *
 * Every action a UI-builder document can carry is a state write — `set`, `select` and `setText`
 * assign, `toggle` negates, `selectOrClear` assigns or clears — and each names a variable declared
 * in the document's `stateVariables`. Nothing in the model calls out to the host, so `hostAction`
 * maps to nothing that exists and every one of them is a `valueChangeAction`.
 *
 * Sibling of `RemoteValueVocabularyProbe`, and the same trick: no test method, no assertion. It is
 * compiled by `compileDebugUnitTestKotlin` against the creation DSL, and compiling is the
 * assertion. Written in snapshot 16942710's spelling (`valueChangeAction`, a remembered
 * `MutableRemote*`); on the released lane `valueChangeAction` is the catalog's own bridge in
 * `RemoteLaneCompat.kt`, which is what the second star import reaches.
 *
 * | Document action              | Remote                            |
 * |------------------------------|-----------------------------------|
 * | `set` / `select` / `setText` | `valueChangeAction(v, <literal>)` |
 * | `toggle` on a boolean        | `valueChangeAction(b, !b)`        |
 * | `selectOrClear`              | **nothing** — see below           |
 *
 * All four state types have a mutable remote form, which is what a generated widget declares for
 * each of the document's variables, and `valueChangeAction` takes both a literal and an expression
 * reference — `(i + 1).createReference()` is how the catalog's own counter sticker writes one.
 *
 * `selectOrClear` is the exception and it is a refusal rather than a gap in this file:
 * `valueChangeAction(s, null)` does not compile, because the second parameter is a non-null
 * `RemoteState<T>`. A design that clears a selection and one that sets it to a sentinel are
 * different designs, so the emitter refuses it by name instead of picking one.
 */
@RemoteComposable
@Composable
fun remoteActionVocabularyProbe() {
  val i = remember { MutableRemoteInt(0) }
  val f = remember { MutableRemoteFloat(0f) }
  val b = remember { MutableRemoteBoolean(false) }
  val s = remember { MutableRemoteString("") }

  // `set` / `select` / `setText`, one per state type.
  valueChangeAction(i, 5.ri)
  valueChangeAction(f, 0.5f.rf)
  valueChangeAction(b, true.rb)
  valueChangeAction(s, "x".rs)

  // `toggle`, and the expression form the catalog's own stickers use for a counted or animated
  // value — both are `valueChangeAction`, so a document that grows arithmetic needs no new
  // mechanism.
  valueChangeAction(b, !b)
  valueChangeAction(f, (1f.rf - f).createReference())
  valueChangeAction(i, (i + 1).createReference())
}
