package com.yogeshpaliyal.gapwise

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Strings for code that has no Context of its own (time texts, status labels, widgets).
 * Reads through the application context on every call, so a language change in system
 * settings (per-app language, Android 13+) is picked up straight away.
 */
@SuppressLint("StaticFieldLeak")
object L10n {
    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun s(@StringRes id: Int, vararg args: Any): String = app.getString(id, *args)

    fun p(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        app.resources.getQuantityString(id, count, *(if (args.isEmpty()) arrayOf<Any>(count) else args))
}
