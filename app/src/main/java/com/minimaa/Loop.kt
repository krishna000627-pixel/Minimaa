package com.minimaa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MOODS = listOf("INSPIRED", "HAPPY", "NEUTRAL", "SAD")
private val MOOD_ICONS = listOf(
    Icons.Default.AutoAwesome,
    Icons.Default.SentimentSatisfied,
    Icons.Default.SentimentNeutral,
    Icons.Default.SentimentDissatisfied
)
private val CATEGORIES = listOf("PERSONAL", "WORK", "IDEAS", "OTHERS")
private val PRIORITIES = listOf("LOW", "MEDIUM", "HIGH")

private fun fmt(t: Long) = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(t)).uppercase()
private fun fmtShort(t: Long) = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(t)).uppercase()
private fun fmtFull(t: Long) = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(t)).uppercase()

private fun parseBranches(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else raw.split("\n•|•\n").filter { it.isNotBlank() }

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(title, color = Dim, fontSize = 12.sp, letterSpacing = 3.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Composable
fun LoopScreen(
    vm: LoopVM,
    initialTab: Int = 0,
    initialOpenNew: Boolean = false,
    onBack: () -> Unit
) {
    val journals by vm.journals.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val binJ by vm.binJ.collectAsStateWithLifecycle()
    val binT by vm.binT.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(initialTab) } // 0 journal, 1 todo, 2 search, 3 bin
    var editingJournal by remember { mutableStateOf<Journal?>(null) }
    var showJournalEditor by remember { mutableStateOf(initialOpenNew && initialTab == 0) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showTaskSheet by remember { mutableStateOf(initialOpenNew && initialTab == 1) }
    var showSettings by remember { mutableStateOf(false) }

    var query by remember { mutableStateOf("") }
    var filterMood by remember { mutableIntStateOf(-1) } // -1 = ALL
    var filterCategory by remember { mutableStateOf("ALL CATEGORIES") }
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showJournalEditor) {
        JournalEditorScreen(
            existing = editingJournal,
            onBack = { showJournalEditor = false; editingJournal = null },
            onDelete = {
                editingJournal?.let { vm.trash(it) }
                showJournalEditor = false
                editingJournal = null
            },
            onSave = { text, mood, branches ->
                vm.saveJournal(editingJournal, text, mood, branches)
                showJournalEditor = false
                editingJournal = null
            }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = if (tab == 3) "RECYCLE BIN" else "DAILY LOOP",
            onBack = if (tab == 3) ({ tab = 0 }) else onBack
        ) {
            if (tab == 3) {
                IconButton({ vm.emptyBin() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Empty Bin", tint = Color(0xFFD93838))
                }
            } else {
                IconButton({ tab = 3 }) { Icon(Icons.Default.Delete, contentDescription = "Recycle Bin", tint = Dim) }
                if (tab == 1) {
                    IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Dim) }
                }
            }
        }

        Box(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            when (tab) {
                0 -> {
                    if (journals.isEmpty()) {
                        EmptyJournal(onBegin = { editingJournal = null; showJournalEditor = true })
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(journals, key = { it.id }) { j ->
                                val branches = parseBranches(j.branches)
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                                        .background(Card)
                                        .clickable { editingJournal = j; showJournalEditor = true }
                                        .padding(14.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${fmt(j.created)} · ${MOODS.getOrElse(j.mood) { "NEUTRAL" }}",
                                            color = Dim,
                                            fontSize = 11.sp,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton({ vm.trash(j) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = Dim, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(j.text, color = Color.White, fontSize = 16.sp)
                                    if (branches.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        branches.forEach { b ->
                                            Row(
                                                Modifier.padding(top = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.AccountTree, null, tint = Dim, modifier = Modifier.size(13.dp))
                                                Text(b, color = Dim, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> TodoTab(
                    tasks = tasks,
                    onToggle = { vm.toggle(it) },
                    onEdit = { editingTask = it; showTaskSheet = true },
                    onTrash = { vm.trash(it) }
                )

                2 -> UniversalSearchTab(
                    query = query,
                    onQueryChange = { query = it },
                    filterMood = filterMood,
                    filterCategory = filterCategory,
                    journals = journals,
                    tasks = tasks,
                    onOpenFilter = { showFilterSheet = true },
                    onEditJournal = { editingJournal = it; showJournalEditor = true },
                    onEditTask = { editingTask = it; showTaskSheet = true }
                )

                else -> RecycleBinTab(
                    binJ = binJ,
                    binT = binT,
                    onRestoreJ = { vm.restore(it) },
                    onRestoreT = { vm.restore(it) },
                    onDeleteJ = { vm.deleteForever(it) },
                    onDeleteT = { vm.deleteForever(it) }
                )
            }
        }

        // Hide bottom bar in Recycle Bin (9212.png), show in Journal/Todo/Search (9210.png shows no '+' in Search, or optional)
        if (tab != 3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    Modifier
                        .then(if (tab == 2) Modifier.widthIn(max = 260.dp) else Modifier.weight(1f))
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Line, RoundedCornerShape(50))
                        .background(Card)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navItems = listOf(
                        Triple(0, "JOURNAL", Icons.Default.Book),
                        Triple(1, "TODO", Icons.Default.Loop),
                        Triple(2, "SEARCH", Icons.Default.Search)
                    )
                    navItems.forEach { (idx, label, icon) ->
                        val selected = tab == idx
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .then(if (selected) Modifier.background(Color.White) else Modifier)
                                .clickable { tab = idx }
                                .padding(horizontal = if (selected) 16.dp else 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (selected) Color.Black else Dim,
                                modifier = Modifier.size(18.dp)
                            )
                            if (selected) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                if (tab != 2) {
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable {
                                if (tab == 0) {
                                    editingJournal = null
                                    showJournalEditor = true
                                } else {
                                    editingTask = null
                                    showTaskSheet = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        IntentionSheet(
            existing = editingTask,
            onDismiss = { showTaskSheet = false; editingTask = null },
            onSave = { title, type, category, priority, scheduledAt ->
                vm.saveTask(editingTask, title, type, category, priority, scheduledAt)
                showTaskSheet = false
                editingTask = null
            }
        )
    }

    if (showFilterSheet) {
        FilterSearchSheet(
            mood = filterMood,
            category = filterCategory,
            onDismiss = { showFilterSheet = false },
            onSelectMood = { filterMood = it },
            onSelectCategory = { filterCategory = it }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = Color(0xFF101010),
            title = { Text("DAILY LOOP SETTINGS", color = Color.White, fontSize = 14.sp, letterSpacing = 2.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total active journals: ${journals.size}", color = Dim, fontSize = 13.sp)
                    Text("Open tasks: ${tasks.count { !it.done }} · Completed: ${tasks.count { it.done }}", color = Dim, fontSize = 13.sp)
                    Text("Items in recycle bin: ${binJ.size + binT.size}", color = Dim, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("DONE", color = Color.White) }
            }
        )
    }
}

@Composable
private fun UniversalSearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    filterMood: Int,
    filterCategory: String,
    journals: List<Journal>,
    tasks: List<Task>,
    onOpenFilter: () -> Unit,
    onEditJournal: (Journal) -> Unit,
    onEditTask: (Task) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .background(Card)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Dim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("SEARCH NOTES & TASKS...", color = Dim, fontSize = 12.sp, letterSpacing = 1.sp)
                        inner()
                    }
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, if (filterMood != -1 || filterCategory != "ALL CATEGORIES") Color.White else Line, RoundedCornerShape(14.dp))
                    .background(Card)
                    .clickable { onOpenFilter() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Filter Search", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        val hasFilter = query.isNotBlank() || filterMood != -1 || filterCategory != "ALL CATEGORIES"
        if (!hasFilter) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF4A4A4A), modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text("Universal Search", color = Dim, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("Find tasks and journal entries.", color = Color(0xFF555555), fontSize = 13.sp)
            }
        } else {
            val jr = journals.filter { j ->
                (query.isBlank() || j.text.contains(query, true)) &&
                    (filterMood == -1 || j.mood == filterMood) &&
                    (filterCategory == "ALL CATEGORIES" || filterCategory == "JOURNAL")
            }
            val tr = tasks.filter { t ->
                (query.isBlank() || t.title.contains(query, true)) &&
                    filterMood == -1 &&
                    (filterCategory == "ALL CATEGORIES" || t.category.equals(filterCategory, true))
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(jr, key = { "sj${it.id}" }) { j ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                            .background(Card)
                            .clickable { onEditJournal(j) }
                            .padding(12.dp)
                    ) {
                        Text("JOURNAL · ${MOODS.getOrElse(j.mood) { "NEUTRAL" }}", color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(j.text, color = Color.White, fontSize = 15.sp)
                    }
                }
                items(tr, key = { "st${it.id}" }) { t ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                            .background(Card)
                            .clickable { onEditTask(t) }
                            .padding(12.dp)
                    ) {
                        Text("${t.type} · ${t.category.uppercase()}", color = Dim, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(t.title, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSearchSheet(
    mood: Int,
    category: String,
    onDismiss: () -> Unit,
    onSelectMood: (Int) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FILTER SEARCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(18.dp))
            Text("MOOD VIBE", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))

            val moodOptions = listOf(-1 to "ALL") + MOODS.mapIndexed { i, m -> i to m }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                moodOptions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (idx, label) ->
                            val selected = mood == idx
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (selected) Color.White else Line, RoundedCornerShape(10.dp))
                                    .background(Card)
                                    .clickable { onSelectMood(idx) },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (idx >= 0) {
                                    Icon(MOOD_ICONS[idx], null, tint = if (selected) Color.White else Dim, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(5.dp))
                                }
                                Text(label, color = if (selected) Color.White else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                        if (row.size < 3) Spacer(Modifier.weight((3 - row.size).toFloat()))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("CATEGORIES", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))

            val catOptions = listOf("ALL CATEGORIES", "JOURNAL", "WORK", "IDEAS", "OTHERS", "PERSONAL")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                catOptions.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val selected = category == cat
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (selected) Color.White else Line, RoundedCornerShape(10.dp))
                                    .background(Card)
                                    .clickable { onSelectCategory(cat) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, color = if (selected) Color.White else Dim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinTab(
    binJ: List<Journal>,
    binT: List<Task>,
    onRestoreJ: (Journal) -> Unit,
    onRestoreT: (Task) -> Unit,
    onDeleteJ: (Journal) -> Unit,
    onDeleteT: (Task) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 JOURNAL, 1 TASKS

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("JOURNAL", "TASKS").forEachIndexed { idx, label ->
                val selected = subTab == idx
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { subTab = idx }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else Dim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(64.dp)
                            .height(2.dp)
                            .background(if (selected) Color.White else Color.Transparent)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (subTab == 0) {
            if (binJ.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No deleted journal entries.", color = Dim, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(binJ, key = { "bj${it.id}" }) { j ->
                        BinCard(
                            deletedAt = j.deleted,
                            text = j.text,
                            onRestore = { onRestoreJ(j) },
                            onDelete = { onDeleteJ(j) }
                        )
                    }
                }
            }
        } else {
            if (binT.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No deleted tasks.", color = Dim, fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(binT, key = { "bt${it.id}" }) { t ->
                        BinCard(
                            deletedAt = t.deleted,
                            text = t.title,
                            onRestore = { onRestoreT(t) },
                            onDelete = { onDeleteT(t) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BinCard(deletedAt: Long, text: String, onRestore: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .background(Card)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "DELETED ${fmtShort(deletedAt)}",
                color = Dim,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRestore, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.History, contentDescription = "Restore", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Forever", tint = Color(0xFFD93838), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text, color = Dim, fontSize = 15.sp)
    }
}

@Composable
private fun EmptyJournal(onBegin: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF2B2B2B), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text("YOUR JOURNAL IS EMPTY", color = Dim, fontSize = 14.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text("Time to write something down...", color = Color(0xFF555555), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(50))
                .clickable { onBegin() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("BEGIN JOURNAL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEditorScreen(
    existing: Journal?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (String, Int, List<String>) -> Unit
) {
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var mood by remember { mutableIntStateOf(existing?.mood ?: 2) }
    var branches by remember { mutableStateOf(parseBranches(existing?.branches ?: "")) }
    var showMoodSheet by remember { mutableStateOf(false) }
    var showBranchSheet by remember { mutableStateOf(false) }
    val dateLabel = fmtFull(existing?.created ?: System.currentTimeMillis())

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("JOURNAL ENTRY", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
                Text(dateLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            IconButton(onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFB73A3A))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                cursorBrush = SolidColor(Color.White),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp, lineHeight = 26.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("What's on your mind today?", color = Color(0xFF4A4A4A), fontSize = 18.sp)
                    inner()
                }
            )
            if (branches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                branches.forEachIndexed { idx, b ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .background(Card)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, null, tint = Dim, modifier = Modifier.size(14.dp))
                        Text(b, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            "×",
                            color = Dim,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { branches = branches.toMutableList().apply { removeAt(idx) } }
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, Line, RoundedCornerShape(50))
                .background(Card)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showBranchSheet = true }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.AccountTree, contentDescription = "New Branch", tint = Dim, modifier = Modifier.size(18.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF181818))
                    .clickable { showMoodSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MOOD_ICONS.getOrElse(mood) { Icons.Default.SentimentNeutral },
                    contentDescription = "Mood",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF333333), CircleShape)
                    .clickable { if (text.isNotBlank()) onSave(text.trim(), mood, branches) else onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showMoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoodSheet = false },
            containerColor = Color(0xFF0A0A0A),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 16.dp)
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
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HOW ARE YOU FEELING?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MOOD_ICONS.forEachIndexed { idx, icon ->
                        val selected = mood == idx
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .then(if (selected) Modifier.background(Color.White) else Modifier)
                                .clickable { mood = idx; showMoodSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = MOODS[idx],
                                tint = if (selected) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBranchSheet) {
        var branchText by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showBranchSheet = false },
            containerColor = Color(0xFF0A0A0A),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 16.dp)
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
                    .padding(bottom = 24.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("NEW BRANCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.height(22.dp))
                BasicTextField(
                    value = branchText,
                    onValueChange = { branchText = it },
                    cursorBrush = SolidColor(Color.White),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 17.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    decorationBox = { inner ->
                        if (branchText.isEmpty()) Text("Connect a thought...", color = Color(0xFF555555), fontSize = 17.sp)
                        inner()
                    }
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        if (branchText.isNotBlank()) branches = branches + branchText.trim()
                        showBranchSheet = false
                    }) {
                        Text("SAVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoTab(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onTrash: (Task) -> Unit
) {
    var showCompleted by remember { mutableStateOf(false) }
    var expandToday by remember { mutableStateOf(true) }
    var expandUpcoming by remember { mutableStateOf(true) }

    val openTasks = tasks.filter { !it.done }
    val doneTasks = tasks.filter { it.done }
    val reminders = openTasks.count { it.type == "REMINDER" }
    val todos = openTasks.count { it.type == "TODO" }
    val habits = openTasks.count { it.type == "HABIT" }

    val shown = if (showCompleted) doneTasks else openTasks
    val todayEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
    }
    val todayList = shown.filter { it.scheduledAt == 0L || it.scheduledAt <= todayEnd }
    val upcomingList = shown.filter { it.scheduledAt > todayEnd }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Text("Today,", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Normal)
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Dim)) { append("You have ") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("$reminders ${if (reminders == 1) "reminder" else "reminders"}, ")
                }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("$todos ${if (todos == 1) "task" else "tasks"}, ")
                }
                withStyle(SpanStyle(color = Dim)) { append("and ") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("$habits ${if (habits == 1) "habit" else "habits"}.")
                }
            },
            fontSize = 22.sp,
            lineHeight = 30.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .background(Card)
                    .padding(4.dp)
            ) {
                listOf(false to "To do", true to "Completed").forEach { (isDone, label) ->
                    val active = showCompleted == isDone
                    Text(
                        label,
                        color = if (active) Color.Black else Dim,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(if (active) Modifier.background(Color.White) else Modifier)
                            .clickable { showCompleted = isDone }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    if (showCompleted) "No completed tasks yet." else "All tasks completed.",
                    color = Color(0xFF555555),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 48.dp)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (todayList.isNotEmpty()) {
                    item(key = "hdr_today") {
                        SectionHeader("TODAY", expandToday) { expandToday = !expandToday }
                    }
                    if (expandToday) {
                        items(todayList, key = { it.id }) { t ->
                            TaskRow(t = t, onToggle = { onToggle(t) }, onEdit = { onEdit(t) }, onTrash = { onTrash(t) })
                        }
                    }
                }
                if (upcomingList.isNotEmpty()) {
                    item(key = "hdr_upcoming") {
                        SectionHeader("UPCOMING", expandUpcoming) { expandUpcoming = !expandUpcoming }
                    }
                    if (expandUpcoming) {
                        items(upcomingList, key = { it.id }) { t ->
                            TaskRow(t = t, onToggle = { onToggle(t) }, onEdit = { onEdit(t) }, onTrash = { onTrash(t) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
private fun TaskRow(t: Task, onToggle: () -> Unit, onEdit: () -> Unit, onTrash: () -> Unit) {
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
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.5.dp, if (t.done) Color.White else Color(0xFF555555), RoundedCornerShape(6.dp))
                .then(if (t.done) Modifier.background(Color.White) else Modifier)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (t.done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            } else {
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF555555)))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                color = if (t.done) Dim else Color.White,
                fontSize = 16.sp,
                textDecoration = if (t.done) TextDecoration.LineThrough else null
            )
            if (t.category != "Personal" || t.priority != "Low" || t.scheduledAt > 0L) {
                val sched = if (t.scheduledAt > 0L) " · ${fmt(t.scheduledAt)}" else ""
                Text(
                    "${t.category.uppercase()} · ${t.priority.uppercase()}$sched",
                    color = Dim,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Dim, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onTrash, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.DragIndicator, contentDescription = "Delete", tint = Color(0xFF3A3A3A), modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionSheet(
    existing: Task?,
    defaultScheduledAt: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "TODO") }
    var category by remember { mutableStateOf(existing?.category?.uppercase() ?: "PERSONAL") }
    var priority by remember { mutableStateOf(existing?.priority?.uppercase() ?: "LOW") }
    var scheduledAt by remember { mutableLongStateOf(existing?.scheduledAt ?: defaultScheduledAt) }

    val scheduleOptions = listOf(
        0L to "SCHEDULE",
        System.currentTimeMillis() to "TODAY",
        System.currentTimeMillis() + 86_400_000L to "TOMORROW",
        System.currentTimeMillis() + 7 * 86_400_000L to "NEXT WEEK"
    )
    var schedIdx by remember {
        mutableIntStateOf(if (scheduledAt > 0L) 1 else 0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (existing == null) "NEW INTENTION" else "EDIT INTENTION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )
            }
            Spacer(Modifier.height(22.dp))
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                cursorBrush = SolidColor(Color.White),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("What needs doing?", color = Color(0xFF555555), fontSize = 18.sp)
                    inner()
                }
            )
            Spacer(Modifier.height(22.dp))
            Text("TYPE", color = Dim, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .background(Card)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf(
                    Triple("TODO", "TODO", Icons.Default.CheckBox),
                    Triple("HABIT", "HABIT", Icons.Default.Loop),
                    Triple("REMINDER", "REMINDER", Icons.Default.CalendarToday)
                )
                types.forEach { (key, label, icon) ->
                    val selected = type == key
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(Color.White) else Modifier)
                            .clickable { type = key }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (selected) Color.Black else Dim, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = if (selected) Color.Black else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectorPill(
                    label = category,
                    icon = Icons.Default.Folder,
                    modifier = Modifier.weight(1f)
                ) {
                    val idx = CATEGORIES.indexOf(category)
                    category = CATEGORIES[(idx + 1) % CATEGORIES.size]
                }
                SelectorPill(
                    label = priority,
                    icon = Icons.Default.Flag,
                    modifier = Modifier.weight(1f)
                ) {
                    val idx = PRIORITIES.indexOf(priority)
                    priority = PRIORITIES[(idx + 1) % PRIORITIES.size]
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Line, RoundedCornerShape(12.dp))
                        .clickable {
                            schedIdx = (schedIdx + 1) % scheduleOptions.size
                            scheduledAt = scheduleOptions[schedIdx].first
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, null, tint = Dim, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (defaultScheduledAt > 0L && schedIdx == 1) fmtShort(scheduledAt) else scheduleOptions[schedIdx].second,
                        color = if (schedIdx == 0) Dim else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                Box(
                    Modifier
                        .weight(0.8f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), type, category.lowercase().replaceFirstChar { it.uppercase() }, priority.lowercase().replaceFirstChar { it.uppercase() }, scheduledAt)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun SelectorPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Icon(icon, null, tint = Dim, modifier = Modifier.size(15.dp))
    }
}
