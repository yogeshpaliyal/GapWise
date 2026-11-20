package com.yogeshpaliyal.gapwise.ui.settings

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
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
            Toast.makeText(context, if (ok) L10n.s(R.string.exported) else L10n.s(R.string.export_failed), Toast.LENGTH_SHORT).show()
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
            val msg = result.fold({ L10n.p(R.plurals.imported, it) }, { L10n.s(R.string.import_bad) })
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L10n.s(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, L10n.s(R.string.back)) } },
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
                    Text(L10n.s(R.string.nudges_toggle), style = MaterialTheme.typography.titleMedium)
                    Text(
                        L10n.s(R.string.nudges_settings_hint),
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
            Item(L10n.s(R.string.suggested_events), L10n.s(R.string.suggested_events_hint)) { onOpenTour() }
            Item(L10n.s(R.string.export_file), L10n.s(R.string.export_file_hint)) {
                exporter.launch("gapwise-${LocalDate.now()}.json")
            }
            Item(L10n.s(R.string.import_file), L10n.s(R.string.import_file_hint)) {
                importer.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
            HorizontalDivider()
            Item(L10n.s(R.string.privacy), L10n.s(R.string.privacy_body)) {}
            HorizontalDivider()
            com.yogeshpaliyal.gapwise.update.UpdateSettings(BuildConfigInfo.versionName(context))
        }
    }
}

@Composable
private fun WidgetSection() {
    val context = LocalContext.current
    val supported = remember { PinWidget.isSupported(context) }
    fun add(kind: PinWidget.Kind) {
        if (!PinWidget.request(context, kind)) {
            Toast.makeText(context, L10n.s(R.string.widget_pin_refused), Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(L10n.s(R.string.widgets_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                if (supported) L10n.s(R.string.widgets_body)
                else L10n.s(R.string.widgets_unsupported),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (supported) {
            WidgetCard(
                icon = Icons.AutoMirrored.Rounded.ViewList,
                title = L10n.s(R.string.sort_due),
                body = L10n.s(R.string.widget_list_body),
            ) { add(PinWidget.Kind.DueSoon) }
            WidgetCard(
                icon = Icons.Rounded.GridView,
                title = L10n.s(R.string.widget_tiles_title),
                body = L10n.s(R.string.widget_tiles_body),
            ) { add(PinWidget.Kind.Tiles) }
        }
    }
}

@Composable
private fun WidgetCard(icon: ImageVector, title: String, body: String, onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onAdd) { Text(L10n.s(R.string.add)) }
        }
    }
}

@Composable
private fun ShapeLegend() {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(L10n.s(R.string.shapes_title), style = MaterialTheme.typography.titleMedium)
        Text(
            L10n.s(R.string.shapes_body),
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
