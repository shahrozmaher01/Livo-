package com.example.ui.screens.messages

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ConversationEntity
import com.example.data.model.PrivateMessageEntity
import com.example.data.model.ReelEntity
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
  repository: LivoRepository,
  onOpenChat: (String) -> Unit
) {
  val currentUser by repository.currentUser.collectAsState()
  val conversations by repository.observeConversations().collectAsState(initial = emptyList())
  val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
  val activeReels by repository.reels.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var showNewMessageDialog by remember { mutableStateOf(false) }
  var filterUnreadOnly by remember { mutableStateOf(false) }

  // Theme accents matching request: Black background, white text, purple & pink accents
  val PureBlack = Color(0xFF000000)
  val DarkSurfaceCard = Color(0xFF0F1528)
  val DarkBorderColor = Color(0xFF1E2845)
  val AccentPurple = Color(0xFFA855F7)
  val AccentPink = Color(0xFFEC4899)

  // Filter conversations by search and unread toggle
  val filteredConversations = remember(conversations, searchQuery, filterUnreadOnly) {
    conversations.filter { conv ->
      val matchesSearch = searchQuery.isBlank() ||
        conv.peerName.contains(searchQuery, ignoreCase = true) ||
        conv.lastMessage.contains(searchQuery, ignoreCase = true) ||
        conv.peerLivoId.toString().contains(searchQuery)
      val matchesUnread = !filterUnreadOnly || conv.unreadCount > 0
      matchesSearch && matchesUnread
    }
  }

  // Filter users for direct search
  val matchingUsers = remember(allUsers, searchQuery, currentUser) {
    if (searchQuery.isBlank()) emptyList()
    else {
      allUsers.filter { user ->
        user.id != currentUser?.id &&
          (user.nickname.contains(searchQuery, ignoreCase = true) ||
            user.livoId.toString().contains(searchQuery))
      }
    }
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(PureBlack)
          .statusBarsPadding()
      ) {
        // TOP HEADER: Title "Chats", Heart Logo, New Message button, Filter button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Original LIVO Heart Emblem
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(
                      Color(0xFF8B5CF6),
                      Color(0xFFEC4899)
                    )
                  )
                )
            ) {
              Text(text = "💜", fontSize = 18.sp)
            }

            Text(
              text = "Chats",
              color = Color.White,
              fontSize = 26.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Unread filter pill toggle
            FilterChip(
              selected = filterUnreadOnly,
              onClick = { filterUnreadOnly = !filterUnreadOnly },
              label = {
                Text(
                  text = "Unread",
                  fontSize = 12.sp,
                  color = if (filterUnreadOnly) Color.White else Color(0xFF94A3B8)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentPurple.copy(alpha = 0.8f),
                containerColor = DarkSurfaceCard
              ),
              border = BorderStroke(
                1.dp,
                if (filterUnreadOnly) AccentPink else DarkBorderColor
              ),
              modifier = Modifier.height(32.dp)
            )

            // New Message Button
            IconButton(
              onClick = { showNewMessageDialog = true },
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(
                      Color(0xFF9333EA),
                      Color(0xFFEC4899)
                    )
                  )
                )
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New Message",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // SEARCH BAR: Placeholder "Search LIVO"
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
              Text(
                text = "Search LIVO",
                color = Color(0xFF64748B),
                fontSize = 14.sp
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(20.dp)
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DarkSurfaceCard,
              unfocusedContainerColor = DarkSurfaceCard,
              focusedBorderColor = AccentPurple,
              unfocusedBorderColor = DarkBorderColor,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
      }
    },
    containerColor = PureBlack
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 90.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. STORY / MOMENT ROW
      if (searchQuery.isBlank()) {
        item {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Moments",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // "Your Story" item with (+) button
              item {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier
                    .width(68.dp)
                    .clickable { showNewMessageDialog = true }
                ) {
                  Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(60.dp)
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF334155), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                    ) {
                      val userAvatar = currentUser?.avatarUrl
                      if (!userAvatar.isNullOrBlank()) {
                        AsyncImage(
                          model = userAvatar,
                          contentDescription = "Your Story Avatar",
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize()
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Default.Person,
                          contentDescription = "Your Story",
                          tint = Color.White,
                          modifier = Modifier.size(30.dp)
                        )
                      }
                    }

                    // Plus badge
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AccentPink)
                        .border(1.5.dp, PureBlack, CircleShape)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Your Story",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              // Real Active Moments / Stories
              items(activeReels.take(8)) { reel ->
                StoryAvatarItem(
                  reel = reel,
                  onClick = { onOpenChat(reel.creatorId) }
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
              color = Color(0xFF131A30),
              thickness = 0.8.dp,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
        }
      }

      // If search query is present, show matching registered users header
      if (searchQuery.isNotBlank() && matchingUsers.isNotEmpty()) {
        item {
          Text(
            text = "People on LIVO",
            color = AccentPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }

        items(matchingUsers) { user ->
          UserSearchResultRow(
            user = user,
            onClick = { onOpenChat(user.id) }
          )
        }

        item {
          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(
            color = Color(0xFF131A30),
            thickness = 0.8.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      // 2. MESSAGES SECTION (Actual Conversations)
      if (filteredConversations.isEmpty() && matchingUsers.isEmpty()) {
        item {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 50.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF111827))
                  .border(1.dp, Color(0xFF374151), CircleShape)
              ) {
                Icon(
                  imageVector = Icons.Default.ChatBubbleOutline,
                  contentDescription = "Empty Messages",
                  tint = AccentPurple,
                  modifier = Modifier.size(34.dp)
                )
              }

              Text(
                text = if (searchQuery.isNotBlank()) "No conversations found" else "No messages yet",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )

              Text(
                text = if (searchQuery.isNotBlank())
                  "Try searching by name or LIVO ID"
                else
                  "Chat with hosts and friends across Livo rooms and agencies.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )

              Spacer(modifier = Modifier.height(6.dp))

              Button(
                onClick = { showNewMessageDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = AccentPurple
                ),
                shape = RoundedCornerShape(20.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start a Conversation")
              }
            }
          }
        }
      } else {
        items(filteredConversations) { conv ->
          // Find if user is online
          val peer = allUsers.find { it.id == conv.peerUserId }
          val isOnline = peer?.isOnline ?: false

          ConversationItemRow(
            conversation = conv,
            isOnline = isOnline,
            onClick = { onOpenChat(conv.peerUserId) }
          )
        }
      }
    }
  }

  // ==========================================================================
  // New Message User Selection Dialog
  // ==========================================================================
  if (showNewMessageDialog) {
    var userSearchText by remember { mutableStateOf("") }
    val selectableUsers = remember(allUsers, userSearchText, currentUser) {
      allUsers.filter { user ->
        user.id != currentUser?.id &&
          (userSearchText.isBlank() ||
            user.nickname.contains(userSearchText, ignoreCase = true) ||
            user.livoId.toString().contains(userSearchText))
      }
    }

    AlertDialog(
      onDismissRequest = { showNewMessageDialog = false },
      containerColor = Color(0xFF0F1528),
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "New Message",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
          IconButton(
            onClick = { showNewMessageDialog = false },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
        ) {
          OutlinedTextField(
            value = userSearchText,
            onValueChange = { userSearchText = it },
            placeholder = { Text("Search by name or LIVO ID...", fontSize = 13.sp) },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = AccentPurple)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF17203B),
              unfocusedContainerColor = Color(0xFF17203B),
              focusedBorderColor = AccentPurple,
              unfocusedBorderColor = DarkBorderColor,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          )

          if (selectableUsers.isEmpty()) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp)
            ) {
              Text(
                text = "No users found",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
              )
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(selectableUsers) { user ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF17203B))
                    .clickable {
                      showNewMessageDialog = false
                      onOpenChat(user.id)
                    }
                    .padding(10.dp)
                ) {
                  Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF223055))
                    ) {
                      if (!user.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                          model = user.avatarUrl,
                          contentDescription = user.nickname,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize()
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Default.Person,
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(24.dp)
                        )
                      }
                    }

                    if (user.isOnline) {
                      Box(
                        modifier = Modifier
                          .size(11.dp)
                          .clip(CircleShape)
                          .background(Color(0xFF10B981))
                          .border(1.5.dp, Color(0xFF17203B), CircleShape)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = user.nickname,
                      color = Color.White,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "ID: ${user.livoId} • Lv.${user.level}",
                      color = Color(0xFF94A3B8),
                      fontSize = 11.sp
                    )
                  }

                  Button(
                    onClick = {
                      showNewMessageDialog = false
                      onOpenChat(user.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Chat", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {}
    )
  }
}

