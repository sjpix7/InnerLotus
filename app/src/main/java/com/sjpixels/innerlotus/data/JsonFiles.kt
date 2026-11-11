package com.sjpixels.innerlotus.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.concurrent.Executors

/** Small file + JSON helpers shared by the stores. Everything is local to the app sandbox. */
object JsonFiles {
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "InnerLotusIO").apply { isDaemon = true } }
    val pretty = Json { prettyPrint = true }

    fun read(file: File): JsonObject? = try {
        if (file.exists()) Json.parseToJsonElement(file.readText()).jsonObject else null
    } catch (e: Exception) {
        null
    }

    /** Atomic write on a background thread: write temp file, then rename over the target. */
    fun writeAsync(file: File, produce: () -> String) {
        io.execute { writeNow(file, produce()) }
    }

    fun writeNow(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    /** Blocks until previously queued writes have finished (used before export / in tests). */
    fun flush() {
        io.submit {}.get()
    }
}

// ---- tiny typed accessors for JsonObject -----------------------------------------------------

fun JsonObject.str(key: String, def: String = ""): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: def

fun JsonObject.long(key: String, def: Long = 0L): Long =
    (this[key] as? JsonPrimitive)?.longOrNull ?: def

fun JsonObject.int(key: String, def: Int = 0): Int =
    (this[key] as? JsonPrimitive)?.intOrNull ?: def

fun JsonObject.float(key: String, def: Float = 0f): Float =
    (this[key] as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: def

fun JsonObject.bool(key: String, def: Boolean = false): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: def

fun JsonObject.arr(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

fun JsonObject.obj(key: String): JsonObject = this[key] as? JsonObject ?: JsonObject(emptyMap())

fun JsonArray.strings(): List<String> = mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

fun JsonElement.asObj(): JsonObject? = this as? JsonObject

fun strArray(items: Iterable<String>): JsonArray = JsonArray(items.map { JsonPrimitive(it) })
