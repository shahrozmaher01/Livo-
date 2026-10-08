package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPanelScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()
  val auditLogs by repository.observeAuditLogs().collectAsState(initial = emptyList())
  val reports by repository.observeReports().collectAsState(initial = emptyList())

  // Section 21 navigation tabs: Home, Users, Send Manager, Admin, Agency, Coin, Settings
  var selectedTab by remember { mutableStateOf(0) }
  val tabs = listOf("Users", "Coin Grant", "Reports", "Audit Logs")

  // User search & selection
  var searchUserQuery by remember { mutableStateOf("") }
  var userSearchResults by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
  var targetUserForAction by remember { mutableStateOf<UserEntity?>(null) }
  var showRoleDialog by remember { mutableStateOf(false) }
  var showGrantCoinsDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(title = "Admin & Operations Control", onBackClick = onBackClick)
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkBackground,
          contentColor = TextPrimary
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
          }
        }
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        // Tab 0: User Management & Role Assignment
        0 -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
          ) {
            OutlinedTextField(
              value = searchUserQuery,
              onValueChange = {
                searchUserQuery = it
                coroutineScope.launch {
                  userSearchResults = repository.searchUsers(it)
                }
              },
              placeholder = { Text("Search User by Livo ID or Nickname...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (userSearchResults.isEmpty()) {
              EmptyStateView(
                icon = Icons.Default.ManageAccounts,
                title = "Search a User to Manage",
                description = "Assign roles (Super Admin, Manager, BD, Agency, Host) or inspect account status."
              )
            } else {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
              ) {
                items(userSearchResults) { user ->
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(12.dp)
                    ) {
                      AvatarWithFrame(avatarUrl = user.avatarUrl, frameId = user.equippedFrameId, size = 46.dp)
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(text = user.nickname, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                          Spacer(modifier = Modifier.width(6.dp))
                          RoleBadge(role = user.role)
                        }
                        Text(text = "ID: ${user.livoId} • Lv.${user.level}", color = TextTertiary, fontSize = 11.sp)
                      }
                      Button(
                        onClick = {
                          targetUserForAction = user
                          showRoleDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LivoViolet),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                      ) {
                        Text("Role", fontSize = 12.sp)
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Button(
                        onClick = {
                          targetUserForAction = user
                          showGrantCoinsDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LivoGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                      ) {
                        Text("Grant 🪙", color = Color.Black, fontSize = 12.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Tab 1: Coin Management
        1 -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
          ) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Server Coin Grant System", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Coin adjustments are authenticated server-side and permanently recorded into system audit logs with transaction UUIDs.",
                  color = TextSecondary,
                  fontSize = 12.sp
                )
              }
            }
            Spacer(modifier = Modifier.height(16.dp))
            EmptyStateView(
              icon = Icons.Default.Paid,
              title = "Select user from 'Users' tab",
              description = "Find user by Livo ID in the Users tab, then tap 'Grant' to adjust balance."
            )
          }
        }

        // Tab 2: User Reports
        2 -> {
          if (reports.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.CheckCircle,
              title = "No pending reports",
              description = "All community reports and user flags have been reviewed."
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(reports) { r ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text(text = "Target: ${r.targetName} (${r.targetType})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LivoRose.copy(alpha = 0.2f)
                      ) {
                        Text(text = r.reason, color = LivoRose, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                      }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Reported by: ${r.reporterName} • \"${r.details}\"", color = TextSecondary, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }

        // Tab 3: System Audit Logs
        3 -> {
          val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
          if (auditLogs.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.History,
              title = "No audit log entries",
              description = "Administrative role changes and coin adjustments will be permanently logged here."
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(auditLogs) { log ->
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text(text = "${log.action} by ${log.actorName}", color = LivoCyanLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text(text = dateFormat.format(Date(log.timestamp)), color = TextTertiary, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.details, color = TextPrimary, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Role Assignment Dialog
  if (showRoleDialog && targetUserForAction != null) {
    val user = targetUserForAction!!
    var selectedRole by remember { mutableStateOf(user.role) }

    AlertDialog(
      onDismissRequest = { showRoleDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Assign Role: ${user.nickname}", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        LazyColumn(modifier = Modifier.height(280.dp)) {
          items(UserRole.values()) { role ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedRole = role }
                .padding(vertical = 8.dp)
            ) {
              RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = role.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.adminAssignRole(user.id, selectedRole)
              showRoleDialog = false
              res.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                userSearchResults = repository.searchUsers(searchUserQuery)
              }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoViolet)
        ) {
          Text("Apply Role")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRoleDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }

  // Grant Coins Dialog
  if (showGrantCoinsDialog && targetUserForAction != null) {
    val user = targetUserForAction!!
    var amountText by remember { mutableStateOf("100000") }
    var reasonText by remember { mutableStateOf("Event prize grant") }

    AlertDialog(
      onDismissRequest = { showGrantCoinsDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Grant Coins: ${user.nickname}", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Coin Amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = reasonText,
            onValueChange = { reasonText = it },
            label = { Text("Audit Reason") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val amount = amountText.toLongOrNull() ?: 0L
            if (amount > 0 && reasonText.isNotBlank()) {
              coroutineScope.launch {
                val res = repository.adminGrantCoins(user.id, amount, reasonText)
                showGrantCoinsDialog = false
                res.onSuccess { msg ->
                  Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                  Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = LivoGold)
        ) {
          Text("Confirm Grant", color = Color.Black)
        }
      },
      dismissButton = {
        TextButton(onClick = { showGrantCoinsDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }
}