@Composable
private fun StoryAvatarItem(
  reel: ReelEntity,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .width(68.dp)
      .clickable(onClick = onClick)
  ) {
    // Unseen story pink/purple gradient ring
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(60.dp)
        .clip(CircleShape)
        .background(
          Brush.sweepGradient(
            listOf(
              Color(0xFFEC4899),
              Color(0xFFA855F7),
              Color(0xFF6366F1),
              Color(0xFFEC4899)
            )
          )
        )
        .padding(2.5.dp)
        .clip(CircleShape)
        .background(Color(0xFF000000))
        .padding(2.dp)
        .clip(CircleShape)
    ) {
      if (!reel.creatorAvatar.isNullOrBlank()) {
        AsyncImage(
          model = reel.creatorAvatar,
          contentDescription = reel.creatorName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = reel.creatorName,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun UserSearchResultRow(
  user: UserEntity,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Box(
      contentAlignment = Alignment.BottomEnd,
      modifier = Modifier.size(46.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E293B))
      ) {
        if (!user.avatarUrl.isNullOrBlank()) {
          AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.nickname,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(24.dp)
          )
        }
      }

      if (user.isOnline) {
        Box(
          modifier = Modifier
            .size(11.dp)
            .clip(CircleShape)
            .background(Color(0xFF10B981))
            .border(1.5.dp, Color(0xFF000000), CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = user.nickname,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "LIVO ID: ${user.livoId} • Lv.${user.level}",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp
      )
    }

    Button(
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.height(32.dp)
    ) {
      Text("Message", fontSize = 12.sp)
    }
  }
}

@Composable
private fun ConversationItemRow(
  conversation: ConversationEntity,
  isOnline: Boolean,
  onClick: () -> Unit
) {
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    // Circular Profile Picture with Online Status Indicator
    Box(
      contentAlignment = Alignment.BottomEnd,
      modifier = Modifier.size(54.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E2845))
      ) {
        if (!conversation.peerAvatar.isNullOrBlank()) {
          AsyncImage(
            model = conversation.peerAvatar,
            contentDescription = conversation.peerName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(28.dp)
          )
        }
      }

      // Online status indicator
      if (isOnline) {
        Box(
          modifier = Modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(Color(0xFF10B981))
            .border(2.dp, Color(0xFF000000), CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

    // Middle Column: Display Name, Role Badge, Message Preview
    Column(modifier = Modifier.weight(1f)) {
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
            text = conversation.peerName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          RoleBadge(role = conversation.peerRole)
        }

        // Timestamp
        Text(
          text = timeFormat.format(Date(conversation.lastTimestamp)),
          color = Color(0xFF64748B),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = conversation.lastMessage,
          color = if (conversation.unreadCount > 0) Color.White else Color(0xFF94A3B8),
          fontSize = 13.sp,
          fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        // Unread Badge indicator
        if (conversation.unreadCount > 0) {
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    Color(0xFFEC4899),
                    Color(0xFFA855F7)
                  )
                )
              )
              .padding(horizontal = 5.dp)
          ) {
            Text(
              text = "${conversation.unreadCount}",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
  peerUserId: String,
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var peerUser by remember { mutableStateOf<UserEntity?>(null) }
  val messages by repository.observePrivateMessages(peerUserId).collectAsState(initial = emptyList())
  val currentUser by repository.currentUser.collectAsState()
  var textInput by remember { mutableStateOf("") }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  val AccentPurple = Color(0xFFA855F7)
  val DarkSurface = Color(0xFF0F1528)

  LaunchedEffect(peerUserId) {
    peerUser = repository.getUserById(peerUserId)
    repository.markConversationRead(peerUserId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              contentAlignment = Alignment.BottomEnd,
              modifier = Modifier.size(40.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1E2845))
              ) {
                if (!peerUser?.avatarUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = peerUser?.avatarUrl,
                    contentDescription = peerUser?.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              if (peerUser?.isOnline == true) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(1.5.dp, Color(0xFF000000), CircleShape)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = peerUser?.nickname ?: "Chat",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = if (peerUser?.isOnline == true) "Online • ID: ${peerUser?.livoId ?: 0}"
                else "Offline • ID: ${peerUser?.livoId ?: 0}",
                color = if (peerUser?.isOnline == true) Color(0xFF10B981) else Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000))
      )
    },
    bottomBar = {
      Surface(
        color = DarkSurface,
        border = BorderStroke(0.6.dp, Color(0xFF1E2845)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Write a message...", color = Color(0xFF64748B), fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF17203B),
              unfocusedContainerColor = Color(0xFF17203B),
              focusedBorderColor = AccentPurple,
              unfocusedBorderColor = Color(0xFF1E2845),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              if (textInput.isNotBlank()) {
                val textToSend = textInput
                textInput = ""
                coroutineScope.launch {
                  repository.sendPrivateMessage(peerUserId, textToSend)
                }
              }
            },
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    Color(0xFF9333EA),
                    Color(0xFFEC4899)
                  )
                )
              )
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    },
    containerColor = Color(0xFF000000)
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      items(messages) { msg ->
        val isMe = msg.senderId == currentUser?.id
        Row(
          horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
          modifier = Modifier.fillMaxWidth()
        ) {
          Surface(
            shape = RoundedCornerShape(
              topStart = 16.dp,
              topEnd = 16.dp,
              bottomStart = if (isMe) 16.dp else 2.dp,
              bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            color = if (isMe) Color(0xFF9333EA) else Color(0xFF141C33),
            border = if (isMe) null else BorderStroke(0.6.dp, Color(0xFF233055)),
            modifier = Modifier.widthIn(max = 280.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
              Text(
                text = msg.content,
                color = Color.White,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.height(3.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.align(Alignment.End)
              ) {
                Text(
                  text = timeFormat.format(Date(msg.timestamp)),
                  color = if (isMe) Color(0xFFE9D5FF) else Color(0xFF64748B),
                  fontSize = 10.sp
                )
                if (isMe) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Delivered",
                    tint = if (msg.isRead) Color(0xFF67E8F9) else Color(0xFFE9D5FF),
                    modifier = Modifier.size(13.dp)
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
