package com.sjpixels.innerlotus.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonElement

/**
 * Runtime translations. Reading [t] or [list] inside a composable subscribes it to language and
 * form-of-address changes, so switching the flag menu re-renders every screen instantly.
 */
object I18n {
    var lang by mutableStateOf(Lang.EN)
        private set
    var feminine by mutableStateOf(true)
    private var table by mutableStateOf(StringTable(emptyMap()))
    private var english: Map<String, JsonElement> = emptyMap()

    fun load(context: Context, l: Lang) {
        if (english.isEmpty()) english = read(context, "en")
        val map = if (l == Lang.EN) english else runCatching { read(context, l.code) }.getOrDefault(english)
        table = StringTable(map, english)
        lang = l
    }

    fun ensureLoaded(context: Context, code: String) {
        if (english.isEmpty() || lang.code != code) load(context, Lang.of(code))
    }

    private fun read(context: Context, code: String): Map<String, JsonElement> =
        context.assets.open("i18n/$code.json").bufferedReader(Charsets.UTF_8).use { StringTable.parse(it.readText()) }

    fun t(key: String): String = table.str(key, feminine)

    fun t(key: String, vararg args: Pair<String, Any?>): String {
        val n = args.firstOrNull { it.first == "n" }?.second
        return table.str(table.pluralKey(key, n), feminine).fill(*args)
    }

    fun list(key: String): List<String> = table.list(key, feminine)

    fun has(key: String): Boolean = table.has(key)

    /** English text regardless of the current language (used for CSV area matching). */
    fun english(key: String): String = StringTable(english).str(key, feminine)
}

/** Shorthand used throughout the UI. */
fun tr(key: String): String = I18n.t(key)

fun tr(key: String, vararg args: Pair<String, Any?>): String = I18n.t(key, *args)
