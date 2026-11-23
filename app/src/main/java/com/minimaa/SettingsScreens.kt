package com.minimaa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var selectedTheme by remember { mutableIntStateOf(prefs.getInt("theme_idx", 1)) }
    var selectedLang by remember { mutableStateOf(prefs.getString("app_lang", "System Default") ?: "System Default") }
    var showLangSheet by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar("MINIMAA", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PULL DOWN TO SEARCH", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Dim, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(10.dp))
            Text("PRIVACY", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            SettingsRow(Icons.Default.Security, "Transparency", "No tracking. No ads. No data collection.") {
                onNavigate("transparency")
            }

            Spacer(Modifier.height(18.dp))
            Text("PREFERENCES", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            SettingsRow(Icons.Default.Shield, "Digital Detox", "Mindful Pause & Web Detox.") {
                onNavigate("detox")
            }
            SettingsRow(Icons.Default.Home, "Home Settings", "Home Apps & Appearance.") {
                onNavigate("home_settings")
            }
            SettingsRow(Icons.Default.Apps, "App Drawer", "Search & Layout behavior.") {
                onNavigate("drawer_settings")
            }
            SettingsRow(Icons.Default.Gesture, "Gestures", "Swipe & Double Tap actions.") {
                onNavigate("gesture_settings")
            }
            SettingsRow(Icons.Default.Wallpaper, "Wallpaper", "Background & Immersion settings.") {
                onNavigate("wallpaper")
            }
            SettingsRow(Icons.Default.Language, "Language", "Change app language.") {
                showLangSheet = true
            }

            // Theme row with 4 inline circles
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, null, tint = Dim, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Theme", color = Color.White, fontSize = 17.sp)
                    Text("Switch Aesthetic", color = Dim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val swatches = listOf(Color(0xFF141414), Color.Black, Color(0xFF1E1E1E), Color.White)
                    swatches.forEachIndexed { idx, color ->
                        val active = selectedTheme == idx
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, if (active) Color.White else Line, CircleShape)
                                .clickable {
                                    selectedTheme = idx
                                    prefs.edit().putInt("theme_idx", idx).apply()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("SYSTEM", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            SettingsRow(Icons.Default.CloudUpload, "Backup & Restore", "Export or local data snapshots.") {
                activeDialog = "Backup & Restore"
            }
            SettingsRow(Icons.Default.Home, "Default Launcher", "Change Minimaa from default (don't)") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            }

            Spacer(Modifier.height(14.dp))
            Text("SYSTEM", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            SettingsRow(Icons.Default.Face, "Developer", "About Minimaa & Binge.") {
                activeDialog = "Developer"
            }

            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "REPORT BUG   •   RATE APP   •   SHARE APP",
                    color = Dim,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { activeDialog = "Community" }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "PRIVACY POLICY",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.clickable { onNavigate("transparency") }
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf(Icons.Default.Forum, Icons.Default.PhotoCamera, Icons.Default.Send).forEach { icon ->
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Line, RoundedCornerShape(12.dp))
                                .background(Card)
                                .clickable { activeDialog = "Community" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = Dim, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showLangSheet) {
        val languages = listOf(
            "System Default", "Deutsch", "English", "Español", "Français",
            "हिन्दी", "日本語", "Português", "中文"
        )
        ModalBottomSheet(
            onDismissRequest = { showLangSheet = false },
            containerColor = Color(0xFF0A0A0A),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 14.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF555555))
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Language",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(6.dp))
                languages.forEach { lang ->
                    val active = selectedLang == lang
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (active) Modifier.background(Color(0xFF141414)) else Modifier)
                            .clickable {
                                selectedLang = lang
                                prefs.edit().putString("app_lang", lang).apply()
                                showLangSheet = false
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            lang,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        if (active) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    activeDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            containerColor = Color(0xFF101010),
            title = { Text(title.uppercase(), color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                Text(
                    when (title) {
                        "Backup & Restore" -> "Local Room database (minimaa.db) and preferences are preserved across v5 updates via Room migration."
                        "Developer" -> "Minimaa v5 by Binge. Built with Kotlin & Jetpack Compose for zero-distraction minimal living."
                        else -> "Minimaa is completely offline and ad-free."
                    },
                    color = Dim,
                    fontSize = 13.sp
                )
            },
            confirmButton = { TextButton({ activeDialog = null }) { Text("OK", color = Color.White) } }
        )
    }
}

@Composable
fun HomeSettingsScreen(
    apps: List<AppInfo>,
    pins: Set<String>,
    onTogglePin: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }

    var rightPanel by remember { mutableStateOf(prefs.getBoolean("right_panel_enabled", true)) }
    var highRefresh by remember { mutableStateOf(prefs.getBoolean("high_refresh", false)) }
    var showHomeAppsDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<String?>(null) }

    var showClock by remember { mutableStateOf(prefs.getBoolean("w_clock", true)) }
    var showCalendar by remember { mutableStateOf(prefs.getBoolean("w_cal", true)) }
    var showMedia by remember { mutableStateOf(prefs.getBoolean("w_media", true)) }
    var showDaysLeft by remember { mutableStateOf(prefs.getBoolean("w_days", true)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("HOME SETTINGS", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionLabel("LAYOUT")
            SettingsRow(Icons.Default.StarBorder, "Home Apps", "Select favorites for home screen") {
                showHomeAppsDialog = true
            }
            SettingsRow(Icons.Default.Layers, "Glance Settings", "Manage segments, tasks, and journals") {
                infoDialog = "Glance Settings"
            }
            DetoxToggleRow("Right Settings Panel", "Access secondary side screen", rightPanel) {
                rightPanel = it; prefs.edit().putBoolean("right_panel_enabled", it).apply()
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("WIDGETS & VISUALS")
            SettingsRow(Icons.Default.Widgets, "Widget Center", "Manage clock, calendar & status bars") {
                showWidgetDialog = true
            }
            SettingsRow(Icons.Default.FormatAlignLeft, "Classic Style Rules", "Configure alignment and dot visibility") {
                infoDialog = "Classic Style Rules"
            }
            SettingsRow(Icons.Default.TextFields, "Typography", "Choose your global app font") {
                infoDialog = "Typography"
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("INTERACTION")
            SettingsRow(Icons.Default.TouchApp, "Interaction Settings", "Gestures, haptics and letter jump") {
                infoDialog = "Interaction Settings"
            }
            DetoxToggleRow(
                "Force High Refresh Rate",
                "Enables smooth 90Hz/120Hz scrolling on supported devices (may increase battery usage)",
                highRefresh
            ) {
                highRefresh = it; prefs.edit().putBoolean("high_refresh", it).apply()
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showHomeAppsDialog) {
        AlertDialog(
            onDismissRequest = { showHomeAppsDialog = false },
            containerColor = Color(0xFF101010),
            title = { Text("HOME APPS", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(apps, key = { it.pkg }) { app ->
                        val checked = app.pkg in pins
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onTogglePin(app.pkg) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onTogglePin(app.pkg) },
                                colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black)
                            )
                            Text(app.label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showHomeAppsDialog = false }) { Text("DONE", color = Color.White) } }
        )
    }

    if (showWidgetDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetDialog = false },
            containerColor = Color(0xFF101010),
            title = { Text("WIDGET CENTER", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                Column {
                    DetoxToggleRow("Dot-Matrix Clock", "Show clock on home screen", showClock) {
                        showClock = it; prefs.edit().putBoolean("w_clock", it).apply()
                    }
                    DetoxToggleRow("Week Calendar", "Show 7-day strip", showCalendar) {
                        showCalendar = it; prefs.edit().putBoolean("w_cal", it).apply()
                    }
                    DetoxToggleRow("Media Player", "Show live music widget", showMedia) {
                        showMedia = it; prefs.edit().putBoolean("w_media", it).apply()
                    }
                    DetoxToggleRow("Days Left Counter", "Show current year countdown", showDaysLeft) {
                        showDaysLeft = it; prefs.edit().putBoolean("w_days", it).apply()
                    }
                }
            },
            confirmButton = { TextButton({ showWidgetDialog = false }) { Text("DONE", color = Color.White) } }
        )
    }

    infoDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            containerColor = Color(0xFF101010),
            title = { Text(title.uppercase(), color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = { Text("$title defaults are active for your current Home Style.", color = Dim, fontSize = 13.sp) },
            confirmButton = { TextButton({ infoDialog = null }) { Text("OK", color = Color.White) } }
        )
    }
}

@Composable
fun AppDrawerSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var showTools by remember { mutableStateOf(prefs.getBoolean("drawer_tools", true)) }
    var showIndex by remember { mutableStateOf(prefs.getBoolean("drawer_index", true)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("APP DRAWER", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            SectionLabel("LAYOUT & SEARCH")
            DetoxToggleRow("Quick Tool Shortcuts", "Show Notes, Math, Cal, Pomo & Body icons", showTools) {
                showTools = it; prefs.edit().putBoolean("drawer_tools", it).apply()
            }
            DetoxToggleRow("A-Z Alphabet Scroller", "Show right-edge quick letter jump bar", showIndex) {
                showIndex = it; prefs.edit().putBoolean("drawer_index", it).apply()
            }
        }
    }
}

@Composable
fun GesturesSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var swipeUpDrawer by remember { mutableStateOf(prefs.getBoolean("g_swipe_up", true)) }
    var doubleTapLock by remember { mutableStateOf(prefs.getBoolean("g_double_tap", false)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("GESTURES", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            SectionLabel("HOME GESTURES")
            DetoxToggleRow("Swipe Up for Drawer", "Open App Drawer by swiping up on Home", swipeUpDrawer) {
                swipeUpDrawer = it; prefs.edit().putBoolean("g_swipe_up", it).apply()
            }
            DetoxToggleRow("Double Tap Action", "Quick-open Daily Loop on double tap", doubleTapLock) {
                doubleTapLock = it; prefs.edit().putBoolean("g_double_tap", it).apply()
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Dim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Text(sub, color = Dim, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Dim, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DigitalDetoxScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }

    var mindfulPause by remember { mutableStateOf(prefs.getBoolean("detox_pause", false)) }
    var webDetox by remember { mutableStateOf(prefs.getBoolean("detox_web", false)) }
    var pornBlock by remember { mutableStateOf(prefs.getBoolean("detox_adult", false)) }
    var notifShield by remember { mutableStateOf(prefs.getBoolean("detox_shield", false)) }
    var hideInactive by remember { mutableStateOf(prefs.getBoolean("detox_hide_inactive", false)) }
    var showUsage by remember { mutableStateOf(prefs.getBoolean("detox_show_usage", false)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("DIGITAL DETOX", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionLabel("APP & WEB BLOCKERS")
            DetoxToggleRow("Mindful Pause", "Wait 3s before opening distracting apps.", mindfulPause) {
                mindfulPause = it; prefs.edit().putBoolean("detox_pause", it).apply()
            }
            DetoxToggleRow("Web Detox", "Block distracting websites in your browser.", webDetox) {
                webDetox = it; prefs.edit().putBoolean("detox_web", it).apply()
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("ADULT BLOCK")
            DetoxToggleRow("Porn Block", "Block adult content everywhere.", pornBlock) {
                pornBlock = it; prefs.edit().putBoolean("detox_adult", it).apply()
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("FOCUS & SHIELD")
            DetoxToggleRow("Notification Shield", "Block distracting pings.", notifShield) {
                notifShield = it; prefs.edit().putBoolean("detox_shield", it).apply()
            }
            DetoxToggleRow("Hide Inactive Apps", "Hide apps not used in 7 days.", hideInactive) {
                hideInactive = it; prefs.edit().putBoolean("detox_hide_inactive", it).apply()
            }
            DetoxToggleRow("Show App Usage", "Display time spent on each app.", showUsage) {
                showUsage = it; prefs.edit().putBoolean("detox_show_usage", it).apply()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Dim, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun DetoxToggleRow(title: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Dim,
                uncheckedTrackColor = Card,
                uncheckedBorderColor = Color(0xFF444444)
            )
        )
    }
}

@Composable
fun WallpaperSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }

    var systemWallpaper by remember { mutableStateOf(prefs.getBoolean("wp_system", false)) }
    var dimming by remember { mutableFloatStateOf(prefs.getFloat("wp_dimming", 0.2f)) }
    var immersiveColor by remember { mutableIntStateOf(prefs.getInt("wp_immersive", 2)) }
    var showGlassDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("WALLPAPER SETTINGS", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionLabel("MAIN SOURCE")
            DetoxToggleRow("System Wallpaper", "Use solid theme colors when disabled", systemWallpaper) {
                systemWallpaper = it; prefs.edit().putBoolean("wp_system", it).apply()
            }

            if (systemWallpaper) {
                SettingsRow(Icons.Default.Image, "Change Wallpaper", "Open system picker for image selection") {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose Wallpaper"))
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("DIMMING")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Home Wallpaper Dimming", color = Color.White, fontSize = 17.sp)
                    Text("${(dimming * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Lower brightness for better contrast", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = dimming,
                    onValueChange = {
                        dimming = it
                        prefs.edit().putFloat("wp_dimming", it).apply()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )

                SettingsRow(Icons.Default.AutoAwesome, "Glass & Transparency", "Configure drawer and panel background blur") {
                    showGlassDialog = true
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("IMMERSION")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, null, tint = Dim, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Immersive Color", color = Color.White, fontSize = 17.sp)
                    Text("Seamless recents transition.", color = Dim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Color.Black, Color(0xFF1A1A1A), Color.White).forEachIndexed { idx, color ->
                        val active = immersiveColor == idx
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (active) 2.dp else 1.dp, if (active) Color.White else Line, CircleShape)
                                .clickable {
                                    immersiveColor = idx
                                    prefs.edit().putInt("wp_immersive", idx).apply()
                                }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showGlassDialog) {
        AlertDialog(
            onDismissRequest = { showGlassDialog = false },
            containerColor = Color(0xFF101010),
            title = { Text("GLASS & TRANSPARENCY", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = { Text("Drawer and panel blur adjusts automatically with your Home Wallpaper Dimming level.", color = Dim, fontSize = 13.sp) },
            confirmButton = { TextButton({ showGlassDialog = false }) { Text("OK", color = Color.White) } }
        )
    }
}

private data class AuditItem(
    val icon: ImageVector,
    val title: String,
    val manual: Boolean,
    val permission: String,
    val desc: String,
    val action: String? = null
)

@Composable
fun TransparencyScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val items = remember {
        listOf(
            AuditItem(Icons.Default.Block, "DETOX & GESTURES", true, "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "To enforce Mindful Pause globally, block distracting sites, and enable advanced gestures (swipe down/double tap).", Settings.ACTION_ACCESSIBILITY_SETTINGS),
            AuditItem(Icons.Default.NotificationsOff, "FOCUS SHIELD & MUSIC", true, "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                "To hide distracting pings and show music controls on your home screen.", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            AuditItem(Icons.Default.Timer, "MINDFUL TRACKING", true, "android.permission.PACKAGE_USAGE_STATS",
                "To know which app is currently open so we can show you a \"Pause\" challenge.", Settings.ACTION_USAGE_ACCESS_SETTINGS),
            AuditItem(Icons.Default.CalendarToday, "AGENDA & TIMELINE", true, "android.permission.READ_CALENDAR",
                "To show your today's events directly in the Agenda drawer local view.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            AuditItem(Icons.Default.Contacts, "CONTACTS ACCESS", true, "android.permission.READ_CONTACTS",
                "To show matching people directly in your app drawer for quick actions.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            AuditItem(Icons.Default.Call, "DIRECT DIALING", true, "android.permission.CALL_PHONE",
                "To call your favorite contacts directly from the search bar or app drawer.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            AuditItem(Icons.Default.DirectionsWalk, "STEP TRACKING", true, "android.permission.ACTIVITY_RECOGNITION",
                "To detect your movement so we can show your daily step count.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            AuditItem(Icons.Default.Timer, "TIMER ALERTS", true, "android.permission.POST_NOTIFICATIONS",
                "To show silent visual reminders when your Pomodoro sessions end.", Settings.ACTION_APP_NOTIFICATION_SETTINGS),
            AuditItem(Icons.Default.Alarm, "HIGH-PRECISION TIMING", true, "android.permission.SCHEDULE_EXACT_ALARM",
                "To ensure your task reminders and pomodoro alerts arrive at the exact second requested.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            AuditItem(Icons.Default.Vibration, "MOTION GESTURES", false, "android.hardware.sensor.accelerometer",
                "Detects physical phone movements for navigation."),
            AuditItem(Icons.Default.Fingerprint, "BIOMETRIC SECURITY", false, "android.permission.USE_BIOMETRIC",
                "To unlock your Private Space using your fingerprint or face."),
            AuditItem(Icons.Default.History, "ALARM RECOVERY", false, "android.permission.RECEIVE_BOOT_COMPLETED",
                "To restore your pending task reminders automatically after a phone restart."),
            AuditItem(Icons.Default.Apps, "APP DISCOVERY", false, "android.permission.QUERY_ALL_PACKAGES",
                "To show you the list of your apps so you can launch them."),
            AuditItem(Icons.Default.KeyboardArrowDown, "STATUS BAR GESTURES", false, "android.permission.EXPAND_STATUS_BAR",
                "To expand the notification panel when you swipe down on the home screen."),
            AuditItem(Icons.Default.Delete, "APP MANAGEMENT", false, "android.permission.REQUEST_DELETE_PACKAGES",
                "To allow you to uninstall apps directly from their long-press menu."),
            AuditItem(Icons.Default.Image, "PURE AESTHETICS", false, "android.permission.SET_WALLPAPER",
                "To set your background to pure black for a seamless experience."),
            AuditItem(Icons.Default.SignalCellularAlt, "STATUS BAR CONNECTIVITY", false, "android.permission.ACCESS_NETWORK_STATE",
                "To show you the Wi-Fi and Cellular signal icons in our custom status bar."),
            AuditItem(Icons.Default.LocalCafe, "DEVELOPER SUPPORT", false, "com.android.vending.BILLING",
                "To process optional one-time coffee donations via Google Play.")
        )
    }

    fun openSetting(action: String?) {
        if (action == null) return
        val intent = when (action) {
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS -> Intent(action, Uri.fromParts("package", ctx.packageName, null))
            Settings.ACTION_APP_NOTIFICATION_SETTINGS -> Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            else -> Intent(action)
        }
        runCatching { ctx.startActivity(intent) }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("TRANSPARENCY", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionLabel("OUR PROMISE")
            Text(
                "We value your privacy above everything else. Minimaa does not collect, sell, or even see your personal data. Everything happens right on your device.",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .background(Card)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.CloudOff, null, tint = Dim, modifier = Modifier.size(22.dp))
                Column {
                    Text("OFFLINE BY DESIGN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Minimaa has zero internet access capability built-in (no cloud sync). It is completely offline and does not track you.",
                        color = Dim,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("PERMISSION AUDIT")
            Spacer(Modifier.height(4.dp))

            items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(item.icon, null, tint = Dim, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            if (item.manual) {
                                Text(
                                    "MANAGE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.clickable { openSetting(item.action) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (item.manual) Color(0xFF282012) else Color(0xFF102818))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (item.manual) "MANUAL" else "AUTOMATIC",
                                    color = if (item.manual) Color(0xFFD99B38) else Color(0xFF4CAF50),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(item.permission, color = Color(0xFF555555), fontSize = 9.sp, maxLines = 1)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(item.desc, color = Dim, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
