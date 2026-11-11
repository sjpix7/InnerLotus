package com.sjpixels.innerlotus.content

import kotlin.random.Random

/**
 * Builds the affirmation decks.
 *
 * Each area has L hand-written area lines (24) and each chakra has S seed lines (6). For one
 * chakra the deck is every (line, seed) pair = L*S = 144 cards; over all seven chakras that is
 * 1008 unique cards per area. Card k of a chakra deck maps to
 *     line = k % L,  seed = (k / L + k) % S
 * which visits every pair exactly once and never repeats the same area line twice in a row.
 */
object Composer {
    val CHAKRAS = listOf("root", "sacral", "solar", "heart", "throat", "thirdeye", "crown")

    data class CardRef(val chakra: String, val line: Int, val seed: Int) {
        val key: String get() = "$chakra:$line:$seed"
    }

    fun chakraDeck(chakra: String, lines: Int, seeds: Int): List<CardRef> {
        if (lines <= 0 || seeds <= 0) return emptyList()
        return List(lines * seeds) { k -> CardRef(chakra, k % lines, (k / lines + k) % seeds) }
    }

    /**
     * @param chakra a chakra id, or null for "random" (all seven chakras, 1008 cards, shuffled).
     * @param shuffleSeed when non-null the deck is shuffled deterministically, so the saved
     *        position stays meaningful between sessions.
     */
    fun deck(chakra: String?, lines: Int, seeds: Int, shuffleSeed: Long?): List<CardRef> {
        return if (chakra != null) {
            val d = chakraDeck(chakra, lines, seeds)
            if (shuffleSeed == null) d else d.shuffled(Random(shuffleSeed))
        } else {
            // Interleave chakras root..crown so even an unshuffled "all" deck rises through them.
            val decks = CHAKRAS.map { chakraDeck(it, lines, seeds) }
            val size = decks.firstOrNull()?.size ?: 0
            val all = ArrayList<CardRef>(size * CHAKRAS.size)
            for (k in 0 until size) for (d in decks) all.add(d[k])
            all.shuffled(Random(shuffleSeed ?: 1008L))
        }
    }

    /** Stable "affirmation of the day" for a given epoch day. */
    fun dailyPick(epochDay: Long, areaCount: Int, lines: Int, seeds: Int): Triple<Int, CardRef, Int> {
        val rnd = Random(epochDay * 31 + 7)
        val area = rnd.nextInt(areaCount)
        val chakraIdx = rnd.nextInt(CHAKRAS.size)
        val ref = CardRef(CHAKRAS[chakraIdx], rnd.nextInt(lines), rnd.nextInt(seeds))
        return Triple(area, ref, chakraIdx)
    }
}
