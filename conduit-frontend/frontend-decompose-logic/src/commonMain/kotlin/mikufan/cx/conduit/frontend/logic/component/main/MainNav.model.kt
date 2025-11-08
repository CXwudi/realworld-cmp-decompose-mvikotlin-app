package mikufan.cx.conduit.frontend.logic.component.main

import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class MainNavState private constructor(
  val menuItems: List<MainNavMenuItem>,
  /**
   * This index is used for navigation bar to indicate which page is selected
   */
  val pageIndex: Int,
) {
  val currentMenuItem: MainNavMenuItem
    get() = menuItems[pageIndex]

  val isLoggedIn: Boolean = menuItems.any { it is MainNavMenuItem.Favourite }

  internal fun with(
    menuItems: List<MainNavMenuItem> = this.menuItems,
    pageIndex: Int = this.pageIndex
  ): MainNavState {
    return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1))
  }

  companion object {
    fun notLoggedIn(pageIndex: Int = 0): MainNavState {
      val menuItems = listOf(
        MainNavMenuItem.Feed,
        MainNavMenuItem.SignInUp
      )
      return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1))
    }

    fun loggedIn(username: String, pageIndex: Int = 0): MainNavState {
      require(username.isNotBlank()) { "Username cannot be blank" }
      val menuItems = listOf(
        MainNavMenuItem.Feed,
        MainNavMenuItem.Favourite(username),
        MainNavMenuItem.Me
      )
      return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1))
    }
  }
}

@Serializable
sealed interface MainNavMenuItem {
  val menuName: String

  @Serializable
  data object Feed : MainNavMenuItem {
    override val menuName: String = "Feeds"
  }

  @Serializable
  data class Favourite(
    val username: String
  ) : MainNavMenuItem {
    override val menuName: String = "Favourites"
  }

  @Serializable
  data object Me : MainNavMenuItem {
    override val menuName: String = "Me"
  }

  @Serializable
  data object SignInUp : MainNavMenuItem {
    override val menuName: String = "Sign in/up"
  }
}

/**
 * Intents for navigation in the main page
 */
sealed interface MainNavIntent {
  data class MenuIndexSwitching(val targetIndex: Int) : MainNavIntent
}
