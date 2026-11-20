package com.yogeshpaliyal.gapwise.backup

import android.content.Context

/** The F-Droid build has no Google Drive backup (it needs Google Play services). */
object DriveBackupWorker {
    fun schedule(context: Context) = Unit
}
