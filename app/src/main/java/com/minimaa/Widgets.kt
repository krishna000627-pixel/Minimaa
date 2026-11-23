package com.minimaa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
                color = Ink,
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
            Text("$day $monthShort", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("//", color = Dim, fontSize = 18.sp)
            Text(dowFull, color = Dim, fontSize = 14.sp, letterSpacing = 2.sp)
        }
        "CLASSIC" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dowFull, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("$day $monthFull", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Light)
        }
        "DRAFT" -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(today.format(DateTimeFormatter.ofPattern("dd.MM.yy")), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Normal)
            Text(dowFull, color = Dim, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        "8-BIT" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$day", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(monthFull, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        "SOUL" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("•  $dowFull", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("$day $monthFull", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Normal)
        }
        "CODE" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(true, false, true, true, true).forEach { on ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Ink else Color(0xFF2B2B2B)))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(false, true, false, true, true).forEach { on ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Ink else Color(0xFF2B2B2B)))
                    }
                }
            }
            Column {
                Text("$day", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(monthShort, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            }
        }
        "MATRIX" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(monthFull, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("•  $day / ${today.lengthOfMonth()}", color = Dim, fontSize = 11.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (r in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (c in 0..9) {
                            val idx = r * 10 + c + 1
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (idx == day) Ink else Color(0xFF2A2A2A)))
                        }
                    }
                }
            }
        }
        "PULSE" -> Column(Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthFull, color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1F1F1F))) {
                Box(Modifier.fillMaxWidth((today.monthValue / 12f).coerceIn(0.1f, 1f)).height(5.dp).background(Ink))
            }
            Spacer(Modifier.height(10.dp))
            Text("DAY $day", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1F1F1F))) {
                Box(Modifier.fillMaxWidth((day.toFloat() / today.lengthOfMonth()).coerceIn(0.1f, 1f)).height(5.dp).background(Ink))
            }
        }
        "ORBIT" -> Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF222222), style = Stroke(3f))
                drawArc(Ink, startAngle = -90f, sweepAngle = 360f * day / today.lengthOfMonth(), useCenter = false, style = Stroke(3f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$day", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(monthShort, color = Dim, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        "GHOST" -> Box(contentAlignment = Alignment.Center) {
            Text(monthFull, color = Color(0xFF242424), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Text("$day", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        "ARABIC" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("• ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar")))}", color = Ink, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 3 downTo -3) {
                    val d = today.plusDays(i.toLong())
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .then(if (i == 0) Modifier.background(Ink) else Modifier.border(1.dp, Line, CircleShape)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${d.dayOfMonth}", color = if (i == 0) InvInk else Dim, fontSize = 12.sp)
                    }
                }
            }
        }
        else -> {
            // VOID (default 7-day strip)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("•  ${today.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()}", color = Ink, fontSize = 12.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in -3..3) {
                        val d = today.plusDays(i.toLong())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = if (i == 0) Ink else Dim, fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .then(if (i == 0) Modifier.background(Ink) else Modifier.border(1.dp, Line, CircleShape)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${d.dayOfMonth}", color = if (i == 0) InvInk else Dim, fontSize = 13.sp, fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal)
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
        containerColor = SheetCol,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("CALENDAR STYLE", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("STANDARD", "EXPERIMENTAL").forEachIndexed { idx, label ->
                    val active = tab == idx
                    Column(Modifier.clickable { tab = idx }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = if (active) Ink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(72.dp).height(2.dp).background(if (active) Ink else Color.Transparent))
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
                                .border(if (active) 1.5.dp else 1.dp, if (active) Ink else Line, RoundedCornerShape(20.dp))
                                .background(Card)
                                .clickable { onSelectStyle(st) }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CalendarStylePreview(style = st, today = today)
                            Spacer(Modifier.height(10.dp))
                            Text(st, color = if (active) Ink else Dim, fontSize = 10.sp, letterSpacing = 2.sp)
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
                    colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Color(0xFF333333))
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
                    Modifier.size(46.dp).clip(CircleShape).border(1.5.dp, Ink, CircleShape).clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Ink, modifier = Modifier.size(22.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(media.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media.artist, color = Dim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            "VINYL" -> {
                Box(Modifier.size(84.dp).clickable { onPlayPause() }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF1F1F1F), style = Stroke(10f))
                        drawCircle(Color(0xFF333333), radius = size.minDimension * 0.28f, style = Stroke(2f))
                    }
                    Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Ink, modifier = Modifier.size(26.dp))
                }
                Text(media.title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            "ARTWORK" -> {
                Text(media.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(media.artist, color = Dim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Ink).clickable { onPlayPause() }, contentAlignment = Alignment.Center) {
                        Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = InvInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
            else -> {
                // SOMETHING
                Column {
                    Text(media.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(media.artist, color = Dim, fontSize = 11.sp, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    DotEqualizerMini(media.isPlaying, Modifier.width(68.dp).height(34.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Ink).clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = InvInk, modifier = Modifier.size(22.dp))
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
                drawCircle(if (lit) Ink else Color(0xFF1F1F1F), radius, Offset((c + 0.5f) * stepX, (r + 0.5f) * stepY))
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
        containerColor = SheetCol,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOTHING MUSIC STYLES", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(styles) { st ->
                    val active = selectedStyle == st
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(if (active) 2.dp else 1.dp, if (active) Ink else Line, RoundedCornerShape(24.dp))
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
                        Text(st, color = if (active) Ink else Dim, fontSize = 10.sp, letterSpacing = 2.sp)
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
                    Text("Less noise,\nmore clarity.", color = Ink, fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
                "FLIP COIN" -> {
                    Text("FLIP COIN", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(coinResult, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO FLIP", color = Dim, fontSize = 9.sp)
                }
                "ROCK PAPER SCISSORS" -> {
                    Text("RPS", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(rpsResult, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO PLAY", color = Dim, fontSize = 9.sp)
                }
                "SPIN THE BOTTLE" -> {
                    Text("SPIN", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("${bottleDeg}°", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("TAP TO SPIN", color = Dim, fontSize = 9.sp)
                }
                "SNAKE GAME" -> {
                    var playing by remember { mutableStateOf(false) }
                    var dir by remember { mutableIntStateOf(0) } // 0=R,1=D,2=L,3=U
                    var snake by remember { mutableStateOf(listOf(2 to 2, 1 to 2, 0 to 2)) }
                    var food by remember { mutableStateOf(5 to 3) }
                    var score by remember { mutableIntStateOf(0) }
                    LaunchedEffect(playing) {
                        while (playing) {
                            kotlinx.coroutines.delay(260)
                            val h = snake.first()
                            val next = when (dir) {
                                0 -> ((h.first + 1) % 8) to h.second
                                1 -> h.first to ((h.second + 1) % 5)
                                2 -> ((h.first + 7) % 8) to h.second
                                else -> h.first to ((h.second + 4) % 5)
                            }
                            if (next in snake) {
                                snake = listOf(2 to 2, 1 to 2, 0 to 2); score = 0; playing = false
                            } else if (next == food) {
                                snake = listOf(next) + snake; score++
                                food = Random.nextInt(8) to Random.nextInt(5)
                            } else {
                                snake = listOf(next) + snake.dropLast(1)
                            }
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().clickable { if (!playing) playing = true else dir = (dir + 1) % 4 },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SNAKE · $score", color = Dim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Canvas(Modifier.fillMaxWidth().height(54.dp)) {
                            val cw = size.width / 8f; val ch = size.height / 5f
                            for (x in 0..7) for (y in 0..4) {
                                val on = (x to y) in snake || (x to y) == food
                                drawCircle(if (on) Ink else Color(0xFF1E1E1E), minOf(cw, ch) * 0.28f, Offset((x + 0.5f) * cw, (y + 0.5f) * ch))
                            }
                        }
                        Text(if (playing) "TAP TO TURN" else "TAP TO PLAY", color = Dim, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
                "DINO RUN" -> {
                    var playing by remember { mutableStateOf(false) }
                    var jump by remember { mutableIntStateOf(0) }
                    var obs by remember { mutableIntStateOf(7) }
                    var score by remember { mutableIntStateOf(0) }
                    LaunchedEffect(playing) {
                        while (playing) {
                            kotlinx.coroutines.delay(180)
                            obs = if (obs <= 0) { score++; 7 } else obs - 1
                            if (jump > 0) jump--
                            if (obs == 1 && jump == 0) { playing = false; score = 0; obs = 7 }
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().clickable { if (!playing) playing = true else if (jump == 0) jump = 2 },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DINO · $score", color = Dim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Canvas(Modifier.fillMaxWidth().height(54.dp)) {
                            val cw = size.width / 8f; val ch = size.height / 4f
                            val dinoY = if (jump > 0) 1 else 3
                            for (x in 0..7) for (y in 0..3) {
                                val isDino = x == 1 && y == dinoY
                                val isObs = x == obs && y == 3
                                drawCircle(if (isDino || isObs) Ink else Color(0xFF1E1E1E), minOf(cw, ch) * 0.28f, Offset((x + 0.5f) * cw, (y + 0.5f) * ch))
                            }
                        }
                        Text(if (playing) "TAP TO JUMP" else "TAP TO PLAY", color = Dim, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
                "PHOTO" -> {
                    var photoUri by remember { mutableStateOf(prefs.getString("widget_photo_uri", null)) }
                    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        if (uri != null) {
                            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            photoUri = uri.toString()
                            prefs.edit().putString("widget_photo_uri", photoUri).apply()
                        }
                    }
                    val bmp = remember(photoUri) {
                        photoUri?.let { u ->
                            runCatching {
                                val uri = Uri.parse(u)
                                val b = if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri))
                                else @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                                b.asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    val mono = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
                    Box(Modifier.fillMaxSize().clickable { picker.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (bmp != null) {
                            Image(bmp, null, colorFilter = mono, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Dim, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("TAP TO PICK", color = Ink, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            }
                        }
                    }
                }
                "CONTACT CARD" -> {
                    var contactName by remember { mutableStateOf(prefs.getString("widget_contact_name", "") ?: "") }
                    var contactPhone by remember { mutableStateOf(prefs.getString("widget_contact_phone", "") ?: "") }
                    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
                        if (uri != null) {
                            runCatching {
                                ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                    if (c.moveToFirst()) {
                                        val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                                        val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)) ?: ""
                                        var phone = ""
                                        ctx.contentResolver.query(
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                            null,
                                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                            arrayOf(id),
                                            null
                                        )?.use { pc ->
                                            if (pc.moveToFirst()) phone = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                                        }
                                        contactName = name; contactPhone = phone
                                        prefs.edit().putString("widget_contact_name", name).putString("widget_contact_phone", phone).apply()
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().clickable {
                            if (contactPhone.isNotBlank()) {
                                runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))) }
                            } else picker.launch(null)
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Ink, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(if (contactName.isNotBlank()) contactName.uppercase() else "PICK CONTACT", color = Ink, fontSize = 10.sp, letterSpacing = 1.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
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
        containerColor = SheetCol,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WIDGET", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
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
                                .border(if (active) 1.5.dp else 1.dp, if (active) Ink else Line, RoundedCornerShape(20.dp))
                                .background(Card)
                                .clickable { onSelectWidget(name) }
                                .padding(12.dp)
                        ) {
                            Icon(icon, null, tint = Dim, modifier = Modifier.align(Alignment.TopStart).size(16.dp))
                            Icon(icon, null, tint = Ink, modifier = Modifier.align(Alignment.Center).size(28.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(name, color = if (active) Ink else Dim, fontSize = 10.sp, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ---------------- CLOCK WIDGET & STYLE SHEET (9260 - 9266) ----------------

private fun numToWords(n: Int): String {
    val ones = arrayOf("ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
        "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN")
    val tens = arrayOf("", "", "TWENTY", "THIRTY", "FORTY", "FIFTY")
    return when {
        n < 20 -> ones[n]
        n % 10 == 0 -> tens[n / 10]
        else -> "${tens[n / 10]} ${ones[n % 10]}"
    }
}

private fun toRomanClock(num: Int): String {
    if (num <= 0) return "N"
    val vals = intArrayOf(50, 40, 10, 9, 5, 4, 1)
    val syms = arrayOf("L", "XL", "X", "IX", "V", "IV", "I")
    var n = num
    val sb = StringBuilder()
    for (i in vals.indices) {
        while (n >= vals[i]) { n -= vals[i]; sb.append(syms[i]) }
    }
    return sb.toString()
}

private fun ishText(h: Int, m: Int): String {
    val h12 = if (h % 12 == 0) 12 else h % 12
    val nextH = if ((h + 1) % 12 == 0) 12 else (h + 1) % 12
    return when {
        m < 5 -> "${numToWords(h12)} O'CLOCK"
        m < 20 -> "QUARTER PAST ${numToWords(h12)}"
        m < 40 -> "HALF PAST ${numToWords(h12)}"
        m < 55 -> "QUARTER TO ${numToWords(nextH)}"
        else -> "ALMOST ${numToWords(nextH)}"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockWidgetCard(
    style: String,
    time: java.time.LocalTime,
    is24Hr: Boolean,
    scale: Float,
    align: Int,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hAlign = when (align) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    Column(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalAlignment = hAlign
    ) {
        ClockPreview(style = style, time = time, is24Hr = is24Hr, compact = false, scale = scale)
    }
}

@Composable
fun ClockPreview(
    style: String,
    time: java.time.LocalTime,
    is24Hr: Boolean,
    compact: Boolean,
    scale: Float = 0.5f
) {
    val dispH = if (is24Hr) time.hour else (if (time.hour % 12 == 0) 12 else time.hour % 12)
    val hh = "%02d".format(dispH)
    val mm = "%02d".format(time.minute)
    val mult = if (compact) 0.52f else (0.75f + scale * 0.5f)

    when (style) {
        "MINIMAA" -> Text("$hh$mm", color = Ink, fontSize = (46 * mult).sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        "MONOLITH" -> Row(
            Modifier.border(1.5.dp, Ink).padding(horizontal = (10 * mult).dp, vertical = (4 * mult).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(hh, color = Ink, fontSize = (38 * mult).sp, fontWeight = FontWeight.ExtraBold)
            Text(" | ", color = Ink, fontSize = (28 * mult).sp)
            Text(mm, color = Ink, fontSize = (38 * mult).sp, fontWeight = FontWeight.ExtraBold)
        }
        "STENCIL" -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hh, color = Dim, fontSize = (42 * mult).sp, fontWeight = FontWeight.Thin)
            Spacer(Modifier.width(4.dp))
            Text(mm, color = Ink, fontSize = (28 * mult).sp, fontWeight = FontWeight.Bold)
        }
        "PIXEL" -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            "$hh$mm".forEach { ch ->
                Box(
                    Modifier.background(Ink).padding(horizontal = (6 * mult).dp, vertical = (4 * mult).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$ch", color = InvInk, fontSize = (28 * mult).sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        "ORGANIC" -> Box(
            Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF141414)).padding(horizontal = (18 * mult).dp, vertical = (8 * mult).dp)
        ) {
            Text("$hh:$mm", color = Ink, fontSize = (34 * mult).sp, fontWeight = FontWeight.Normal)
        }
        "WIRED" -> Text("$hh:$mm", color = Ink, fontSize = (38 * mult).sp, fontWeight = FontWeight.Light)
        "BOOMER", "PYTHAGORAS", "SATURN", "INVISIBLE", "AREA 51", "TENET" -> {
            val sz = (110 * mult).dp
            val hAngle = Math.toRadians(((time.hour % 12) * 30.0 + time.minute * 0.5) - 90.0)
            val mAngle = Math.toRadians((time.minute * 6.0) - 90.0)
            Canvas(Modifier.size(sz)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                when (style) {
                    "BOOMER", "TENET" -> {
                        drawCircle(Color(0xFF2B2B2B), r, c, style = Stroke(2f))
                        for (i in 0 until 12) {
                            val a = Math.toRadians(i * 30.0)
                            drawLine(
                                Dim,
                                Offset(c.x + (r * 0.85f * kotlin.math.cos(a)).toFloat(), c.y + (r * 0.85f * kotlin.math.sin(a)).toFloat()),
                                Offset(c.x + (r * 0.96f * kotlin.math.cos(a)).toFloat(), c.y + (r * 0.96f * kotlin.math.sin(a)).toFloat()),
                                1.5f
                            )
                        }
                        drawLine(Ink, c, Offset(c.x + (r * 0.5f * kotlin.math.cos(hAngle)).toFloat(), c.y + (r * 0.5f * kotlin.math.sin(hAngle)).toFloat()), 4f)
                        drawLine(Ink, c, Offset(c.x + (r * 0.78f * kotlin.math.cos(mAngle)).toFloat(), c.y + (r * 0.78f * kotlin.math.sin(mAngle)).toFloat()), 2.5f)
                    }
                    "SATURN" -> {
                        drawCircle(Color(0xFF2B2B2B), r * 0.9f, c, style = Stroke(1.5f))
                        drawCircle(Color(0xFF2B2B2B), r * 0.58f, c, style = Stroke(1.5f))
                        drawCircle(Ink, 5f, Offset(c.x + (r * 0.58f * kotlin.math.cos(hAngle)).toFloat(), c.y + (r * 0.58f * kotlin.math.sin(hAngle)).toFloat()))
                        drawCircle(Ink, 3.5f, Offset(c.x + (r * 0.9f * kotlin.math.cos(mAngle)).toFloat(), c.y + (r * 0.9f * kotlin.math.sin(mAngle)).toFloat()))
                    }
                    "AREA 51" -> {
                        drawCircle(Color(0xFF2B2B2B), r, c, style = Stroke(1.5f))
                        drawCircle(Color(0xFF222222), r * 0.65f, c, style = Stroke(1.5f))
                        drawCircle(Color(0xFF222222), r * 0.3f, c, style = Stroke(1.5f))
                        drawLine(Ink, c, Offset(c.x + (r * 0.5f * kotlin.math.cos(hAngle)).toFloat(), c.y + (r * 0.5f * kotlin.math.sin(hAngle)).toFloat()), 3f)
                        drawLine(Ink, c, Offset(c.x + (r * 0.9f * kotlin.math.cos(mAngle)).toFloat(), c.y + (r * 0.9f * kotlin.math.sin(mAngle)).toFloat()), 2f)
                    }
                    else -> {
                        drawCircle(Ink, 4f, Offset(c.x + (r * 0.55f * kotlin.math.cos(hAngle)).toFloat(), c.y + (r * 0.55f * kotlin.math.sin(hAngle)).toFloat()), style = Stroke(2f))
                        drawCircle(Ink, 3f, Offset(c.x + (r * 0.85f * kotlin.math.cos(mAngle)).toFloat(), c.y + (r * 0.85f * kotlin.math.sin(mAngle)).toFloat()))
                    }
                }
            }
        }
        "101010" -> Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            listOf(time.hour to 5, time.minute to 6).forEach { (v, bits) ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (b in (bits - 1) downTo 0) {
                        val on = ((v shr b) and 1) == 1
                        Box(Modifier.size((11 * mult).dp).clip(CircleShape).background(if (on) Ink else Color(0xFF262626)))
                    }
                }
            }
        }
        "YAPPER" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numToWords(dispH), color = Ink, fontSize = (18 * mult).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(if (time.minute == 0) "O'CLOCK" else numToWords(time.minute), color = Ink, fontSize = (18 * mult).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        "ISH" -> Text(ishText(time.hour, time.minute), color = Ink, fontSize = (17 * mult).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        "BLOCKS" -> Column(Modifier.width((120 * mult).dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF222222))) {
                Box(Modifier.fillMaxWidth((time.hour / 23f).coerceIn(0.05f, 1f)).height(8.dp).background(Ink))
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF222222))) {
                Box(Modifier.fillMaxWidth((time.minute / 59f).coerceIn(0.05f, 1f)).height(8.dp).background(Ink))
            }
        }
        "DICE" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(time.hour / 10, time.hour % 10, time.minute / 10, time.minute % 10).forEach { d ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (i in 0..3) {
                        Box(Modifier.size((6 * mult).dp).clip(CircleShape).background(if (i < (d % 5)) Ink else Color(0xFF2A2A2A)))
                    }
                }
            }
        }
        "ET TU?" -> Text("${toRomanClock(dispH)}:${toRomanClock(time.minute)}", color = Ink, fontSize = (32 * mult).sp, fontWeight = FontWeight.ExtraBold)
        "TIDE" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.width(10.dp).height((44 * mult).dp).background(Color(0xFF222222)), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight((time.hour / 23f).coerceIn(0.1f, 1f)).background(Ink))
                }
                Box(Modifier.width(10.dp).height((44 * mult).dp).background(Color(0xFF222222)), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight((time.minute / 59f).coerceIn(0.1f, 1f)).background(Ink))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("$hh $mm", color = Dim, fontSize = (11 * mult).sp)
        }
        else -> {
            DotText("$hh:$mm", if (compact) Modifier.width(86.dp) else Modifier.fillMaxWidth((0.58f + scale * 0.28f).coerceIn(0.55f, 0.9f)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStyleSheet(
    selectedStyle: String,
    clockSize: Float,
    clockAlign: Int,
    is24Hr: Boolean,
    time: java.time.LocalTime,
    onSelectStyle: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAlignChange: (Int) -> Unit,
    onToggle24Hr: () -> Unit,
    onDismiss: () -> Unit
) {
    val standard = listOf("MINIMAA", "MONOLITH", "STENCIL", "PIXEL", "ORGANIC", "WIRED")
    val analog = listOf("BOOMER", "PYTHAGORAS", "SATURN", "INVISIBLE", "AREA 51", "TENET")
    val experimental = listOf("SOMETHING", "101010", "YAPPER", "BLOCKS", "ISH", "DICE", "ET TU?", "TIDE")
    var tab by remember {
        mutableIntStateOf(
            when (selectedStyle) {
                in standard -> 0
                in analog -> 1
                else -> 2
            }
        )
    }
    val itemsList = when (tab) { 0 -> standard; 1 -> analog; else -> experimental }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetCol,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 14.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color(0xFF555555)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("CLOCK STYLE", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("STANDARD", "ANALOG", "EXPERIMENTAL").forEachIndexed { idx, label ->
                    val active = tab == idx
                    Column(Modifier.clickable { tab = idx }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = if (active) Ink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(68.dp).height(2.dp).background(if (active) Ink else Color.Transparent))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(itemsList) { st ->
                    val active = selectedStyle == st
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(if (active) 1.5.dp else 1.dp, if (active) Ink else Line, RoundedCornerShape(18.dp))
                                .background(Card)
                                .clickable { onSelectStyle(st) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ClockPreview(style = st, time = time, is24Hr = is24Hr, compact = true)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(st, color = if (active) Ink else Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SIZE", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                Slider(
                    value = clockSize,
                    onValueChange = onSizeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Color(0xFF333333))
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    listOf(Icons.Default.FormatAlignLeft, Icons.Default.FormatAlignCenter, Icons.Default.FormatAlignRight).forEachIndexed { idx, icon ->
                        Icon(
                            icon,
                            null,
                            tint = if (clockAlign == idx) Ink else Dim,
                            modifier = Modifier.size(20.dp).clickable { onAlignChange(idx) }
                        )
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Line, RoundedCornerShape(8.dp))
                        .clickable { onToggle24Hr() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(if (is24Hr) "24 HR" else "12 HR", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
            }
        }
    }
}
