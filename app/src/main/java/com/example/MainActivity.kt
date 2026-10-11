package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.LivoBrandLogo
import com.example.ui.navigation.LivoNavGraph
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as LivoApplication
    val repository = app.repository

    setContent {
      MyApplicationTheme {
        var isSplashDone by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
          delay(850)
          isSplashDone = true
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DarkBackground
        ) {
          AnimatedContent(
            targetState = isSplashDone,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
            label = "splash_transition"
          ) { showApp ->
            if (!showApp) {
              SplashScreenView()
            } else {
              val navController = rememberNavController()
              LivoNavGraph(
                navController = navController,
                repository = repository
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SplashScreenView() {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      LivoBrandLogo(
        size = 130.dp,
        showGlow = true
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "Livo",
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Live Social Audio & Voice Communities",
        color = TextSecondary,
        fontSize = 12.sp
      )
    }
  }
}
