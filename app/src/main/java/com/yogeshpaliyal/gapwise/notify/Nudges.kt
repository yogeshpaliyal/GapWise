package com.yogeshpaliyal.gapwise.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yogeshpaliyal.gapwise.MainActivity
import com.yogeshpaliyal.gapwise.R
import com.yogeshpaliyal.gapwise.app
import com.yogeshpaliyal.gapwise.data.EventWithLogs
import com.yogeshpaliyal.gapwise.domain.RhythmCalculator
import com.yogeshpaliyal.gapwise.domain.TimeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object Nudges {
    const val CHANNEL = "nudges"
    private const val WORK = "gapwise-nudges"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.channel_nudges),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_nudges_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Checks a few times a day. Cheap: it only reads the local database. */
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (!context.app.prefs.nudgesOn) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<NudgeWorker>(3, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** True if this event should get a nudge right now. */
    fun isDue(item: EventWithLogs, now: Long): Boolean {
        val e = item.event
        if (e.paused || !e.nudgesEnabled || e.hidden) return false
        val r = RhythmCalculator.compute(item)
        val next = r.nextExpectedAt ?: return false
        val last = r.lastAt ?: return false
        // Once per cycle: only if we haven't nudged since the last log.
        return now >= next && e.lastNudgedAt < last
    }

    fun notificationId(eventId: Long) = (eventId % Int.MAX_VALUE).toInt() + 1000

    fun show(context: Context, item: EventWithLogs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val e = item.event
        val r = RhythmCalculator.compute(item)
        val id = notificationId(e.id)

        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_EVENT_ID, e.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val log = PendingIntent.getBroadcast(
            context, id,
            Intent(context, NudgeActionReceiver::class.java)
                .setAction(NudgeActionReceiver.ACTION_LOG)
                .putExtra(MainActivity.EXTRA_EVENT_ID, e.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = buildString {
            append(TimeText.elapsedInline(r.lastAt).replaceFirstChar { it.uppercase() })
            r.usualDays?.let { append(" · ${TimeText.usualText(it).lowercase()}") }
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${e.emoji} ${e.name} — usually around now")
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, if (e.isMultiDay) "Start" else "Log it", log)
            .addAction(0, "View insights", open)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}

class NudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!ctx.app.prefs.nudgesOn) return Result.success()
        val repo = ctx.app.repository
        val now = System.currentTimeMillis()
        repo.getAll().filter { Nudges.isDue(it, now) }.forEach { item ->
            Nudges.show(ctx, item)
            repo.markNudged(item.event.id, now)
        }
        return Result.success()
    }
}

/** Handles "Log it" from a notification without opening the app. */
class NudgeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG) return
        val id = intent.getLongExtra(MainActivity.EXTRA_EVENT_ID, 0L)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                context.app.repository.logNow(id)
                NotificationManagerCompat.from(context).cancel(Nudges.notificationId(id))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOG = "com.yogeshpaliyal.gapwise.LOG_FROM_NUDGE"
    }
}
