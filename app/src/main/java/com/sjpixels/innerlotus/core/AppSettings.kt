package com.sjpixels.innerlotus.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** A SharedPreferences value mirrored into Compose state, so UI recomposes when it changes. */
class Pref<T>(
    private val sp: SharedPreferences,
    private val key: String,
    default: T,
    read: (SharedPreferences, String, T) -> T,
    private val write: (SharedPreferences.Editor, String, T) -> Unit,
) : ReadWriteProperty<Any?, T> {
    private val state = mutableStateOf(read(sp, key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        val e = sp.edit()
        write(e, key, value)
        e.apply()
    }
}

private fun SharedPreferences.bool(key: String, def: Boolean) =
    Pref(this, key, def, { p, k, d -> p.getBoolean(k, d) }, { e, k, v -> e.putBoolean(k, v) })

private fun SharedPreferences.int(key: String, def: Int) =
    Pref(this, key, def, { p, k, d -> p.getInt(k, d) }, { e, k, v -> e.putInt(k, v) })

private fun SharedPreferences.float(key: String, def: Float) =
    Pref(this, key, def, { p, k, d -> p.getFloat(k, d) }, { e, k, v -> e.putFloat(k, v) })

private fun SharedPreferences.string(key: String, def: String) =
    Pref(this, key, def, { p, k, d -> p.getString(k, d) ?: d }, { e, k, v -> e.putString(k, v) })

/** All user settings. Stored privately on the device (SharedPreferences "settings"). */
class AppSettings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var onboarded by prefs.bool("onboarded", false)
    var lang by prefs.string("lang", "")
    var feminine by prefs.bool("feminine", true)

    /** "rotate" (every [rotateSeconds]) or "fixed" ([bgFixed]). */
    var bgMode by prefs.string("bgMode", "rotate")
    var bgFixed by prefs.string("bgFixed", "lotus")
    var rotateSeconds by prefs.int("rotateSeconds", 15)

    /** Affirmation card background: "area", "chakra", "app" or a background id. */
    var cardStyle by prefs.string("cardStyle", "area")

    var ttsRate by prefs.float("ttsRate", 0.9f)
    var ttsPitch by prefs.float("ttsPitch", 1.0f)
    /** "manual", "auto", "repeat", "count" */
    var ttsMode by prefs.string("ttsMode", "manual")
    var ttsCount by prefs.int("ttsCount", 3)
    var readCore by prefs.bool("readCore", true)
    var shuffle by prefs.bool("shuffle", false)
    var keepScreenOn by prefs.bool("keepScreenOn", true)

    var remindAffirm by prefs.bool("remindAffirm", false)
    var remindAffirmAt by prefs.int("remindAffirmAt", 8 * 60)
    var remindJournal by prefs.bool("remindJournal", false)
    var remindJournalAt by prefs.int("remindJournalAt", 21 * 60)

    /** SHA-256 of salt+PIN, or "" when the journal is not locked. */
    var journalPinHash by prefs.string("journalPinHash", "")
    var journalPinSalt by prefs.string("journalPinSalt", "")

    var eftTechnique by prefs.string("eftTechnique", "basic")
    var eftVoice by prefs.bool("eftVoice", true)
    var breathVoice by prefs.bool("breathVoice", true)
    var breathMinutes by prefs.int("breathMinutes", 3)
    var sleepTimerMin by prefs.int("sleepTimerMin", 0)

    /** We ask for notification permission once, the first time sound plays (for the Stop control). */
    var askedSoundNotif by prefs.bool("askedSoundNotif", false)

    /** How many times the section rail has shown all its labels (first-use hint, max 3). */
    var railHintsShown by prefs.int("railHintsShown", 0)

    fun clearAll() {
        val keepLang = lang
        prefs.edit().clear().apply()
        lang = keepLang
        onboarded = true
    }
}
