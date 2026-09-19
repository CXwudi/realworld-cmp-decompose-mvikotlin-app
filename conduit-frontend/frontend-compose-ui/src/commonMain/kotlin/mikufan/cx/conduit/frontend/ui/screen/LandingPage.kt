package mikufan.cx.conduit.frontend.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageIntent
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageLabel
import mikufan.cx.conduit.frontend.logic.component.landing.LandingPageState
import mikufan.cx.conduit.frontend.logic.component.landing.LandingViewModel
import mikufan.cx.conduit.frontend.ui.theme.LocalSpace

/**
 * Landing page Composable taking native [LandingViewModel].
 */
@Composable
fun LandingPage(viewModel: LandingViewModel, modifier: Modifier = Modifier) {
  LandingPage(
    state = viewModel.state,
    labels = viewModel.labels,
    onSend = viewModel::send,
    modifier = modifier,
  )
}

/**
 * Plain state/intent/label Composable contract for Landing page.
 */
@Composable
fun LandingPage(
  state: StateFlow<LandingPageState>,
  labels: Flow<LandingPageLabel>,
  onSend: (LandingPageIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentState by state.collectAsState()
  val urlText = remember { derivedStateOf { currentState.url } }

  showErrorAlert(labels = labels)

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacingLarge * 2)
    ) {
      OutlinedTextField(
        value = urlText.value,
        label = { Text("URL") },
        onValueChange = { onSend(LandingPageIntent.TextChanged(it)) },
        singleLine = true,
      )
      Button(onClick = { onSend(LandingPageIntent.CheckAndMoveToMainPage) }) {
        Text("Connect")
      }
    }
  }
}

@Composable
private fun showErrorAlert(labels: Flow<LandingPageLabel>) {
  val errorMsgState = remember { mutableStateOf("") }
  val showErrorAlert by remember { derivedStateOf { errorMsgState.value.isNotBlank() } }

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(labels, lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      labels.collect { label ->
        when (label) {
          is LandingPageLabel.Failure -> {
            errorMsgState.value = label.message
          }
          else -> {}
        }
      }
    }
  }

  if (showErrorAlert) {
    AlertDialog(
      onDismissRequest = { errorMsgState.value = "" },
      shape = MaterialTheme.shapes.large,
      tonalElevation = LocalSpace.current.vertical.spacingLarge,
      icon = {
        Icon(
          imageVector = Icons.Filled.Warning,
          contentDescription = "Error Icon",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(LocalSpace.current.vertical.spacingLarge * 4)
        )
      },
      title = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall
          )
        }
      },
      text = {
        val scrollState = rememberScrollState()
        Text(
          text = errorMsgState.value,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .heightIn(max = 600.dp)
            .verticalScroll(scrollState)
        )
      },
      confirmButton = {
        TextButton(
          onClick = { errorMsgState.value = "" }
        ) {
          Text("OK")
        }
      },
    )
  }
}
