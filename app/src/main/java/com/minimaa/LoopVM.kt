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
    fun addJournal(text: String, mood: Int) = viewModelScope.launch { dao.upsertJ(Journal(text = text, mood = mood)) }
    fun addTask(title: String, type: String) = viewModelScope.launch { dao.upsertT(Task(title = title, type = type)) }
    fun toggle(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(done = !t.done)) }
    fun trash(j: Journal) = viewModelScope.launch { dao.upsertJ(j.copy(deleted = now())) }
    fun trash(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(deleted = now())) }
    fun restore(j: Journal) = viewModelScope.launch { dao.upsertJ(j.copy(deleted = 0)) }
    fun restore(t: Task) = viewModelScope.launch { dao.upsertT(t.copy(deleted = 0)) }
    fun emptyBin() = viewModelScope.launch { dao.purgeJ(); dao.purgeT() }
}
