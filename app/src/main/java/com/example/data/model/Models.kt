package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ============================================================================
// 1. Roles & Permissions
// ============================================================================
enum class UserRole(val displayName: String, val levelPriority: Int) {
  OFFICIAL("Official", 100),
  SUPER_ADMIN("Super Admin", 90),
  ADMIN_LEADER("Admin Leader", 85),
  ADMIN("Admin", 80),
  MANAGER("Manager", 70),
  BD_LEADER("BD Leader", 65),
  BD("BD", 60),
  AGENCY_LEADER("Agency Leader", 55),
  AGENCY("Agency", 50),
  SUPER_COIN_RESELLER("Super Coin Reseller", 45),
  COIN_RESELLER("Coin Reseller", 40),
  CS_LEADER("C's Leader", 35),
  CS("C's", 30),
  HOST("Host", 20),
  USER("User", 0)
}

// ============================================================================
// 2. User & Session
// ============================================================================
@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String, // UUID
  val livoId: Long, // Unique 8-digit numeric ID (e.g., 10293847)
  val email: String,
  val nickname: String,
  val avatarUrl: String,
  val bio: String = "",
  val gender: String = "unspecified",
  val countryCode: String = "US",
  val role: UserRole = UserRole.USER,
  val level: Int = 1,
  val experiencePoints: Long = 0,
  val wealthScore: Long = 0, // Coins spent
  val charmScore: Long = 0, // Gift value received
  val followersCount: Int = 0,
  val followingCount: Int = 0,
  val visitorsCount: Int = 0,
  val equippedFrameId: String? = null,
  val isOnline: Boolean = true,
  val isBanned: Boolean = false,
  val banReason: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)

data class UserSession(
  val userId: String,
  val token: String,
  val email: String,
  val nickname: String,
  val avatarUrl: String,
  val livoId: Long,
  val role: UserRole,
  val expiresAt: Long
)

@Entity(tableName = "follows", primaryKeys = ["followerId", "followingId"])
data class FollowEntity(
  val followerId: String,
  val followingId: String,
  val createdAt: Long = System.currentTimeMillis()
)

// ============================================================================
// 3. Voice Rooms & Seats
// ============================================================================
enum class RoomCategory(val displayName: String) {
  CHAT("Social Chat"),
  MUSIC("Live Music"),
  GAMING("Gaming & Chill"),
  KARAOKE("Karaoke Party"),
  DATING("Match & Friends"),
  RADIO("Late Night Radio")
}

@Entity(tableName = "rooms")
data class RoomEntity(
  @PrimaryKey val id: String,
  val roomNumber: Long, // 6-digit public ID (e.g., 889210)
  val title: String,
  val ownerId: String,
  val ownerLivoId: Long,
  val ownerName: String,
  val ownerAvatar: String,
  val coverUrl: String,
  val backgroundUrl: String,
  val announcement: String = "Welcome to Livo Voice Room! Be respectful and have fun.",
  val category: RoomCategory = RoomCategory.CHAT,
  val seatCount: Int = 8, // 8, 9, 10 or 12
  val isLocked: Boolean = false,
  val isActive: Boolean = true,
  val listenersCount: Int = 1,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_seats", primaryKeys = ["roomId", "seatIndex"])
data class RoomSeatEntity(
  val roomId: String,
  val seatIndex: Int,
  val userId: String? = null,
  val userLivoId: Long? = null,
  val userName: String? = null,
  val userAvatar: String? = null,
  val userRole: UserRole? = null,
  val equippedFrameId: String? = null,
  val isLocked: Boolean = false,
  val isMuted: Boolean = false,
  val isSpeaking: Boolean = false
)

@Entity(tableName = "room_messages")
data class RoomMessageEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val roomId: String,
  val senderId: String?,
  val senderName: String,
  val senderAvatar: String = "",
  val senderLivoId: Long = 0,
  val messageType: String = "TEXT", // "TEXT", "GIFT", "SYSTEM", "JOIN"
  val content: String,
  val giftIcon: String? = null,
  val giftQuantity: Int = 0,
  val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 4. Direct Messages
// ============================================================================
@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String, // peerUserId or composite
  val peerUserId: String,
  val peerLivoId: Long,
  val peerName: String,
  val peerAvatar: String,
  val peerRole: UserRole,
  val lastMessage: String,
  val lastTimestamp: Long,
  val unreadCount: Int = 0
)

@Entity(tableName = "private_messages")
data class PrivateMessageEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val conversationId: String,
  val senderId: String,
  val receiverId: String,
  val content: String,
  val isRead: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 5. Coins, Wallet & Transactions
// Business Rule: $1 = 28,000 Coins
// ============================================================================
const val COINS_PER_USD = 28000L

