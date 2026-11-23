package com.minimaa

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallpaper
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private fun num(d: Double) = if (d % 1.0 == 0.0 && abs(d) < 1e15) d.toLong().toString() else d.toString()

@Composable
fun CalcScreen(onBack: () -> Unit) {
    var disp by remember { mutableStateOf("0") }
    var acc by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf<String?>(null) }
    var fresh by remember { mutableStateOf(true) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }

    fun apply(a: Double, b: Double, o: String) = when (o) {
        "+" -> a + b; "-" -> a - b; "*" -> a * b; else -> if (b == 0.0) Double.NaN else a / b
    }

    fun press(k: String) {
        when (k) {
            "AC" -> { disp = "0"; acc = null; op = null; fresh = true }
            "DEL" -> disp = if (disp.length > 1) disp.dropLast(1) else "0"
            "+/-" -> disp = if (disp.startsWith("-")) disp.drop(1) else if (disp != "0") "-$disp" else disp
            "%" -> disp = num((disp.toDoubleOrNull() ?: 0.0) / 100)
            "+", "-", "*", "/" -> {
                val cur = disp.toDoubleOrNull() ?: 0.0
                acc = if (op != null && !fresh) apply(acc ?: 0.0, cur, op!!) else cur
                disp = num(acc!!); op = k; fresh = true
            }
            "=" -> if (op != null) {
                val b = disp.toDoubleOrNull() ?: 0.0
                val res = num(apply(acc ?: 0.0, b, op!!))
                history = listOf("${num(acc ?: 0.0)} $op ${num(b)} = $res") + history.take(19)
                disp = res; acc = null; op = null; fresh = true
            }
            else -> if (fresh) { disp = if (k == ".") "0." else k; fresh = false } else if (k != "." || !disp.contains(".")) disp += k
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("CALCULATOR", onBack) {
            IconButton({ showHistory = true }) {
                Icon(Icons.Default.History, contentDescription = "History", tint = Dim)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(disp, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(10.dp))
        listOf(
            listOf("AC", "+/-", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "DEL", "=")
        ).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { k ->
                    val isOp = k in setOf("/", "*", "-", "+", "=")
                    Box(
                        Modifier
                            .weight(1f)
                            .height(70.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, if (isOp) Color(0xFF333333) else Line, RoundedCornerShape(14.dp))
                            .background(Card)
                            .clickable { press(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            k,
                            color = if (isOp) Color.White else Dim,
                            fontSize = if (k == "DEL") 14.sp else 20.sp,
                            fontWeight = if (isOp) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            containerColor = Color(0xFF101010),
            title = { Text("HISTORY", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                if (history.isEmpty()) Text("No calculations yet.", color = Dim, fontSize = 13.sp)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(8).forEach { Text(it, color = Color.White, fontSize = 14.sp) }
                }
            },
            confirmButton = { TextButton({ showHistory = false }) { Text("CLOSE", color = Color.White) } },
            dismissButton = {
                if (history.isNotEmpty()) TextButton({ history = emptyList() }) { Text("CLEAR", color = Dim) }
            }
        )
    }
}

@Composable
fun PomoScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var workMins by remember { mutableIntStateOf(prefs.getInt("pomo_work", 25)) }
    var breakMins by remember { mutableIntStateOf(prefs.getInt("pomo_break", 5)) }
    var left by remember { mutableIntStateOf(workMins * 60) }
    var running by remember { mutableStateOf(false) }
    var work by remember { mutableStateOf(true) }
    var sessions by remember { mutableIntStateOf(prefs.getInt("pomo_sessions", 0)) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000); left--
            if (left <= 0) {
                if (work) {
                    sessions++
                    prefs.edit().putInt("pomo_sessions", sessions).apply()
                }
                work = !work
                left = (if (work) workMins else breakMins) * 60
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("POMODORO", onBack) {
            IconButton({ showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Pomodoro Settings", tint = Dim)
            }
        }
        Spacer(Modifier.weight(1f))

        // Circular Timer Icon Badge (9217.png)
        Box(
            Modifier
                .size(104.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF333333), CircleShape)
                .background(Card),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }

        Spacer(Modifier.height(22.dp))

        // 4 session progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 4) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (i < (sessions % 4)) Color.White else Color(0xFF2B2B2B))
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(if (work) "WORK" else "BREAK", color = Dim, fontSize = 12.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "%02d:%02d".format(left / 60, left % 60),
            color = if (running) Color.White else Color(0xFF888888),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(28.dp))

        // Controls row: Reset | Circular Play/Pause | Skip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            IconButton({ running = false; work = true; left = workMins * 60 }) {
                Icon(Icons.Default.Replay, contentDescription = "Reset", tint = Dim, modifier = Modifier.size(24.dp))
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { running = !running },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Start/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton({ work = !work; left = (if (work) workMins else breakMins) * 60 }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = Dim, modifier = Modifier.size(26.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("TODAY: $sessions SESSIONS", color = Dim, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = Color(0xFF101010),
            title = { Text("POMODORO SETTINGS", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Work duration", color = Color.White, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45).forEach { m ->
                                Text(
                                    "${m}m",
                                    color = if (workMins == m) Color.Black else Dim,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (workMins == m) Color.White else Card)
                                        .clickable {
                                            workMins = m
                                            prefs.edit().putInt("pomo_work", m).apply()
                                            if (work && !running) left = m * 60
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Break duration", color = Color.White, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { m ->
                                Text(
                                    "${m}m",
                                    color = if (breakMins == m) Color.Black else Dim,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (breakMins == m) Color.White else Card)
                                        .clickable {
                                            breakMins = m
                                            prefs.edit().putInt("pomo_break", m).apply()
                                            if (!work && !running) left = m * 60
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showSettings = false }) { Text("DONE", color = Color.White) } }
        )
    }
}

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    onSaveTask: (Task?, String, String, String, String, Long) -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddSheet by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val dayTasks = remember(tasks, selectedDate) {
        tasks.filter { t ->
            if (t.scheduledAt == 0L) selectedDate == LocalDate.now()
            else Instant.ofEpochMilli(t.scheduledAt).atZone(zone).toLocalDate() == selectedDate
        }
    }

    val firstDay = currentMonth.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value - 1) // Monday = 0 .. Sunday = 6
    val daysInMonth = currentMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("CALENDAR", onBack) {
                IconButton(onOpenSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) }
            }

            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                // Month Header + < > Arrows
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text("${currentMonth.year}", color = Dim, fontSize = 12.sp, letterSpacing = 2.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton({ currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Dim)
                        }
                        IconButton({ currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, null, tint = Dim)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // M T W T F S S header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Text(d, color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Month Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in 0 until 7) {
                                val cellIdx = r * 7 + c
                                val dayNum = cellIdx - startOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val date = currentMonth.atDay(dayNum)
                                    val isSel = date == selectedDate
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .then(
                                                if (isSel) Modifier.background(Color.White)
                                                else Modifier.border(1.dp, Line, RoundedCornerShape(10.dp)).background(Card)
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            color = if (isSel) Color.Black else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Spacer(Modifier.height(16.dp))

                // Selected Day Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${selectedDate.dayOfMonth}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${dayTasks.size} TASKS", color = Dim, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = "Add Task",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp).clickable { showAddSheet = true }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (dayTasks.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No tasks for this day.", color = Color(0xFF555555), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dayTasks, key = { it.id }) { t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                                    .background(Card)
                                    .clickable { onToggleTask(t) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.title, color = if (t.done) Dim else Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Text(t.type, color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom-right white circular + FAB
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { showAddSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(26.dp))
        }
    }

    if (showAddSheet) {
        val defaultMillis = selectedDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        IntentionSheet(
            existing = null,
            defaultScheduledAt = defaultMillis,
            onDismiss = { showAddSheet = false },
            onSave = { title, type, cat, prio, sched ->
                onSaveTask(null, title, type, cat, prio, if (sched == 0L) defaultMillis else sched)
                showAddSheet = false
            }
        )
    }
}

@Composable
fun BodyScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var steps by remember { mutableIntStateOf(prefs.getInt("body_steps", 0)) }
    var stepGoal by remember { mutableIntStateOf(prefs.getInt("body_step_goal", 10000)) }
    var mindfulnessMins by remember { mutableIntStateOf(prefs.getInt("body_mind", 0)) }
    var sleepHours by remember { mutableFloatStateOf(prefs.getFloat("body_sleep", 0f)) }
    var mindRunning by remember { mutableStateOf(false) }
    var mindSeconds by remember { mutableIntStateOf(0) }
    var showStepGoalDialog by remember { mutableStateOf(false) }

    // Live hardware step counter listener if present on device
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        var base = -1f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values.firstOrNull() ?: return
                if (base < 0f) base = total
                val delta = (total - base).toInt().coerceAtLeast(0)
                if (delta > 0) {
                    steps += delta
                    base = total
                    prefs.edit().putInt("body_steps", steps).apply()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }

    LaunchedEffect(mindRunning) {
        while (mindRunning) {
            delay(1000)
            mindSeconds++
            if (mindSeconds >= 60) {
                mindSeconds = 0
                mindfulnessMins++
                prefs.edit().putInt("body_mind", mindfulnessMins).apply()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("BODY - BETA", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // STEPS Card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .background(Card)
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsWalk, null, tint = Dim, modifier = Modifier.size(16.dp))
                        Text("STEPS", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                    Row(
                        Modifier.clickable {
                            steps += 500
                            prefs.edit().putInt("body_steps", steps).apply()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("+500", color = Dim, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Icon(Icons.Default.TrendingUp, null, tint = Dim, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$steps", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .clickable { showStepGoalDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("OF $stepGoal", color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Icon(Icons.Default.Edit, null, tint = Dim, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1F1F1F))) {
                    val pct = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth(pct).height(2.dp).background(Color.White))
                }
            }

            // MINDFULNESS Card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .background(Card)
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SelfImprovement, null, tint = Dim, modifier = Modifier.size(16.dp))
                        Text("MINDFULNESS", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                    Row(
                        Modifier.clickable { mindRunning = !mindRunning },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(if (mindRunning) "PAUSE" else "START", color = if (mindRunning) Color.White else Dim, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Icon(Icons.Default.Timer, null, tint = if (mindRunning) Color.White else Dim, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$mindfulnessMins", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (mindRunning) "LIVE ${mindSeconds}S" else "MINUTES",
                            color = Dim,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1F1F1F))) {
                    val pct = (mindfulnessMins / 20f).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth(pct).height(2.dp).background(Color.White))
                }
            }

            // SLEEP Card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .background(Card)
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bedtime, null, tint = Dim, modifier = Modifier.size(16.dp))
                        Text("SLEEP", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                    }
                    Row(
                        Modifier.clickable {
                            sleepHours = (sleepHours + 0.5f).coerceAtMost(24f)
                            prefs.edit().putFloat("body_sleep", sleepHours).apply()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("+0.5h", color = Dim, fontSize = 11.sp, letterSpacing = 1.sp)
                        Icon(Icons.Default.AddCircleOutline, null, tint = Dim, modifier = Modifier.size(15.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(String.format(Locale.US, "%.1f", sleepHours), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .clickable {
                                sleepHours = 0f
                                prefs.edit().putFloat("body_sleep", 0f).apply()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("HOURS RESTED", color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Icon(Icons.Default.Edit, null, tint = Dim, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF1F1F1F))) {
                    val pct = (sleepHours / 8f).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth(pct).height(2.dp).background(Color.White))
                }
            }

            // INSIGHT Card
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .background(Card)
                    .padding(18.dp)
            ) {
                Text("INSIGHT", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Goal: $stepGoal steps. Tap the target above to change it. Physical rhythm is the foundation of your digital clarity. Daily calibration helps maintain accuracy.",
                    color = Dim,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showStepGoalDialog) {
        AlertDialog(
            onDismissRequest = { showStepGoalDialog = false },
            containerColor = Color(0xFF101010),
            title = { Text("STEP GOAL", color = Color.White, fontSize = 13.sp, letterSpacing = 2.sp) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5000, 8000, 10000, 12000).forEach { g ->
                        Text(
                            "${g / 1000}k",
                            color = if (stepGoal == g) Color.Black else Dim,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (stepGoal == g) Color.White else Card)
                                .clickable {
                                    stepGoal = g
                                    prefs.edit().putInt("body_step_goal", g).apply()
                                    showStepGoalDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton({ showStepGoalDialog = false }) { Text("DONE", color = Color.White) } }
        )
    }
}
