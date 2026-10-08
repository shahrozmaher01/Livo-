package com.example.ui.screens.rankings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RankingItem
import com.example.data.model.RankingPeriod
import com.example.data.model.RankingType
import com.example.data.model.UserEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.AvatarWithFrame
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LevelBadge
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*

@Composable
fun RankingsScreen(
  repository: LivoRepository,
  onOpenUserProfile: (UserEntity) -> Unit,
  onOpenRoom: (String) -> Unit
) {
  var selectedType by remember { mutableStateOf(RankingType.WEALTH) }
  var selectedPeriod by remember { mutableStateOf(RankingPeriod.ALL_TIME) }
  var rankings by remember { mutableStateOf<List<RankingItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(selectedType, selectedPeriod) {
    isLoading = true
    rankings = repository.getRankings(selectedType, selectedPeriod)
    isLoading = false
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Livo Leaderboards")

        // Type Tabs: Top Wealth, Top Charm, Top Room
        TabRow(
          selectedTabIndex = selectedType.ordinal,
          containerColor = DarkBackground,
          contentColor = TextPrimary
        ) {
          Tab(
            selected = selectedType == RankingType.WEALTH,
            onClick = { selectedType = RankingType.WEALTH },
            text = { Text("Top Wealth 🪙", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
          Tab(
            selected = selectedType == RankingType.CHARM,
            onClick = { selectedType = RankingType.CHARM },
            text = { Text("Top Charm ✨", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
          Tab(
            selected = selectedType == RankingType.ROOM,
            onClick = { selectedType = RankingType.ROOM },
            text = { Text("Top Rooms 🎙️", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
          )
        }

        // Period Filters: Daily, Weekly, Monthly, All-time
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          RankingPeriod.values().forEach { period ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selectedPeriod == period) LivoViolet else DarkSurfaceCard,
              modifier = Modifier
                .clickable { selectedPeriod = period }
                .padding(horizontal = 4.dp)
            ) {
              Text(
                text = period.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                color = if (selectedPeriod == period) Color.White else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
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
      if (isLoading) {
        CircularProgressIndicator(
          color = LivoViolet,
          modifier = Modifier.align(Alignment.Center)
        )
      } else if (rankings.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.EmojiEvents,
          title = "No ranking data yet",
          description = "Rankings will appear as users send gifts, participate in voice rooms, and gain charm."
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(rankings) { item ->
            RankingRow(
              item = item,
              rankingType = selectedType,
              onClick = {
                if (selectedType == RankingType.ROOM) {
                  onOpenRoom(item.entityId)
                } else {
                  val u = UserEntity(
                    id = item.entityId,
                    livoId = item.numericId,
                    email = "",
                    nickname = item.name,
                    avatarUrl = item.avatarUrl,
                    level = item.level,
                    role = item.role
                  )
                  onOpenUserProfile(u)
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun RankingRow(
  item: RankingItem,
  rankingType: RankingType,
  onClick: () -> Unit
) {
  val (rankBadgeColor, rankTextColor) = when (item.rank) {
    1 -> LivoGold to Color.Black
    2 -> Color(0xFFE2E8F0) to Color.Black
    3 -> Color(0xFFD97706) to Color.White
    else -> Color.Transparent to TextSecondary
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = if (item.rank <= 3) androidx.compose.foundation.BorderStroke(1.dp, rankBadgeColor.copy(alpha = 0.5f)) else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      // Rank position badge
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .then(if (item.rank <= 3) Modifier.background(rankBadgeColor) else Modifier)
      ) {
        Text(
          text = "${item.rank}",
          color = rankTextColor,
          fontWeight = FontWeight.ExtraBold,
          fontSize = if (item.rank <= 3) 14.sp else 13.sp
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      AvatarWithFrame(
        avatarUrl = item.avatarUrl,
        frameId = item.frameId,
        size = 46.dp
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.name,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          LevelBadge(level = item.level)
        }
        Text(
          text = if (rankingType == RankingType.ROOM) "Room ID: ${item.numericId}" else "Livo ID: ${item.numericId}",
          color = TextTertiary,
          fontSize = 11.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        val scoreLabel = when (rankingType) {
          RankingType.WEALTH -> "🪙 ${String.format("%,d", item.score)}"
          RankingType.CHARM -> "✨ ${String.format("%,d", item.score)}"
          RankingType.ROOM -> "🎙️ ${item.score} pts"
        }
        Text(
          text = scoreLabel,
          color = if (rankingType == RankingType.WEALTH) LivoGold else LivoRose,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  }
}
