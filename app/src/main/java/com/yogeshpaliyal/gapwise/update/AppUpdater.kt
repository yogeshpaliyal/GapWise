package com.yogeshpaliyal.gapwise.update

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/** Where an update stands. The UI only reads this; each store flavor drives it. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Available(val version: String?) : UpdateState

    /** [progress] is 0..1, or null while the size isn't known yet. */
    data class Downloading(val progress: Float?) : UpdateState
    data object ReadyToInstall : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Store-specific update and rating behaviour. Each product flavor provides
 * `Updaters.create(context)`: GitHub releases for the sideloaded build,
 * Play In-App Updates and In-App Review for the Play build.
 */
interface AppUpdater {
    val state: StateFlow<UpdateState>

    /** Shown in settings, e.g. "Checks GitHub releases". */
    val sourceLabel: String

    /** Call from Activity.onCreate: registers result launchers and checks on every resume. */
    fun attach(activity: ComponentActivity)

    /** [userInitiated] checks always run and report errors; automatic ones are throttled and quiet. */
    fun check(userInitiated: Boolean)

    fun startUpdate(activity: Activity)

    /** Play only: restart into a downloaded update. */
    fun completeUpdate() {}

    val rateLabel: String
    fun rate(activity: Activity)
}

val LocalUpdater = staticCompositionLocalOf<AppUpdater?> { null }

/** JVM tests (Robolectric) shouldn't hit the network for update checks. */
internal val isRobolectric: Boolean get() = Build.FINGERPRINT == "robolectric"

/** Compares "1.10.2" with "1.9" numerically. Suffixes like "-master" are ignored. */
fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val x = parts(a)
    val y = parts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
        if (c != 0) return c
    }
    return 0
}
