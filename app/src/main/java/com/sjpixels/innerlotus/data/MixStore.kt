package com.sjpixels.innerlotus.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

/** A saved soundscape: layer id ("freq:f528", "lib:rain") -> volume 0..1. */
data class Mix(val id: String = UUID.randomUUID().toString(), val name: String, val layers: Map<String, Float>)

class MixStore(private val file: File) {
    private val _mixes = MutableStateFlow(load())
    val mixes: StateFlow<List<Mix>> = _mixes

    private fun load(): List<Mix> = JsonFiles.read(file)?.let { parse(it) } ?: emptyList()

    private fun parse(o: JsonObject): List<Mix> = o.arr("mixes").mapNotNull { el ->
        val m = el.asObj() ?: return@mapNotNull null
        val layers = m.obj("layers").mapNotNull { (k, v) ->
            (v as? JsonPrimitive)?.content?.toFloatOrNull()?.let { k to it.coerceIn(0f, 1f) }
        }.toMap()
        if (layers.isEmpty()) null else Mix(m.str("id").ifEmpty { UUID.randomUUID().toString() }, m.str("name", "Mix"), layers)
    }

    private fun toJson(list: List<Mix>): JsonObject = buildJsonObject {
        put("app", "InnerLotus")
        put("type", "mixes")
        put("version", 1)
        put("mixes", JsonArray(list.map { m ->
            buildJsonObject {
                put("id", m.id); put("name", m.name)
                put("layers", JsonObject(m.layers.mapValues { JsonPrimitive(it.value) }))
            }
        }))
    }

    private fun commit(list: List<Mix>) {
        _mixes.value = list
        JsonFiles.writeAsync(file) { toJson(list).toString() }
    }

    fun save(name: String, layers: Map<String, Float>) = commit(_mixes.value + Mix(name = name.trim().ifEmpty { "Mix" }, layers = layers))
    fun delete(id: String) = commit(_mixes.value.filterNot { it.id == id })
    fun clearAll() = commit(emptyList())
    fun toJsonObject(): JsonObject = toJson(_mixes.value)

    fun importJson(text: String): Int {
        val o = Json.parseToJsonElement(text).jsonObject
        val src = if (o.containsKey("mixes") && o["mixes"] is JsonObject) o.obj("mixes") else o
        val ids = _mixes.value.map { it.id }.toSet()
        val fresh = parse(src).filter { it.id !in ids }
        if (fresh.isNotEmpty()) commit(_mixes.value + fresh)
        return fresh.size
    }
}
