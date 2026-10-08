package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.UserProfileSheet
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.agency.AgencyScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.events.EventsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.SearchScreen
import com.example.ui.screens.messages.ChatDetailScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.BackpackScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.rankings.RankingsScreen
import com.example.ui.screens.room.VoiceRoomScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.*

@Composable
fun LivoNavGraph(
  navController: NavHostController,
  repository: LivoRepository,
  modifier: Modifier = Modifier
) {
  val currentUser by repository.currentUser.collectAsState()
  var inspectedUser by remember { mutableStateOf<UserEntity?>(null) }

  val startDestination = if (currentUser != null) Screen.Main.route else Screen.Login.route

  Box(modifier = modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = startDestination
    ) {
      // 1. Authentication
      composable(Screen.Login.route) {
        LoginScreen(
          repository = repository,
          onLoginSuccess = {
            navController.navigate(Screen.Main.route) {
              popUpTo(Screen.Login.route) { inclusive = true }
            }
          }
        )
      }

      // 2. Main Tabs Container
      composable(Screen.Main.route) {
        MainTabContainer(
          repository = repository,
          onOpenRoom = { roomId -> navController.navigate(Screen.VoiceRoom.createRoute(roomId)) },
          onOpenSearch = { navController.navigate(Screen.Search.route) },
          onOpenWallet = { navController.navigate(Screen.Wallet.route) },
          onOpenRankings = { /* Tab handles this or direct */ },
          onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
          onOpenEvents = { navController.navigate(Screen.Events.route) },
          onOpenAgency = { navController.navigate(Screen.Agency.route) },
          onOpenBackpack = { navController.navigate(Screen.Backpack.route) },
          onOpenAdminPanel = { navController.navigate(Screen.AdminPanel.route) },
          onOpenChat = { peerId -> navController.navigate(Screen.ChatDetail.createRoute(peerId)) },
          onOpenUserProfile = { user -> inspectedUser = user },
          onLogout = {
            repository.logout()
            navController.navigate(Screen.Login.route) {
              popUpTo(Screen.Main.route) { inclusive = true }
            }
          }
        )
      }

      // 3. Search Screen
      composable(Screen.Search.route) {
        SearchScreen(
          repository = repository,
          onOpenRoom = { roomId -> navController.navigate(Screen.VoiceRoom.createRoute(roomId)) },
          onOpenUserProfile = { user -> inspectedUser = user },
          onBackClick = { navController.popBackStack() }
        )
      }

      // 4. Voice Room Screen
      composable(
        route = Screen.VoiceRoom.route,
        arguments = listOf(navArgument("roomId") { type = NavType.StringType })
      ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
        VoiceRoomScreen(
          roomId = roomId,
          repository = repository,
          onLeaveRoom = { navController.popBackStack() },
          onOpenWallet = { navController.navigate(Screen.Wallet.route) },
          onOpenUserProfile = { user -> inspectedUser = user }
        )
      }

      // 5. 1-to-1 Chat Detail Screen
      composable(
        route = Screen.ChatDetail.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
      ) { backStackEntry ->
        val peerUserId = backStackEntry.arguments?.getString("userId") ?: ""
        ChatDetailScreen(
          peerUserId = peerUserId,
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 6. Wallet Screen
      composable(Screen.Wallet.route) {
        WalletScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 7. Backpack Screen
      composable(Screen.Backpack.route) {
        BackpackScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 8. Agency Screen
      composable(Screen.Agency.route) {
        AgencyScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 9. Admin Panel Screen
      composable(Screen.AdminPanel.route) {
        AdminPanelScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 10. Events Screen
      composable(Screen.Events.route) {
        EventsScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }

      // 11. Notifications Screen
      composable(Screen.Notifications.route) {
        NotificationsScreen(
          repository = repository,
          onBackClick = { navController.popBackStack() }
        )
      }
    }

    // Modal user profile sheet
    if (inspectedUser != null) {
      UserProfileSheet(
        user = inspectedUser!!,
        repository = repository,
        onDismiss = { inspectedUser = null },
        onOpenChat = { peerId ->
          inspectedUser = null
          navController.navigate(Screen.ChatDetail.createRoute(peerId))
        }
      )
    }
  }
}

@Composable
fun MainTabContainer(
  repository: LivoRepository,
  onOpenRoom: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenWallet: () -> Unit,
  onOpenRankings: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenEvents: () -> Unit,
  onOpenAgency: () -> Unit,
  onOpenBackpack: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onOpenChat: (String) -> Unit,
  onOpenUserProfile: (UserEntity) -> Unit,
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
  val unreadMessages by repository.totalUnreadMessages.collectAsState(initial = 0)

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
      ) {
        NavigationBarItem(
          selected = selectedTab == BottomTab.HOME,
          onClick = { selectedTab = BottomTab.HOME },
          icon = { Icon(Icons.Default.Explore, contentDescription = "Home") },
          label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LivoViolet,
            selectedTextColor = LivoViolet,
            indicatorColor = DarkSurfaceElevated,
            unselectedIconColor = TextTertiary,
            unselectedTextColor = TextTertiary
          )
        )

        NavigationBarItem(
          selected = selectedTab == BottomTab.RANKINGS,
          onClick = { selectedTab = BottomTab.RANKINGS },
          icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboards") },
          label = { Text("Ranks", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LivoGold,
            selectedTextColor = LivoGold,
            indicatorColor = DarkSurfaceElevated,
            unselectedIconColor = TextTertiary,
            unselectedTextColor = TextTertiary
          )
        )

        NavigationBarItem(
          selected = selectedTab == BottomTab.MESSAGES,
          onClick = { selectedTab = BottomTab.MESSAGES },
          icon = {
            BadgedBox(
              badge = {
                if (unreadMessages > 0) {
                  Badge(containerColor = LivoRose) {
                    Text("$unreadMessages")
                  }
                }
              }
            ) {
              Icon(Icons.Default.ChatBubble, contentDescription = "Messages")
            }
          },
          label = { Text("Messages", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LivoCyan,
            selectedTextColor = LivoCyan,
            indicatorColor = DarkSurfaceElevated,
            unselectedIconColor = TextTertiary,
            unselectedTextColor = TextTertiary
          )
        )

        NavigationBarItem(
          selected = selectedTab == BottomTab.PROFILE,
          onClick = { selectedTab = BottomTab.PROFILE },
          icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
          label = { Text("Me", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = LivoViolet,
            selectedTextColor = LivoViolet,
            indicatorColor = DarkSurfaceElevated,
            unselectedIconColor = TextTertiary,
            unselectedTextColor = TextTertiary
          )
        )
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        BottomTab.HOME -> {
          HomeScreen(
            repository = repository,
            onOpenRoom = onOpenRoom,
            onOpenSearch = onOpenSearch,
            onOpenWallet = onOpenWallet,
            onOpenRankings = { selectedTab = BottomTab.RANKINGS },
            onOpenNotifications = onOpenNotifications,
            onOpenEvents = onOpenEvents,
            onOpenAgency = onOpenAgency
          )
        }
        BottomTab.RANKINGS -> {
          RankingsScreen(
            repository = repository,
            onOpenUserProfile = onOpenUserProfile,
            onOpenRoom = onOpenRoom
          )
        }
        BottomTab.MESSAGES -> {
          MessagesScreen(
            repository = repository,
            onOpenChat = onOpenChat
          )
        }
        BottomTab.PROFILE -> {
          ProfileScreen(
            repository = repository,
            onOpenWallet = onOpenWallet,
            onOpenBackpack = onOpenBackpack,
            onOpenAgency = onOpenAgency,
            onOpenAdminPanel = onOpenAdminPanel,
            onLogout = onLogout
          )
        }
      }
    }
  }
}
