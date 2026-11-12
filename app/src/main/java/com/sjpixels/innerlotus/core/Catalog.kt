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

    /**
     * Chakras from root (index 0) to crown (index 6). Colours follow the classic rainbow order,
     * softened to the app's pastel palette (dusty rose → terracotta → honey → sage → mist → periwinkle → mauve).
     */
    data class Chakra(
        val id: String,
        val index: Int,
        val color: Long,      // soft symbol colour
        val ink: Long,        // deeper, readable text colour
        val toneHz: Float,    // Solfeggio tone used in the Chakra Guide
        val bg: String,
    )

    val chakras: List<Chakra> = listOf(
        Chakra("root", 0, 0xFFC68891, 0xFF9C5562, 396f, "autumn"),
        Chakra("sacral", 1, 0xFFD59E7E, 0xFFA56645, 417f, "dune"),
        Chakra("solar", 2, 0xFFD2B36F, 0xFF8E6F25, 528f, "meadow"),
        Chakra("heart", 3, 0xFF8FAE97, 0xFF557E61, 639f, "botanical"),
        Chakra("throat", 4, 0xFF88B6C4, 0xFF477F91, 741f, "lagoon"),
        Chakra("thirdeye", 5, 0xFF8990C3, 0xFF555C9A, 852f, "lake"),
        Chakra("crown", 6, 0xFFAB8DBB, 0xFF7A5890, 963f, "lavender"),
    )

    fun chakra(id: String?): Chakra? = chakras.firstOrNull { it.id == id }

    /**
     * Background images (res/drawable-nodpi/bg_<id>.webp). "dawn" — a glass lotus on still water at
     * dawn — is the signature image: default background and welcome-screen art.
     */
    const val SIGNATURE_BG = "dawn"
    val backgrounds: List<String> = listOf(
        "dawn", "lotus", "lavender", "cherry", "lake", "woodland", "stream", "mountains", "ocean", "bamboo",
        "wave", "clouds", "garden", "autumn", "botanical", "lagoon", "meadow", "snow", "dune",
    )

    val eftTopics = listOf(
        "anxiety", "stress", "anger", "sadness", "fear", "selfworth",
        "money", "sleep", "cravings", "procrastination", "pain", "hurt",
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
    const val DEVELOPER_NAME = "Sj Pixels"
    const val HELPLINE_URL = "https://findahelpline.com/"
}
