package mikufan.cx.conduit.frontend.logic.component.main.me

data class AddArticleState(
  val title: String = "",
  val description: String = "",
  val body: String = "",
  val tagList: List<String> = emptyList(),
  val errorMsg: String = "",
)

sealed interface AddArticleIntent {
  data class TitleChanged(val title: String) : AddArticleIntent
  data class DescriptionChanged(val description: String) : AddArticleIntent
  data class BodyChanged(val body: String) : AddArticleIntent
  data class TagListChanged(val tagRawString: String) : AddArticleIntent
  data object Publish : AddArticleIntent
  data object BackWithoutPublish : AddArticleIntent
}

sealed interface AddArticleLabel {
  data object PublishSuccess : AddArticleLabel
  data object BackWithoutPublish : AddArticleLabel
}
