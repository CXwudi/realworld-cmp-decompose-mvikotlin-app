package mikufan.cx.conduit.frontend.logic.component.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Standard contract for MVI ViewModels and presenters.
 * Accepts an [Intent] and exposes a single [State] [StateFlow].
 */
interface MviComponent<in Intent : Any, out State : Any> {
  val state: StateFlow<State>
  fun send(intent: Intent)
}

/**
 * Contract for components or ViewModels that emit one-off [Label] events.
 */
interface LabelEmitter<out Label : Any> {
  val labels: Flow<Label>
}
