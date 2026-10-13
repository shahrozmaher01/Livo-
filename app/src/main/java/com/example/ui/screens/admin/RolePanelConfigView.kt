package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RolePanelConfigView(
  panelType: PanelType,
  panelConfig: PanelConfigEntity?,
  assignments: List<UserPanelAssignmentEntity>,
  repository: LivoRepository,
  isSuperAdminView: Boolean = false
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by repository.currentUser.collectAsState()

  // Parse modules from config
  val activeModules = remember(panelConfig) {
    if (panelConfig != null && panelConfig.modulesJson.isNotBlank()) {
      runCatching {
        val arr = JSONArray(panelConfig.modulesJson)
        (0 until arr.length()).map { idx ->
          val obj = arr.getJSONObject(idx)
          val actsArr = obj.optJSONArray("allowedActions") ?: JSONArray()
          val acts = (0 until actsArr.length()).map { actsArr.getString(it) }
          PanelModuleConfig(
            moduleId = obj.optString("moduleId"),
            moduleName = obj.optString("moduleName"),
            isEnabled = obj.optBoolean("isEnabled", true),
            allowedActions = acts,
            accessScope = obj.optString("accessScope", "GLOBAL"),
            maxLimit = obj.optLong("maxLimit", 0L)
          )
        }
      }.getOrDefault(emptyList())
    } else emptyList()
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Role Hero Overview
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column {
              Text(panelType.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Text("Configured & Controlled by Owner", color = Color(0xFFCBD5E1), fontSize = 11.sp)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF10B981).copy(alpha = 0.2f)
            ) {
              Text(
                text = "${assignments.size} Active Seats",
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          if (isSuperAdminView) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFDC2626).copy(alpha = 0.2f),
              border = BorderStroke(0.8.dp, Color(0xFFDC2626).copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
              ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Protected Owner Controls: Super Admins cannot override Owner credentials, delete audit logs, or alter core financial balances.",
                  color = Color(0xFFFECACA),
                  fontSize = 10.5.sp
                )
              }
            }
          }
        }
      }
    }

    // 2. Active Authorized Modules for this role
    item {
      Text("Permitted Modules (${activeModules.count { it.isEnabled }})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }

    items(activeModules) { mod ->
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (mod.isEnabled) Color(0xFF0F172A) else Color(0xFF090D1A)
        ),
        border = BorderStroke(0.8.dp, if (mod.isEnabled) Color(0xFF334155) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(12.dp).fillMaxWidth()
        ) {
          Column {
            Text(mod.moduleName, color = if (mod.isEnabled) Color.White else Color(0xFF64748B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
              text = "Scope: ${mod.accessScope} • Actions: ${mod.allowedActions.joinToString(", ")}",
              color = Color(0xFF94A3B8),
              fontSize = 10.sp
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (mod.isEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
          ) {
            Text(
              text = if (mod.isEnabled) "ACTIVE" else "LOCKED",
              color = if (mod.isEnabled) Color(0xFF34D399) else Color(0xFFF87171),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }

    // 3. Assigned Users List
    item {
      Spacer(modifier = Modifier.height(4.dp))
      Text("Assigned Users (${assignments.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }

    if (assignments.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No users currently assigned to this panel. Go to Official Panel to distribute access.", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
          }
        }
      }
    } else {
      items(assignments) { a ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(0.8.dp, Color(0xFF334155)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              AsyncImage(
                model = a.userAvatar,
                contentDescription = null,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(a.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("LIVO ID: ${a.userLivoId} • Status: ${a.status}", color = Color(0xFF94A3B8), fontSize = 10.5.sp)
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (a.status == "ACTIVE") {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      repository.updateAssignmentStatus(a.id, "SUSPENDED")
                      Toast.makeText(context, "Suspended access", Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Suspend", fontSize = 10.sp)
                }
              } else {
                Button(
                  onClick = {
                    coroutineScope.launch {
                      repository.updateAssignmentStatus(a.id, "ACTIVE")
                      Toast.makeText(context, "Restored access", Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Restore", fontSize = 10.sp)
                }
              }

              Button(
                onClick = {
                  coroutineScope.launch {
                    repository.removeAssignment(a.id)
                    Toast.makeText(context, "Revoked panel", Toast.LENGTH_SHORT).show()
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text("Revoke", fontSize = 10.sp)
              }
            }
          }
        }
      }
    }
  }
}
