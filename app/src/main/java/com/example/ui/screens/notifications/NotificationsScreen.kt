package com.example.ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val notifications by repository.observeNotifications(currentUser?.id ?: "").collectAsState(initial = emptyList())
  val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

  // Auto mark all read when viewing screen (Section 18 requirement)
  LaunchedEffect(currentUser?.id) {
    currentUser?.id?.let {
      repository.markNotificationsRead(it)
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
          title = "Notifications",
          onBackClick = onBackClick,
          actions = {
            IconButton(
              onClick = {
                currentUser?.id?.let {
                  coroutineScope.launch { repository.markNotificationsRead(it) }
                }
              }
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = LivoCyan)
            }
          }
        )
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (notifications.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.Notifications,
          title = "No notifications",
          description = "You're all caught up! Gift alerts, follows, and room invitations will appear here."
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(notifications) { notif ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = notif.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Text(
                    text = dateFormat.format(Date(notif.timestamp)),
                    color = TextTertiary,
                    fontSize = 11.sp
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = notif.content,
                  color = TextSecondary,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
