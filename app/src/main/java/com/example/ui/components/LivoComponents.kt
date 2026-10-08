package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*

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
  icon: ImageVector,
  title: String,
  description: String,
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
