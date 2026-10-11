package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.UserProfileSheet
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.agency.AgencyScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.events.EventsScreen
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.SearchScreen
import com.example.ui.screens.messages.ChatDetailScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.BackpackScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.rankings.RankingsScreen
import com.example.ui.screens.reels.ReelsScreen
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
          onOpenRankings = { navController.navigate(Screen.Rankings.route) },
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

      // 12. Rankings Screen
      composable(Screen.Rankings.route) {
        RankingsScreen(
          repository = repository,
          onOpenUserProfile = { user -> inspectedUser = user },
          onOpenRoom = { roomId -> navController.navigate(Screen.VoiceRoom.createRoute(roomId)) }
        )
      }

      // 13. Reels Screen
      composable(Screen.Reels.route) {
        ReelsScreen(
          repository = repository,
          onOpenSearch = { navController.navigate(Screen.Search.route) },
          onOpenNotifications = { navController.navigate(Screen.Notifications.route) },
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

  val GoldActive = Color(0xFFF5BA42)
  val InactiveTab = Color(0xFF9E9EA7)

  Scaffold(
    bottomBar = {
      Surface(
        color = Color(0xFF070A14),
        tonalElevation = 8.dp,
        border = BorderStroke(0.6.dp, Color(0xFF161F33)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(58.dp)
        ) {
          // 1. Home Tab
          BottomTabItem(
            label = "Home",
            isSelected = selectedTab == BottomTab.HOME,
            activeColor = Color(0xFF818CF8),
            inactiveColor = Color(0xFF718096),
            showIndicator = true,
            onClick = { selectedTab = BottomTab.HOME },
            icon = {
              Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = if (selectedTab == BottomTab.HOME) Color.White else Color(0xFF718096),
                modifier = Modifier.size(22.dp)
              )
            }
          )

          // 2. Moment Tab (Sparkles icon like screenshot)
          BottomTabItem(
            label = "Moment",
            isSelected = selectedTab == BottomTab.MOMENT,
            activeColor = Color(0xFF818CF8),
            inactiveColor = Color(0xFF718096),
            showIndicator = true,
            onClick = { selectedTab = BottomTab.MOMENT },
            icon = {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Moment",
                tint = if (selectedTab == BottomTab.MOMENT) Color.White else Color(0xFF718096),
                modifier = Modifier.size(20.dp)
              )
            }
          )

          // 3. Explore Tab (Compass)
          BottomTabItem(
            label = "Explore",
            isSelected = selectedTab == BottomTab.EXPLORE,
            activeColor = Color(0xFF818CF8),
            inactiveColor = Color(0xFF718096),
            showIndicator = true,
            onClick = { selectedTab = BottomTab.EXPLORE },
            icon = {
              Icon(
                imageVector = Icons.Filled.Explore,
                contentDescription = "Explore",
                tint = if (selectedTab == BottomTab.EXPLORE) Color.White else Color(0xFF718096),
                modifier = Modifier.size(22.dp)
              )
            }
          )

          // 4. Chat Tab (ChatBubble with red dot)
          BottomTabItem(
            label = "Chat",
            isSelected = selectedTab == BottomTab.CHAT,
            activeColor = Color(0xFF818CF8),
            inactiveColor = Color(0xFF718096),
            showIndicator = true,
            badgeCount = unreadMessages.coerceAtLeast(1),
            onClick = { selectedTab = BottomTab.CHAT },
            icon = {
              Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = "Chat",
                tint = if (selectedTab == BottomTab.CHAT) Color.White else Color(0xFF718096),
                modifier = Modifier.size(20.dp)
              )
            }
          )

          // 5. Me Tab (Glowing purple avatar highlight matching screenshot)
          BottomTabItem(
            label = "Me",
            isSelected = selectedTab == BottomTab.ME,
            activeColor = Color(0xFFC084FC),
            inactiveColor = Color(0xFF718096),
            showIndicator = true,
            onClick = { selectedTab = BottomTab.ME },
            icon = {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(if (selectedTab == BottomTab.ME) Color(0xFF9333EA) else Color.Transparent)
              ) {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = "Me",
                  tint = if (selectedTab == BottomTab.ME) Color.White else Color(0xFF718096),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          )
        }
      }
    },
    containerColor = Color.Black
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
            onOpenRankings = onOpenRankings,
            onOpenNotifications = onOpenNotifications,
            onOpenEvents = onOpenEvents,
            onOpenAgency = onOpenAgency,
            onOpenReels = { selectedTab = BottomTab.MOMENT }
          )
        }
        BottomTab.MOMENT -> {
          ReelsScreen(
            repository = repository,
            onOpenSearch = onOpenSearch,
            onOpenNotifications = onOpenNotifications,
            onBackClick = { selectedTab = BottomTab.HOME }
          )
        }
        BottomTab.EXPLORE -> {
          ExploreScreen(
            repository = repository,
            onOpenRoom = onOpenRoom,
            onOpenSearch = onOpenSearch,
            onOpenRankings = onOpenRankings,
            onOpenEvents = onOpenEvents,
            onOpenUserProfile = onOpenUserProfile
          )
        }
        BottomTab.CHAT -> {
          MessagesScreen(
            repository = repository,
            onOpenChat = onOpenChat
          )
        }
        BottomTab.ME -> {
          ProfileScreen(
            repository = repository,
            onOpenWallet = onOpenWallet,
            onOpenBackpack = onOpenBackpack,
            onOpenAgency = onOpenAgency,
            onOpenAdminPanel = onOpenAdminPanel,
            onOpenRoom = onOpenRoom,
            onLogout = onLogout
          )
        }
      }
    }
  }
}

@Composable
private fun BottomTabItem(
  label: String,
  isSelected: Boolean,
  activeColor: Color,
  inactiveColor: Color,
  showIndicator: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  icon: @Composable () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 4.dp)
  ) {
    Box {
      icon()
      if (badgeCount > 0) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-3).dp)
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFF43F5E))
        )
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    Text(
      text = label,
      color = if (isSelected) activeColor else inactiveColor,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Golden underline bar when selected and showIndicator is true
    if (isSelected && showIndicator) {
      Box(
        modifier = Modifier
          .width(22.dp)
          .height(2.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(activeColor)
      )
    } else {
      Spacer(modifier = Modifier.height(2.dp))
    }
  }
}
