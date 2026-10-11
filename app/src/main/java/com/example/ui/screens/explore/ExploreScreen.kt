package com.example.ui.screens.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RoomCategory
import com.example.data.model.RoomEntity
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.AvatarWithFrame
import com.example.ui.theme.*

@Composable
fun ExploreScreen(
  repository: LivoRepository,
  onOpenRoom: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenRankings: () -> Unit,
  onOpenEvents: () -> Unit,
  onOpenUserProfile: (UserEntity) -> Unit
) {
  val activeRooms by repository.activeRooms.collectAsState(initial = emptyList())
  var selectedCategory by remember { mutableStateOf<RoomCategory?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  val filteredRooms = remember(activeRooms, selectedCategory, searchQuery) {
    activeRooms.filter { room ->
      val matchesCategory = selectedCategory == null || room.category == selectedCategory
      val matchesSearch = searchQuery.isBlank() ||
        room.title.contains(searchQuery, ignoreCase = true) ||
        room.roomNumber.toString().contains(searchQuery) ||
        room.ownerName.contains(searchQuery, ignoreCase = true)
      matchesCategory && matchesSearch
    }
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkBackground)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(
              text = "Explore",
              color = TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Discover voice rooms, parties & community",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
              onClick = onOpenSearch,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
            ) {
              Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            IconButton(
              onClick = onOpenRankings,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
            ) {
              Icon(Icons.Default.EmojiEvents, contentDescription = "Rankings", tint = LivoGold, modifier = Modifier.size(20.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by Room Name, ID or Host", color = TextTertiary, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceCard,
            unfocusedContainerColor = DarkSurfaceCard,
            focusedBorderColor = LivoViolet,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 80.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. Quick Feature Shortcuts (Rankings, Events, Trending)
      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          FeatureShortcutCard(
            title = "Leaderboards",
            subtitle = "Top Wealth & Charm",
            icon = Icons.Default.Leaderboard,
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
            modifier = Modifier.weight(1f),
            onClick = onOpenRankings
          )
          FeatureShortcutCard(
            title = "Live Events",
            subtitle = "Parties & Battles",
            icon = Icons.Default.Festival,
            gradient = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
            modifier = Modifier.weight(1f),
            onClick = onOpenEvents
          )
        }
      }

      // 2. Categories Horizontal Scroll
      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { selectedCategory = null },
              label = { Text("All Rooms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LivoViolet,
                selectedLabelColor = Color.White,
                containerColor = DarkSurfaceCard,
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = if (selectedCategory == null) LivoViolet else DarkSurfaceBorder,
                selectedBorderColor = LivoViolet,
                enabled = true,
                selected = selectedCategory == null
              ),
              shape = RoundedCornerShape(12.dp)
            )
          }

          items(RoomCategory.values()) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { selectedCategory = if (selectedCategory == category) null else category },
              label = { Text(category.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LivoViolet,
                selectedLabelColor = Color.White,
                containerColor = DarkSurfaceCard,
                labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = if (selectedCategory == category) LivoViolet else DarkSurfaceBorder,
                selectedBorderColor = LivoViolet,
                enabled = true,
                selected = selectedCategory == category
              ),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }
      }

      // 3. Rooms Count Header
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = if (selectedCategory != null) selectedCategory!!.displayName else "Active Voice Rooms",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${filteredRooms.size} live",
            color = LivoCyanLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // 4. Room List
      if (filteredRooms.isEmpty()) {
        item {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp)
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.MeetingRoom,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No active voice rooms found",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Be the first to create a party or check back soon!",
                color = TextTertiary,
                fontSize = 12.sp
              )
            }
          }
        }
      } else {
        items(filteredRooms) { room ->
          ExploreRoomCard(
            room = room,
            onClick = { onOpenRoom(room.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun FeatureShortcutCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  gradient: List<Color>,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.horizontalGradient(gradient))
      .clickable(onClick = onClick)
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
          .background(Color.White.copy(alpha = 0.2f))
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
      }
    }
  }
}

@Composable
private fun ExploreRoomCard(
  room: RoomEntity,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      // Room cover or avatar
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(RoundedCornerShape(12.dp))
      ) {
        AsyncImage(
          model = room.coverUrl,
          contentDescription = room.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        // Category pill
        Surface(
          color = Color.Black.copy(alpha = 0.7f),
          shape = RoundedCornerShape(bottomEnd = 8.dp),
          modifier = Modifier.align(Alignment.TopStart)
        ) {
          Text(
            text = room.category.name.take(4),
            color = LivoGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = room.title,
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          AvatarWithFrame(avatarUrl = room.ownerAvatar, size = 18.dp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = room.ownerName,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "ID: ${room.roomNumber}",
            color = TextTertiary,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "• ${room.seatCount} seats",
            color = TextTertiary,
            fontSize = 11.sp
          )
        }
      }

      // Enter Button
      Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = LivoViolet),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text("Enter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
