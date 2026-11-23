package com.minimaa

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object ThemeState {
    var isLight by mutableStateOf(false)
    var Bg by mutableStateOf(Color.Black)
    var CardBg by mutableStateOf(Color(0xFF0C0C0C))
    var LineCol by mutableStateOf(Color(0xFF222222))
    var DimCol by mutableStateOf(Color(0xFF8A8A8A))
    var InkCol by mutableStateOf(Color.White)
    var InvInkCol by mutableStateOf(Color.Black)
    var SheetBg by mutableStateOf(Color(0xFF0A0A0A))

    fun apply(idx: Int, systemDark: Boolean) {
        val light = (idx == 3) || (idx == 0 && !systemDark)
        isLight = light
        if (light) {
            Bg = Color(0xFFF2F2F2)
            CardBg = Color(0xFFE6E6E6)
            LineCol = Color(0xFFD2D2D2)
            DimCol = Color(0xFF6E6E6E)
            InkCol = Color(0xFF111111)
            InvInkCol = Color.White
            SheetBg = Color(0xFFF2F2F2)
        } else {
            Bg = if (idx == 2) Color(0xFF141414) else Color.Black
            CardBg = if (idx == 2) Color(0xFF1C1C1C) else Color(0xFF0C0C0C)
            LineCol = if (idx == 2) Color(0xFF2C2C2C) else Color(0xFF222222)
            DimCol = Color(0xFF8A8A8A)
            InkCol = Color.White
            InvInkCol = Color.Black
            SheetBg = Color(0xFF0A0A0A)
        }
    }
}

val Dim: Color get() = ThemeState.DimCol
val Card: Color get() = ThemeState.CardBg
val Line: Color get() = ThemeState.LineCol
val Ink: Color get() = ThemeState.InkCol
val InvInk: Color get() = ThemeState.InvInkCol
val BgCol: Color get() = ThemeState.Bg
val SheetCol: Color get() = ThemeState.SheetBg

private val GLYPHS = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    ':' to listOf("0", "1", "0", "0", "0", "1", "0")
)

private fun toRoman(num: Int): String {
    if (num <= 0) return "0"
    val vals = intArrayOf(50, 40, 10, 9, 5, 4, 1)
    val syms = arrayOf("L", "XL", "X", "IX", "V", "IV", "I")
    var n = num
    val sb = StringBuilder()
    for (i in vals.indices) {
        while (n >= vals[i]) {
            n -= vals[i]
            sb.append(syms[i])
        }
    }
    return sb.toString()
}

@Composable
fun DotText(text: String, modifier: Modifier = Modifier) {
    val safe = text.filter { it in GLYPHS }
    if (safe.isEmpty()) return
    var totalCols = safe.length - 1
    for (c in safe) totalCols += if (c == ':') 1 else 5
    val cols = totalCols.coerceAtLeast(1)
    Canvas(modifier.aspectRatio(cols / 7f)) {
        val step = size.width / cols
        var x = 0
        safe.forEach { c ->
            val g = GLYPHS[c] ?: return@forEach
            g.forEachIndexed { r, row ->
                row.forEachIndexed { cc, ch ->
                    if (ch == '1') drawCircle(Ink, step * 0.32f, Offset((x + cc + 0.5f) * step, (r + 0.5f) * step))
                }
            }
            x += g[0].length + 1
        }
    }
}

@Composable
private fun DotEqualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(280)
            tick++
        }
    }
    val heights = remember(tick, isPlaying) {
        if (!isPlaying) List(9) { 1 }
        else List(9) { idx -> 1 + ((tick * 3 + idx * 5) % 4) }
    }
    Canvas(modifier) {
        val cols = 9
        val rows = 5
        val stepX = size.width / cols
        val stepY = size.height / rows
        val radius = minOf(stepX, stepY) * 0.24f
        for (c in 0 until cols) {
            val activeRows = heights[c]
            for (r in 0 until rows) {
                val fromBottom = rows - 1 - r
                val lit = fromBottom < activeRows
                drawCircle(
                    color = if (lit) Ink else Color(0xFF1F1F1F),
                    radius = radius,
                    center = Offset((c + 0.5f) * stepX, (r + 0.5f) * stepY)
                )
            }
        }
    }
}

private fun batteryLevel(ctx: Context): Int = runCatching {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
}.getOrDefault(100)

