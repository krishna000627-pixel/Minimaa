package com.minimaa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

private val iconCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun rememberAppIcon(pkg: String): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(pkg) {
        iconCache[pkg] ?: runCatching {
            val d = ctx.packageManager.getApplicationIcon(pkg)
            val bmp = if (d is BitmapDrawable && d.bitmap != null) {
                d.bitmap
            } else {
                val w = d.intrinsicWidth.coerceAtLeast(96)
                val h = d.intrinsicHeight.coerceAtLeast(96)
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                    val c = AndroidCanvas(b)
                    d.setBounds(0, 0, c.width, c.height)
                    d.draw(c)
                }
            }
            bmp.asImageBitmap().also { iconCache[pkg] = it }
        }.getOrNull()
    }
}

@Composable
fun AppIconGraphic(app: AppInfo, mode: String = "Icons", modifier: Modifier = Modifier) {
    val bmp = rememberAppIcon(app.pkg)
    val mono = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    when {
        mode == "Icons" && bmp != null -> {
            Image(
                bitmap = bmp,
                contentDescription = app.label,
                colorFilter = mono,
                modifier = modifier.size(34.dp).clip(CircleShape)
            )
        }
        mode == "Dots" -> {
            DotText(app.label.take(1).uppercase(), modifier = modifier.height(22.dp))
        }
        else -> {
            Text(
                app.label.take(2).uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ---------------- CALENDAR WIDGET & STYLE SHEET (9254 - 9258) ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarWidgetCard(
    style: String,
    today: LocalDate,
    onLongPress: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Line, RoundedCornerShape(28.dp))
            .background(Card)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CalendarStylePreview(style = style, today = today)
    }
}

@Composable
fun CalendarStylePreview(style: String, today: LocalDate) {
    val day = today.dayOfMonth
    val monthFull = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
    val monthShort = today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    val dowFull = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
    val dowShort = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()

    when (style) {
        "MINIMAA" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$day $monthShort", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("//", color = Dim, fontSize = 18.sp)
            Text(dowFull, color = Dim, fontSize = 14.sp, letterSpacing = 2.sp)
        }
        "CLASSIC" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dowFull, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("$day $monthFull", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
        }
        "DRAFT" -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(today.format(DateTimeFormatter.ofPattern("dd.MM.yy")), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Normal)
            Text(dowFull, color = Dim, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        "8-BIT" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$day", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(monthFull, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        "SOUL" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("•  $dowFull", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("$day $monthFull", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Normal)
        }
        "CODE" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(true, false, true, true, true).forEach { on ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Color.White else Color(0xFF2B2B2B)))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(false, true, false, true, true).forEach { on ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Color.White else Color(0xFF2B2B2B)))
                    }
                }
            }
            Column {
                Text("$day", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(monthShort, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
        "MATRIX" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(monthFull, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("•  $day / ${today.lengthOfMonth()}", color = Dim, fontSize = 11.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (r in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (c in 0..9) {
                            val idx = r * 10 + c + 1
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (idx == day) Color.White else Color(0xFF2A2A2A)))
                        }
                    }
                }
            }
        }
        "PULSE" -> Column(Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthFull, color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1F1F1F))) {
                Box(Modifier.fillMaxWidth((today.monthValue / 12f).coerceIn(0.1f, 1f)).height(5.dp).background(Color.White))
            }
            Spacer(Modifier.height(10.dp))
            Text("DAY $day", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1F1F1F))) {
                Box(Modifier.fillMaxWidth((day.toFloat() / today.lengthOfMonth()).coerceIn(0.1f, 1f)).height(5.dp).background(Color.White))
            }
        }
        "ORBIT" -> Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF222222), style = Stroke(3f))
                drawArc(Color.White, startAngle = -90f, sweepAngle = 360f * day / today.lengthOfMonth(), useCenter = false, style = Stroke(3f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$day", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(monthShort, color = Dim, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        "GHOST" -> Box(contentAlignment = Alignment.Center) {
            Text(monthFull, color = Color(0xFF242424), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Text("$day", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        "ARABIC" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("• ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar")))}", color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 3 downTo -3) {
                    val d = today.plusDays(i.toLong())
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .then(if (i == 0) Modifier.background(Color.White) else Modifier.border(1.dp, Line, CircleShape)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${d.dayOfMonth}", color = if (i == 0) Color.Black else Dim, fontSize = 12.sp)
                    }
                }
            }
        }
        else -> {
            // VOID (default 7-day strip)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("•  ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()}", color = Color.White, fontSize = 12.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in -3..3) {
                        val d = today.plusDays(i.toLong())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = if (i == 0) Color.White else Dim, fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .then(if (i == 0) Modifier.background(Color.White) else Modifier.border(1.dp, Line, CircleShape)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${d.dayOfMonth}", color = if (i == 0) Color.Black else Dim, fontSize = 13.sp, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarStyleSheet(
    selectedStyle: String,
    calSize: Float,
    onSelectStyle: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(if (selectedStyle in listOf("MINIMAA", "CLASSIC", "DRAFT", "8-BIT", "SOUL")) 0 else 1) }
    val today = LocalDate.now()
    val standardStyles = listOf("MINIMAA", "CLASSIC", "DRAFT", "8-BIT", "SOUL")
    val expStyles = listOf("VOID", "CODE", "MATRIX", "PULSE", "ORBIT", "GHOST", "ARABIC")
    val list = if (tab == 0) standardStyles else expStyles

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("CALENDAR STYLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("STANDARD", "EXPERIMENTAL").forEachIndexed { idx, label ->
                    val active = tab == idx
                    Column(Modifier.clickable { tab = idx }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = if (active) Color.White else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(72.dp).height(2.dp).background(if (active) Color.White else Color.Transparent))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.heightIn(max = 340.dp)) {
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list) { st ->
                        val active = selectedStyle == st
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color.White else Line, RoundedCornerShape(20.dp))
                                .background(Card)
                                .clickable { onSelectStyle(st) }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CalendarStylePreview(style = st, today = today)
                            Spacer(Modifier.height(10.dp))
                            Text(st, color = if (active) Color.White else Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SIZE", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                Slider(
                    value = calSize,
                    onValueChange = onSizeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0xFF333333))
                )
            }
        }
    }
}

