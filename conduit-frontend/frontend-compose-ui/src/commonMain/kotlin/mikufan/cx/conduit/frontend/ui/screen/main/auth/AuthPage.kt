package mikufan.cx.conduit.frontend.ui.screen.main.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageComponent
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageIntent
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageLabel
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageMode
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthPageState
import mikufan.cx.conduit.frontend.logic.component.main.auth.AuthViewModel
import mikufan.cx.conduit.frontend.ui.common.PasswordTextField
import mikufan.cx.conduit.frontend.ui.common.layout.PageColumn
import mikufan.cx.conduit.frontend.ui.theme.LocalSpace

@Composable
fun AuthPage(
  viewModel: AuthViewModel,
  modifier: Modifier = Modifier,
) {
  AuthPageContent(
    stateFlow = viewModel.state,
    labelsFlow = viewModel.labels,
    onSend = viewModel::send,
    modifier = modifier,
  )
}

@Composable
fun AuthPage(
  component: AuthPageComponent,
  modifier: Modifier = Modifier,
) {
  AuthPageContent(
    stateFlow = component.state,
    labelsFlow = component.labels,
    onSend = component::send,
    modifier = modifier,
  )
}

@Composable
private fun AuthPageContent(
  stateFlow: StateFlow<AuthPageState>,
  labelsFlow: Flow<AuthPageLabel>,
  onSend: (AuthPageIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by stateFlow.collectAsState()
  val isRegisterMode = remember { derivedStateOf { state.mode == AuthPageMode.REGISTER } }

  val email = remember { derivedStateOf { state.email } }
  val username = remember { derivedStateOf { state.username } }
  val password = remember { derivedStateOf { state.password } }

  showErrorAlert(labels = labelsFlow)

  val paddingLarge = LocalSpace.current.vertical.paddingLarge

  PageColumn(
    modifier = modifier,
    verticalSpacing = LocalSpace.current.vertical.spacingLarge * 4,
    verticalPadding = LocalSpace.current.vertical.paddingLarge * 4,
    verticalArrangementAlignment = Alignment.CenterVertically
  ) {
    val horizontalPadding = LocalSpace.current.horizontal.padding

    EmailTextField(
      emailProvider = email,
      onEmailChanged = { onSend(AuthPageIntent.EmailChanged(it)) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    )

    AnimatedVisibility(
      visible = isRegisterMode.value,
    ) {
      OutlinedTextField(
        value = username.value,
        onValueChange = { onSend(AuthPageIntent.UsernameChanged(it)) },
        label = { Text("Username") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = horizontalPadding),
      )
    }

    PasswordTextField(
      passwordProvider = password,
      onPasswordChanged = { onSend(AuthPageIntent.PasswordChanged(it)) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding),
    )

    Spacer(
      modifier = Modifier.height(paddingLarge * 2)
    )

    Button(
      onClick = { onSend(AuthPageIntent.AuthAction) },
    ) {
      AnimatedText(
        isRegisterMode = isRegisterMode,
        textForLoginMode = "Login",
        textForRegisterMode = "Register"
      )
    }

    SwitchModeRow(
      isRegisterMode = isRegisterMode,
      onChangeUrlClick = { onSend(AuthPageIntent.BackToLanding) },
      onSwitchModeClick = { onSend(AuthPageIntent.SwitchMode) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)
    )
  }
}

@Composable
private fun EmailTextField(
  emailProvider: State<String>,
  onEmailChanged: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = emailProvider.value,
    onValueChange = onEmailChanged,
    label = { Text("Email") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
private fun SwitchModeRow(
  isRegisterMode: State<Boolean>,
  onChangeUrlClick: () -> Unit,
  onSwitchModeClick: () -> Unit,
  modifier: Modifier = Modifier,
) = Row(
  modifier = modifier,
  horizontalArrangement = Arrangement.SpaceBetween,
) {
  TextButton(onClick = onChangeUrlClick) {
    Text("Change URL")
  }
  TextButton(onClick = onSwitchModeClick) {
    AnimatedText(
      isRegisterMode = isRegisterMode,
      textForLoginMode = "To Register",
      textForRegisterMode = "To Login"
    )
  }
}

@Composable
private fun AnimatedText(
  isRegisterMode: State<Boolean>,
  textForLoginMode: String,
  textForRegisterMode: String,
  modifier: Modifier = Modifier,
) {
  val text =
    remember { derivedStateOf { if (isRegisterMode.value) textForRegisterMode else textForLoginMode } }
  AnimatedContent(
    targetState = text.value,
  ) {
    Text(it, modifier = modifier)
  }
}

@Composable
private fun showErrorAlert(labels: Flow<AuthPageLabel>) {
  val errorMsgState = rememberSaveable { mutableStateOf("") }
  val showErrorAlert by remember { derivedStateOf { errorMsgState.value.isNotBlank() } }

  LaunchedEffect(labels) {
    labels.collect { label ->
      when (label) {
        is AuthPageLabel.Failure -> {
          errorMsgState.value = label.message
        }
        else -> {}
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
