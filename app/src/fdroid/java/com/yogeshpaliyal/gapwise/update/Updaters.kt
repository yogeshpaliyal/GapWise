package com.yogeshpaliyal.gapwise.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Updaters {
    fun create(context: Context): AppUpdater = StoreManagedUpdater
}

/**
 * F-Droid / IzzyOnDroid build: the store handles updates, so the app never checks or downloads
 * anything itself (F-Droid's inclusion policy asks for this). Only the "star on GitHub" row stays.
 */
private object StoreManagedUpdater : AppUpdater {
    override val state: StateFlow<UpdateState> = MutableStateFlow(UpdateState.Idle)
    override val sourceLabel: String get() = L10n.s(R.string.source_store)
    override val canCheck: Boolean = false
    override fun attach(activity: ComponentActivity) = Unit
    override fun check(userInitiated: Boolean) = Unit
    override fun startUpdate(activity: Activity) = Unit
    override val rateLabel: String get() = L10n.s(R.string.rate_github)
    override fun rate(activity: Activity) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yogeshpaliyal/GapWise")))
    }
}
