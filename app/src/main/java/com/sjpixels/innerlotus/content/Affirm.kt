package com.sjpixels.innerlotus.content

import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import java.time.LocalDate

/** Turns deck references into displayable, translated affirmation cards. */
object Affirm {
    const val LINES = 24
    const val SEEDS = 6

    data class Card(
        val area: String,
        val chakra: String?,
        val core: String?,
        val main: String,
        val sub: String?,
        val favKey: String,
        val custom: Boolean,
    )

    fun lines(area: String): List<String> = I18n.list("area.$area.lines")
    fun seeds(chakra: String): List<String> = I18n.list("chakra.$chakra.seeds")

    fun builtIn(area: String, ref: Composer.CardRef): Card {
        val l = lines(area)
        val s = seeds(ref.chakra)
        return Card(
            area = area,
            chakra = ref.chakra,
            core = tr("chakra.${ref.chakra}.core"),
            main = l.getOrElse(ref.line) { l.firstOrNull().orEmpty() },
            sub = s.getOrElse(ref.seed) { s.firstOrNull() },
            favKey = "b|$area|${ref.chakra}|${ref.line}|${ref.seed}",
            custom = false,
        )
    }

    fun custom(area: String, text: String, chakra: String?): Card = Card(
        area = area,
        chakra = chakra,
        core = chakra?.let { tr("chakra.$it.core") },
        main = text,
        sub = null,
        favKey = "c|$area|$text",
        custom = true,
    )

    /** Rebuilds a card from a favourite key (works after a language switch). */
    fun fromKey(key: String): Card? {
        val p = key.split("|", limit = 5)
        return when (p.getOrNull(0)) {
            "b" -> if (p.size == 5) builtIn(p[1], Composer.CardRef(p[2], p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0)) else null
            "c" -> if (p.size >= 3) custom(p[1], key.substringAfter("c|${p[1]}|"), null) else null
            else -> null
        }
    }

    fun daily(day: Long = LocalDate.now().toEpochDay()): Card {
        val (areaIdx, ref, _) = Composer.dailyPick(day, Catalog.areas.size, LINES, SEEDS)
        return builtIn(Catalog.areas[areaIdx].id, ref)
    }

    fun speakText(c: Card, includeCore: Boolean): String = buildString {
        val code = I18n.lang.code
        val cjk = code == "zh" || code == "ja"
        if (includeCore && !c.core.isNullOrBlank()) {
            val locale = I18n.lang.locale
            append(c.core.lowercase(locale).replaceFirstChar { it.titlecase(locale) })
            append(
                when {
                    cjk -> "。"
                    code == "hi" -> "। "
                    else -> ". "
                }
            )
        }
        append(c.main)
        if (!c.sub.isNullOrBlank()) append(if (cjk) "" else " ").append(c.sub)
    }

    fun plainText(c: Card): String = buildString {
        if (!c.core.isNullOrBlank()) append(c.core).append("\n")
        append(c.main)
        if (!c.sub.isNullOrBlank()) append("\n").append(c.sub)
    }
}
