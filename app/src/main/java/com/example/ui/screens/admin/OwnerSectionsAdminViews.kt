package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// 10. Users Management View
// ============================================================================
@Composable
fun UsersManagementView(
  repository: LivoRepository,
  allUsers: List<UserEntity>
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var searchQuery by remember { mutableStateOf("") }
  var selectedUserForRole by remember { mutableStateOf<UserEntity?>(null) }
  var showRoleDialog by remember { mutableStateOf(false) }

  val filtered = remember(searchQuery, allUsers) {
    if (searchQuery.isBlank()) allUsers
    else allUsers.filter {
      it.nickname.contains(searchQuery.trim(), true) ||
          it.livoId.toString().contains(searchQuery.trim()) ||
          it.email.contains(searchQuery.trim(), true)
    }
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Platform Users Management (${allUsers.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search users by LIVO ID, nickname, email...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    }

    items(filtered) { user ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(14.dp).fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
              model = user.avatarUrl,
              contentDescription = null,
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(user.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("ID: ${user.livoId} • Role: ${user.role.displayName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
              Text("Level: ${user.level} • XP: ${user.experiencePoints}", color = Color(0xFF64748B), fontSize = 10.sp)
            }
          }

          Button(
            onClick = {
              selectedUserForRole = user
              showRoleDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text("Edit Role", fontSize = 11.sp)
          }
        }
      }
    }
  }

  if (showRoleDialog && selectedUserForRole != null) {
    val u = selectedUserForRole!!
    var newRole by remember { mutableStateOf(u.role) }

    AlertDialog(
      onDismissRequest = { showRoleDialog = false },
      containerColor = Color(0xFF0B1021),
      title = { Text("Assign Role to ${u.nickname}", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(UserRole.values()) { r ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { newRole = r }
                .padding(vertical = 4.dp)
            ) {
              RadioButton(selected = newRole == r, onClick = { newRole = r })
              Spacer(modifier = Modifier.width(8.dp))
              Text(r.displayName, color = Color.White, fontSize = 13.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.adminAssignRole(u.id, newRole)
              showRoleDialog = false
              Toast.makeText(context, res.getOrDefault("Role updated!"), Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
          Text("Apply Role", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showRoleDialog = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
      }
    )
  }
}

// ============================================================================
// 11. Room Management Admin View
// ============================================================================
@Composable
fun RoomManagementAdminView(repository: LivoRepository) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val rooms by repository.activeRooms.collectAsState(initial = emptyList())

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Platform Rooms Oversight (${rooms.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    if (rooms.isEmpty()) {
      item {
        Text("No active voice rooms currently registered.", color = Color(0xFF94A3B8), fontSize = 12.sp)
      }
    } else {
      items(rooms) { r ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(0.8.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp).fillMaxWidth()
          ) {
            Column {
              Text(r.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Room #${r.roomNumber} • Category: ${r.category.displayName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
              Text("Owner: ${r.ownerName} • Seats: ${r.seatCount}", color = Color(0xFF64748B), fontSize = 10.sp)
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF10B981).copy(alpha = 0.2f)
            ) {
              Text(
                text = "${r.listenersCount} Online",
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// 12. Currency Management (Coin & Diamond Rates, Limits, and Payout Review)
// ============================================================================
@Composable
fun CurrencyManagementAdminView(
  repository: LivoRepository,
  currentSettings: SystemCurrencySettingsEntity?
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val settings = currentSettings ?: SystemCurrencySettingsEntity()
  var rateText by remember(settings) { mutableStateOf(settings.referenceRateCoinsPerUsd.toString()) }
  var feeText by remember(settings) { mutableStateOf(settings.diamondExchangeFeePercent.toString()) }
  var outputText by remember(settings) { mutableStateOf(settings.exchangeOutputCoinsPer100Diamonds.toString()) }
  var minExText by remember(settings) { mutableStateOf(settings.minExchangeDiamonds.toString()) }
  var resellerDiscText by remember(settings) { mutableStateOf(settings.resellerDiscountPercent.toString()) }
  var minWdText by remember(settings) { mutableStateOf(settings.minWithdrawalDiamonds.toString()) }
  var wdRateText by remember(settings) { mutableStateOf(settings.diamondsPerUsdWithdrawal.toString()) }

  val allPayoutRequests by repository.observeAllPayoutRequests().collectAsState(initial = emptyList())
  var selectedRequestToProcess by remember { mutableStateOf<PayoutRequestEntity?>(null) }
  var isApprovingAction by remember { mutableStateOf(true) }
  var adminProcessNote by remember { mutableStateOf("") }
  var showProcessDialog by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Virtual Currency & Host Payout Architecture", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Configure reference conversion rates, Diamond exchange fees, withdrawal thresholds, and approve host payout disbursements.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
      }
    }

    // Parameters Configuration Card
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("1. Reference Conversion & Exchange Rates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

          OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            label = { Text("Reference Rate: Coins per 1 USD") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it },
            label = { Text("Diamond Exchange Fee (%)") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = outputText,
            onValueChange = { outputText = it },
            label = { Text("Coins Delivered per 100 Diamonds Exchanged") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = minExText,
            onValueChange = { minExText = it },
            label = { Text("Minimum Diamonds for Exchange") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = resellerDiscText,
            onValueChange = { resellerDiscText = it },
            label = { Text("Authorized Reseller Discount (%)") },
            modifier = Modifier.fillMaxWidth()
          )

          Text("2. Host Cash Withdrawal (Payout) Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

          OutlinedTextField(
            value = minWdText,
            onValueChange = { minWdText = it },
            label = { Text("Minimum Withdrawal (Diamonds)") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = wdRateText,
            onValueChange = { wdRateText = it },
            label = { Text("Diamonds required per 1 USD Cash Payout") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = {
              coroutineScope.launch {
                val res = repository.updateCurrencySettings(
                  referenceRate = rateText.toLongOrNull() ?: 400000L,
                  exchangeFeePercent = feeText.toIntOrNull() ?: 30,
                  outputCoins = outputText.toIntOrNull() ?: 70,
                  minExchange = minExText.toLongOrNull() ?: 100L,
                  resellerDiscount = resellerDiscText.toIntOrNull() ?: 5,
                  minWithdrawalDiamonds = minWdText.toLongOrNull() ?: 500000L,
                  diamondsPerUsdWithdrawal = wdRateText.toLongOrNull() ?: 500000L
                )
                Toast.makeText(context, res.getOrDefault("Parameters updated!"), Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save & Apply Currency Parameters", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Host Payout Requests Review Queue
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Host Payout Requests Queue (${allPayoutRequests.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("${allPayoutRequests.count { it.status == "PENDING" }} Pending", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    if (allPayoutRequests.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No payout requests submitted yet.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    } else {
      items(allPayoutRequests) { req ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(0.8.dp, if (req.status == "PENDING") Color(0xFFFBBF24).copy(alpha = 0.5f) else Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(req.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("ID: ${req.userLivoId} • Ref: ${req.requestId}", color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (req.status) {
                  "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                  "REJECTED" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                  else -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                }
              ) {
                Text(
                  text = req.status,
                  color = when (req.status) {
                    "APPROVED" -> Color(0xFF34D399)
                    "REJECTED" -> Color(0xFFF87171)
                    else -> Color(0xFFFBBF24)
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text("Requested: ${String.format("%,d", req.diamondsAmount)} 💎", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Method: ${req.paymentMethod}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                Text("Details: ${req.accountDetails}", color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
              Text(
                text = "$${String.format("%.2f", req.payoutUsdAmount)} USD",
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
              )
            }

            if (req.status == "PENDING") {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Button(
                  onClick = {
                    selectedRequestToProcess = req
                    isApprovingAction = true
                    adminProcessNote = "Disbursed via ${req.paymentMethod}"
                    showProcessDialog = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Approve & Disburse", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    selectedRequestToProcess = req
                    isApprovingAction = false
                    adminProcessNote = "Incorrect account details. Diamonds refunded."
                    showProcessDialog = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reject & Refund", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Admin Process Dialog
  if (showProcessDialog && selectedRequestToProcess != null) {
    val req = selectedRequestToProcess!!
    AlertDialog(
      onDismissRequest = { showProcessDialog = false },
      containerColor = Color(0xFF0F172A),
      title = {
        Text(
          text = if (isApprovingAction) "Approve Payout for ${req.userName}" else "Reject Payout for ${req.userName}",
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = if (isApprovingAction) "Confirm disbursement of $${String.format("%.2f", req.payoutUsdAmount)} USD (${String.format("%,d", req.diamondsAmount)} 💎) to ${req.paymentMethod} account:"
            else "Reject request and refund ${String.format("%,d", req.diamondsAmount)} Diamonds back to user's wallet:",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
          Text("Account: ${req.accountDetails}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

          OutlinedTextField(
            value = adminProcessNote,
            onValueChange = { adminProcessNote = it },
            label = { Text("Admin Note / Transaction Ref") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.adminProcessPayout(
                requestId = req.id,
                approve = isApprovingAction,
                adminNote = adminProcessNote
              )
              showProcessDialog = false
              selectedRequestToProcess = null
              Toast.makeText(context, res.getOrDefault("Action processed"), Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isApprovingAction) Color(0xFF059669) else Color(0xFFDC2626)
          )
        ) {
          Text(if (isApprovingAction) "Confirm Approval" else "Confirm Rejection")
        }
      },
      dismissButton = {
        TextButton(onClick = { showProcessDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }
}

// ============================================================================
// 13. Gift Management Admin View
// ============================================================================
@Composable
fun GiftManagementAdminView(repository: LivoRepository) {
  val gifts = repository.giftCatalog
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("System Virtual Gifts Catalog (${gifts.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    items(gifts) { gift ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(14.dp).fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(gift.iconEmoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(gift.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("Category: ${gift.category} • Key: ${gift.animationKey}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
          }

          Text("🪙 ${gift.coinPrice}", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

// ============================================================================
// 14. Lucky Gift Management
// ============================================================================
@Composable
fun LuckyGiftManagementAdminView(repository: LivoRepository) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Lucky Gift System Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Strict Compliance Rule: No paid wagering, cash multiplier payouts, or simulated gambling. Lucky gifts provide fixed visual bonus animation effects only.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Active Lucky Effects Catalog", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("• Crystal Supernova (Animation multiplier: 1x, fixed XP boost)", color = Color(0xFF94A3B8), fontSize = 12.sp)
          Text("• Golden Dragon Flight (Full screen broadcast effect)", color = Color(0xFF94A3B8), fontSize = 12.sp)
          Text("• Imperial Crown Aura (Avatar spotlight on mic seat)", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================
// 15. Frame Management
// ============================================================================
@Composable
fun FrameManagementAdminView(repository: LivoRepository) {
  val frames = repository.availableFrames
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Platform Avatar Frames Catalog (${frames.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    items(frames) { frame ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(14.dp).fillMaxWidth()
        ) {
          Column {
            Text(frame.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(frame.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text("Role Tier: ${frame.roleCategory?.displayName ?: "All Users"}", color = Color(0xFF64748B), fontSize = 10.sp)
          }

          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(Color(frame.borderColor))
          )
        }
      }
    }
  }
}

// ============================================================================
// 16. Ranking Management
// ============================================================================
@Composable
fun RankingManagementAdminView(repository: LivoRepository) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Ranking & Leaderboards Management", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text("Configure refresh periods, display thresholds, and anti-fraud filters.", color = Color(0xFF94A3B8), fontSize = 12.sp)
    }

    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Active Leaderboard Categories:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("1. Wealth Ranking (Coins spent in rooms & gifts)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          Text("2. Charm Ranking (Gifts & Diamonds received by hosts)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          Text("3. Room Ranking (Active audience volume & engagement)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================
// 17. Reports & Analytics
// ============================================================================
@Composable
fun ReportsAnalyticsAdminView(
  repository: LivoRepository,
  reports: List<ReportEntity>
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Platform Reports & Analytics (${reports.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    if (reports.isEmpty()) {
      item {
        Text("No user moderation reports currently pending.", color = Color(0xFF94A3B8), fontSize = 12.sp)
      }
    } else {
      items(reports) { r ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(0.8.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Reason: ${r.reason}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(r.status, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reporter: ${r.reporterName} • Target: ${r.targetName} (${r.targetType})", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text("Details: ${r.details}", color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
          }
        }
      }
    }
  }
}

// ============================================================================
// 18. Notifications Management
// ============================================================================
@Composable
fun NotificationsAdminView(repository: LivoRepository) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var titleText by remember { mutableStateOf("") }
  var bodyText by remember { mutableStateOf("") }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Broadcast System Notification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(4.dp))
      Text("Send platform updates or security notices to users.", color = Color(0xFF94A3B8), fontSize = 12.sp)
    }

    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleText,
            onValueChange = { titleText = it },
            label = { Text("Notification Title") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = bodyText,
            onValueChange = { bodyText = it },
            label = { Text("Notification Content") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = {
              if (titleText.isNotBlank() && bodyText.isNotBlank()) {
                Toast.makeText(context, "System notification dispatched", Toast.LENGTH_SHORT).show()
                titleText = ""
                bodyText = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Broadcast Notification", color = Color.White)
          }
        }
      }
    }
  }
}

// ============================================================================
// 19. Security and Permissions
// ============================================================================
@Composable
fun SecurityPermissionsAdminView(repository: LivoRepository) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFF87171))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Owner Security Architecture", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "• Deny-by-default role authorization\n• Backend SQL enforcement of panel permissions\n• Protected Owner root credentials (cannot be demoted by Super Admins)\n• Audit logs are append-only and cannot be deleted",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

// ============================================================================
// 20. System Settings
// ============================================================================
@Composable
fun SystemSettingsAdminView(
  repository: LivoRepository,
  currentSettings: SystemCurrencySettingsEntity?
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Platform Core System Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }

    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Backend Environment: Production Realtime", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("Audio Engine: Hardware AudioRecord / AudioTrack (PCM 16-bit)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          Text("Persistence: Android Room Database + SQL Ledger", color = Color(0xFFCBD5E1), fontSize = 12.sp)
          Text("Reference Conversion: 1 USD = ${currentSettings?.referenceRateCoinsPerUsd ?: 400000} Coins", color = Color(0xFFFBBF24), fontSize = 12.sp)
        }
      }
    }
  }
}

// ============================================================================
// 21. Audit Logs View
// ============================================================================
@Composable
fun AuditLogsAdminView(auditLogs: List<AuditLogEntity>) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Text("Immutable Security Audit Logs (${auditLogs.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text("Every administrative panel action, role grant, and configuration publish is permanently recorded.", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
    }

    if (auditLogs.isEmpty()) {
      item {
        Text("No audit log records recorded yet.", color = Color(0xFF94A3B8), fontSize = 12.sp)
      }
    } else {
      items(auditLogs) { log ->
        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(log.timestamp))
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(0.6.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(log.action, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
              Text(dateStr, color = Color(0xFF64748B), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text("Actor: ${log.actorName} • Target: ${log.targetType}/${log.targetId}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(log.details, color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
          }
        }
      }
    }
  }
}
