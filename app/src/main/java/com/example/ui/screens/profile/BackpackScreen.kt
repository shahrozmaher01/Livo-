package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FrameItem
import com.example.data.repository.LivoRepository
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BackpackScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  var selectedFrameId by remember { mutableStateOf(currentUser?.equippedFrameId ?: "frame_none") }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Backpack & Frames", onBackClick = onBackClick)
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Live Avatar Preview with equipped frame
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
        ) {
          AvatarWithFrame(
            avatarUrl = currentUser?.avatarUrl ?: "",
            frameId = if (selectedFrameId == "frame_none") null else selectedFrameId,
            size = 96.dp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Avatar Frame Preview",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = "Select a frame below to decorate your avatar in rooms and rankings",
            color = TextTertiary,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Available Avatar Frames",
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.height(12.dp))

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(repository.availableFrames) { frame ->
          val isEquipped = selectedFrameId == frame.id
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isEquipped) LivoViolet.copy(alpha = 0.25f) else DarkSurfaceCard
            ),
            border = if (isEquipped) androidx.compose.foundation.BorderStroke(1.5.dp, LivoViolet) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selectedFrameId = frame.id
                coroutineScope.launch {
                  repository.equipFrame(frame.id)
                  Toast.makeText(context, "Equipped ${frame.name}", Toast.LENGTH_SHORT).show()
                }
              }
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(14.dp)
            ) {
              AvatarWithFrame(
                avatarUrl = currentUser?.avatarUrl ?: "",
                frameId = if (frame.id == "frame_none") null else frame.id,
                size = 56.dp
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = frame.name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Text(
                text = frame.description,
                color = TextTertiary,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isEquipped) SpeakingGreen else DarkSurfaceBorder
              ) {
                Text(
                  text = if (isEquipped) "Equipped" else "Equip",
                  color = if (isEquipped) Color.White else TextSecondary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
