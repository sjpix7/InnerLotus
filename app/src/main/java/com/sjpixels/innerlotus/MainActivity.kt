package com.sjpixels.innerlotus

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.sjpixels.innerlotus.audio.SoundEngine
import com.sjpixels.innerlotus.audio.SoundService
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
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
import com.sjpixels.innerlotus.ui.theme.LotusType
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.components.LotusDialog
import com.sjpixels.innerlotus.ui.components.SectionRail
import com.sjpixels.innerlotus.ui.components.SectionRailState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
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
        // The app only has a light theme, so keep dark system-bar icons even in system dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        App.init(applicationContext)
        // Only on a fresh start: a recreated activity still carries the original launch intent.
        if (savedInstanceState == null) handleIntent(intent)
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
                DisposableEffect(holder) {
                    nav.onPopped = { holder.removeState(it.toString()) }
                    onDispose { nav.onPopped = null }
                }
                val blur by animateDpAsState(if (SectionRailState.open) 16.dp else 0.dp, tween(260), label = "railBlur")
                AnimatedContent(
                    targetState = nav.current,
                    modifier = Modifier.blur(blur),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "screen",
                ) { screen ->
                    holder.SaveableStateProvider(screen.toString()) { ScreenHost(screen) }
                }
                SectionRail()
            }
            TtsMissingDialog()
            SoundNotificationPermission()
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

/**
 * Android 13+ hides a foreground service's notification until POST_NOTIFICATIONS is granted, which
 * would leave Sound Healing without its "Stop" control on the lock screen. Ask once, the first time
 * sound starts playing.
 */
@Composable
private fun SoundNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && SoundService.state == SoundService.State.RUNNING) SoundService.refreshNotification(ctx)
    }
    val playing = SoundEngine.isPlaying
    LaunchedEffect(playing) {
        if (playing && !App.settings.askedSoundNotif && !Reminders.canNotify(ctx)) {
            App.settings.askedSoundNotif = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** Offers to install a voice when the selected language has no TTS voice on this phone. */
@Composable
private fun TtsMissingDialog() {
    val lang = App.ttsMissing ?: return
    val ctx = LocalContext.current
    LotusDialog(
        onDismiss = { App.ttsMissing = null },
        title = tr("ui.tts.missing_title"),
        text = { Text(tr("ui.tts.missing_text", "lang" to lang.nativeName), style = LotusType.small) },
        confirm = {
            TextButton(onClick = { App.ttsMissing = null; Speaker.openInstallVoices(ctx) }) {
                Text(tr("ui.tts.install"), style = LotusType.button.copy(color = Lotus.Accent))
            }
        },
        dismiss = {
            TextButton(onClick = { App.ttsMissing = null }) { Text(tr("ui.common.later"), style = LotusType.button.copy(color = Lotus.Ink)) }
        },
    )
}
