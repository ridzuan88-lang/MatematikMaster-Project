package com.chewanninja.matematikmaster

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MmTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(vm.message) {
        val m = vm.message
        if (m != null) {
            Toast.makeText(ctx, m, Toast.LENGTH_LONG).show()
            vm.message = null
        }
    }
    val s = vm.screen
    // Quiz mengendalikan butang Kembali sendiri (dialog keluar).
    if (s != Screen.Quiz) {
        BackHandler(enabled = vm.canGoBack()) { vm.back() }
    }
    when (s) {
        Screen.Loading -> Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) { T("Memuatkan…", 20, true) }
        Screen.Setup -> SetupScreen(vm)
        Screen.Login -> LoginScreen(vm)
        Screen.Home -> HomeScreen(vm)
        is Screen.QuizSetup -> QuizSetupScreen(vm, s.mode)
        Screen.Quiz -> QuizScreen(vm)
        Screen.Result -> ResultScreen(vm)
        is Screen.Stats -> StatsScreen(vm, s.profileId)
        Screen.Leaderboard -> LeaderboardScreen(vm)
        Screen.Badges -> BadgesScreen(vm)
        Screen.Parent -> ParentScreen(vm)
    }
}
