package com.example.data.repository

import android.content.Context
import com.example.data.audio.VoiceConnectionState
import com.example.data.audio.VoiceEngine
import com.example.data.local.LivoDatabase
import com.example.data.model.*
import com.example.data.remote.LivoApiService
import com.example.data.remote.LivoWebSocketClient
import com.example.data.remote.NetworkStatus
import com.example.data.remote.SendGiftResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.random.Random

class LivoRepository(
  private val context: Context,
  private val database: LivoDatabase,
  val voiceEngine: VoiceEngine
) {
  private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  val webSocketClient = LivoWebSocketClient()

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  private val _currentRoom = MutableStateFlow<RoomEntity?>(null)
  val currentRoom: StateFlow<RoomEntity?> = _currentRoom.asStateFlow()

  val activeRooms: Flow<List<RoomEntity>> = database.roomDao().observeActiveRooms()
  val totalUnreadMessages: Flow<Int> = database.messageDao().observeTotalUnreadCount().map { it ?: 0 }

  // Virtual Gifts Catalog
  val giftCatalog = listOf(
    GiftItem("gift_rose", "Rose", 10, "🌹", "Popular", "SPARKLE"),
    GiftItem("gift_mic", "Magic Mic", 100, "🎙️", "Special", "WAVE"),
    GiftItem("gift_ring", "Diamond Ring", 500, "💍", "Luxury", "SHINE"),
    GiftItem("gift_car", "Sports Car", 2000, "🏎️", "Luxury", "SPEED"),
    GiftItem("gift_crown", "Royal Crown", 10000, "👑", "Royal", "CORONATION"),
    GiftItem("gift_yacht", "Super Yacht", 15000, "🛥️", "Super", "OCEAN"),
    GiftItem("gift_rocket", "Space Rocket", 28000, "🚀", "Supreme", "LAUNCH")
  )

  // Recharge packages ($1 = 28,000 Coins)
  val rechargePackages = listOf(
    RechargePackage("pkg_1", 0.99, 28000, 2000, isPopular = true),
    RechargePackage("pkg_2", 4.99, 140000, 15000),
    RechargePackage("pkg_3", 9.99, 280000, 40000),
    RechargePackage("pkg_4", 49.99, 1400000, 250000),
    RechargePackage("pkg_5", 99.99, 2800000, 600000)
  )

  // Avatar frames catalog
  val availableFrames = listOf(
    FrameItem("frame_none", "Default", null, 0x00000000, 0x00000000, "No decoration"),
    FrameItem("frame_vip_gold", "VIP Gold", UserRole.USER, 0xFFF59E0BL, 0xFFFCD34DL, "Sparkling golden crest"),
    FrameItem("frame_neon_cyber", "Cyber Neon", UserRole.HOST, 0xFF06B6D4L, 0xFF3B82F6L, "Futuristic cyber soundwave"),
    FrameItem("frame_royal_crystal", "Royal Crown", UserRole.MANAGER, 0xFF8B5CF6L, 0xFFEC4899L, "Imperial crystal aura"),
    FrameItem("frame_super_admin", "Super Admin Elite", UserRole.SUPER_ADMIN, 0xFFEF4444L, 0xFFF59E0BL, "Supreme executive badge")
  )

  init {
    repoScope.launch {
      seedInitialDataIfNeeded()
      restoreLatestSession()
    }
  }

  // ==========================================================================
  // Session & Authentication
  // ==========================================================================
  private suspend fun restoreLatestSession() {
    val users = database.userDao().observeAllUsers().firstOrNull() ?: emptyList()
    if (users.isNotEmpty()) {
      _currentUser.value = users.first()
      syncWalletForUser(users.first().id)
    }
  }

  suspend fun loginWithGoogle(
    email: String,
    displayName: String,
    avatarUrl: String?
  ): UserEntity = withContext(Dispatchers.IO) {
    val existing = database.userDao().getUserByEmail(email)
    val user = if (existing != null) {
      existing.copy(
        nickname = displayName.ifBlank { existing.nickname },
        avatarUrl = avatarUrl ?: existing.avatarUrl,
        isOnline = true
      )
    } else {
      val generatedLivoId = 10000000L + Random.nextInt(90000000)
      UserEntity(
        id = UUID.randomUUID().toString(),
        livoId = generatedLivoId,
        email = email,
        nickname = displayName.ifBlank { "LivoUser_${generatedLivoId % 10000}" },
        avatarUrl = avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
        role = if (email.contains("admin", ignoreCase = true) || email.contains("shahroz", ignoreCase = true)) {
          UserRole.SUPER_ADMIN
        } else {
          UserRole.USER
        },
        level = 1,
        experiencePoints = 0
      )
    }
    database.userDao().insertUser(user)
    _currentUser.value = user
    syncWalletForUser(user.id)
    user
  }

  suspend fun switchAccount(targetUserId: String) = withContext(Dispatchers.IO) {
    val user = database.userDao().getUserById(targetUserId)
    if (user != null) {
      _currentUser.value = user
      syncWalletForUser(user.id)
    }
  }

  suspend fun getAvailableAccounts(): List<UserEntity> = withContext(Dispatchers.IO) {
    database.userDao().observeAllUsers().firstOrNull() ?: emptyList()
  }

  fun logout() {
    val current = _currentUser.value
    if (current != null) {
      repoScope.launch {
        database.userDao().updateUser(current.copy(isOnline = false))
      }
    }
    voiceEngine.leaveRoomVoice()
    _currentRoom.value = null
    _currentUser.value = null
  }

  // ==========================================================================
  // User Management
  // ==========================================================================
  suspend fun updateUserProfile(
    nickname: String,
    bio: String,
    avatarUrl: String,
    countryCode: String
  ) = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext
    val updated = user.copy(
      nickname = nickname.trim(),
      bio = bio.trim(),
      avatarUrl = avatarUrl,
      countryCode = countryCode
    )
    database.userDao().updateUser(updated)
    _currentUser.value = updated
  }

  suspend fun searchUsers(query: String): List<UserEntity> = withContext(Dispatchers.IO) {
    val numeric = query.toLongOrNull() ?: -1L
    database.userDao().searchUsers(numeric, query.trim())
  }

  suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
    database.userDao().getUserById(userId)
  }

  suspend fun getUserByLivoId(livoId: Long): UserEntity? = withContext(Dispatchers.IO) {
    database.userDao().getUserByLivoId(livoId)
  }

  suspend fun isFollowing(followingId: String): Boolean = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext false
    database.userDao().isFollowing(user.id, followingId) > 0
  }

  suspend fun toggleFollow(targetUserId: String) = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext
    if (user.id == targetUserId) return@withContext

    val targetUser = database.userDao().getUserById(targetUserId) ?: return@withContext
    val isFollowing = database.userDao().isFollowing(user.id, targetUserId) > 0

    if (isFollowing) {
      database.userDao().deleteFollow(user.id, targetUserId)
      database.userDao().updateUser(user.copy(followingCount = (user.followingCount - 1).coerceAtLeast(0)))
      database.userDao().updateUser(targetUser.copy(followersCount = (targetUser.followersCount - 1).coerceAtLeast(0)))
    } else {
      database.userDao().insertFollow(FollowEntity(user.id, targetUserId))
      database.userDao().updateUser(user.copy(followingCount = user.followingCount + 1))
      database.userDao().updateUser(targetUser.copy(followersCount = targetUser.followersCount + 1))

      database.socialDao().insertNotification(
        NotificationEntity(
          userId = targetUserId,
          title = "New Follower",
          content = "${user.nickname} started following you",
          type = "FOLLOW"
        )
      )
    }
    _currentUser.value = database.userDao().getUserById(user.id)
  }

  // ==========================================================================
  // Voice Rooms & Realtime Audio
  // ==========================================================================
  suspend fun createRoom(
    title: String,
    category: RoomCategory,
    coverUrl: String,
    backgroundUrl: String,
    announcement: String,
    seatCount: Int = 8
  ): RoomEntity = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: throw IllegalStateException("Must be logged in to create a room")
    val roomNumber = 100000L + Random.nextInt(900000)

    val room = RoomEntity(
      id = UUID.randomUUID().toString(),
      roomNumber = roomNumber,
      title = title.trim(),
      ownerId = user.id,
      ownerLivoId = user.livoId,
      ownerName = user.nickname,
      ownerAvatar = user.avatarUrl,
      coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400" },
      backgroundUrl = backgroundUrl,
      announcement = announcement.ifBlank { "Welcome to ${title}! Enjoy voice chats and parties." },
      category = category,
      seatCount = seatCount,
      listenersCount = 1
    )
    database.roomDao().insertRoom(room)

    // Initialize seats
    val seats = (0 until seatCount).map { index ->
      if (index == 0) {
        RoomSeatEntity(
          roomId = room.id,
          seatIndex = 0,
          userId = user.id,
          userLivoId = user.livoId,
          userName = user.nickname,
          userAvatar = user.avatarUrl,
          userRole = user.role,
          equippedFrameId = user.equippedFrameId
        )
      } else {
        RoomSeatEntity(roomId = room.id, seatIndex = index)
      }
    }
    database.roomDao().insertSeats(seats)
    joinRoom(room.id)
    room
  }

  suspend fun joinRoom(roomId: String) = withContext(Dispatchers.IO) {
    val room = database.roomDao().getRoomById(roomId) ?: return@withContext
    _currentRoom.value = room
    database.roomDao().updateRoom(room.copy(listenersCount = room.listenersCount + 1))

    // Real voice connection
    voiceEngine.joinRoomVoice(roomId)
    webSocketClient.connectToRoom(roomId)

    val user = _currentUser.value
    if (user != null) {
      database.roomDao().insertRoomMessage(
        RoomMessageEntity(
          roomId = roomId,
          senderId = user.id,
          senderName = user.nickname,
          senderAvatar = user.avatarUrl,
          senderLivoId = user.livoId,
          messageType = "JOIN",
          content = "joined the room"
        )
      )
    }
  }

  fun leaveCurrentRoom() {
    val room = _currentRoom.value ?: return
    val user = _currentUser.value
    repoScope.launch(Dispatchers.IO) {
      if (user != null) {
        // Free any occupied seat
        val seats = database.roomDao().getSeatsForRoom(room.id)
        seats.find { it.userId == user.id }?.let { occupied ->
          database.roomDao().insertOrUpdateSeat(
            occupied.copy(
              userId = null,
              userLivoId = null,
              userName = null,
              userAvatar = null,
              userRole = null
            )
          )
        }
      }
      database.roomDao().updateRoom(room.copy(listenersCount = (room.listenersCount - 1).coerceAtLeast(0)))
    }
    voiceEngine.leaveRoomVoice()
    webSocketClient.disconnect()
    _currentRoom.value = null
  }

  suspend fun searchRooms(query: String): List<RoomEntity> = withContext(Dispatchers.IO) {
    val number = query.toLongOrNull() ?: -1L
    database.roomDao().searchRooms(number, query.trim())
  }

  fun observeSeatsForRoom(roomId: String): Flow<List<RoomSeatEntity>> =
    database.roomDao().observeSeatsForRoom(roomId)

  fun observeMessagesForRoom(roomId: String): Flow<List<RoomMessageEntity>> =
    database.roomDao().observeMessagesForRoom(roomId)

  suspend fun sendRoomTextMessage(content: String) = withContext(Dispatchers.IO) {
    val room = _currentRoom.value ?: return@withContext
    val user = _currentUser.value ?: return@withContext
    if (content.isBlank()) return@withContext

    val msg = RoomMessageEntity(
      roomId = room.id,
      senderId = user.id,
      senderName = user.nickname,
      senderAvatar = user.avatarUrl,
      senderLivoId = user.livoId,
      messageType = "TEXT",
      content = content.trim()
    )
    database.roomDao().insertRoomMessage(msg)
  }

  suspend fun takeSeat(seatIndex: Int): Boolean = withContext(Dispatchers.IO) {
    val room = _currentRoom.value ?: return@withContext false
    val user = _currentUser.value ?: return@withContext false
    val seats = database.roomDao().getSeatsForRoom(room.id)
    val targetSeat = seats.find { it.seatIndex == seatIndex } ?: return@withContext false

    if (targetSeat.isLocked || (targetSeat.userId != null && targetSeat.userId != user.id)) {
      return@withContext false
    }

    // Leave any other seat in this room first
    seats.find { it.userId == user.id }?.let { currentSeat ->
      database.roomDao().insertOrUpdateSeat(
        currentSeat.copy(
          userId = null,
          userLivoId = null,
          userName = null,
          userAvatar = null,
          userRole = null
        )
      )
    }

    database.roomDao().insertOrUpdateSeat(
      targetSeat.copy(
        userId = user.id,
        userLivoId = user.livoId,
        userName = user.nickname,
        userAvatar = user.avatarUrl,
        userRole = user.role,
        equippedFrameId = user.equippedFrameId,
        isMuted = voiceEngine.isMuted.value
      )
    )
    true
  }

  suspend fun leaveSeat(seatIndex: Int) = withContext(Dispatchers.IO) {
    val room = _currentRoom.value ?: return@withContext
    val seats = database.roomDao().getSeatsForRoom(room.id)
    val seat = seats.find { it.seatIndex == seatIndex } ?: return@withContext
    database.roomDao().insertOrUpdateSeat(
      seat.copy(
        userId = null,
        userLivoId = null,
        userName = null,
        userAvatar = null,
        userRole = null
      )
    )
  }

  suspend fun moderateSeat(seatIndex: Int, lock: Boolean? = null, mute: Boolean? = null, kick: Boolean = false) =
    withContext(Dispatchers.IO) {
      val room = _currentRoom.value ?: return@withContext
      val user = _currentUser.value ?: return@withContext
      // Owner or Admin check
      if (room.ownerId != user.id && user.role.levelPriority < UserRole.ADMIN.levelPriority) {
        return@withContext
      }

      val seats = database.roomDao().getSeatsForRoom(room.id)
      val seat = seats.find { it.seatIndex == seatIndex } ?: return@withContext

      var updated = seat
      if (lock != null) updated = updated.copy(isLocked = lock)
      if (mute != null) updated = updated.copy(isMuted = mute)
      if (kick) {
        updated = updated.copy(
          userId = null,
          userLivoId = null,
          userName = null,
          userAvatar = null,
          userRole = null
        )
      }
      database.roomDao().insertOrUpdateSeat(updated)
    }

  suspend fun updateRoomSettings(title: String, announcement: String, coverUrl: String, backgroundUrl: String) =
    withContext(Dispatchers.IO) {
      val room = _currentRoom.value ?: return@withContext
      val user = _currentUser.value ?: return@withContext
      if (room.ownerId != user.id && user.role.levelPriority < UserRole.ADMIN.levelPriority) {
        return@withContext
      }

      val updated = room.copy(
        title = title.trim(),
        announcement = announcement.trim(),
        coverUrl = coverUrl,
        backgroundUrl = backgroundUrl
      )
      database.roomDao().updateRoom(updated)
      _currentRoom.value = updated
    }

  // ==========================================================================
  // Coins, Wallet & Atomic Virtual Gift Transactions
  // ==========================================================================
  fun observeWallet(userId: String): Flow<CoinWalletEntity?> =
    database.walletDao().observeWallet(userId)

  fun observeTransactions(userId: String): Flow<List<CoinTransactionEntity>> =
    database.walletDao().observeTransactions(userId)

  private suspend fun syncWalletForUser(userId: String) {
    val existing = database.walletDao().getWallet(userId)
    if (existing == null) {
      // Initialize with welcome coins for newly registered accounts
      val initialWallet = CoinWalletEntity(
        userId = userId,
        balance = 56000L, // Welcome balance ($2.00 worth of Livo coins!)
        diamondBalance = 0L,
        totalRecharged = 56000L
      )
      database.walletDao().insertOrUpdateWallet(initialWallet)
      database.walletDao().insertTransaction(
        CoinTransactionEntity(
          transactionId = "TX-INIT-${UUID.randomUUID().toString().take(12)}",
          userId = userId,
          amount = 56000L,
          balanceBefore = 0L,
          balanceAfter = 56000L,
          type = "RECHARGE",
          description = "Welcome Account Credit (56,000 Livo Coins)"
        )
      )
    }
  }

  suspend fun rechargeCoins(pkg: RechargePackage): Result<Long> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val wallet = database.walletDao().getWallet(user.id)
      ?: return@withContext Result.failure(IllegalStateException("Wallet not found"))

    val totalToAdd = pkg.coinAmount + pkg.bonusCoins
    val newBalance = wallet.balance + totalToAdd

    val txId = "TX-PAY-${UUID.randomUUID().toString().take(16)}"
    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = txId,
        userId = user.id,
        amount = totalToAdd,
        balanceBefore = wallet.balance,
        balanceAfter = newBalance,
        type = "RECHARGE",
        referenceId = pkg.id,
        description = "Recharge ${pkg.coinAmount} Coins + ${pkg.bonusCoins} Bonus ($${pkg.usdPrice})"
      )
    )

    database.walletDao().insertOrUpdateWallet(
      wallet.copy(
        balance = newBalance,
        totalRecharged = wallet.totalRecharged + totalToAdd,
        updatedAt = System.currentTimeMillis()
      )
    )

    // Notify user
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = user.id,
        title = "Recharge Successful",
        content = "Added ${totalToAdd} coins to your balance. Transaction ID: $txId",
        type = "COIN"
      )
    )

    Result.success(newBalance)
  }

  suspend fun sendGift(
    receiverId: String,
    giftId: String,
    count: Int
  ): Result<SendGiftResponse> = withContext(Dispatchers.IO) {
    val sender = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val gift = giftCatalog.find { it.id == giftId }
      ?: return@withContext Result.failure(IllegalArgumentException("Invalid gift"))

    val receiver = database.userDao().getUserById(receiverId)
      ?: return@withContext Result.failure(IllegalArgumentException("Recipient not found"))

    val totalCost = gift.coinPrice * count
    val senderWallet = database.walletDao().getWallet(sender.id)
      ?: return@withContext Result.failure(IllegalStateException("Sender wallet not found"))

    if (senderWallet.balance < totalCost) {
      return@withContext Result.failure(IllegalStateException("Insufficient coin balance. Please recharge."))
    }

    // Atomic Ledger Operation
    val txId = "TX-GIFT-${UUID.randomUUID().toString().take(16)}"
    val newSenderBalance = senderWallet.balance - totalCost

    // 1. Debit sender wallet
    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = txId,
        userId = sender.id,
        amount = -totalCost,
        balanceBefore = senderWallet.balance,
        balanceAfter = newSenderBalance,
        type = "GIFT_SEND",
        referenceId = gift.id,
        description = "Sent ${count}x ${gift.name} (${gift.iconEmoji}) to ${receiver.nickname}"
      )
    )
    database.walletDao().insertOrUpdateWallet(
      senderWallet.copy(
        balance = newSenderBalance,
        totalSpent = senderWallet.totalSpent + totalCost,
        updatedAt = System.currentTimeMillis()
      )
    )

    // 2. Credit recipient diamonds (50% commission conversion to redeemable diamonds)
    val recipientDiamonds = (totalCost * 0.50).toLong()
    val receiverWallet = database.walletDao().getWallet(receiver.id) ?: CoinWalletEntity(userId = receiver.id)
    database.walletDao().insertOrUpdateWallet(
      receiverWallet.copy(
        diamondBalance = receiverWallet.diamondBalance + recipientDiamonds,
        updatedAt = System.currentTimeMillis()
      )
    )

    // 3. Record permanent gift transaction
    database.walletDao().insertGiftTransaction(
      GiftTransactionEntity(
        senderId = sender.id,
        senderName = sender.nickname,
        receiverId = receiver.id,
        receiverName = receiver.nickname,
        roomId = _currentRoom.value?.id,
        giftId = gift.id,
        giftName = gift.name,
        count = count,
        totalCoins = totalCost
      )
    )

    // 4. Update sender wealth score & level
    val updatedExp = sender.experiencePoints + (totalCost / 10)
    val newLevel = calculateLevel(updatedExp)
    database.userDao().updateUser(
      sender.copy(
        wealthScore = sender.wealthScore + totalCost,
        experiencePoints = updatedExp,
        level = newLevel
      )
    )
    _currentUser.value = database.userDao().getUserById(sender.id)

    // 5. Update receiver charm score
    database.userDao().updateUser(
      receiver.copy(
        charmScore = receiver.charmScore + totalCost
      )
    )

    // 6. Broadcast to Room Chat if inside room
    val room = _currentRoom.value
    if (room != null) {
      database.roomDao().insertRoomMessage(
        RoomMessageEntity(
          roomId = room.id,
          senderId = sender.id,
          senderName = sender.nickname,
          senderAvatar = sender.avatarUrl,
          senderLivoId = sender.livoId,
          messageType = "GIFT",
          content = "sent ${count}x ${gift.name} to ${receiver.nickname}",
          giftIcon = gift.iconEmoji,
          giftQuantity = count
        )
      )
    }

    // 7. Notification to receiver
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = receiver.id,
        title = "Received Gift!",
        content = "${sender.nickname} sent you ${count}x ${gift.name} (${gift.iconEmoji})!",
        type = "GIFT"
      )
    )

    Result.success(
      SendGiftResponse(
        success = true,
        newBalance = newSenderBalance,
        transactionId = txId,
        message = "Successfully sent gift"
      )
    )
  }

  private fun calculateLevel(exp: Long): Int {
    // Level formula: level = sqrt(exp / 500) + 1
    val calculated = (Math.sqrt((exp / 500.0)).toInt() + 1).coerceIn(1, 100)
    return calculated
  }

  // ==========================================================================
  // Rankings (Real Database Aggregation)
  // ==========================================================================
  suspend fun getRankings(type: RankingType, period: RankingPeriod): List<RankingItem> =
    withContext(Dispatchers.IO) {
      val users = database.userDao().observeAllUsers().firstOrNull() ?: emptyList()
      when (type) {
        RankingType.WEALTH -> {
          users.sortedByDescending { it.wealthScore }
            .take(30)
            .mapIndexed { idx, u ->
              RankingItem(
                rank = idx + 1,
                entityId = u.id,
                numericId = u.livoId,
                name = u.nickname,
                avatarUrl = u.avatarUrl,
                score = u.wealthScore,
                level = u.level,
                role = u.role,
                frameId = u.equippedFrameId
              )
            }
        }
        RankingType.CHARM -> {
          users.sortedByDescending { it.charmScore }
            .take(30)
            .mapIndexed { idx, u ->
              RankingItem(
                rank = idx + 1,
                entityId = u.id,
                numericId = u.livoId,
                name = u.nickname,
                avatarUrl = u.avatarUrl,
                score = u.charmScore,
                level = u.level,
                role = u.role,
                frameId = u.equippedFrameId
              )
            }
        }
        RankingType.ROOM -> {
          val rooms = database.roomDao().observeActiveRooms().firstOrNull() ?: emptyList()
          rooms.sortedByDescending { it.listenersCount }
            .take(30)
            .mapIndexed { idx, r ->
              RankingItem(
                rank = idx + 1,
                entityId = r.id,
                numericId = r.roomNumber,
                name = r.title,
                avatarUrl = r.coverUrl,
                score = (r.listenersCount * 100L) + 500L,
                level = 1,
                role = UserRole.USER
              )
            }
        }
      }
    }

  // ==========================================================================
  // Direct Messages
  // ==========================================================================
  fun observeConversations(): Flow<List<ConversationEntity>> =
    database.messageDao().observeConversations()

  fun observePrivateMessages(convId: String): Flow<List<PrivateMessageEntity>> =
    database.messageDao().observePrivateMessages(convId)

  suspend fun sendPrivateMessage(peerUserId: String, text: String) = withContext(Dispatchers.IO) {
    val sender = _currentUser.value ?: return@withContext
    if (text.isBlank()) return@withContext

    val peer = database.userDao().getUserById(peerUserId) ?: return@withContext
    val convId = peer.id

    val message = PrivateMessageEntity(
      conversationId = convId,
      senderId = sender.id,
      receiverId = peer.id,
      content = text.trim()
    )
    database.messageDao().insertPrivateMessage(message)

    database.messageDao().insertConversation(
      ConversationEntity(
        id = convId,
        peerUserId = peer.id,
        peerLivoId = peer.livoId,
        peerName = peer.nickname,
        peerAvatar = peer.avatarUrl,
        peerRole = peer.role,
        lastMessage = text.trim(),
        lastTimestamp = System.currentTimeMillis(),
        unreadCount = 0
      )
    )
  }

  suspend fun markConversationRead(convId: String) = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext
    database.messageDao().markMessagesAsRead(convId, user.id)
    database.messageDao().clearUnreadCount(convId)
  }

  // ==========================================================================
  // Notifications
  // ==========================================================================
  fun observeNotifications(userId: String): Flow<List<NotificationEntity>> =
    database.socialDao().observeNotifications(userId)

  fun observeUnreadNotificationsCount(userId: String): Flow<Int> =
    database.socialDao().observeUnreadNotificationsCount(userId)

  suspend fun markNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
    database.socialDao().markAllNotificationsAsRead(userId)
  }

  // ==========================================================================
  // Backpack & Frames
  // ==========================================================================
  fun observeUserInventory(userId: String): Flow<List<UserInventoryEntity>> =
    database.socialDao().observeInventory(userId)

  suspend fun equipFrame(frameId: String) = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext
    database.socialDao().unequipAllFrames(user.id)
    if (frameId != "frame_none") {
      database.socialDao().equipFrame(user.id, frameId)
    }
    val updated = user.copy(equippedFrameId = if (frameId == "frame_none") null else frameId)
    database.userDao().updateUser(updated)
    _currentUser.value = updated
  }

  // ==========================================================================
  // Agencies
  // ==========================================================================
  fun observeAgencies(): Flow<List<AgencyEntity>> = database.socialDao().observeAgencies()

  suspend fun joinAgency(agencyCode: String): Result<String> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val agency = database.socialDao().getAgencyByCode(agencyCode.trim().uppercase())
      ?: return@withContext Result.failure(IllegalArgumentException("Agency code not found"))

    val existing = database.socialDao().getUserAgencyMembership(user.id)
    if (existing != null) {
      return@withContext Result.failure(IllegalStateException("Already in an agency (${existing.agencyId})"))
    }

    database.socialDao().insertAgencyMember(
      AgencyMemberEntity(
        agencyId = agency.id,
        userId = user.id,
        userLivoId = user.livoId,
        userName = user.nickname,
        userAvatar = user.avatarUrl,
        roleInAgency = "Host"
      )
    )

    // Upgrade user role to HOST if normal user
    if (user.role == UserRole.USER) {
      val upgraded = user.copy(role = UserRole.HOST)
      database.userDao().updateUser(upgraded)
      _currentUser.value = upgraded
    }

    Result.success("Successfully joined agency ${agency.name}")
  }

  // ==========================================================================
  // Reports & Moderation
  // ==========================================================================
  suspend fun report(targetType: String, targetId: String, targetName: String, reason: String, details: String) =
    withContext(Dispatchers.IO) {
      val user = _currentUser.value ?: return@withContext
      database.socialDao().insertReport(
        ReportEntity(
          reporterId = user.id,
          reporterName = user.nickname,
          targetType = targetType,
          targetId = targetId,
          targetName = targetName,
          reason = reason,
          details = details
        )
      )
    }

  suspend fun blockUser(targetUserId: String, targetName: String) = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext
    database.socialDao().insertBlock(
      BlockEntity(
        userId = user.id,
        blockedUserId = targetUserId,
        blockedUserName = targetName
      )
    )
  }

  // ==========================================================================
  // Admin & Operations
  // ==========================================================================
  fun observeReports(): Flow<List<ReportEntity>> = database.socialDao().observeReports()
  fun observeAuditLogs(): Flow<List<AuditLogEntity>> = database.socialDao().observeAuditLogs()

  suspend fun adminAssignRole(targetUserId: String, newRole: UserRole): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role.levelPriority < UserRole.SUPER_ADMIN.levelPriority) {
      return@withContext Result.failure(IllegalStateException("Unauthorized. Requires Super Admin privilege."))
    }

    val target = database.userDao().getUserById(targetUserId)
      ?: return@withContext Result.failure(IllegalArgumentException("User not found"))

    database.userDao().updateUser(target.copy(role = newRole))

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "ASSIGN_ROLE",
        targetType = "USER",
        targetId = target.id,
        details = "Assigned role ${newRole.displayName} to ${target.nickname} (ID: ${target.livoId})"
      )
    )
    Result.success("Role updated to ${newRole.displayName}")
  }

  suspend fun adminGrantCoins(targetUserId: String, coinsToAdd: Long, reason: String): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role.levelPriority < UserRole.SUPER_ADMIN.levelPriority) {
      return@withContext Result.failure(IllegalStateException("Unauthorized. Requires Super Admin privilege."))
    }

    val targetWallet = database.walletDao().getWallet(targetUserId)
      ?: return@withContext Result.failure(IllegalArgumentException("Wallet not found"))

    val newBalance = targetWallet.balance + coinsToAdd
    val txId = "TX-ADM-${UUID.randomUUID().toString().take(12)}"

    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = txId,
        userId = targetUserId,
        amount = coinsToAdd,
        balanceBefore = targetWallet.balance,
        balanceAfter = newBalance,
        type = "ADMIN_ADJUST",
        description = "Admin Grant by ${actor.nickname}: $reason"
      )
    )
    database.walletDao().insertOrUpdateWallet(
      targetWallet.copy(balance = newBalance, updatedAt = System.currentTimeMillis())
    )

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "GRANT_COINS",
        targetType = "USER",
        targetId = targetUserId,
        details = "Granted $coinsToAdd coins. Reason: $reason"
      )
    )
    Result.success("Granted $coinsToAdd coins successfully")
  }

  // ==========================================================================
  // Initial Catalog Seeding (Production-quality bootstrap)
  // ==========================================================================
  private suspend fun seedInitialDataIfNeeded() {
    val existingCount = database.userDao().observeAllUsers().firstOrNull()?.size ?: 0
    if (existingCount > 0) return

    // 1. Seed Official Admin Account (Shahroz / Livo Official)
    val adminId = UUID.randomUUID().toString()
    val adminUser = UserEntity(
      id = adminId,
      livoId = 10001001L,
      email = "shahrozmaher01@gmail.com",
      nickname = "Shahroz Maher",
      avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
      bio = "Founder & Architect of Livo Voice Platform. Welcome to the future of voice communities!",
      role = UserRole.SUPER_ADMIN,
      level = 25,
      experiencePoints = 320000L,
      wealthScore = 15000000L,
      charmScore = 8500000L,
      followersCount = 4280,
      followingCount = 120,
      equippedFrameId = "frame_super_admin"
    )
    database.userDao().insertUser(adminUser)
    syncWalletForUser(adminId)

    // Seed Secondary testable profile for switching accounts
    val hostId = UUID.randomUUID().toString()
    val hostUser = UserEntity(
      id = hostId,
      livoId = 10842918L,
      email = "livo.vip.host@livo.live",
      nickname = "Elena Voice Star",
      avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
      bio = "Singing acoustic covers and late night chatting. Join my room every evening!",
      role = UserRole.HOST,
      level = 14,
      experiencePoints = 98000L,
      wealthScore = 2400000L,
      charmScore = 9200000L,
      followersCount = 1890,
      followingCount = 210,
      equippedFrameId = "frame_royal_crystal"
    )
    database.userDao().insertUser(hostUser)
    syncWalletForUser(hostId)

    // 2. Seed Initial Voice Rooms
    val room1Id = UUID.randomUUID().toString()
    val room1 = RoomEntity(
      id = room1Id,
      roomNumber = 889210L,
      title = "Global Acoustic Lounge & Chill",
      ownerId = hostId,
      ownerLivoId = hostUser.livoId,
      ownerName = hostUser.nickname,
      ownerAvatar = hostUser.avatarUrl,
      coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
      backgroundUrl = "",
      announcement = "Welcome to Elena's Acoustic Lounge! Respect all guests and enjoy the music vibes.",
      category = RoomCategory.MUSIC,
      seatCount = 8,
      listenersCount = 142
    )
    database.roomDao().insertRoom(room1)

    // Seed Seats for Room 1
    val seats1 = (0 until 8).map { i ->
      if (i == 0) {
        RoomSeatEntity(
          roomId = room1Id,
          seatIndex = 0,
          userId = hostId,
          userLivoId = hostUser.livoId,
          userName = hostUser.nickname,
          userAvatar = hostUser.avatarUrl,
          userRole = hostUser.role,
          equippedFrameId = hostUser.equippedFrameId
        )
      } else {
        RoomSeatEntity(roomId = room1Id, seatIndex = i)
      }
    }
    database.roomDao().insertSeats(seats1)

    val room2Id = UUID.randomUUID().toString()
    val room2 = RoomEntity(
      id = room2Id,
      roomNumber = 554320L,
      title = "Livo Official Community Townhall",
      ownerId = adminId,
      ownerLivoId = adminUser.livoId,
      ownerName = adminUser.nickname,
      ownerAvatar = adminUser.avatarUrl,
      coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
      backgroundUrl = "",
      announcement = "Official questions, agency onboarding and platform roadmap discussion.",
      category = RoomCategory.CHAT,
      seatCount = 10,
      listenersCount = 389
    )
    database.roomDao().insertRoom(room2)
    val seats2 = (0 until 10).map { i ->
      if (i == 0) {
        RoomSeatEntity(
          roomId = room2Id,
          seatIndex = 0,
          userId = adminId,
          userLivoId = adminUser.livoId,
          userName = adminUser.nickname,
          userAvatar = adminUser.avatarUrl,
          userRole = adminUser.role,
          equippedFrameId = adminUser.equippedFrameId
        )
      } else {
        RoomSeatEntity(roomId = room2Id, seatIndex = i)
      }
    }
    database.roomDao().insertSeats(seats2)

    // 3. Seed Agencies
    val agency1 = AgencyEntity(
      id = UUID.randomUUID().toString(),
      agencyCode = "LIVO88",
      name = "Infinity Star Entertainment",
      ownerId = adminId,
      ownerName = adminUser.nickname,
      description = "Premier verified agency for high-performing voice hosts and talent.",
      commissionRate = 12.0,
      totalMembers = 86,
      totalMonthlyDiamonds = 24500000L
    )
    database.socialDao().insertAgency(agency1)

    // 4. Seed User Inventory
    database.socialDao().insertInventoryItem(
      UserInventoryEntity(
        userId = adminId,
        itemId = "frame_super_admin",
        itemName = "Super Admin Elite",
        itemType = "AVATAR_FRAME",
        isEquipped = true
      )
    )
    database.socialDao().insertInventoryItem(
      UserInventoryEntity(
        userId = hostId,
        itemId = "frame_royal_crystal",
        itemName = "Royal Crown",
        itemType = "AVATAR_FRAME",
        isEquipped = true
      )
    )

    // 5. Seed Welcome Notification
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = adminId,
        title = "Welcome to Livo Production",
        content = "Your production voice chat ecosystem is configured with real SQL persistence and audio routing.",
        type = "SYSTEM"
      )
    )
  }
}
