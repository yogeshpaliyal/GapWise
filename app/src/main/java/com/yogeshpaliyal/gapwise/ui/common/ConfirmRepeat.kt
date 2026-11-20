package com.yogeshpaliyal.gapwise.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.data.LogResult
import com.yogeshpaliyal.gapwise.data.secondsAgoText
import com.yogeshpaliyal.gapwise.ui.theme.GapwiseTheme
import kotlinx.coroutines.launch

/** What a repeat tap would do, so the prompt names the right action. */
data class RepeatPrompt(val eventId: Long, val name: String, val agoMs: Long, val wouldEnd: Boolean)

fun EventWithLogs.repeatPrompt(now: Long = System.currentTimeMillis()): RepeatPrompt? =
    recentActionAgo(now)?.let { RepeatPrompt(event.id, event.name, it, wouldEnd = ongoing != null) }

/** Asks before logging the same event twice within a minute. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConfirmRepeatDialog(prompt: RepeatPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val (title, body, action) = if (prompt.wouldEnd) {
        Triple(
            "End ${prompt.name} already?",
            "It started ${secondsAgoText(prompt.agoMs)}. Ending it now records it as lasting less than a minute.",
            "End it",
        )
    } else {
        Triple(
            "Log ${prompt.name} again?",
            "You logged it ${secondsAgoText(prompt.agoMs)}. Tap Log again only if you really did it twice.",
            "Log again",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * A see-through activity that shows only [ConfirmRepeatDialog]. Home-screen widgets and
 * launcher shortcuts can't show dialogs themselves, so they open this instead.
 */
class ConfirmLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val id = intent?.getLongExtra(EXTRA_EVENT_ID, 0L) ?: 0L
        if (id <= 0) {
            finish()
            return
        }
        val repo = app.repository
        setContent {
            GapwiseTheme {
                var prompt by remember { mutableStateOf<RepeatPrompt?>(null) }
                LaunchedEffect(id) {
                    val item = repo.get(id)
                    if (item == null) {
                        finish()
                        return@LaunchedEffect
                    }
                    val p = item.repeatPrompt()
                    if (p == null) {
                        // The minute passed while this opened: no need to ask any more.
                        log(id, item.event.name)
                    } else {
                        prompt = p
                    }
                }
                prompt?.let { p ->
                    ConfirmRepeatDialog(
                        prompt = p,
                        onConfirm = { log(id, p.name) },
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }

    private fun log(id: Long, name: String) {
        lifecycleScope.launch {
            val result = app.repository.logNow(id)
            val msg = when (result) {
                is LogResult.Ended -> "Ended $name"
                is LogResult.Added -> "Logged $name"
                null -> "That event no longer exists"
            }
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"

        fun intent(context: Context, eventId: Long): Intent =
            Intent(context, ConfirmLogActivity::class.java)
                .putExtra(EXTRA_EVENT_ID, eventId)
                // Distinct data so each event gets its own PendingIntent from widgets.
                .setData(android.net.Uri.parse("gapwise://confirm/$eventId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
