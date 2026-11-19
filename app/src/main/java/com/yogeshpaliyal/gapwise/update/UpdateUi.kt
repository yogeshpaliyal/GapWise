package com.yogeshpaliyal.gapwise.update

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Home-screen card for an update in progress. Shows only when there's
 * something to do: available, downloading, or ready to restart.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val updater = LocalUpdater.current ?: return
    val state by updater.state.collectAsState()
    val activity = LocalContext.current as? Activity
    var dismissed by rememberSaveable { mutableStateOf(false) }

    val visible = !dismissed && (state is UpdateState.Available || state is UpdateState.Downloading || state is UpdateState.ReadyToInstall)
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        color = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SystemUpdate, null) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val s = state
                        Text(
                            when (s) {
                                is UpdateState.ReadyToInstall -> "Update ready"
                                is UpdateState.Downloading -> "Downloading update"
                                is UpdateState.Available -> if (s.version != null) "Version ${s.version} is available" else "An update is available"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            when (s) {
                                is UpdateState.ReadyToInstall -> "Restart Gapwise to finish. Your data stays as it is."
                                is UpdateState.Downloading -> "Keep using the app. We'll let you know when it's ready."
                                else -> "Get the latest fixes and features."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                val s = state
                if (s is UpdateState.Downloading) {
                    val p = s.progress
                    if (p != null) {
                        LinearWavyProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        if (s !is UpdateState.ReadyToInstall) {
                            TextButton(onClick = { dismissed = true }) { Text("Later") }
                        }
                        FilledTonalButton(
                            onClick = {
                                if (s is UpdateState.ReadyToInstall) updater.completeUpdate()
                                else activity?.let(updater::startUpdate)
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) { Text(if (s is UpdateState.ReadyToInstall) "Restart now" else "Update") }
                    }
                }
            }
        }
    }
}

/** Settings rows: check for updates, and rate the app (store-specific). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateSettings(versionName: String) {
    val updater = LocalUpdater.current ?: return
    val state by updater.state.collectAsState()
    val activity = LocalContext.current as? Activity
    val s = state
    val subtitle = when (s) {
        UpdateState.Idle -> "Version $versionName. ${updater.sourceLabel}."
        UpdateState.Checking -> "Checking…"
        is UpdateState.UpToDate -> "You're on the latest version ($versionName)."
        is UpdateState.Available -> (if (s.version != null) "Version ${s.version} is available." else "An update is available.") + " Tap to update."
        is UpdateState.Downloading -> "Downloading the update…"
        UpdateState.ReadyToInstall -> "Update downloaded. Tap to restart and finish."
        is UpdateState.Failed -> s.message
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable {
                when (s) {
                    is UpdateState.Available -> activity?.let(updater::startUpdate)
                    UpdateState.ReadyToInstall -> updater.completeUpdate()
                    UpdateState.Checking, is UpdateState.Downloading -> Unit
                    else -> updater.check(userInitiated = true)
                }
            }
            .padding(16.dp),
    ) {
        Text("Check for updates", style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedVisibility(visible = s == UpdateState.Checking) {
            Column {
                Spacer(Modifier.height(8.dp))
                LinearWavyProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { activity?.let(updater::rate) }
            .padding(16.dp),
    ) {
        Text(updater.rateLabel, style = MaterialTheme.typography.titleMedium)
        Text("It helps other people find Gapwise.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
