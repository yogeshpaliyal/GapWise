package com.yogeshpaliyal.gapwise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yogeshpaliyal.gapwise.ui.detail.DetailScreen
import com.yogeshpaliyal.gapwise.ui.edit.EditScreen
import com.yogeshpaliyal.gapwise.ui.home.HomeScreen
import com.yogeshpaliyal.gapwise.ui.settings.SettingsScreen
import com.yogeshpaliyal.gapwise.ui.theme.GapwiseTheme
import com.yogeshpaliyal.gapwise.ui.welcome.IntroScreen
import com.yogeshpaliyal.gapwise.ui.welcome.WelcomeScreen
import com.yogeshpaliyal.gapwise.update.AppUpdater
import com.yogeshpaliyal.gapwise.update.LocalUpdater
import com.yogeshpaliyal.gapwise.update.Updaters
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {

    /** Store-specific updates: GitHub releases or Google Play, depending on the flavor. */
    private lateinit var updater: AppUpdater

    /** Set when opened from a nudge, widget or shortcut that targets a screen. */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        updater = Updaters.create(this).also { it.attach(this) }

        val start = if (app.prefs.welcomeSeen) Routes.HOME else Routes.INTRO

        setContent {
            CompositionLocalProvider(LocalUpdater provides updater) {
            GapwiseTheme {
                val nav = rememberNavController()
                LaunchedEffect(pendingRoute.value) {
                    pendingRoute.value?.let {
                        nav.navigate(it)
                        pendingRoute.value = null
                    }
                }
                NavHost(navController = nav, startDestination = start) {
                    composable(Routes.INTRO) {
                        IntroScreen(onDone = {
                            nav.navigate(Routes.WELCOME) { popUpTo(Routes.INTRO) { inclusive = true } }
                        })
                    }
                    composable(Routes.WELCOME) {
                        WelcomeScreen(onDone = {
                            app.prefs.welcomeSeen = true
                            nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                        })
                    }
                    composable(Routes.TOUR) {
                        WelcomeScreen(onDone = { nav.popBackStack() })
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onOpen = { nav.navigate(Routes.detail(it)) },
                            onAdd = { nav.navigate(Routes.edit(0)) },
                            onSettings = { nav.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(
                        Routes.DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        DetailScreen(
                            onBack = { nav.popBackStack() },
                            onEdit = { id -> nav.navigate(Routes.edit(id)) },
                        )
                    }
                    composable(
                        Routes.EDIT,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        EditScreen(onDone = { nav.popBackStack() })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { nav.popBackStack() },
                            onOpenTour = { nav.navigate(Routes.TOUR) },
                        )
                    }
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        when {
            intent.action == ACTION_ADD_EVENT -> pendingRoute.value = Routes.edit(0)
            eventId > 0 -> pendingRoute.value = Routes.detail(eventId)
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val ACTION_ADD_EVENT = "com.yogeshpaliyal.gapwise.ADD_EVENT"
    }
}

object Routes {
    const val INTRO = "intro"
    const val WELCOME = "welcome"
    const val TOUR = "tour"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit/{id}"
    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long) = "edit/$id"
}
