package mikufan.cx.conduit.frontend.logic.component.root

import kotlinx.serialization.Serializable

/**
 * Plain serializable route definitions for the root navigation.
 * No Compose or NavKey dependencies in logic module.
 *
 * [Main] route includes [Main.serverUrl] as its entry identity so that switching servers
 * clears the old subtree and cannot consume its saved navigation, while same-server
 * login/profile changes retain the root entry identity.
 */
@Serializable
sealed interface RootRoute {

  @Serializable
  data object Landing : RootRoute

  @Serializable
  data class Main(val serverUrl: String) : RootRoute
}