private fun hasNetwork(ctx: Context): Boolean = runCatching {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(false)

@Composable
fun HomePagerScreen(
    apps: List<AppInfo>,
    pinned: List<AppInfo>,
    sidebarPkgs: Set<String>,
    journals: List<Journal>,
    tasks: List<Task>,
    go: (String) -> Unit,
    goLoop: (tab: Int, openNew: Boolean) -> Unit,
    launch: (String) -> Unit,
    togglePin: (String) -> Unit,
    onPinsReordered: () -> Unit,
    onSaveSidebarPkgs: (Set<String>) -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    val glanceEnabled = prefs.getBoolean("glance_enabled", true)
    val rightPanelEnabled = prefs.getBoolean("right_panel_enabled", true)
    var homeStyle by remember { mutableStateOf(prefs.getString("home_style", "SOMETHING") ?: "SOMETHING") }
    var hudGrid by remember { mutableStateOf(prefs.getBoolean("hud_grid", true)) }
    val pages = buildList {
        if (glanceEnabled) add("glance")
        add("home")
        if (rightPanelEnabled) add("side")
    }
    val homeIdx = pages.indexOf("home").coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = homeIdx, pageCount = { pages.size })
    var sidebarOpen by remember { mutableStateOf(false) }
    var showSidebarSetup by remember { mutableStateOf(false) }
    val notifs by SystemBridge.notifs.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (pages[page]) {
                "home" -> HomePage(
                    homeStyle = homeStyle,
                    hudGrid = hudGrid,
                    pinned = pinned,
                    go = go,
                    apps = apps,
                    launch = launch,
                    togglePin = togglePin,
                    onPinsReordered = onPinsReordered
                )
                "glance" -> MorningDashboardPage(
                    apps = apps,
                    sidebarPkgs = sidebarPkgs,
                    notifs = notifs,
                    journals = journals,
                    tasks = tasks,
                    goLoop = goLoop,
                    onOpenSettings = { go("settings") },
                    launch = launch,
                    onOpenSetup = { showSidebarSetup = true }
                )
                else -> SideOptionsPage(
                    selectedStyle = homeStyle,
                    hudGrid = hudGrid,
                    onToggleHudGrid = {
                        hudGrid = it
                        prefs.edit().putBoolean("hud_grid", it).apply()
                    },
                    onSelectStyle = {
                        homeStyle = it
                        prefs.edit().putString("home_style", it).apply()
                    },
                    onOpenSettings = { go("settings") }
                )
            }
        }

        // Left-edge quick-view sidebar handle
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .width(12.dp)
                .height(64.dp)
                .clickable { sidebarOpen = true },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF444444))
            )
        }

        AnimatedVisibility(
            visible = sidebarOpen,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            QuickViewSidebar(
                apps = apps,
                sidebarPkgs = sidebarPkgs,
                notifs = notifs,
                onClose = { sidebarOpen = false },
                onLaunch = { pkg -> sidebarOpen = false; launch(pkg) },
                onOpenSetup = { sidebarOpen = false; showSidebarSetup = true }
            )
        }
    }

    if (showSidebarSetup) {
        SidebarSetupDialog(
            ctx = ctx,
            apps = apps,
            selected = sidebarPkgs,
            onDismiss = { showSidebarSetup = false },
            onSave = {
                onSaveSidebarPkgs(it)
                showSidebarSetup = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePage(
    homeStyle: String,
    hudGrid: Boolean,
    pinned: List<AppInfo>,
    apps: List<AppInfo>,
    go: (String) -> Unit,
    launch: (String) -> Unit,
    togglePin: (String) -> Unit,
    onPinsReordered: () -> Unit
) {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf(LocalTime.now()) }
    var battery by remember { mutableIntStateOf(batteryLevel(ctx)) }
    var online by remember { mutableStateOf(hasNetwork(ctx)) }
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    val media by SystemBridge.media.collectAsStateWithLifecycle()
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    var clockStyle by remember { mutableStateOf(prefs.getString("clock_style", "SOMETHING") ?: "SOMETHING") }
    var clockSize by remember { mutableFloatStateOf(prefs.getFloat("clock_size", 0.5f)) }
    var clockAlign by remember { mutableIntStateOf(prefs.getInt("clock_align", 1)) }
    var clock24Hr by remember { mutableStateOf(prefs.getBoolean("clock_24hr", true)) }
    var showClockSheet by remember { mutableStateOf(false) }
    var calStyle by remember { mutableStateOf(prefs.getString("cal_style", "VOID") ?: "VOID") }
    var calSize by remember { mutableFloatStateOf(prefs.getFloat("cal_size", 0.5f)) }
    var musicStyle by remember { mutableStateOf(prefs.getString("music_style", "SOMETHING") ?: "SOMETHING") }
    var rightWidget by remember { mutableStateOf(prefs.getString("right_widget", "COUNTDOWN") ?: "COUNTDOWN") }
    var iconMode by remember { mutableStateOf(prefs.getString("icon_mode", "Icons") ?: "Icons") }
    var showCalSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var showWidgetSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            battery = batteryLevel(ctx)
            online = hasNetwork(ctx)
            delay(1000)
        }
    }

    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 31))
    val displayedPins = pinned.take(6)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy < -30) go("drawer") } }
    ) {
        // HUD background grid lines (9244.png)
        if (homeStyle == "HUD" && hudGrid) {
            Canvas(Modifier.fillMaxSize()) {
                val cols = 4
                val rows = 6
                val lineColor = Color(0xFF141414)
                for (c in 1 until cols) {
                    val x = size.width * c / cols
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                }
                for (r in 1 until rows) {
                    val y = size.height * r / rows
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
                }
            }
        }

        when (homeStyle) {
            "ZEN" -> {
                // 9247.png: Orbital rings + ONE THING AT A TIME + MINDFUL / FOCUS + bottom media line
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            drawCircle(Color(0xFF2B2B2B), radius = size.minDimension * 0.34f, center = center, style = Stroke(1.5f))
                            drawCircle(Color(0xFF2B2B2B), radius = size.minDimension * 0.25f, center = center, style = Stroke(1.5f))
                            drawCircle(Ink, radius = 4f, center = Offset(center.x - size.minDimension * 0.24f, center.y - size.minDimension * 0.24f))
                            drawCircle(Ink, radius = 6f, center = Offset(center.x - size.minDimension * 0.17f, center.y - size.minDimension * 0.17f))
                            drawCircle(Color(0xFF444444), radius = 3f, center = center)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("ONE THING AT A TIME.", color = Dim, fontSize = 11.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Line, RoundedCornerShape(50))
                                .background(Card)
                                .clickable { go("body") }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SelfImprovement, null, tint = Ink, modifier = Modifier.size(16.dp))
                            Text("MINDFUL", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Line, RoundedCornerShape(50))
                                .background(Card)
                                .clickable { go("pomo") }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, tint = Ink, modifier = Modifier.size(16.dp))
                            Text("FOCUS", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MediaLine(media) { SystemBridge.togglePlayPause(ctx) }
                }
            }

            "CLASSIC", "ESSENTIAL" -> {
                // 9248.png (CLASSIC) & 9246.png (ESSENTIAL)
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(14.dp))
                    StatusPills(online, battery)
                    Spacer(Modifier.height(28.dp))
                    val romanTime = "${toRoman(time.hour)}:${toRoman(time.minute)}"
                    Text(romanTime, color = Ink, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Spacer(Modifier.height(10.dp))
                    WeekStripRow(today)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Spacer(Modifier.height(18.dp))
                    MediaLine(media) { SystemBridge.togglePlayPause(ctx) }
                    Spacer(Modifier.height(22.dp))

                    if (homeStyle == "CLASSIC") {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            displayedPins.forEach { a ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { launch(a.pkg) },
                                            onLongClick = { longPressedApp = a }
                                        )
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("•   ", color = Dim, fontSize = 16.sp)
                                    Text(a.label, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        // ESSENTIAL 2-column bordered grid (9246.png)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            displayedPins.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { a ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(104.dp)
                                                .border(1.dp, Line)
                                                .background(InvInk)
                                                .combinedClickable(
                                                    onClick = { launch(a.pkg) },
                                                    onLongClick = { longPressedApp = a }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(a.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            "HUD" -> {
                // 9244.png: HUD technical layout
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(14.dp))
                    StatusPills(online, battery)
                    Spacer(Modifier.height(22.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Line)
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF0E0E0E))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                color = Ink,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        Spacer(Modifier.height(12.dp))
                        WeekStripRow(today)
                    }
                    Spacer(Modifier.height(16.dp))
                    MediaLine(media) { SystemBridge.togglePlayPause(ctx) }
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        displayedPins.forEach { a ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { launch(a.pkg) },
                                        onLongClick = { longPressedApp = a }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(5.dp).background(Ink))
                                    Spacer(Modifier.width(12.dp))
                                    Text(a.label, color = Ink, fontSize = 18.sp)
                                }
                                Text("RUN", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                // SOMETHING (Default flagship dot-matrix layout — 9199.png / 9235.png)
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(14.dp))
                    StatusPills(online, battery)
                    Spacer(Modifier.height(22.dp))
                    ClockWidgetCard(
                        style = clockStyle,
                        time = time,
                        is24Hr = clock24Hr,
                        scale = clockSize,
                        align = clockAlign,
                        onLongPress = { showClockSheet = true }
                    )
                    Spacer(Modifier.height(24.dp))

                    CalendarWidgetCard(
                        style = calStyle,
                        today = today,
                        onLongPress = { showCalSheet = true }
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MusicWidgetCard(
                            style = musicStyle,
                            media = media,
                            onCardClick = { media.pkg?.let { launch(it) } ?: go("pomo") },
                            onPlayPause = { SystemBridge.togglePlayPause(ctx) },
                            onLongPress = { showMusicSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        RightWidgetCard(
                            widgetType = rightWidget,
                            battery = battery,
                            daysLeft = daysLeft,
                            onGo = go,
                            onLongPress = { showWidgetSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    displayedPins.chunked(3).forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { a ->
                                Column(
                                    Modifier
                                        .width(92.dp)
                                        .combinedClickable(
                                            onClick = { launch(a.pkg) },
                                            onLongClick = { longPressedApp = a }
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Line, CircleShape)
                                            .background(Card),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppIconGraphic(app = a, mode = iconMode)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        a.label.uppercase(),
                                        color = Dim,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    if (displayedPins.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Swipe up for apps · Long-press to pin", color = Dim, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    var showRearrangeSheet by remember { mutableStateOf(false) }

    longPressedApp?.let { app ->
        HomeAppOptionsSheet(
            ctx = ctx,
            app = app,
            onDismiss = { longPressedApp = null },
            onRemoveFromHome = {
                togglePin(app.pkg)
                longPressedApp = null
            },
            onRearrange = {
                longPressedApp = null
                showRearrangeSheet = true
            },
            iconMode = iconMode,
            onIconModeChange = {
                iconMode = it
                prefs.edit().putString("icon_mode", it).apply()
            },
            onOpenSettings = {
                longPressedApp = null
                go("settings")
            }
        )
    }

    if (showRearrangeSheet) {
        RearrangeFavoritesSheet(
            pinned = pinned,
            onDone = { newOrder ->
                prefs.edit().putString("pins_order", newOrder.joinToString(",")).apply()
                onPinsReordered()
                showRearrangeSheet = false
            },
            onDismiss = { showRearrangeSheet = false }
        )
    }

    if (showClockSheet) {
        ClockStyleSheet(
            selectedStyle = clockStyle,
            clockSize = clockSize,
            clockAlign = clockAlign,
            is24Hr = clock24Hr,
            time = time,
            onSelectStyle = {
                clockStyle = it
                prefs.edit().putString("clock_style", it).apply()
            },
            onSizeChange = {
                clockSize = it
                prefs.edit().putFloat("clock_size", it).apply()
            },
            onAlignChange = {
                clockAlign = it
                prefs.edit().putInt("clock_align", it).apply()
            },
            onToggle24Hr = {
                clock24Hr = !clock24Hr
                prefs.edit().putBoolean("clock_24hr", clock24Hr).apply()
            },
            onDismiss = { showClockSheet = false }
        )
    }

    if (showCalSheet) {
        CalendarStyleSheet(
            selectedStyle = calStyle,
            calSize = calSize,
            onSelectStyle = {
                calStyle = it
                prefs.edit().putString("cal_style", it).apply()
            },
            onSizeChange = {
                calSize = it
                prefs.edit().putFloat("cal_size", it).apply()
            },
            onDismiss = { showCalSheet = false }
        )
    }

    if (showMusicSheet) {
        MusicStyleSheet(
            selectedStyle = musicStyle,
            media = media,
            onSelectStyle = {
                musicStyle = it
                prefs.edit().putString("music_style", it).apply()
            },
            onDismiss = { showMusicSheet = false }
        )
    }

    if (showWidgetSheet) {
        WidgetPickerSheet(
            selectedWidget = rightWidget,
            onSelectWidget = {
                rightWidget = it
                prefs.edit().putString("right_widget", it).apply()
                showWidgetSheet = false
            },
            onDismiss = { showWidgetSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAppOptionsSheet(
    ctx: Context,
    app: AppInfo,
    onDismiss: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onRearrange: () -> Unit,
    iconMode: String,
    onIconModeChange: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedPreset by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(app.label, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Edit, null, tint = Dim, modifier = Modifier.size(16.dp))
                    }
                    Text(app.pkg, color = Dim, fontSize = 12.sp)
                }
                val hasUsage = remember { UsageHelper.hasUsageAccess(ctx) }
                val usedMins = remember(hasUsage) { if (hasUsage) (UsageHelper.getTodayUsageMinutes(ctx)[app.pkg] ?: 0) else 0 }

                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(50))
                        .clickable {
                            if (hasUsage) UsageHelper.openDigitalWellbeingAppUsage(ctx, app.pkg)
                            else runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(if (hasUsage) Icons.Default.HourglassEmpty else Icons.Default.Lock, null, tint = Dim, modifier = Modifier.size(14.dp))
                    Text(
                        if (hasUsage) UsageHelper.formatMinutes(usedMins) else "Enable Time",
                        color = Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SheetOptionRow(Icons.Default.StarBorder, "REMOVE FROM HOME", onRemoveFromHome)
            SheetOptionRow(Icons.Default.FormatListNumbered, "REARRANGE APPS", onRearrange)
            SheetOptionRow(Icons.Default.HourglassEmpty, "SCREEN TIME") {
                UsageHelper.openDigitalWellbeingAppUsage(ctx, app.pkg)
                onDismiss()
            }
            SheetOptionRow(Icons.Default.Info, "APP INFO") {
                runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.pkg, null)))
                }
                onDismiss()
            }
            SheetOptionRow(Icons.Default.Delete, "UNINSTALL APP") {
                runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.pkg, null)))
                }
                onDismiss()
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CUSTOM ICON", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Icons", "Dots", "Letters").forEach { mode ->
                        val active = iconMode == mode
                        Text(
                            mode,
                            color = if (active) Ink else Dim,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (active) Ink else Line, RoundedCornerShape(8.dp))
                                .clickable { onIconModeChange(mode) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf("▫", "◈", "💬", "🎮", "👾", "🎵", "▶")) { sym ->
                    val idx = listOf("▫", "◈", "💬", "🎮", "👾", "🎵", "▶").indexOf(sym)
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.dp, if (selectedPreset == idx) Ink else Line, CircleShape)
                            .background(Card)
                            .clickable { selectedPreset = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sym, color = Ink, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.dp, Line, CircleShape)
                .background(Card),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Dim, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Dim, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatusPills(online: Boolean, battery: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, Line, RoundedCornerShape(50))
                .background(Card)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SignalCellularAlt,
                contentDescription = "Signal",
                tint = if (online) Ink else Dim,
                modifier = Modifier.size(14.dp)
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, Line, RoundedCornerShape(50))
                .background(Card)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$battery%", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.BatteryStd, contentDescription = "Battery", tint = Dim, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun WeekStripRow(today: LocalDate, circleBorder: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in -3..3) {
            val d = today.plusDays(i.toLong())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = if (i == 0) Ink else Dim,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .then(
                            when {
                                i == 0 -> Modifier.background(Ink)
                                circleBorder -> Modifier.border(1.dp, Line, CircleShape)
                                else -> Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${d.dayOfMonth}",
                        color = if (i == 0) InvInk else Dim,
                        fontSize = 13.sp,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLine(media: MediaState, onClick: () -> Unit) {
    Row(
        Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Dim, modifier = Modifier.size(13.dp))
        Text(
            "${media.title} - ${media.artist}",
            color = Dim,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MorningDashboardPage(
    apps: List<AppInfo>,
    sidebarPkgs: Set<String>,
    notifs: List<NotifItem>,
    journals: List<Journal>,
    tasks: List<Task>,
    goLoop: (tab: Int, openNew: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    launch: (String) -> Unit,
    onOpenSetup: () -> Unit
) {
    val ctx = LocalContext.current
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "GOOD MORNING"
        hour < 17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase()
    val latestJournal = journals.firstOrNull()
    val openTasks = tasks.filter { !it.done }
    var dismissedSetup by remember { mutableStateOf(false) }
    val notifEnabled = SystemBridge.isNotificationAccessEnabled(ctx)
    val filteredNotifs = notifs.filter { sidebarPkgs.isEmpty() || it.pkg in sidebarPkgs }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                greeting,
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Dim)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Line, RoundedCornerShape(8.dp))
                .background(Card)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(todayLabel, color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(30.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("JOURNAL", color = Dim, fontSize = 14.sp, letterSpacing = 4.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { goLoop(0, true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Journal", tint = Ink, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { goLoop(0, false) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open Journal", tint = Dim, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            latestJournal?.text ?: "Jot down a quick thought...",
            color = if (latestJournal != null) Ink else Color(0xFF555555),
            fontSize = 14.sp,
            fontStyle = if (latestJournal != null) FontStyle.Normal else FontStyle.Italic,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { goLoop(0, latestJournal == null) }
                .padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(26.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("TASKS", color = Dim, fontSize = 14.sp, letterSpacing = 4.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { goLoop(1, true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Ink, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { goLoop(1, false) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open Tasks", tint = Dim, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (openTasks.isEmpty()) {
            Text(
                "Your plate is clear.",
                color = Color(0xFF555555),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { goLoop(1, true) }
                    .padding(vertical = 4.dp)
            )
        } else {
            openTasks.take(3).forEach { t ->
                Text(
                    "•  ${t.title}",
                    color = Ink,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { goLoop(1, false) }
                        .padding(vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("NOTIFICATIONS", color = Dim, fontSize = 11.sp, letterSpacing = 3.sp)
            if (notifEnabled || sidebarPkgs.isNotEmpty()) {
                Text(
                    "SET UP",
                    color = Dim,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable { onOpenSetup() }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if ((!notifEnabled || sidebarPkgs.isEmpty()) && !dismissedSetup) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Line, RoundedCornerShape(20.dp))
                    .background(Card)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsNone, null, tint = Ink, modifier = Modifier.size(18.dp))
                }
                Text(
                    "Choose which apps show up in your quick-view sidebar.",
                    color = Ink,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ink)
                        .clickable { onOpenSetup() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("SET UP", color = InvInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Dim,
                    modifier = Modifier.size(18.dp).clickable { dismissedSetup = true }
                )
            }
        } else if (filteredNotifs.isEmpty()) {
            Text("No recent notifications.", color = Color(0xFF555555), fontSize = 13.sp, fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredNotifs.take(5), key = { it.key }) { n ->
                    val appName = apps.find { it.pkg == n.pkg }?.label ?: n.pkg
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Line, RoundedCornerShape(16.dp))
                            .background(Card)
                            .clickable { launch(n.pkg) }
                            .padding(12.dp)
                    ) {
                        Text(appName.uppercase(), color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                        if (n.title.isNotEmpty()) {
                            Text(n.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (n.text.isNotEmpty()) {
                            Text(n.text, color = Dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickViewSidebar(
    apps: List<AppInfo>,
    sidebarPkgs: Set<String>,
    notifs: List<NotifItem>,
    onClose: () -> Unit,
    onLaunch: (String) -> Unit,
    onOpenSetup: () -> Unit
) {
    val chosenApps = apps.filter { it.pkg in sidebarPkgs }
    Row(
        Modifier
            .fillMaxSize()
            .background(InvInk.copy(alpha = 0.72f))
            .clickable { onClose() }
    ) {
        Column(
            Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(SheetCol)
                .border(1.dp, Line)
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("QUICK VIEW", color = Ink, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text("SET UP", color = Dim, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.clickable { onOpenSetup() })
            }
            Spacer(Modifier.height(18.dp))
            if (chosenApps.isEmpty()) {
                Text("No sidebar apps selected yet.", color = Dim, fontSize = 13.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chosenApps, key = { it.pkg }) { app ->
                        val count = notifs.count { it.pkg == app.pkg }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Line, RoundedCornerShape(14.dp))
                                .background(Card)
                                .clickable { onLaunch(app.pkg) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(app.label, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (count > 0) {
                                Box(Modifier.clip(CircleShape).background(Ink).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("$count", color = InvInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SidebarSetupDialog(
    ctx: Context,
    apps: List<AppInfo>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var picked by remember { mutableStateOf(selected) }
    val notifEnabled = SystemBridge.isNotificationAccessEnabled(ctx)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetCol,
        title = { Text("QUICK-VIEW SIDEBAR", color = Ink, fontSize = 14.sp, letterSpacing = 2.sp) },
        text = {
            Column(Modifier.heightIn(max = 380.dp)) {
                if (!notifEnabled) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Notification Access", color = Ink, fontSize = 12.sp)
                        Text("OPEN", color = Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                LazyColumn {
                    items(apps, key = { it.pkg }) { app ->
                        val checked = app.pkg in picked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { picked = if (checked) picked - app.pkg else picked + app.pkg }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { picked = if (checked) picked - app.pkg else picked + app.pkg },
                                colors = CheckboxDefaults.colors(checkedColor = Ink, checkmarkColor = InvInk)
                            )
                            Text(app.label, color = Ink, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(picked) }) { Text("SAVE", color = Ink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Dim) } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    pins: Set<String>,
    go: (String) -> Unit,
    launch: (String) -> Unit,
    togglePin: (String) -> Unit,
    close: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var hiddenPkgs by remember { mutableStateOf(prefs.getStringSet("hidden_pkgs", emptySet()).orEmpty().toSet()) }
    var privatePkgs by remember { mutableStateOf(prefs.getStringSet("private_pkgs", emptySet()).orEmpty().toSet()) }
    val showHideOption = remember { prefs.getBoolean("show_hide_option", true) }
    var longPressedDrawerApp by remember { mutableStateOf<AppInfo?>(null) }
    val degoogle = remember { prefs.getBoolean("drawer_degoogle", false) }
    val hideHeaders = remember { prefs.getBoolean("drawer_hide_headers", false) }
    val rightAlign = remember { prefs.getBoolean("drawer_right_align", false) }
    val scrollerLeft = remember { prefs.getString("drawer_scroller_pos", "RIGHT") == "LEFT" }
    val bottomSearch = remember { prefs.getBoolean("drawer_bottom_search", false) }
    val autoKb = remember { prefs.getBoolean("drawer_auto_kb", false) }
    val autoLaunch = remember { prefs.getBoolean("drawer_auto_launch", false) }
    val webSearch = remember { prefs.getBoolean("drawer_web_search", false) }
    val contactSearch = remember { prefs.getBoolean("drawer_contact_search", false) }
    val showUsage = remember { prefs.getBoolean("detox_show_usage", false) }
    val hideInactive = remember { prefs.getBoolean("detox_hide_inactive", false) }
    val showTools = remember { prefs.getBoolean("drawer_tools", true) }
    val folders = remember { prefs.getStringSet("drawer_folders", emptySet()).orEmpty().toList().sorted() }
    val privateEnabled = remember { prefs.getBoolean("private_enabled", true) }

    val usageMap = remember(showUsage) { if (showUsage) UsageHelper.getTodayUsageMinutes(ctx) else emptyMap() }
    val active7d = remember(hideInactive) { if (hideInactive) UsageHelper.getActivePackagesLast7Days(ctx) else emptySet() }

    var q by remember { mutableStateOf("") }
    var expandedFolder by remember { mutableStateOf<String?>(null) }
    var showPrivateVault by remember { mutableStateOf(false) }
    var vaultUnlocked by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoKb) runCatching { focusRequester.requestFocus() }
    }

    val shown = apps.filter {
        it.pkg !in hiddenPkgs && it.pkg !in privatePkgs &&
            (!degoogle || !it.pkg.startsWith("com.google.")) &&
            (!hideInactive || active7d.isEmpty() || it.pkg in active7d) &&
            it.label.contains(q, ignoreCase = true)
    }

    LaunchedEffect(q, shown.size) {
        if (autoLaunch && q.isNotBlank() && shown.size == 1) {
            launch(shown.first().pkg)
        }
    }

    val contactMatches = remember(q, contactSearch) {
        if (contactSearch && q.isNotBlank()) ContactHelper.searchContacts(ctx, q) else emptyList()
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letterIndexMap = remember(shown) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        var last = ' '
        shown.forEach { a ->
            val ch = a.label.firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it else '#' } ?: '#'
            if (!hideHeaders && ch != last) { last = ch; map[ch] = idx; idx++ }
            idx++
        }
        map
    }

    val searchBar: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .background(Card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Dim, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    q,
                    { q = it },
                    singleLine = true,
                    cursorBrush = SolidColor(Ink),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (q.isEmpty()) Text("Search apps...", color = Dim, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            if (privateEnabled) {
                IconButton(onClick = { showPrivateVault = true }) {
                    Icon(Icons.Default.Lock, contentDescription = "Private Space", tint = Dim)
                }
            }
            IconButton(onClick = { go("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Dim)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 8.dp)) {
        if (!bottomSearch) searchBar()

        if (showTools) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    Triple("NOTES", Icons.Default.Description, "loop"),
                    Triple("MATH", Icons.Default.Calculate, "calc"),
                    Triple("CAL", Icons.Default.CalendarMonth, "calendar"),
                    Triple("POMO", Icons.Default.Timer, "pomo"),
                    Triple("BODY", Icons.Default.FavoriteBorder, "body")
                ).forEach { (label, icon, route) ->
                    Column(Modifier.clickable { go(route) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).border(1.dp, Line, CircleShape).background(Card),
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.height(6.dp))
                        Text(label, color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
                if (q.isEmpty() && folders.isNotEmpty()) {
                    items(folders, key = { "fld_$it" }) { fname ->
                        val isOpen = expandedFolder == fname
                        val folderPkgs = prefs.getStringSet("folder_$fname", emptySet()).orEmpty()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Line, RoundedCornerShape(14.dp))
                                .background(Card)
                                .clickable { expandedFolder = if (isOpen) null else fname }
                                .padding(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Folder, null, tint = Ink, modifier = Modifier.size(18.dp))
                                    Text(fname, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("${folderPkgs.size}", color = Dim, fontSize = 11.sp)
                            }
                            if (isOpen) {
                                Spacer(Modifier.height(8.dp))
                                apps.filter { it.pkg in folderPkgs }.forEach { fa ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable { launch(fa.pkg) }.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIconGraphic(fa, "Icons", Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(fa.label, color = Ink, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                var last = ' '
                shown.forEach { a ->
                    val ch = a.label.firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it else '#' } ?: '#'
                    if (!hideHeaders && ch != last) {
                        last = ch
                        item(key = "h$ch") {
                            Text(
                                "$ch",
                                color = Dim,
                                fontSize = 11.sp,
                                textAlign = if (rightAlign) TextAlign.End else TextAlign.Start,
                                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp)
                            )
                        }
                    }
                    item(key = a.pkg) {
                        val mins = usageMap[a.pkg] ?: 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { launch(a.pkg) }, onLongClick = { longPressedDrawerApp = a })
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (rightAlign) Arrangement.End else Arrangement.Start
                        ) {
                            AppIconGraphic(app = a, mode = "Icons", modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(
                                a.label + if (a.pkg in pins) "  •" else "",
                                color = Ink,
                                fontSize = 19.sp,
                                modifier = if (!rightAlign) Modifier.weight(1f) else Modifier
                            )
                            if (showUsage && mins > 0) {
                                Text(UsageHelper.formatMinutes(mins), color = Dim, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (contactMatches.isNotEmpty()) {
                    item(key = "hdr_contacts") {
                        Text("CONTACTS", color = Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    items(contactMatches, key = { "c_${it.first}_${it.second}" }) { (name, phone) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = Ink, fontSize = 16.sp)
                            Text("CALL", color = Dim, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        }
                    }
                }

                if (webSearch && q.isNotBlank() && shown.isEmpty()) {
                    item(key = "web_search") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Line, RoundedCornerShape(14.dp))
                                .background(Card)
                                .clickable {
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))) }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = Ink, modifier = Modifier.size(18.dp))
                            Text("Search web for '$q'", color = Ink, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (q.isEmpty()) {
                val letters = listOf('#') + ('A'..'Z').toList()
                Column(
                    Modifier.align(if (scrollerLeft) Alignment.CenterStart else Alignment.CenterEnd).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    letters.forEach { ch ->
                        val active = ch in letterIndexMap
                        Text(
                            "$ch",
                            color = if (active) Dim else Color(0xFF333333),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable(enabled = active) {
                                    letterIndexMap[ch]?.let { targetIdx -> scope.launch { listState.scrollToItem(targetIdx) } }
                                }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        if (bottomSearch) searchBar()
    }

    if (showPrivateVault) {
        val savedPin = prefs.getString("private_pin", "0000") ?: "0000"
        var pinInput by remember { mutableStateOf("") }
        Box(Modifier.fillMaxSize().background(InvInk)) {
            if (!vaultUnlocked) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        IconButton({ showPrivateVault = false }) { Icon(Icons.Default.Close, null, tint = Dim) }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Ink, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("ENTER PIN", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Unlock your private space", color = Dim, fontSize = 12.sp)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (i in 0 until 4) {
                                val filled = i < pinInput.length
                                Box(
                                    Modifier.size(12.dp).clip(CircleShape)
                                        .then(if (filled) Modifier.background(Ink) else Modifier.border(1.5.dp, Ink, CircleShape))
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { d ->
                                    Box(Modifier.size(60.dp).clip(CircleShape).clickable {
                                        if (pinInput.length < 4) {
                                            pinInput += d
                                            if (pinInput == savedPin) vaultUnlocked = true
                                            else if (pinInput.length == 4) pinInput = ""
                                        }
                                    }, contentAlignment = Alignment.Center) {
                                        Text(d, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(60.dp).clip(CircleShape).clickable {
                                if (android.os.Build.VERSION.SDK_INT >= 28) {
                                    runCatching {
                                        val bp = android.hardware.biometrics.BiometricPrompt.Builder(ctx)
                                            .setTitle("Private Space")
                                            .setSubtitle("Unlock with biometrics")
                                            .setNegativeButton("Cancel", ctx.mainExecutor) { _, _ -> }
                                            .build()
                                        bp.authenticate(
                                            android.os.CancellationSignal(),
                                            ctx.mainExecutor,
                                            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                                                    vaultUnlocked = true
                                                }
                                            }
                                        )
                                    }.onFailure { vaultUnlocked = true }
                                } else {
                                    vaultUnlocked = true
                                }
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Fingerprint, null, tint = Ink, modifier = Modifier.size(26.dp))
                            }
                            Box(Modifier.size(60.dp).clip(CircleShape).clickable {
                                if (pinInput.length < 4) {
                                    pinInput += "0"
                                    if (pinInput == savedPin) vaultUnlocked = true
                                    else if (pinInput.length == 4) pinInput = ""
                                }
                            }, contentAlignment = Alignment.Center) {
                                Text("0", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.size(60.dp).clip(CircleShape).clickable {
                                if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Backspace, null, tint = Ink, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            } else {
                // 9300.png: PRIVATE SPACE unlocked view
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ showPrivateVault = false; vaultUnlocked = false }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Dim)
                        }
                        Text("PRIVATE SPACE", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                        IconButton({ showPrivateVault = false; vaultUnlocked = false; go("drawer_private") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Dim)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val vaultApps = apps.filter { it.pkg in privatePkgs }
                    if (vaultApps.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No apps in Private Space yet.\nLong-press an app in the drawer to move it here.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(vaultApps, key = { it.pkg }) { pa ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { launch(pa.pkg) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pa.label, color = Ink, fontSize = 18.sp)
                                    IconButton(onClick = {
                                        privatePkgs = privatePkgs - pa.pkg
                                        prefs.edit().putStringSet("private_pkgs", privatePkgs).apply()
                                    }) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Remove from Private Space", tint = Dim, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    longPressedDrawerApp?.let { app ->
        DrawerAppOptionsSheet(
            ctx = ctx,
            app = app,
            isPinned = app.pkg in pins,
            showHideOption = showHideOption,
            onDismiss = { longPressedDrawerApp = null },
            onToggleHome = { togglePin(app.pkg); longPressedDrawerApp = null },
            onMoveToPrivate = {
                privatePkgs = privatePkgs + app.pkg
                prefs.edit().putStringSet("private_pkgs", privatePkgs).apply()
                longPressedDrawerApp = null
            },
            onHideApp = {
                hiddenPkgs = hiddenPkgs + app.pkg
                prefs.edit().putStringSet("hidden_pkgs", hiddenPkgs).apply()
                longPressedDrawerApp = null
            }
        )
    }
}

@Composable
private fun SideOptionsPage(
    selectedStyle: String,
    hudGrid: Boolean,
    onToggleHudGrid: (Boolean) -> Unit,
    onSelectStyle: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val styles = listOf("CLASSIC", "ZEN", "ESSENTIAL", "HUD", "SOMETHING")
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text("SIDE", color = Dim, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(4.dp))
        Text("OPTIONS", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, null, tint = Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(14.dp))
            Text("MINIMAA SETTINGS", color = Ink, fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Dim, modifier = Modifier.size(18.dp))
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(22.dp))

        Text("HOME STYLE", color = Dim, fontSize = 11.sp, letterSpacing = 2.5.sp)
        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.forEach { style ->
                val active = selectedStyle == style
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (active) Modifier.border(1.dp, Color(0xFF444444), RoundedCornerShape(14.dp)).background(Card)
                            else Modifier
                        )
                        .clickable { onSelectStyle(style) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        style,
                        color = if (active) Ink else Dim,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 2.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (active && style == "HUD") {
                            Text("GRID", color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            Switch(
                                checked = hudGrid,
                                onCheckedChange = onToggleHudGrid,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = InvInk,
                                    checkedTrackColor = Ink,
                                    uncheckedThumbColor = Dim,
                                    uncheckedTrackColor = Card
                                )
                            )
                        }
                        if (active) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Ink))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(Ink))
            Text("MINIMAA BY BINGE", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerAppOptionsSheet(
    ctx: Context,
    app: AppInfo,
    isPinned: Boolean,
    showHideOption: Boolean,
    onDismiss: () -> Unit,
    onToggleHome: () -> Unit,
    onMoveToPrivate: () -> Unit,
    onHideApp: () -> Unit
) {
    val hasUsage = remember { UsageHelper.hasUsageAccess(ctx) }
    val usedMins = remember(hasUsage) { if (hasUsage) (UsageHelper.getTodayUsageMinutes(ctx)[app.pkg] ?: 0) else 0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetCol,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 26.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(app.label, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Edit, null, tint = Dim, modifier = Modifier.size(16.dp))
                    }
                    Text(app.pkg, color = Dim, fontSize = 12.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).border(1.dp, Color(0xFF444444), RoundedCornerShape(50))
                        .clickable {
                        if (hasUsage) UsageHelper.openDigitalWellbeingAppUsage(ctx, app.pkg)
                        else runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(if (hasUsage) Icons.Default.HourglassEmpty else Icons.Default.Lock, null, tint = Dim, modifier = Modifier.size(14.dp))
                    Text(if (hasUsage) UsageHelper.formatMinutes(usedMins) else "Enable Time", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(22.dp))
            SheetOptionRow(Icons.Default.StarBorder, if (isPinned) "REMOVE FROM HOME" else "ADD TO HOME", onToggleHome)
            SheetOptionRow(Icons.Default.Lock, "MOVE TO PRIVATE SPACE", onMoveToPrivate)
            if (showHideOption) SheetOptionRow(Icons.Default.VisibilityOff, "Hide app", onHideApp)
            SheetOptionRow(Icons.Default.HourglassEmpty, "SCREEN TIME") {
                UsageHelper.openDigitalWellbeingAppUsage(ctx, app.pkg)
                onDismiss()
            }
            SheetOptionRow(Icons.Default.Info, "APP INFO") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.pkg, null))) }
                onDismiss()
            }
            SheetOptionRow(Icons.Default.Delete, "UNINSTALL APP") {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.pkg, null))) }
                onDismiss()
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RearrangeFavoritesSheet(
    pinned: List<AppInfo>,
    onDone: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var list by remember(pinned) { mutableStateOf(pinned) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetCol,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555))) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 26.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("REARRANGE FAVORITES", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list.size, key = { list[it].pkg }) { idx ->
                    val app = list[idx]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Line, RoundedCornerShape(16.dp))
                            .background(Card)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).border(1.dp, Color(0xFF444444), CircleShape))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(app.label.uppercase(), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                        if (idx > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = Dim, modifier = Modifier.size(20.dp).clickable {
                                val m = list.toMutableList()
                                val item = m.removeAt(idx)
                                m.add(idx - 1, item)
                                list = m
                            })
                        }
                        if (idx < list.lastIndex) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Dim, modifier = Modifier.size(20.dp).clickable {
                                val m = list.toMutableList()
                                val item = m.removeAt(idx)
                                m.add(idx + 1, item)
                                list = m
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Ink)
                    .clickable { onDone(list.map { it.pkg }) },
                contentAlignment = Alignment.Center
            ) {
                Text("DONE", color = InvInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}