// ---------------- MUSIC WIDGET & STYLES SHEET (9252, 9253) ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicWidgetCard(
    style: String,
    media: MediaState,
    onCardClick: () -> Unit,
    onPlayPause: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .height(144.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .background(Card)
            .combinedClickable(onClick = onCardClick, onLongClick = onLongPress)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = if (style == "MINIMAA" || style == "VINYL") Alignment.CenterHorizontally else Alignment.Start
    ) {
        when (style) {
            "MINIMAA" -> {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, Color.White, CircleShape).clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(media.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media.artist, color = Dim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            "VINYL" -> {
                Box(Modifier.size(84.dp).clickable { onPlayPause() }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF1F1F1F), style = Stroke(10f))
                        drawCircle(Color(0xFF333333), radius = size.minDimension * 0.28f, style = Stroke(2f))
                    }
                    Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text(media.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            "ARTWORK" -> {
                Text(media.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(media.artist, color = Dim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White).clickable { onPlayPause() }, contentAlignment = Alignment.Center) {
                        Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
            else -> {
                // SOMETHING
                Column {
                    Text(media.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media.artist, color = Dim, fontSize = 11.sp, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    DotEqualizerMini(media.isPlaying, Modifier.width(68.dp).height(34.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DotEqualizerMini(isPlaying: Boolean, modifier: Modifier = Modifier) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) { kotlinx.coroutines.delay(280); tick++ }
    }
    val heights = remember(tick, isPlaying) {
        if (!isPlaying) List(9) { 1 } else List(9) { idx -> 1 + ((tick * 3 + idx * 5) % 4) }
    }
    Canvas(modifier) {
        val cols = 9; val rows = 5
        val stepX = size.width / cols; val stepY = size.height / rows
        val radius = minOf(stepX, stepY) * 0.24f
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val lit = (rows - 1 - r) < heights[c]
                drawCircle(if (lit) Color.White else Color(0xFF1F1F1F), radius, Offset((c + 0.5f) * stepX, (r + 0.5f) * stepY))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStyleSheet(
    selectedStyle: String,
    media: MediaState,
    onSelectStyle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val styles = listOf("SOMETHING", "MINIMAA", "VINYL", "ARTWORK")
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOTHING MUSIC STYLES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(styles) { st ->
                    val active = selectedStyle == st
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(if (active) 2.dp else 1.dp, if (active) Color.White else Line, RoundedCornerShape(24.dp))
                        ) {
                            MusicWidgetCard(
                                style = st,
                                media = media,
                                onCardClick = { onSelectStyle(st) },
                                onPlayPause = { onSelectStyle(st) },
                                onLongPress = { onSelectStyle(st) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(st, color = if (active) Color.White else Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

// ---------------- RIGHT WIDGET & PICKER SHEET (9249, 9250, 9251) ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RightWidgetCard(
    widgetType: String,
    battery: Int,
    daysLeft: Long,
    onGo: (String) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("minimaa", Context.MODE_PRIVATE) }
    var coinResult by remember { mutableStateOf("HEADS") }
    var rpsResult by remember { mutableStateOf("ROCK") }
    var bottleDeg by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .height(144.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .background(Card)
            .combinedClickable(
                onClick = {
                    when (widgetType) {
                        "STEP COUNTER" -> onGo("body")
                        "SCREEN TIME" -> onGo("pomo")
                        "FLIP COIN" -> coinResult = if (Random.nextBoolean()) "HEADS" else "TAILS"
                        "ROCK PAPER SCISSORS" -> rpsResult = listOf("ROCK", "PAPER", "SCISSORS").random()
                        "SPIN THE BOTTLE" -> bottleDeg = (bottleDeg + 45 + Random.nextInt(180)) % 360
                    }
                },
                onLongClick = onLongPress
            )
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
            when (widgetType) {
                "BATTERY LEVEL" -> {
                    Text("BATTERY", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    DotText("$battery", Modifier.height(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("PERCENT", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                "STEP COUNTER" -> {
                    val steps = prefs.getInt("body_steps", 0)
                    Text("STEPS TODAY", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    DotText("$steps", Modifier.height(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("TAP FOR BODY", color = Dim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                "SCREEN TIME" -> {
                    val sess = prefs.getInt("pomo_sessions", 0)
                    Text("FOCUS", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    DotText("$sess", Modifier.height(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("SESSIONS", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                "QUOTE" -> {
                    Text("QUOTE", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Less noise,\nmore clarity.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
                "FLIP COIN" -> {
                    Text("FLIP COIN", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(coinResult, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO FLIP", color = Dim, fontSize = 9.sp)
                }
                "ROCK PAPER SCISSORS" -> {
                    Text("RPS", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(rpsResult, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO PLAY", color = Dim, fontSize = 9.sp)
                }
                "SPIN THE BOTTLE" -> {
                    Text("SPIN", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("${bottleDeg}°", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO SPIN", color = Dim, fontSize = 9.sp)
                }
                "SNAKE GAME", "DINO RUN" -> {
                    Text(widgetType, color = Dim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("MINI ARCADE", color = Dim, fontSize = 9.sp)
                }
                "PHOTO" -> {
                    Icon(Icons.Default.PhotoCamera, null, tint = Dim, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("PHOTO", color = Color.White, fontSize = 11.sp, letterSpacing = 2.sp)
                }
                "CONTACT CARD" -> {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("CONTACT", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                else -> {
                    // COUNTDOWN
                    Text("CURRENT YEAR", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    DotText(daysLeft.toString(), Modifier.height(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("DAYS LEFT", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    selectedWidget: String,
    onSelectWidget: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val widgets = listOf(
        "BATTERY LEVEL" to Icons.Default.BatteryStd,
        "SCREEN TIME" to Icons.Default.HourglassEmpty,
        "STEP COUNTER" to Icons.Default.DirectionsWalk,
        "PHOTO" to Icons.Default.PhotoCamera,
        "CONTACT CARD" to Icons.Default.Person,
        "COUNTDOWN" to Icons.Default.Schedule,
        "QUOTE" to Icons.Default.FormatQuote,
        "SNAKE GAME" to Icons.Default.SportsEsports,
        "DINO RUN" to Icons.Default.SportsEsports,
        "ROCK PAPER SCISSORS" to Icons.Default.BackHand,
        "SPIN THE BOTTLE" to Icons.Default.Loop,
        "FLIP COIN" to Icons.Default.Toll
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WIDGET", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(widgets) { (name, icon) ->
                    val active = selectedWidget == name
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(116.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Color.White else Line, RoundedCornerShape(20.dp))
                                .background(Card)
                                .clickable { onSelectWidget(name) }
                                .padding(12.dp)
                        ) {
                            Icon(icon, null, tint = Dim, modifier = Modifier.align(Alignment.TopStart).size(16.dp))
                            Icon(icon, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(name, color = if (active) Color.White else Dim, fontSize = 10.sp, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