@Entity(tableName = "coin_wallets")
data class CoinWalletEntity(
  @PrimaryKey val userId: String,
  val balance: Long = 0,
  val diamondBalance: Long = 0, // Received from gifts, redeemable
  val totalRecharged: Long = 0,
  val totalSpent: Long = 0,
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "coin_transactions")
data class CoinTransactionEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val transactionId: String, // Idempotency reference
  val userId: String,
  val amount: Long, // + for recharge, - for spend
  val balanceBefore: Long,
  val balanceAfter: Long,
  val type: String, // RECHARGE, GIFT_SEND, REWARD, ADMIN_ADJUST
  val referenceId: String? = null,
  val description: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class RechargePackage(
  val id: String,
  val usdPrice: Double,
  val coinAmount: Long,
  val bonusCoins: Long = 0,
  val isPopular: Boolean = false
)

// ============================================================================
// 6. Virtual Gifts
// ============================================================================
data class GiftItem(
  val id: String,
  val name: String,
  val coinPrice: Long,
  val iconEmoji: String,
  val category: String = "Popular",
  val animationKey: String = "STANDARD"
)

@Entity(tableName = "gift_transactions")
data class GiftTransactionEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val senderId: String,
  val senderName: String,
  val receiverId: String,
  val receiverName: String,
  val roomId: String?,
  val giftId: String,
  val giftName: String,
  val count: Int,
  val totalCoins: Long,
  val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 7. Rankings & Leaderboards
// ============================================================================
enum class RankingType { WEALTH, CHARM, ROOM }
enum class RankingPeriod { DAILY, WEEKLY, MONTHLY, ALL_TIME }

data class RankingItem(
  val rank: Int,
  val entityId: String,
  val numericId: Long, // Livo ID or Room Number
  val name: String,
  val avatarUrl: String,
  val score: Long,
  val level: Int = 1,
  val role: UserRole = UserRole.USER,
  val frameId: String? = null
)

// ============================================================================
// 8. Agency System
// ============================================================================
@Entity(tableName = "agencies")
data class AgencyEntity(
  @PrimaryKey val id: String,
  val agencyCode: String,
  val name: String,
  val ownerId: String,
  val ownerName: String,
  val description: String,
  val commissionRate: Double = 10.0,
  val totalMembers: Int = 0,
  val totalMonthlyDiamonds: Long = 0,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agency_members", primaryKeys = ["agencyId", "userId"])
data class AgencyMemberEntity(
  val agencyId: String,
  val userId: String,
  val userLivoId: Long,
  val userName: String,
  val userAvatar: String,
  val roleInAgency: String = "Host", // Manager, Host, Member
  val monthlyEarnings: Long = 0,
  val joinedAt: Long = System.currentTimeMillis()
)

// ============================================================================
// 9. Inventory, Frames & Decorations
// ============================================================================
data class FrameItem(
  val id: String,
  val name: String,
  val roleCategory: UserRole? = null,
  val borderColor: Long, // ARGB
  val glowColor: Long,
  val description: String
)

@Entity(tableName = "user_inventory", primaryKeys = ["userId", "itemId"])
data class UserInventoryEntity(
  val userId: String,
  val itemId: String,
  val itemName: String,
  val itemType: String, // AVATAR_FRAME, ROOM_FRAME
  val isEquipped: Boolean = false,
  val acquiredAt: Long = System.currentTimeMillis()
)

// ============================================================================
// 10. Events
// ============================================================================
data class EventItem(
  val id: String,
  val title: String,
  val description: String,
  val bannerRes: String,
  val startTime: Long,
  val endTime: Long,
  val rewards: String,
  val rules: String,
  val isActive: Boolean = true
)

// ============================================================================
// 11. Notifications
// ============================================================================
@Entity(tableName = "notifications")
data class NotificationEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val title: String,
  val content: String,
  val type: String, // GIFT, FOLLOW, INVITE, SYSTEM, COIN, ADMIN
  val isRead: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// 12. Moderation, Reports, Blocking & Audit Logs
// ============================================================================
@Entity(tableName = "reports")
data class ReportEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val reporterId: String,
  val reporterName: String,
  val targetType: String, // USER, ROOM
  val targetId: String,
  val targetName: String,
  val reason: String,
  val details: String,
  val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocks", primaryKeys = ["userId", "blockedUserId"])
data class BlockEntity(
  val userId: String,
  val blockedUserId: String,
  val blockedUserName: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val actorId: String,
  val actorName: String,
  val action: String, // GRANT_COINS, BAN_USER, ASSIGN_ROLE, CREATE_AGENCY
  val targetType: String,
  val targetId: String,
  val details: String,
  val timestamp: Long = System.currentTimeMillis()
)
