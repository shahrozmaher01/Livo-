package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  suspend fun getUserById(userId: String): UserEntity?

  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  fun observeUserById(userId: String): Flow<UserEntity?>

  @Query("SELECT * FROM users WHERE livoId = :livoId LIMIT 1")
  suspend fun getUserByLivoId(livoId: Long): UserEntity?

  @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
  suspend fun getUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users WHERE googleSubId = :subId LIMIT 1")
  suspend fun getUserByGoogleSubId(subId: String): UserEntity?

  @Query("SELECT * FROM users WHERE livoId = :livoId OR nickname LIKE '%' || :query || '%' LIMIT 30")
  suspend fun searchUsers(livoId: Long, query: String): List<UserEntity>

  @Query("SELECT * FROM users ORDER BY level DESC LIMIT 50")
  fun observeAllUsers(): Flow<List<UserEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Update
  suspend fun updateUser(user: UserEntity)

  @Query("SELECT COUNT(*) FROM follows WHERE followerId = :followerId AND followingId = :followingId")
  suspend fun isFollowing(followerId: String, followingId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFollow(follow: FollowEntity)

  @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
  suspend fun deleteFollow(followerId: String, followingId: String)

  @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
  suspend fun getFollowingCount(userId: String): Int

  @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
  suspend fun getFollowersCount(userId: String): Int

  @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
  fun observeFollowingCount(userId: String): Flow<Int>

  @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
  fun observeFollowersCount(userId: String): Flow<Int>
}

@Dao
interface RoomDao {
  @Query("SELECT * FROM rooms WHERE isActive = 1 ORDER BY listenersCount DESC, createdAt DESC")
  fun observeActiveRooms(): Flow<List<RoomEntity>>

  @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
  suspend fun getRoomById(roomId: String): RoomEntity?

  @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
  fun observeRoomById(roomId: String): Flow<RoomEntity?>

  @Query("SELECT * FROM rooms WHERE ownerId = :ownerId AND isActive = 1 LIMIT 1")
  suspend fun getRoomByOwnerId(ownerId: String): RoomEntity?

  @Query("SELECT * FROM rooms WHERE ownerId = :ownerId AND isActive = 1 LIMIT 1")
  fun observeRoomByOwnerId(ownerId: String): Flow<RoomEntity?>

  @Query("SELECT * FROM rooms WHERE roomNumber = :number OR title LIKE '%' || :query || '%'")
  suspend fun searchRooms(number: Long, query: String): List<RoomEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoom(room: RoomEntity)

  @Update
  suspend fun updateRoom(room: RoomEntity)

  @Query("UPDATE rooms SET isActive = 0 WHERE id = :roomId")
  suspend fun closeRoom(roomId: String)

  // Seats
  @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
  fun observeSeatsForRoom(roomId: String): Flow<List<RoomSeatEntity>>

  @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
  suspend fun getSeatsForRoom(roomId: String): List<RoomSeatEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSeats(seats: List<RoomSeatEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateSeat(seat: RoomSeatEntity)

  // Room chat messages
  @Query("SELECT * FROM room_messages WHERE roomId = :roomId ORDER BY timestamp ASC LIMIT 100")
  fun observeMessagesForRoom(roomId: String): Flow<List<RoomMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoomMessage(message: RoomMessageEntity)
}

@Dao
interface MessageDao {
  @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
  fun observeConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :convId LIMIT 1")
  suspend fun getConversation(convId: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conv: ConversationEntity)

  @Query("SELECT * FROM private_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
  fun observePrivateMessages(convId: String): Flow<List<PrivateMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPrivateMessage(message: PrivateMessageEntity)

  @Query("UPDATE private_messages SET isRead = 1 WHERE conversationId = :convId AND receiverId = :userId")
  suspend fun markMessagesAsRead(convId: String, userId: String)

  @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :convId")
  suspend fun clearUnreadCount(convId: String)

  @Query("SELECT SUM(unreadCount) FROM conversations")
  fun observeTotalUnreadCount(): Flow<Int?>
}

@Dao
interface WalletDao {
  @Query("SELECT * FROM coin_wallets WHERE userId = :userId LIMIT 1")
  fun observeWallet(userId: String): Flow<CoinWalletEntity?>

