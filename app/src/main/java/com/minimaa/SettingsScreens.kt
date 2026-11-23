package com.minimaa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
fun SettingsScreen(vm: LoopVM, journals: List<Journal>, tasks: List<Task>, onThemeChange: (Int) -> Unit, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var selectedTheme by remember { mutableIntStateOf(prefs.getInt("theme_idx", 1)) }
    var selectedLang by remember { mutableStateOf(prefs.getString("app_lang", "System Default") ?: "System Default") }
    var showLangSheet by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(BackupHelper.exportJson(journals, tasks).toByteArray())
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val raw = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                val (jl, tl) = BackupHelper.importJson(raw)
                jl.forEach { j -> vm.saveJournal(null, j.text, j.mood, if (j.branches.isBlank()) emptyList() else j.branches.split("\n•|•\n")) }
                tl.forEach { t -> vm.saveTask(null, t.title, t.type, t.category, t.priority, t.scheduledAt) }
            }
        }
    }

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
                    Text("Theme", color = Ink, fontSize = 17.sp)
                    Text("Switch Aesthetic", color = Dim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val swatches = listOf(Color(0xFF141414), Color(0xFF000000), Color(0xFF1E1E1E), Color(0xFFFFFFFF))
                    swatches.forEachIndexed { idx, color ->
                        val active = selectedTheme == idx
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, if (active) Ink else Line, CircleShape)
                                .clickable {
                                    selectedTheme = idx
                                    prefs.edit().putInt("theme_idx", idx).apply()
                                    onThemeChange(idx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (color == Color(0xFFFFFFFF)) Color(0xFF000000) else Color(0xFFFFFFFF),
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
            containerColor = SheetCol,
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
                    color = Ink,
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
                                val tag = when (lang) {
                                    "Deutsch" -> "de"; "English" -> "en"; "Español" -> "es"
                                    "Français" -> "fr"; "हिन्दी" -> "hi"; "日本語" -> "ja"
                                    "Português" -> "pt"; "中文" -> "zh"; else -> ""
                                }
                                val loc = if (tag.isEmpty()) java.util.Locale.getDefault() else java.util.Locale(tag)
                                java.util.Locale.setDefault(loc)
                                val cfg = ctx.resources.configuration
                                cfg.setLocale(loc)
                                @Suppress("DEPRECATION")
                                ctx.resources.updateConfiguration(cfg, ctx.resources.displayMetrics)
                                showLangSheet = false
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            lang,
                            color = Ink,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        if (active) {
                            Icon(Icons.Default.Check, null, tint = Ink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    activeDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            containerColor = SheetCol,
            title = { Text(title.uppercase(), color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                Text(
                    when (title) {
                        "Backup & Restore" -> "Export your journals and tasks to a local JSON file, or restore from a previous snapshot."
                        "Developer" -> "Minimaa v10 by Binge. Built with Kotlin & Jetpack Compose for zero-distraction minimal living."
                        else -> "Minimaa is completely offline and ad-free."
                    },
                    color = Dim,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                if (title == "Backup & Restore") {
                    Row {
                        TextButton({ activeDialog = null; exportLauncher.launch("minimaa_backup.json") }) { Text("EXPORT", color = Ink) }
                        TextButton({ activeDialog = null; importLauncher.launch(arrayOf("application/json")) }) { Text("IMPORT", color = Dim) }
                    }
                } else {
                    TextButton({ activeDialog = null }) { Text("OK", color = Ink) }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsScreen(
    apps: List<AppInfo>,
    pins: Set<String>,
    onTogglePin: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }

    var rightPanel by remember { mutableStateOf(prefs.getBoolean("right_panel_enabled", true)) }
    var highRefresh by remember { mutableStateOf(prefs.getBoolean("high_refresh", false)) }
    var showHomeAppsDialog by remember { mutableStateOf(false) }
    var showWidgetDialog by remember { mutableStateOf(false) }
    var showClassicSheet by remember { mutableStateOf(false) }
    var classicAlign by remember { mutableStateOf(prefs.getString("classic_align", "LEFT") ?: "LEFT") }
    var classicDot by remember { mutableStateOf(prefs.getBoolean("classic_dot", true)) }
    var showEvents by remember { mutableStateOf(prefs.getBoolean("w_events", false)) }
    var minimaaBar by remember { mutableStateOf(prefs.getBoolean("w_minimaa_bar", true)) }
    var nativeBar by remember { mutableStateOf(prefs.getBoolean("w_native_bar", false)) }
    var showTypographySheet by remember { mutableStateOf(false) }
    var showInteractionSheet by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<String?>(null) }
    var globalFont by remember { mutableStateOf(prefs.getString("global_font", "MINIMAA") ?: "MINIMAA") }
    var haptics by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", true)) }
    var alphaScroller by remember { mutableStateOf(prefs.getBoolean("drawer_index", true)) }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { prefs.edit().putString("custom_font_uri", uri.toString()).apply() }
    }

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
                onNavigate("home_apps")
            }
            SettingsRow(Icons.Default.Layers, "Glance Settings", "Manage segments, tasks, and journals") {
                onNavigate("glance_settings")
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
                showClassicSheet = true
            }
            SettingsRow(Icons.Default.TextFields, "Typography", "Choose your global app font") {
                showTypographySheet = true
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("INTERACTION")
            SettingsRow(Icons.Default.TouchApp, "Interaction Settings", "Gestures, haptics and letter jump") {
                showInteractionSheet = true
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
            containerColor = SheetCol,
            title = { Text("HOME APPS", color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
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
                                colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk)
                            )
                            Text(app.label, color = Ink, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showHomeAppsDialog = false }) { Text("DONE", color = Ink) } }
        )
    }

    if (showWidgetDialog) {
        ModalBottomSheet(
            onDismissRequest = { showWidgetDialog = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("WIDGET CENTER", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("CORE WIDGETS")
                val core = listOf(
                    Triple("CLOCK", showClock) { v: Boolean -> showClock = v; prefs.edit().putBoolean("w_clock", v).apply() },
                    Triple("CALENDAR", showCalendar) { v: Boolean -> showCalendar = v; prefs.edit().putBoolean("w_cal", v).apply() },
                    Triple("MUSIC", showMedia) { v: Boolean -> showMedia = v; prefs.edit().putBoolean("w_media", v).apply() },
                    Triple("SNAKE / GAME", showDaysLeft) { v: Boolean -> showDaysLeft = v; prefs.edit().putBoolean("w_days", v).apply() }
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    core.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (lbl, active, toggle) ->
                                Box(
                                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, if (active) Ink else Line, RoundedCornerShape(10.dp))
                                        .background(Card)
                                        .clickable { toggle(!active) },
                                    contentAlignment = Alignment.Center
                                ) { Text(lbl, color = if (active) Ink else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                DetoxToggleRow("Show Today's Events", "Show today's events under the calendar", showEvents) {
                    showEvents = it; prefs.edit().putBoolean("w_events", it).apply()
                }
                Spacer(Modifier.height(8.dp))
                SectionLabel("STATUS BARS")
                DetoxToggleRow("Minimaa Status Bar", "Show custom bar at the top", minimaaBar) {
                    minimaaBar = it; prefs.edit().putBoolean("w_minimaa_bar", it).apply()
                }
                DetoxToggleRow("Native Status Bar", "Show phone system status bar", nativeBar) {
                    nativeBar = it; prefs.edit().putBoolean("w_native_bar", it).apply()
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("TIP: LONG PRESS WIDGETS ON HOME TO CUSTOMIZE STYLES", color = Color(0xFF555555), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
    }

    if (showClassicSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClassicSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("CLASSIC RULES", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("APP ALIGNMENT")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LEFT", "CENTER", "RIGHT").forEach { al ->
                        val active = classicAlign == al
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(10.dp))
                                .background(Card)
                                .clickable { classicAlign = al; prefs.edit().putString("classic_align", al).apply() },
                            contentAlignment = Alignment.Center
                        ) { Text(al, color = if (active) Ink else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("DECORATION")
                DetoxToggleRow("Leading Dot", "Show dot next to app names", classicDot) {
                    classicDot = it; prefs.edit().putBoolean("classic_dot", it).apply()
                }
            }
        }
    }

    if (showTypographySheet) {
        ModalBottomSheet(
            onDismissRequest = { showTypographySheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("TYPOGRAPHY", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("GLOBAL FONT")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("SYSTEM", "MINIMAA").forEach { fnt ->
                        val active = globalFont == fnt
                        Box(
                            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(12.dp))
                                .background(Card)
                                .clickable { globalFont = fnt; prefs.edit().putString("global_font", fnt).apply() },
                            contentAlignment = Alignment.Center
                        ) { Text(fnt, color = if (active) Ink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).background(Card).padding(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("USE CUSTOM FONT", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                        Icon(Icons.Default.AddCircleOutline, null, tint = Dim, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { fontPicker.launch(arrayOf("font/*", "application/x-font-ttf", "application/octet-stream")) },
                        contentAlignment = Alignment.Center
                    ) { Text("+  PICK", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
                }
            }
        }
    }

    if (showInteractionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInteractionSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("INTERACTION", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                DetoxToggleRow("Haptic Feedback", "Tactile response on interaction", haptics) {
                    haptics = it; prefs.edit().putBoolean("haptic_feedback", it).apply()
                }
                DetoxToggleRow("Alphabet Scroller", "Screen edge drag letter jump", alphaScroller) {
                    alphaScroller = it; prefs.edit().putBoolean("drawer_index", it).apply()
                }
            }
        }
    }

    infoDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            containerColor = SheetCol,
            title = { Text(title.uppercase(), color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = { Text("$title defaults are active for your current Home Style.", color = Dim, fontSize = 13.sp) },
            confirmButton = { TextButton({ infoDialog = null }) { Text("OK", color = Ink) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var transitionStyle by remember { mutableStateOf(prefs.getString("drawer_transition", "INSTANT") ?: "INSTANT") }
    var showScrollerSheet by remember { mutableStateOf(false) }
    var showLayoutSheet by remember { mutableStateOf(false) }
    var showTransitionSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("APP DRAWER", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SectionLabel("GENERAL")
            SettingsRow(Icons.Default.Search, "Search & Navigation", "Search bar placement, web search & contacts") {
                onNavigate("drawer_search")
            }
            SettingsRow(Icons.Default.Folder, "App Folders", "Create folders and manage categorization") {
                onNavigate("drawer_folders")
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("SECURITY & PRIVACY")
            SettingsRow(Icons.Default.Lock, "Private Space", "Lock sensitive apps in a secure vault") {
                onNavigate("drawer_private")
            }
            SettingsRow(Icons.Default.VisibilityOff, "Hidden Apps", "Hide apps entirely from the app drawer") {
                onNavigate("drawer_hidden")
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("LAYOUT & GESTURES")
            SettingsRow(Icons.Default.SwapVert, "Scroller Configuration", "Positioning, drag areas and focus scroll") {
                showScrollerSheet = true
            }
            SettingsRow(Icons.Default.Visibility, "Layout & Visibility", "Alignment, Google apps and system apps") {
                showLayoutSheet = true
            }
            SettingsRow(Icons.Default.Movie, "Transition Style", transitionStyle) {
                showTransitionSheet = true
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showScrollerSheet) {
        var pos by remember { mutableStateOf(prefs.getString("drawer_scroller_pos", "RIGHT") ?: "RIGHT") }
        var dragArea by remember { mutableStateOf(prefs.getString("drawer_drag_area", "BOTH") ?: "BOTH") }
        var alwaysShow by remember { mutableStateOf(prefs.getBoolean("drawer_scroller_always", true)) }
        var focusScroll by remember { mutableStateOf(prefs.getBoolean("drawer_focus_scroll", false)) }
        var draggable by remember { mutableStateOf(prefs.getBoolean("drawer_draggable", false)) }

        ModalBottomSheet(
            onDismissRequest = { showScrollerSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("SCROLLER CONFIG", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("POSITION")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("LEFT", "RIGHT").forEach { opt ->
                        val active = pos == opt
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(10.dp))
                                .background(Card)
                                .clickable { pos = opt; prefs.edit().putString("drawer_scroller_pos", opt).apply() },
                            contentAlignment = Alignment.Center
                        ) { Text(opt, color = if (active) Ink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("DRAG AREA")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LEFT", "BOTH", "RIGHT").forEach { opt ->
                        val active = dragArea == opt
                        Box(
                            Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(10.dp))
                                .background(Card)
                                .clickable { dragArea = opt; prefs.edit().putString("drawer_drag_area", opt).apply() },
                            contentAlignment = Alignment.Center
                        ) { Text(opt, color = if (active) Ink else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("BEHAVIOR")
                DetoxToggleRow("Always Show", "Keep current scroller visible", alwaysShow) {
                    alwaysShow = it; prefs.edit().putBoolean("drawer_scroller_always", it).apply()
                }
                DetoxToggleRow("Focus Scrolling", "Only show matches while scrolling", focusScroll) {
                    focusScroll = it; prefs.edit().putBoolean("drawer_focus_scroll", it).apply()
                }
                DetoxToggleRow("Draggable Scroller", "Allow pulling the scroller vertically", draggable) {
                    draggable = it; prefs.edit().putBoolean("drawer_draggable", it).apply()
                }
            }
        }
    }

    if (showLayoutSheet) {
        var degoogle by remember { mutableStateOf(prefs.getBoolean("drawer_degoogle", false)) }
        var minimaaHub by remember { mutableStateOf(prefs.getBoolean("drawer_hub", true)) }
        var hideHeaders by remember { mutableStateOf(prefs.getBoolean("drawer_hide_headers", false)) }
        var rightAlign by remember { mutableStateOf(prefs.getBoolean("drawer_right_align", false)) }

        ModalBottomSheet(
            onDismissRequest = { showLayoutSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("DRAWER LAYOUT", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("APP FILTERS")
                DetoxToggleRow("De-google", "Hide all Google applications", degoogle) {
                    degoogle = it; prefs.edit().putBoolean("drawer_degoogle", it).apply()
                }
                DetoxToggleRow("Minimaa Hub", "Show Minimaa apps in list", minimaaHub) {
                    minimaaHub = it; prefs.edit().putBoolean("drawer_hub", it).apply()
                }
                DetoxToggleRow("Hide Alphabet Titles", "Remove letter headers from the app list", hideHeaders) {
                    hideHeaders = it; prefs.edit().putBoolean("drawer_hide_headers", it).apply()
                }
                Spacer(Modifier.height(10.dp))
                SectionLabel("ALIGNMENT")
                DetoxToggleRow("Sinful Alignment", "Align app text to the right", rightAlign) {
                    rightAlign = it; prefs.edit().putBoolean("drawer_right_align", it).apply()
                }
            }
        }
    }

    if (showTransitionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTransitionSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("TRANSITION STYLE", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("Transition Style")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INSTANT", "SPRINGY", "FAST FADE").forEach { st ->
                        val active = transitionStyle == st
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(10.dp))
                                .background(Card)
                                .clickable {
                                    transitionStyle = st
                                    prefs.edit().putString("drawer_transition", st).apply()
                                    showTransitionSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(st, color = if (active) Ink else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerSearchBarsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var bottomSearch by remember { mutableStateOf(prefs.getBoolean("drawer_bottom_search", false)) }
    var autoKeyboard by remember { mutableStateOf(prefs.getBoolean("drawer_auto_kb", false)) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("drawer_auto_launch", false)) }
    var webSearch by remember { mutableStateOf(prefs.getBoolean("drawer_web_search", false)) }
    var contactSearch by remember { mutableStateOf(prefs.getBoolean("drawer_contact_search", false)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("SEARCH BARS", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            SectionLabel("SEARCH & NAVIGATION")
            DetoxToggleRow("Bottom Search Bar", "Display search bar at bottom of the drawer.", bottomSearch) {
                bottomSearch = it; prefs.edit().putBoolean("drawer_bottom_search", it).apply()
            }
            DetoxToggleRow("Keyboard Auto-Open", "Open keyboard immediately upon entry.", autoKeyboard) {
                autoKeyboard = it; prefs.edit().putBoolean("drawer_auto_kb", it).apply()
            }
            DetoxToggleRow("Auto-Launch App", "Open app automatically if it's the only result.", autoLaunch) {
                autoLaunch = it; prefs.edit().putBoolean("drawer_auto_launch", it).apply()
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("INTEGRATIONS")
            DetoxToggleRow("Web Search", "Find results on the web when apps don't match.", webSearch) {
                webSearch = it; prefs.edit().putBoolean("drawer_web_search", it).apply()
            }
            DetoxToggleRow("Contact Search", "Find and call contacts from the search bar.", contactSearch) {
                contactSearch = it; prefs.edit().putBoolean("drawer_contact_search", it).apply()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerFoldersScreen(apps: List<AppInfo>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var foldersTop by remember { mutableStateOf(prefs.getBoolean("folder_top", false)) }
    var folderMenu by remember { mutableStateOf(prefs.getBoolean("folder_menu", false)) }
    var quickEdit by remember { mutableStateOf(prefs.getBoolean("folder_quick_edit", true)) }
    var folders by remember { mutableStateOf(prefs.getStringSet("drawer_folders", emptySet()).orEmpty().toList().sorted()) }
    var showNewFolder by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("FOLDERS", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            SectionLabel("BEHAVIOR")
            DetoxToggleRow("Folders at Top", "Show app groups above the main list.", foldersTop) {
                foldersTop = it; prefs.edit().putBoolean("folder_top", it).apply()
            }
            DetoxToggleRow("Folder Action in Menu", "Add 'Create Folder' to app long-press options.", folderMenu) {
                folderMenu = it; prefs.edit().putBoolean("folder_menu", it).apply()
            }
            DetoxToggleRow("Show Quick Edit", "Show 'ADD APPS' button in the folder sheet.", quickEdit) {
                quickEdit = it; prefs.edit().putBoolean("folder_quick_edit", it).apply()
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("YOUR GROUPS")
            if (folders.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No folders created yet.", color = Dim, fontSize = 14.sp)
                }
            } else {
                folders.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp)).background(Card).padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.Delete, null, tint = Dim, modifier = Modifier.size(18.dp).clickable {
                            folders = folders - name
                            prefs.edit().putStringSet("drawer_folders", folders.toSet()).apply()
                        })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showNewFolder = true }, contentAlignment = Alignment.Center) {
                Text("+  CREATE NEW FOLDER", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }

    if (showNewFolder) {
        var folderName by remember { mutableStateOf("") }
        var q by remember { mutableStateOf("") }
        var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
        ModalBottomSheet(
            onDismissRequest = { showNewFolder = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("NEW FOLDER", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp)).background(Card), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, null, tint = Ink, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = { folderName = it },
                            placeholder = { Text("Folder Name", color = Dim) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${selectedApps.size} apps selected", color = Dim, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        if (folderName.isNotBlank()) {
                            folders = (folders + folderName.trim()).distinct()
                            prefs.edit().putStringSet("drawer_folders", folders.toSet()).apply()
                            prefs.edit().putStringSet("folder_${folderName.trim()}", selectedApps).apply()
                        }
                        showNewFolder = false
                    }) { Text("SAVE", color = Ink, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = q, onValueChange = { q = it }, placeholder = { Text("Search apps...", color = Dim) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(apps.filter { it.label.contains(q, true) }, key = { it.pkg }) { app ->
                        val checked = app.pkg in selectedApps
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedApps = if (checked) selectedApps - app.pkg else selectedApps + app.pkg }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, color = Ink, fontSize = 14.sp)
                                Text(app.pkg, color = Dim, fontSize = 10.sp)
                            }
                            Checkbox(checked = checked, onCheckedChange = { selectedApps = if (checked) selectedApps - app.pkg else selectedApps + app.pkg },
                                colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerPrivateSpaceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("private_enabled", true)) }
    var accessSide by remember { mutableStateOf(prefs.getString("private_side", "LEFT") ?: "LEFT") }
    var useFingerprint by remember { mutableStateOf(prefs.getBoolean("private_fp", false)) }
    var showPinDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("PRIVATE SPACE", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            DetoxToggleRow("Private Space", "Lock sensitive apps in a secure vault", enabled) {
                enabled = it; prefs.edit().putBoolean("private_enabled", it).apply()
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("ORIENTATION")
            Text("ACCESS SIDE", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Swipe from this edge of the app drawer to open your private space.", color = Dim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Line, RoundedCornerShape(10.dp)).background(Card).padding(4.dp)) {
                listOf("LEFT", "RIGHT").forEach { side ->
                    val active = accessSide == side
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .then(if (active) Modifier.background(Ink) else Modifier)
                            .clickable { accessSide = side; prefs.edit().putString("private_side", side).apply() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(side, color = if (active) InvInk else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionLabel("SECURITY")
            DetoxToggleRow("Unlock with Fingerprint", "Use your device biometrics to quickly unlock the vault.", useFingerprint) {
                useFingerprint = it; prefs.edit().putBoolean("private_fp", it).apply()
            }
            SettingsRow(Icons.Default.Fingerprint, "Reset Vault PIN", "Authenticate with biometrics to set a new PIN.") {
                showPinDialog = true
            }
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = SheetCol,
            title = { Text("SET VAULT PIN", color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 6) newPin = it }, placeholder = { Text("4-6 digit PIN", color = Dim) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPin.length >= 4) prefs.edit().putString("private_pin", newPin).apply()
                    showPinDialog = false
                }) { Text("SAVE", color = Ink) }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("CANCEL", color = Dim) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerHiddenAppsScreen(apps: List<AppInfo>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(prefs.getStringSet("hidden_pkgs", emptySet()).orEmpty().toSet()) }
    var showHideOption by remember { mutableStateOf(prefs.getBoolean("show_hide_option", true)) }
    var showOptSheet by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopBar("HIDDEN APPS", onBack) {
            IconButton({ showOptSheet = true }) { Icon(Icons.Default.Settings, null, tint = Dim) }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("Search apps...", color = Dim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Dim) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(apps.filter { it.label.contains(q, true) }, key = { it.pkg }) { app ->
                    val isHidden = app.pkg in hidden
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                hidden = if (isHidden) hidden - app.pkg else hidden + app.pkg
                                prefs.edit().putStringSet("hidden_pkgs", hidden).apply()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Ink, fontSize = 16.sp)
                            Text(app.pkg, color = Dim, fontSize = 11.sp)
                        }
                        Checkbox(
                            checked = isHidden,
                            onCheckedChange = {
                                hidden = if (isHidden) hidden - app.pkg else hidden + app.pkg
                                prefs.edit().putStringSet("hidden_pkgs", hidden).apply()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk)
                        )
                    }
                }
            }
        }
    }

    if (showOptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptSheet = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("HIDDEN APPS", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                DetoxToggleRow("Show Hide Option", "Show 'Hide app' option in app long-press menu", showHideOption) {
                    showHideOption = it; prefs.edit().putBoolean("show_hide_option", it).apply()
                }
            }
        }
    }
}

@Composable
fun GesturesSettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var swipeNotifs by remember { mutableStateOf(prefs.getBoolean("g_swipe_notifs", false)) }
    var doubleTapLock by remember { mutableStateOf(prefs.getBoolean("g_double_tap_lock", false)) }
    var longPressHome by remember { mutableStateOf(prefs.getBoolean("g_long_press", false)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("GESTURES", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
            DetoxToggleRow("Swipe for Notifications", "Swipe down on home to open panel.", swipeNotifs) {
                swipeNotifs = it; prefs.edit().putBoolean("g_swipe_notifs", it).apply()
            }
            DetoxToggleRow("Double Tap and Lock", "Double tap on home to lock screen.", doubleTapLock) {
                doubleTapLock = it; prefs.edit().putBoolean("g_double_tap_lock", it).apply()
            }
            DetoxToggleRow("Long Press Gesture", "Long press on home to trigger an action.", longPressHome) {
                longPressHome = it; prefs.edit().putBoolean("g_long_press", it).apply()
            }
            Row(
                Modifier.fillMaxWidth().clickable { onNavigate("motion_gestures") }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Motion Gestures", color = Ink, fontSize = 17.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Twist, shake or tilt to perform actions.", color = Dim, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Dim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionGesturesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("motion_enabled", false)) }
    var sensitivity by remember { mutableFloatStateOf(prefs.getFloat("motion_sens", 3f)) }
    var showAccessConfirm by remember { mutableStateOf(false) }
    var activeMotionKey by remember { mutableStateOf<String?>(null) }

    val gestures = listOf(
        "motion_shake" to "Shake Phone",
        "motion_twist_l" to "Twist Left",
        "motion_twist_r" to "Twist Right",
        "motion_tilt_f" to "Tilt Forward",
        "motion_tilt_b" to "Tilt Backward"
    )
    val actions = listOf(
        "System Back" to Icons.Default.ArrowBack,
        "System Home" to Icons.Default.Home,
        "Recent Apps" to Icons.Default.Layers,
        "Notifications" to Icons.Default.NotificationsNone,
        "Quick Settings" to Icons.Default.Settings,
        "Lock Screen" to Icons.Default.Lock,
        "Toggle Flashlight" to Icons.Default.FlashlightOn,
        "Volume Up" to Icons.Default.VolumeUp,
        "Volume Down" to Icons.Default.VolumeDown,
        "Cycle Ringer Mode" to Icons.Default.Loop,
        "Open Camera" to Icons.Default.PhotoCamera,
        "Open App..." to Icons.Default.Apps
    )

    Column(Modifier.fillMaxSize()) {
        TopBar("MOTION GESTURES  BETA", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            DetoxToggleRow("Enable Motion Gestures", "Twist, shake or tilt to perform actions.", enabled) { newVal ->
                if (newVal) showAccessConfirm = true else {
                    enabled = false; prefs.edit().putBoolean("motion_enabled", false).apply()
                }
            }
            Spacer(Modifier.height(10.dp))
            gestures.forEach { (key, title) ->
                val cur = prefs.getString(key, "None") ?: "None"
                Row(
                    Modifier.fillMaxWidth().clickable { activeMotionKey = key }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Ink, fontSize = 17.sp)
                        Text(cur, color = Dim, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Dim, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("SENSITIVITY")
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it; prefs.edit().putFloat("motion_sens", it).apply() },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Color(0xFF333333))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val lvl = sensitivity.toInt()
                Text(when (lvl) { 1 -> "VERY LOW"; 2 -> "LOW"; 3 -> "NORMAL"; 4 -> "HIGH"; else -> "VERY HIGH" }, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("$lvl / 5", color = Dim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text("Higher sensitivity makes gestures easier to trigger with smaller movements.", color = Dim, fontSize = 12.sp)
        }
    }

    if (showAccessConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showAccessConfirm = false },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("CONFIRMATION", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("ANDROID ACCESSIBILITY SERVICE", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Minimaa uses Android Accessibility Service to power Motion & Shortcut Gestures, Double-Tap to Lock, Mindful Pause, and Web Detox. Everything is processed locally on your phone.",
                    color = Dim, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Line, RoundedCornerShape(14.dp)).background(Card).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Shield, null, tint = Dim, modifier = Modifier.size(20.dp))
                    Column {
                        Text("100% Privacy-First Guarantee", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("All data is processed locally on your phone.", color = Dim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { showAccessConfirm = false },
                        contentAlignment = Alignment.Center
                    ) { Text("DON'T ALLOW", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Ink)
                            .clickable {
                                enabled = true; prefs.edit().putBoolean("motion_enabled", true).apply()
                                showAccessConfirm = false
                                runCatching { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("ALLOW PERMISSION", color = InvInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                }
            }
        }
    }

    activeMotionKey?.let { key ->
        val currentAct = prefs.getString(key, "None") ?: "None"
        ModalBottomSheet(
            onDismissRequest = { activeMotionKey = null },
            containerColor = SheetCol,
            dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("CHOOSE ACTION", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(actions) { (name, icon) ->
                        val active = currentAct == name
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .then(if (active) Modifier.border(1.dp, Line, RoundedCornerShape(12.dp)).background(Card) else Modifier)
                                .clickable {
                                    prefs.edit().putString(key, name).apply()
                                    activeMotionKey = null
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = Dim, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(name, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (active) Icon(Icons.Default.CheckCircleOutline, null, tint = Ink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
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
            Text(title, color = Ink, fontSize = 17.sp)
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
            Text(title, color = Ink, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InvInk,
                checkedTrackColor = Ink,
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
                    Text("Home Wallpaper Dimming", color = Ink, fontSize = 17.sp)
                    Text("${(dimming * 100).toInt()}%", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        thumbColor = Ink,
                        activeTrackColor = Ink,
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
                    Text("Immersive Color", color = Ink, fontSize = 17.sp)
                    Text("Seamless recents transition.", color = Dim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Color(0xFF000000), Color(0xFF1A1A1A), Color(0xFFFFFFFF)).forEachIndexed { idx, color ->
                        val active = immersiveColor == idx
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (active) 2.dp else 1.dp, if (active) Ink else Line, CircleShape)
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
            containerColor = SheetCol,
            title = { Text("GLASS & TRANSPARENCY", color = Ink, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = { Text("Drawer and panel blur adjusts automatically with your Home Wallpaper Dimming level.", color = Dim, fontSize = 13.sp) },
            confirmButton = { TextButton({ showGlassDialog = false }) { Text("OK", color = Ink) } }
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
                color = Ink,
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
                    Text("OFFLINE BY DESIGN", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                            Text(item.title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            if (item.manual) {
                                Text(
                                    "MANAGE",
                                    color = Ink,
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


@Composable
fun HomeAppsScreen(
    apps: List<AppInfo>,
    pins: Set<String>,
    onTogglePin: (String) -> Unit,
    onBack: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("HOME APPS", onBack) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Check, null, tint = Ink) }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("Search apps...", color = Dim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Dim) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            val sorted = remember(apps, pins, q) {
                apps.filter { it.label.contains(q, true) }
                    .sortedWith(compareByDescending<AppInfo> { it.pkg in pins }.thenBy { it.label.lowercase() })
            }
            LazyColumn {
                items(sorted, key = { it.pkg }) { app ->
                    val checked = app.pkg in pins
                    Row(
                        Modifier.fillMaxWidth().clickable { onTogglePin(app.pkg) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Ink, fontSize = 16.sp)
                            Text(app.pkg, color = Dim, fontSize = 11.sp)
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onTogglePin(app.pkg) },
                            colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlanceSettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var enableGlance by remember { mutableStateOf(prefs.getBoolean("glance_enabled", true)) }
    var showJournal by remember { mutableStateOf(prefs.getBoolean("glance_journal", true)) }
    var showTasks by remember { mutableStateOf(prefs.getBoolean("glance_tasks", true)) }
    var showNotifs by remember { mutableStateOf(prefs.getBoolean("glance_notifs", true)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("GLANCE SETTINGS", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            SectionLabel("MAIN FEATURE")
            DetoxToggleRow("Enable Glance Panel", "Swipe left on home to view dashboard.", enableGlance) {
                enableGlance = it; prefs.edit().putBoolean("glance_enabled", it).apply()
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("PANEL SECTIONS")
            DetoxToggleRow("Show Journal", "Display quick thoughts and notes.", showJournal) {
                showJournal = it; prefs.edit().putBoolean("glance_journal", it).apply()
            }
            DetoxToggleRow("Show Tasks", "Display intentions and to-do list.", showTasks) {
                showTasks = it; prefs.edit().putBoolean("glance_tasks", it).apply()
            }
            DetoxToggleRow("Show Notifications", "Display configured app notifications.", showNotifs) {
                showNotifs = it; prefs.edit().putBoolean("glance_notifs", it).apply()
            }
            Spacer(Modifier.height(8.dp))
            SettingsRow(Icons.Default.ViewSidebar, "Select Sidebar Apps", "Choose which apps show notifications.") {
                onNavigate("sidebar_apps")
            }
        }
    }
}

@Composable
fun SidebarAppsScreen(
    apps: List<AppInfo>,
    sidebarPkgs: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit
) {
    var picked by remember { mutableStateOf(sidebarPkgs) }
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("SIDEBAR APPS", onBack) {
            IconButton(onClick = { onSave(picked); onBack() }) {
                Icon(Icons.Default.Check, null, tint = Ink)
            }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("Search apps...", color = Dim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Dim) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("Only selected apps will appear in the sidebar.", color = Dim, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(10.dp))
            LazyColumn {
                items(apps.filter { it.label.contains(q, true) }, key = { it.pkg }) { app ->
                    val checked = app.pkg in picked
                    Row(
                        Modifier.fillMaxWidth().clickable { picked = if (checked) picked - app.pkg else picked + app.pkg }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = Ink, fontSize = 16.sp)
                            Text(app.pkg, color = Dim, fontSize = 11.sp)
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { picked = if (checked) picked - app.pkg else picked + app.pkg },
                            colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk)
                        )
                    }
                }
            }
        }
    }
}
