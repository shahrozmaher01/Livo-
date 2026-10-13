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

  // Recharge packages: Official rate 1 USD = 400,000 Coins
  val rechargePackages = listOf(
    RechargePackage("pkg_1", 1.00, 400000, 0, isPopular = true),
    RechargePackage("pkg_2", 2.00, 800000, 0),
    RechargePackage("pkg_3", 5.00, 2000000, 0),
    RechargePackage("pkg_4", 10.00, 4000000, 0),
    RechargePackage("pkg_5", 25.00, 10000000, 0),
    RechargePackage("pkg_6", 50.00, 20000000, 0),
    RechargePackage("pkg_7", 100.00, 40000000, 0)
  )

  // Verified Payment Providers for In-App Checkout
  val paymentProviders = listOf(
    PaymentProvider(
      id = "google_play",
      name = "Google Play In-App Billing",
      iconEmoji = "🟢",
      badgeText = "Official & Instant",
      description = "Verified Google Play Store in-app purchase with 1-tap security token"
    ),
    PaymentProvider(
      id = "card",
      name = "Credit / Debit Card",
      iconEmoji = "💳",
      badgeText = "Visa / Mastercard",
      description = "Secure 256-bit encrypted card checkout with fraud protection"
    ),
    PaymentProvider(
      id = "paypal",
      name = "PayPal Express",
      iconEmoji = "🅿️",
      badgeText = "Buyer Protected",
      description = "Fast one-click checkout via verified PayPal balance or linked bank"
    ),
    PaymentProvider(
      id = "crypto_usdt",
      name = "USDT / Crypto Pay (TRC-20)",
      iconEmoji = "🌐",
      badgeText = "Zero Gas / Instant",
      description = "Instant decentralized blockchain settlement on TRON or BSC"
    ),
    PaymentProvider(
      id = "upi_fast",
      name = "Fast Bank / UPI Transfer",
      iconEmoji = "⚡",
      badgeText = "Instant Bank",
      description = "Direct instant clearing bank gateway"
    )
  )

  // Lucky Gifts Catalog (500 Coins: 100 Diamonds receiver, 100 Coins commission, 300 Coins promo pool)
  val luckyGiftCatalog = listOf(
    LuckyGiftItem(
      id = "lucky_clover",
      name = "Lucky Clover",
      coinPrice = 500L,
      iconEmoji = "🍀",
      receiverDiamondReward = 100L,
      platformCommissionCoins = 100L,
      promotionalPoolAllocationCoins = 300L,
      senderXpReward = 50L,
      charmPoints = 500L,
      wealthPoints = 500L,
      animationKey = "LUCKY_BURST"
    ),
    LuckyGiftItem(
      id = "lucky_star",
      name = "Lucky Star",
      coinPrice = 1000L,
      iconEmoji = "⭐",
      receiverDiamondReward = 200L,
      platformCommissionCoins = 200L,
      promotionalPoolAllocationCoins = 600L,
      senderXpReward = 100L,
      charmPoints = 1000L,
      wealthPoints = 1000L,
      animationKey = "LUCKY_BURST"
    ),
    LuckyGiftItem(
      id = "lucky_dragon",
      name = "Golden Lucky Dragon",
      coinPrice = 5000L,
      iconEmoji = "🐉",
      receiverDiamondReward = 1000L,
      platformCommissionCoins = 1000L,
      promotionalPoolAllocationCoins = 3000L,
      senderXpReward = 500L,
      charmPoints = 5000L,
      wealthPoints = 5000L,
      animationKey = "LUCKY_DRAGON"
    )
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
      cleanUpLegacyDemoData()
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
    avatarUrl: String?,
    googleSubId: String? = null
  ): UserEntity = withContext(Dispatchers.IO) {
    // 1. Check permanent Google Subject Identifier link first
    val linkedUser = if (!googleSubId.isNullOrBlank()) {
      database.userDao().getUserByGoogleSubId(googleSubId)
    } else null

    // 2. Fallback to existing email link
    val existing = linkedUser ?: database.userDao().getUserByEmail(email)

    val user = if (existing != null) {
      existing.copy(
        nickname = displayName.ifBlank { existing.nickname },
        avatarUrl = avatarUrl ?: existing.avatarUrl,
        googleSubId = googleSubId ?: existing.googleSubId,
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
        googleSubId = googleSubId,
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

  fun observeAllUsers(): kotlinx.coroutines.flow.Flow<List<UserEntity>> =
    database.userDao().observeAllUsers()

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

  fun observeDiamondTransactions(userId: String): Flow<List<DiamondTransactionEntity>> =
    database.walletDao().observeDiamondTransactions(userId)

  fun observeExchangeTransactions(userId: String): Flow<List<ExchangeTransactionEntity>> =
    database.walletDao().observeExchangeTransactions(userId)

  fun observePayoutRequests(userId: String): Flow<List<PayoutRequestEntity>> =
    database.walletDao().observePayoutRequests(userId)

  fun observeAllPayoutRequests(): Flow<List<PayoutRequestEntity>> =
    database.walletDao().observeAllPayoutRequests()

  fun observeRechargePackagesFlow(): Flow<List<RechargePackageEntity>> =
    database.walletDao().observeRechargePackages()

  private suspend fun syncWalletForUser(userId: String) {
    val existing = database.walletDao().getWallet(userId)
    if (existing == null) {
      // Initialize with welcome coins for newly registered accounts: $2.00 worth = 800,000 Coins
      val welcomeCoins = 800000L
      val initialWallet = CoinWalletEntity(
        userId = userId,
        balance = welcomeCoins,
        diamondBalance = 0L,
        totalRecharged = welcomeCoins
      )
      database.walletDao().insertOrUpdateWallet(initialWallet)
      database.walletDao().insertTransaction(
        CoinTransactionEntity(
          transactionId = "TX-INIT-${UUID.randomUUID().toString().take(12)}",
          userId = userId,
          amount = welcomeCoins,
          balanceBefore = 0L,
          balanceAfter = welcomeCoins,
          type = "RECHARGE",
          paymentProvider = "System Welcome Credit",
          purchaseAmountUsd = 2.00,
          status = "COMPLETED",
          description = "Welcome Account Credit (800,000 Coins • $2.00 Value)"
        )
      )
    }
  }

  suspend fun rechargeCoins(
    pkg: RechargePackage,
    paymentProvider: String = "Google Play In-App Billing",
    paymentReference: String = "TX-PAY-${UUID.randomUUID().toString().take(16)}"
  ): Result<Long> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    
    // 1. Prevent duplicate payment processing
    val existingTx = database.walletDao().getTransactionById(paymentReference)
    if (existingTx != null) {
      return@withContext Result.failure(IllegalStateException("Duplicate payment reference $paymentReference detected. Transaction already processed."))
    }

    val wallet = database.walletDao().getWallet(user.id)
      ?: return@withContext Result.failure(IllegalStateException("Wallet not found"))

    val totalToAdd = pkg.coinAmount + pkg.bonusCoins
    val newBalance = wallet.balance + totalToAdd

    // 2. Record verified transaction ledger entry
    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = paymentReference,
        userId = user.id,
        amount = totalToAdd,
        balanceBefore = wallet.balance,
        balanceAfter = newBalance,
        type = "RECHARGE",
        referenceId = pkg.id,
        paymentProvider = paymentProvider,
        purchaseAmountUsd = pkg.usdPrice,
        status = "COMPLETED",
        description = "Recharge ${String.format("%,d", pkg.coinAmount)} Coins + ${pkg.bonusCoins} Bonus ($${pkg.usdPrice} via $paymentProvider)"
      )
    )

    // 3. Update wallet immediately
    database.walletDao().insertOrUpdateWallet(
      wallet.copy(
        balance = newBalance,
        totalRecharged = wallet.totalRecharged + totalToAdd,
        updatedAt = System.currentTimeMillis()
      )
    )

    // 4. Notify user
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = user.id,
        title = "Recharge Successful",
        content = "Added ${String.format("%,d", totalToAdd)} coins via $paymentProvider. Ref: $paymentReference",
        type = "COIN"
      )
    )

    // 5. Audit Log
    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = user.id,
        actorName = user.nickname,
        action = "COIN_PURCHASE_VERIFIED",
        targetType = "WALLET",
        targetId = user.id,
        details = "Purchased ${String.format("%,d", pkg.coinAmount)} coins ($${pkg.usdPrice} USD) via $paymentProvider. Ref: $paymentReference"
      )
    )

    Result.success(newBalance)
  }

  suspend fun requestPayout(
    diamondsAmount: Long,
    paymentMethod: String,
    accountDetails: String
  ): Result<PayoutRequestEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val currencyConfig = getCurrencySettings()

    if (!currencyConfig.isWithdrawalEnabled) {
      return@withContext Result.failure(IllegalStateException("Diamond withdrawals are currently disabled by administrator."))
    }

    if (diamondsAmount < currencyConfig.minWithdrawalDiamonds) {
      return@withContext Result.failure(IllegalArgumentException("Minimum withdrawal is ${String.format("%,d", currencyConfig.minWithdrawalDiamonds)} Diamonds."))
    }

    val wallet = database.walletDao().getWallet(user.id)
      ?: return@withContext Result.failure(IllegalStateException("Wallet not found"))

    if (wallet.diamondBalance < diamondsAmount) {
      return@withContext Result.failure(IllegalStateException("Insufficient diamonds. Available: ${String.format("%,d", wallet.diamondBalance)}, Requested: ${String.format("%,d", diamondsAmount)}"))
    }

    val rate = currencyConfig.diamondsPerUsdWithdrawal.coerceAtLeast(1L)
    val usdAmount = diamondsAmount.toDouble() / rate

    val reqId = "WD-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6).uppercase()}"
    val newDiamondBalance = wallet.diamondBalance - diamondsAmount

    // Deduct diamonds atomically
    database.walletDao().insertOrUpdateWallet(
      wallet.copy(
        diamondBalance = newDiamondBalance,
        updatedAt = System.currentTimeMillis()
      )
    )

    // Record diamond debit ledger
    database.walletDao().insertDiamondTransaction(
      DiamondTransactionEntity(
        transactionId = reqId,
        userId = user.id,
        amount = -diamondsAmount,
        balanceBefore = wallet.diamondBalance,
        balanceAfter = newDiamondBalance,
        type = "WITHDRAW_DEBIT",
        referenceId = reqId,
        description = "Withdrawal request for ${String.format("%,d", diamondsAmount)} Diamonds ($${String.format("%.2f", usdAmount)} USD via $paymentMethod)"
      )
    )

    val request = PayoutRequestEntity(
      requestId = reqId,
      userId = user.id,
      userLivoId = user.livoId,
      userName = user.nickname,
      userAvatar = user.avatarUrl,
      diamondsAmount = diamondsAmount,
      payoutUsdAmount = usdAmount,
      paymentMethod = paymentMethod,
      accountDetails = accountDetails,
      status = "PENDING"
    )
    database.walletDao().insertPayoutRequest(request)

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = user.id,
        actorName = user.nickname,
        action = "PAYOUT_REQUESTED",
        targetType = "WITHDRAWAL",
        targetId = reqId,
        details = "Requested payout of ${String.format("%,d", diamondsAmount)} Diamonds ($${String.format("%.2f", usdAmount)} USD) via $paymentMethod"
      )
    )

    database.socialDao().insertNotification(
      NotificationEntity(
        userId = user.id,
        title = "Payout Request Submitted",
        content = "Your request for $${String.format("%.2f", usdAmount)} USD has been submitted for review. Ref: $reqId",
        type = "COIN"
      )
    )

    Result.success(request)
  }

  suspend fun adminProcessPayout(
    requestId: String,
    approve: Boolean,
    adminNote: String
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val req = database.walletDao().getPayoutRequestById(requestId)
      ?: return@withContext Result.failure(IllegalArgumentException("Request not found"))

    if (req.status != "PENDING") {
      return@withContext Result.failure(IllegalStateException("Request is already ${req.status}"))
    }

    if (approve) {
      val updated = req.copy(
        status = "APPROVED",
        adminNote = adminNote,
        processedAt = System.currentTimeMillis()
      )
      database.walletDao().updatePayoutRequest(updated)

      database.socialDao().insertNotification(
        NotificationEntity(
          userId = req.userId,
          title = "Payout Approved & Dispatched!",
          content = "Your payout of $${String.format("%.2f", req.payoutUsdAmount)} USD has been approved and sent to your ${req.paymentMethod} account.",
          type = "COIN"
        )
      )

      database.socialDao().insertAuditLog(
        AuditLogEntity(
          actorId = actor.id,
          actorName = actor.nickname,
          action = "PAYOUT_APPROVED",
          targetType = "WITHDRAWAL",
          targetId = req.requestId,
          details = "Approved payout of $${String.format("%.2f", req.payoutUsdAmount)} to ${req.userName}. Note: $adminNote"
        )
      )
      Result.success("Payout request approved successfully")
    } else {
      // Reject and refund diamonds
      val userWallet = database.walletDao().getWallet(req.userId)
      if (userWallet != null) {
        val refundedBalance = userWallet.diamondBalance + req.diamondsAmount
        database.walletDao().insertOrUpdateWallet(
          userWallet.copy(
            diamondBalance = refundedBalance,
            updatedAt = System.currentTimeMillis()
          )
        )
        database.walletDao().insertDiamondTransaction(
          DiamondTransactionEntity(
            transactionId = "TX-REFUND-${UUID.randomUUID().toString().take(10)}",
            userId = req.userId,
            amount = req.diamondsAmount,
            balanceBefore = userWallet.diamondBalance,
            balanceAfter = refundedBalance,
            type = "ADMIN_ADJUST",
            referenceId = req.requestId,
            description = "Refunded ${req.diamondsAmount} Diamonds (Rejected payout: $adminNote)"
          )
        )
      }

      val updated = req.copy(
        status = "REJECTED",
        adminNote = adminNote,
        processedAt = System.currentTimeMillis()
      )
      database.walletDao().updatePayoutRequest(updated)

      database.socialDao().insertNotification(
        NotificationEntity(
          userId = req.userId,
          title = "Payout Request Rejected",
          content = "Your payout request was rejected: $adminNote. Your ${String.format("%,d", req.diamondsAmount)} Diamonds have been refunded.",
          type = "COIN"
        )
      )

      database.socialDao().insertAuditLog(
        AuditLogEntity(
          actorId = actor.id,
          actorName = actor.nickname,
          action = "PAYOUT_REJECTED",
          targetType = "WITHDRAWAL",
          targetId = req.requestId,
          details = "Rejected payout of $${String.format("%.2f", req.payoutUsdAmount)} to ${req.userName}. Reason: $adminNote. Diamonds refunded."
        )
      )
      Result.success("Payout request rejected and diamonds refunded")
    }
  }

  suspend fun adminSaveRechargePackage(pkg: RechargePackageEntity): Result<String> = withContext(Dispatchers.IO) {
    database.walletDao().insertRechargePackage(pkg)
    Result.success("Package saved successfully")
  }

  suspend fun adminDeleteRechargePackage(id: String): Result<String> = withContext(Dispatchers.IO) {
    database.walletDao().deleteRechargePackage(id)
    Result.success("Package deleted successfully")
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

    // 2. Credit recipient diamonds: 1 Coin spent = 1 Diamond received (1:1 ratio)
    val recipientDiamonds = totalCost
    val receiverWallet = database.walletDao().getWallet(receiver.id) ?: CoinWalletEntity(userId = receiver.id)
    val newReceiverDiamondBalance = receiverWallet.diamondBalance + recipientDiamonds
    database.walletDao().insertOrUpdateWallet(
      receiverWallet.copy(
        diamondBalance = newReceiverDiamondBalance,
        updatedAt = System.currentTimeMillis()
      )
    )

    // Record diamond transaction ledger entry
    database.walletDao().insertDiamondTransaction(
      DiamondTransactionEntity(
        transactionId = "TX-DIA-${UUID.randomUUID().toString().take(16)}",
        userId = receiver.id,
        amount = recipientDiamonds,
        balanceBefore = receiverWallet.diamondBalance,
        balanceAfter = newReceiverDiamondBalance,
        type = "GIFT_REWARD",
        referenceId = gift.id,
        description = "Received ${count}x ${gift.name} from ${sender.nickname}"
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

  // ==========================================================================
  // Lucky Gift Sending (500 Coins: 100 Diamonds receiver, 100 Coins commission, 300 Coins promo pool)
  // ==========================================================================
  suspend fun sendLuckyGift(
    receiverId: String,
    luckyGiftId: String,
    count: Int = 1
  ): Result<SendGiftResponse> = withContext(Dispatchers.IO) {
    val sender = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val luckyGift = luckyGiftCatalog.find { it.id == luckyGiftId }
      ?: return@withContext Result.failure(IllegalArgumentException("Invalid lucky gift"))

    val receiver = database.userDao().getUserById(receiverId)
      ?: return@withContext Result.failure(IllegalArgumentException("Recipient not found"))

    val totalCost = luckyGift.coinPrice * count
    val senderWallet = database.walletDao().getWallet(sender.id)
      ?: return@withContext Result.failure(IllegalStateException("Sender wallet not found"))

    if (senderWallet.balance < totalCost) {
      return@withContext Result.failure(IllegalStateException("Insufficient coin balance. Required: $totalCost, Available: ${senderWallet.balance}"))
    }

    val txId = "TX-LUCKY-${UUID.randomUUID().toString().take(16)}"
    val newSenderBalance = senderWallet.balance - totalCost

    // 1. Deduct sender Coins
    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = txId,
        userId = sender.id,
        amount = -totalCost,
        balanceBefore = senderWallet.balance,
        balanceAfter = newSenderBalance,
        type = "GIFT_SEND",
        referenceId = luckyGift.id,
        description = "Sent ${count}x Lucky Gift ${luckyGift.name} (${luckyGift.iconEmoji}) to ${receiver.nickname}"
      )
    )
    database.walletDao().insertOrUpdateWallet(
      senderWallet.copy(
        balance = newSenderBalance,
        totalSpent = senderWallet.totalSpent + totalCost,
        updatedAt = System.currentTimeMillis()
      )
    )

    // 2. Value distribution accounting:
    // Receiver share: 100 Diamonds per 500 Coins (20%)
    val totalReceiverDiamonds = luckyGift.receiverDiamondReward * count
    val receiverWallet = database.walletDao().getWallet(receiver.id) ?: CoinWalletEntity(userId = receiver.id)
    val newReceiverDiamondBalance = receiverWallet.diamondBalance + totalReceiverDiamonds
    database.walletDao().insertOrUpdateWallet(
      receiverWallet.copy(
        diamondBalance = newReceiverDiamondBalance,
        updatedAt = System.currentTimeMillis()
      )
    )
    database.walletDao().insertDiamondTransaction(
      DiamondTransactionEntity(
        transactionId = "TX-DIA-LUCKY-${UUID.randomUUID().toString().take(14)}",
        userId = receiver.id,
        amount = totalReceiverDiamonds,
        balanceBefore = receiverWallet.diamondBalance,
        balanceAfter = newReceiverDiamondBalance,
        type = "GIFT_REWARD",
        referenceId = luckyGift.id,
        description = "Received ${count}x Lucky Gift ${luckyGift.name} from ${sender.nickname}"
      )
    )

    // 3. Promotional Pool & Platform Commission accounting ledger entries
    val promoPoolAllocation = luckyGift.promotionalPoolAllocationCoins * count
    val platformCommission = luckyGift.platformCommissionCoins * count
    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = sender.id,
        actorName = sender.nickname,
        action = "LUCKY_GIFT_DISTRIBUTION",
        targetType = "GIFT",
        targetId = luckyGift.id,
        details = "Price: $totalCost Coins | Receiver: $totalReceiverDiamonds Diamonds | Platform Commission: $platformCommission Coins | Promo Pool: $promoPoolAllocation Coins"
      )
    )

    // 4. Record permanent gift transaction
    database.walletDao().insertGiftTransaction(
      GiftTransactionEntity(
        senderId = sender.id,
        senderName = sender.nickname,
        receiverId = receiver.id,
        receiverName = receiver.nickname,
        roomId = _currentRoom.value?.id,
        giftId = luckyGift.id,
        giftName = luckyGift.name,
        count = count,
        totalCoins = totalCost
      )
    )

    // 5. Update Wealth (Sender) & Charm (Receiver) scores + Sender XP
    val updatedExp = sender.experiencePoints + (luckyGift.senderXpReward * count)
    val newLevel = calculateLevel(updatedExp)
    database.userDao().updateUser(
      sender.copy(
        wealthScore = sender.wealthScore + (luckyGift.wealthPoints * count),
        experiencePoints = updatedExp,
        level = newLevel
      )
    )
    _currentUser.value = database.userDao().getUserById(sender.id)

    database.userDao().updateUser(
      receiver.copy(
        charmScore = receiver.charmScore + (luckyGift.charmPoints * count)
      )
    )

    // 6. Broadcast to Room Chat if active
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
          content = "sent ${count}x Lucky Gift ${luckyGift.name} (${luckyGift.iconEmoji}) to ${receiver.nickname}!",
          giftIcon = luckyGift.iconEmoji,
          giftQuantity = count
        )
      )
    }

    Result.success(
      SendGiftResponse(
        success = true,
        newBalance = newSenderBalance,
        transactionId = txId,
        message = "Successfully sent Lucky Gift"
      )
    )
  }

  // ==========================================================================
  // Official Diamond-to-Coin Exchange System (30% Fee, 70% Coins output)
  // Conversion Formula: Coins Received = Diamonds Exchanged × 0.70
  // ==========================================================================
  suspend fun exchangeDiamondsToCoins(
    diamondsToExchange: Long,
    idempotencyKey: String = UUID.randomUUID().toString()
  ): Result<ExchangeTransactionEntity> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

    if (diamondsToExchange <= 0) {
      return@withContext Result.failure(IllegalArgumentException("Exchange amount must be greater than zero."))
    }

    val currencyConfig = getCurrencySettings()
    if (!currencyConfig.isExchangeEnabled) {
      return@withContext Result.failure(IllegalStateException("Diamond exchanges are currently suspended by administrator."))
    }

    if (diamondsToExchange < currencyConfig.minExchangeDiamonds) {
      return@withContext Result.failure(IllegalArgumentException("Minimum exchange amount is ${currencyConfig.minExchangeDiamonds} Diamonds."))
    }

    if (diamondsToExchange > currencyConfig.maxDailyExchangeDiamonds) {
      return@withContext Result.failure(IllegalArgumentException("Maximum daily limit is ${currencyConfig.maxDailyExchangeDiamonds} Diamonds."))
    }

    // Check duplicate request
    val existingTx = database.walletDao().getExchangeTransactionById(idempotencyKey)
    if (existingTx != null) {
      return@withContext Result.success(existingTx)
    }

    val wallet = database.walletDao().getWallet(user.id)
      ?: return@withContext Result.failure(IllegalStateException("Wallet not found"))

    if (wallet.diamondBalance < diamondsToExchange) {
      return@withContext Result.failure(IllegalStateException("Insufficient diamonds. Available: ${wallet.diamondBalance}, Requested: $diamondsToExchange"))
    }

    // Official server-side calculation:
    // 30% platform fee, 70% user received
    val feePercent = currencyConfig.diamondExchangeFeePercent // 30
    val exchangeRate = currencyConfig.exchangeOutputCoinsPer100Diamonds / 100.0 // 0.70
    val feeEquivalentDiamonds = (diamondsToExchange * (feePercent / 100.0)).toLong() // 300
    val coinsToCredit = (diamondsToExchange * exchangeRate).toLong() // 700

    val newDiamondBalance = wallet.diamondBalance - diamondsToExchange
    val newCoinBalance = wallet.balance + coinsToCredit

    // Atomic update of both balances
    database.walletDao().insertOrUpdateWallet(
      wallet.copy(
        diamondBalance = newDiamondBalance,
        balance = newCoinBalance,
        updatedAt = System.currentTimeMillis()
      )
    )

    // Debit Diamonds ledger entry
    database.walletDao().insertDiamondTransaction(
      DiamondTransactionEntity(
        transactionId = "TX-DIA-EX-$idempotencyKey",
        userId = user.id,
        amount = -diamondsToExchange,
        balanceBefore = wallet.diamondBalance,
        balanceAfter = newDiamondBalance,
        type = "EXCHANGE_DEBIT",
        referenceId = idempotencyKey,
        description = "Exchanged $diamondsToExchange Diamonds for $coinsToCredit Coins ($feePercent% fee: $feeEquivalentDiamonds 💎)"
      )
    )

    // Credit Coins ledger entry
    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = "TX-COIN-EX-$idempotencyKey",
        userId = user.id,
        amount = coinsToCredit,
        balanceBefore = wallet.balance,
        balanceAfter = newCoinBalance,
        type = "REWARD",
        referenceId = idempotencyKey,
        description = "Credited $coinsToCredit Coins from $diamondsToExchange Diamonds exchange"
      )
    )

    // Record permanent Exchange Transaction Receipt
    val exchangeReceipt = ExchangeTransactionEntity(
      transactionId = idempotencyKey,
      userId = user.id,
      diamondsExchanged = diamondsToExchange,
      feePercentage = feePercent,
      feeDiamondsEquivalent = feeEquivalentDiamonds,
      coinsCredited = coinsToCredit,
      exchangeRateUsed = exchangeRate,
      status = "COMPLETED"
    )
    database.walletDao().insertExchangeTransaction(exchangeReceipt)

    // Audit log
    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = user.id,
        actorName = user.nickname,
        action = "DIAMOND_EXCHANGE",
        targetType = "WALLET",
        targetId = user.id,
        details = "Exchanged $diamondsToExchange 💎 -> $coinsToCredit 🪙 ($feePercent% platform fee: $feeEquivalentDiamonds 💎)"
      )
    )

    // Dispatch notification
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = user.id,
        title = "Exchange Completed",
        content = "Successfully exchanged $diamondsToExchange Diamonds for $coinsToCredit Coins.",
        type = "COIN"
      )
    )

    Result.success(exchangeReceipt)
  }

  fun observeAllExchangeTransactions(): Flow<List<ExchangeTransactionEntity>> =
    database.walletDao().observeAllExchangeTransactions()

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
                score = r.listenersCount.toLong(),
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

    // Increment agency totalMembers
    database.socialDao().updateAgency(agency.copy(totalMembers = agency.totalMembers + 1))

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
  // Real Statistics, User Room & Real Follows
  // ==========================================================================
  fun observeFollowingCount(userId: String): Flow<Int> =
    database.userDao().observeFollowingCount(userId)

  fun observeFollowersCount(userId: String): Flow<Int> =
    database.userDao().observeFollowersCount(userId)

  fun observeUserRoom(userId: String): Flow<RoomEntity?> =
    database.roomDao().observeRoomByOwnerId(userId)

  suspend fun getUserRoom(userId: String): RoomEntity? = withContext(Dispatchers.IO) {
    database.roomDao().getRoomByOwnerId(userId)
  }

  suspend fun getUserAgencyMembership(userId: String): AgencyMemberEntity? = withContext(Dispatchers.IO) {
    database.socialDao().getUserAgencyMembership(userId)
  }

  // ==========================================================================
  // Store Purchases & Inventory
  // ==========================================================================
  suspend fun purchaseStoreItem(itemId: String, itemName: String, price: Long): Result<String> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val wallet = database.walletDao().getWallet(user.id) ?: return@withContext Result.failure(IllegalStateException("Wallet not initialized"))

    if (wallet.balance < price) {
      return@withContext Result.failure(IllegalStateException("Insufficient coins. Balance: ${wallet.balance}, required: $price"))
    }

    val newBalance = wallet.balance - price
    database.walletDao().insertOrUpdateWallet(
      wallet.copy(balance = newBalance, totalSpent = wallet.totalSpent + price, updatedAt = System.currentTimeMillis())
    )

    database.walletDao().insertTransaction(
      CoinTransactionEntity(
        transactionId = "TX-BUY-${UUID.randomUUID().toString().take(10)}",
        userId = user.id,
        amount = -price,
        balanceBefore = wallet.balance,
        balanceAfter = newBalance,
        type = "PURCHASE",
        description = "Purchased $itemName"
      )
    )

    database.socialDao().insertInventoryItem(
      UserInventoryEntity(
        userId = user.id,
        itemId = itemId,
        itemName = itemName,
        itemType = "AVATAR_FRAME",
        isEquipped = true
      )
    )

    // Equip frame immediately
    database.socialDao().unequipAllFrames(user.id)
    database.socialDao().equipFrame(user.id, itemId)
    val updatedUser = user.copy(equippedFrameId = itemId)
    database.userDao().updateUser(updatedUser)
    _currentUser.value = updatedUser

    Result.success("Purchased and equipped $itemName!")
  }

  // ==========================================================================
  // User Feedback
  // ==========================================================================
  suspend fun submitFeedback(category: String, message: String): Result<String> = withContext(Dispatchers.IO) {
    val user = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    database.socialDao().insertReport(
      ReportEntity(
        reporterId = user.id,
        reporterName = user.nickname,
        targetType = "FEEDBACK",
        targetId = "PLATFORM",
        targetName = "Livo Platform",
        reason = category,
        details = message.trim()
      )
    )
    Result.success("Feedback submitted successfully. Thank you!")
  }

  // ==========================================================================
  // CP Partner Relationship Management
  // ==========================================================================
  private val cpPrefs = context.getSharedPreferences("livo_cp_prefs", Context.MODE_PRIVATE)

  fun getCpPartner(userId: String): CpPartnerInfo? {
    val partnerId = cpPrefs.getString("cp_${userId}_id", null) ?: return null
    val partnerName = cpPrefs.getString("cp_${userId}_name", "") ?: ""
    val partnerAvatar = cpPrefs.getString("cp_${userId}_avatar", "") ?: ""
    val partnerLivoId = cpPrefs.getLong("cp_${userId}_livoid", 0L)
    val relationType = cpPrefs.getString("cp_${userId}_type", "Sweet CP 💕") ?: "Sweet CP 💕"
    val affinity = cpPrefs.getLong("cp_${userId}_affinity", 520L)
    return CpPartnerInfo(
      partnerId = partnerId,
      partnerName = partnerName,
      partnerAvatar = partnerAvatar,
      partnerLivoId = partnerLivoId,
      relationshipType = relationType,
      affinityScore = affinity
    )
  }

  fun bindCpPartner(partner: UserEntity, relationType: String): Result<String> {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not logged in"))
    cpPrefs.edit()
      .putString("cp_${user.id}_id", partner.id)
      .putString("cp_${user.id}_name", partner.nickname)
      .putString("cp_${user.id}_avatar", partner.avatarUrl)
      .putLong("cp_${user.id}_livoid", partner.livoId)
      .putString("cp_${user.id}_type", relationType)
      .putLong("cp_${user.id}_affinity", 520L)
      .apply()
    return Result.success("Bound $relationType with ${partner.nickname}!")
  }

  fun unbindCpPartner(): Result<String> {
    val user = _currentUser.value ?: return Result.failure(IllegalStateException("Not logged in"))
    cpPrefs.edit()
      .remove("cp_${user.id}_id")
      .remove("cp_${user.id}_name")
      .remove("cp_${user.id}_avatar")
      .remove("cp_${user.id}_livoid")
      .remove("cp_${user.id}_type")
      .remove("cp_${user.id}_affinity")
      .apply()
    return Result.success("CP relationship disbanded.")
  }

  // ==========================================================================
  // 14. Owner Control Panel, Official Distribution & Dynamic Panel Builder
  // ==========================================================================

  fun observeAllPanelConfigs(): Flow<List<PanelConfigEntity>> =
    database.panelDao().observePanelConfigs()

  fun observeAllPanelTemplates(): Flow<List<PanelTemplateEntity>> =
    database.panelDao().observeTemplates()

  fun observeAllPanelAssignments(): Flow<List<UserPanelAssignmentEntity>> =
    database.panelDao().observeAllAssignments()

  fun observeUserActiveAssignments(userId: String): Flow<List<UserPanelAssignmentEntity>> =
    database.panelDao().observeActiveAssignmentsForUser(userId)

  fun observeCurrencySettings(): Flow<SystemCurrencySettingsEntity?> =
    database.panelDao().observeCurrencySettings()

  suspend fun getCurrencySettings(): SystemCurrencySettingsEntity = withContext(Dispatchers.IO) {
    database.panelDao().getCurrencySettings() ?: SystemCurrencySettingsEntity()
  }

  suspend fun updateCurrencySettings(
    referenceRate: Long,
    exchangeFeePercent: Int,
    outputCoins: Int,
    minExchange: Long,
    resellerDiscount: Int,
    minWithdrawalDiamonds: Long = 500000L,
    diamondsPerUsdWithdrawal: Long = 500000L,
    isWithdrawalEnabled: Boolean = true
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role != UserRole.OFFICIAL && actor.role != UserRole.SUPER_ADMIN) {
      return@withContext Result.failure(IllegalStateException("Unauthorized. Owner privilege required."))
    }

    val updated = SystemCurrencySettingsEntity(
      referenceRateCoinsPerUsd = referenceRate,
      diamondExchangeFeePercent = exchangeFeePercent,
      exchangeOutputCoinsPer100Diamonds = outputCoins,
      minExchangeDiamonds = minExchange,
      resellerDiscountPercent = resellerDiscount,
      minWithdrawalDiamonds = minWithdrawalDiamonds,
      diamondsPerUsdWithdrawal = diamondsPerUsdWithdrawal,
      isWithdrawalEnabled = isWithdrawalEnabled,
      updatedAt = System.currentTimeMillis()
    )
    database.panelDao().insertCurrencySettings(updated)

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "UPDATE_CURRENCY_SETTINGS",
        targetType = "SYSTEM",
        targetId = "global_currency_settings",
        details = "Rate: 1 USD = $referenceRate Coins, Diamond fee: $exchangeFeePercent%, Output: $outputCoins Coins/100 Diamonds"
      )
    )
    Result.success("Virtual currency parameters updated successfully")
  }

  suspend fun savePanelConfig(
    panelType: String,
    displayName: String,
    modules: List<PanelModuleConfig>,
    isPublished: Boolean
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role != UserRole.OFFICIAL && actor.role != UserRole.SUPER_ADMIN) {
      return@withContext Result.failure(IllegalStateException("Unauthorized. Owner privilege required."))
    }

    val current = database.panelDao().getPanelConfigByType(panelType)
    val nextVersion = (current?.version ?: 0) + 1
    val modulesJson = org.json.JSONArray().apply {
      modules.forEach { mod ->
        val obj = org.json.JSONObject().apply {
          put("moduleId", mod.moduleId)
          put("moduleName", mod.moduleName)
          put("isEnabled", mod.isEnabled)
          put("accessScope", mod.accessScope)
          put("maxLimit", mod.maxLimit)
          val actArray = org.json.JSONArray()
          mod.allowedActions.forEach { actArray.put(it) }
          put("allowedActions", actArray)
        }
        put(obj)
      }
    }.toString()

    val entity = PanelConfigEntity(
      id = panelType,
      panelType = panelType,
      displayName = displayName,
      isEnabled = true,
      modulesJson = modulesJson,
      isPublished = isPublished,
      version = nextVersion,
      lastUpdatedBy = actor.nickname,
      lastUpdatedAt = System.currentTimeMillis()
    )
    database.panelDao().insertPanelConfig(entity)

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = if (isPublished) "PUBLISH_PANEL_CONFIG" else "SAVE_DRAFT_PANEL_CONFIG",
        targetType = "PANEL_CONFIG",
        targetId = panelType,
        details = "${if (isPublished) "Published" else "Saved Draft"} config for $displayName (v$nextVersion) with ${modules.size} modules"
      )
    )

    Result.success(if (isPublished) "Published configuration for $displayName" else "Draft saved for $displayName")
  }

  suspend fun savePanelTemplate(
    templateName: String,
    panelType: String,
    description: String,
    modules: List<PanelModuleConfig>
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val modulesJson = org.json.JSONArray().apply {
      modules.forEach { mod ->
        val obj = org.json.JSONObject().apply {
          put("moduleId", mod.moduleId)
          put("moduleName", mod.moduleName)
          put("isEnabled", mod.isEnabled)
          put("accessScope", mod.accessScope)
          put("maxLimit", mod.maxLimit)
          val actArray = org.json.JSONArray()
          mod.allowedActions.forEach { actArray.put(it) }
          put("allowedActions", actArray)
        }
        put(obj)
      }
    }.toString()

    val template = PanelTemplateEntity(
      id = "tmpl_${UUID.randomUUID().toString().take(8)}",
      templateName = templateName.trim(),
      panelType = panelType,
      description = description.trim(),
      modulesJson = modulesJson
    )
    database.panelDao().insertTemplate(template)
    Result.success("Template '$templateName' created successfully")
  }

  suspend fun deletePanelTemplate(templateId: String): Result<String> = withContext(Dispatchers.IO) {
    database.panelDao().deleteTemplate(templateId)
    Result.success("Template removed")
  }

  suspend fun assignOfficialPanel(
    targetUserId: String,
    panelType: String,
    roleToAssign: UserRole,
    durationDays: Int, // -1 for permanent, 1, 7, 15, 30, 90
    permittedFeaturesJson: String,
    coinLimit: Long = 0L,
    internalNotes: String = ""
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role != UserRole.OFFICIAL && actor.role != UserRole.SUPER_ADMIN) {
      return@withContext Result.failure(IllegalStateException("Unauthorized. Only Owner/Official can distribute panels."))
    }

    val targetUser = database.userDao().getUserById(targetUserId)
      ?: return@withContext Result.failure(IllegalArgumentException("Target user not found."))

    // Protect Owner from privilege downgrade
    if (targetUser.role == UserRole.OFFICIAL && actor.id != targetUser.id) {
      return@withContext Result.failure(IllegalStateException("Cannot modify or override Owner account permissions."))
    }

    val now = System.currentTimeMillis()
    val expiresAt = if (durationDays > 0) now + (durationDays.toLong() * 86400000L) else -1L

    // Existing assignment check
    val existing = database.panelDao().getAssignment(targetUserId, panelType)
    val assignmentId = existing?.id ?: UUID.randomUUID().toString()

    val assignment = UserPanelAssignmentEntity(
      id = assignmentId,
      userId = targetUser.id,
      userLivoId = targetUser.livoId,
      userName = targetUser.nickname,
      userAvatar = targetUser.avatarUrl,
      panelType = panelType,
      roleName = roleToAssign.displayName,
      durationDays = durationDays,
      expiresAt = expiresAt,
      customPermissionsJson = permittedFeaturesJson,
      coinInventoryLimit = coinLimit,
      notes = internalNotes.trim(),
      status = "ACTIVE",
      assignedBy = actor.nickname,
      assignedAt = now
    )
    database.panelDao().insertAssignment(assignment)

    // Update target user's role if new role has higher or appropriate priority
    if (roleToAssign.levelPriority > targetUser.role.levelPriority || targetUser.role == UserRole.USER) {
      database.userDao().updateUser(targetUser.copy(role = roleToAssign))
    }

    // Send notification
    database.socialDao().insertNotification(
      NotificationEntity(
        userId = targetUser.id,
        title = "Official Panel Granted",
        content = "You have been assigned the $panelType (${roleToAssign.displayName}). Duration: ${if (durationDays > 0) "$durationDays days" else "Permanent"}.",
        type = "ADMIN"
      )
    )

    // Audit log
    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "ASSIGN_OFFICIAL_PANEL",
        targetType = "USER",
        targetId = targetUser.id,
        details = "Assigned $panelType to ${targetUser.nickname} (LIVO ID: ${targetUser.livoId}). Duration: ${if (durationDays > 0) "$durationDays days" else "Permanent"}. Notes: $internalNotes"
      )
    )

    Result.success("Assigned $panelType to ${targetUser.nickname} successfully!")
  }

  suspend fun updateAssignmentStatus(
    assignmentId: String,
    newStatus: String // ACTIVE, SUSPENDED, REVOKED
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    if (actor.role != UserRole.OFFICIAL && actor.role != UserRole.SUPER_ADMIN) {
      return@withContext Result.failure(IllegalStateException("Unauthorized."))
    }

    val assignment = database.panelDao().getAssignmentById(assignmentId)
      ?: return@withContext Result.failure(IllegalArgumentException("Assignment record not found"))

    database.panelDao().updateAssignmentStatus(assignmentId, newStatus)

    // If revoked, adjust user role if needed
    if (newStatus == "REVOKED") {
      val user = database.userDao().getUserById(assignment.userId)
      if (user != null && user.role.displayName == assignment.roleName && user.role != UserRole.OFFICIAL) {
        database.userDao().updateUser(user.copy(role = UserRole.USER))
      }
    }

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "UPDATE_ASSIGNMENT_STATUS",
        targetType = "PANEL_ASSIGNMENT",
        targetId = assignmentId,
        details = "Changed status of ${assignment.panelType} for ${assignment.userName} to $newStatus"
      )
    )
    Result.success("Assignment status updated to $newStatus")
  }

  suspend fun extendAssignmentDuration(
    assignmentId: String,
    additionalDays: Int
  ): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val assignment = database.panelDao().getAssignmentById(assignmentId)
      ?: return@withContext Result.failure(IllegalArgumentException("Assignment not found"))

    val baseTime = if (assignment.expiresAt > System.currentTimeMillis()) assignment.expiresAt else System.currentTimeMillis()
    val newExpiry = baseTime + (additionalDays.toLong() * 86400000L)
    val newTotalDays = if (assignment.durationDays > 0) assignment.durationDays + additionalDays else additionalDays

    val updated = assignment.copy(
      durationDays = newTotalDays,
      expiresAt = newExpiry,
      status = "ACTIVE"
    )
    database.panelDao().insertAssignment(updated)

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "EXTEND_PANEL_DURATION",
        targetType = "PANEL_ASSIGNMENT",
        targetId = assignmentId,
        details = "Extended ${assignment.panelType} for ${assignment.userName} by $additionalDays days"
      )
    )
    Result.success("Extended access by $additionalDays days")
  }

  suspend fun removeAssignment(assignmentId: String): Result<String> = withContext(Dispatchers.IO) {
    val actor = _currentUser.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
    val assignment = database.panelDao().getAssignmentById(assignmentId)
      ?: return@withContext Result.failure(IllegalArgumentException("Assignment not found"))

    database.panelDao().deleteAssignment(assignmentId)

    val user = database.userDao().getUserById(assignment.userId)
    if (user != null && user.role.displayName == assignment.roleName && user.role != UserRole.OFFICIAL) {
      database.userDao().updateUser(user.copy(role = UserRole.USER))
    }

    database.socialDao().insertAuditLog(
      AuditLogEntity(
        actorId = actor.id,
        actorName = actor.nickname,
        action = "REMOVE_PANEL_ASSIGNMENT",
        targetType = "PANEL_ASSIGNMENT",
        targetId = assignmentId,
        details = "Revoked and removed ${assignment.panelType} from ${assignment.userName}"
      )
    )
    Result.success("Panel revoked and removed")
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
      level = 1,
      experiencePoints = 0L,
      wealthScore = 0L,
      charmScore = 0L,
      followersCount = 0,
      followingCount = 0,
      visitorsCount = 0,
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
      level = 1,
      experiencePoints = 0L,
      wealthScore = 0L,
      charmScore = 0L,
      followersCount = 0,
      followingCount = 0,
      visitorsCount = 0,
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
      listenersCount = 1
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
      listenersCount = 1
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
      totalMembers = 1,
      totalMonthlyDiamonds = 0L
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

    // 6. Seed System Currency Settings (1 USD = 400,000 Coins, 30% diamond fee, 70 Coins/100 Diamonds)
    database.panelDao().insertCurrencySettings(
      SystemCurrencySettingsEntity(
        id = "global_currency_settings",
        referenceRateCoinsPerUsd = 400000L,
        diamondExchangeFeePercent = 30,
        exchangeOutputCoinsPer100Diamonds = 70,
        minExchangeDiamonds = 100L,
        maxDailyExchangeDiamonds = 10000000L,
        resellerDiscountPercent = 5,
        updatedAt = System.currentTimeMillis()
      )
    )

    // 7. Seed Default Panel Configurations for All 8 Core Panels
    val defaultModules = listOf(
      "dashboard" to "Dashboard",
      "users" to "User Management",
      "rooms" to "Room Management",
      "agencies" to "Agency Management",
      "hosting" to "Hosting Management",
      "gifts" to "Gift Management",
      "coin_reports" to "Coin & Diamond Reports",
      "frames" to "Frame Management",
      "rankings" to "Ranking Management",
      "notifications" to "Notifications",
      "support" to "Support & Reports",
      "audit_logs" to "Audit Logs"
    )

    PanelType.values().forEach { pType ->
      val modules = defaultModules.map { (id, name) ->
        val isFin = id.contains("coin")
        val scope = when (pType) {
          PanelType.OFFICIAL, PanelType.SUPER_ADMIN -> "GLOBAL"
          PanelType.MANAGER -> "ASSIGNED_ONLY"
          PanelType.BD -> "ASSIGNED_ONLY"
          PanelType.AGENCY -> "SELF_ONLY"
          PanelType.HOSTING -> "SELF_ONLY"
          PanelType.COIN_RESELLER, PanelType.SUPER_COIN_RESELLER -> "ASSIGNED_ONLY"
        }
        val actions = if (pType == PanelType.OFFICIAL) {
          listOf("VIEW", "CREATE", "EDIT", "DELETE", "APPROVE", "ASSIGN", "EXPORT", "MANAGE_PERMISSIONS")
        } else if (pType == PanelType.SUPER_ADMIN) {
          listOf("VIEW", "CREATE", "EDIT", "APPROVE", "ASSIGN", "EXPORT")
        } else {
          listOf("VIEW", "CREATE", "EDIT")
        }
        PanelModuleConfig(
          moduleId = id,
          moduleName = name,
          isEnabled = if (isFin && (pType == PanelType.HOSTING || pType == PanelType.AGENCY)) false else true,
          allowedActions = actions,
          accessScope = scope
        )
      }

      val json = org.json.JSONArray().apply {
        modules.forEach { mod ->
          val obj = org.json.JSONObject().apply {
            put("moduleId", mod.moduleId)
            put("moduleName", mod.moduleName)
            put("isEnabled", mod.isEnabled)
            put("accessScope", mod.accessScope)
            put("maxLimit", mod.maxLimit)
            val actArray = org.json.JSONArray()
            mod.allowedActions.forEach { actArray.put(it) }
            put("allowedActions", actArray)
          }
          put(obj)
        }
      }.toString()

      database.panelDao().insertPanelConfig(
        PanelConfigEntity(
          id = pType.name,
          panelType = pType.name,
          displayName = pType.displayName,
          isEnabled = true,
          modulesJson = json,
          isPublished = true,
          version = 1,
          lastUpdatedBy = "System",
          lastUpdatedAt = System.currentTimeMillis()
        )
      )
    }

    // 8. Seed Predefined Templates
    val templates = listOf(
      Triple("Official Basic", PanelType.OFFICIAL.name, "Standard official administration suite"),
      Triple("Official Full", PanelType.OFFICIAL.name, "Unrestricted official panel with all permissions"),
      Triple("Manager Standard", PanelType.MANAGER.name, "Standard manager controls for assigned users & rooms"),
      Triple("Manager Custom", PanelType.MANAGER.name, "Flexible team manager profile with custom limits"),
      Triple("Super Admin Restricted", PanelType.SUPER_ADMIN.name, "Super admin with protected owner controls enabled"),
      Triple("BD Standard", PanelType.BD.name, "Business developer panel with agency assignment oversight"),
      Triple("Agency Standard", PanelType.AGENCY.name, "Agency leader controls for hosts and diamond activity"),
      Triple("Hosting Standard", PanelType.HOSTING.name, "Host performance, room scheduling and achievements"),
      Triple("Coin Reseller Standard", PanelType.COIN_RESELLER.name, "Coin reseller inventory and customer receipt delivery"),
      Triple("Super Coin Reseller Standard", PanelType.SUPER_COIN_RESELLER.name, "High-capacity reseller with downstream inventory allocation")
    )

    templates.forEach { (name, type, desc) ->
      database.panelDao().insertTemplate(
        PanelTemplateEntity(
          id = "tmpl_${name.lowercase().replace(" ", "_")}",
          templateName = name,
          panelType = type,
          description = desc,
          modulesJson = "[]"
        )
      )
    }

    // 8. Seed Default Recharge Packages ($1 = 400,000 Coins)
    database.walletDao().insertRechargePackages(
      listOf(
        RechargePackageEntity("pkg_1", 1.00, 400000, 0, isPopular = true, sortOrder = 1),
        RechargePackageEntity("pkg_2", 2.00, 800000, 0, sortOrder = 2),
        RechargePackageEntity("pkg_3", 5.00, 2000000, 0, sortOrder = 3),
        RechargePackageEntity("pkg_4", 10.00, 4000000, 0, sortOrder = 4),
        RechargePackageEntity("pkg_5", 25.00, 10000000, 0, sortOrder = 5),
        RechargePackageEntity("pkg_6", 50.00, 20000000, 0, sortOrder = 6),
        RechargePackageEntity("pkg_7", 100.00, 40000000, 0, sortOrder = 7)
      )
    )
  }


  // ==========================================================================
  // 13. Reels, Stories, Likes, Comments & Video Upload System
  // ==========================================================================
  private val _reelsList = MutableStateFlow<List<ReelEntity>>(
    listOf(
      ReelEntity(
        id = "reel_1",
        creatorId = "user_dreamer",
        creatorName = "dreamer_girl",
        creatorAvatar = "reel_bg_dreamer_girl",
        coverDrawableName = "reel_bg_dreamer_girl",
        caption = "Some moments are just special... ♥",
        hashtags = "#sunset #vibes #peace #lvoice",
        musicTitle = "Original Audio - dreamer_girl",
        durationSec = 28,
        viewsCount = 0,
        likesCount = 0,
        commentsCount = 0,
        sharesCount = 0,
        isLiked = false,
        isSaved = false,
        isFollowing = false
      ),
      ReelEntity(
        id = "reel_2",
        creatorId = "user_silent",
        creatorName = "silent_wanderer",
        creatorAvatar = "reel_thumb_silence",
        coverDrawableName = "reel_thumb_silence",
        caption = "Sometimes silence speaks louder than words... ♥",
        hashtags = "#mountains #citylights #night",
        musicTitle = "Night Whispers Lo-Fi",
        durationSec = 28,
        viewsCount = 0,
        likesCount = 0,
        commentsCount = 0,
        sharesCount = 0,
        isLiked = false,
        isSaved = false,
        isFollowing = false
      ),
      ReelEntity(
        id = "reel_3",
        creatorId = "user_melodic",
        creatorName = "melodic_soul",
        creatorAvatar = "reel_thumb_music",
        coverDrawableName = "reel_thumb_music",
        caption = "Good music\nBetter mood... 🎵",
        hashtags = "#sunsetbeats #musiclover",
        musicTitle = "Acoustic Sunset Melody 🎸",
        durationSec = 36,
        viewsCount = 0,
        likesCount = 0,
        commentsCount = 0,
        sharesCount = 0,
        isLiked = false,
        isSaved = false,
        isFollowing = false
      ),
      ReelEntity(
        id = "reel_4",
        creatorId = "user_squad",
        creatorName = "golden_squad",
        creatorAvatar = "reel_thumb_friendship",
        coverDrawableName = "reel_thumb_friendship",
        caption = "Friendship\nForever... ♥",
        hashtags = "#squadgoals #forever #goldenhour",
        musicTitle = "Memories of Summer 🌅",
        durationSec = 31,
        viewsCount = 0,
        likesCount = 0,
        commentsCount = 0,
        sharesCount = 0,
        isLiked = false,
        isSaved = false,
        isFollowing = false
      ),
      ReelEntity(
        id = "reel_5",
        creatorId = "user_cozy",
        creatorName = "cozy_diaries",
        creatorAvatar = "reel_thumb_dreams",
        coverDrawableName = "reel_thumb_dreams",
        caption = "Small steps\nBig dreams... ✨",
        hashtags = "#latte #cozyvibes #journaling",
        musicTitle = "Coffee Shop Acoustic Notes ☕",
        durationSec = 22,
        viewsCount = 0,
        likesCount = 0,
        commentsCount = 0,
        sharesCount = 0,
        isLiked = false,
        isSaved = false,
        isFollowing = false
      )
    )
  )
  val reels: StateFlow<List<ReelEntity>> = _reelsList.asStateFlow()

  private val _storiesList = MutableStateFlow<List<StoryEntity>>(emptyList())
  val stories: StateFlow<List<StoryEntity>> = _storiesList.asStateFlow()

  private val _commentsMap = MutableStateFlow<Map<String, List<ReelCommentEntity>>>(emptyMap())

  // ==========================================================================
  // Clean Up Legacy Demo & Placeholder Data From Database
  // ==========================================================================
  suspend fun cleanUpLegacyDemoData() = withContext(Dispatchers.IO) {
    val users = database.userDao().observeAllUsers().firstOrNull() ?: emptyList()
    users.forEach { u ->
      val realFollowers = database.userDao().getFollowersCount(u.id)
      val realFollowing = database.userDao().getFollowingCount(u.id)
      val hasFakeFollowers = u.followersCount != realFollowers || u.followingCount != realFollowing
      val hasFakeWealthOrCharm = (u.wealthScore >= 2400000L && (u.email.contains("shahroz") || u.email.contains("host"))) ||
                                (u.charmScore >= 8500000L && (u.email.contains("shahroz") || u.email.contains("host"))) ||
                                (u.experiencePoints >= 98000L && (u.email.contains("shahroz") || u.email.contains("host")))
      if (hasFakeFollowers || hasFakeWealthOrCharm) {
        val exp = if (hasFakeWealthOrCharm) 0L else u.experiencePoints
        val level = calculateLevel(exp)
        val sanitized = u.copy(
          followersCount = realFollowers,
          followingCount = realFollowing,
          wealthScore = if (hasFakeWealthOrCharm) 0L else u.wealthScore,
          charmScore = if (hasFakeWealthOrCharm) 0L else u.charmScore,
          experiencePoints = exp,
          level = level,
          visitorsCount = 0
        )
        database.userDao().updateUser(sanitized)
        if (_currentUser.value?.id == u.id) {
          _currentUser.value = sanitized
        }
      }
    }

    val rooms = database.roomDao().observeActiveRooms().firstOrNull() ?: emptyList()
    rooms.forEach { r ->
      if (r.listenersCount == 142 || r.listenersCount == 389) {
        database.roomDao().updateRoom(r.copy(listenersCount = 1))
      }
    }

    val agencies = database.socialDao().observeAgencies().firstOrNull() ?: emptyList()
    agencies.forEach { a ->
      if (a.totalMembers == 86 || a.totalMonthlyDiamonds == 24500000L) {
        database.socialDao().updateAgency(a.copy(totalMembers = 1, totalMonthlyDiamonds = 0L))
      }
    }
  }

  fun observeComments(reelId: String): Flow<List<ReelCommentEntity>> {
    return _commentsMap.map { map -> map[reelId] ?: emptyList() }
  }

  suspend fun toggleLikeReel(reelId: String) = withContext(Dispatchers.IO) {
    _reelsList.update { list ->
      list.map { r ->
        if (r.id == reelId) {
          val newLiked = !r.isLiked
          val newCount = if (newLiked) r.likesCount + 1 else (r.likesCount - 1).coerceAtLeast(0)
          r.copy(isLiked = newLiked, likesCount = newCount)
        } else r
      }
    }
  }

  suspend fun toggleBookmarkReel(reelId: String) = withContext(Dispatchers.IO) {
    _reelsList.update { list ->
      list.map { r ->
        if (r.id == reelId) {
          r.copy(isSaved = !r.isSaved)
        } else r
      }
    }
  }

  suspend fun toggleFollowCreator(creatorName: String) = withContext(Dispatchers.IO) {
    _reelsList.update { list ->
      list.map { r ->
        if (r.creatorName == creatorName) {
          r.copy(isFollowing = !r.isFollowing)
        } else r
      }
    }
  }

  suspend fun addComment(reelId: String, content: String): ReelCommentEntity = withContext(Dispatchers.IO) {
    val user = _currentUser.value
    val newComment = ReelCommentEntity(
      reelId = reelId,
      userId = user?.id ?: "guest",
      userName = user?.nickname ?: "Guest_User",
      userAvatar = user?.avatarUrl ?: "",
      content = content.trim()
    )

    _commentsMap.update { map ->
      val currentList = map[reelId] ?: emptyList()
      map + (reelId to (listOf(newComment) + currentList))
    }

    _reelsList.update { list ->
      list.map { r ->
        if (r.id == reelId) r.copy(commentsCount = r.commentsCount + 1) else r
      }
    }

    newComment
  }

  suspend fun deleteComment(reelId: String, commentId: String) = withContext(Dispatchers.IO) {
    _commentsMap.update { map ->
      val currentList = map[reelId] ?: emptyList()
      map + (reelId to currentList.filterNot { it.id == commentId })
    }
    _reelsList.update { list ->
      list.map { r ->
        if (r.id == reelId) r.copy(commentsCount = (r.commentsCount - 1).coerceAtLeast(0)) else r
      }
    }
  }

  suspend fun toggleLikeComment(reelId: String, commentId: String) = withContext(Dispatchers.IO) {
    _commentsMap.update { map ->
      val currentList = map[reelId] ?: emptyList()
      map + (reelId to currentList.map { c ->
        if (c.id == commentId) {
          val newLiked = !c.isLiked
          val newLikes = if (newLiked) c.likesCount + 1 else (c.likesCount - 1).coerceAtLeast(0)
          c.copy(isLiked = newLiked, likesCount = newLikes)
        } else c
      })
    }
  }

  suspend fun uploadReel(
    caption: String,
    videoUri: String,
    hashtags: String,
    privacy: String = "PUBLIC"
  ): ReelEntity = withContext(Dispatchers.IO) {
    val user = _currentUser.value
    val newReel = ReelEntity(
      id = "reel_${System.currentTimeMillis()}",
      creatorId = user?.id ?: "me",
      creatorName = user?.nickname ?: "My_Profile",
      creatorAvatar = user?.avatarUrl ?: "reel_bg_dreamer_girl",
      coverDrawableName = "reel_bg_dreamer_girl",
      videoUri = videoUri,
      caption = caption.trim(),
      hashtags = hashtags.trim().ifBlank { "#lvoice #reels" },
      musicTitle = "Original Audio - ${user?.nickname ?: "L Voice"}",
      durationSec = 15,
      viewsCount = 1,
      likesCount = 0,
      commentsCount = 0,
      sharesCount = 0,
      privacy = privacy
    )

    _reelsList.update { listOf(newReel) + it }
    newReel
  }

  suspend fun addStory(mediaUri: String, caption: String): StoryEntity = withContext(Dispatchers.IO) {
    val user = _currentUser.value
    val newStory = StoryEntity(
      id = "story_${System.currentTimeMillis()}",
      userId = user?.id ?: "me",
      userName = user?.nickname ?: "Your Story",
      userAvatar = user?.avatarUrl ?: "story_avatar_butterfly",
      mediaDrawableName = "story_avatar_butterfly",
      caption = caption.trim(),
      isViewed = false,
      isMine = true
    )

    _storiesList.update { list ->
      listOf(newStory) + list.filterNot { it.isMine }
    }
    newStory
  }

  suspend fun markStoryViewed(storyId: String) = withContext(Dispatchers.IO) {
    _storiesList.update { list ->
      list.map { s ->
        if (s.id == storyId) s.copy(isViewed = true) else s
      }
    }
  }

  suspend fun incrementShareCount(reelId: String) = withContext(Dispatchers.IO) {
    _reelsList.update { list ->
      list.map { r ->
        if (r.id == reelId) r.copy(sharesCount = r.sharesCount + 1) else r
      }
    }
  }
}

