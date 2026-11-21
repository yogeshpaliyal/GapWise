package com.yogeshpaliyal.gapwise.ui.home

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.data.LogResult
import com.yogeshpaliyal.gapwise.ui.common.RepeatPrompt
import com.yogeshpaliyal.gapwise.ui.common.repeatPrompt
import com.yogeshpaliyal.gapwise.domain.MilestoneInfo
import com.yogeshpaliyal.gapwise.domain.Rhythm
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortMode(val key: String, private val labelRes: Int, private val shortRes: Int) {
    DUE("due", R.string.sort_due, R.string.sort_due_short),
    RECENT("recent", R.string.sort_recent, R.string.sort_recent_short),
    NAME("name", R.string.sort_name, R.string.sort_name_short),
    CUSTOM("custom", R.string.sort_custom, R.string.sort_custom_short);

    val label: String get() = L10n.s(labelRes)
    val shortLabel: String get() = L10n.s(shortRes)

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: DUE
    }
}

data class EventUi(val item: EventWithLogs, val rhythm: Rhythm) {
    val event get() = item.event
    val ongoing get() = item.ongoing != null
    val isMilestone get() = item.event.isMilestone

    /** Milestones only: elapsed time and what's coming up, as of [now]. */
    fun milestone(now: Long): MilestoneInfo? = item.milestoneAt?.let { TimeText.milestone(it, now) }
}

data class Filters(
    val query: String = "",
    val category: String? = null,
    val sort: SortMode = SortMode.DUE,
    val grid: Boolean = true,
    val showHidden: Boolean = false,
    /** Show only milestones. */
    val milestones: Boolean = false,
)

data class HomeState(
    val loading: Boolean = true,
    val events: List<EventUi> = emptyList(),
    val categories: List<String> = emptyList(),
    val filters: Filters = Filters(),
    val hiddenCount: Int = 0,
    val milestoneCount: Int = 0,
    val totalCount: Int = 0,
    val now: Long = System.currentTimeMillis(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.app.repository
    private val prefs = application.app.prefs

    private val filters = MutableStateFlow(Filters(sort = SortMode.of(prefs.sort), grid = prefs.gridView))

    /** Re-evaluates "x days ago" text every minute while the screen is open. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    private val _logged = MutableSharedFlow<Pair<String, LogResult>>(extraBufferCapacity = 4)
    val logged: SharedFlow<Pair<String, LogResult>> = _logged

    val state: StateFlow<HomeState> = combine(repo.observeAll(), filters, ticker) { items, f, now ->
        val all = items.map { EventUi(it, RhythmCalculator.compute(it)) }
        val q = f.query.trim()
        // If the last milestone goes, the filter goes with it rather than leaving home empty.
        val onlyMilestones = f.milestones && all.any { it.isMilestone }
        val visible = all
            .filter { f.showHidden || !it.event.hidden }
            .filter { f.category == null || it.event.category == f.category }
            .filter { !onlyMilestones || it.isMilestone }
            .filter { q.isEmpty() || it.event.name.contains(q, ignoreCase = true) }
        HomeState(
            loading = false,
            events = sort(visible, f.sort, now),
            categories = all.map { it.event.category }.distinct().sorted(),
            filters = f.copy(milestones = onlyMilestones),
            hiddenCount = all.count { it.event.hidden },
            milestoneCount = all.count { it.isMilestone && (f.showHidden || !it.event.hidden) },
            totalCount = all.size,
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun sort(list: List<EventUi>, mode: SortMode, now: Long): List<EventUi> {
        val ordered = when (mode) {
            SortMode.DUE -> list.sortedWith(
                compareBy<EventUi> { it.event.paused }
                    .thenBy { if (it.ongoing) 0 else 1 }
                    .thenBy { it.rhythm.nextExpectedAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.rhythm.lastAt ?: 0L },
            )
            SortMode.RECENT -> list.sortedByDescending { it.rhythm.lastAt ?: 0L }
            SortMode.NAME -> list.sortedBy { it.event.name.lowercase() }
            SortMode.CUSTOM -> list.sortedBy { it.event.sortOrder }
        }
        // Pinned always float to the top, keeping their relative order.
        return ordered.sortedByDescending { it.event.pinned }
    }

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun setCategory(c: String?) = filters.update { it.copy(category = c, milestones = false) }
    fun showMilestones() = filters.update { it.copy(category = null, milestones = !it.milestones) }
    fun toggleHidden() = filters.update { it.copy(showHidden = !it.showHidden) }

    fun setSort(s: SortMode) {
        prefs.sort = s.key
        filters.update { it.copy(sort = s) }
    }

    fun toggleGrid() {
        filters.update { it.copy(grid = !it.grid) }
        prefs.gridView = filters.value.grid
    }

    /** Set when a tap lands within a minute of the last one; the screen asks before logging. */
    val confirm = MutableStateFlow<RepeatPrompt?>(null)

    fun log(ui: EventUi) = viewModelScope.launch {
        if (ui.isMilestone) return@launch
        // Check fresh data: the list on screen may be a moment old.
        val prompt = repo.get(ui.event.id)?.repeatPrompt()
        if (prompt != null) {
            confirm.value = prompt
            return@launch
        }
        repo.logNow(ui.event.id)?.let { _logged.emit(ui.event.name to it) }
    }

    fun confirmRepeat() = viewModelScope.launch {
        val p = confirm.value ?: return@launch
        confirm.value = null
        repo.logNow(p.eventId)?.let { _logged.emit(p.name to it) }
    }

    fun cancelRepeat() {
        confirm.value = null
    }

    fun undo(result: LogResult) = viewModelScope.launch { repo.undo(result) }
}
