package com.example.ui.screens.room

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.audio.VoiceConnectionState
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
  roomId: String,
  repository: LivoRepository,
  onLeaveRoom: () -> Unit,
  onOpenWallet: () -> Unit,
  onOpenUserProfile: (UserEntity) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val currentRoom by repository.currentRoom.collectAsState()
  val seats by repository.observeSeatsForRoom(roomId).collectAsState(initial = emptyList())
  val messages by repository.observeMessagesForRoom(roomId).collectAsState(initial = emptyList())
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)

  // Voice engine states
  val isMuted by repository.voiceEngine.isMuted.collectAsState()
  val isSpeakerOn by repository.voiceEngine.isSpeakerOn.collectAsState()
  val speakingLevel by repository.voiceEngine.speakingLevel.collectAsState()
  val voiceState by repository.voiceEngine.connectionState.collectAsState()

  // Sheet dialog states
  var showGiftSheet by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var selectedSeatForModeration by remember { mutableStateOf<RoomSeatEntity?>(null) }
  var targetUserForGift by remember { mutableStateOf<UserEntity?>(null) }
  var messageInput by remember { mutableStateOf("") }
  var latestGiftBanner by remember { mutableStateOf<String?>(null) }

  val chatListState = rememberLazyListState()

  // Scroll to bottom when new messages arrive
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      chatListState.animateScrollToItem(messages.size - 1)
    }
  }

  // Handle back button to cleanly leave voice room
  BackHandler {
    repository.leaveCurrentRoom()
    onLeaveRoom()
  }

  // Microphone permission request
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      repository.voiceEngine.setMuted(false)
      Toast.makeText(context, "Microphone enabled", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Microphone permission required for speaking", Toast.LENGTH_SHORT).show()
    }
  }

  fun toggleMicrophone() {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
      repository.voiceEngine.setMuted(!isMuted)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Room Background Wallpaper
    Image(
      painter = painterResource(id = R.drawable.bg_room_cyber_lounge),
      contentDescription = "Room Wallpaper",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // Dark gradient overlay for readability
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xCC080914),
              Color(0x99080914),
              Color(0xF0080914)
            )
          )
        )
    )

    Scaffold(
      topBar = {
        // Voice Room Header
        Surface(
          color = Color.Transparent,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            // Room metadata & owner
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              IconButton(
                onClick = {
                  repository.leaveCurrentRoom()
                  onLeaveRoom()
                }
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Leave Room",
                  tint = Color.White
                )
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceCard.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 2.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Column {
                    Text(
                      text = currentRoom?.title ?: "Livo Room",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "ID: ${currentRoom?.roomNumber ?: 0} • ${currentRoom?.listenersCount ?: 1} online",
                      color = LivoCyanLight,
                      fontSize = 10.sp
                    )
                  }
                }
              }
            }

            // Room controls (Audio status, Speaker toggle, Room Settings)
            Row(verticalAlignment = Alignment.CenterVertically) {
              // Voice connection state indicator
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (voiceState) {
                  VoiceConnectionState.CONNECTED -> SpeakingGreen.copy(alpha = 0.2f)
                  VoiceConnectionState.CONNECTING -> LivoGold.copy(alpha = 0.2f)
                  else -> LivoRose.copy(alpha = 0.2f)
                },
                modifier = Modifier.padding(end = 6.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(
                        when (voiceState) {
                          VoiceConnectionState.CONNECTED -> SpeakingGreen
                          VoiceConnectionState.CONNECTING -> LivoGold
                          else -> LivoRose
                        }
                      )
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (voiceState == VoiceConnectionState.CONNECTED) "Voice Live" else "Connecting",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              IconButton(
                onClick = { repository.voiceEngine.toggleSpeaker() },
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(DarkSurfaceElevated.copy(alpha = 0.8f))
              ) {
                Icon(
                  imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                  contentDescription = "Toggle Speaker",
                  tint = if (isSpeakerOn) LivoCyan else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(DarkSurfaceElevated.copy(alpha = 0.8f))
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Room Settings",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      },
      bottomBar = {
        // Bottom Action Bar: Mic toggle, Text Chat Input, Gift Button
        Surface(
          color = DarkSurface.copy(alpha = 0.95f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            // Microphone Toggle Button
            IconButton(
              onClick = { toggleMicrophone() },
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (!isMuted) SpeakingGreen else DarkSurfaceBorder)
            ) {
              Icon(
                imageVector = if (!isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Mic Toggle",
                tint = if (!isMuted) Color.White else TextSecondary,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Real-time Chat Input Field
            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it },
              placeholder = { Text("Say something...", fontSize = 12.sp, color = TextSecondary) },
              singleLine = true,
              shape = RoundedCornerShape(20.dp),
              trailingIcon = {
                if (messageInput.isNotBlank()) {
                  IconButton(
                    onClick = {
                      coroutineScope.launch {
                        repository.sendRoomTextMessage(messageInput)
                        messageInput = ""
                      }
                    }
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Send,
                      contentDescription = "Send",
                      tint = LivoVioletLight
                    )
                  }
                }
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedBorderColor = LivoViolet,
                unfocusedBorderColor = Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Gift Button with bouncing spark
            Button(
              onClick = { showGiftSheet = true },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LivoRose),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.height(46.dp)
            ) {
              Text(text = "🎁", fontSize = 18.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "Gift", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      },
      containerColor = Color.Transparent
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Room Announcement Banner
        if (!currentRoom?.announcement.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0x99171A37),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(text = "📢", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = currentRoom?.announcement ?: "",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Animated Gift Banner (Broadcasting when a gift is sent!)
        AnimatedVisibility(
          visible = latestGiftBanner != null,
          enter = slideInVertically() + fadeIn(),
          exit = slideOutVertically() + fadeOut()
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LivoRose.copy(alpha = 0.9f),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(10.dp)
            ) {
              Text(text = "✨", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = latestGiftBanner ?: "",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }

        // Section 9: 8 or 10 Seated Grid
        val seatCount = currentRoom?.seatCount ?: 8
        LazyVerticalGrid(
          columns = GridCells.Fixed(4),
          userScrollEnabled = false,
          contentPadding = PaddingValues(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(seats.take(seatCount)) { seat ->
            val isUserOnThisSeat = seat.userId == currentUser?.id
            val isSpeaking = if (isUserOnThisSeat) (!isMuted && speakingLevel > 0.05f) else seat.isSpeaking

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  if (seat.userId == null) {
                    coroutineScope.launch {
                      val success = repository.takeSeat(seat.seatIndex)
                      if (!success) {
                        Toast.makeText(context, "Seat is locked or taken", Toast.LENGTH_SHORT).show()
                      }
                    }
                  } else {
                    selectedSeatForModeration = seat
                  }
                }
                .padding(4.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(58.dp)
              ) {
                if (seat.userId != null) {
                  AvatarWithFrame(
                    avatarUrl = seat.userAvatar ?: "",
                    frameId = seat.equippedFrameId,
                    size = 52.dp,
                    isSpeaking = isSpeaking,
                    speakingLevel = if (isUserOnThisSeat) speakingLevel else 0.5f
                  )

                  // Mic mute badge on avatar
                  if (seat.isMuted || (isUserOnThisSeat && isMuted)) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(LivoRose)
                    ) {
                      Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                      )
                    }
                  }
                } else if (seat.isLocked) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(48.dp)
                      .clip(CircleShape)
                      .background(DarkSurfaceElevated.copy(alpha = 0.6f))
                  ) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = "Locked",
                      tint = TextSecondary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                } else {
                  // Empty seat with seat number
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(48.dp)
                      .clip(CircleShape)
                      .border(1.5.dp, DarkSurfaceBorder, CircleShape)
                      .background(DarkSurfaceCard.copy(alpha = 0.5f))
                  ) {
                    Text(
                      text = "${seat.seatIndex + 1}",
                      color = TextTertiary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = seat.userName ?: "Seat ${seat.seatIndex + 1}",
                color = if (seat.userId != null) TextPrimary else TextTertiary,
                fontSize = 11.sp,
                fontWeight = if (seat.userId != null) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 11: Realtime Room Chat Stream
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
        ) {
          LazyColumn(
            state = chatListState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(messages) { msg ->
              when (msg.messageType) {
                "GIFT" -> {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LivoRose.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivoRose.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text(text = msg.giftIcon ?: "🎁", fontSize = 18.sp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "${msg.senderName} ${msg.content}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
                "JOIN" -> {
                  Text(
                    text = "👋 ${msg.senderName} ${msg.content}",
                    color = LivoCyanLight,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                  )
                }
                else -> {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceCard.copy(alpha = 0.75f),
                    modifier = Modifier.widthIn(max = 300.dp)
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                      Text(
                        text = msg.senderName,
                        color = LivoVioletLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      )
                      Text(
                        text = msg.content,
                        color = TextPrimary,
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
    }
  }

  // Seat Action / Moderation Sheet
  if (selectedSeatForModeration != null) {
    val seat = selectedSeatForModeration!!
    val isOwner = currentRoom?.ownerId == currentUser?.id
    val isSuperAdmin = (currentUser?.role?.levelPriority ?: 0) >= UserRole.SUPER_ADMIN.levelPriority
    val canModerate = isOwner || isSuperAdmin
    val isOccupiedByMe = seat.userId == currentUser?.id

    ModalBottomSheet(
      onDismissRequest = { selectedSeatForModeration = null },
      containerColor = DarkSurfaceElevated
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        AvatarWithFrame(
          avatarUrl = seat.userAvatar ?: "",
          frameId = seat.equippedFrameId,
          size = 64.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = seat.userName ?: "Seat ${seat.seatIndex + 1}",
          color = TextPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Seat Position #${seat.seatIndex + 1}",
          color = TextTertiary,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Actions
        if (isOccupiedByMe) {
          LivoButton(
            text = "Leave Seat",
            onClick = {
              coroutineScope.launch {
                repository.leaveSeat(seat.seatIndex)
                selectedSeatForModeration = null
              }
            },
            containerColor = LivoRose
          )
        } else if (seat.userId != null) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Button(
              onClick = {
                val user = UserEntity(
                  id = seat.userId,
                  livoId = seat.userLivoId ?: 0,
                  email = "",
                  nickname = seat.userName ?: "",
                  avatarUrl = seat.userAvatar ?: "",
                  role = seat.userRole ?: UserRole.USER
                )
                selectedSeatForModeration = null
                onOpenUserProfile(user)
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LivoViolet),
              modifier = Modifier.weight(1f)
            ) {
              Text("View Profile")
            }

            Button(
              onClick = {
                val user = UserEntity(
                  id = seat.userId,
                  livoId = seat.userLivoId ?: 0,
                  email = "",
                  nickname = seat.userName ?: "",
                  avatarUrl = seat.userAvatar ?: "",
                  role = seat.userRole ?: UserRole.USER
                )
                targetUserForGift = user
                selectedSeatForModeration = null
                showGiftSheet = true
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = LivoRose),
              modifier = Modifier.weight(1f)
            ) {
              Text("Send Gift 🎁")
            }
          }
        }

        // Moderation controls for Room Owner / Admins
        if (canModerate) {
          Spacer(modifier = Modifier.height(14.dp))
          Divider(color = DarkSurfaceBorder)
          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Owner / Moderation Controls",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedButton(
              onClick = {
                coroutineScope.launch {
                  repository.moderateSeat(seat.seatIndex, lock = !seat.isLocked)
                  selectedSeatForModeration = null
                }
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(if (seat.isLocked) "Unlock Seat" else "Lock Seat", fontSize = 12.sp)
            }

            if (seat.userId != null) {
              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    repository.moderateSeat(seat.seatIndex, mute = !seat.isMuted)
                    selectedSeatForModeration = null
                  }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(if (seat.isMuted) "Unmute" else "Mute Mic", fontSize = 12.sp)
              }

              Button(
                onClick = {
                  coroutineScope.launch {
                    repository.moderateSeat(seat.seatIndex, kick = true)
                    selectedSeatForModeration = null
                  }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LivoRose),
                modifier = Modifier.weight(1f)
              ) {
                Text("Kick Seat", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }
  }

  // Section 15: Virtual Gift Picker Bottom Sheet
  if (showGiftSheet) {
    var selectedGift by remember { mutableStateOf(repository.giftCatalog.first()) }
    var selectedMultiplier by remember { mutableStateOf(1) }
    var isSendingGift by remember { mutableStateOf(false) }

    // Target recipient defaults to targetUserForGift or room owner
    val recipientName = targetUserForGift?.nickname ?: currentRoom?.ownerName ?: "Host"
    val recipientId = targetUserForGift?.id ?: currentRoom?.ownerId ?: ""

    ModalBottomSheet(
      onDismissRequest = {
        showGiftSheet = false
        targetUserForGift = null
      },
      containerColor = DarkSurfaceElevated
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(
              text = "Send Virtual Gift",
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
            Text(
              text = "To: $recipientName",
              color = LivoCyanLight,
              fontSize = 12.sp
            )
          }

          CoinPill(
            balance = wallet?.balance ?: 0,
            onClick = onOpenWallet
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gifts Grid
        LazyVerticalGrid(
          columns = GridCells.Fixed(4),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.height(200.dp)
        ) {
          items(repository.giftCatalog) { gift ->
            val isSelected = selectedGift.id == gift.id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) LivoViolet.copy(alpha = 0.3f) else DarkSurfaceCard,
              border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, LivoViolet) else null,
              modifier = Modifier
                .clickable { selectedGift = gift }
                .padding(2.dp)
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
              ) {
                Text(text = gift.iconEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = gift.name,
                  color = TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${gift.coinPrice} 🪙",
                  color = LivoGoldLight,
                  fontSize = 10.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quantity Selector: 1x, 10x, 66x, 99x, 520x
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf(1, 10, 66, 99, 520).forEach { count ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (selectedMultiplier == count) LivoRose else DarkSurfaceCard,
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

        val totalCost = selectedGift.coinPrice * selectedMultiplier

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(text = "Total Coins", color = TextSecondary, fontSize = 11.sp)
            Text(
              text = String.format("%,d 🪙", totalCost),
              color = LivoGold,
              fontSize = 17.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }

          Button(
            onClick = {
              if (recipientId.isBlank()) {
                Toast.makeText(context, "Select a recipient", Toast.LENGTH_SHORT).show()
                return@Button
              }
              isSendingGift = true
              coroutineScope.launch {
                val result = repository.sendGift(
                  receiverId = recipientId,
                  giftId = selectedGift.id,
                  count = selectedMultiplier
                )
                isSendingGift = false
                result.onSuccess {
                  latestGiftBanner = "${currentUser?.nickname} sent ${selectedMultiplier}x ${selectedGift.name} to $recipientName!"
                  showGiftSheet = false
                  targetUserForGift = null
                }.onFailure { err ->
                  Toast.makeText(context, err.message ?: "Gift failed", Toast.LENGTH_LONG).show()
                }
              }
            },
            enabled = !isSendingGift && (wallet?.balance ?: 0) >= totalCost,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LivoRose),
            modifier = Modifier.height(48.dp)
          ) {
            if (isSendingGift) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
              Text(
                text = if ((wallet?.balance ?: 0) < totalCost) "Recharge Required" else "Send Gift 🎁",
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }

  // Room Customization Dialog (Section 10)
  if (showSettingsDialog) {
    var editTitle by remember { mutableStateOf(currentRoom?.title ?: "") }
    var editAnnouncement by remember { mutableStateOf(currentRoom?.announcement ?: "") }

    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text("Room Customization", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            label = { Text("Room Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editAnnouncement,
            onValueChange = { editAnnouncement = it },
            label = { Text("Room Notice / Rules") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
          Text(
            text = "Only the room owner and authorized admins can modify room information.",
            color = TextTertiary,
            fontSize = 11.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              repository.updateRoomSettings(
                title = editTitle,
                announcement = editAnnouncement,
                coverUrl = currentRoom?.coverUrl ?: "",
                backgroundUrl = currentRoom?.backgroundUrl ?: ""
              )
              showSettingsDialog = false
              Toast.makeText(context, "Room settings updated", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoViolet)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSettingsDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}
