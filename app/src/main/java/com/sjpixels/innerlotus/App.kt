package com.sjpixels.innerlotus

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sjpixels.innerlotus.core.AppSettings
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import com.sjpixels.innerlotus.data.AffirmationStore
import com.sjpixels.innerlotus.data.HabitStore
import com.sjpixels.innerlotus.data.JournalStore
import com.sjpixels.innerlotus.data.MixStore
import com.sjpixels.innerlotus.reminders.Reminders
import com.sjpixels.innerlotus.tts.Speaker
import java.io.File

class InnerLotusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.init(this)
    }
}

/** Every screen of the app. Data classes so a screen with parameters is a distinct entry. */
sealed interface Screen {
    data object Home : Screen
    data class Learn(val topic: String) : Screen
    data class Areas(val chakra: String? = null) : Screen
    data class ChakraPick(val area: String) : Screen
    data class Deck(val area: String, val chakra: String?) : Screen
    data class BulkEdit(val area: String) : Screen
    data object AffirmData : Screen
    data object Eft : Screen
    data class EftSession(val topic: String, val technique: String) : Screen
    data object Sound : Screen
    data object Hoopono : Screen
    data object Journal : Screen
    data class JournalEdit(val id: String?, val mode: String) : Screen
    data object Habits : Screen
    data object HabitStats : Screen
    data object AppUsage : Screen
    data object Breath : Screen
    data class BreathSession(val pattern: String) : Screen
    data object ChakraGuide : Screen
    data object CalmNow : Screen
    data object Settings : Screen
    data object About : Screen
}

/** Minimal back-stack navigator kept outside the Activity so it survives configuration changes. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(s: Screen) {
        if (current != s) stack.add(s)
    }

    /** Replace the current screen (e.g. after saving an editor). */
    fun replace(s: Screen) {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack.add(s)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun home() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

/** Application-wide singletons (simple service locator; no DI framework needed). */
object App {
    lateinit var context: Context
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var affirmations: AffirmationStore
        private set
    lateinit var journal: JournalStore
        private set
    lateinit var habits: HabitStore
        private set
    lateinit var mixes: MixStore
        private set

    val nav = Navigator()

    /** Set when the chosen language has no installed TTS voice; the UI shows a helper dialog. */
    var ttsMissing by mutableStateOf<Lang?>(null)

    /** Journal is unlocked for this app session after the PIN is entered. */
    var journalUnlocked by mutableStateOf(false)

    private var initialized = false

    fun init(app: Context) {
        if (initialized) return
        initialized = true
        context = app.applicationContext
        settings = AppSettings(context)
        val dir = File(context.filesDir, "data").apply { mkdirs() }
        affirmations = AffirmationStore(File(dir, "affirmations.json"))
        journal = JournalStore(File(dir, "journal.json"))
        habits = HabitStore(File(dir, "habits.json"))
        mixes = MixStore(File(dir, "mixes.json"))
        val lang = if (settings.lang.isEmpty()) Lang.fromSystem() else Lang.of(settings.lang)
        I18n.feminine = settings.feminine
        I18n.load(context, lang)
        Speaker.init(context)
        Reminders.ensureChannels(context)
    }

    fun setLanguage(l: Lang) {
        settings.lang = l.code
        I18n.load(context, l)
        Speaker.applyLanguage { available -> if (!available) ttsMissing = l }
    }

    fun setFeminine(f: Boolean) {
        settings.feminine = f
        I18n.feminine = f
    }
}
