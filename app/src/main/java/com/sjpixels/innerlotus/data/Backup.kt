package com.sjpixels.innerlotus.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One-file backup of everything the user created (no settings secrets such as the PIN). */
object Backup {
    data class Result(val affirmations: Int, val journal: Int, val habits: Int, val mixes: Int)

    fun export(aff: AffirmationStore, journal: JournalStore, habits: HabitStore, mixes: MixStore): String {
        val o = buildJsonObject {
            put("app", "InnerLotus")
            put("type", "backup")
            put("version", 1)
            put("created", System.currentTimeMillis())
            put("affirmations", aff.toJsonObject())
            put("journal", journal.toJsonObject())
            put("habits", habits.toJsonObject())
            put("mixes", mixes.toJsonObject())
        }
        return JsonFiles.pretty.encodeToString(JsonObject.serializer(), o)
    }

    /** Accepts a full backup or any single-section export. */
    fun import(text: String, aff: AffirmationStore, journal: JournalStore, habits: HabitStore, mixes: MixStore): Result =
        Result(
            affirmations = runCatching { aff.importJson(text) }.getOrDefault(0),
            journal = runCatching { journal.importJson(text) }.getOrDefault(0),
            habits = runCatching { habits.importJson(text) }.getOrDefault(0),
            mixes = runCatching { mixes.importJson(text) }.getOrDefault(0),
        )
}
