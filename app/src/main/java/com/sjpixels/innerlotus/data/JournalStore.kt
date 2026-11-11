package com.sjpixels.innerlotus.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val created: Long = System.currentTimeMillis(),
    val updated: Long = created,
    /** "free", "gratitude" or "prompt" */
    val mode: String = "free",
    val prompt: String = "",
    val text: String = "",
    /** mood id from Catalog.moods, or "" */
    val mood: String = "",
)

/** Private journal, stored only in the app's internal files directory. */
class JournalStore(private val file: File) {

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<JournalEntry>> = _entries

    private fun load(): List<JournalEntry> {
        val o = JsonFiles.read(file) ?: return emptyList()
        return parseEntries(o.arr("entries")).sortedByDescending { it.created }
    }

    private fun parseEntries(arr: JsonArray): List<JournalEntry> = arr.mapNotNull { el ->
        val e = el.asObj() ?: return@mapNotNull null
        val id = e.str("id").ifEmpty { return@mapNotNull null }
        JournalEntry(
            id = id,
            created = e.long("created", System.currentTimeMillis()),
            updated = e.long("updated", e.long("created")),
            mode = e.str("mode", "free"),
            prompt = e.str("prompt"),
            text = e.str("text"),
            mood = e.str("mood"),
        )
    }

    private fun entryJson(e: JournalEntry): JsonObject = buildJsonObject {
        put("id", e.id); put("created", e.created); put("updated", e.updated)
        put("mode", e.mode); put("prompt", e.prompt); put("text", e.text); put("mood", e.mood)
    }

    private fun toJson(list: List<JournalEntry>): JsonObject = buildJsonObject {
        put("app", "InnerLotus")
        put("type", "journal")
        put("version", 1)
        put("entries", JsonArray(list.map { entryJson(it) }))
    }

    private fun commit(list: List<JournalEntry>) {
        val sorted = list.sortedByDescending { it.created }
        _entries.value = sorted
        JsonFiles.writeAsync(file) { toJson(sorted).toString() }
    }

    fun get(id: String): JournalEntry? = _entries.value.firstOrNull { it.id == id }

    fun upsert(e: JournalEntry) {
        val list = _entries.value.filterNot { it.id == e.id } + e.copy(updated = System.currentTimeMillis())
        commit(list)
    }

    fun delete(id: String) = commit(_entries.value.filterNot { it.id == id })

    fun deleteWhere(pred: (JournalEntry) -> Boolean): Int {
        val before = _entries.value.size
        commit(_entries.value.filterNot(pred))
        return before - _entries.value.size
    }

    fun deleteAll() = commit(emptyList())

    fun exportJson(list: List<JournalEntry> = _entries.value): String =
        JsonFiles.pretty.encodeToString(JsonObject.serializer(), toJson(list))

    fun toJsonObject(): JsonObject = toJson(_entries.value)

    /** Plain-text export for reading or printing. [dateLabel] formats a timestamp. */
    fun exportText(list: List<JournalEntry>, dateLabel: (Long) -> String, moodLabel: (String) -> String): String =
        buildString {
            for (e in list.sortedBy { it.created }) {
                append("── ").append(dateLabel(e.created))
                if (e.mood.isNotEmpty()) append("  ·  ").append(moodLabel(e.mood))
                append(" ──\n")
                if (e.prompt.isNotBlank()) append("» ").append(e.prompt).append("\n")
                append(e.text.trim()).append("\n\n")
            }
        }

    /** Merge entries from an exported JSON file (or a full backup). Returns number imported. */
    fun importJson(text: String): Int {
        val o = Json.parseToJsonElement(text).jsonObject
        val src = if (o.containsKey("journal")) o.obj("journal") else o
        val incoming = parseEntries(src.arr("entries"))
        val existing = _entries.value.associateBy { it.id }
        val fresh = incoming.filter { it.id !in existing }
        if (fresh.isNotEmpty()) commit(_entries.value + fresh)
        return fresh.size
    }
}
