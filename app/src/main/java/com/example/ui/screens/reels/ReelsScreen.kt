package com.example.ui.screens.reels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ReelCommentEntity
import com.example.data.model.ReelEntity
import com.example.data.model.StoryEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.AvatarWithFrame
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
  repository: LivoRepository,
  initialReelIndex: Int = 0,
  onOpenSearch: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  onBackClick: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  val reels by repository.reels.collectAsState()
  val stories by repository.stories.collectAsState()
  val currentUser by repository.currentUser.collectAsState()

  var activeReelIndex by remember(initialReelIndex) { mutableStateOf(initialReelIndex) }
  val activeReel = reels.getOrNull(activeReelIndex % reels.size.coerceAtLeast(1))

  var selectedTab by remember { mutableStateOf("Reels") } // "Reels" or "Friends"
  var isPlaying by remember { mutableStateOf(true) }
  var isMuted by remember { mutableStateOf(false) }

  // Modals & Bottom Sheets
  var showUploadDialog by remember { mutableStateOf(false) }
  var showCommentsSheet by remember { mutableStateOf(false) }
  var showShareSheet by remember { mutableStateOf(false) }
  var showMoreOptionsSheet by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var activeStoryViewer by remember { mutableStateOf<StoryEntity?>(null) }
  var showAddStoryDialog by remember { mutableStateOf(false) }

  // Video Picker for Reels Upload
  var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedVideoUri = uri
      showUploadDialog = true
    }
  }

  // Photo/Media Picker for Stories
  var selectedStoryUri by remember { mutableStateOf<Uri?>(null) }
  val storyPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedStoryUri = uri
      showAddStoryDialog = true
    }
  }

  val GoldColor = Color(0xFFF5BA42)
  val DarkBg = Color.Black

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBg)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      // 1. TOP HEADER: L Voice Logo, Search bar, Notifications, Upload (+)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        // Crowned golden L + "L Voice" script
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { onBackClick?.invoke() }
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_l_voice_crown),
            contentDescription = "L Voice",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "L Voice",
            color = Color(0xFFF3C775),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
          )
        }

        // Center Search Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141419))
            .border(BorderStroke(0.8.dp, Color(0xFF282834)), RoundedCornerShape(20.dp))
            .clickable { onOpenSearch() }
            .padding(horizontal = 12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color(0xFF7A7A85),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Search...",
            color = Color(0xFF7A7A85),
            fontSize = 13.sp
          )
        }

        // Right Icons: Notifications & Upload (+)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsNone,
              contentDescription = "Notifications",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }

          // Upload (+) button
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E1E26),
            border = BorderStroke(1.dp, Color(0xFF383846)),
            modifier = Modifier
              .size(34.dp)
              .clickable {
                videoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
              }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Upload Reel",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // 2. SUB-TABS: "Reels", "Friends", and Filter Settings icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Reels Tab
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { selectedTab = "Reels" }
          ) {
            Text(
              text = "Reels",
              color = if (selectedTab == "Reels") Color.White else Color(0xFF7A7A85),
              fontWeight = if (selectedTab == "Reels") FontWeight.Bold else FontWeight.Medium,
              fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            if (selectedTab == "Reels") {
              Box(
                modifier = Modifier
                  .width(28.dp)
                  .height(2.5.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(GoldColor)
              )
            } else {
              Spacer(modifier = Modifier.height(2.5.dp))
            }
          }

          // Friends Tab
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { selectedTab = "Friends" }
          ) {
            Text(
              text = "Friends",
              color = if (selectedTab == "Friends") Color.White else Color(0xFF7A7A85),
              fontWeight = if (selectedTab == "Friends") FontWeight.Bold else FontWeight.Medium,
              fontSize = 17.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            // 3-dot friend indicator
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              repeat(3) {
                Box(
                  modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5A5A66))
                )
              }
            }
          }
        }

        // Filter / Equalizer sliders button
        IconButton(
          onClick = { showFilterDialog = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Filter",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 3. STORIES HORIZONTAL CAROUSEL
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Add Story (+) Button
        item {
          StoryCircleItem(
            name = "Add Story",
            isAddButton = true,
            onClick = {
              storyPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            }
          )
        }

        // Dynamic Stories from repository
        items(stories) { story ->
          StoryCircleItem(
            name = story.userName,
            drawableName = story.mediaDrawableName,
            isViewed = story.isViewed,
            onClick = {
              coroutineScope.launch {
                repository.markStoryViewed(story.id)
                activeStoryViewer = story
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 4. REELS VIDEO AREA (9:16 portrait card with rounded corners)
      if (activeReel != null) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF101016))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              isPlaying = !isPlaying
            }
        ) {
          // Reel Media Visual (matches screenshot's dreamer_girl exactly)
          val coverDrawableId = when (activeReel.coverDrawableName) {
            "reel_bg_dreamer_girl" -> R.drawable.reel_bg_dreamer_girl
            "reel_thumb_silence" -> R.drawable.reel_thumb_silence
            "reel_thumb_music" -> R.drawable.reel_thumb_music
            "reel_thumb_friendship" -> R.drawable.reel_thumb_friendship
            "reel_thumb_dreams" -> R.drawable.reel_thumb_dreams
            else -> R.drawable.reel_bg_dreamer_girl
          }

          Image(
            painter = painterResource(id = coverDrawableId),
            contentDescription = activeReel.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          // Smooth Vignette Gradient Overlays
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color(0x66000000),
                    Color.Transparent,
                    Color(0xCC000000)
                  )
                )
              )
          )

          // Top Badge: `▶ Reel` Pill & Mute button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            // Mute / Unmute Button
            Surface(
              shape = CircleShape,
              color = Color(0x66000000),
              modifier = Modifier
                .size(32.dp)
                .clickable { isMuted = !isMuted }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                  contentDescription = "Mute",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            // `▶ Reel` Pill Badge (as seen in screenshot)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0x77000000),
              modifier = Modifier.height(26.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Reel",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          // Center Pause Indicator
          if (!isPlaying) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x88000000))
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Paused",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
              )
            }
          }

          // RIGHT-SIDE ACTION BUTTONS: Like, Comment, Share, Save, More
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(end = 12.dp, bottom = 26.dp)
          ) {
            // 1. LIKE BUTTON
            ActionColumnItem(
              icon = {
                val scale by animateFloatAsState(
                  targetValue = if (activeReel.isLiked) 1.25f else 1.0f,
                  animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                  label = "like_scale"
                )
                Icon(
                  imageVector = if (activeReel.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "Like",
                  tint = if (activeReel.isLiked) Color(0xFFF43F5E) else Color.White,
                  modifier = Modifier
                    .size(28.dp)
                    .scale(scale)
                )
              },
              count = formatCount(activeReel.likesCount),
              onClick = {
                coroutineScope.launch { repository.toggleLikeReel(activeReel.id) }
              }
            )

            // 2. COMMENT BUTTON
            ActionColumnItem(
              icon = {
                Icon(
                  imageVector = Icons.Default.ChatBubbleOutline,
                  contentDescription = "Comment",
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
              },
              count = formatCount(activeReel.commentsCount),
              onClick = { showCommentsSheet = true }
            )

            // 3. SHARE BUTTON
            ActionColumnItem(
              icon = {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Share",
                  tint = Color.White,
                  modifier = Modifier.size(25.dp)
                )
              },
              count = formatCount(activeReel.sharesCount),
              onClick = { showShareSheet = true }
            )

            // 4. SAVE / BOOKMARK BUTTON
            ActionColumnItem(
              icon = {
                Icon(
                  imageVector = if (activeReel.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = "Save",
                  tint = if (activeReel.isSaved) GoldColor else Color.White,
                  modifier = Modifier.size(26.dp)
                )
              },
              count = null,
              onClick = {
                coroutineScope.launch {
                  repository.toggleBookmarkReel(activeReel.id)
                  val msg = if (!activeReel.isSaved) "Saved to your bookmarks" else "Removed from bookmarks"
                  Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
              }
            )

            // 5. MORE OPTIONS BUTTON (•••)
            ActionColumnItem(
              icon = {
                Icon(
                  imageVector = Icons.Default.MoreHoriz,
                  contentDescription = "More Options",
                  tint = Color.White,
                  modifier = Modifier.size(26.dp)
                )
              },
              count = null,
              onClick = { showMoreOptionsSheet = true }
            )
          }

          // BOTTOM VIDEO INFORMATION OVERLAY
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(start = 14.dp, bottom = 22.dp, end = 85.dp)
          ) {
            // Creator info & Follow button
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Creator Avatar with story ring
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.sweepGradient(
                      listOf(Color(0xFFF5BA42), Color(0xFFF43F5E), Color(0xFF7C3AED), Color(0xFFF5BA42))
                    )
                  )
                  .padding(2.dp)
              ) {
                Image(
                  painter = painterResource(id = R.drawable.reel_bg_dreamer_girl),
                  contentDescription = activeReel.creatorName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Text(
                text = activeReel.creatorName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )

              Spacer(modifier = Modifier.width(8.dp))

              // Follow / Following button (matches screenshot)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (activeReel.isFollowing) Color(0x33FFFFFF) else Color.Transparent,
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier
                  .clickable {
                    coroutineScope.launch { repository.toggleFollowCreator(activeReel.creatorName) }
                  }
              ) {
                Text(
                  text = if (activeReel.isFollowing) "Following" else "Follow",
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
              text = "${activeReel.caption} ${activeReel.hashtags}",
              color = Color.White,
              fontSize = 13.sp,
              lineHeight = 17.sp,
              fontWeight = FontWeight.Normal,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Original audio / sound track
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x44000000))
                .clickable {
                  Toast.makeText(context, "Sound: ${activeReel.musicTitle}", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Audio",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = activeReel.musicTitle,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
              )
            }
          }

          // Next / Previous Video Quick Switcher Floating Arrows
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .padding(end = 8.dp)
          ) {
            IconButton(
              onClick = {
                if (reels.isNotEmpty()) {
                  activeReelIndex = if (activeReelIndex > 0) activeReelIndex - 1 else reels.size - 1
                  isPlaying = true
                }
              },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x44000000))
            ) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous Reel",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = {
                if (reels.isNotEmpty()) {
                  activeReelIndex = (activeReelIndex + 1) % reels.size
                  isPlaying = true
                }
              },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0x44000000))
            ) {
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Next Reel",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }

    // =========================================================================
    // MODALS & BOTTOM SHEETS
    // =========================================================================

    // 1. Comments Bottom Sheet
    if (showCommentsSheet && activeReel != null) {
      ReelCommentsBottomSheet(
        reelId = activeReel.id,
        repository = repository,
        onDismiss = { showCommentsSheet = false }
      )
    }

    // 2. Share Bottom Sheet
    if (showShareSheet && activeReel != null) {
      ReelShareBottomSheet(
        reel = activeReel,
        repository = repository,
        onDismiss = { showShareSheet = false }
      )
    }

    // 3. More Options Bottom Sheet
    if (showMoreOptionsSheet && activeReel != null) {
      ReelMoreOptionsBottomSheet(
        reel = activeReel,
        onDismiss = { showMoreOptionsSheet = false }
      )
    }

    // 4. Video Upload Dialog (Section 2 requirement)
    if (showUploadDialog) {
      ReelUploadDialog(
        videoUri = selectedVideoUri,
        repository = repository,
        onDismiss = {
          showUploadDialog = false
          selectedVideoUri = null
        },
        onUploadSuccess = {
          showUploadDialog = false
          selectedVideoUri = null
          activeReelIndex = 0
          Toast.makeText(context, "Reel published successfully! 🎉", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // 5. Add Story Dialog
    if (showAddStoryDialog) {
      AddStoryDialog(
        mediaUri = selectedStoryUri,
        repository = repository,
        onDismiss = {
          showAddStoryDialog = false
          selectedStoryUri = null
        },
        onStoryPublished = {
          showAddStoryDialog = false
          selectedStoryUri = null
          Toast.makeText(context, "Story added to your profile! 🌟", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // 6. Story Viewer Dialog
    if (activeStoryViewer != null) {
      StoryViewerDialog(
        story = activeStoryViewer!!,
        onDismiss = { activeStoryViewer = null }
      )
    }

    // 7. Filter & Settings Dialog
    if (showFilterDialog) {
      ReelsFilterDialog(
        currentFilter = selectedTab,
        onSelectFilter = { filter ->
          selectedTab = filter
          showFilterDialog = false
        },
        onDismiss = { showFilterDialog = false }
      )
    }
  }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================

@Composable
private fun StoryCircleItem(
  name: String,
  drawableName: String? = null,
  isAddButton: Boolean = false,
  isViewed: Boolean = false,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .width(58.dp)
  ) {
    if (isAddButton) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color(0xFF16151E))
          .border(BorderStroke(1.2.dp, Color(0xFF383846)), CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Story",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }
    } else {
      // Ring Gradient (Pink/Gold for unseen, subtle Gray for viewed)
      val ringBrush = if (!isViewed) {
        Brush.sweepGradient(
          listOf(
            Color(0xFFF5BA42),
            Color(0xFFF43F5E),
            Color(0xFF9333EA),
            Color(0xFF06B6D4),
            Color(0xFFF5BA42)
          )
        )
      } else {
        Brush.sweepGradient(listOf(Color(0xFF3E3E4C), Color(0xFF2E2E3A)))
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(ringBrush)
          .padding(2.5.dp)
      ) {
        val imageRes = when (drawableName) {
          "story_avatar_daisy" -> R.drawable.story_avatar_daisy
          "story_avatar_butterfly" -> R.drawable.story_avatar_butterfly
          "reel_bg_dreamer_girl" -> R.drawable.reel_bg_dreamer_girl
          "reel_thumb_silence" -> R.drawable.reel_thumb_silence
          "reel_thumb_music" -> R.drawable.reel_thumb_music
          else -> R.drawable.story_avatar_butterfly
        }

        Image(
          painter = painterResource(id = imageRes),
          contentDescription = name,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = name,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ActionColumnItem(
  icon: @Composable () -> Unit,
  count: String?,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    icon()
    if (count != null) {
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = count,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

// =============================================================================
// COMMENTS BOTTOM SHEET
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelCommentsBottomSheet(
  reelId: String,
  repository: LivoRepository,
  onDismiss: () -> Unit
) {
  val comments by repository.observeComments(reelId).collectAsState(initial = emptyList())
  val coroutineScope = rememberCoroutineScope()
  var newCommentText by remember { mutableStateOf("") }
  val currentUser by repository.currentUser.collectAsState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    contentColor = Color.White,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Comments (${comments.size})",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
        }
      }

      HorizontalDivider(color = Color(0xFF282834), modifier = Modifier.padding(vertical = 8.dp))

      // Comments List
      if (comments.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          Text("No comments yet. Be the first to comment!", color = Color.Gray, fontSize = 13.sp)
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(comments) { comment ->
            Row(
              verticalAlignment = Alignment.Top,
              modifier = Modifier.fillMaxWidth()
            ) {
              AvatarWithFrame(
                avatarUrl = comment.userAvatar,
                size = 34.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = comment.userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "just now",
                    color = Color.Gray,
                    fontSize = 11.sp
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = comment.content,
                  color = Color(0xFFE2E8F0),
                  fontSize = 13.sp,
                  lineHeight = 17.sp
                )
              }

              // Like comment
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                  onClick = {
                    coroutineScope.launch { repository.toggleLikeComment(reelId, comment.id) }
                  },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (comment.isLiked) Color(0xFFF43F5E) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                  )
                }
                if (comment.likesCount > 0) {
                  Text("${comment.likesCount}", color = Color.Gray, fontSize = 10.sp)
                }
              }
            }
          }
        }
      }

      // Add Comment Input Row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 10.dp)
      ) {
        OutlinedTextField(
          value = newCommentText,
          onValueChange = { newCommentText = it },
          placeholder = { Text("Add a comment...", color = Color.Gray, fontSize = 13.sp) },
          singleLine = true,
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E1E28),
            unfocusedContainerColor = Color(0xFF1E1E28),
            focusedBorderColor = Color(0xFFF5BA42),
            unfocusedBorderColor = Color(0xFF383846)
          ),
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (newCommentText.isNotBlank()) {
              coroutineScope.launch {
                repository.addComment(reelId, newCommentText)
                newCommentText = ""
              }
            }
          },
          enabled = newCommentText.isNotBlank()
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Post",
            tint = if (newCommentText.isNotBlank()) Color(0xFFF5BA42) else Color.Gray
          )
        }
      }
    }
  }
}

