package com.minimaa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
    var loopInitialTab by remember { mutableIntStateOf(0) }
    var loopOpenNew by remember { mutableStateOf(false) }
    var pins by remember { mutableStateOf(prefs.getStringSet("pins", emptySet()).orEmpty().toSet()) }
    var sidebarPkgs by remember { mutableStateOf(prefs.getStringSet("sidebar_pkgs", emptySet()).orEmpty().toSet()) }
    var pausePkg by remember { mutableStateOf<String?>(null) }

    val apps = remember {
        val pm = ctx.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
    val vm: LoopVM = viewModel()
    val journals by vm.journals.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()

    BackHandler(screen != "home") {
        screen = when (screen) {
            "detox", "wallpaper", "transparency" -> "settings"
            else -> "home"
        }
    }

    fun launchNow(pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
    }

    fun launch(pkg: String) {
        if (prefs.getBoolean("detox_pause", false)) {
            pausePkg = pkg
        } else {
            launchNow(pkg)
        }
    }

    fun togglePin(pkg: String) {
        pins = if (pkg in pins) pins - pkg else pins + pkg
        prefs.edit().putStringSet("pins", pins).apply()
    }
    fun saveSidebarPkgs(newPkgs: Set<String>) {
        sidebarPkgs = newPkgs
        prefs.edit().putStringSet("sidebar_pkgs", newPkgs).apply()
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            "home" -> HomePagerScreen(
                apps = apps,
                pinned = apps.filter { it.pkg in pins },
                sidebarPkgs = sidebarPkgs,
                journals = journals,
                tasks = tasks,
                go = { screen = it },
                goLoop = { tab, openNew ->
                    loopInitialTab = tab
                    loopOpenNew = openNew
                    screen = "loop"
                },
                launch = ::launch,
                onSaveSidebarPkgs = ::saveSidebarPkgs
            )
            "drawer" -> DrawerScreen(apps, pins, { screen = it }, ::launch, ::togglePin) { screen = "home" }
            "loop" -> LoopScreen(vm, initialTab = loopInitialTab, initialOpenNew = loopOpenNew) {
                loopOpenNew = false
                screen = "home"
            }
            "calc" -> CalcScreen { screen = "home" }
            "pomo" -> PomoScreen { screen = "home" }
            "calendar" -> CalendarScreen(
                tasks = tasks,
                onSaveTask = { existing, title, type, cat, prio, sched -> vm.saveTask(existing, title, type, cat, prio, sched) },
                onToggleTask = { vm.toggle(it) },
                onOpenSettings = { screen = "settings" },
                onBack = { screen = "home" }
            )
            "body" -> BodyScreen { screen = "home" }
            "settings" -> SettingsScreen(onNavigate = { screen = it }) { screen = "home" }
            "detox" -> DigitalDetoxScreen { screen = "settings" }
            "wallpaper" -> WallpaperSettingsScreen { screen = "settings" }
            "transparency" -> TransparencyScreen { screen = "settings" }
        }

        // 3-second Mindful Pause overlay when enabled in Digital Detox
        pausePkg?.let { pkg ->
            var sec by remember(pkg) { mutableIntStateOf(3) }
            LaunchedEffect(pkg) {
                while (sec > 0) {
                    delay(1000)
                    sec--
                }
                pausePkg = null
                launchNow(pkg)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MINDFUL PAUSE", color = Dim, fontSize = 12.sp, letterSpacing = 4.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("$sec", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Take a breath before opening.", color = Dim, fontSize = 13.sp)
                    Spacer(Modifier.height(28.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Line, RoundedCornerShape(50))
                            .clickable { pausePkg = null }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}
