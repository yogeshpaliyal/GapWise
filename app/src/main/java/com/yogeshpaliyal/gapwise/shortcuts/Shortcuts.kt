package com.yogeshpaliyal.gapwise.shortcuts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.R
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.data.LogResult
import kotlinx.coroutines.launch

/**
 * Long-press the launcher icon to log pinned events in one tap.
 * These dynamic shortcuts are also what Google Assistant can surface.
 */
object ShortcutPublisher {
    const val ACTION_LOG = "com.yogeshpaliyal.gapwise.LOG_SHORTCUT"

    fun publish(context: Context, items: List<EventWithLogs>) {
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtMost(4) - 1 // keep room for "New event"
        val chosen = items
            .filter { !it.event.hidden }
            .sortedWith(compareByDescending<EventWithLogs> { it.event.pinned }.thenBy { it.event.sortOrder })
            .take(max.coerceAtLeast(0))
        val shortcuts = chosen.mapIndexed { i, item ->
            val e = item.event
            ShortcutInfoCompat.Builder(context, "log_${e.id}")
                .setShortLabel("${e.emoji} ${e.name}".take(25))
                .setLongLabel("Log ${e.name}")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setRank(i)
                .setIntent(
                    Intent(context, LogShortcutActivity::class.java)
                        .setAction(ACTION_LOG)
                        .putExtra(MainActivity.EXTRA_EVENT_ID, e.id),
                )
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}

/** Invisible activity: logs the event, shows a toast, and closes. */
class LogShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getLongExtra(MainActivity.EXTRA_EVENT_ID, 0L) ?: 0L
        if (id <= 0) {
            finish()
            return
        }
        lifecycleScope.launch {
            val repo = app.repository
            val name = repo.get(id)?.event?.name
            val result = repo.logNow(id)
            val msg = when {
                result == null || name == null -> "Event not found"
                result is LogResult.Ended -> "Ended “$name”"
                else -> "Logged “$name”"
            }
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
