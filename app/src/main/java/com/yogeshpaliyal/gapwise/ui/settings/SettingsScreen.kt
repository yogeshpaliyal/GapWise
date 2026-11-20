package com.yogeshpaliyal.gapwise.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yogeshpaliyal.gapwise.BuildConfigInfo
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.notify.Nudges
import com.yogeshpaliyal.gapwise.widget.PinWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenTour: () -> Unit) {
    val context = LocalContext.current
    val repo = context.app.repository
    val prefs = context.app.prefs
    val scope = rememberCoroutineScope()
    var nudges by remember { mutableStateOf(prefs.nudgesOn) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("empty")
                repo.importJson(text)
            }
            val msg = result.fold({ "Imported $it events" }, { "That file doesn't look like a GapWise backup" })
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().clickable {
                    nudges = !nudges; prefs.nudgesOn = nudges; Nudges.schedule(context)
                }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Gentle nudges", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A quiet notification when something is usually due. You can turn them off per event too.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = nudges, onCheckedChange = {
                    nudges = it; prefs.nudgesOn = it; Nudges.schedule(context)
                })
            }
            HorizontalDivider()
            DriveSection()
            HorizontalDivider()
            WidgetSection()
            HorizontalDivider()
            ShapeLegend()
            HorizontalDivider()
            Item("Suggested events", "Browse over 40 starter ideas") { onOpenTour() }
            Item("Export to a file", "Save all events and history as a JSON file") {
                exporter.launch("gapwise-${LocalDate.now()}.json")
            }
            Item("Import from a file", "Add events from a GapWise JSON file. Nothing is overwritten.") {
                importer.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
            HorizontalDivider()
            Item("Privacy", "No account, no ads, no tracking. Your data stays on this phone unless you back it up to your own Google Drive.") {}
            HorizontalDivider()
            com.yogeshpaliyal.gapwise.update.UpdateSettings(BuildConfigInfo.versionName(context))
        }
    }
}

@Composable
private fun WidgetSection() {
    val context = LocalContext.current
    val supported = remember { PinWidget.isSupported(context) }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Home screen widgets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            if (supported) "Add one straight from here, then pick which events it shows."
            else "Your launcher can't add widgets from inside an app. Long-press an empty spot on your home screen, open Widgets and look for GapWise.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (supported) {
            fun add(kind: PinWidget.Kind) {
                if (!PinWidget.request(context, kind)) {
                    Toast.makeText(context, "Your launcher didn't allow adding the widget", Toast.LENGTH_SHORT).show()
                }
            }
            Item("Add the Due soon widget", "A list of what's coming up. Tap a row to log it.") { add(PinWidget.Kind.DueSoon) }
            Item("Add the Event tiles widget", "A grid of tiles, from 1 to 12. Tap a tile to log it.") { add(PinWidget.Kind.Tiles) }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShapeLegend() {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Reading the shapes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Each event's icon changes shape as time passes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        listOf(GapStatus.FRESH, GapStatus.BUILDING, GapStatus.DUE, GapStatus.PAST, GapStatus.LEARNING, GapStatus.PAUSED).forEach { st ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                StatusBadge("", st, container = MaterialTheme.colorScheme.primaryContainer, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(st.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Item(title: String, body: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
