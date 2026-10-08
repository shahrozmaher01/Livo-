package com.example.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.data.model.PrivateMessageEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LivoTopBar
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
  repository: LivoRepository,
  onOpenChat: (String) -> Unit
) {
  val conversations by repository.observeConversations().collectAsState(initial = emptyList())

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Messages")
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (conversations.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.Chat,
          title = "No messages yet",
          description = "Connect with hosts and friends in voice rooms to start private conversations."
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(conversations) { conv ->
            ConversationRow(conversation = conv, onClick = { onOpenChat(conv.peerUserId) })
          }
        }
      }
    }
  }
}

@Composable
fun ConversationRow(
  conversation: ConversationEntity,
  onClick: () -> Unit
) {
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      AvatarWithFrame(
        avatarUrl = conversation.peerAvatar,
        size = 50.dp
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = conversation.peerName,
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            RoleBadge(role = conversation.peerRole)
          }

          Text(
            text = timeFormat.format(Date(conversation.lastTimestamp)),
            color = TextTertiary,
            fontSize = 11.sp
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
            color = TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (conversation.unreadCount > 0) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(LivoRose)
            ) {
              Text(
                text = "${conversation.unreadCount}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
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
  var peerUser by remember { mutableStateOf<com.example.data.model.UserEntity?>(null) }
  val messages by repository.observePrivateMessages(peerUserId).collectAsState(initial = emptyList())
  val currentUser by repository.currentUser.collectAsState()
  var textInput by remember { mutableStateOf("") }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

  LaunchedEffect(peerUserId) {
    peerUser = repository.getUserById(peerUserId)
    repository.markConversationRead(peerUserId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarWithFrame(avatarUrl = peerUser?.avatarUrl ?: "", size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = peerUser?.nickname ?: "Chat",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Text(
                text = "ID: ${peerUser?.livoId ?: 0} • Online",
                color = OnlineGreen,
                fontSize = 11.sp
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
      )
    },
    bottomBar = {
      Surface(
        color = DarkSurface,
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
            placeholder = { Text("Write a message...", fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (textInput.isNotBlank()) {
                coroutineScope.launch {
                  repository.sendPrivateMessage(peerUserId, textInput)
                  textInput = ""
                }
              }
            },
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(LivoViolet)
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
          }
        }
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
              topStart = 14.dp,
              topEnd = 14.dp,
              bottomStart = if (isMe) 14.dp else 2.dp,
              bottomEnd = if (isMe) 2.dp else 14.dp
            ),
            color = if (isMe) LivoViolet else DarkSurfaceCard,
            modifier = Modifier.widthIn(max = 280.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Text(
                text = msg.content,
                color = Color.White,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = timeFormat.format(Date(msg.timestamp)),
                color = if (isMe) LivoVioletLight else TextTertiary,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }
        }
      }
    }
  }
}
