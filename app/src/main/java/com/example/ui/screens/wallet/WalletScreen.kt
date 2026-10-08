package com.example.ui.screens.wallet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.COINS_PER_USD
import com.example.data.model.CoinTransactionEntity
import com.example.data.model.RechargePackage
import com.example.data.repository.LivoRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val wallet by repository.observeWallet(currentUser?.id ?: "").collectAsState(initial = null)
  val transactions by repository.observeTransactions(currentUser?.id ?: "").collectAsState(initial = emptyList())

  var selectedTab by remember { mutableStateOf(0) } // 0: Recharge Packages, 1: Transaction Ledger
  var isProcessingRecharge by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Livo Wallet", onBackClick = onBackClick)
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(bottom = 32.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. Wallet Balance Hero Card
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, LivoGold.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    Color(0xFF282512),
                    Color(0xFF1E1C2E)
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
                Text(
                  text = "Official Rate: $1.00 = 28,000 Coins",
                  color = LivoGoldLight,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = LivoGold.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "VERIFIED LEDGER",
                    color = LivoGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Available Coins",
                color = TextSecondary,
                fontSize = 13.sp
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🪙", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = String.format("%,d", wallet?.balance ?: 0),
                  color = Color.White,
                  fontSize = 32.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }

              Spacer(modifier = Modifier.height(12.dp))
              Divider(color = DarkSurfaceBorder)
              Spacer(modifier = Modifier.height(12.dp))

              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column {
                  Text(text = "Diamonds (Withdrawable)", color = TextTertiary, fontSize = 11.sp)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💎", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = String.format("%,d", wallet?.diamondBalance ?: 0),
                      color = LivoCyanLight,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp
                    )
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(text = "Total Spent", color = TextTertiary, fontSize = 11.sp)
                  Text(
                    text = String.format("%,d 🪙", wallet?.totalSpent ?: 0),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }
            }
          }
        }
      }

      // 2. Tab Navigation
      item {
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkBackground,
          contentColor = TextPrimary,
          modifier = Modifier.padding(horizontal = 16.dp)
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Recharge Coins", fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Ledger History (${transactions.size})", fontWeight = FontWeight.Bold) }
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // 3. Recharge Packages vs Transaction History
      if (selectedTab == 0) {
        items(repository.rechargePackages) { pkg ->
          RechargePackageRow(
            pkg = pkg,
            isProcessing = isProcessingRecharge,
            onBuyClick = {
              isProcessingRecharge = true
              coroutineScope.launch {
                val res = repository.rechargeCoins(pkg)
                isProcessingRecharge = false
                res.onSuccess {
                  Toast.makeText(context, "Recharged successfully! Added ${pkg.coinAmount + pkg.bonusCoins} coins.", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                  Toast.makeText(context, err.message ?: "Transaction failed", Toast.LENGTH_SHORT).show()
                }
              }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
        }
      } else {
        if (transactions.isEmpty()) {
          item {
            EmptyStateView(
              icon = Icons.Default.Receipt,
              title = "No transactions yet",
              description = "Your verified coin purchases, spending and gifts will appear here."
            )
          }
        } else {
          items(transactions) { tx ->
            TransactionRow(tx = tx, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
          }
        }
      }
    }
  }
}

@Composable
fun RechargePackageRow(
  pkg: RechargePackage,
  isProcessing: Boolean,
  onBuyClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(
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
          Text(text = "🪙", fontSize = 20.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = String.format("%,d Coins", pkg.coinAmount),
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
          text = "Standard $1 = 28,000 Coin rate applied",
          color = TextTertiary,
          fontSize = 11.sp
        )
      }

      Button(
        onClick = onBuyClick,
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
fun TransactionRow(
  tx: CoinTransactionEntity,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
  val isCredit = tx.amount > 0

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
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
          text = "${dateFormat.format(Date(tx.timestamp))} • ${tx.transactionId}",
          color = TextTertiary,
          fontSize = 10.sp
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (isCredit) "+${String.format("%,d", tx.amount)}" else String.format("%,d", tx.amount),
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
