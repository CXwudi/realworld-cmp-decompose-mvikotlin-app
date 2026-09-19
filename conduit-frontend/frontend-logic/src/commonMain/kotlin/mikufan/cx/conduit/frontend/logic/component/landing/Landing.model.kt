package mikufan.cx.conduit.frontend.logic.component.landing

import kotlinx.serialization.Serializable

sealed interface LandingPageIntent {
  data class TextChanged(val text: String) : LandingPageIntent
  data object CheckAndMoveToMainPage : LandingPageIntent
}

sealed interface LandingPageLabel {
  data object ToNextPage : LandingPageLabel
  data class Failure(val message: String) : LandingPageLabel
}

@Serializable
data class LandingPageState(
  val url: String,
)