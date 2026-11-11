package com.sjpixels.innerlotus.data

/** Minimal RFC-4180 CSV reader/writer (quotes, commas and newlines inside quotes). */
object Csv {
    fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun row(vararg cells: String): String = cells.joinToString(",") { escape(it) }

    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        val t = text.removePrefix("\uFEFF")
        while (i < t.length) {
            val c = t[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < t.length && t[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
                } else cell.append(c)
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { row.add(cell.toString()); cell.setLength(0) }
                    '\r' -> {}
                    '\n' -> { row.add(cell.toString()); cell.setLength(0); rows.add(row); row = ArrayList() }
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}

/**
 * Reads affirmations from a user file. Supports:
 *  - Inner Lotus JSON export (handled by AffirmationStore.importJson)
 *  - CSV with an "affirmation" column and optional "area" column
 *  - plain text, one affirmation per line
 * Returns area -> lines; lines without an area go to [defaultArea].
 */
object AffirmationImport {
    fun parse(text: String, defaultArea: String, areaIds: Set<String>, areaNameToId: Map<String, String>): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()
        fun add(area: String, line: String) {
            val l = line.trim().trim('"').trim()
            if (l.isNotEmpty()) out.getOrPut(area) { ArrayList() }.add(l)
        }
        val firstLine = text.lineSequence().firstOrNull()?.lowercase().orEmpty()
        if (firstLine.contains("affirmation") && firstLine.contains(",")) {
            val rows = Csv.parse(text)
            val header = rows.first().map { it.trim().lowercase() }
            val aCol = header.indexOf("affirmation")
            val areaCol = header.indexOf("area")
            for (r in rows.drop(1)) {
                val line = r.getOrNull(aCol) ?: continue
                val rawArea = if (areaCol >= 0) r.getOrNull(areaCol)?.trim().orEmpty() else ""
                val area = when {
                    rawArea in areaIds -> rawArea
                    areaNameToId.containsKey(rawArea.lowercase()) -> areaNameToId.getValue(rawArea.lowercase())
                    else -> defaultArea
                }
                add(area, line)
            }
        } else {
            text.lineSequence().forEach { line ->
                val l = line.trim().removePrefix("-").removePrefix("•").trim()
                if (l.isNotEmpty() && !l.startsWith("#")) add(defaultArea, l)
            }
        }
        return out
    }
}
