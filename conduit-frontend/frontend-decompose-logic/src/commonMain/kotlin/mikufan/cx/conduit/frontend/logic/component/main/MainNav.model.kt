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
  /**
   * Whether the navigation state has been reconciled with the authoritative UserConfig from KStore.
   * While false, navigation displays do not instantiate child entries.
   */
  val isReady: Boolean = false,
  /**
   * Replacement generation counter distinguishing new tab tree instances.
   * Increments whenever a different tab is selected, ensuring distinct identities during rapid A->B->A.
   */
  val generation: Long = 0L,
) {
  val currentMenuItem: MainNavMenuItem
    get() = menuItems[pageIndex]

  val isLoggedIn: Boolean = menuItems.any { it is MainNavMenuItem.Favourite }

  val currentUsername: String?
    get() = (menuItems.find { it is MainNavMenuItem.Favourite } as? MainNavMenuItem.Favourite)?.username

  val currentChildRoute: MainChildRoute?
    get() {
      if (!isReady) return null
      return when (val item = currentMenuItem) {
        MainNavMenuItem.Feed -> MainChildRoute.Feed(generation)
        is MainNavMenuItem.Favourite -> MainChildRoute.Favourite(item.username, generation)
        MainNavMenuItem.Me -> MainChildRoute.Me(generation)
        MainNavMenuItem.SignInUp -> MainChildRoute.Auth(generation)
      }
    }

  internal fun with(
    menuItems: List<MainNavMenuItem> = this.menuItems,
    pageIndex: Int = this.pageIndex,
    isReady: Boolean = this.isReady,
    generation: Long = this.generation,
  ): MainNavState {
    return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1), isReady, generation)
  }

  companion object {
    fun notLoggedIn(
      pageIndex: Int = 0,
      isReady: Boolean = false,
      generation: Long = 0L,
    ): MainNavState {
      val menuItems = listOf(
        MainNavMenuItem.Feed,
        MainNavMenuItem.SignInUp,
      )
      return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1), isReady, generation)
    }

    fun loggedIn(
      username: String,
      pageIndex: Int = 0,
      isReady: Boolean = false,
      generation: Long = 0L,
    ): MainNavState {
      require(username.isNotBlank()) { "Username cannot be blank" }
      val menuItems = listOf(
        MainNavMenuItem.Feed,
        MainNavMenuItem.Favourite(username),
        MainNavMenuItem.Me,
      )
      return MainNavState(menuItems, pageIndex.coerceIn(0, menuItems.size - 1), isReady, generation)
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
 * Compact serializable tab enumeration for SavedState persistence.
 */
@Serializable
enum class MainNavTab {
  FEED,
  FAVOURITE,
  ME,
  SIGN_IN_UP;

  companion object {
    fun fromMenuItem(item: MainNavMenuItem): MainNavTab = when (item) {
      MainNavMenuItem.Feed -> FEED
      is MainNavMenuItem.Favourite -> FAVOURITE
      MainNavMenuItem.Me -> ME
      MainNavMenuItem.SignInUp -> SIGN_IN_UP
    }
  }
}

/**
 * Compact serializable navigation snapshot persisted in SavedStateHandle.
 */
@Serializable
data class MainNavSavedSnapshot(
  val selectedTab: MainNavTab,
  val accountUsername: String? = null,
  val generation: Long = 0L,
)

/**
 * Child routes rendered by NavDisplay inside the main navigation shell.
 */
@Serializable
sealed interface MainChildRoute {
  val generation: Long

  @Serializable
  data class Feed(override val generation: Long) : MainChildRoute

  @Serializable
  data class Favourite(val username: String, override val generation: Long) : MainChildRoute

  @Serializable
  data class Me(override val generation: Long) : MainChildRoute

  @Serializable
  data class Auth(override val generation: Long) : MainChildRoute
}

/**
 * Intents for navigation in the main page
 */
sealed interface MainNavIntent {
  data class MenuIndexSwitching(val targetIndex: Int) : MainNavIntent
}