// =============================================================================
// SHARE BOTTOM SHEET
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelShareBottomSheet(
  reel: ReelEntity,
  repository: LivoRepository,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    contentColor = Color.White
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text("Share Reel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
      Spacer(modifier = Modifier.height(16.dp))

      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Native System Share
        ShareActionItem(
          icon = Icons.Default.Share,
          label = "Share Via...",
          onClick = {
            coroutineScope.launch { repository.incrementShareCount(reel.id) }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Watch ${reel.creatorName}'s Reel on L Voice")
              putExtra(Intent.EXTRA_TEXT, "Check out this reel by ${reel.creatorName} on L Voice: ${reel.caption}\nhttps://lvoice.app/reels/${reel.id}")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
            onDismiss()
          }
        )

        // Copy Link
        ShareActionItem(
          icon = Icons.Default.Link,
          label = "Copy Link",
          onClick = {
            coroutineScope.launch { repository.incrementShareCount(reel.id) }
            clipboardManager.setText(AnnotatedString("https://lvoice.app/reels/${reel.id}"))
            Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
            onDismiss()
          }
        )

        // Save Video
        ShareActionItem(
          icon = Icons.Default.Download,
          label = "Save Video",
          onClick = {
            Toast.makeText(context, "Video saved to device gallery!", Toast.LENGTH_SHORT).show()
            onDismiss()
          }
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun ShareActionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape)
        .background(Color(0xFF262534))
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = label, color = Color.White, fontSize = 12.sp)
  }
}

