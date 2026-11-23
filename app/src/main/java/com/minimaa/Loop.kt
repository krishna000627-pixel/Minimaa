package com.minimaa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MOODS = listOf("INSPIRED", "HAPPY", "NEUTRAL", "SAD")
private fun fmt(t: Long) = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(t)).uppercase()

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
        Text(title, color = Dim, fontSize = 13.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun LoopScreen(vm: LoopVM, onBack: () -> Unit) {
    val journals by vm.journals.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val binJ by vm.binJ.collectAsStateWithLifecycle()
    val binT by vm.binT.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }   // 0 journal, 1 todo, 2 search, 3 bin
    var adding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopBar("DAILY LOOP", if (tab == 3) ({ tab = 0 }) else onBack) {
            IconButton({ tab = 3 }) { Icon(Icons.Default.Delete, null, tint = Dim) }
        }
        Box(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when (tab) {
                0 -> if (journals.isEmpty()) Empty("YOUR JOURNAL IS EMPTY", "Time to write something down...")
                else LazyColumn { items(journals, key = { it.id }) { j ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${fmt(j.created)} · ${MOODS[j.mood]}", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                            Text(j.text, color = Color.White, fontSize = 18.sp)
                        }
                        IconButton({ vm.trash(j) }) { Icon(Icons.Default.Delete, null, tint = Dim) }
                    }
                } }
                1 -> {
                    val open = tasks.count { !it.done }
                    Column {
                        Text("Today, you have $open ${if (open == 1) "task" else "tasks"}.", color = Color.White, fontSize = 26.sp, modifier = Modifier.padding(vertical = 16.dp))
                        if (tasks.isEmpty()) Empty("ALL TASKS COMPLETED", "")
                        LazyColumn { items(tasks, key = { it.id }) { t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Card).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(t.done, { vm.toggle(t) })
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, color = if (t.done) Dim else Color.White, fontSize = 18.sp, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                                    Text(t.type, color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                                }
                                IconButton({ vm.trash(t) }) { Icon(Icons.Default.Delete, null, tint = Dim) }
                            }
                        } }
                    }
                }
                2 -> Column {
                    OutlinedTextField(query, { query = it }, placeholder = { Text("SEARCH NOTES & TASKS...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    val jr = if (query.isBlank()) emptyList() else journals.filter { it.text.contains(query, true) }
                    val tr = if (query.isBlank()) emptyList() else tasks.filter { it.title.contains(query, true) }
                    LazyColumn {
                        items(jr) { Text(it.text, color = Color.White, modifier = Modifier.padding(vertical = 8.dp)) }
                        items(tr) { Text(it.title, color = Color.White, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                }
                else -> Column {
                    TextButton({ vm.emptyBin() }) { Text("EMPTY BIN", color = Color(0xFFFF5252)) }
                    LazyColumn {
                        items(binJ, key = { "j${it.id}" }) { j -> BinRow(j.text) { vm.restore(j) } }
                        items(binT, key = { "t${it.id}" }) { t -> BinRow(t.title) { vm.restore(t) } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, Line, RoundedCornerShape(50)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("JOURNAL", "TODO", "SEARCH").forEachIndexed { i, l ->
                    Text(l, fontSize = 12.sp, letterSpacing = 2.sp, color = if (tab == i) Color.Black else Dim,
                        modifier = Modifier.clip(RoundedCornerShape(50)).then(if (tab == i) Modifier.background(Color.White) else Modifier)
                            .clickable { tab = i }.padding(horizontal = 14.dp, vertical = 12.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            FloatingActionButton({ adding = true }, containerColor = Color.White, contentColor = Color.Black) { Icon(Icons.Default.Add, null) }
        }
    }
    if (adding) AddDialog(isJournal = tab == 0, onDismiss = { adding = false },
        onSave = { text, pick -> if (tab == 0) vm.addJournal(text, pick) else vm.addTask(text, listOf("TODO", "HABIT", "REMINDER")[pick]); adding = false })
}

@Composable
private fun BinRow(text: String, restore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Dim, fontSize = 16.sp, modifier = Modifier.weight(1f))
        TextButton(restore) { Text("RESTORE", color = Color.White) }
    }
}

@Composable
private fun Empty(title: String, sub: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = Dim, fontSize = 16.sp, letterSpacing = 3.sp)
        if (sub.isNotEmpty()) Text(sub, color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDialog(isJournal: Boolean, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var pick by remember { mutableIntStateOf(if (isJournal) 2 else 0) }
    val options = if (isJournal) MOODS else listOf("TODO", "HABIT", "REMINDER")
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (isJournal) "NEW ENTRY" else "NEW INTENTION", letterSpacing = 2.sp) },
        text = {
            Column {
                OutlinedTextField(text, { text = it }, placeholder = { Text(if (isJournal) "What's on your mind today?" else "What needs doing?") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { i, o -> FilterChip(pick == i, { pick = i }, label = { Text(o, fontSize = 11.sp) }) }
                }
            }
        },
        confirmButton = { TextButton({ if (text.isNotBlank()) onSave(text.trim(), pick) }) { Text("SAVE") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } })
}
