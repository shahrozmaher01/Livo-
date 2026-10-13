package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LivoBrandLogo(
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  showGlow: Boolean = true,
  shape: Shape = RoundedCornerShape(size * 0.22f),
  contentScale: ContentScale = ContentScale.Fit,
  onClick: (() -> Unit)? = null
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    if (showGlow) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .scale(1.10f)
          .clip(shape)
          .background(
            Brush.radialGradient(
              listOf(
                LivoCyan.copy(alpha = 0.40f),
                LivoViolet.copy(alpha = 0.25f),
                Color.Transparent
              )
            )
          )
      )
    }
    Image(
      painter = painterResource(id = R.drawable.ic_livo_logo),
      contentDescription = "Livo Brand Logo",
      contentScale = contentScale,
      modifier = Modifier
        .fillMaxSize()
        .clip(shape)
    )
  }
}

@Composable
fun LivoBrandHeader(
  modifier: Modifier = Modifier,
  logoSize: Dp = 38.dp,
  title: String = "Livo",
  subtitle: String? = "Live Audio Social",
  onClick: (() -> Unit)? = null
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    LivoBrandLogo(size = logoSize, showGlow = true)
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        letterSpacing = 0.5.sp
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          color = TextSecondary,
          fontSize = 11.sp
        )
      }
    }
  }
}

@Composable
fun AvatarWithFrame(
  avatarUrl: String,
  frameId: String? = null,
  size: Dp = 56.dp,
  isSpeaking: Boolean = false,
  speakingLevel: Float = 0f,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isSpeaking) 1.15f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  val frameBorderColor = when (frameId) {
    "frame_super_admin" -> Brush.sweepGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFFEF4444)))
    "frame_royal_crystal" -> Brush.sweepGradient(listOf(LivoViolet, LivoRose, LivoCyan, LivoViolet))
    "frame_neon_cyber" -> Brush.sweepGradient(listOf(LivoCyan, Color(0xFF3B82F6), LivoCyan))
    "frame_vip_gold" -> Brush.sweepGradient(listOf(LivoGold, LivoGoldLight, LivoGold))
    else -> null
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    // Speaking wave ring
    if (isSpeaking) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .scale(pulseScale)
          .clip(CircleShape)
          .border(2.5.dp, SpeakingGreen, CircleShape)
      )
    }

    // Avatar image
    AsyncImage(
      model = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200" },
      contentDescription = "User Avatar",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(if (frameBorderColor != null) size - 6.dp else size)
        .clip(CircleShape)
        .background(DarkSurfaceElevated)
    )

    // Decorative frame overlay
    if (frameBorderColor != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .border(3.dp, frameBorderColor, CircleShape)
      )
    }
  }
}