// =============================================================================
// MORE OPTIONS BOTTOM SHEET
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelMoreOptionsBottomSheet(
  reel: ReelEntity,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    contentColor = Color.White
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text("Reel Options", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
      Spacer(modifier = Modifier.height(14.dp))

      MoreOptionRow(
        icon = Icons.Default.VisibilityOff,
        title = "Not Interested",
        subtitle = "See fewer reels like this in your feed",
        onClick = {
          Toast.makeText(context, "We will show fewer reels like this.", Toast.LENGTH_SHORT).show()
          onDismiss()
        }
      )

      MoreOptionRow(
        icon = Icons.Default.ReportProblem,
        title = "Report Reel",
        subtitle = "Report inappropriate or harmful content",
        tint = Color(0xFFF43F5E),
        onClick = {
          Toast.makeText(context, "Reel reported to L Voice moderators.", Toast.LENGTH_SHORT).show()
          onDismiss()
        }
      )

      MoreOptionRow(
        icon = Icons.Default.MusicNote,
        title = "Sound Details",
        subtitle = reel.musicTitle,
        onClick = {
          Toast.makeText(context, "Audio track: ${reel.musicTitle}", Toast.LENGTH_SHORT).show()
          onDismiss()
        }
      )
    }
  }
}

@Composable
private fun MoreOptionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  tint: Color = Color.White,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.width(14.dp))
    Column {
      Text(text = title, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      Text(text = subtitle, color = Color.Gray, fontSize = 11.sp)
    }
  }
}

