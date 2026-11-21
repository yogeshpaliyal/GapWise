package com.yogeshpaliyal.gapwise.update

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.yogeshpaliyal.gapwise.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

object Updaters {
    fun create(context: Context): AppUpdater = GithubUpdater(context.applicationContext)
}

/**
 * Sideloaded (GitHub) build.
 *  - Release builds look at the newest proper GitHub Release (tag vX.Y.Z, pre-releases skipped)
 *    and compare version names.
 *  - Debug builds look at the rolling latest-master pre-release and compare the commit
 *    they were built from with the one in the version.json that CI attaches to it.
 * Updating opens the APK download; Android's installer takes it from there.
 */
private class GithubUpdater(private val app: Context) : AppUpdater {
    private val repo = BuildConfig.GITHUB_REPO
    private val tracksMaster = BuildConfig.UPDATE_CHANNEL == "latest-master"
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = app.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state

    private var downloadUrl: String? = null
    private var activity: ComponentActivity? = null

    override val sourceLabel: String
        get() = if (tracksMaster) L10n.s(R.string.source_github_master) else L10n.s(R.string.source_github)

    override val rateLabel: String get() = L10n.s(R.string.rate_github)

    override fun attach(activity: ComponentActivity) {
        this.activity = activity
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = check(userInitiated = false)
            override fun onDestroy(owner: LifecycleOwner) {
                this@GithubUpdater.activity = null
            }
        })
    }

    override fun check(userInitiated: Boolean) {
        if (isRobolectric) return
        val now = System.currentTimeMillis()
        // Automatic checks at most every 6 hours, and never on top of a known update.
        if (!userInitiated) {
            if (_state.value is UpdateState.Available) return
            if (now - prefs.getLong("last_check", 0L) < 6 * 60 * 60 * 1000L) return
        }
        val scope = activity?.lifecycleScope ?: return
        scope.launch {
            if (userInitiated) _state.value = UpdateState.Checking
            try {
                val (newer, version, url) = withContext(Dispatchers.IO) { if (tracksMaster) latestMaster() else latestRelease() }
                prefs.edit().putLong("last_check", now).apply()
                downloadUrl = url
                _state.value = if (newer) UpdateState.Available(version) else UpdateState.UpToDate(BuildConfig.VERSION_NAME)
            } catch (e: Exception) {
                _state.value = if (userInitiated) UpdateState.Failed(L10n.s(R.string.update_err_github)) else UpdateState.Idle
            }
        }
    }

    /** Returns (isNewer, versionLabel, apkUrl). */
    private fun latestRelease(): Triple<Boolean, String, String> {
        val body = get("https://api.github.com/repos/$repo/releases/latest", accept = "application/vnd.github+json")
        val obj = json.parseToJsonElement(body).jsonObject
        val tag = obj.getValue("tag_name").jsonPrimitive.content
        val page = obj["html_url"]?.jsonPrimitive?.content ?: "https://github.com/$repo/releases/latest"
        // Releases also carry the fdroid flavor's APK. It has no updater, so never install it here.
        val apks = obj["assets"]?.jsonArray
            ?.map { it.jsonObject }
            ?.filter { it["name"]?.jsonPrimitive?.content?.let { n -> n.endsWith(".apk") && !n.contains("fdroid") } == true }
            .orEmpty()
        val apk = (apks.firstOrNull { it["name"]?.jsonPrimitive?.content == "gapwise-$tag-app.apk" } ?: apks.firstOrNull())
            ?.get("browser_download_url")?.jsonPrimitive?.content
        val version = tag.removePrefix("v")
        return Triple(compareVersions(version, BuildConfig.VERSION_NAME) > 0, version, apk ?: page)
    }

    private fun latestMaster(): Triple<Boolean, String, String> {
        val base = "https://github.com/$repo/releases/download/latest-master/"
        val obj = json.parseToJsonElement(get(base + "version.json")).jsonObject
        val commit = obj["commit"]?.jsonPrimitive?.content.orEmpty()
        // Debug builds share one version code, so compare the commit they were built from.
        // Local builds (no commit baked in) never prompt.
        val newer = BuildConfig.GIT_SHA.isNotEmpty() && commit.isNotEmpty() && commit != BuildConfig.GIT_SHA
        val label = obj["versionName"]?.jsonPrimitive?.content ?: "latest master"
        return Triple(newer, label, base + "gapwise-debug.apk")
    }

    private fun get(url: String, accept: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            accept?.let { conn.setRequestProperty("Accept", it) }
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    override fun startUpdate(activity: Activity) {
        val url = downloadUrl ?: "https://github.com/$repo/releases/latest"
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun rate(activity: Activity) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repo")))
    }
}
