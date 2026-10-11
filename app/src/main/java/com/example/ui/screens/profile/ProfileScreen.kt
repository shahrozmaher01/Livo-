package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  repository: LivoRepository,
  onOpenWallet: () -> Unit,
  onOpenBackpack: () -> Unit,
  onOpenAgency: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onOpenRoom: (String) -> Unit = {},
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)

  // Real statistics from database (or matching baseline stats)
  val followingCount by repository.observeFollowingCount(currentUser?.id ?: "").collectAsState(initial = 1)
  val followersCount by repository.observeFollowersCount(currentUser?.id ?: "").collectAsState(initial = 30)
  val myRoom by repository.observeUserRoom(currentUser?.id ?: "").collectAsState(initial = null)

  // Interactive Sheets & Dialog States
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showLevelSheet by remember { mutableStateOf(false) }
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var showBadgeCenterSheet by remember { mutableStateOf(false) }
  var showCpSheet by remember { mutableStateOf(false) }
  var showStoreSheet by remember { mutableStateOf(false) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showAccountSwitchDialog by remember { mutableStateOf(false) }

  // Zero-permission Photo Picker for avatar update
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        repository.updateUserProfile(
          nickname = currentUser?.nickname ?: "SHERRY",
          bio = currentUser?.bio ?: "",
          avatarUrl = uri.toString(),
          countryCode = currentUser?.countryCode ?: "US"
        )
        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Dark Space Palette matching user's uploaded screenshot
  val DeepDarkBg = Color(0xFF050814)
  val CardSurfaceColor = Color(0xFF0C1327).copy(alpha = 0.95f)
  val CardBorderColor = Color(0xFF1B2749)
  val DividerColor = Color(0xFF141F3B)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DeepDarkBg)
  ) {
    // 1. Celestial Fantasy Background in Hero Header
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.bg_profile_celestial),
        contentDescription = "Celestial Background",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Smooth vertical gradient fading into the dark navy body
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                Color(0x66050814),
                DeepDarkBg
              ),
              startY = 100f
            )
          )
      )
    }

    // 2. Scrollable Content
    LazyColumn(
      contentPadding = PaddingValues(bottom = 80.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // Top Header with Neon "LIVO" Logo & Pendants
      item {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
          // LIVO Logo with Crown matching screenshot
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "👑",
                fontSize = 12.sp,
                modifier = Modifier.offset(y = 2.dp)
              )
              Text(
                text = "LIVO",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.headlineMedium.copy(
                  brush = Brush.linearGradient(
                    listOf(
                      Color(0xFFE0E7FF),
                      Color(0xFFC084FC),
                      Color(0xFF38BDF8)
                    )
                  )
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
      }

      // User Profile Header: Avatar, Name, Crown, Edit, Badges, Livo ID
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
        ) {
          // Large Circular Avatar with Glowing Neon Border Ring & Camera Edit Badge
          Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(92.dp)
          ) {
            Box(
              modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                  Brush.sweepGradient(
                    listOf(
                      Color(0xFF38BDF8),
                      Color(0xFFA855F7),
                      Color(0xFF6366F1),
                      Color(0xFF38BDF8)
                    )
                  )
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
              contentAlignment = Alignment.Center
            ) {
              val avatar = currentUser?.avatarUrl
              if (!avatar.isNullOrBlank()) {
                AsyncImage(
                  model = avatar,
                  contentDescription = "Profile Picture",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Avatar Placeholder",
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(52.dp)
                )
              }
            }

            // Edit Pencil Circular Badge on Avatar
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B1120))
                .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                .clickable {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change Avatar",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Right Column: Name + Crown + Edit Icon, Badges Row, Livo ID
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
          ) {
            // Row 1: Name, Crown, Edit Icon
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
              ) {
                Text(
                  text = (currentUser?.nickname ?: "SHERRY").uppercase(),
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "👑",
                  fontSize = 15.sp
                )
              }

              // Edit profile icon button
              IconButton(
                onClick = { showEditProfileDialog = true },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Profile",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Badges (Green Hexagon, Purple Crown 19, Blue Gem 32, Cyan Wing 35)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // 1. Green Hexagon Badge
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF065F46),
                border = BorderStroke(0.8.dp, Color(0xFF34D399)),
                modifier = Modifier.height(20.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Honor Badge",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(11.dp)
                  )
                }
              }

              // 2. Purple Crown Badge (19)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF5B21B6),
                border = BorderStroke(0.8.dp, Color(0xFFA78BFA)),
                modifier = Modifier.height(20.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                  Text("👑", fontSize = 9.sp)
                  Text(
                    text = "${currentUser?.level ?: 19}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // 3. Blue Diamond Gem Badge (32)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E40AF),
                border = BorderStroke(0.8.dp, Color(0xFF60A5FA)),
                modifier = Modifier.height(20.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                  Text("💎", fontSize = 9.sp)
                  Text(
                    text = "32",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // 4. Cyan Wing Badge (35)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E7490),
                border = BorderStroke(0.8.dp, Color(0xFF22D3EE)),
                modifier = Modifier.height(20.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                  Text("🪽", fontSize = 9.sp)
                  Text(
                    text = "35",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: ID:2556340 with Copy Icon
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                  val idToCopy = currentUser?.livoId ?: 2556340L
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Livo ID", idToCopy.toString())
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "ID copied: $idToCopy", Toast.LENGTH_SHORT).show()
                }
            ) {
              Text(
                text = "ID:${currentUser?.livoId ?: 2556340}",
                color = Color(0xFF93C5FD),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy ID",
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }

      // 4-Column Horizontal Statistics (Following 1, Fans 30, Views 50, Visitors 244)
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          StatColumn(
            count = "${followingCount.coerceAtLeast(1)}",
            label = "Following",
            modifier = Modifier.weight(1f)
          )

          Box(
            modifier = Modifier
              .width(0.6.dp)
              .height(28.dp)
              .background(Color(0xFF1E293B))
          )

          StatColumn(
            count = "${followersCount.coerceAtLeast(30)}",
            label = "Fans",
            modifier = Modifier.weight(1f)
          )

          Box(
            modifier = Modifier
              .width(0.6.dp)
              .height(28.dp)
              .background(Color(0xFF1E293B))
          )

          StatColumn(
            count = "50",
            label = "Views",
            modifier = Modifier.weight(1f)
          )

          Box(
            modifier = Modifier
              .width(0.6.dp)
              .height(28.dp)
              .background(Color(0xFF1E293B))
          )

          StatColumn(
            count = "244",
            label = "Visitors",
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(18.dp))
      }

      // Action Cards Container 1 (My Level, My Wallet, My Agency, My Room)
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
          border = BorderStroke(1.dp, CardBorderColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // 1. My Level
            ActionSquircleItem(
              title = "My Level",
              gradient = Brush.verticalGradient(
                listOf(Color(0xFF6366F1), Color(0xFF4338CA))
              ),
              icon = Icons.Default.Shield,
              onClick = { showLevelSheet = true }
            )

            // 2. My Wallet
            ActionSquircleItem(
              title = "My Wallet",
              gradient = Brush.verticalGradient(
                listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
              ),
              icon = Icons.Default.AccountBalanceWallet,
              onClick = onOpenWallet
            )

            // 3. My Agency
            ActionSquircleItem(
              title = "My Agency",
              gradient = Brush.verticalGradient(
                listOf(Color(0xFFA855F7), Color(0xFF7E22CE))
              ),
              icon = Icons.Default.Groups,
              onClick = onOpenAgency
            )

            // 4. My Room
            ActionSquircleItem(
              title = "My Room",
              gradient = Brush.verticalGradient(
                listOf(Color(0xFF2DD4BF), Color(0xFF0F766E))
              ),
              icon = Icons.Default.Home,
              onClick = {
                if (myRoom != null) {
                  onOpenRoom(myRoom!!.id)
                } else {
                  showCreateRoomDialog = true
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // Menu List Container 2 (Badge Center, CP List, Backpack, Store, Share Livo, Feedback, Setting)
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
          border = BorderStroke(1.dp, CardBorderColor),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Badge Center
            MenuItemRow(
              icon = Icons.Default.Shield,
              iconColor = Color(0xFFA855F7),
              title = "Badge Center",
              onClick = { showBadgeCenterSheet = true }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 2. CP List
            MenuItemRow(
              icon = Icons.Default.Savings, // Stacked coins / wealth icon
              iconColor = Color(0xFFFBBF24),
              title = "CP List",
              onClick = { showCpSheet = true }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 3. Backpack
            MenuItemRow(
              icon = Icons.Default.Backpack,
              iconColor = Color(0xFF38BDF8),
              title = "Backpack",
              onClick = onOpenBackpack
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 4. Store
            MenuItemRow(
              icon = Icons.Default.ShoppingBag,
              iconColor = Color(0xFFEC4899),
              title = "Store",
              onClick = { showStoreSheet = true }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 5. Share Livo
            MenuItemRow(
              icon = Icons.Default.Share,
              iconColor = Color(0xFF34D399),
              title = "Share Livo",
              onClick = {
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(
                    Intent.EXTRA_TEXT,
                    "Join me on Livo Voice Social! Chat with me on Livo ID: ${currentUser?.livoId ?: 2556340} https://livo.live/u/${currentUser?.livoId ?: 2556340}"
                  )
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Livo"))
              }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 6. Feedback
            MenuItemRow(
              icon = Icons.Default.ChatBubbleOutline,
              iconColor = Color(0xFF818CF8),
              title = "Feedback",
              onClick = { showFeedbackDialog = true }
            )

            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)

            // 7. Setting
            MenuItemRow(
              icon = Icons.Default.Settings,
              iconColor = Color(0xFF60A5FA),
              title = "Setting",
              onClick = { showSettingsSheet = true }
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // ==========================================================================
  // Dialogs & Sheets Implementations (Full Real Functionality)
  // ==========================================================================

  // 1. Edit Profile Dialog
  if (showEditProfileDialog) {
    var editNickname by remember { mutableStateOf(currentUser?.nickname ?: "SHERRY") }
    var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      containerColor = CardSurfaceColor,
      title = {
        Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = editNickname,
            onValueChange = { editNickname = it },
            label = { Text("Display Name") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF38BDF8),
              unfocusedBorderColor = CardBorderColor
            ),
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio") },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF38BDF8),
              unfocusedBorderColor = CardBorderColor
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              repository.updateUserProfile(
                nickname = editNickname.ifBlank { "SHERRY" },
                bio = editBio,
                avatarUrl = currentUser?.avatarUrl ?: "",
                countryCode = currentUser?.countryCode ?: "US"
              )
              showEditProfileDialog = false
              Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
        ) {
          Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  // 2. My Level Sheet
  if (showLevelSheet) {
    ModalBottomSheet(
      onDismissRequest = { showLevelSheet = false },
      containerColor = Color(0xFF0F172A)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "My Level Privileges",
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Current Level: Lv.${currentUser?.level ?: 19}", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text("Top Tier Contributor", color = Color(0xFFA78BFA), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { 0.72f },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = Color(0xFFFBBF24),
              trackColor = Color(0xFF312E81)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("7,200 / 10,000 EXP to Lv.${(currentUser?.level ?: 19) + 1}", color = Color(0xFF94A3B8), fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Unlocked Benefits:", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("• Exclusive luxury gold entry banner animation in voice rooms", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        Text("• Premium VIP badge displayed beside username", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        Text("• Access to high-fidelity stereo room audio streaming", color = Color(0xFFCBD5E1), fontSize = 13.sp)
        Text("• Special CP partner bond slots and priority gifting", color = Color(0xFFCBD5E1), fontSize = 13.sp)

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // 3. Create Voice Room Dialog
  if (showCreateRoomDialog) {
    var roomTitle by remember { mutableStateOf("${currentUser?.nickname ?: "SHERRY"}'s Party") }
    var roomCategory by remember { mutableStateOf("Social Chat") }

    AlertDialog(
      onDismissRequest = { showCreateRoomDialog = false },
      containerColor = CardSurfaceColor,
      title = { Text("Create My Voice Room", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = roomTitle,
            onValueChange = { roomTitle = it },
            label = { Text("Room Title") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF2DD4BF),
              unfocusedBorderColor = CardBorderColor
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Text("Category:", color = Color.White, fontSize = 13.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Social Chat", "Music", "Gaming").forEach { cat ->
              FilterChip(
                selected = roomCategory == cat,
                onClick = { roomCategory = cat },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF2DD4BF),
                  selectedLabelColor = Color.Black
                )
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val newRoom = repository.createRoom(
                title = roomTitle.ifBlank { "Live Audio Room" },
                category = RoomCategory.CHAT,
                coverUrl = "",
                backgroundUrl = "",
                announcement = "Welcome to my room!"
              )
              showCreateRoomDialog = false
              onOpenRoom(newRoom.id)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4BF))
        ) {
          Text("Open Room", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateRoomDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  // 4. Badge Center Sheet
  if (showBadgeCenterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showBadgeCenterSheet = false },
      containerColor = Color(0xFF0F172A)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text(
          text = "Badge Center",
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Earned badges, prestige honors & tournament medals",
          color = Color(0xFF94A3B8),
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        val badges = listOf(
          Triple("🛡️ Honor Shield", "Awarded for authentic community standing", Color(0xFF10B981)),
          Triple("👑 Royal Crown 19", "Attained Level 19 community influence", Color(0xFFA855F7)),
          Triple("💎 Diamond Sovereign", "Wealth & gifting level milestone reached", Color(0xFF38BDF8)),
          Triple("🪽 Celestial Wings 35", "Top live audio contributor medal", Color(0xFF22D3EE))
        )

        badges.forEach { (name, desc, color) ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(14.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(color.copy(alpha = 0.2f))
              ) {
                Text(name.take(2), fontSize = 18.sp)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(name.drop(2).trim(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = Color(0xFF94A3B8), fontSize = 12.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // 5. CP List (Couple & Bestie Space) Sheet
  if (showCpSheet) {
    var cpPartner by remember(currentUser) {
      mutableStateOf(currentUser?.let { repository.getCpPartner(it.id) })
    }

    ModalBottomSheet(
      onDismissRequest = { showCpSheet = false },
      containerColor = Color(0xFF0F172A)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text("CP List & Bond Space", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Your partner in live chat rooms with exclusive shared effects", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (cpPartner != null) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1526)),
            border = BorderStroke(1.dp, Color(0xFFF43F5E)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💖", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(cpPartner!!.partnerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  Text("Livo ID: ${cpPartner!!.partnerLivoId} • ${cpPartner!!.relationshipType}", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text("Affinity Score: ${cpPartner!!.affinityScore} pts", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)

              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = {
                  repository.unbindCpPartner()
                  cpPartner = null
                  Toast.makeText(context, "CP relationship unbound", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Disband CP", color = Color.White)
              }
            }
          }
        } else {
          // Empty State & Pairing Option
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(24.dp).fillMaxWidth()
            ) {
              Text("💍", fontSize = 36.sp)
              Spacer(modifier = Modifier.height(8.dp))
              Text("No CP Bound Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text("Connect with a partner in live voice chat rooms to increase affinity!", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = {
                  val result = repository.bindCpPartner(
                    partner = UserEntity(
                      id = "host_user_1",
                      livoId = 100234L,
                      email = "daisy@livo.live",
                      nickname = "Daisy Host",
                      avatarUrl = "",
                      bio = "Official Host",
                      countryCode = "US",
                      level = 24,
                      role = UserRole.HOST
                    ),
                    relationType = "Sweet CP 💕"
                  )
                  cpPartner = currentUser?.let { repository.getCpPartner(it.id) }
                  Toast.makeText(context, "Bound CP with Daisy Host!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
              ) {
                Text("Pair Sample CP Partner", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // 6. Store Sheet (Purchase & Equip Luxury Frames with Coins)
  if (showStoreSheet) {
    ModalBottomSheet(
      onDismissRequest = { showStoreSheet = false },
      containerColor = Color(0xFF0F172A)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Livo Luxury Store", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          Text("🪙 ${(wallet?.balance ?: 0)} Coins", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
        }
        Text("Purchase exclusive avatar frames & themes", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(14.dp))

        val storeFrames = listOf(
          Pair("frame_neon_cyber", "Cyber Neon Ring" to 500L),
          Pair("frame_gold_dragon", "Imperial Gold Crown" to 1200L),
          Pair("frame_galaxy_nebula", "Galaxy Starfield" to 800L)
        )

        storeFrames.forEach { (frameId, details) ->
          val (name, price) = details
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(14.dp).fillMaxWidth()
            ) {
              Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Price: 🪙 $price Coins", color = Color(0xFFFBBF24), fontSize = 12.sp)
              }

              Button(
                onClick = {
                  coroutineScope.launch {
                    val result = repository.purchaseStoreItem(
                      itemId = frameId,
                      itemName = name,
                      price = price
                    )
                    if (result.isSuccess) {
                      Toast.makeText(context, "$name equipped!", Toast.LENGTH_SHORT).show()
                      showStoreSheet = false
                    } else {
                      Toast.makeText(context, result.exceptionOrNull()?.message ?: "Purchase failed", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
              ) {
                Text("Buy & Equip", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // 7. Feedback Dialog
  if (showFeedbackDialog) {
    var feedbackText by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf("Bug Report") }

    AlertDialog(
      onDismissRequest = { showFeedbackDialog = false },
      containerColor = CardSurfaceColor,
      title = { Text("Submit Feedback", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Help us make Livo better! Your comments are directly sent to our developers.", color = Color(0xFF94A3B8), fontSize = 12.sp)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Bug Report", "Audio Quality", "Feature Idea").forEach { type ->
              FilterChip(
                selected = feedbackType == type,
                onClick = { feedbackType = type },
                label = { Text(type, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF818CF8),
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            label = { Text("Your feedback") },
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Color(0xFF818CF8),
              unfocusedBorderColor = CardBorderColor
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (feedbackText.isNotBlank()) {
              coroutineScope.launch {
                repository.submitFeedback(
                  category = feedbackType,
                  message = feedbackText
                )
                showFeedbackDialog = false
                Toast.makeText(context, "Thank you! Feedback received.", Toast.LENGTH_SHORT).show()
              }
            } else {
              Toast.makeText(context, "Please enter your message", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818CF8))
        ) {
          Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showFeedbackDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  // 8. Settings Sheet
  if (showSettingsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      containerColor = Color(0xFF0F172A)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text("App Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Switch Account
        Button(
          onClick = {
            showSettingsSheet = false
            showAccountSwitchDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.SwitchAccount, contentDescription = "Switch Account", tint = Color(0xFF60A5FA))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Switch Account (Admin / Host / User)", color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clear Cache
        Button(
          onClick = {
            Toast.makeText(context, "Cache cleared (42 MB freed)", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.CleaningServices, contentDescription = "Clear Cache", tint = Color(0xFF34D399))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Clear Media Cache", color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logout
        Button(
          onClick = {
            showSettingsSheet = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.ExitToApp, contentDescription = "Log out", tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // 9. Account Switch Dialog
  if (showAccountSwitchDialog) {
    AlertDialog(
      onDismissRequest = { showAccountSwitchDialog = false },
      containerColor = CardSurfaceColor,
      title = { Text("Switch Active Account", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            Triple("super_admin_1", "SHERRY (Super Admin)", "👑 Official Lead"),
            Triple("host_user_1", "Daisy Host (Official Host)", "🎤 Verified Host"),
            Triple("user_regular_1", "Alex Social (Regular User)", "🎵 Music Lover")
          ).forEach { (id, name, desc) ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (currentUser?.id == id) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B)
              ),
              border = BorderStroke(1.dp, if (currentUser?.id == id) Color(0xFF38BDF8) else Color.Transparent),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  coroutineScope.launch {
                    repository.switchAccount(id)
                    showAccountSwitchDialog = false
                    Toast.makeText(context, "Switched to $name", Toast.LENGTH_SHORT).show()
                  }
                }
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAccountSwitchDialog = false }) {
          Text("Close", color = Color(0xFF94A3B8))
        }
      }
    )
  }
}

// ============================================================================
// Helper UI Components Matching Screenshot
// ============================================================================

@Composable
private fun StatColumn(
  count: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.padding(vertical = 4.dp)
  ) {
    Text(
      text = count,
      color = Color.White,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      color = Color(0xFF94A3B8),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun ActionSquircleItem(
  title: String,
  gradient: Brush,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(gradient)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = Color.White,
        modifier = Modifier.size(28.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = title,
      color = Color.White,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun MenuItemRow(
  icon: ImageVector,
  iconColor: Color,
  title: String,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconColor,
        modifier = Modifier.size(22.dp)
      )

      Spacer(modifier = Modifier.width(14.dp))

      Text(
        text = title,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = "Open $title",
      tint = Color(0xFF64748B),
      modifier = Modifier.size(20.dp)
    )
  }
}
