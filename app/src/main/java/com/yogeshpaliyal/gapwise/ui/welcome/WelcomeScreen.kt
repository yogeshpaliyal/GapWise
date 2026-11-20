package com.yogeshpaliyal.gapwise.ui.welcome

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.heightIn
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.PickedSuggestion
import com.yogeshpaliyal.gapwise.data.Suggestion
import com.yogeshpaliyal.gapwise.data.Suggestions
import kotlinx.coroutines.launch

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.app.repository
    /** Picked suggestions, in the order they were picked, with when they were last done. */
    val selected = mutableStateMapOf<Suggestion, LastTime>()
    private val order = mutableStateListOf<Suggestion>()
    var saving by mutableStateOf(false)
        private set

    fun pick(s: Suggestion, last: LastTime) {
        if (s !in selected) order.add(s)
        selected[s] = last
    }

    fun remove(s: Suggestion) {
        selected.remove(s); order.remove(s)
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        saving = true
        val chosen = order.mapNotNull { s -> selected[s]?.let { PickedSuggestion(s, it.startAt, it.endAt) } }
        if (chosen.isNotEmpty()) repo.addSuggestions(chosen)
        onDone()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit, vm: WelcomeViewModel = viewModel()) {
    var filter by remember { mutableStateOf<String?>(null) }
    var asking by remember { mutableStateOf<Suggestion?>(null) }
    val categories = remember { Suggestions.all.map { it.category }.distinct() }
    val shown = Suggestions.all.filter { filter == null || it.category == filter }

    Surface(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(L10n.s(R.string.welcome_title), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                L10n.s(R.string.welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ToggleButton(checked = filter == null, onCheckedChange = { filter = null }) { Text(L10n.s(R.string.filter_all)) } }
            items(categories) { c ->
                ToggleButton(checked = filter == c, onCheckedChange = { filter = c }) { Text(c) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(shown, key = { it.name + it.category }) { s ->
                val chosen = vm.selected[s]
                val on = chosen != null
                Surface(
                    onClick = { asking = s },
                    shape = MaterialTheme.shapes.medium,
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(
                            s.emoji,
                            if (on) GapStatus.FRESH else GapStatus.NEW,
                            container = MaterialTheme.colorScheme.surfaceContainerLowest,
                            size = 40.dp,
                        )
                        Spacer(Modifier.padding(5.dp))
                        Column {
                            Text(s.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            if (chosen != null) {
                                Text(
                                    chosen.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    L10n.s(R.string.welcome_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { vm.finish(onDone) }, enabled = !vm.saving) { Text(L10n.s(R.string.skip)) }
            Button(
                onClick = { vm.finish(onDone) },
                enabled = vm.selected.isNotEmpty() && !vm.saving,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(
                    when (vm.selected.size) {
                        0 -> L10n.s(R.string.pick_a_few)
                        else -> L10n.p(R.plurals.add_n_events, vm.selected.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

    asking?.let { s ->
        LastTimeDialog(
            suggestion = s,
            current = vm.selected[s],
            onDismiss = { asking = null },
            onRemove = { vm.remove(s); asking = null },
            onPick = { vm.pick(s, it); asking = null },
        )
    }
}
