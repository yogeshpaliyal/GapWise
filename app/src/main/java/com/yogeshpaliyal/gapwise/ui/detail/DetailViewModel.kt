package com.yogeshpaliyal.gapwise.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventEntity
import com.yogeshpaliyal.gapwise.data.LogEntity
import com.yogeshpaliyal.gapwise.data.LogResult
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val repo = application.app.repository
    val eventId: Long = checkNotNull(handle.get<Long>("id"))

    /** Null while loading, or after the event is deleted. */
    val state: StateFlow<EventUi?> = repo.observe(eventId)
        .map { it?.let { item -> EventUi(item, RhythmCalculator.compute(item)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _logged = MutableSharedFlow<LogResult>(extraBufferCapacity = 4)
    val logged: SharedFlow<LogResult> = _logged

    fun logNow() = viewModelScope.launch { repo.logNow(eventId)?.let { _logged.emit(it) } }
    fun undo(r: LogResult) = viewModelScope.launch { repo.undo(r) }

    fun addLog(start: Long, end: Long?, note: String?) = viewModelScope.launch { repo.addLog(eventId, start, end, note) }
    fun updateLog(log: LogEntity) = viewModelScope.launch { repo.updateLog(log) }
    fun deleteLog(id: Long) = viewModelScope.launch { repo.deleteLog(id) }

    private fun edit(block: (EventEntity) -> EventEntity) = viewModelScope.launch {
        state.value?.event?.let { repo.updateEvent(block(it)) }
    }

    fun togglePin() = edit { it.copy(pinned = !it.pinned) }
    fun toggleHidden() = edit { it.copy(hidden = !it.hidden) }
    fun togglePaused() = edit { it.copy(paused = !it.paused) }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        state.value?.event?.let { repo.deleteEvent(it) }
        onDone()
    }
}
