package com.example.ui.screens.agency

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgencyEntity
import com.example.data.repository.LivoRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LivoButton
import com.example.ui.components.LivoTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AgencyScreen(
  repository: LivoRepository,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val agencies by repository.observeAgencies().collectAsState(initial = emptyList())
  var showJoinAgencyDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        LivoTopBar(
          title = "Host Agency Center",
          onBackClick = onBackClick,
          actions = {
            IconButton(onClick = { showJoinAgencyDialog = true }) {
              Icon(Icons.Default.Add, contentDescription = "Join Agency", tint = LivoCyan)
            }
          }
        )
      }
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showJoinAgencyDialog = true },
        containerColor = LivoCyan,
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Default.Group, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Join Agency by Code", fontWeight = FontWeight.Bold)
      }
    },
    containerColor = DarkBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (agencies.isEmpty()) {
        EmptyStateView(
          icon = Icons.Default.Groups,
          title = "No registered agencies",
          description = "Agencies provide host management, revenue share, and live voice training."
        )
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Livo Agency Ecosystem",
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Verified agency talent receive customized room badges, higher revenue share rates, and priority room placement.",
                  color = TextSecondary,
                  fontSize = 12.sp,
                  lineHeight = 16.sp
                )
              }
            }
          }

          items(agencies) { agency ->
            AgencyCard(agency = agency)
          }
        }
      }
    }
  }

  // Join Agency Dialog
  if (showJoinAgencyDialog) {
    var agencyCodeInput by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showJoinAgencyDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Join Host Agency", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter the 6-character Agency Code provided by your agency manager (e.g. LIVO88).",
            color = TextSecondary,
            fontSize = 13.sp
          )
          OutlinedTextField(
            value = agencyCodeInput,
            onValueChange = { agencyCodeInput = it.uppercase() },
            label = { Text("Agency Code") },
            placeholder = { Text("e.g. LIVO88") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (agencyCodeInput.isNotBlank()) {
              isJoining = true
              coroutineScope.launch {
                val res = repository.joinAgency(agencyCodeInput.trim())
                isJoining = false
                res.onSuccess { msg ->
                  Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                  showJoinAgencyDialog = false
                }.onFailure { err ->
                  Toast.makeText(context, err.message ?: "Failed to join", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          enabled = !isJoining,
          colors = ButtonDefaults.buttonColors(containerColor = LivoCyan)
        ) {
          Text("Join Agency", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showJoinAgencyDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
fun AgencyCard(agency: AgencyEntity) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = agency.name,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          Text(
            text = "Code: ${agency.agencyCode} • Owner: ${agency.ownerName}",
            color = LivoCyanLight,
            fontSize = 12.sp
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = LivoCyan.copy(alpha = 0.2f)
        ) {
          Text(
            text = "${agency.commissionRate}% Share",
            color = LivoCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = agency.description,
        color = TextSecondary,
        fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(10.dp))
      Divider(color = DarkSurfaceBorder)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Hosts: ${agency.totalMembers}",
          color = TextTertiary,
          fontSize = 11.sp
        )
        Text(
          text = "Monthly: 💎 ${String.format("%,d", agency.totalMonthlyDiamonds)}",
          color = LivoCyanLight,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        )
      }
    }
  }
}
