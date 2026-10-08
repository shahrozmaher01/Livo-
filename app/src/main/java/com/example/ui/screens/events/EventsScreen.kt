package com.example.ui.screens.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.LivoRepository
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*

@Composable
fun EventsScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Livo Events & Festivals", onBackClick = onBackClick)
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.banner_voice_carnival),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              Surface(
                shape = RoundedCornerShape(bottomEnd = 12.dp),
                color = LivoRose,
                modifier = Modifier.align(Alignment.TopStart)
              ) {
                Text(
                  text = "MAIN CAMPAIGN",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Livo Summer Voice Carnival",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Participate in live voice rooms, receive event gifts, and climb the Top Wealth & Charm leaderboards to win permanent Royal Crystal frames, exclusive VIP sound badges, and 1,000,000 coin bonuses!",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(14.dp))
              Divider(color = DarkSurfaceBorder)
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(text = "Status: Ongoing", color = SpeakingGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "Reward Pool: 10,000,000 🪙", color = LivoGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
