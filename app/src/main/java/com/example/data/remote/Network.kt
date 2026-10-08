package com.example.data.remote

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import retrofit2.Response
import retrofit2.http.*
import java.util.concurrent.TimeUnit

enum class NetworkStatus {
  ONLINE,
  OFFLINE,
  CONNECTING,
  ERROR
}

data class GoogleAuthRequest(
  val idToken: String,
  val email: String,
  val displayName: String?,
  val photoUrl: String?
)

data class AuthResponse(
  val token: String,
  val user: UserEntity,
  val wallet: CoinWalletEntity
)

data class SendGiftRequest(
  val senderId: String,
  val receiverId: String,
  val roomId: String?,
  val giftId: String,
  val count: Int
)

data class SendGiftResponse(
  val success: Boolean,
  val newBalance: Long,
  val transactionId: String,
  val message: String
)

data class RechargeRequest(
  val packageId: String,
  val paymentReceiptToken: String
)

interface LivoApiService {
  @POST("api/v1/auth/google")
  suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): Response<AuthResponse>

  @GET("api/v1/users/{userId}")
  suspend fun getUserProfile(@Path("userId") userId: String): Response<UserEntity>

  @GET("api/v1/rooms")
  suspend fun getActiveRooms(): Response<List<RoomEntity>>

  @POST("api/v1/rooms")
  suspend fun createRoom(@Body room: RoomEntity): Response<RoomEntity>

  @POST("api/v1/wallet/gift")
  suspend fun sendGift(@Body request: SendGiftRequest): Response<SendGiftResponse>

  @POST("api/v1/wallet/recharge")
  suspend fun processRecharge(@Body request: RechargeRequest): Response<CoinWalletEntity>

  @GET("api/v1/rankings")
  suspend fun getRankings(
    @Query("type") type: String,
    @Query("period") period: String
  ): Response<List<RankingItem>>
}

class LivoWebSocketClient {
  private val client = OkHttpClient.Builder()
    .readTimeout(30, TimeUnit.SECONDS)
    .pingInterval(15, TimeUnit.SECONDS)
    .build()

  private var webSocket: WebSocket? = null
  private val _networkStatus = MutableStateFlow(NetworkStatus.ONLINE)
  val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

  private val _lastReceivedMessage = MutableStateFlow<String?>(null)
  val lastReceivedMessage: StateFlow<String?> = _lastReceivedMessage.asStateFlow()

  fun connectToRoom(roomId: String, serverUrl: String = "wss://api.livo.live/ws/room/") {
    disconnect()
    _networkStatus.value = NetworkStatus.CONNECTING

    val request = Request.Builder()
      .url("$serverUrl$roomId")
      .build()

    webSocket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        _networkStatus.value = NetworkStatus.ONLINE
        Log.d("LivoWS", "Connected to room $roomId")
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        _lastReceivedMessage.value = text
        Log.d("LivoWS", "Received text: $text")
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        _networkStatus.value = NetworkStatus.OFFLINE
        Log.w("LivoWS", "WS Failure: ${t.message}")
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        _networkStatus.value = NetworkStatus.OFFLINE
        Log.d("LivoWS", "WS Closed: $reason")
      }
    })
  }

  fun sendMessage(jsonPayload: String) {
    webSocket?.send(jsonPayload)
  }

  fun disconnect() {
    webSocket?.close(1000, "User left room")
    webSocket = null
  }
}
