package com.example.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ReelEntity
import com.example.data.model.RoomCategory
import com.example.data.model.RoomEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.screens.reels.ReelsScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  repository: LivoRepository,
  onOpenRoom: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenWallet: () -> Unit,
  onOpenRankings: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenEvents: () -> Unit,
  onOpenAgency: () -> Unit,
  onOpenReels: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val activeRooms by repository.activeRooms.collectAsState(initial = emptyList())
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)
  val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
  val topRankedUsers = remember(allUsers) {
    allUsers.sortedByDescending { it.charmScore + it.wealthScore }.take(3)
  }

  var selectedCategory by remember { mutableStateOf("All") }
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var showAllRoomsDialog by remember { mutableStateOf(false) }
  var selectedReelIndex by remember { mutableStateOf<Int?>(null) }

  // Golden Luxury Theme Colors matching the user's screenshot
  val GoldAccent = Color(0xFFF5BA42)
  val GoldDarkBorder = Color(0xFFE5A93C)
  val DarkCardBg = Color(0xFF14131A)
  val DarkPillBg = Color(0xFF16151E)
  val BorderSubtle = Color(0xFF282834)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    LazyColumn(
      contentPadding = PaddingValues(bottom = 24.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Top Search Bar
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(Color(0xFF141419))
              .border(BorderStroke(1.dp, BorderSubtle), RoundedCornerShape(24.dp))
              .clickable { onOpenSearch() }
              .padding(horizontal = 16.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = Color(0xFF7A7A85),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Search...",
              color = Color(0xFF7A7A85),
              fontSize = 14.sp
            )
          }
        }
      }

      // 2. Header Bar: Crowned 'L' + "L Voice" on left, Home & Reel icons on right
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          // Brand Logo: Crowned golden L + golden italic script "L Voice"
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onOpenEvents() }
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_l_voice_crown),
              contentDescription = "L Voice Emblem",
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "L Voice",
              color = Color(0xFFF3C775),
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              fontStyle = FontStyle.Italic,
              letterSpacing = 0.5.sp
            )
          }

          // Right Icons: Home & Reel clapperboard
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            IconButton(
              onClick = { /* Stay on Home */ },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = "Home",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
              )
            }

            IconButton(
              onClick = onOpenReels,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_reel),
                contentDescription = "Reel",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }

      // 3. Featured Hero Event Banner: "Good Vibes Better Conversations ♥"
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
              BorderStroke(0.6.dp, GoldAccent.copy(alpha = 0.35f)),
              RoundedCornerShape(16.dp)
            )
            .clickable { onOpenEvents() }
        ) {
          Image(
            painter = painterResource(id = R.drawable.banner_good_vibes),
            contentDescription = "Good Vibes Better Conversations",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      }

      // 4. Ranking Widget Pill: Crown "Ranking >" + Top 3 Rank Podiums
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DarkCardBg),
          border = BorderStroke(1.dp, Color(0xFF423824)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenRankings() }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            // Left "Ranking >"
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_crown),
                contentDescription = "Crown",
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ranking",
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = ">",
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }

            // Top 3 Podium Users (Real Data from database)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              val user1 = topRankedUsers.getOrNull(0)
              val user2 = topRankedUsers.getOrNull(1)
              val user3 = topRankedUsers.getOrNull(2)

              // Rank 1
              RankUserPodiumItem(
                rankNumber = 1,
                crownColor = Color(0xFFF5BA42),
                score = user1?.let { formatReelCount(it.charmScore + it.wealthScore) } ?: "-",
                avatarUrl = user1?.avatarUrl
              )

              // Rank 2
              RankUserPodiumItem(
                rankNumber = 2,
                crownColor = Color(0xFFE2E8F0),
                score = user2?.let { formatReelCount(it.charmScore + it.wealthScore) } ?: "-",
                avatarUrl = user2?.avatarUrl
              )

              // Rank 3
              RankUserPodiumItem(
                rankNumber = 3,
                crownColor = Color(0xFFCD7F32),
                score = user3?.let { formatReelCount(it.charmScore + it.wealthScore) } ?: "-",
                avatarUrl = user3?.avatarUrl
              )
            }
          }
        }
      }

      // 5. Top Categories Header
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
        ) {
          Text(
            text = "Top Categories",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showAllRoomsDialog = true }
          ) {
            Text(
              text = "More",
              color = Color(0xFF8E8E93),
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = ">",
              color = Color(0xFF8E8E93),
              fontSize = 12.sp
            )
          }
        }
      }

      // 6. Top Categories Chips (Two-row layout as in screenshot)
      item {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          // Row 1: All, Pakistan, India
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            CategoryChip(
              title = "All",
              emoji = "🔥",
              isSelected = selectedCategory == "All",
              modifier = Modifier.weight(1.1f),
              onClick = { selectedCategory = "All" }
            )
            CategoryChip(
              title = "Pakistan",
              emoji = "🇵🇰",
              isSelected = selectedCategory == "Pakistan",
              modifier = Modifier.weight(1.3f),
              onClick = { selectedCategory = "Pakistan" }
            )
            CategoryChip(
              title = "India",
              emoji = "🇮🇳",
              isSelected = selectedCategory == "India",
              modifier = Modifier.weight(1.2f),
              onClick = { selectedCategory = "India" }
            )
          }

          // Row 2: Bangladesh, Nepal
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            CategoryChip(
              title = "Bangladesh",
              emoji = "🇧🇩",
              isSelected = selectedCategory == "Bangladesh",
              modifier = Modifier.weight(1.4f),
              onClick = { selectedCategory = "Bangladesh" }
            )
            CategoryChip(
              title = "Nepal",
              emoji = "🇳🇵",
              isSelected = selectedCategory == "Nepal",
              modifier = Modifier.weight(1.2f),
              onClick = { selectedCategory = "Nepal" }
            )
            Spacer(modifier = Modifier.weight(1.2f))
          }
        }
      }

      // 7. Reels Section Header
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_reel),
            contentDescription = "Reels",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Reels",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        }
      }

      // 8. 2-Column Reels Grid (Card 1, Card 2, Card 3, Card 4)
      item {
        val reelsList by repository.reels.collectAsState()
        if (reelsList.isNotEmpty()) {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          ) {
            // Row 1: First 2 reels
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              if (reelsList.isNotEmpty()) {
                ReelCardItem(
                  reel = reelsList[0],
                  modifier = Modifier.weight(1f),
                  onClick = { selectedReelIndex = 0 }
                )
              }
              if (reelsList.size > 1) {
                ReelCardItem(
                  reel = reelsList[1],
                  modifier = Modifier.weight(1f),
                  onClick = { selectedReelIndex = 1 }
                )
              }
            }

            // Row 2: Next 2 reels
            if (reelsList.size > 2) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                ReelCardItem(
                  reel = reelsList[2],
                  modifier = Modifier.weight(1f),
                  onClick = { selectedReelIndex = 2 }
                )
                if (reelsList.size > 3) {
                  ReelCardItem(
                    reel = reelsList[3],
                    modifier = Modifier.weight(1f),
                    onClick = { selectedReelIndex = 3 }
                  )
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      // 9. Active Voice Rooms List (real persistent rooms accessible directly from home)
      if (activeRooms.isNotEmpty()) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Live Voice Chat Rooms",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
            Text(
              text = "${activeRooms.size} online",
              color = SpeakingGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        items(activeRooms) { room ->
          RoomCard(
            room = room,
            onClick = { onOpenRoom(room.id) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
          )
        }
      }
    }

    // Full-screen Reel Viewer Dialog when tapped
    if (selectedReelIndex != null) {
      Box(modifier = Modifier.fillMaxSize()) {
        ReelsScreen(
          repository = repository,
          initialReelIndex = selectedReelIndex ?: 0,
          onBackClick = { selectedReelIndex = null }
        )
      }
    }

    // Create Room Dialog
    if (showCreateRoomDialog) {
      CreateRoomDialog(
        onDismiss = { showCreateRoomDialog = false },
        onCreate = { title, announcement, category ->
          coroutineScope.launch {
            try {
              val created = repository.createRoom(
                title = title,
                category = category,
                coverUrl = "",
                backgroundUrl = "",
                announcement = announcement
              )
              showCreateRoomDialog = false
              onOpenRoom(created.id)
            } catch (e: Exception) {
              Toast.makeText(context, e.message ?: "Failed to create room", Toast.LENGTH_SHORT).show()
            }
          }
        }
      )
    }

    // All Rooms / More Categories Dialog
    if (showAllRoomsDialog) {
      AlertDialog(
        onDismissRequest = { showAllRoomsDialog = false },
        containerColor = DarkCardBg,
        title = {
          Text("All Voice Categories", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoomCategory.values().forEach { cat ->
              val emoji = when (cat) {
                RoomCategory.CHAT -> "💬"
                RoomCategory.MUSIC -> "🎵"
                RoomCategory.GAMING -> "🎮"
                RoomCategory.KARAOKE -> "🎤"
                RoomCategory.DATING -> "💖"
                RoomCategory.RADIO -> "📻"
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1B1A24),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedCategory = cat.displayName
                    showAllRoomsDialog = false
                  }
                  .padding(12.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(emoji, fontSize = 20.sp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(cat.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Browse active rooms", color = Color.Gray, fontSize = 11.sp)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showAllRoomsDialog = false }) {
            Text("Close", color = GoldAccent)
          }
        }
      )
    }
  }
}

@Composable
private fun RankUserPodiumItem(
  rankNumber: Int,
  crownColor: Color,
  score: String,
  avatarRes: Int? = null,
  avatarUrl: String? = null
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // Avatar with mini crown overlay
    Box(
      contentAlignment = Alignment.TopStart,
      modifier = Modifier.size(34.dp)
    ) {
      if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
          model = avatarUrl,
          contentDescription = "Rank $rankNumber Avatar",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.Center)
            .clip(CircleShape)
            .border(1.dp, crownColor, CircleShape)
        )
      } else if (avatarRes != null) {
        Image(
          painter = painterResource(id = avatarRes),
          contentDescription = "Rank $rankNumber Avatar",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.Center)
            .clip(CircleShape)
            .border(1.dp, crownColor, CircleShape)
        )
      } else {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.Center)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(Color(0xFF382E1E), Color(0xFF1E1A2B))
              )
            )
            .border(1.dp, crownColor, CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = crownColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Small Crown
      Icon(
        painter = painterResource(id = R.drawable.ic_crown),
        contentDescription = "Crown",
        tint = crownColor,
        modifier = Modifier
          .size(12.dp)
          .align(Alignment.TopStart)
      )
    }

    // Rank Number
    Text(
      text = "$rankNumber",
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )

    // Fire Score
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = "🔥", fontSize = 9.sp)
      Text(
        text = score,
        color = Color(0xFFE2E8F0),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
private fun CategoryChip(
  title: String,
  emoji: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) Color(0xFF261D0C) else Color(0xFF16151E),
    border = BorderStroke(
      width = if (isSelected) 1.2.dp else 0.8.dp,
      color = if (isSelected) Color(0xFFE5A93C) else Color(0xFF2A2838)
    ),
    modifier = modifier
      .height(38.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp)
    ) {
      Text(text = emoji, fontSize = 13.sp)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = title,
        color = if (isSelected) Color(0xFFFDE68A) else Color.White,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun ReelCardItem(
  reel: ReelEntity,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val imageRes = when (reel.coverDrawableName) {
    "reel_bg_dreamer_girl" -> R.drawable.reel_bg_dreamer_girl
    "reel_thumb_silence" -> R.drawable.reel_thumb_silence
    "reel_thumb_music" -> R.drawable.reel_thumb_music
    "reel_thumb_friendship" -> R.drawable.reel_thumb_friendship
    "reel_thumb_dreams" -> R.drawable.reel_thumb_dreams
    else -> R.drawable.reel_bg_dreamer_girl
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF16151E)),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column {
      // Reel Video Cover Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.15f)
          .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
      ) {
        Image(
          painter = painterResource(id = imageRes),
          contentDescription = reel.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient for view count readability
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color(0xAA000000)
                ),
                startY = 60f
              )
            )
        )

        // "Reel" Pill Badge at Top Right
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0x99000000),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
        ) {
          Text(
            text = "Reel",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        // Bottom Stats Row (Play icon + Views on left, Duration on right)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = formatReelCount(reel.viewsCount),
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "00:${reel.durationSec.toString().padStart(2, '0')}",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
          )
        }
      }

      // Reel Caption Below Image
      Text(
        text = reel.caption,
        color = Color(0xFFE2E8F0),
        fontSize = 11.sp,
        lineHeight = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
      )
    }
  }
}

private fun formatReelCount(count: Long): String {
  return when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
    else -> "$count"
  }
}

@Composable
private fun CreateRoomDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, announcement: String, category: RoomCategory) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var announcement by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(RoomCategory.CHAT) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        LivoBrandLogo(size = 24.dp, showGlow = false)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Voice Room", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Room Name") },
          placeholder = { Text("e.g. Late Night Hangout") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = announcement,
          onValueChange = { announcement = it },
          label = { Text("Announcement / Rules") },
          placeholder = { Text("Welcome to our voice room!") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreate(title.trim(), announcement.trim(), selectedCategory)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5BA42))
      ) {
        Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color.Gray)
      }
    }
  )
}
