package com.yogeshpaliyal.gapwise.ui.welcome

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.yogeshpaliyal.gapwise.data.Suggestion
import com.yogeshpaliyal.gapwise.data.Suggestions
import kotlinx.coroutines.launch

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.app.repository
    val selected = mutableStateListOf<Suggestion>()
    var saving by mutableStateOf(false)
        private set

    fun toggle(s: Suggestion) {
        if (s in selected) selected.remove(s) else selected.add(s)
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        saving = true
        if (selected.isNotEmpty()) repo.addSuggestions(selected.toList())
        onDone()
    }
}

@Composable
fun WelcomeScreen(onDone: () -> Unit, vm: WelcomeViewModel = viewModel()) {
    var filter by remember { mutableStateOf<String?>(null) }
    val categories = remember { Suggestions.all.map { it.category }.distinct() }
    val shown = Suggestions.all.filter { filter == null || it.category == filter }

    Surface(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Remember the last time.\nKnow the next.", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick a few things you do now and then. Gapwise learns how often you usually do them. No streaks, no guilt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") }) }
            items(categories) { c ->
                FilterChip(selected = filter == c, onClick = { filter = c }, label = { Text(c) })
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
                val on = s in vm.selected
                Surface(
                    onClick = { vm.toggle(s) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji, fontSize = 22.sp)
                        Spacer(Modifier.padding(4.dp))
                        Text(s.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "We'll set an approximate “last time” for each. Fix it later from the event's page.",
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
            TextButton(onClick = { vm.finish(onDone) }, enabled = !vm.saving) { Text("Skip") }
            Button(onClick = { vm.finish(onDone) }, enabled = vm.selected.isNotEmpty() && !vm.saving) {
                Text(if (vm.selected.isEmpty()) "Pick some" else "Add ${vm.selected.size}")
            }
        }
    }
}
}
