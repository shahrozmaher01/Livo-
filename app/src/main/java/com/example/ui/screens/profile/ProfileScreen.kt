package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  repository: LivoRepository,
  onOpenWallet: () -> Unit,
  onOpenBackpack: () -> Unit,
  onOpenAgency: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showAccountSwitchDialog by remember { mutableStateOf(false) }

  // Photo Picker Launcher for selecting new avatar from gallery (Google Play zero-permission compliant!)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        repository.updateUserProfile(
          nickname = currentUser?.nickname ?: "",
          bio = currentUser?.bio ?: "",
          avatarUrl = uri.toString(),
          countryCode = currentUser?.countryCode ?: "US"
        )
        Toast.makeText(context, "Profile picture updated from gallery", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(
          title = "My Profile",
          actions = {
            IconButton(onClick = { showAccountSwitchDialog = true }) {
              Icon(Icons.Default.SwitchAccount, contentDescription = "Switch Account", tint = TextPrimary)
            }
          }
        )
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 90.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. Profile Hero Section
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            // Avatar with photo pick button overlay
            Box(contentAlignment = Alignment.BottomEnd) {
              AvatarWithFrame(
                avatarUrl = currentUser?.avatarUrl ?: "",
                frameId = currentUser?.equippedFrameId,
                size = 88.dp,
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
              )
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(LivoViolet)
                  .clickable {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  }
              ) {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = "Change photo",
                  tint = Color.White,
                  modifier = Modifier.size(15.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nickname + Level + Role
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = currentUser?.nickname ?: "User",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              )
              Spacer(modifier = Modifier.width(6.dp))
              LevelBadge(level = currentUser?.level ?: 1)
            }

            Spacer(modifier = Modifier.height(4.dp))
            RoleBadge(role = currentUser?.role ?: UserRole.USER)

            Spacer(modifier = Modifier.height(8.dp))

            // Unique 8-digit numeric Livo ID with copy
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceCard)
                .clickable {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Livo ID", currentUser?.livoId?.toString() ?: "")
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Livo ID copied: ${currentUser?.livoId}", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Livo ID: ${currentUser?.livoId ?: 0}",
                color = LivoCyanLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = LivoCyanLight,
                modifier = Modifier.size(14.dp)
              )
            }

            if (!currentUser?.bio.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = currentUser?.bio ?: "",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = DarkSurfaceBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // Social Stats: Followers, Following, Charm, Wealth
            Row(
              horizontalArrangement = Arrangement.SpaceEvenly,
              modifier = Modifier.fillMaxWidth()
            ) {
              StatColumn(title = "Followers", value = "${currentUser?.followersCount ?: 0}")
              StatColumn(title = "Following", value = "${currentUser?.followingCount ?: 0}")
              StatColumn(title = "Wealth 🪙", value = String.format("%,d", currentUser?.wealthScore ?: 0))
              StatColumn(title = "Charm ✨", value = String.format("%,d", currentUser?.charmScore ?: 0))
            }
          }
        }
      }

      // 2. Navigation Actions
      item {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          ProfileMenuItem(
            icon = Icons.Default.AccountBalanceWallet,
            title = "My Wallet & Coins",
            subtitle = "${String.format("%,d", wallet?.balance ?: 0)} Coins Available",
            iconColor = LivoGold,
            onClick = onOpenWallet
          )

          ProfileMenuItem(
            icon = Icons.Default.Shield,
            title = "Backpack & Avatar Frames",
            subtitle = "Equip luxury frames and badges",
            iconColor = LivoVioletLight,
            onClick = onOpenBackpack
          )

          ProfileMenuItem(
            icon = Icons.Default.Groups,
            title = "Host Agency Center",
            subtitle = "Manage agency membership & hosts",
            iconColor = LivoCyan,
            onClick = onOpenAgency
          )

          // Admin panel visible if user is an Admin, Super Admin, or Official
          val isElevatedRole = (currentUser?.role?.levelPriority ?: 0) >= UserRole.ADMIN.levelPriority
          if (isElevatedRole) {
            ProfileMenuItem(
              icon = Icons.Default.AdminPanelSettings,
              title = "Admin & Operations Control",
              subtitle = "System management, coins grant & audit logs",
              iconColor = LivoRose,
              onClick = onOpenAdminPanel
            )
          }

          ProfileMenuItem(
            icon = Icons.Default.Edit,
            title = "Edit Profile Information",
            subtitle = "Change nickname, bio, and country",
            iconColor = TextSecondary,
            onClick = { showEditProfileDialog = true }
          )

          ProfileMenuItem(
            icon = Icons.Default.Logout,
            title = "Sign Out",
            subtitle = "Return to account selection",
            iconColor = LivoRose,
            onClick = onLogout
          )
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditProfileDialog) {
    var editNickname by remember { mutableStateOf(currentUser?.nickname ?: "") }
    var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var editCountry by remember { mutableStateOf(currentUser?.countryCode ?: "US") }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editNickname,
            onValueChange = { editNickname = it },
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editCountry,
            onValueChange = { editCountry = it },
            label = { Text("Country Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              repository.updateUserProfile(
                nickname = editNickname,
                bio = editBio,
                avatarUrl = currentUser?.avatarUrl ?: "",
                countryCode = editCountry
              )
              showEditProfileDialog = false
              Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoViolet)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Account Switch Dialog (Section 4)
  if (showAccountSwitchDialog) {
    var accounts by remember { mutableStateOf<List<UserEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
      accounts = repository.getAvailableAccounts()
    }

    AlertDialog(
      onDismissRequest = { showAccountSwitchDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Switch Livo Account", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          accounts.forEach { acc ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (acc.id == currentUser?.id) LivoViolet.copy(alpha = 0.2f) else DarkSurfaceCard)
                .clickable {
                  coroutineScope.launch {
                    repository.switchAccount(acc.id)
                    showAccountSwitchDialog = false
                    Toast.makeText(context, "Switched to ${acc.nickname}", Toast.LENGTH_SHORT).show()
                  }
                }
                .padding(10.dp)
            ) {
              AvatarWithFrame(avatarUrl = acc.avatarUrl, frameId = acc.equippedFrameId, size = 38.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(text = acc.nickname, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "ID: ${acc.livoId} • ${acc.role.displayName}", color = TextTertiary, fontSize = 11.sp)
              }
              if (acc.id == currentUser?.id) {
                Text(text = "Active", color = SpeakingGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAccountSwitchDialog = false }) {
          Text("Close", color = TextPrimary)
        }
      }
    )
  }
}

@Composable
private fun StatColumn(title: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Text(text = title, color = TextTertiary, fontSize = 11.sp)
  }
}

@Composable
private fun ProfileMenuItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  iconColor: Color,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.15f))
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(text = subtitle, color = TextTertiary, fontSize = 11.sp)
      }
      Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
    }
  }
}
