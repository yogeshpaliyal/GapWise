package com.yogeshpaliyal.gapwise

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.yogeshpaliyal.gapwise.data.GapwiseDatabase
import com.yogeshpaliyal.gapwise.data.GapwiseRepository
import com.yogeshpaliyal.gapwise.notify.Nudges

class GapwiseApp : Application(), Configuration.Provider {
    /** WorkManager initialises on demand from this, rather than at startup. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    val database: GapwiseDatabase by lazy { GapwiseDatabase.create(this) }
    val repository: GapwiseRepository by lazy { GapwiseRepository(this, database.dao()) }
    val prefs: Prefs by lazy { Prefs(this) }

    override fun onCreate() {
        super.onCreate()
        Nudges.createChannel(this)
        Nudges.schedule(this)
    }
}

val Context.app: GapwiseApp get() = applicationContext as GapwiseApp

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("gapwise", Context.MODE_PRIVATE)

    var gridView: Boolean
        get() = sp.getBoolean("grid", true)
        set(v) = sp.edit().putBoolean("grid", v).apply()

    var sort: String
        get() = sp.getString("sort", "due") ?: "due"
        set(v) = sp.edit().putString("sort", v).apply()

    var nudgesOn: Boolean
        get() = sp.getBoolean("nudges", true)
        set(v) = sp.edit().putBoolean("nudges", v).apply()

    var welcomeSeen: Boolean
        get() = sp.getBoolean("welcome_seen", false)
        set(v) = sp.edit().putBoolean("welcome_seen", v).apply()
}