// =============================================================================
// VIDEO UPLOAD DIALOG (Section 2 Requirement)
// =============================================================================

@Composable
private fun ReelUploadDialog(
  videoUri: Uri?,
  repository: LivoRepository,
  onDismiss: () -> Unit,
  onUploadSuccess: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var caption by remember { mutableStateOf("") }
  var hashtags by remember { mutableStateOf("#lvoice #sunset #vibes") }
  var selectedPrivacy by remember { mutableStateOf("PUBLIC") }
  var isUploading by remember { mutableStateOf(false) }
  var uploadProgress by remember { mutableStateOf(0.0f) }

  AlertDialog(
    onDismissRequest = { if (!isUploading) onDismiss() },
    containerColor = Color(0xFF14131A),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(id = R.drawable.ic_reel),
          contentDescription = null,
          tint = Color(0xFFF5BA42),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create New Reel", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Thumbnail Preview
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF20202A))
        ) {
          Image(
            painter = painterResource(id = R.drawable.reel_bg_dreamer_girl),
            contentDescription = "Selected Video Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x88000000),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
          ) {
            Text("0:15 HD", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
          }
        }

        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it },
          label = { Text("Caption") },
          placeholder = { Text("Describe your reel...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = hashtags,
          onValueChange = { hashtags = it },
          label = { Text("Hashtags") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Privacy Selector
        Text("Audience Privacy", color = Color.Gray, fontSize = 12.sp)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf("PUBLIC", "FRIENDS", "PRIVATE").forEach { privacy ->
            FilterChip(
              selected = selectedPrivacy == privacy,
              onClick = { selectedPrivacy = privacy },
              label = { Text(privacy, fontSize = 11.sp) }
            )
          }
        }

        // Upload progress bar when publishing
        if (isUploading) {
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { uploadProgress },
            color = Color(0xFFF5BA42),
            modifier = Modifier.fillMaxWidth()
          )
          Text("Uploading and encoding video: ${(uploadProgress * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (caption.isNotBlank()) {
            isUploading = true
            coroutineScope.launch {
              // Simulated encoding & chunk upload
              repeat(10) { i ->
                delay(80)
                uploadProgress = (i + 1) * 0.1f
              }
              repository.uploadReel(
                caption = caption,
                videoUri = videoUri?.toString() ?: "",
                hashtags = hashtags,
                privacy = selectedPrivacy
              )
              isUploading = false
              onUploadSuccess()
            }
          }
        },
        enabled = caption.isNotBlank() && !isUploading,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5BA42))
      ) {
        Text("Publish Reel", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      if (!isUploading) {
        TextButton(onClick = onDismiss) {
          Text("Cancel", color = Color.Gray)
        }
      }
    }
  )
}

