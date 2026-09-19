package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailComponent
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailInfo
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailIntent
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailViewModel
import mikufan.cx.conduit.frontend.ui.common.ProfileImage
import mikufan.cx.conduit.frontend.ui.common.layout.PageColumn
import mikufan.cx.conduit.frontend.ui.theme.LocalSpace

/**
 * Display the article detail screen with a native [ArticleDetailViewModel].
 */
@Composable
fun ArticleContent(
  viewModel: ArticleDetailViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.state.collectAsState()

  val titleState = remember { derivedStateOf { state.basicInfo.title } }
  val authorThumbnailState = remember { derivedStateOf { state.basicInfo.authorThumbnail } }
  val authorUsernameState = remember { derivedStateOf { state.basicInfo.authorUsername } }
  val createdAtState = remember { derivedStateOf { state.detailInfo?.createdAt } }
  val bodyState = remember { derivedStateOf { state.detailInfo?.bodyMarkdown ?: "Loading content..." } }

  ArticleContentLayout(
    titleState = titleState,
    authorThumbnailState = authorThumbnailState,
    authorUsernameState = authorUsernameState,
    createdAtState = createdAtState,
    bodyState = bodyState,
    onBackToList = { viewModel.send(ArticleDetailIntent.BackToList) },
    modifier = modifier,
  )
}

/**
 * Display the article detail screen with a legacy [ArticleDetailComponent].
 * Retained for previews and backwards compatibility.
 */
@Composable
fun AnimatedVisibilityScope.ArticleContent(component: ArticleDetailComponent, modifier: Modifier = Modifier) {
  val state by component.state.collectAsState()

  // Use remember and derivedStateOf for fields retrieved from state
  val titleState = remember { derivedStateOf { state.basicInfo.title } }
  val authorThumbnailState = remember { derivedStateOf { state.basicInfo.authorThumbnail } }
  val authorUsernameState = remember { derivedStateOf { state.basicInfo.authorUsername } }
  val createdAtState = remember { derivedStateOf { state.detailInfo?.createdAt } }
  val bodyState = remember { derivedStateOf { state.detailInfo?.bodyMarkdown ?: "Loading content..." } }

  ArticleContentLayout(
    titleState = titleState,
    authorThumbnailState = authorThumbnailState,
    authorUsernameState = authorUsernameState,
    createdAtState = createdAtState,
    bodyState = bodyState,
    onBackToList = { component.send(ArticleDetailIntent.BackToList) },
    modifier = modifier,
  )
}

@Composable
private fun ArticleContentLayout(
  titleState: State<String>,
  authorThumbnailState: State<String?>,
  authorUsernameState: State<String>,
  createdAtState: State<Instant?>,
  bodyState: State<String>,
  onBackToList: () -> Unit,
  modifier: Modifier = Modifier,
) {
  PageColumn(
    modifier = modifier
  ) {
    val horizontalPadding = LocalSpace.current.horizontal.padding

    // Go back button
    IconButton(
      onClick = onBackToList,
      modifier = Modifier
        .align(Alignment.Start)
        .padding(horizontal = horizontalPadding)
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Default.ArrowBack,
        contentDescription = "Go back"
      )
    }

    // Article title
    Text(
      text = titleState.value,
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    )

    // Author information section
    ArticleAuthorInfo(
      authorThumbnailState = authorThumbnailState,
      authorUsernameState = authorUsernameState,
      createdAtState = createdAtState,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    )

    // Horizontal divider (no horizontal padding - full width)
    HorizontalDivider(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.outlineVariant
    )

    // Article body content
    Markdown(
      content = bodyState.value,
      imageTransformer = Coil3ImageTransformerImpl,
      components = markdownComponents(
        codeBlock = highlightedCodeBlock,
        codeFence = highlightedCodeFence,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    )
  }
}

/**
 * Displays article author information with profile image, username, and date.
 */
@Composable
private fun ArticleAuthorInfo(
  authorThumbnailState: State<String?>,
  authorUsernameState: State<String>,
  createdAtState: State<Instant?>,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(LocalSpace.current.horizontal.spacingSmall),
    modifier = modifier
  ) {
    ProfileImage(
      imageUrl = authorThumbnailState.value,
      username = authorUsernameState.value,
      size = 48.dp
    )
    Column {
      Text(
        text = authorUsernameState.value,
        style = MaterialTheme.typography.titleMedium
      )
      createdAtState.value?.let { date ->
        Text(
          text = formatArticleDate(date),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}


/**
 * Formats an Instant to a human-readable date string for articles.
 */
private fun formatArticleDate(instant: Instant): String {
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  val month = when (localDateTime.month.number) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> "Unknown"
  }
  return "$month ${localDateTime.day}, ${localDateTime.year}"
}
