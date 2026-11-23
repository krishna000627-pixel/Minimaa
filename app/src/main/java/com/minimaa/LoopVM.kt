package com.minimaa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoopVM @Inject constructor(private val dao: LoopDao) : ViewModel() {
    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.hot() =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journals = dao.journals().hot()
    val tasks = dao.tasks().hot()
    val binJ = dao.trashedJournals().hot()
    val binT = dao.trashedTasks().hot()

    private fun now() = System.currentTimeMillis()

    fun saveJournal(existing: Journal?, text: String, mood: Int, branches: List<String>) = viewModelScope.launch {
        val joined = branches.filter { it.isNotBlank() }.joinToString("\n•|•\n")
        val item = existing?.copy(text = text, mood = mood, branches = joined)
            ?: Journal(text = text, mood = mood, branches = joined)
        dao.upsertJ(item)
    }

    fun saveTask(
        existing: Task?,
        title: String,
        type: String,
        category: String,
        priority: String,
        scheduledAt: Long
    ) = viewModelScope.launch {
        val item = existing?.copy(
            title = title,
            type = type,
            category = category,
            priority = priority,
            scheduledAt = scheduledAt
        ) ?: Task(
            title = title,
            type = type,
            category = category,
            priority = priority,
            scheduledAt = scheduledAt
        )
        dao.upsertT(item)
    }

    fun toggle(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(done = !t.done)) }
    fun trash(j: Journal) = viewModelScope.launch { dao.upsertJ(j.copy(deleted = now())) }
    fun trash(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(deleted = now())) }
    fun restore(j: Journal) = viewModelScope.launch { dao.upsertJ(j.copy(deleted = 0)) }
    fun restore(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(deleted = 0)) }
    fun deleteForever(j: Journal) = viewModelScope.launch { dao.deleteJ(j) }
    fun deleteForever(t: Task) = viewModelScope.launch { dao.deleteT(t) }
    fun emptyBin() = viewModelScope.launch { dao.purgeJ(); dao.purgeT() }
}
