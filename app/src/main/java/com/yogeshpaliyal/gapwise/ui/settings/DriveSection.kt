package com.yogeshpaliyal.gapwise.ui.settings

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

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
        L10n.s(R.string.drive_err_setup)
    e is ApiException && e.statusCode == 7 -> L10n.s(R.string.drive_err_google)
    e is DriveBackup.DriveException && e.code == 403 ->
        L10n.s(R.string.drive_err_refused)
    e is DriveBackup.DriveException && e.code == 401 -> L10n.s(R.string.drive_err_expired)
    e is IOException -> L10n.s(R.string.drive_err_offline)
    e is kotlinx.serialization.SerializationException || e is IllegalArgumentException ->
        L10n.s(R.string.drive_err_unreadable)
    else -> e.message ?: L10n.s(R.string.drive_err_other)
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
            DriveAction.BACKUP -> guarded(L10n.s(R.string.drive_backing_up)) {
                val remote = DriveBackup.upload(token, repo.exportJson())
                prefs.lastDriveBackupAt = remote.modifiedAt
                last = remote.modifiedAt
                message = L10n.s(R.string.drive_backed_up)
            }
            DriveAction.RESTORE -> guarded(L10n.s(R.string.drive_looking)) {
                val remote = DriveBackup.latest(token)
                if (remote == null) message = L10n.s(R.string.drive_none)
                else restoreOffer = token to remote
            }
            DriveAction.AUTO_ON -> {
                prefs.driveAutoBackup = true
                auto = true
                DriveBackupWorker.schedule(context)
                message = L10n.s(R.string.drive_auto_on)
            }
        }
    }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val action = pending
        pending = null
        val token = DriveBackup.tokenFrom(context, result.data)
        if (action == null || token == null) {
            message = L10n.s(R.string.drive_denied)
        } else {
            scope.launch { run(action, token) }
        }
    }

    fun start(action: DriveAction) {
        scope.launch {
            guarded(L10n.s(R.string.drive_connecting)) {
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
                    Text(L10n.s(R.string.drive_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (last > 0) L10n.s(R.string.drive_last, TimeText.dateTime(last)) else L10n.s(R.string.drive_never),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                L10n.s(R.string.drive_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { start(DriveAction.BACKUP) },
                    enabled = busy == null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(L10n.s(R.string.drive_backup_now)) }
                FilledTonalButton(
                    onClick = { start(DriveAction.RESTORE) },
                    enabled = busy == null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text(L10n.s(R.string.drive_restore)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(L10n.s(R.string.drive_daily), style = MaterialTheme.typography.titleMedium)
                    Text(
                        L10n.s(R.string.drive_daily_hint),
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
            title = { Text(L10n.s(R.string.drive_restore_title, TimeText.dateTime(remote.modifiedAt))) },
            text = {
                Text(L10n.s(R.string.drive_restore_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreOffer = null
                    scope.launch {
                        guarded(L10n.s(R.string.drive_restoring)) {
                            val content = DriveBackup.download(token, remote.id)
                            val count = repo.restoreJson(content)
                            message = L10n.p(R.plurals.drive_restored, count)
                        }
                    }
                }) { Text(L10n.s(R.string.drive_replace)) }
            },
            dismissButton = { TextButton(onClick = { restoreOffer = null }) { Text(L10n.s(R.string.cancel)) } },
        )
    }
}
