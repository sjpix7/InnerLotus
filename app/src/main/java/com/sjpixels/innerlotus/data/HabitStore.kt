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
import java.time.LocalDate
import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String = "✨",
    val color: Long = 0xFF8A5CD6,
    /** times per day; 1 = simple check-off */
    val target: Int = 1,
    /** ISO days 1 (Mon) .. 7 (Sun); empty = every day */
    val days: Set<Int> = emptySet(),
    val created: Long = LocalDate.now().toEpochDay(),
    val order: Int = 0,
)

data class HabitState(
    val habits: List<Habit> = emptyList(),
    /** habitId -> (epochDay -> count) */
    val logs: Map<String, Map<Long, Int>> = emptyMap(),
)

class HabitStore(private val file: File) {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<HabitState> = _state

    private fun load(): HabitState {
        val o = JsonFiles.read(file) ?: return HabitState()
        return fromJson(o)
    }

    private fun fromJson(o: JsonObject): HabitState {
        val habits = o.arr("habits").mapNotNull { el ->
            val h = el.asObj() ?: return@mapNotNull null
            Habit(
                id = h.str("id").ifEmpty { UUID.randomUUID().toString() },
                name = h.str("name").ifEmpty { return@mapNotNull null },
                emoji = h.str("emoji", "✨"),
                color = h.long("color", 0xFF8A5CD6),
                target = h.int("target", 1).coerceIn(1, 99),
                days = h.arr("days").strings().mapNotNull { it.toIntOrNull() }.filter { it in 1..7 }.toSet(),
                created = h.long("created", LocalDate.now().toEpochDay()),
                order = h.int("order", 0),
            )
        }.sortedBy { it.order }
        val logs = o.obj("logs").mapValues { (_, v) ->
            (v as? JsonObject)?.mapNotNull { (d, c) ->
                val day = d.toLongOrNull() ?: return@mapNotNull null
                val n = (c as? JsonPrimitive)?.content?.toIntOrNull() ?: return@mapNotNull null
                day to n
            }?.toMap() ?: emptyMap()
        }
        return HabitState(habits, logs)
    }

    private fun toJson(s: HabitState): JsonObject = buildJsonObject {
        put("app", "InnerLotus")
        put("type", "habits")
        put("version", 1)
        put("habits", JsonArray(s.habits.map { h ->
            buildJsonObject {
                put("id", h.id); put("name", h.name); put("emoji", h.emoji); put("color", h.color)
                put("target", h.target); put("days", strArray(h.days.sorted().map { it.toString() }))
                put("created", h.created); put("order", h.order)
            }
        }))
        put("logs", JsonObject(s.logs.mapValues { (_, m) ->
            JsonObject(m.filterValues { it > 0 }.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) })
        }))
    }

    private fun update(f: (HabitState) -> HabitState) {
        val next = f(_state.value)
        _state.value = next
        JsonFiles.writeAsync(file) { toJson(next).toString() }
    }

    fun addHabit(h: Habit) = update { s ->
        s.copy(habits = s.habits + h.copy(order = (s.habits.maxOfOrNull { it.order } ?: -1) + 1))
    }

    fun updateHabit(h: Habit) = update { s -> s.copy(habits = s.habits.map { if (it.id == h.id) h else it }) }

    fun deleteHabit(id: String) = update { s -> s.copy(habits = s.habits.filterNot { it.id == id }, logs = s.logs - id) }

    fun move(id: String, delta: Int) = update { s ->
        val list = s.habits.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@update s
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        s.copy(habits = list.mapIndexed { idx, h -> h.copy(order = idx) })
    }

    fun count(id: String, day: Long): Int = _state.value.logs[id]?.get(day) ?: 0

    fun setCount(id: String, day: Long, n: Int) = update { s ->
        val m = s.logs[id].orEmpty().toMutableMap()
        if (n <= 0) m.remove(day) else m[day] = n.coerceAtMost(999)
        s.copy(logs = s.logs + (id to m))
    }

    fun increment(id: String, day: Long, delta: Int) = setCount(id, day, count(id, day) + delta)

    fun clearAll() = update { HabitState() }

    fun exportJson(): String = JsonFiles.pretty.encodeToString(JsonObject.serializer(), toJson(_state.value))

    fun toJsonObject(): JsonObject = toJson(_state.value)

    /** Merge habits and logs from an exported file. Returns number of habits added. */
    fun importJson(text: String): Int {
        val o = Json.parseToJsonElement(text).jsonObject
        val src = if (o.containsKey("habits") && o["habits"] is JsonObject) o.obj("habits") else o
        val other = fromJson(src)
        var added = 0
        update { s ->
            val ids = s.habits.map { it.id }.toSet()
            val newHabits = other.habits.filter { it.id !in ids }
            added = newHabits.size
            val logs = s.logs.toMutableMap()
            for ((hid, m) in other.logs) logs[hid] = logs[hid].orEmpty() + m
            s.copy(habits = s.habits + newHabits.mapIndexed { i, h -> h.copy(order = s.habits.size + i) }, logs = logs)
        }
        return added
    }
}
