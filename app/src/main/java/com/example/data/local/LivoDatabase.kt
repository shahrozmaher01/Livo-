package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*

class Converters {
  @TypeConverter
  fun fromUserRole(value: UserRole): String = value.name

  @TypeConverter
  fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.USER)

  @TypeConverter
  fun fromRoomCategory(value: RoomCategory): String = value.name

  @TypeConverter
  fun toRoomCategory(value: String): RoomCategory = runCatching { RoomCategory.valueOf(value) }.getOrDefault(RoomCategory.CHAT)
}

@Database(
  entities = [
    UserEntity::class,
    FollowEntity::class,
    RoomEntity::class,
    RoomSeatEntity::class,
    RoomMessageEntity::class,
    ConversationEntity::class,
    PrivateMessageEntity::class,
    CoinWalletEntity::class,
    CoinTransactionEntity::class,
    GiftTransactionEntity::class,
    AgencyEntity::class,
    AgencyMemberEntity::class,
    UserInventoryEntity::class,
    NotificationEntity::class,
    ReportEntity::class,
    BlockEntity::class,
    AuditLogEntity::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LivoDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun roomDao(): RoomDao
  abstract fun messageDao(): MessageDao
  abstract fun walletDao(): WalletDao
  abstract fun socialDao(): SocialDao

  companion object {
    @Volatile
    private var INSTANCE: LivoDatabase? = null

    fun getDatabase(context: Context): LivoDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LivoDatabase::class.java,
          "livo_production.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
