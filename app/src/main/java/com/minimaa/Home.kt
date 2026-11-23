package com.minimaa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val cols = text.sumOf { if (it == ':') 1 else 5 } + (text.length - 1)
    Canvas(modifier.aspectRatio(cols / 7f)) {
        val step = size.width / cols
        var x = 0
        text.forEach { c ->
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
fun HomeScreen(pinned: List<AppInfo>, go: (String) -> Unit, launch: (String) -> Unit) {
    var time by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { time = LocalTime.now(); delay(1000) } }
    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 31))
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp)
            .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy < -30) go("drawer") } },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        DotText(time.format(DateTimeFormatter.ofPattern("HH:mm")), Modifier.fillMaxWidth(0.8f))
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).border(1.dp, Line, RoundedCornerShape(28.dp)).background(Card).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(), color = Color.White, fontSize = 13.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in -3..3) {
                    val d = today.plusDays(i.toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), color = Dim, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .then(if (i == 0) Modifier.background(Color.White) else Modifier.border(1.dp, Line, CircleShape)),
                            contentAlignment = Alignment.Center) {
                            Text("${d.dayOfMonth}", color = if (i == 0) Color.Black else Dim, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, Line, RoundedCornerShape(24.dp)).clickable { go("pomo") }.padding(16.dp)) {
                Column { Text("FOCUS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("POMODORO", color = Dim, fontSize = 12.sp) }
            }
            Column(Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, Line, RoundedCornerShape(24.dp)),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("CURRENT YEAR", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("$daysLeft", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("DAYS LEFT", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("LOOP" to "loop", "CALC" to "calc", "POMO" to "pomo", "APPS" to "drawer").forEach { (l, s) ->
                Text(l, color = Color.White, fontSize = 12.sp, letterSpacing = 2.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, Line, RoundedCornerShape(50)).clickable { go(s) }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        pinned.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { a ->
                    Column(Modifier.width(90.dp).clickable { launch(a.pkg) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp).clip(CircleShape).border(1.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
                            Text(a.label.take(1).uppercase(), color = Color.White, fontSize = 22.sp)
                        }
                        Text(a.label.uppercase(), color = Dim, fontSize = 10.sp, letterSpacing = 1.sp, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (pinned.isEmpty()) Text("Long-press an app in the drawer to pin it here.", color = Dim, fontSize = 12.sp)
    }
}

private fun Modifier.background(Card) = this.then(Modifier.background(Card))
private fun Modifier.background(c: Color) = this.then(androidx.compose.foundation.background(c))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(apps: List<AppInfo>, pins: Set<String>, launch: (String) -> Unit, togglePin: (String) -> Unit, close: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).border(1.dp, Line, RoundedCornerShape(50)).padding(horizontal = 20.dp, vertical = 14.dp)) {
            BasicTextField(q, { q = it }, singleLine = true, cursorBrush = SolidColor(Color.White),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                decorationBox = { inner -> if (q.isEmpty()) Text("Search apps...", color = Dim, fontSize = 18.sp); inner() })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            var last = ' '
            shown.forEach { a ->
                val ch = a.label.first().uppercaseChar().let { if (it.isLetter()) it else '#' }
                if (ch != last) { last = ch; item(key = "h$ch") { Text("$ch", color = Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) } }
                item(key = a.pkg) {
                    Text(a.label + if (a.pkg in pins) "  •" else "", color = Color.White, fontSize = 22.sp,
                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { launch(a.pkg) }, onLongClick = { togglePin(a.pkg) }).padding(vertical = 12.dp))
                }
            }
        }
    }
}
