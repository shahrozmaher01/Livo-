package com.example.ui.navigation

sealed class Screen(val route: String) {
  object Login : Screen("login")
  object Main : Screen("main")
  object Search : Screen("search")
  object VoiceRoom : Screen("voice_room/{roomId}") {
    fun createRoute(roomId: String) = "voice_room/$roomId"
  }
  object ChatDetail : Screen("chat/{userId}") {
    fun createRoute(userId: String) = "chat/$userId"
  }
  object Wallet : Screen("wallet")
  object Backpack : Screen("backpack")
  object Agency : Screen("agency")
  object AdminPanel : Screen("admin_panel")
  object Events : Screen("events")
  object Notifications : Screen("notifications")
  object EditProfile : Screen("edit_profile")
}

enum class BottomTab(val title: String) {
  HOME("Home"),
  RANKINGS("Ranks"),
  MESSAGES("Messages"),
  PROFILE("Me")
}
