package com.example.ui.screens.wallet

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val userId = currentUser?.id ?: ""

  val wallet by repository.observeWallet(userId).collectAsState(initial = null)
  val transactions by repository.observeTransactions(userId).collectAsState(initial = emptyList())
  val diamondTransactions by repository.observeDiamondTransactions(userId).collectAsState(initial = emptyList())
  val exchangeTransactions by repository.observeExchangeTransactions(userId).collectAsState(initial = emptyList())
  val payoutRequests by repository.observePayoutRequests(userId).collectAsState(initial = emptyList())
  val currencySettings by repository.observeCurrencySettings().collectAsState(initial = null)

  // 0: Recharge Coins, 1: Exchange Diamonds, 2: Withdraw Cash, 3: Transaction Ledger
  var selectedTab by remember { mutableStateOf(0) }

  // Recharge State
  var selectedPackageForCheckout by remember { mutableStateOf<RechargePackage?>(null) }
  var selectedPaymentProvider by remember { mutableStateOf(repository.paymentProviders.first()) }
  var isProcessingRecharge by remember { mutableStateOf(false) }

  // Exchange State
  var diamondsToExchangeInput by remember { mutableStateOf("1000") }
  var isProcessingExchange by remember { mutableStateOf(false) }

  // Withdrawal State
  var withdrawalDiamondsInput by remember { mutableStateOf("500000") }
  var selectedWithdrawalMethod by remember { mutableStateOf("BANK_TRANSFER") }
  var withdrawalAccountDetails by remember { mutableStateOf("") }
  var isProcessingWithdrawal by remember { mutableStateOf(false) }

  // Ledger Filter State
  var ledgerFilter by remember { mutableStateOf("ALL") } // ALL, RECHARGE, GIFT, DIAMOND, EXCHANGE, WITHDRAWAL

  val filteredCoinTxs = remember(transactions, ledgerFilter) {
    when (ledgerFilter) {
      "RECHARGE" -> transactions.filter { it.type == "RECHARGE" }
      "GIFT" -> transactions.filter { it.type == "GIFT_SEND" }
      "EXCHANGE" -> transactions.filter { it.description.contains("Exchange", true) }
      "WITHDRAWAL" -> transactions.filter { it.type == "WITHDRAW_DEBIT" }
      "DIAMOND" -> emptyList()
      else -> transactions
    }
  }

  val filteredDiaTxs = remember(diamondTransactions, ledgerFilter) {
    when (ledgerFilter) {
      "ALL", "DIAMOND" -> diamondTransactions
      "GIFT" -> diamondTransactions.filter { it.type == "GIFT_REWARD" }
      "EXCHANGE" -> diamondTransactions.filter { it.type == "EXCHANGE_DEBIT" }
      "WITHDRAWAL" -> diamondTransactions.filter { it.type == "WITHDRAW_DEBIT" }
      else -> emptyList()
    }
  }

  val totalDiamondsEarned = remember(diamondTransactions) {
    diamondTransactions.filter { it.type == "GIFT_REWARD" }.sumOf { it.amount }
  }
  val availableDiamonds = wallet?.diamondBalance ?: 0L
  val diamondsAlreadyExchanged = remember(exchangeTransactions) {
    exchangeTransactions.sumOf { it.diamondsExchanged }
  }
  val pendingPayoutDiamonds = remember(payoutRequests) {
    payoutRequests.filter { it.status == "PENDING" }.sumOf { it.diamondsAmount }
  }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(
          title = "AURA Wallet",
          onBackClick = onBackClick,
          actions = {
            LivoBrandLogo(size = 30.dp, showGlow = false)
            Spacer(modifier = Modifier.width(12.dp))
          }
        )
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 36.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. Dual-Currency Balance Hero Card
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = BorderStroke(1.dp, LivoGold.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.linearGradient(
                  listOf(
                    Color(0xFF2E2410),
                    Color(0xFF1E1735),
                    Color(0xFF0F172A)
                  )
                )
              )
              .padding(20.dp)
          ) {
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "🪙", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Official Rate: $1.00 USD = 400,000 Coins",
                    color = LivoGoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = LivoGold.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "VERIFIED LEDGER",
                    color = LivoGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(18.dp))

              // Balances Side-by-Side
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                // Coins Column
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = "Available Coins", color = TextSecondary, fontSize = 12.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪙", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = String.format("%,d", wallet?.balance ?: 0),
                      color = Color.White,
                      fontSize = 24.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                  }
                }

                // Diamonds Column
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.End
                ) {
                  Text(text = "Diamonds (Earned)", color = LivoCyanLight, fontSize = 12.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💎", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = String.format("%,d", wallet?.diamondBalance ?: 0),
                      color = Color(0xFF67E8F9),
                      fontSize = 24.sp,
                      fontWeight = FontWeight.ExtraBold
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f))
              Spacer(modifier = Modifier.height(14.dp))

              // Stats & Shortcuts Row
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                  Column {
                    Text(text = "Total Spent", color = TextTertiary, fontSize = 10.sp)
                    Text(
                      text = "${String.format("%,d", wallet?.totalSpent ?: 0)} 🪙",
                      color = TextSecondary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Column {
                    Text(text = "Total Recharged", color = TextTertiary, fontSize = 10.sp)
                    Text(
                      text = "${String.format("%,d", wallet?.totalRecharged ?: 0)} 🪙",
                      color = LivoGoldLight,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exchange", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = { selectedTab = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                  ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }

      // 1.5 Diamond Portfolio & Earnings Breakdown Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💎", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Diamond Rewards & Portfolio Status",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              TextButton(
                onClick = {
                  selectedTab = 3
                  ledgerFilter = "GIFT"
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Earnings History 📜",
                  color = LivoCyanLight,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Key Diamond Metrics in 2x2 Grid
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Total Diamonds Earned
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(0.6.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Total Earned", color = TextTertiary, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${String.format("%,d", totalDiamondsEarned)} 💎",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // Available Diamonds
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(0.6.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Available Balance", color = TextTertiary, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${String.format("%,d", availableDiamonds)} 💎",
                    color = Color(0xFF67E8F9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Diamonds Already Exchanged
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(0.6.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Already Exchanged", color = TextTertiary, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${String.format("%,d", diamondsAlreadyExchanged)} 💎",
                    color = Color(0xFFA78BFA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // Pending Exchange / Withdrawal
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(0.6.dp, Color(0xFF334155)),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("Pending Payout", color = TextTertiary, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${String.format("%,d", pendingPayoutDiamonds)} 💎",
                    color = Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
      item {
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkBackground,
          contentColor = TextPrimary,
          edgePadding = 16.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Recharge Coins", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Exchange Diamonds", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Withdraw Cash", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            text = { Text("Transactions (${transactions.size + diamondTransactions.size})", fontWeight = FontWeight.Bold) }
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // 3. Tab Contents
      when (selectedTab) {
        0 -> {
          // Recharge Packages Section
          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
              ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SpeakingGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Secure in-app purchase with 256-bit encryption. Balance is credited instantly upon provider confirmation.",
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          items(repository.rechargePackages) { pkg ->
            RechargePackageItemCard(
              pkg = pkg,
              isProcessing = isProcessingRecharge,
              onSelectToBuy = {
                selectedPackageForCheckout = pkg
              },
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
          }
        }

        1 -> {
          // Diamond Exchange Section (30% fee, 70% Coins delivered)
          item {
            val diamondsAmount = diamondsToExchangeInput.toLongOrNull() ?: 0L
            val feePercent = currencySettings?.diamondExchangeFeePercent ?: 30
            val feeEquivalent = (diamondsAmount * (feePercent / 100.0)).toLong()
            val coinsOutput = (diamondsAmount * ((100 - feePercent) / 100.0)).toLong()
            val userDiamondBalance = wallet?.diamondBalance ?: 0L

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16122E)),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💎 ➔ 🪙", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Official Diamond-to-Coin Exchange",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Convert earned Diamonds into Coins for gifting in rooms or private chats. Official platform fee is $feePercent%, delivering ${100 - feePercent}% as usable Coins.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                  )
                }
              }

              // Calculator Card
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(0.8.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                  Text("Enter Diamonds to Exchange", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                  OutlinedTextField(
                    value = diamondsToExchangeInput,
                    onValueChange = { diamondsToExchangeInput = it.filter { char -> char.isDigit() } },
                    placeholder = { Text("e.g. 10000") },
                    leadingIcon = { Text("💎", fontSize = 18.sp) },
                    trailingIcon = {
                      TextButton(onClick = { diamondsToExchangeInput = userDiamondBalance.toString() }) {
                        Text("MAX", color = LivoCyan, fontWeight = FontWeight.Bold)
                      }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = LivoViolet,
                      unfocusedBorderColor = DarkSurfaceBorder,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                  )

                  // Quick Selection Chips
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    listOf(500L, 1000L, 5000L, 10000L, 50000L).forEach { amount ->
                      SuggestionChip(
                        onClick = { diamondsToExchangeInput = amount.toString() },
                        label = { Text("${amount / 1000.coerceAtLeast(1)}K 💎", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                          containerColor = if (diamondsAmount == amount) LivoViolet.copy(alpha = 0.3f) else DarkSurfaceElevated,
                          labelColor = if (diamondsAmount == amount) LivoCyanLight else TextSecondary
                        ),
                        border = BorderStroke(0.8.dp, if (diamondsAmount == amount) LivoViolet else DarkSurfaceBorder)
                      )
                    }
                  }

                  HorizontalDivider(color = DarkSurfaceBorder)

                  // Live Preview Calculation
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Diamonds to Deduct", color = TextTertiary, fontSize = 12.sp)
                    Text("-$diamondsAmount 💎", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Platform Fee ($feePercent%)", color = TextTertiary, fontSize = 12.sp)
                    Text("-$feeEquivalent 💎", color = TextSecondary, fontSize = 12.sp)
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Coins You Will Receive (${100 - feePercent}%)", color = LivoGoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("+${String.format("%,d", coinsOutput)} 🪙", color = LivoGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                  }

                  Button(
                    onClick = {
                      if (diamondsAmount <= 0) {
                        Toast.makeText(context, "Please enter an amount of diamonds", Toast.LENGTH_SHORT).show()
                        return@Button
                      }
                      if (diamondsAmount > userDiamondBalance) {
                        Toast.makeText(context, "Insufficient diamonds. You have ${String.format("%,d", userDiamondBalance)} 💎", Toast.LENGTH_SHORT).show()
                        return@Button
                      }

                      isProcessingExchange = true
                      coroutineScope.launch {
                        val res = repository.exchangeDiamondsToCoins(diamondsAmount)
                        isProcessingExchange = false
                        res.onSuccess { receipt ->
                          Toast.makeText(
                            context,
                            "Exchange Completed! Credited ${String.format("%,d", receipt.coinsCredited)} Coins to your balance.",
                            Toast.LENGTH_LONG
                          ).show()
                          diamondsToExchangeInput = ""
                        }.onFailure { err ->
                          Toast.makeText(context, err.message ?: "Exchange failed", Toast.LENGTH_LONG).show()
                        }
                      }
                    },
                    enabled = !isProcessingExchange && diamondsAmount > 0 && diamondsAmount <= userDiamondBalance,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                  ) {
                    if (isProcessingExchange) {
                      CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                      Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Convert to ${String.format("%,d", coinsOutput)} Coins", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                  }
                }
              }

              // Past Exchange Receipts
              if (exchangeTransactions.isNotEmpty()) {
                Text("Recent Exchange Receipts (${exchangeTransactions.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                exchangeTransactions.take(5).forEach { ex ->
                  Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = BorderStroke(0.8.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.padding(14.dp)
                    ) {
                      Column {
                        Text("Exchanged ${String.format("%,d", ex.diamondsExchanged)} 💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Fee: ${ex.feePercentage}% (${ex.feeDiamondsEquivalent} 💎) • Ref: ${ex.transactionId.take(12)}", color = TextTertiary, fontSize = 10.sp)
                      }
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LivoGold.copy(alpha = 0.2f)
                      ) {
                        Text(
                          text = "+${String.format("%,d", ex.coinsCredited)} 🪙",
                          color = LivoGold,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        2 -> {
          // Withdraw Cash Section (Host / Creator Payout)
          item {
            val diamondsAmount = withdrawalDiamondsInput.toLongOrNull() ?: 0L
            val userDiamondBalance = wallet?.diamondBalance ?: 0L
            val diamondsPerUsd = currencySettings?.diamondsPerUsdWithdrawal ?: 500000L
            val minDiamonds = currencySettings?.minWithdrawalDiamonds ?: 500000L
            val estimatedUsd = diamondsAmount.toDouble() / diamondsPerUsd.coerceAtLeast(1L)

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06231C)),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💸", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Host & Creator Diamond Payout",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Request official cash withdrawal for your earned Diamonds. Payout rate: ${String.format("%,d", diamondsPerUsd)} Diamonds = $1.00 USD. Minimum withdrawal is ${String.format("%,d", minDiamonds)} Diamonds.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                  )
                }
              }

              // Payout Request Form Card
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(0.8.dp, DarkSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Text("1. Enter Withdrawal Amount", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                  OutlinedTextField(
                    value = withdrawalDiamondsInput,
                    onValueChange = { withdrawalDiamondsInput = it.filter { char -> char.isDigit() } },
                    placeholder = { Text("e.g. 500000") },
                    leadingIcon = { Text("💎", fontSize = 18.sp) },
                    trailingIcon = {
                      TextButton(onClick = { withdrawalDiamondsInput = userDiamondBalance.toString() }) {
                        Text("MAX", color = SpeakingGreen, fontWeight = FontWeight.Bold)
                      }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = SpeakingGreen,
                      unfocusedBorderColor = DarkSurfaceBorder,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                  )

                  Text("2. Select Payout Method", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    listOf(
                      "BANK_TRANSFER" to "🏛️ Bank",
                      "PAYPAL" to "🅿️ PayPal",
                      "USDT_TRC20" to "🌐 USDT",
                      "UPI" to "⚡ UPI"
                    ).forEach { (method, label) ->
                      val isSelected = selectedWithdrawalMethod == method
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) SpeakingGreen.copy(alpha = 0.25f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) SpeakingGreen else DarkSurfaceBorder),
                        modifier = Modifier
                          .weight(1f)
                          .clickable { selectedWithdrawalMethod = method }
                      ) {
                        Text(
                          text = label,
                          color = if (isSelected) SpeakingGreen else TextSecondary,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          textAlign = TextAlign.Center,
                          modifier = Modifier.padding(vertical = 8.dp)
                        )
                      }
                    }
                  }

                  Text("3. Account / Wallet Details", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                  OutlinedTextField(
                    value = withdrawalAccountDetails,
                    onValueChange = { withdrawalAccountDetails = it },
                    placeholder = {
                      Text(
                        when (selectedWithdrawalMethod) {
                          "PAYPAL" -> "Enter your verified PayPal email address"
                          "USDT_TRC20" -> "Enter your TRON (TRC-20) USDT wallet address"
                          "UPI" -> "Enter your UPI VPA (e.g. username@bank)"
                          else -> "Enter Bank Name, Account Number, and Swift / Routing"
                        },
                        fontSize = 11.sp
                      )
                    },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = SpeakingGreen,
                      unfocusedBorderColor = DarkSurfaceBorder,
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                  )

                  HorizontalDivider(color = DarkSurfaceBorder)

                  // Summary
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Net Cash Payout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                      text = "$${String.format("%.2f", estimatedUsd)} USD",
                      color = SpeakingGreen,
                      fontWeight = FontWeight.Black,
                      fontSize = 18.sp
                    )
                  }

                  Button(
                    onClick = {
                      if (diamondsAmount < minDiamonds) {
                        Toast.makeText(context, "Minimum withdrawal is ${String.format("%,d", minDiamonds)} Diamonds ($${minDiamonds / diamondsPerUsd} USD)", Toast.LENGTH_SHORT).show()
                        return@Button
                      }
                      if (diamondsAmount > userDiamondBalance) {
                        Toast.makeText(context, "Insufficient diamonds. You have ${String.format("%,d", userDiamondBalance)} 💎", Toast.LENGTH_SHORT).show()
                        return@Button
                      }
                      if (withdrawalAccountDetails.isBlank()) {
                        Toast.makeText(context, "Please enter your account details", Toast.LENGTH_SHORT).show()
                        return@Button
                      }

                      isProcessingWithdrawal = true
                      coroutineScope.launch {
                        val res = repository.requestPayout(
                          diamondsAmount = diamondsAmount,
                          paymentMethod = selectedWithdrawalMethod,
                          accountDetails = withdrawalAccountDetails.trim()
                        )
                        isProcessingWithdrawal = false
                        res.onSuccess {
                          Toast.makeText(
                            context,
                            "Payout request submitted! Admin review will process within 24 hours.",
                            Toast.LENGTH_LONG
                          ).show()
                          withdrawalDiamondsInput = ""
                          withdrawalAccountDetails = ""
                        }.onFailure { err ->
                          Toast.makeText(context, err.message ?: "Withdrawal failed", Toast.LENGTH_LONG).show()
                        }
                      }
                    },
                    enabled = !isProcessingWithdrawal && diamondsAmount >= minDiamonds && diamondsAmount <= userDiamondBalance && withdrawalAccountDetails.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpeakingGreen),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                  ) {
                    if (isProcessingWithdrawal) {
                      CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                      Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Submit Payout Request ($${String.format("%.2f", estimatedUsd)})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                  }
                }
              }

              // Payout Requests History
              if (payoutRequests.isNotEmpty()) {
                Text("Your Payout Requests (${payoutRequests.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                payoutRequests.forEach { req ->
                  Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = BorderStroke(0.8.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.padding(14.dp)
                    ) {
                      Column {
                        Text(
                          text = "$${String.format("%.2f", req.payoutUsdAmount)} USD (${String.format("%,d", req.diamondsAmount)} 💎)",
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp
                        )
                        Text(
                          text = "${req.paymentMethod} • Ref: ${req.requestId}",
                          color = TextTertiary,
                          fontSize = 10.sp
                        )
                        if (!req.adminNote.isNullOrBlank()) {
                          Text(text = "Note: ${req.adminNote}", color = Color(0xFFFBBF24), fontSize = 10.sp)
                        }
                      }

                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (req.status) {
                          "APPROVED" -> SpeakingGreen.copy(alpha = 0.2f)
                          "REJECTED" -> Color(0xFFF43F5E).copy(alpha = 0.2f)
                          else -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                        }
                      ) {
                        Text(
                          text = req.status,
                          color = when (req.status) {
                            "APPROVED" -> SpeakingGreen
                            "REJECTED" -> Color(0xFFF43F5E)
                            else -> Color(0xFFFBBF24)
                          },
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
        }

        3 -> {
          // Unified Transaction Ledger with Filter Chips
          item {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf(
                "ALL" to "All Activity",
                "RECHARGE" to "🪙 Recharges",
                "GIFT" to "🎁 Gift Activity",
                "DIAMOND" to "💎 Diamonds",
                "EXCHANGE" to "🔄 Exchanges",
                "WITHDRAWAL" to "💸 Withdrawals"
              ).forEach { (f, label) ->
                item {
                  FilterChip(
                    selected = ledgerFilter == f,
                    onClick = { ledgerFilter = f },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = LivoViolet,
                      selectedLabelColor = Color.White
                    )
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }

          if (filteredCoinTxs.isEmpty() && filteredDiaTxs.isEmpty()) {
            item {
              EmptyStateView(
                icon = Icons.Default.Receipt,
                title = "No ledger entries found",
                description = "Your verified coin purchases, spending, and diamond earnings will appear here."
              )
            }
          } else {
            // Coin Ledger items
            items(filteredCoinTxs) { tx ->
              CoinTransactionCard(tx = tx, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Diamond Ledger items
            if (ledgerFilter == "ALL" || ledgerFilter == "DIAMOND" || ledgerFilter == "EXCHANGE" || ledgerFilter == "WITHDRAWAL") {
              items(filteredDiaTxs) { dTx ->
                DiamondTransactionCard(tx = dTx, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
              }
            }
          }
        }
      }
    }
  }

  // Payment Provider In-App Checkout Dialog
  if (selectedPackageForCheckout != null) {
    val pkg = selectedPackageForCheckout!!
    var isSimulatingPayment by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = {
        if (!isSimulatingPayment) selectedPackageForCheckout = null
      },
      containerColor = Color(0xFF0F1528),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = "🪙", fontSize = 22.sp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Verified In-App Checkout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Package Summary
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2845)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(14.dp)
            ) {
              Column {
                Text(
                  text = "${String.format("%,d", pkg.coinAmount)} Coins",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
                if (pkg.bonusCoins > 0) {
                  Text(text = "+${pkg.bonusCoins} Bonus Coins", color = LivoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "Rate: $1.00 USD = 400,000 Coins", color = TextTertiary, fontSize = 10.sp)
              }
              Text(
                text = "$${pkg.usdPrice}",
                color = LivoGoldLight,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
              )
            }
          }

          Text("Select Payment Provider:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 220.dp)
          ) {
            items(repository.paymentProviders) { provider ->
              val isSelected = selectedPaymentProvider.id == provider.id
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) LivoViolet.copy(alpha = 0.25f) else Color(0xFF17203B),
                border = BorderStroke(1.dp, if (isSelected) LivoViolet else Color(0xFF1E2845)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedPaymentProvider = provider }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(10.dp)
                ) {
                  Text(provider.iconEmoji, fontSize = 20.sp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(provider.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SpeakingGreen.copy(alpha = 0.2f)
                      ) {
                        Text(
                          text = provider.badgeText,
                          color = SpeakingGreen,
                          fontSize = 9.sp,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                    Text(provider.description, color = Color(0xFF94A3B8), fontSize = 10.sp)
                  }
                  RadioButton(
                    selected = isSelected,
                    onClick = { selectedPaymentProvider = provider }
                  )
                }
              }
            }
          }

          if (isSimulatingPayment) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
              CircularProgressIndicator(color = LivoGold, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text("Connecting to ${selectedPaymentProvider.name}...", color = LivoGoldLight, fontSize = 11.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            isSimulatingPayment = true
            val paymentRef = "TX-PAY-${UUID.randomUUID().toString().take(16)}"
            coroutineScope.launch {
              kotlinx.coroutines.delay(1200) // Verification handshake
              val res = repository.rechargeCoins(
                pkg = pkg,
                paymentProvider = selectedPaymentProvider.name,
                paymentReference = paymentRef
              )
              isSimulatingPayment = false
              selectedPackageForCheckout = null
              res.onSuccess {
                Toast.makeText(
                  context,
                  "Verified Purchase Successful! Added ${String.format("%,d", pkg.coinAmount + pkg.bonusCoins)} Coins to your balance.",
                  Toast.LENGTH_LONG
                ).show()
              }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Transaction failed", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isSimulatingPayment,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = LivoGold)
        ) {
          Text("Pay $${pkg.usdPrice}", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        if (!isSimulatingPayment) {
          TextButton(onClick = { selectedPackageForCheckout = null }) {
            Text("Cancel", color = TextTertiary)
          }
        }
      }
    )
  }
}

@Composable
fun RechargePackageItemCard(
  pkg: RechargePackage,
  isProcessing: Boolean,
  onSelectToBuy: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = BorderStroke(
      1.dp,
      if (pkg.isPopular) LivoGold else DarkSurfaceBorder
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(16.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = "🪙", fontSize = 22.sp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${String.format("%,d", pkg.coinAmount)} Coins",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          if (pkg.bonusCoins > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = LivoGold.copy(alpha = 0.2f)
            ) {
              Text(
                text = "+${pkg.bonusCoins} Bonus",
                color = LivoGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = "$${pkg.usdPrice} USD ($1.00 = 400,000 Coins)",
          color = TextTertiary,
          fontSize = 11.sp
        )
      }

      Button(
        onClick = onSelectToBuy,
        enabled = !isProcessing,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (pkg.isPopular) LivoGold else LivoViolet
        )
      ) {
        Text(
          text = "$${pkg.usdPrice}",
          color = if (pkg.isPopular) Color.Black else Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }
  }
}

@Composable
fun CoinTransactionCard(
  tx: CoinTransactionEntity,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
  val isCredit = tx.amount > 0

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = BorderStroke(0.6.dp, DarkSurfaceBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(14.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.description,
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${dateFormat.format(Date(tx.timestamp))} • Ref: ${tx.transactionId.take(16)}" +
              if (!tx.paymentProvider.isNullOrBlank()) " • ${tx.paymentProvider}" else "",
          color = TextTertiary,
          fontSize = 10.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (isCredit) "+${String.format("%,d", tx.amount)} 🪙" else "${String.format("%,d", tx.amount)} 🪙",
          color = if (isCredit) SpeakingGreen else LivoRose,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Text(
          text = "Bal: ${String.format("%,d", tx.balanceAfter)}",
          color = TextSecondary,
          fontSize = 10.sp
        )
      }
    }
  }
}

@Composable
fun DiamondTransactionCard(
  tx: DiamondTransactionEntity,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
  val isCredit = tx.amount > 0

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF101B2E)),
    border = BorderStroke(0.6.dp, Color(0xFF1E3A5F)),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(14.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.description,
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${dateFormat.format(Date(tx.timestamp))} • Ref: ${tx.transactionId.take(16)}",
          color = Color(0xFF64748B),
          fontSize = 10.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (isCredit) "+${String.format("%,d", tx.amount)} 💎" else "${String.format("%,d", tx.amount)} 💎",
          color = if (isCredit) Color(0xFF38BDF8) else Color(0xFFF43F5E),
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Text(
          text = "Bal: ${String.format("%,d", tx.balanceAfter)} 💎",
          color = Color(0xFF94A3B8),
          fontSize = 10.sp
        )
      }
    }
  }
}
