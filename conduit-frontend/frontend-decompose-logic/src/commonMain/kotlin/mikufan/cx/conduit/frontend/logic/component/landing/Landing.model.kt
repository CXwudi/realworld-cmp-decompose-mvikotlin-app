package mikufan.cx.conduit.frontend.logic.component.landing

import kotlinx.serialization.Serializable

sealed interface LandingPageIntent {
  data class TextChanged(val text: String) : LandingPageIntent
  data object CheckAndMoveToMainPage : LandingPageIntent
}

sealed interface LandingPageLabel {
  data object ToNextPage : LandingPageLabel
}

@Serializable
data class LandingPageState(
  val url: String,
  val errorMsg: String,
)