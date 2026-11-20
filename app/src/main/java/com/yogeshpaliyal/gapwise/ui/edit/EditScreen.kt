package com.yogeshpaliyal.gapwise.ui.edit

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.Categories
import com.yogeshpaliyal.gapwise.data.EventEntity
import com.yogeshpaliyal.gapwise.data.Palette
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.PastDatePickerDialog
import com.yogeshpaliyal.gapwise.ui.home.eventColor
import kotlinx.coroutines.launch

class EditViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val repo = application.app.repository
    val eventId: Long = handle.get<Long>("id") ?: 0L
    val isNew get() = eventId == 0L

    var name by mutableStateOf("")
    var emoji by mutableStateOf("✨")
    var color by mutableStateOf(Palette.colors.first())
    var category by mutableStateOf(Categories.HOME)
    var multiDay by mutableStateOf(false)
    var nudges by mutableStateOf(true)
    var firstLogAt by mutableStateOf<Long?>(null)
    /** For events with a start and an end: when the last one ended. Null means it's still going. */
    var firstEndAt by mutableStateOf<Long?>(null)
    var categories by mutableStateOf(Categories.defaults)
        private set

    private var original: EventEntity? = null

    init {
        viewModelScope.launch {
            val all = repo.getAll()
            categories = (Categories.defaults + all.map { it.event.category }).distinct()
            if (isNew) {
                color = Palette.forIndex(all.size)
            } else {
                repo.get(eventId)?.event?.let { e ->
                    original = e
                    name = e.name; emoji = e.emoji; color = e.color; category = e.category
                    multiDay = e.isMultiDay; nudges = e.nudgesEnabled
                }
            }
        }
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        val base = original
        if (base == null) {
            repo.createEvent(
                EventEntity(
                    name = trimmed, emoji = emoji.trim(), color = color, category = category,
                    isMultiDay = multiDay, nudgesEnabled = nudges,
                ),
                firstLogAt = firstLogAt,
                firstEndAt = if (multiDay) firstEndAt else null,
            )
        } else {
            repo.updateEvent(
                base.copy(
                    name = trimmed, emoji = emoji.trim(), color = color, category = category,
                    isMultiDay = multiDay, nudgesEnabled = nudges,
                ),
            )
        }
        onDone()
    }
}

private val quickEmojis = listOf(
    "✨", "💇", "🦷", "🪴", "📞", "🛏️", "🚗", "🏍️", "🛢️", "🧽", "💊", "🩺",
    "🪥", "🧺", "🏃", "📚", "💳", "🏡", "🐶", "🎂", "🧘", "☕", "🎮", "✈️",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditScreen(onDone: () -> Unit, vm: EditViewModel = viewModel()) {
    var pickDate by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "New event" else "Edit event") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.Close, "Cancel") } },
                actions = {
                    TextButton(onClick = { vm.save(onDone) }, enabled = vm.name.isNotBlank()) { Text(if (vm.isNew) "Add" else "Save") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.emoji,
                    onValueChange = { vm.emoji = it.take(8) },
                    label = { Text("Icon") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.size(width = 84.dp, height = 72.dp),
                )
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text("What do you want to track?") },
                    placeholder = { Text("e.g. Haircut") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                quickEmojis.forEach { e ->
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (vm.emoji == e) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { vm.emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 20.sp) }
                }
            }

            Label("Color")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Palette.colors.forEach { c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(eventColor(c))
                            .border(
                                width = if (vm.color == c) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { vm.color = c },
                    )
                }
            }

            Label("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                vm.categories.forEach { c ->
                    ToggleButton(checked = vm.category == c, onCheckedChange = { vm.category = c }) { Text(c) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("Or a new category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { vm.category = newCategory.trim(); newCategory = "" },
                    enabled = newCategory.isNotBlank(),
                ) { Text("Use") }
            }

            ToggleRow(
                "Has a start and an end",
                "For things that last days, like a trip, a sick day or a period.",
                vm.multiDay,
            ) { vm.multiDay = it }
            ToggleRow(
                "Gentle nudges",
                "A quiet notification around when you usually do this. Ignoring it is fine.",
                vm.nudges,
            ) { vm.nudges = it }

            if (vm.isNew) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(if (vm.multiDay) "Last time" else "Last time you did it")
                    if (vm.multiDay) SubLabel("Started")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { pickDate = true }) {
                            Text(vm.firstLogAt?.let { TimeText.date(it) } ?: "Pick a date")
                        }
                        OutlinedButton(onClick = { vm.firstLogAt = System.currentTimeMillis() }) { Text("Today") }
                        if (vm.firstLogAt != null) {
                            TextButton(onClick = { vm.firstLogAt = null; vm.firstEndAt = null }) { Text("Clear") }
                        }
                    }
                    if (vm.multiDay) {
                        SubLabel("Ended")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { pickEnd = true }, enabled = vm.firstLogAt != null) {
                                Text(vm.firstEndAt?.let { TimeText.date(it) } ?: "Pick a date")
                            }
                            ToggleButton(
                                checked = vm.firstLogAt != null && vm.firstEndAt == null,
                                onCheckedChange = { vm.firstEndAt = null },
                                enabled = vm.firstLogAt != null,
                            ) { Text("Still going") }
                        }
                        Text(
                            if (vm.firstLogAt == null) "Pick when it started first."
                            else if (vm.firstEndAt == null) "It will show as in progress until you end it."
                            else TimeText.daysBetween(vm.firstLogAt!!, vm.firstEndAt!!).plus(1).let { n ->
                                if (n == 1L) "Lasted 1 day." else "Lasted $n days."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = { vm.save(onDone) },
                enabled = vm.name.isNotBlank(),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 24.dp),
            ) { Text(if (vm.isNew) "Add event" else "Save changes", style = MaterialTheme.typography.titleMedium) }
        }
    }

    if (pickDate) {
        PastDatePickerDialog(vm.firstLogAt ?: System.currentTimeMillis(), { pickDate = false }) {
            vm.firstLogAt = it
            // Keep the end on or after the start.
            vm.firstEndAt = vm.firstEndAt?.coerceAtLeast(it)
        }
    }
    if (pickEnd) {
        PastDatePickerDialog(vm.firstEndAt ?: System.currentTimeMillis(), { pickEnd = false }) { end ->
            vm.firstEndAt = vm.firstLogAt?.let { end.coerceAtLeast(it) } ?: end
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.titleLarge)

@Composable
private fun SubLabel(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
