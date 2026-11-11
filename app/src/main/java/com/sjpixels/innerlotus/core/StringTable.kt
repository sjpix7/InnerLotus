package com.sjpixels.innerlotus.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Gender markup `{masc|fem}` used in Hindi, Russian, Spanish, French and Italian content. */
object Gender {
    private val RX = Regex("""\{([^{}|]*)\|([^{}|]*)\}""")
    fun resolve(s: String, feminine: Boolean): String =
        if (s.indexOf('|') < 0) s
        else RX.replace(s) { m -> if (feminine) m.groupValues[2] else m.groupValues[1] }
}

/**
 * A flat key -> string | list table loaded from assets/i18n/<lang>.json, with English fallback.
 * Pure Kotlin so it can be unit-tested on the JVM.
 */
class StringTable(
    private val map: Map<String, JsonElement>,
    private val fallback: Map<String, JsonElement> = emptyMap(),
) {
    fun raw(key: String): String? =
        ((map[key] ?: fallback[key]) as? JsonPrimitive)?.contentOrNull

    fun str(key: String, feminine: Boolean): String =
        raw(key)?.let { Gender.resolve(it, feminine) } ?: key

    fun list(key: String, feminine: Boolean): List<String> {
        val arr = (map[key] ?: fallback[key]) as? JsonArray ?: return emptyList()
        return arr.map { Gender.resolve(it.jsonPrimitive.content, feminine) }
    }

    fun has(key: String): Boolean = map.containsKey(key) || fallback.containsKey(key)

    companion object {
        fun parse(json: String): Map<String, JsonElement> = Json.parseToJsonElement(json).jsonObject
    }
}

/** Replaces {name} style placeholders. */
fun String.fill(vararg args: Pair<String, Any?>): String {
    var out = this
    for ((k, v) in args) out = out.replace("{$k}", v?.toString() ?: "")
    return out
}
