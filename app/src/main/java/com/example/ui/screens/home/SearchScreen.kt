package com.example.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RoomEntity
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
  repository: LivoRepository,
  onOpenRoom: (String) -> Unit,
  onOpenUserProfile: (UserEntity) -> Unit,
  onBackClick: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableStateOf(0) } // 0: Rooms, 1: Users

  var roomResults by remember { mutableStateOf<List<RoomEntity>>(emptyList()) }
  var userResults by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
  var isSearching by remember { mutableStateOf(false) }

  fun performSearch() {
    if (searchQuery.isBlank()) {
      roomResults = emptyList()
      userResults = emptyList()
      return
    }
    isSearching = true
    coroutineScope.launch {
      if (selectedTab == 0) {
        roomResults = repository.searchRooms(searchQuery)
      } else {
        userResults = repository.searchUsers(searchQuery)
      }
      isSearching = false
    }
  }

  LaunchedEffect(searchQuery, selectedTab) {
    performSearch()
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Search Livo", onBackClick = onBackClick)
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(if (selectedTab == 0) "Enter Room ID or Room Name..." else "Enter Livo ID or Nickname...")
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = LivoViolet,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkBackground,
          contentColor = TextPrimary
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Voice Rooms (${roomResults.size})", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Users (${userResults.size})", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }
          )
        }
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (isSearching) {
        CircularProgressIndicator(
          color = LivoViolet,
          modifier = Modifier.align(Alignment.Center)
        )
      } else if (searchQuery.isBlank()) {
        EmptyStateView(
          icon = Icons.Default.Search,
          title = "Search Voice Rooms or Users",
          description = "Find your friends using their unique 8-digit Livo ID or join live parties by Room ID."
        )
      } else if (selectedTab == 0) {
        if (roomResults.isEmpty()) {
          EmptyStateView(
            icon = Icons.Default.MeetingRoom,
            title = "No rooms found",
            description = "No active voice rooms matched '$searchQuery'."
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(roomResults) { room ->
              RoomCard(room = room, onClick = { onOpenRoom(room.id) })
            }
          }
        }
      } else {
        if (userResults.isEmpty()) {
          EmptyStateView(
            icon = Icons.Default.Person,
            title = "No users found",
            description = "No Livo users matched '$searchQuery'."
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(userResults) { user ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onOpenUserProfile(user) }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(14.dp)
                ) {
                  AvatarWithFrame(
                    avatarUrl = user.avatarUrl,
                    frameId = user.equippedFrameId,
                    size = 50.dp
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = user.nickname,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      LevelBadge(level = user.level)
                      Spacer(modifier = Modifier.width(4.dp))
                      RoleBadge(role = user.role)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Livo ID: ${user.livoId} • Followers: ${user.followersCount}",
                      color = TextTertiary,
                      fontSize = 12.sp
                    )
                  }
                  Text(
                    text = "View",
                    color = LivoCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
