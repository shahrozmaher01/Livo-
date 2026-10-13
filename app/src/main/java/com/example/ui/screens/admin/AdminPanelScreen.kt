package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Exactly 21 specified navigation items in the exact requested order
enum class OwnerNavSection(val index: Int, val title: String, val icon: ImageVector) {
  OFFICIAL_PANEL(0, "Official Panel", Icons.Default.VerifiedUser),
  MANAGER_PANEL(1, "Manager Panel", Icons.Default.SupervisorAccount),
  SUPER_ADMIN_PANEL(2, "Super Admin Panel", Icons.Default.Security),
  BD_PANEL(3, "BD Panel", Icons.Default.BusinessCenter),
  AGENCY_PANEL(4, "Agency Panel", Icons.Default.Groups),
  HOSTING_PANEL(5, "Hosting Panel", Icons.Default.Mic),
  COIN_RESELLER_PANEL(6, "Coin Reseller Panel", Icons.Default.MonetizationOn),
  SUPER_COIN_RESELLER_PANEL(7, "Super Coin Reseller Panel", Icons.Default.AccountBalance),
  PANEL_BUILDER(8, "Panel Builder", Icons.Default.Build),
  USERS_MANAGEMENT(9, "Users Management", Icons.Default.People),
  ROOM_MANAGEMENT(10, "Room Management", Icons.Default.MeetingRoom),
  COIN_DIAMOND_MANAGEMENT(11, "Coin and Diamond Management", Icons.Default.Savings),
  GIFT_MANAGEMENT(12, "Gift Management", Icons.Default.CardGiftcard),
  LUCKY_GIFT_MANAGEMENT(13, "Lucky Gift Management", Icons.Default.Stars),
  FRAME_MANAGEMENT(14, "Frame Management", Icons.Default.FilterFrames),
  RANKING_MANAGEMENT(15, "Ranking Management", Icons.Default.EmojiEvents),
  REPORTS_ANALYTICS(16, "Reports and Analytics", Icons.Default.BarChart),
  NOTIFICATIONS(17, "Notifications", Icons.Default.Notifications),
  SECURITY_PERMISSIONS(18, "Security and Permissions", Icons.Default.Lock),
  SYSTEM_SETTINGS(19, "System Settings", Icons.Default.Settings),
  AUDIT_LOGS(20, "Audit Logs", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()

  // Database streams
  val panelConfigs by repository.observeAllPanelConfigs().collectAsState(initial = emptyList())
  val panelTemplates by repository.observeAllPanelTemplates().collectAsState(initial = emptyList())
  val allAssignments by repository.observeAllPanelAssignments().collectAsState(initial = emptyList())
  val currencySettings by repository.observeCurrencySettings().collectAsState(initial = null)
  val auditLogs by repository.observeAuditLogs().collectAsState(initial = emptyList())
  val reports by repository.observeReports().collectAsState(initial = emptyList())
  val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
  val allAgencies by repository.observeAgencies().collectAsState(initial = emptyList())

  var currentSection by remember { mutableStateOf(OwnerNavSection.OFFICIAL_PANEL) }

  // Dark Theme Palette matching LIVO Brand
  val DarkBg = Color(0xFF060914)
  val SidebarBg = Color(0xFF0B1021)
  val CardBg = Color(0xFF10172D)
  val CardBorder = Color(0xFF1E2A4A)
  val AccentPurple = Color(0xFF8B5CF6)
  val AccentPink = Color(0xFFEC4899)
  val GoldColor = Color(0xFFFBBF24)

  // Drawer / Mobile Nav state
  val drawerState = rememberDrawerState(DrawerValue.Closed)

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = SidebarBg,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
        ) {
          // Sidebar Brand Header
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(AccentPurple, AccentPink)))
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("LIVO OWNER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
              Text("Ultimate Control Panel", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
          }

          HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)

          // 21 Navigation Items in Exact Order
          LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.weight(1f)
          ) {
            items(OwnerNavSection.values()) { section ->
              val isSelected = currentSection == section
              val isCoreTop8 = section.index < 8
              val isBuilder = section == OwnerNavSection.PANEL_BUILDER

              NavigationDrawerItem(
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "${section.index + 1}. ${section.title}",
                      fontSize = 12.5.sp,
                      fontWeight = if (isSelected || isCoreTop8) FontWeight.Bold else FontWeight.Medium,
                      color = when {
                        isSelected -> Color.White
                        isCoreTop8 -> Color(0xFFE2E8F0)
                        else -> Color(0xFF94A3B8)
                      },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                },
                icon = {
                  Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = when {
                      isSelected -> AccentPink
                      isCoreTop8 -> GoldColor
                      isBuilder -> Color(0xFF38BDF8)
                      else -> Color(0xFF64748B)
                    },
                    modifier = Modifier.size(18.dp)
                  )
                },
                selected = isSelected,
                onClick = {
                  currentSection = section
                  coroutineScope.launch { drawerState.close() }
                },
                colors = NavigationDrawerItemDefaults.colors(
                  selectedContainerColor = AccentPurple.copy(alpha = 0.25f),
                  unselectedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .padding(horizontal = 10.dp, vertical = 2.dp)
                  .height(44.dp)
              )
            }
          }

          // Active User Status Footer
          HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            AsyncImage(
              model = currentUser?.avatarUrl,
              contentDescription = null,
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.5.dp, GoldColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(currentUser?.nickname ?: "Owner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text("Role: ${currentUser?.role?.displayName} (Full Rights)", color = GoldColor, fontSize = 10.sp)
            }
          }
        }
      }
    }
  ) {
    Scaffold(
      topBar = {
        Surface(
          color = SidebarBg,
          border = BorderStroke(0.8.dp, CardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                  Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu", tint = GoldColor)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                  Text(
                    text = "${currentSection.index + 1}. ${currentSection.title}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                  Text("LIVO Owner Central Executive", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.5f))
              ) {
                Text(
                  text = "OWNER SECURE",
                  color = Color(0xFFC084FC),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            // Horizontal Top Quick Bar for First 8 Core Panels + Builder
            ScrollableTabRow(
              selectedTabIndex = currentSection.index,
              containerColor = SidebarBg,
              contentColor = Color.White,
              edgePadding = 8.dp,
              divider = {}
            ) {
              OwnerNavSection.values().forEach { sec ->
                Tab(
                  selected = currentSection == sec,
                  onClick = { currentSection = sec },
                  text = {
                    Text(
                      text = "${sec.index + 1}. ${sec.title}",
                      fontSize = 11.5.sp,
                      fontWeight = if (currentSection == sec) FontWeight.Bold else FontWeight.Normal,
                      color = if (currentSection == sec) Color(0xFFFDE047) else Color(0xFF94A3B8)
                    )
                  }
                )
              }
            }
          }
        }
      },
      containerColor = DarkBg
    ) { paddingVals ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingVals)
      ) {
        when (currentSection) {
          // 1. Official Panel Distribution (Primary Section)
          OwnerNavSection.OFFICIAL_PANEL -> {
            OfficialPanelDistributionView(
              repository = repository,
              allUsers = allUsers,
              allAssignments = allAssignments.filter { it.panelType == PanelType.OFFICIAL.name }
            )
          }

          // 2. Manager Panel
          OwnerNavSection.MANAGER_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.MANAGER,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.MANAGER.name },
              assignments = allAssignments.filter { it.panelType == PanelType.MANAGER.name },
              repository = repository
            )
          }

          // 3. Super Admin Panel
          OwnerNavSection.SUPER_ADMIN_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.SUPER_ADMIN,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.SUPER_ADMIN.name },
              assignments = allAssignments.filter { it.panelType == PanelType.SUPER_ADMIN.name },
              repository = repository,
              isSuperAdminView = true
            )
          }

          // 4. BD Panel
          OwnerNavSection.BD_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.BD,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.BD.name },
              assignments = allAssignments.filter { it.panelType == PanelType.BD.name },
              repository = repository
            )
          }

          // 5. Agency Panel
          OwnerNavSection.AGENCY_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.AGENCY,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.AGENCY.name },
              assignments = allAssignments.filter { it.panelType == PanelType.AGENCY.name },
              repository = repository
            )
          }

          // 6. Hosting Panel
          OwnerNavSection.HOSTING_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.HOSTING,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.HOSTING.name },
              assignments = allAssignments.filter { it.panelType == PanelType.HOSTING.name },
              repository = repository
            )
          }

          // 7. Coin Reseller Panel
          OwnerNavSection.COIN_RESELLER_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.COIN_RESELLER,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.COIN_RESELLER.name },
              assignments = allAssignments.filter { it.panelType == PanelType.COIN_RESELLER.name },
              repository = repository
            )
          }

          // 8. Super Coin Reseller Panel
          OwnerNavSection.SUPER_COIN_RESELLER_PANEL -> {
            RolePanelConfigView(
              panelType = PanelType.SUPER_COIN_RESELLER,
              panelConfig = panelConfigs.firstOrNull { it.panelType == PanelType.SUPER_COIN_RESELLER.name },
              assignments = allAssignments.filter { it.panelType == PanelType.SUPER_COIN_RESELLER.name },
              repository = repository
            )
          }

          // 9. Dynamic Panel Builder (Most Important Feature)
          OwnerNavSection.PANEL_BUILDER -> {
            DynamicPanelBuilderView(
              repository = repository,
              panelConfigs = panelConfigs,
              templates = panelTemplates
            )
          }

          // 10. Users Management
          OwnerNavSection.USERS_MANAGEMENT -> {
            UsersManagementView(repository = repository, allUsers = allUsers)
          }

          // 11. Room Management
          OwnerNavSection.ROOM_MANAGEMENT -> {
            RoomManagementAdminView(repository = repository)
          }

          // 12. Coin and Diamond Management
          OwnerNavSection.COIN_DIAMOND_MANAGEMENT -> {
            CurrencyManagementAdminView(
              repository = repository,
              currentSettings = currencySettings
            )
          }

          // 13. Gift Management
          OwnerNavSection.GIFT_MANAGEMENT -> {
            GiftManagementAdminView(repository = repository)
          }

          // 14. Lucky Gift Management
          OwnerNavSection.LUCKY_GIFT_MANAGEMENT -> {
            LuckyGiftManagementAdminView(repository = repository)
          }

          // 15. Frame Management
          OwnerNavSection.FRAME_MANAGEMENT -> {
            FrameManagementAdminView(repository = repository)
          }

          // 16. Ranking Management
          OwnerNavSection.RANKING_MANAGEMENT -> {
            RankingManagementAdminView(repository = repository)
          }

          // 17. Reports and Analytics
          OwnerNavSection.REPORTS_ANALYTICS -> {
            ReportsAnalyticsAdminView(repository = repository, reports = reports)
          }

          // 18. Notifications
          OwnerNavSection.NOTIFICATIONS -> {
            NotificationsAdminView(repository = repository)
          }

          // 19. Security and Permissions
          OwnerNavSection.SECURITY_PERMISSIONS -> {
            SecurityPermissionsAdminView(repository = repository)
          }

          // 20. System Settings
          OwnerNavSection.SYSTEM_SETTINGS -> {
            SystemSettingsAdminView(repository = repository, currentSettings = currencySettings)
          }

          // 21. Audit Logs
          OwnerNavSection.AUDIT_LOGS -> {
            AuditLogsAdminView(auditLogs = auditLogs)
          }
        }
      }
    }
  }
}
