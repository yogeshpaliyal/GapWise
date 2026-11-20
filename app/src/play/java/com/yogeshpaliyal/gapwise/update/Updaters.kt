package com.yogeshpaliyal.gapwise.update

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.yogeshpaliyal.gapwise.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Updaters {
    fun create(context: Context): AppUpdater = PlayUpdater(context.applicationContext)
}

/**
 * Google Play build: Play In-App Updates (flexible flow, so you keep using the
 * app while it downloads) and Play In-App Review for rating.
 */
private class PlayUpdater(private val app: Context) : AppUpdater {
    private val manager = AppUpdateManagerFactory.create(app)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state

    private var info: AppUpdateInfo? = null
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null

    override val sourceLabel: String get() = L10n.s(R.string.source_play)
    override val rateLabel: String get() = L10n.s(R.string.rate_play)

    private val listener = InstallStateUpdatedListener { s ->
        _state.value = when (s.installStatus()) {
            InstallStatus.PENDING -> UpdateState.Downloading(null)
            InstallStatus.DOWNLOADING -> {
                val total = s.totalBytesToDownload()
                UpdateState.Downloading(if (total > 0) s.bytesDownloaded().toFloat() / total else null)
            }
            InstallStatus.DOWNLOADED -> UpdateState.ReadyToInstall
            InstallStatus.FAILED -> UpdateState.Failed(L10n.s(R.string.update_err_download))
            InstallStatus.CANCELED -> UpdateState.Available(null)
            else -> _state.value
        }
    }

    override fun attach(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // Declined: keep offering it quietly on the home screen.
            if (result.resultCode != Activity.RESULT_OK && _state.value !is UpdateState.Downloading) {
                _state.value = UpdateState.Available(info?.availableVersionCode()?.toString())
            }
        }
        manager.registerListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = check(userInitiated = false)
            override fun onDestroy(owner: LifecycleOwner) {
                manager.unregisterListener(listener)
                launcher = null
            }
        })
    }

    override fun check(userInitiated: Boolean) {
        if (isRobolectric) return
        if (userInitiated) _state.value = UpdateState.Checking
        manager.appUpdateInfo
            .addOnSuccessListener { i ->
                info = i
                _state.value = when {
                    i.installStatus() == InstallStatus.DOWNLOADED -> UpdateState.ReadyToInstall
                    i.installStatus() == InstallStatus.DOWNLOADING -> UpdateState.Downloading(null)
                    i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> UpdateState.Available(null)
                    else -> UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
            .addOnFailureListener {
                // Common on devices without Play or for builds not installed from Play.
                _state.value = if (userInitiated) {
                    UpdateState.Failed(L10n.s(R.string.update_err_play))
                } else {
                    UpdateState.Idle
                }
            }
    }

    override fun startUpdate(activity: Activity) {
        val i = info
        val l = launcher
        if (i == null || l == null) {
            openListing(activity)
            return
        }
        val started = runCatching {
            manager.startUpdateFlowForResult(i, l, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
        }.getOrDefault(false)
        if (!started) openListing(activity)
    }

    override fun completeUpdate() {
        manager.completeUpdate()
    }

    override fun rate(activity: Activity) {
        val reviews = ReviewManagerFactory.create(app)
        reviews.requestReviewFlow()
            .addOnSuccessListener { info -> reviews.launchReviewFlow(activity, info) }
            .addOnFailureListener { openListing(activity) }
    }

    private fun openListing(activity: Activity) {
        val pkg = app.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        runCatching { activity.startActivity(market) }
            .onFailure { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) }
    }
}
