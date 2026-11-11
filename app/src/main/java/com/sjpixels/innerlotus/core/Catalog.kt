package com.sjpixels.innerlotus.core

/**
 * Static catalog of ids used across the app. Display text for every id lives in the
 * i18n assets (e.g. "area.<id>.name"), so adding a language never touches this file.
 */
object Catalog {

    data class Area(val id: String, val emoji: String, val bg: String)

    val areas: List<Area> = listOf(
        Area("wealth", "💰", "dune"),
        Area("health", "🌿", "meadow"),
        Area("love", "💞", "cherry"),
        Area("relationships", "🤝", "garden"),
        Area("family", "👪", "autumn"),
        Area("career", "💼", "lagoon"),
        Area("business", "🚀", "wave"),
        Area("selflove", "🌸", "lotus"),
        Area("confidence", "🦁", "ocean"),
        Area("mental", "🧠", "lake"),
        Area("spirituality", "🕉️", "clouds"),
        Area("success", "🏆", "mountains"),
        Area("creativity", "🎨", "lavender"),
        Area("growth", "🌱", "botanical"),
        Area("gratitude", "🙏", "meadow"),
        Area("joy", "😊", "cherry"),
        Area("sleep", "🌙", "snow"),
        Area("beauty", "🌺", "garden"),
        Area("forgiveness", "🕊️", "clouds"),
        Area("productivity", "🎯", "bamboo"),
        Area("home", "🏠", "autumn"),
        Area("purpose", "🗺️", "mountains"),
        Area("parenting", "🤱", "meadow"),
        Area("friendship", "👫", "garden"),
        Area("travel", "✈️", "ocean"),
        Area("fertility", "🤰", "lotus"),
        Area("eating", "🥗", "botanical"),
        Area("recovery", "💪", "stream"),
        Area("communication", "💬", "lagoon"),
        Area("leadership", "👑", "dune"),
        Area("studies", "📚", "woodland"),
        Area("aging", "🌼", "autumn"),
        Area("luck", "🍀", "woodland"),
    )

    fun area(id: String): Area = areas.firstOrNull { it.id == id } ?: areas.first()

    /** Chakras from root (index 0) to crown (index 6). Colours follow the classic rainbow chart. */
    data class Chakra(
        val id: String,
        val index: Int,
        val color: Long,      // vivid colour for symbols
        val ink: Long,        // darker, readable text colour
        val toneHz: Float,    // Solfeggio tone used in the Chakra Guide
        val bg: String,
    )

    val chakras: List<Chakra> = listOf(
        Chakra("root", 0, 0xFFE53935, 0xFFC62828, 396f, "autumn"),
        Chakra("sacral", 1, 0xFFFB8C00, 0xFFE65100, 417f, "dune"),
        Chakra("solar", 2, 0xFFFDD835, 0xFFA67C00, 528f, "meadow"),
        Chakra("heart", 3, 0xFF43A047, 0xFF2E7D32, 639f, "botanical"),
        Chakra("throat", 4, 0xFF26C6DA, 0xFF00838F, 741f, "lagoon"),
        Chakra("thirdeye", 5, 0xFF3949AB, 0xFF283593, 852f, "lake"),
        Chakra("crown", 6, 0xFF8E24AA, 0xFF6A1B9A, 963f, "lavender"),
    )

    fun chakra(id: String?): Chakra? = chakras.firstOrNull { it.id == id }

    /** Background images bundled from theme-bg.zip (res/drawable-nodpi/bg_<id>.webp). */
    val backgrounds: List<String> = listOf(
        "lotus", "lavender", "cherry", "lake", "woodland", "stream", "mountains", "ocean", "bamboo",
        "wave", "clouds", "garden", "autumn", "botanical", "lagoon", "meadow", "snow", "dune",
    )

    val eftTopics = listOf(
        "anxiety", "stress", "anger", "sadness", "fear", "selfworth",
        "money", "sleep", "cravings", "procrastination", "pain", "hurt",
    )
    val eftTopicEmoji = mapOf(
        "anxiety" to "😟", "stress" to "🌀", "anger" to "😤", "sadness" to "😢", "fear" to "😨",
        "selfworth" to "🌟", "money" to "💸", "sleep" to "🌙", "cravings" to "🍫",
        "procrastination" to "⏳", "pain" to "🤕", "hurt" to "💔",
    )
    val eftTechniques = listOf("basic", "full", "gentle", "positive")

    /** Tapping sequence after the set-up (Karate Chop). */
    val eftSequence = listOf("th", "eb", "se", "ue", "un", "ch", "cb", "ua")

    val hooponoTopics = listOf(
        "self", "partner", "parents", "children", "friend", "work", "money", "health",
        "body", "home", "past", "ancestors", "earth", "anxiety", "person",
    )

    val moods = listOf(
        "joyful" to "😄", "calm" to "😌", "okay" to "🙂", "sad" to "😔", "anxious" to "😟", "angry" to "😠",
    )

    const val MANTRA_MALA_URL = "https://play.google.com/store/apps/details?id=com.sjpixels.mantramala"
    const val MANTRA_MALA_MARKET = "market://details?id=com.sjpixels.mantramala"
    const val DEVELOPER_URL = "https://sjpixels.com/"
    const val HELPLINE_URL = "https://findahelpline.com/"
}
