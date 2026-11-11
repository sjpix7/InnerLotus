package com.sjpixels.innerlotus.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * The user's own affirmations (per area), favourites and reading positions.
 * Built-in affirmations are generated from assets and never stored.
 *
 * Favourite keys:  "b|<area>|<chakra>|<line>|<seed>" for built-in cards (language-independent),
 *                  "c|<area>|<text>" for custom affirmations.
 */
class AffirmationStore(private val file: File) {

    data class State(
        val custom: Map<String, List<String>> = emptyMap(),
        val favorites: List<String> = emptyList(),
        val positions: Map<String, Int> = emptyMap(),
        val shuffleSeed: Long = 108L,
    )

    private val _state = MutableStateFlow(load())
    val state: StateFlow<State> = _state

    private fun load(): State {
        val o = JsonFiles.read(file) ?: return State()
        return fromJson(o)
    }

    private fun fromJson(o: JsonObject): State {
        val custom = o.obj("custom").mapValues { (_, v) -> (v as? kotlinx.serialization.json.JsonArray)?.strings() ?: emptyList() }
        val favs = o.arr("favorites").strings()
        val pos = o.obj("positions").mapNotNull { (k, v) -> (v as? JsonPrimitive)?.content?.toIntOrNull()?.let { k to it } }.toMap()
        return State(custom, favs, pos, o.long("shuffleSeed", 108L))
    }

    private fun toJson(s: State): JsonObject = buildJsonObject {
        put("app", "InnerLotus")
        put("type", "affirmations")
        put("version", 1)
        put("custom", JsonObject(s.custom.mapValues { strArray(it.value) }))
        put("favorites", strArray(s.favorites))
        put("positions", JsonObject(s.positions.mapValues { JsonPrimitive(it.value) }))
        put("shuffleSeed", s.shuffleSeed)
    }

    private fun update(f: (State) -> State) {
        val next = f(_state.value)
        _state.value = next
        JsonFiles.writeAsync(file) { toJson(next).toString() }
    }

    fun customFor(area: String): List<String> = _state.value.custom[area].orEmpty()

    fun setCustom(area: String, lines: List<String>) = update { s ->
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        s.copy(custom = s.custom + (area to clean))
    }

    /** Adds lines (deduplicated); returns how many were new. */
    fun addCustom(area: String, lines: List<String>): Int {
        var added = 0
        update { s ->
            val existing = s.custom[area].orEmpty()
            val set = existing.toMutableSet()
            val merged = existing.toMutableList()
            for (l in lines.map { it.trim() }.filter { it.isNotEmpty() }) {
                if (set.add(l)) { merged.add(l); added++ }
            }
            s.copy(custom = s.custom + (area to merged))
        }
        return added
    }

    fun isFavorite(key: String): Boolean = key in _state.value.favorites

    fun toggleFavorite(key: String) = update { s ->
        if (key in s.favorites) s.copy(favorites = s.favorites - key) else s.copy(favorites = s.favorites + key)
    }

    fun setPosition(deckKey: String, pos: Int) {
        if (_state.value.positions[deckKey] == pos) return
        update { s -> s.copy(positions = s.positions + (deckKey to pos)) }
    }

    fun reshuffle() = update { s -> s.copy(shuffleSeed = s.shuffleSeed + 1, positions = emptyMap()) }

    fun clearAll() = update { State() }

    /** Export the user's affirmations (+ favourites). [areas] null = everything. */
    fun exportJson(areas: Collection<String>? = null): String {
        val s = _state.value
        val filtered = if (areas == null) s else s.copy(
            custom = s.custom.filterKeys { it in areas },
            favorites = s.favorites.filter { f -> areas.any { f.startsWith("b|$it|") || f.startsWith("c|$it|") } },
            positions = emptyMap(),
        )
        return JsonFiles.pretty.encodeToString(JsonObject.serializer(), toJson(filtered))
    }

    fun toJsonObject(): JsonObject = toJson(_state.value)

    /** Merges an exported file. Returns number of affirmations added. */
    fun importJson(text: String): Int {
        val o = Json.parseToJsonElement(text).jsonObject
        val src = if (o.containsKey("affirmations")) o.obj("affirmations") else o
        val other = fromJson(src)
        var added = 0
        for ((area, lines) in other.custom) added += addCustom(area, lines)
        update { s -> s.copy(favorites = (s.favorites + other.favorites).distinct()) }
        return added
    }
}
