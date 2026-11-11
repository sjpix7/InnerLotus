package com.sjpixels.innerlotus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.reminders.Reminders
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.AppBackground
import com.sjpixels.innerlotus.ui.screens.AboutScreen
import com.sjpixels.innerlotus.ui.screens.AffirmDataScreen
import com.sjpixels.innerlotus.ui.screens.AppUsageScreen
import com.sjpixels.innerlotus.ui.screens.AreasScreen
import com.sjpixels.innerlotus.ui.screens.BreathScreen
import com.sjpixels.innerlotus.ui.screens.BreathSessionScreen
import com.sjpixels.innerlotus.ui.screens.BulkEditScreen
import com.sjpixels.innerlotus.ui.screens.CalmNowScreen
import com.sjpixels.innerlotus.ui.screens.ChakraGuideScreen
import com.sjpixels.innerlotus.ui.screens.ChakraPickScreen
import com.sjpixels.innerlotus.ui.screens.DeckScreen
import com.sjpixels.innerlotus.ui.screens.EftScreen
import com.sjpixels.innerlotus.ui.screens.EftSessionScreen
import com.sjpixels.innerlotus.ui.screens.HabitStatsScreen
import com.sjpixels.innerlotus.ui.screens.HabitsScreen
import com.sjpixels.innerlotus.ui.screens.HomeScreen
import com.sjpixels.innerlotus.ui.screens.HooponoScreen
import com.sjpixels.innerlotus.ui.screens.JournalEditScreen
import com.sjpixels.innerlotus.ui.screens.JournalScreen
import com.sjpixels.innerlotus.ui.screens.LearnScreen
import com.sjpixels.innerlotus.ui.screens.OnboardingScreen
import com.sjpixels.innerlotus.ui.screens.SettingsScreen
import com.sjpixels.innerlotus.ui.screens.SoundScreen
import com.sjpixels.innerlotus.ui.theme.InnerLotusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        App.init(applicationContext)
        handleIntent(intent)
        setContent { InnerLotusRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Opens the right screen when a reminder notification is tapped. */
    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra(Reminders.EXTRA_OPEN)) {
            "journal" -> { App.nav.home(); App.nav.go(Screen.Journal) }
            "daily" -> App.nav.home()
        }
        intent?.removeExtra(Reminders.EXTRA_OPEN)
    }
}

@Composable
fun InnerLotusRoot() {
    InnerLotusTheme {
        Box(Modifier.fillMaxSize()) {
            AppBackground()
            if (!App.settings.onboarded) {
                OnboardingScreen()
            } else {
                val nav = App.nav
                BackHandler(enabled = nav.canGoBack) { nav.back() }
                val holder = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = nav.current,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen",
                ) { screen ->
                    holder.SaveableStateProvider(screen.toString()) { ScreenHost(screen) }
                }
            }
            TtsMissingDialog()
        }
    }
}

@Composable
private fun ScreenHost(screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen()
        is Screen.Learn -> LearnScreen(screen.topic)
        is Screen.Areas -> AreasScreen(screen.chakra)
        is Screen.ChakraPick -> ChakraPickScreen(screen.area)
        is Screen.Deck -> DeckScreen(screen.area, screen.chakra)
        is Screen.BulkEdit -> BulkEditScreen(screen.area)
        Screen.AffirmData -> AffirmDataScreen()
        Screen.Eft -> EftScreen()
        is Screen.EftSession -> EftSessionScreen(screen.topic, screen.technique)
        Screen.Sound -> SoundScreen()
        Screen.Hoopono -> HooponoScreen()
        Screen.Journal -> JournalScreen()
        is Screen.JournalEdit -> JournalEditScreen(screen.id, screen.mode)
        Screen.Habits -> HabitsScreen()
        Screen.HabitStats -> HabitStatsScreen()
        Screen.AppUsage -> AppUsageScreen()
        Screen.Breath -> BreathScreen()
        is Screen.BreathSession -> BreathSessionScreen(screen.pattern)
        Screen.ChakraGuide -> ChakraGuideScreen()
        Screen.CalmNow -> CalmNowScreen()
        Screen.Settings -> SettingsScreen()
        Screen.About -> AboutScreen()
    }
}

/** Offers to install a voice when the selected language has no TTS voice on this phone. */
@Composable
private fun TtsMissingDialog() {
    val lang = App.ttsMissing ?: return
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = { App.ttsMissing = null },
        title = { Text(tr("ui.tts.missing_title")) },
        text = { Text(tr("ui.tts.missing_text", "lang" to lang.nativeName)) },
        confirmButton = {
            TextButton(onClick = { App.ttsMissing = null; Speaker.openInstallVoices(ctx) }) { Text(tr("ui.tts.install")) }
        },
        dismissButton = { TextButton(onClick = { App.ttsMissing = null }) { Text(tr("ui.common.later")) } },
    )
}
