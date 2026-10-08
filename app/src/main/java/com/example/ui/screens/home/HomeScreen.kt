package com.example.ui.screens.home

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.RoomCategory
import com.example.data.model.RoomEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  repository: LivoRepository,
  onOpenRoom: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenWallet: () -> Unit,
  onOpenRankings: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenEvents: () -> Unit,
  onOpenAgency: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val activeRooms by repository.activeRooms.collectAsState(initial = emptyList())
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)
  val unreadNotifications by repository.observeUnreadNotificationsCount(currentUser?.id ?: "").collectAsState(initial = 0)

  var selectedCategory by remember { mutableStateOf<RoomCategory?>(null) }
  var showCreateRoomDialog by remember { mutableStateOf(false) }

  val filteredRooms = remember(activeRooms, selectedCategory) {
    if (selectedCategory == null) activeRooms
    else activeRooms.filter { it.category == selectedCategory }
  }

  Scaffold(
    topBar = {
      // Premium Voice Chat Header
      Surface(
        color = DarkBackground,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            // User identity overview
            Row(verticalAlignment = Alignment.CenterVertically) {
              AvatarWithFrame(
                avatarUrl = currentUser?.avatarUrl ?: "",
                frameId = currentUser?.equippedFrameId,
                size = 46.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = currentUser?.nickname ?: "Guest",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  LevelBadge(level = currentUser?.level ?: 1)
                }
                Text(
                  text = "ID: ${currentUser?.livoId ?: 0}",
                  color = TextTertiary,
                  fontSize = 11.sp
                )
              }
            }

            // Wallet Coins & Notifications
            Row(verticalAlignment = Alignment.CenterVertically) {
              CoinPill(
                balance = wallet?.balance ?: 0,
                onClick = onOpenWallet
              )

              Spacer(modifier = Modifier.width(10.dp))

              // Notification bell with red dot badge
              Box {
                IconButton(
                  onClick = onOpenNotifications,
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                if (unreadNotifications > 0) {
                  Box(
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(LivoRose)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Search Trigger Bar
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurfaceElevated)
              .clickable { onOpenSearch() }
              .padding(horizontal = 14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Search by Room ID, room name, or Livo ID...",
              color = TextSecondary,
              fontSize = 13.sp
            )
          }
        }
      }
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showCreateRoomDialog = true },
        containerColor = LivoViolet,
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = "Create Room")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Room", fontWeight = FontWeight.Bold)
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
      // 1. Featured Event Banner (Voice Carnival)
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenEvents() }
        ) {
          // Bundled event illustration
          Image(
            painter = painterResource(id = R.drawable.banner_voice_carnival),
            contentDescription = "Livo Voice Festival",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Transparent, Color(0xCC000000))
                )
              )
          )
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(14.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = LivoRose
            ) {
              Text(
                text = "LIVE EVENT",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Livo Summer Voice Carnival",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Text(
              text = "Win exclusive Royal Crown frames & millions in coins!",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      // 2. Quick Discovery Actions
      item {
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          QuickActionItem(
            icon = Icons.Default.Leaderboard,
            title = "Leaderboards",
            tint = LivoGold,
            onClick = onOpenRankings
          )
          QuickActionItem(
            icon = Icons.Default.Groups,
            title = "Agencies",
            tint = LivoCyan,
            onClick = onOpenAgency
          )
          QuickActionItem(
            icon = Icons.Default.Celebration,
            title = "Events",
            tint = LivoRose,
            onClick = onOpenEvents
          )
          QuickActionItem(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Wallet",
            tint = LivoVioletLight,
            onClick = onOpenWallet
          )
        }
      }

      // 3. Category Filter Chips
      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { selectedCategory = null },
              label = { Text("All Rooms", fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LivoViolet,
                selectedLabelColor = Color.White,
                containerColor = DarkSurfaceElevated,
                labelColor = TextSecondary
              )
            )
          }
          items(RoomCategory.values()) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat.displayName, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LivoViolet,
                selectedLabelColor = Color.White,
                containerColor = DarkSurfaceElevated,
                labelColor = TextSecondary
              )
            )
          }
        }
      }

      // 4. Room List Header
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Text(
            text = "Active Voice Rooms",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          )
          Text(
            text = "${filteredRooms.size} online",
            color = OnlineGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // 5. Room Cards Grid/List
      if (filteredRooms.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Default.RecordVoiceOver,
            title = "No active voice rooms yet",
            description = "Be the first to open a voice chat room and invite friends to talk!",
            actionButtonText = "Create Voice Room",
            onActionClick = { showCreateRoomDialog = true }
          )
        }
      } else {
        items(filteredRooms) { room ->
          RoomCard(
            room = room,
            onClick = { onOpenRoom(room.id) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }
      }
    }
  }

  // Create Room Dialog
  if (showCreateRoomDialog) {
    var title by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(RoomCategory.CHAT) }
    var seatCount by remember { mutableStateOf(8) }
    var announcement by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showCreateRoomDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text("Create Live Voice Room", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Room Title") },
            placeholder = { Text("e.g., Midnight Chill & Music") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Text("Category", fontSize = 12.sp, color = TextSecondary)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            RoomCategory.values().take(3).forEach { cat ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selectedCat == cat) LivoViolet else DarkSurfaceCard,
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedCat = cat }
                  .padding(vertical = 4.dp)
              ) {
                Text(
                  text = cat.displayName.substringBefore(" "),
                  color = Color.White,
                  fontSize = 11.sp,
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              }
            }
          }

          Text("Seat Capacity", fontSize = 12.sp, color = TextSecondary)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 10, 12).forEach { count ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (seatCount == count) LivoCyan else DarkSurfaceCard,
                modifier = Modifier
                  .clickable { seatCount = count }
                  .padding(vertical = 2.dp)
              ) {
                Text(
                  text = "$count Seats",
                  color = if (seatCount == count) Color.Black else TextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = announcement,
            onValueChange = { announcement = it },
            label = { Text("Announcement / Notice") },
            placeholder = { Text("Welcome rules for room guests") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank()) {
              coroutineScope.launch {
                val created = repository.createRoom(
                  title = title,
                  category = selectedCat,
                  coverUrl = "",
                  backgroundUrl = "",
                  announcement = announcement,
                  seatCount = seatCount
                )
                showCreateRoomDialog = false
                onOpenRoom(created.id)
              }
            } else {
              Toast.makeText(context, "Please enter a room title", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoViolet)
        ) {
          Text("Go Live")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateRoomDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun QuickActionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  tint: Color,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(8.dp)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(tint.copy(alpha = 0.15f))
    ) {
      Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun RoomCard(
  room: RoomEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      // Room cover with listener badge
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(RoundedCornerShape(14.dp))
      ) {
        AsyncImage(
          model = room.coverUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400" },
          contentDescription = room.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        // Live wave indicator
        Surface(
          shape = RoundedCornerShape(bottomStart = 8.dp),
          color = Color(0xCC000000),
          modifier = Modifier.align(Alignment.TopEnd)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(SpeakingGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${room.listenersCount}",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Room info details
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = room.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = LivoViolet.copy(alpha = 0.2f)
          ) {
            Text(
              text = room.category.displayName.substringBefore(" "),
              color = LivoVioletLight,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "ID: ${room.roomNumber} • ${room.seatCount} Seats",
          color = TextTertiary,
          fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          AsyncImage(
            model = room.ownerAvatar,
            contentDescription = room.ownerName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Host: ${room.ownerName}",
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