// =============================================================================
// STORY VIEWER & ADD STORY DIALOGS
// =============================================================================

@Composable
private fun AddStoryDialog(
  mediaUri: Uri?,
  repository: LivoRepository,
  onDismiss: () -> Unit,
  onStoryPublished: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var storyCaption by remember { mutableStateOf("") }
  var isPublishing by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    title = {
      Text("Post to Your Story", color = Color.White, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222030))
        ) {
          Image(
            painter = painterResource(id = R.drawable.reel_bg_dreamer_girl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        OutlinedTextField(
          value = storyCaption,
          onValueChange = { storyCaption = it },
          label = { Text("Story Caption (Optional)") },
          placeholder = { Text("Add words...") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          isPublishing = true
          coroutineScope.launch {
            repository.addStory(
              mediaUri = mediaUri?.toString() ?: "",
              caption = storyCaption
            )
            isPublishing = false
            onStoryPublished()
          }
        },
        enabled = !isPublishing,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5BA42))
      ) {
        Text("Share Story", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color.Gray)
      }
    }
  )
}

@Composable
private fun StoryViewerDialog(
  story: StoryEntity,
  onDismiss: () -> Unit
) {
  var progress by remember { mutableStateOf(0.0f) }

  LaunchedEffect(story.id) {
    while (progress < 1.0f) {
      delay(50)
      progress += 0.015f
    }
    onDismiss()
  }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(20.dp))
        .background(Color.Black)
    ) {
      val imageRes = when (story.mediaDrawableName) {
        "story_avatar_daisy" -> R.drawable.story_avatar_daisy
        "story_avatar_butterfly" -> R.drawable.story_avatar_butterfly
        "reel_bg_dreamer_girl" -> R.drawable.reel_bg_dreamer_girl
        "reel_thumb_silence" -> R.drawable.reel_thumb_silence
        "reel_thumb_music" -> R.drawable.reel_thumb_music
        else -> R.drawable.story_avatar_butterfly
      }

      Image(
        painter = painterResource(id = imageRes),
        contentDescription = story.caption,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Top progress timer bar & author
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
      ) {
        LinearProgressIndicator(
          progress = { progress },
          color = Color.White,
          trackColor = Color(0x55FFFFFF),
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = imageRes),
              contentDescription = story.userName,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(story.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
      }

      // Bottom story caption
      if (story.caption.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0x88000000),
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
        ) {
          Text(
            text = story.caption,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ReelsFilterDialog(
  currentFilter: String,
  onSelectFilter: (String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF14131A),
    title = {
      Text("Feed Preferences", color = Color.White, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Reels", "Friends", "Trending Hot", "Music Vibes", "Visual Poetry").forEach { opt ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentFilter == opt) Color(0xFF2E2414) else Color(0xFF1E1E28),
            border = BorderStroke(
              1.dp,
              if (currentFilter == opt) Color(0xFFF5BA42) else Color.Transparent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectFilter(opt) }
              .padding(10.dp)
          ) {
            Text(
              text = opt,
              color = if (currentFilter == opt) Color(0xFFF5BA42) else Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done", color = Color(0xFFF5BA42))
      }
    }
  )
}

private fun formatCount(count: Long): String {
  return when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
    count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
    else -> "$count"
  }
}
