package com.minimaa

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val Dim = Color(0xFF8A8A8A)
val Card = Color(0xFF0C0C0C)
val Line = Color(0xFF222222)

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

@Composable
fun DotText(text: String, modifier: Modifier = Modifier) {
    val safe = text.filter { it in GLYPHS }
    if (safe.isEmpty()) return
    val cols = (safe.sumOf { if (it == ':') 1 else 5 } + (safe.length - 1)).coerceAtLeast(1)
    Canvas(modifier.aspectRatio(cols / 7f)) {
        val step = size.width / cols
        var x = 0
        safe.forEach { c ->
            val g = GLYPHS[c] ?: return@forEach
            g.forEachIndexed { r, row ->
                row.forEachIndexed { cc, ch ->
                    if (ch == '1') drawCircle(Color.White, step * 0.32f, Offset((x + cc + 0.5f) * step, (r + 0.5f) * step))
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
                    color = if (lit) Color.White else Color(0xFF1F1F1F),
                    radius = radius,
                    center = Offset((c + 0.5f) * stepX, (r + 0.5f) * stepY)
                )
            }
        }
    }
}

private fun batteryLevel(ctx: Context): Int {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
}

private fun hasNetwork(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

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
    onSaveSidebarPkgs: (Set<String>) -> Unit
) {
    val ctx = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var sidebarOpen by remember { mutableStateOf(false) }
    var showSidebarSetup by remember { mutableStateOf(false) }
    val notifs by SystemBridge.notifs.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                HomePage(pinned = pinned, go = go, launch = launch)
            } else {
                MorningDashboardPage(
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

@Composable
private fun HomePage(pinned: List<AppInfo>, go: (String) -> Unit, launch: (String) -> Unit) {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf(LocalTime.now()) }
    var battery by remember { mutableIntStateOf(batteryLevel(ctx)) }
    var online by remember { mutableStateOf(hasNetwork(ctx)) }
    val media by SystemBridge.media.collectAsStateWithLifecycle()

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy < -30) go("drawer") } },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))
        // Top status pills: signal + battery
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
                    tint = if (online) Color.White else Dim,
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
                Text("$battery%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(
                    Icons.Default.BatteryStd,
                    contentDescription = "Battery",
                    tint = Dim,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        DotText(time.format(DateTimeFormatter.ofPattern("HH:mm")), Modifier.fillMaxWidth(0.76f))
        Spacer(Modifier.height(24.dp))

        // Calendar Week Card
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Line, RoundedCornerShape(28.dp))
                .background(Card)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "•  ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()}",
                color = Color.White,
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in -3..3) {
                    val d = today.plusDays(i.toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            color = if (i == 0) Color.White else Dim,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .then(if (i == 0) Modifier.background(Color.White) else Modifier.border(1.dp, Line, CircleShape)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${d.dayOfMonth}",
                                color = if (i == 0) Color.Black else Dim,
                                fontSize = 13.sp,
                                fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2-column widget row: Media Player + Current Year Days Left
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Media Player Card
            Column(
                Modifier
                    .weight(1f)
                    .height(144.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Line, RoundedCornerShape(24.dp))
                    .background(Card)
                    .clickable {
                        media.pkg?.let { launch(it) } ?: go("pomo")
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        media.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        media.artist,
                        color = Dim,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    DotEqualizer(
                        isPlaying = media.isPlaying,
                        modifier = Modifier
                            .width(68.dp)
                            .height(34.dp)
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { SystemBridge.togglePlayPause(ctx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Current Year Days Left Card
            Box(
                Modifier
                    .weight(1f)
                    .height(144.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Line, RoundedCornerShape(24.dp))
                    .background(Card)
                    .padding(10.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("CURRENT YEAR", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    DotText(daysLeft.toString(), Modifier.height(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("DAYS LEFT", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Pinned Apps 2x3 Grid
        val displayedPins = pinned.take(6)
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
                            .clickable { launch(a.pkg) },
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
                            Text(
                                a.label.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light
                            )
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
                color = Color.White,
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

        // JOURNAL Section
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "JOURNAL",
                color = Dim,
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { goLoop(0, true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Journal", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { goLoop(0, false) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open Journal", tint = Dim, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            latestJournal?.text ?: "Jot down a quick thought...",
            color = if (latestJournal != null) Color.White else Color(0xFF555555),
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

        // TASKS Section
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TASKS",
                color = Dim,
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { goLoop(1, true) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White, modifier = Modifier.size(18.dp))
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
                    color = Color.White,
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

        // NOTIFICATIONS Section
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
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "Choose which apps show up in your quick-view sidebar.",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable { onOpenSetup() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("SET UP", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Dim,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { dismissedSetup = true }
                )
            }
        } else if (filteredNotifs.isEmpty()) {
            Text(
                "No recent notifications.",
                color = Color(0xFF555555),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )
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
                            Text(n.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onClose() }
    ) {
        Column(
            Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Line)
                .clickable(enabled = false) {}
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("QUICK VIEW", color = Color.White, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
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
                            Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (count > 0) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("$count", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101010),
        title = { Text("QUICK-VIEW SIDEBAR", color = Color.White, fontSize = 14.sp, letterSpacing = 2.sp) },
        text = {
            Column(Modifier.heightIn(max = 380.dp)) {
                if (!notifEnabled) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable {
                                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Notification Access", color = Color.White, fontSize = 12.sp)
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
                                .clickable {
                                    picked = if (checked) picked - app.pkg else picked + app.pkg
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    picked = if (checked) picked - app.pkg else picked + app.pkg
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black)
                            )
                            Text(app.label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked) }) { Text("SAVE", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Dim) }
        }
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
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val letterIndexMap = remember(shown) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        var last = ' '
        shown.forEach { a ->
            val ch = a.label.firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it else '#' } ?: '#'
            if (ch != last) {
                last = ch
                map[ch] = idx
                idx++
            }
            idx++
        }
        map
    }

    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 8.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
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
                    cursorBrush = SolidColor(Color.White),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (q.isEmpty()) Text("Search apps...", color = Dim, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            IconButton(onClick = { go("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Dim)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5 circular quick-tool icons (9213.png): NOTES, MATH, CAL, POMO, BODY
        Row(
            Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tools = listOf(
                Triple("NOTES", Icons.Default.Description, "loop"),
                Triple("MATH", Icons.Default.Calculate, "calc"),
                Triple("CAL", Icons.Default.CalendarMonth, "calendar"),
                Triple("POMO", Icons.Default.Timer, "pomo"),
                Triple("BODY", Icons.Default.FavoriteBorder, "body")
            )
            tools.forEach { (label, icon, route) ->
                Column(
                    Modifier.clickable { go(route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Line, CircleShape)
                            .background(Card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(label, color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
                var last = ' '
                shown.forEach { a ->
                    val ch = a.label.firstOrNull()?.uppercaseChar()?.let { if (it.isLetter()) it else '#' } ?: '#'
                    if (ch != last) {
                        last = ch
                        item(key = "h$ch") {
                            Text("$ch", color = Dim, fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                        }
                    }
                    item(key = a.pkg) {
                        Text(
                            a.label + if (a.pkg in pins) "  •" else "",
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { launch(a.pkg) }, onLongClick = { togglePin(a.pkg) })
                                .padding(vertical = 11.dp)
                        )
                    }
                }
            }

            // Right-edge A-Z Alphabet Scroller (9213.png)
            if (q.isEmpty()) {
                val letters = listOf('#') + ('A'..'Z').toList()
                Column(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 8.dp),
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
                                    letterIndexMap[ch]?.let { targetIdx ->
                                        scope.launch { listState.scrollToItem(targetIdx) }
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