@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
  val gradient = when {
    level >= 20 -> Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFF59E0B)))
    level >= 10 -> Brush.horizontalGradient(listOf(LivoViolet, LivoRose))
    level >= 5 -> Brush.horizontalGradient(listOf(LivoCyan, Color(0xFF3B82F6)))
    else -> Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(gradient)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = "Lv.$level",
      color = Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun RoleBadge(role: UserRole, modifier: Modifier = Modifier) {
  if (role == UserRole.USER) return

  val (bgColor, textColor) = when (role) {
    UserRole.SUPER_ADMIN, UserRole.OFFICIAL -> Color(0xFFEF4444) to Color.White
    UserRole.ADMIN, UserRole.ADMIN_LEADER -> Color(0xFFF59E0B) to Color.Black
    UserRole.MANAGER -> LivoViolet to Color.White
    UserRole.HOST -> LivoCyan to Color.Black
    UserRole.AGENCY, UserRole.AGENCY_LEADER -> LivoRose to Color.White
    else -> DarkSurfaceBorder to TextPrimary
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = bgColor,
    modifier = modifier
  ) {
    Text(
      text = role.displayName,
      color = textColor,
      fontSize = 9.sp,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun CoinPill(
  balance: Long,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = DarkSurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, LivoGold.copy(alpha = 0.5f)),
    modifier = modifier
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
      Text(text = "🪙", fontSize = 13.sp)
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = String.format("%,d", balance),
        color = LivoGoldLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      if (onClick != null) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Recharge",
          tint = LivoGold,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
fun AudioWaveVisualizer(
  speakingLevel: Float,
  modifier: Modifier = Modifier,
  barCount: Int = 5,
  color: Color = SpeakingGreen
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave")

  Row(
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    repeat(barCount) { index ->
      val factor = (index + 1) * 0.2f
      val animatedHeight by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = (4f + (speakingLevel * 20f * (1f + factor))).coerceAtMost(26f),
        animationSpec = infiniteRepeatable(
          animation = tween(200 + (index * 60), easing = LinearEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "bar_$index"
      )
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(animatedHeight.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(color)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivoTopBar(
  title: String,
  onBackClick: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {}
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(onClick = onBackClick) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary
          )
        }
      }
    },
    actions = actions,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = DarkBackground,
      titleContentColor = TextPrimary
    )
  )
}

@Composable
fun LivoButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  containerColor: Color = LivoViolet
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      disabledContainerColor = containerColor.copy(alpha = 0.4f)
    ),
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        color = Color.White,
        modifier = Modifier.size(22.dp),
        strokeWidth = 2.5.dp
      )
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(imageVector = icon, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector? = null,
  title: String,
  description: String,
  showBrandLogo: Boolean = false,
  actionButtonText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp)
  ) {
    if (showBrandLogo) {
      LivoBrandLogo(size = 64.dp, showGlow = true)
    } else if (icon != null) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = LivoVioletLight,
          modifier = Modifier.size(36.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      color = TextPrimary,
      fontWeight = FontWeight.Bold,
      fontSize = 17.sp,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = description,
      color = TextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center
    )
    if (actionButtonText != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(18.dp))
      OutlinedButton(
        onClick = onActionClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LivoViolet)
      ) {
        Text(actionButtonText, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
fun ErrorRetryView(
  errorMessage: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(24.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Warning,
      contentDescription = null,
      tint = LivoRose,
      modifier = Modifier.size(42.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "Operation Failed",
      color = TextPrimary,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = errorMessage,
      color = TextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onRetry,
      colors = ButtonDefaults.buttonColors(containerColor = LivoViolet),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Retry", fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
fun RoomCard(
  room: RoomEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(14.dp))
      ) {
        AsyncImage(
          model = room.coverUrl,
          contentDescription = room.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = room.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = LivoViolet.copy(alpha = 0.2f)
          ) {
            Text(
              text = room.category.displayName,
              color = LivoVioletLight,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Host: ${room.ownerName} • ID: ${room.roomNumber}",
          color = TextSecondary,
          fontSize = 12.sp,
          maxLines = 1
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = null,
          tint = SpeakingGreen,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "${room.listenersCount}",
          color = SpeakingGreen,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftSendingModalSheet(
  targetUser: UserEntity,
  repository: LivoRepository,
  onDismiss: () -> Unit,
  onGiftSent: ((giftName: String, count: Int, totalCoins: Long) -> Unit)? = null
) {
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)

  var selectedTab by remember { mutableStateOf(0) } // 0: Standard, 1: Lucky Gifts
  var selectedGiftId by remember { mutableStateOf(repository.giftCatalog.first().id) }
  var selectedLuckyGiftId by remember { mutableStateOf(repository.luckyGiftCatalog.first().id) }
  var selectedMultiplier by remember { mutableStateOf(1) }
  var isSending by remember { mutableStateOf(false) }
  var celebrationBanner by remember { mutableStateOf<String?>(null) }

  val standardGift = repository.giftCatalog.find { it.id == selectedGiftId } ?: repository.giftCatalog.first()
  val luckyGift = repository.luckyGiftCatalog.find { it.id == selectedLuckyGiftId } ?: repository.luckyGiftCatalog.first()

  val totalCost = if (selectedTab == 0) standardGift.coinPrice * selectedMultiplier
  else luckyGift.coinPrice * selectedMultiplier

  val userBalance = wallet?.balance ?: 0L

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0D1326),
    dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF334155)) }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 28.dp)
    ) {
      // Recipient Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          AvatarWithFrame(
            avatarUrl = targetUser.avatarUrl,
            frameId = targetUser.equippedFrameId,
            size = 46.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Send Gift to ${targetUser.nickname}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.width(6.dp))
              LevelBadge(level = targetUser.level)
            }
            Text(
              text = "ID: ${targetUser.livoId} • Recipient earns Diamonds 💎",
              color = Color(0xFF67E8F9),
              fontSize = 11.sp
            )
          }
        }

        // Sender Balance Pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF1E2845),
          border = BorderStroke(0.8.dp, LivoGold.copy(alpha = 0.5f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("🪙", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = String.format("%,d", userBalance),
              color = LivoGoldLight,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Gift Tabs (Standard vs Lucky)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF0B1020),
        contentColor = Color.White
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Standard Gifts (${repository.giftCatalog.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("🍀 Lucky Rewards (${repository.luckyGiftCatalog.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Gift Grid / List
      if (selectedTab == 0) {
        androidx.compose.foundation.lazy.LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(repository.giftCatalog) { gift ->
            val isSelected = selectedGiftId == gift.id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) LivoViolet.copy(alpha = 0.35f) else Color(0xFF16203B),
              border = BorderStroke(1.2.dp, if (isSelected) LivoViolet else Color(0xFF233054)),
              modifier = Modifier
                .width(82.dp)
                .clickable { selectedGiftId = gift.id }
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
              ) {
                Text(gift.iconEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = gift.name,
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${String.format("%,d", gift.coinPrice)} 🪙",
                  color = LivoGoldLight,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      } else {
        androidx.compose.foundation.lazy.LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(repository.luckyGiftCatalog) { lucky ->
            val isSelected = selectedLuckyGiftId == lucky.id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF16203B),
              border = BorderStroke(1.2.dp, if (isSelected) Color(0xFF10B981) else Color(0xFF233054)),
              modifier = Modifier
                .width(110.dp)
                .clickable { selectedLuckyGiftId = lucky.id }
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(10.dp)
              ) {
                Text(lucky.iconEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = lucky.name,
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${lucky.coinPrice} 🪙",
                  color = LivoGoldLight,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF10B981).copy(alpha = 0.2f),
                  modifier = Modifier.padding(top = 4.dp)
                ) {
                  Text(
                    text = "+${lucky.receiverDiamondReward} 💎",
                    color = Color(0xFF34D399),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quantity Multiplier (1x, 10x, 66x, 99x, 520x)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf(1, 10, 66, 99, 520).forEach { count ->
          val isSelected = selectedMultiplier == count
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) LivoRose else Color(0xFF16203B),
            border = BorderStroke(0.8.dp, if (isSelected) LivoRose else Color(0xFF233054)),
            modifier = Modifier
              .weight(1f)
              .clickable { selectedMultiplier = count }
          ) {
            Text(
              text = "${count}x",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Total & Send Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text("Total Cost", color = TextTertiary, fontSize = 11.sp)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🪙", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = String.format("%,d", totalCost),
              color = LivoGold,
              fontSize = 18.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }

        Button(
          onClick = {
            if (userBalance < totalCost) return@Button
            isSending = true
            coroutineScope.launch {
              if (selectedTab == 0) {
                val res = repository.sendGift(
                  receiverId = targetUser.id,
                  giftId = standardGift.id,
                  count = selectedMultiplier
                )
                isSending = false
                res.onSuccess {
                  celebrationBanner = "Sent ${selectedMultiplier}x ${standardGift.name} to ${targetUser.nickname}!"
                  onGiftSent?.invoke(standardGift.name, selectedMultiplier, totalCost)
                  onDismiss()
                }
              } else {
                val res = repository.sendLuckyGift(
                  receiverId = targetUser.id,
                  luckyGiftId = luckyGift.id,
                  count = selectedMultiplier
                )
                isSending = false
                res.onSuccess {
                  celebrationBanner = "Sent ${selectedMultiplier}x Lucky ${luckyGift.name} to ${targetUser.nickname}!"
                  onGiftSent?.invoke(luckyGift.name, selectedMultiplier, totalCost)
                  onDismiss()
                }
              }
            }
          },
          enabled = !isSending && userBalance >= totalCost,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (userBalance < totalCost) Color(0xFF334155) else LivoRose
          ),
          modifier = Modifier.height(46.dp)
        ) {
          if (isSending) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
          } else {
            Text(
              text = if (userBalance < totalCost) "Insufficient Coins" else "Send Gift 🎁",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  }
}


