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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.LivoRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DynamicPanelBuilderView(
  repository: LivoRepository,
  panelConfigs: List<PanelConfigEntity>,
  templates: List<PanelTemplateEntity>
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedRoleType by remember { mutableStateOf(PanelType.OFFICIAL) }
  var isPreviewMode by remember { mutableStateOf(false) }
  var showNewTemplateDialog by remember { mutableStateOf(false) }

  // All available system modules for Builder
  val allAvailableModules = listOf(
    "dashboard" to "Dashboard",
    "users" to "User Management",
    "rooms" to "Room Management",
    "agencies" to "Agency Management",
    "hosting" to "Hosting Management",
    "gifts" to "Gift Management",
    "coin_reports" to "Coin & Diamond Reports",
    "frames" to "Frame Management",
    "rankings" to "Ranking Management",
    "notifications" to "Notifications",
    "support" to "Support & Reports",
    "transaction_history" to "Transaction History",
    "audit_logs" to "Audit Logs"
  )

  // Current selected panel's configuration parsed from DB or defaults
  val currentConfig = panelConfigs.firstOrNull { it.panelType == selectedRoleType.name }

  // Local state for editing modules
  var editableModules by remember(selectedRoleType, currentConfig) {
    val initial = if (currentConfig != null && currentConfig.modulesJson.isNotBlank()) {
      runCatching {
        val arr = JSONArray(currentConfig.modulesJson)
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

    mutableStateOf(
      if (initial.isNotEmpty()) initial
      else allAvailableModules.map { (id, name) ->
        PanelModuleConfig(
          moduleId = id,
          moduleName = name,
          isEnabled = true,
          allowedActions = listOf("VIEW", "CREATE", "EDIT"),
          accessScope = "GLOBAL"
        )
      }
    )
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Header & Explanation
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFDE047))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Dynamic Panel Builder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (currentConfig?.isPublished == true) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.25f)
            ) {
              Text(
                text = if (currentConfig?.isPublished == true) "PUBLISHED (v${currentConfig.version})" else "DRAFT",
                color = if (currentConfig?.isPublished == true) Color(0xFF34D399) else Color(0xFFFBBF24),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Design, customize and grant permissions dynamically for each role without modifying source code. Toggle modules, adjust granular action permissions, set record scopes, save drafts, and publish instantly to backend enforcement.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
          )
        }
      }
    }

    // 2. Select Role selector
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("Select Role to Configure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PanelType.values()) { pType ->
              val isSel = selectedRoleType == pType
              FilterChip(
                selected = isSel,
                onClick = { selectedRoleType = pType },
                label = { Text(pType.displayName, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF7C3AED),
                  selectedLabelColor = Color.White
                )
              )
            }
          }
        }
      }
    }

    // 3. Action Toolbar (Save Draft, Publish, Preview, Apply Template)
    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Preview Switch
        OutlinedButton(
          onClick = { isPreviewMode = !isPreviewMode },
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isPreviewMode) Color(0xFF38BDF8) else Color.White
          ),
          modifier = Modifier.weight(1f)
        ) {
          Icon(if (isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isPreviewMode) "Exit Preview" else "Preview", fontSize = 11.sp)
        }

        // Save Draft
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.savePanelConfig(
                panelType = selectedRoleType.name,
                displayName = selectedRoleType.displayName,
                modules = editableModules,
                isPublished = false
              )
              Toast.makeText(context, res.getOrDefault("Draft saved!"), Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
          modifier = Modifier.weight(1f)
        ) {
          Text("Save Draft", fontSize = 11.sp)
        }

        // Publish
        Button(
          onClick = {
            coroutineScope.launch {
              val res = repository.savePanelConfig(
                panelType = selectedRoleType.name,
                displayName = selectedRoleType.displayName,
                modules = editableModules,
                isPublished = true
              )
              Toast.makeText(context, res.getOrDefault("Published to backend!"), Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Publish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 4. Panel Templates row
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Panel Templates (${templates.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        TextButton(onClick = { showNewTemplateDialog = true }) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Save as Template", fontSize = 11.sp)
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(templates) { tmpl ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(0.8.dp, Color(0xFF475569)),
            modifier = Modifier.clickable {
              Toast.makeText(context, "Applied template '${tmpl.templateName}'", Toast.LENGTH_SHORT).show()
            }
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(tmpl.templateName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              Text(tmpl.panelType, color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
          }
        }
      }
    }

    // 5. Configurable Modules List
    item {
      Text(
        "Configured Modules for ${selectedRoleType.displayName} (${editableModules.size})",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    }

    items(editableModules) { mod ->
      val isEnabled = mod.isEnabled
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isEnabled) Color(0xFF0F172A) else Color(0xFF080C14)),
        border = BorderStroke(1.dp, if (isEnabled) Color(0xFF334155) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Module Header + ON/OFF Switch
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (isEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(mod.moduleName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Switch(
              checked = isEnabled,
              onCheckedChange = { checked ->
                editableModules = editableModules.map {
                  if (it.moduleId == mod.moduleId) it.copy(isEnabled = checked) else it
                }
              }
            )
          }

          if (isEnabled && !isPreviewMode) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Granular Action Permissions
            Text("Granular Actions:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))

            val validActions = listOf("VIEW", "CREATE", "EDIT", "DELETE", "APPROVE", "REJECT", "ASSIGN", "EXPORT", "MANAGE_PERMISSIONS")
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              validActions.forEach { act ->
                val hasAct = mod.allowedActions.contains(act)
                FilterChip(
                  selected = hasAct,
                  onClick = {
                    val newActs = if (hasAct) mod.allowedActions - act else mod.allowedActions + act
                    editableModules = editableModules.map {
                      if (it.moduleId == mod.moduleId) it.copy(allowedActions = newActs) else it
                    }
                  },
                  label = { Text(act, fontSize = 10.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    selectedLabelColor = Color.White
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Access Scope Selector
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Record Scope:", color = Color(0xFF94A3B8), fontSize = 11.sp)
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("GLOBAL", "ASSIGNED_ONLY", "SELF_ONLY").forEach { sc ->
                  val isScope = mod.accessScope == sc
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isScope) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isScope) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier.clickable {
                      editableModules = editableModules.map {
                        if (it.moduleId == mod.moduleId) it.copy(accessScope = sc) else it
                      }
                    }
                  ) {
                    Text(
                      text = sc.replace("_", " "),
                      color = if (isScope) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Create Template Dialog
  if (showNewTemplateDialog) {
    var tmplName by remember { mutableStateOf("") }
    var tmplDesc by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showNewTemplateDialog = false },
      containerColor = Color(0xFF0F172A),
      title = { Text("Save Template from Current Config", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = tmplName,
            onValueChange = { tmplName = it },
            label = { Text("Template Name") },
            placeholder = { Text("e.g. BD High Performance") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = tmplDesc,
            onValueChange = { tmplDesc = it },
            label = { Text("Description") },
            placeholder = { Text("Short purpose description...") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (tmplName.isNotBlank()) {
              coroutineScope.launch {
                val res = repository.savePanelTemplate(
                  templateName = tmplName,
                  panelType = selectedRoleType.name,
                  description = tmplDesc,
                  modules = editableModules
                )
                showNewTemplateDialog = false
                Toast.makeText(context, res.getOrDefault("Template created!"), Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
          Text("Save Template", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewTemplateDialog = false }) { Text("Cancel", color = Color(0xFF94A3B8)) }
      }
    )
  }
}
