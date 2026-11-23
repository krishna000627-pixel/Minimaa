package com.minimaa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint

data class AppInfo(val label: String, val pkg: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color.Black)) {
                Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) { Root() }
            }
        }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var screen by remember { mutableStateOf("home") }
    var pins by remember { mutableStateOf(prefs.getStringSet("pins", emptySet())!!.toSet()) }
    val apps = remember {
        val pm = ctx.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
    val vm: LoopVM = viewModel()
    BackHandler(screen != "home") { screen = "home" }
    fun launch(pkg: String) = ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
    fun togglePin(pkg: String) {
        pins = if (pkg in pins) pins - pkg else pins + pkg
        prefs.edit().putStringSet("pins", pins).apply()
    }
    when (screen) {
        "home" -> HomeScreen(apps.filter { it.pkg in pins }, { screen = it }, ::launch)
        "drawer" -> DrawerScreen(apps, pins, ::launch, ::togglePin) { screen = "home" }
        "loop" -> LoopScreen(vm) { screen = "home" }
        "calc" -> CalcScreen { screen = "home" }
        "pomo" -> PomoScreen { screen = "home" }
    }
}
