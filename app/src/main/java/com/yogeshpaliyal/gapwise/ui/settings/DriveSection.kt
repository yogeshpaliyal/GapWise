package com.yogeshpaliyal.gapwise.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.api.ApiException
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.backup.DriveBackup
import com.yogeshpaliyal.gapwise.backup.DriveBackupWorker
import com.yogeshpaliyal.gapwise.domain.TimeText
import kotlinx.coroutines.launch
import java.io.IOException

private enum class DriveAction { BACKUP, RESTORE, AUTO_ON }

/** Turns failures into something a person can act on. */
private fun friendly(e: Throwable): String = when {
    e is ApiException && e.statusCode == 10 ->
        "Google Drive isn't set up for this build of the app yet. See \"Google Drive backup\" in the README."
    e is ApiException && e.statusCode == 7 -> "Couldn't reach Google. Check your connection and try again."
    e is DriveBackup.DriveException && e.code == 403 ->
        "Google Drive refused the request. The Drive API may not be enabled for this app's Google Cloud project."
    e is DriveBackup.DriveException && e.code == 401 -> "Drive access expired. Try again to sign in."
    e is IOException -> "Couldn't reach Google Drive. Check your connection and try again."
    e is kotlinx.serialization.SerializationException || e is IllegalArgumentException ->
        "The backup in Drive couldn't be read, so nothing was changed."
    else -> e.message ?: "Something went wrong with Google Drive."
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DriveSection() {
    val context = LocalContext.current
    val prefs = context.app.prefs
    val repo = context.app.repository
    val scope = rememberCoroutineScope()

    var last by remember { mutableLongStateOf(prefs.lastDriveBackupAt) }
    var auto by remember { mutableStateOf(prefs.driveAutoBackup) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<DriveAction?>(null) }
    var restoreOffer by remember { mutableStateOf<Pair<String, DriveBackup.RemoteBackup>?>(null) }

    suspend fun guarded(label: String, block: suspend () -> Unit) {
        busy = label
        message = null
        try {
            block()
        } catch (e: Exception) {
            message = friendly(e)
        } finally {
            busy = null
        }
    }

    suspend fun run(action: DriveAction, token: String) {
        when (action) {
            DriveAction.BACKUP -> guarded("Backing up…") {
                val remote = DriveBackup.upload(token, repo.exportJson())
                prefs.lastDriveBackupAt = remote.modifiedAt
                last = remote.modifiedAt
                message = "Backed up to Google Drive."
            }
            DriveAction.RESTORE -> guarded("Looking for your backup…") {
                val remote = DriveBackup.latest(token)
                if (remote == null) message = "There's no Gapwise backup in this Google account yet."
                else restoreOffer = token to remote
            }
            DriveAction.AUTO_ON -> {
                prefs.driveAutoBackup = true
                auto = true
                DriveBackupWorker.schedule(context)
                message = "Gapwise will back up once a day on Wi-Fi."
            }
        }
    }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val action = pending
        pending = null
        val token = DriveBackup.tokenFrom(context, result.data)
        if (action == null || token == null) {
            message = "Google Drive access wasn't granted, so nothing was changed."
        } else {
            scope.launch { run(action, token) }
        }
    }

    fun start(action: DriveAction) {
        scope.launch {
            guarded("Connecting to Google…") {
                when (val auth = DriveBackup.authorize(context)) {
                    is DriveBackup.Auth.Granted -> run(action, auth.token)
                    is DriveBackup.Auth.NeedsConsent -> {
                        pending = action
                        consent.launch(IntentSenderRequest.Builder(auth.intent).build())
                    }
                }
            }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (last > 0) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Google Drive backup", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (last > 0) "Last backup ${TimeText.dateTime(last)}" else "Not backed up yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Saved to a private app folder in your Google Drive that only Gapwise can see. Each backup replaces the last one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { start(DriveAction.BACKUP) },
                    enabled = busy == null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Back up now") }
                FilledTonalButton(
                    onClick = { start(DriveAction.RESTORE) },
                    enabled = busy == null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Restore") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Back up every day", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "On Wi-Fi, when the battery isn't low",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = auto,
                    enabled = busy == null,
                    onCheckedChange = { on ->
                        if (on) {
                            start(DriveAction.AUTO_ON)
                        } else {
                            prefs.driveAutoBackup = false
                            auto = false
                            DriveBackupWorker.schedule(context)
                        }
                    },
                )
            }
            AnimatedVisibility(visible = busy != null) {
                Column {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(busy.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    restoreOffer?.let { (token, remote) ->
        AlertDialog(
            onDismissRequest = { restoreOffer = null },
            title = { Text("Restore from ${TimeText.dateTime(remote.modifiedAt)}?") },
            text = {
                Text("This replaces every event and all history on this phone with the backup from Google Drive. Anything you added since then will be lost.")
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreOffer = null
                    scope.launch {
                        guarded("Restoring…") {
                            val content = DriveBackup.download(token, remote.id)
                            val count = repo.restoreJson(content)
                            message = "Restored $count event${if (count == 1) "" else "s"} from Google Drive."
                        }
                    }
                }) { Text("Replace and restore") }
            },
            dismissButton = { TextButton(onClick = { restoreOffer = null }) { Text("Cancel") } },
        )
    }
}
