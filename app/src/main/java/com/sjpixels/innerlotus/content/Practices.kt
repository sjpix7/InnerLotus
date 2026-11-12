package com.sjpixels.innerlotus.content

/** Breathing patterns. Phase cue keys map to "breath.cue.<cue>" strings. */
object Breath {
    enum class Kind { IN, HOLD, OUT }

    data class Phase(val cue: String, val seconds: Float, val kind: Kind)

    val patterns: Map<String, List<Phase>> = linkedMapOf(
        "box" to listOf(
            Phase("inhale", 4f, Kind.IN), Phase("hold", 4f, Kind.HOLD),
            Phase("exhale", 4f, Kind.OUT), Phase("hold", 4f, Kind.HOLD),
        ),
        "478" to listOf(
            Phase("inhale", 4f, Kind.IN), Phase("hold", 7f, Kind.HOLD), Phase("exhale", 8f, Kind.OUT),
        ),
        "coherent" to listOf(Phase("inhale", 5.5f, Kind.IN), Phase("exhale", 5.5f, Kind.OUT)),
        "sigh" to listOf(
            Phase("inhale", 2.5f, Kind.IN), Phase("topup", 1f, Kind.IN), Phase("exhale", 6f, Kind.OUT),
        ),
        "nadi" to listOf(
            Phase("in_left", 4f, Kind.IN), Phase("out_right", 6f, Kind.OUT),
            Phase("in_right", 4f, Kind.IN), Phase("out_left", 6f, Kind.OUT),
        ),
        "bhramari" to listOf(Phase("inhale", 4f, Kind.IN), Phase("hum", 8f, Kind.OUT)),
        "calm" to listOf(Phase("inhale", 4f, Kind.IN), Phase("exhale", 6f, Kind.OUT)),
    )

    fun cycleSeconds(id: String): Float = patterns[id]?.sumOf { it.seconds.toDouble() }?.toFloat() ?: 10f
}

/** Builds the Ho'oponopono prayer text from translated parts. */
object Prayer {
    fun build(
        opening: String,
        intro: String,
        focus: String,
        phrases: List<String>,
        closing: String,
        name: String?,
        rounds: Int = 3,
    ): String {
        val who = name?.takeIf { it.isNotBlank() }?.trim()
        val introText = if (who != null) intro.replace("{name}", who) else intro
        val sb = StringBuilder()
        sb.append(opening).append("\n\n")
        sb.append(introText).append("\n")
        sb.append(focus).append("\n\n")
        repeat(rounds.coerceAtLeast(1)) {
            sb.append(phrases.joinToString("\n")).append("\n\n")
        }
        sb.append(closing)
        return sb.toString().trim()
    }
}