  @Query("SELECT * FROM coin_wallets WHERE userId = :userId LIMIT 1")
  suspend fun getWallet(userId: String): CoinWalletEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateWallet(wallet: CoinWalletEntity)

  @Query("SELECT * FROM coin_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
  fun observeTransactions(userId: String): Flow<List<CoinTransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(tx: CoinTransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGiftTransaction(tx: GiftTransactionEntity)

  @Query("SELECT * FROM gift_transactions WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC LIMIT 50")
  fun observeGiftTransactions(userId: String): Flow<List<GiftTransactionEntity>>

  // Diamond Ledger Transactions
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDiamondTransaction(tx: DiamondTransactionEntity)

  @Query("SELECT * FROM diamond_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
  fun observeDiamondTransactions(userId: String): Flow<List<DiamondTransactionEntity>>

  @Query("SELECT * FROM coin_transactions WHERE transactionId = :txId LIMIT 1")
  suspend fun getTransactionById(txId: String): CoinTransactionEntity?

  // Exchange Transactions
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExchangeTransaction(tx: ExchangeTransactionEntity)

  @Query("SELECT * FROM exchange_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
  fun observeExchangeTransactions(userId: String): Flow<List<ExchangeTransactionEntity>>

  @Query("SELECT * FROM exchange_transactions ORDER BY timestamp DESC LIMIT 200")
  fun observeAllExchangeTransactions(): Flow<List<ExchangeTransactionEntity>>

  @Query("SELECT * FROM exchange_transactions WHERE transactionId = :txId LIMIT 1")
  suspend fun getExchangeTransactionById(txId: String): ExchangeTransactionEntity?

  // Payout Requests (Diamond Withdrawals)
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayoutRequest(request: PayoutRequestEntity)

  @Update
  suspend fun updatePayoutRequest(request: PayoutRequestEntity)

  @Query("SELECT * FROM payout_requests WHERE userId = :userId ORDER BY createdAt DESC LIMIT 100")
  fun observePayoutRequests(userId: String): Flow<List<PayoutRequestEntity>>

  @Query("SELECT * FROM payout_requests ORDER BY createdAt DESC LIMIT 200")
  fun observeAllPayoutRequests(): Flow<List<PayoutRequestEntity>>

  @Query("SELECT * FROM payout_requests WHERE id = :id LIMIT 1")
  suspend fun getPayoutRequestById(id: String): PayoutRequestEntity?

  // Recharge Packages
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRechargePackages(packages: List<RechargePackageEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRechargePackage(pkg: RechargePackageEntity)

  @Query("SELECT * FROM recharge_packages WHERE isEnabled = 1 ORDER BY sortOrder ASC, usdPrice ASC")
  fun observeRechargePackages(): Flow<List<RechargePackageEntity>>

  @Query("DELETE FROM recharge_packages WHERE id = :id")
  suspend fun deleteRechargePackage(id: String)
}


@Dao
interface SocialDao {
  // Agencies
  @Query("SELECT * FROM agencies WHERE agencyCode = :code LIMIT 1")
  suspend fun getAgencyByCode(code: String): AgencyEntity?

  @Query("SELECT * FROM agencies ORDER BY totalMonthlyDiamonds DESC")
  fun observeAgencies(): Flow<List<AgencyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAgency(agency: AgencyEntity)

  @Query("SELECT * FROM agency_members WHERE agencyId = :agencyId")
  fun observeAgencyMembers(agencyId: String): Flow<List<AgencyMemberEntity>>

  @Query("SELECT * FROM agency_members WHERE userId = :userId LIMIT 1")
  suspend fun getUserAgencyMembership(userId: String): AgencyMemberEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAgencyMember(member: AgencyMemberEntity)

  // Notifications
  @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
  fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

  @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
  fun observeUnreadNotificationsCount(userId: String): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: NotificationEntity)

  @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
  suspend fun markAllNotificationsAsRead(userId: String)

  // Reports & Blocks
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReport(report: ReportEntity)

  @Query("SELECT * FROM reports ORDER BY timestamp DESC")
  fun observeReports(): Flow<List<ReportEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBlock(block: BlockEntity)

  @Query("DELETE FROM blocks WHERE userId = :userId AND blockedUserId = :blockedId")
  suspend fun removeBlock(userId: String, blockedId: String)

  @Query("SELECT * FROM blocks WHERE userId = :userId")
  fun observeBlocks(userId: String): Flow<List<BlockEntity>>

  // Inventory
  @Query("SELECT * FROM user_inventory WHERE userId = :userId")
  fun observeInventory(userId: String): Flow<List<UserInventoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInventoryItem(item: UserInventoryEntity)

  @Query("UPDATE user_inventory SET isEquipped = 0 WHERE userId = :userId")
  suspend fun unequipAllFrames(userId: String)

  @Query("UPDATE user_inventory SET isEquipped = 1 WHERE userId = :userId AND itemId = :itemId")
  suspend fun equipFrame(userId: String, itemId: String)

  // Audit Logs
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuditLog(log: AuditLogEntity)

  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
  fun observeAuditLogs(): Flow<List<AuditLogEntity>>
}

@Dao
interface PanelDao {
  // Panel Configurations
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPanelConfig(config: PanelConfigEntity)

  @Query("SELECT * FROM panel_configurations")
  fun observePanelConfigs(): Flow<List<PanelConfigEntity>>

  @Query("SELECT * FROM panel_configurations WHERE panelType = :panelType LIMIT 1")
  suspend fun getPanelConfigByType(panelType: String): PanelConfigEntity?

  // Panel Templates
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTemplate(template: PanelTemplateEntity)

  @Query("SELECT * FROM panel_templates")
  fun observeTemplates(): Flow<List<PanelTemplateEntity>>

  @Query("DELETE FROM panel_templates WHERE id = :templateId")
  suspend fun deleteTemplate(templateId: String)

  // User Panel Assignments
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAssignment(assignment: UserPanelAssignmentEntity)

  @Query("SELECT * FROM user_panel_assignments ORDER BY assignedAt DESC")
  fun observeAllAssignments(): Flow<List<UserPanelAssignmentEntity>>

  @Query("SELECT * FROM user_panel_assignments WHERE userId = :userId AND status = 'ACTIVE'")
  fun observeActiveAssignmentsForUser(userId: String): Flow<List<UserPanelAssignmentEntity>>

  @Query("SELECT * FROM user_panel_assignments WHERE userId = :userId AND panelType = :panelType LIMIT 1")
  suspend fun getAssignment(userId: String, panelType: String): UserPanelAssignmentEntity?

  @Query("UPDATE user_panel_assignments SET status = :status WHERE id = :assignmentId")
  suspend fun updateAssignmentStatus(assignmentId: String, status: String)

  @Query("DELETE FROM user_panel_assignments WHERE id = :assignmentId")
  suspend fun deleteAssignment(assignmentId: String)

  @Query("SELECT * FROM user_panel_assignments WHERE id = :assignmentId LIMIT 1")
  suspend fun getAssignmentById(assignmentId: String): UserPanelAssignmentEntity?

  // Currency Settings
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCurrencySettings(settings: SystemCurrencySettingsEntity)

  @Query("SELECT * FROM system_currency_settings WHERE id = 'global_currency_settings' LIMIT 1")
  fun observeCurrencySettings(): Flow<SystemCurrencySettingsEntity?>

  @Query("SELECT * FROM system_currency_settings WHERE id = 'global_currency_settings' LIMIT 1")
  suspend fun getCurrencySettings(): SystemCurrencySettingsEntity?
}

