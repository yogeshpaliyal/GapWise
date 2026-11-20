package com.yogeshpaliyal.gapwise.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Adds a widget to the home screen from inside the app, for launchers that hide (or don't have)
 * a widget picker. The launcher shows its own "Add to home screen" dialog; once the user accepts,
 * the widget picker opens so they can choose which events it shows, just as when it's added the
 * usual way.
 */
object PinWidget {

    enum class Kind(val receiver: Class<*>) {
        DueSoon(GapwiseWidgetReceiver::class.java),
        Tiles(GapwiseTilesWidgetReceiver::class.java),
    }

    /** False when the launcher can't add widgets on an app's behalf. */
    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Asks the launcher to add the widget. Returns false if it can't. */
    fun request(context: Context, kind: Kind): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        // The launcher fills in EXTRA_APPWIDGET_ID, so the PendingIntent has to be mutable.
        val pick = PendingIntent.getActivity(
            context,
            kind.ordinal,
            Intent(context, TilesWidgetConfigActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return runCatching {
            manager.requestPinAppWidget(ComponentName(context, kind.receiver), null, pick)
        }.getOrDefault(false)
    }
}
