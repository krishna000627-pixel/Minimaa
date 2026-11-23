package com.minimaa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

private fun num(d: Double) = if (d % 1.0 == 0.0 && abs(d) < 1e15) d.toLong().toString() else d.toString()

@Composable
fun CalcScreen(onBack: () -> Unit) {
    var disp by remember { mutableStateOf("0") }
    var acc by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf<String?>(null) }
    var fresh by remember { mutableStateOf(true) }
    fun apply(a: Double, b: Double, o: String) = when (o) { "+" -> a + b; "-" -> a - b; "*" -> a * b; else -> if (b == 0.0) Double.NaN else a / b }
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
            "=" -> if (op != null) { disp = num(apply(acc ?: 0.0, disp.toDoubleOrNull() ?: 0.0, op!!)); acc = null; op = null; fresh = true }
            else -> if (fresh) { disp = if (k == ".") "0." else k; fresh = false } else if (k != "." || !disp.contains(".")) disp += k
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("CALCULATOR", onBack)
        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
            Text(disp, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Light, maxLines = 1)
        }
        listOf(listOf("AC", "+/-", "%", "/"), listOf("7", "8", "9", "*"), listOf("4", "5", "6", "-"), listOf("1", "2", "3", "+"), listOf("0", ".", "DEL", "=")).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { k ->
                    Box(Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { press(k) },
                        contentAlignment = Alignment.Center) { Text(k, color = Color.White, fontSize = 22.sp) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun PomoScreen(onBack: () -> Unit) {
    var left by remember { mutableIntStateOf(1500) }
    var running by remember { mutableStateOf(false) }
    var work by remember { mutableStateOf(true) }
    var sessions by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            delay(1000); left--
            if (left <= 0) { if (work) sessions++; work = !work; left = if (work) 1500 else 300 }
        }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) { TopBar("POMODORO", onBack) }
        Spacer(Modifier.weight(1f))
        Text(if (work) "WORK" else "BREAK", color = Dim, fontSize = 14.sp, letterSpacing = 6.sp)
        Text("%02d:%02d".format(left / 60, left % 60), color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("RESET", color = Dim, modifier = Modifier.clickable { running = false; work = true; left = 1500 }.padding(12.dp))
            Text(if (running) "PAUSE" else "START", color = Color.Black, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { running = !running }.padding(horizontal = 28.dp, vertical = 12.dp))
            Text("SKIP", color = Dim, modifier = Modifier.clickable { work = !work; left = if (work) 1500 else 300 }.padding(12.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("TODAY: $sessions SESSIONS", color = Dim, fontSize = 12.sp, letterSpacing = 3.sp)
        Spacer(Modifier.weight(1f))
    }
}
