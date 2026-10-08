package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSheet(
  user: UserEntity,
  repository: LivoRepository,
  onDismiss: () -> Unit,
  onOpenChat: (String) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  var isFollowing by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }

  LaunchedEffect(user.id) {
    isFollowing = repository.isFollowing(user.id)
  }

  val isMe = currentUser?.id == user.id

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      AvatarWithFrame(
        avatarUrl = user.avatarUrl,
        frameId = user.equippedFrameId,
        size = 80.dp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = user.nickname,
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        LevelBadge(level = user.level)
      }

      Spacer(modifier = Modifier.height(4.dp))
      RoleBadge(role = user.role)

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Livo ID: ${user.livoId} • ${user.countryCode}",
        color = LivoCyanLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )

      if (user.bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = user.bio,
          color = TextSecondary,
          fontSize = 12.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      Divider(color = DarkSurfaceBorder)
      Spacer(modifier = Modifier.height(14.dp))

      // Stats row
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "${user.followersCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = "Followers", color = TextTertiary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "${user.followingCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = "Following", color = TextTertiary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = String.format("%,d", user.wealthScore), color = LivoGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = "Wealth 🪙", color = TextTertiary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = String.format("%,d", user.charmScore), color = LivoRose, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text(text = "Charm ✨", color = TextTertiary, fontSize = 11.sp)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Actions
      if (!isMe) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Button(
            onClick = {
              coroutineScope.launch {
                repository.toggleFollow(user.id)
                isFollowing = !isFollowing
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isFollowing) DarkSurfaceBorder else LivoViolet
            ),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isFollowing) "Following" else "Follow")
          }

          Button(
            onClick = {
              onDismiss()
              onOpenChat(user.id)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LivoCyan),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Message", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Moderation / Safety: Report & Block
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextButton(onClick = { showReportDialog = true }) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Report User", color = TextTertiary, fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.width(16.dp))

          TextButton(
            onClick = {
              coroutineScope.launch {
                repository.blockUser(user.id, user.nickname)
                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                onDismiss()
              }
            }
          ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = LivoRose, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Block User", color = LivoRose, fontSize = 11.sp)
          }
        }
      }
    }
  }

  // Report Dialog
  if (showReportDialog) {
    var reason by remember { mutableStateOf("Harassment / Inappropriate behavior") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Report ${user.nickname}", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select violation reason:", color = TextSecondary, fontSize = 12.sp)
          listOf(
            "Harassment / Inappropriate behavior",
            "Coin scam / Fraud",
            "Impersonation",
            "Spam"
          ).forEach { r ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { reason = r }
                .padding(vertical = 4.dp)
            ) {
              RadioButton(selected = reason == r, onClick = { reason = r })
              Spacer(modifier = Modifier.width(6.dp))
              Text(r, color = TextPrimary, fontSize = 12.sp)
            }
          }
          OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            placeholder = { Text("Additional details...") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              repository.report(
                targetType = "USER",
                targetId = user.id,
                targetName = user.nickname,
                reason = reason,
                details = details
              )
              showReportDialog = false
              Toast.makeText(context, "Report submitted for admin review", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoRose)
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }
}
