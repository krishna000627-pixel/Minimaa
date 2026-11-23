package com.minimaa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
            val sysDark = isSystemInDarkTheme()
            val prefs = remember { getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
            LaunchedEffect(Unit) { ThemeState.apply(prefs.getInt("theme_idx", 1), sysDark) }
            val scheme = if (ThemeState.isLight) lightColorScheme(background = BgCol, surface = BgCol) else darkColorScheme(background = BgCol, surface = BgCol)
            MaterialTheme(colorScheme = scheme) {
                Box(Modifier.fillMaxSize().background(BgCol).systemBarsPadding()) { Root() }
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
    var pinsOrder by remember { mutableStateOf(prefs.getString("pins_order", "")?.split(",")?.filter { it.isNotBlank() }.orEmpty()) }
    var sidebarPkgs by remember { mutableStateOf(prefs.getStringSet("sidebar_pkgs", emptySet()).orEmpty().toSet()) }
    var themeIdx by remember { mutableIntStateOf(prefs.getInt("theme_idx", 1)) }
    var pausePkg by remember { mutableStateOf<String?>(null) }
    var limitBlockedPkg by remember { mutableStateOf<String?>(null) }

    var showPermDialog by remember {
        val notifOk = SystemBridge.isNotificationAccessEnabled(ctx)
        val postOk = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val asked = prefs.getBoolean("perms_asked", false)
        mutableStateOf(!asked && (!notifOk || !postOk))
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!SystemBridge.isNotificationAccessEnabled(ctx)) {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
    }

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
            "detox", "wallpaper", "transparency", "home_settings", "drawer_settings", "gesture_settings" -> "settings"
            "motion_gestures" -> "gesture_settings"
            "home_apps", "glance_settings" -> "home_settings"
            "sidebar_apps" -> "glance_settings"
            "drawer_search", "drawer_folders", "drawer_private", "drawer_hidden" -> "drawer_settings"
            else -> "home"
        }
    }

    fun launchNow(pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
    }

    fun launch(pkg: String) {
        val limit = prefs.getInt("limit_$pkg", 0)
        if (limit > 0 && (UsageHelper.getTodayUsageMinutes(ctx)[pkg] ?: 0) >= limit) {
            limitBlockedPkg = pkg
        } else if (prefs.getBoolean("detox_pause", false)) {
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

    val sysDark = isSystemInDarkTheme()
    LaunchedEffect(themeIdx, sysDark) { ThemeState.apply(themeIdx, sysDark) }
    Box(Modifier.fillMaxSize().background(BgCol)) {
        when (screen) {
            "home" -> HomePagerScreen(
                apps = apps,
                pinned = apps.filter { it.pkg in pins }.sortedBy { a ->
                    val i = pinsOrder.indexOf(a.pkg)
                    if (i >= 0) i else Int.MAX_VALUE
                },
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
                togglePin = ::togglePin,
                onPinsReordered = { pinsOrder = prefs.getString("pins_order", "")?.split(",")?.filter { it.isNotBlank() }.orEmpty() },
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
            "settings" -> SettingsScreen(vm, journals, tasks, onThemeChange = { themeIdx = it }, onNavigate = { screen = it }) { screen = "home" }
            "detox" -> DigitalDetoxScreen { screen = "settings" }
            "wallpaper" -> WallpaperSettingsScreen { screen = "settings" }
            "transparency" -> TransparencyScreen { screen = "settings" }
            "home_settings" -> HomeSettingsScreen(apps, pins, ::togglePin, onNavigate = { screen = it }) { screen = "settings" }
            "home_apps" -> HomeAppsScreen(apps, pins, ::togglePin) { screen = "home_settings" }
            "glance_settings" -> GlanceSettingsScreen(onNavigate = { screen = it }) { screen = "home_settings" }
            "sidebar_apps" -> SidebarAppsScreen(apps, sidebarPkgs, ::saveSidebarPkgs) { screen = "glance_settings" }
            "drawer_settings" -> AppDrawerSettingsScreen(onNavigate = { screen = it }) { screen = "settings" }
            "drawer_search" -> DrawerSearchBarsScreen { screen = "drawer_settings" }
            "drawer_folders" -> DrawerFoldersScreen(apps) { screen = "drawer_settings" }
            "drawer_private" -> DrawerPrivateSpaceScreen { screen = "drawer_settings" }
            "drawer_hidden" -> DrawerHiddenAppsScreen(apps) { screen = "drawer_settings" }
            "gesture_settings" -> GesturesSettingsScreen(onNavigate = { screen = it }) { screen = "settings" }
            "motion_gestures" -> MotionGesturesScreen { screen = "gesture_settings" }
        }

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
                Modifier.fillMaxSize().background(InvInk.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MINDFUL PAUSE", color = Dim, fontSize = 12.sp, letterSpacing = 4.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("$sec", color = Ink, fontSize = 64.sp, fontWeight = FontWeight.Bold)
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
                        Text("CANCEL", color = Ink, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }

        limitBlockedPkg?.let { pkg ->
            Box(
                Modifier.fillMaxSize().background(InvInk.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DAILY LIMIT REACHED", color = Dim, fontSize = 12.sp, letterSpacing = 4.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("You reached your daily limit for this app.", color = Ink, fontSize = 14.sp)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).border(1.dp, Line, RoundedCornerShape(50))
                                .clickable { limitBlockedPkg = null }.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) { Text("CLOSE", color = Ink, fontSize = 11.sp, letterSpacing = 2.sp) }
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(Ink)
                                .clickable { val p = pkg; limitBlockedPkg = null; launchNow(p) }.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) { Text("OPEN ANYWAY", color = InvInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                    }
                }
            }
        }

        if (showPermDialog) {
            AlertDialog(
                onDismissRequest = {
                    prefs.edit().putBoolean("perms_asked", true).apply()
                    showPermDialog = false
                },
                containerColor = SheetCol,
                title = { Text("PERMISSIONS", color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
                text = {
                    Text(
                        "Allow Notifications, Step Activity, and Notification Access for live music controls, step counter, and quick-view sidebar.",
                        color = Dim,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("perms_asked", true).apply()
                        showPermDialog = false
                        val req = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= 33) req.add(Manifest.permission.POST_NOTIFICATIONS)
                        if (Build.VERSION.SDK_INT >= 29) req.add(Manifest.permission.ACTIVITY_RECOGNITION)
                        if (req.isNotEmpty()) permLauncher.launch(req.toTypedArray())
                        else if (!SystemBridge.isNotificationAccessEnabled(ctx)) {
                            runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                        }
                    }) { Text("ALLOW", color = Ink) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        prefs.edit().putBoolean("perms_asked", true).apply()
                        showPermDialog = false
                    }) { Text("SKIP", color = Dim) }
                }
            )
        }
    }
}
