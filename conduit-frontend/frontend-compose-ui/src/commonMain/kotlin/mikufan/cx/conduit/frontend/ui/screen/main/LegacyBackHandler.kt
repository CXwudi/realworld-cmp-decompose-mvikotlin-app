package mikufan.cx.conduit.frontend.ui.screen.main

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.backhandler.BackDispatcher

/**
 * Multiplatform bridge for binding legacy Decompose [BackDispatcher] to system back handling.
 */
@Composable
expect fun LegacyBackHandler(backDispatcher: BackDispatcher)
