package com.example.ui.screens.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OfficialPanelDistributionView(
  repository: LivoRepository,
  allUsers: List<UserEntity>,
  allAssignments: List<UserPanelAssignmentEntity>
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var searchQuery by remember { mutableStateOf("") }
  var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
  var showAssignDialog by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var historyTargetAssignment by remember { mutableStateOf<UserPanelAssignmentEntity?>(null) }

  // Filtered users
  val filteredUsers = remember(searchQuery, allUsers) {
    if (searchQuery.isBlank()) allUsers.take(15)
    else allUsers.filter {
      it.livoId.toString().contains(searchQuery.trim()) ||
          it.nickname.contains(searchQuery.trim(), ignoreCase = true) ||
          it.email.contains(searchQuery.trim(), ignoreCase = true)
    }
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Header Banner
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFFDE047))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Official Panel Distribution System", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Assign Official authority, configure access duration, set granular feature permissions, and control active credentials with full audit log tracking.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
        }
      }
    }

    // 2. Search & Select User
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("1. Search & Select Existing User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by LIVO ID, Nickname, or Email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8))
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(12.dp))

          // User Selection Chips / Cards
          if (filteredUsers.isEmpty()) {
            Text("No users found matching query.", color = Color(0xFF94A3B8), fontSize = 12.sp)
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(filteredUsers) { u ->
                val isSelected = selectedUser?.id == u.id
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFF1E293B),
                  border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)),
                  modifier = Modifier.clickable {
                    selectedUser = u
                    showAssignDialog = true
                  }
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                  ) {
                    AsyncImage(
                      model = u.avatarUrl,
                      contentDescription = null,
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(u.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      Text("ID: ${u.livoId} • ${u.role.displayName}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Active Official Panel Assignments Table
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Active Official Assignments (${allAssignments.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (selectedUser != null) {
          Button(
            onClick = { showAssignDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Assign to ${selectedUser?.nickname?.take(8)}", fontSize = 12.sp)
          }
        }
      }
    }

    if (allAssignments.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No active official panel assignments yet. Search a user above to distribute a panel.", color = Color(0xFF94A3B8), fontSize = 12.sp)
          }
        }
      }
    } else {
      items(allAssignments) { assignment ->
        val isExpired = assignment.expiresAt in 1..System.currentTimeMillis()
        val expiryText = if (assignment.expiresAt <= 0) "Permanent"
        else if (isExpired) "Expired"
        else SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(assignment.expiresAt))

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, if (assignment.status == "ACTIVE") Color(0xFF334155) else Color(0xFFEF4444).copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                  model = assignment.userAvatar,
                  contentDescription = null,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF818CF8), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(assignment.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text("LIVO ID: ${assignment.userLivoId} • ${assignment.roleName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (assignment.status) {
                  "ACTIVE" -> Color(0xFF10B981).copy(alpha = 0.2f)
                  "SUSPENDED" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                  else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                }
              ) {
                Text(
                  text = assignment.status,
                  color = when (assignment.status) {
                    "ACTIVE" -> Color(0xFF34D399)
                    "SUSPENDED" -> Color(0xFFFBBF24)
                    else -> Color(0xFFF87171)
                  },
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Duration: ${if (assignment.durationDays > 0) "${assignment.durationDays} Days" else "Permanent"}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
              Text("Expires: $expiryText", color = if (isExpired) Color(0xFFF87171) else Color(0xFF94A3B8), fontSize = 11.sp)
              Text("By: ${assignment.assignedBy}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }

            if (assignment.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text("Note: ${assignment.notes}", color = Color(0xFF64748B), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Management Actions row
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              // Extend Duration
              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    val res = repository.extendAssignmentDuration(assignment.id, 30)
                    Toast.makeText(context, res.getOrDefault("Extended 30 days"), Toast.LENGTH_SHORT).show()
                  }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("+30d", fontSize = 10.sp)
              }

              // Suspend / Restore
              if (assignment.status == "ACTIVE") {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      val res = repository.updateAssignmentStatus(assignment.id, "SUSPENDED")
                      Toast.makeText(context, res.getOrDefault("Suspended"), Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Suspend", fontSize = 10.sp)
                }
              } else {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      val res = repository.updateAssignmentStatus(assignment.id, "ACTIVE")
                      Toast.makeText(context, res.getOrDefault("Restored"), Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Restore", fontSize = 10.sp)
                }
              }

              // Remove Panel
              Button(
                onClick = {
                  coroutineScope.launch {
                    val res = repository.removeAssignment(assignment.id)
                    Toast.makeText(context, res.getOrDefault("Removed"), Toast.LENGTH_SHORT).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Remove", fontSize = 10.sp)
              }

              // View History
              OutlinedButton(
                onClick = {
                  historyTargetAssignment = assignment
                  showHistoryDialog = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("History", fontSize = 10.sp)
              }
            }
          }
        }
      }
    }
  }

  // Assign Official Panel Dialog
  if (showAssignDialog && selectedUser != null) {
    val user = selectedUser!!
    var selectedRole by remember { mutableStateOf(UserRole.OFFICIAL) }
    var selectedDurationDays by remember { mutableStateOf(30) }
    var coinLimitText by remember { mutableStateOf("0") }
    var internalNote by remember { mutableStateOf("") }
    var permittedFeatures by remember {
      mutableStateOf(setOf("Dashboard", "User Management", "Room Management", "Reports", "Audit Logs"))
    }

    val availableFeaturesList = listOf(
      "Dashboard", "User Management", "Room Management", "Agency Management",
      "Hosting Management", "Gift Management", "Reports", "Audit Logs", "Security Controls"
    )

    AlertDialog(
      onDismissRequest = { showAssignDialog = false },
      containerColor = Color(0xFF0B1021),
      title = {
        Text("Assign Official Panel: ${user.nickname}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Text("User ID: ${user.livoId} • Current Role: ${user.role.displayName}", color = Color(0xFF94A3B8), fontSize = 11.sp)
          }

          // Duration selection
          item {
            Text("Access Duration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1 to "1d", 7 to "7d", 15 to "15d", 30 to "30d", 90 to "90d", -1 to "Perm").forEach { (days, label) ->
                FilterChip(
                  selected = selectedDurationDays == days,
                  onClick = { selectedDurationDays = days },
                  label = { Text(label, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6366F1),
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }

          // Role selection
          item {
            Text("Official Role Assignment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(UserRole.OFFICIAL, UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.MANAGER).forEach { r ->
                FilterChip(
                  selected = selectedRole == r,
                  onClick = { selectedRole = r },
                  label = { Text(r.displayName, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF8B5CF6),
                    selectedLabelColor = Color.White
                  )
                )
              }
            }
          }

          // Permitted Features
          item {
            Text("Permitted Features", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              availableFeaturesList.forEach { feat ->
                val isChecked = permittedFeatures.contains(feat)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      permittedFeatures = if (isChecked) permittedFeatures - feat else permittedFeatures + feat
                    }
                ) {
                  Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                      permittedFeatures = if (checked) permittedFeatures + feat else permittedFeatures - feat
                    }
                  )
                  Text(feat, color = Color.White, fontSize = 12.sp)
                }
              }
            }
          }

          // Internal Note
          item {
            OutlinedTextField(
              value = internalNote,
              onValueChange = { internalNote = it },
              label = { Text("Internal Note / Reason") },
              placeholder = { Text("e.g. Official Operations Team onboarding") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.assignOfficialPanel(
                targetUserId = user.id,
                panelType = PanelType.OFFICIAL.name,
                roleToAssign = selectedRole,
                durationDays = selectedDurationDays,
                permittedFeaturesJson = permittedFeatures.joinToString(","),
                coinLimit = coinLimitText.toLongOrNull() ?: 0L,
                internalNotes = internalNote
              )
              showAssignDialog = false
              res.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
              }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
          Text("Confirm & Send Panel", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAssignDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  // View Assignment History Dialog
  if (showHistoryDialog && historyTargetAssignment != null) {
    val a = historyTargetAssignment!!
    AlertDialog(
      onDismissRequest = { showHistoryDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("Assignment Record: ${a.userName}", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Panel: ${a.panelType}", color = Color(0xFFFDE047), fontWeight = FontWeight.Bold)
          Text("Role: ${a.roleName}", color = Color.White)
          Text("Assigned By: ${a.assignedBy}", color = Color(0xFFCBD5E1))
          Text("Assigned Date: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(a.assignedAt))}", color = Color(0xFFCBD5E1))
          Text("Duration: ${if (a.durationDays > 0) "${a.durationDays} Days" else "Permanent"}", color = Color(0xFFCBD5E1))
          Text("Features: ${a.customPermissionsJson.ifBlank { "Standard Suite" }}", color = Color(0xFF94A3B8), fontSize = 12.sp)
          if (a.notes.isNotBlank()) {
            Text("Notes: ${a.notes}", color = Color(0xFF94A3B8), fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showHistoryDialog = false }) {
          Text("Done", color = Color.White)
        }
      }
    )
  }
}
