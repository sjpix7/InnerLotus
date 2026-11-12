package com.sjpixels.innerlotus.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Cyclone
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FlutterDash
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhonelinkOff
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every pictogram in the app: thin Material "Outlined" icons, never emoji.
 * Lookups take the same ids the data layer uses (area ids, EFT topics, mood ids, sound-library
 * ids, breath-pattern ids) and fall back to a gentle default so nothing is ever blank.
 */
object LotusIcons {

    // ---- App sections (Home tiles, sidebar) ----
    val Home: ImageVector = Icons.Outlined.Home
    val Affirmations: ImageVector = Icons.Outlined.AutoAwesome
    val Eft: ImageVector = Icons.Outlined.TouchApp
    val Sound: ImageVector = Icons.Outlined.MusicNote
    val Hoopono: ImageVector = Icons.Outlined.LocalFlorist
    val Journal: ImageVector = Icons.Outlined.AutoStories
    val Habits: ImageVector = Icons.Outlined.TaskAlt
    val Breath: ImageVector = Icons.Outlined.Air
    val Chakras: ImageVector = Icons.Outlined.FilterVintage
    val Calm: ImageVector = Icons.Outlined.Waves
    val Settings: ImageVector = Icons.Outlined.Settings
    val Lotus: ImageVector = Icons.Outlined.Spa

    fun section(key: String): ImageVector = when (key) {
        "home" -> Home
        "affirmations" -> Affirmations
        "eft" -> Eft
        "sound" -> Sound
        "hoopono" -> Hoopono
        "journal" -> Journal
        "habits" -> Habits
        "breath" -> Breath
        "chakras" -> Chakras
        "calm" -> Calm
        "settings" -> Settings
        else -> Lotus
    }

    /** "Learn" topics (LearnCard / Learn page). */
    fun learn(topic: String): ImageVector = when (topic) {
        "affirmations" -> Affirmations
        "eft" -> Eft
        "sound" -> Sound
        "hooponopono" -> Hoopono
        "journal" -> Journal
        "breath" -> Breath
        "chakra" -> Chakras
        else -> Icons.AutoMirrored.Outlined.MenuBook
    }

    // ---- 33 life areas ----
    fun area(id: String): ImageVector = when (id) {
        "wealth" -> Icons.Outlined.Savings
        "health" -> Icons.Outlined.Eco
        "love" -> Icons.Outlined.FavoriteBorder
        "relationships" -> Icons.Outlined.AllInclusive
        "family" -> Icons.Outlined.FamilyRestroom
        "career" -> Icons.Outlined.WorkOutline
        "business" -> Icons.Outlined.RocketLaunch
        "selflove" -> Icons.Outlined.Spa
        "confidence" -> Icons.Outlined.WbSunny
        "mental" -> Icons.Outlined.Psychology
        "spirituality" -> Icons.Outlined.SelfImprovement
        "success" -> Icons.Outlined.EmojiEvents
        "creativity" -> Icons.Outlined.Palette
        "growth" -> Icons.AutoMirrored.Outlined.TrendingUp
        "gratitude" -> Icons.Outlined.VolunteerActivism
        "joy" -> Icons.Outlined.Celebration
        "sleep" -> Icons.Outlined.Bedtime
        "beauty" -> Icons.Outlined.Diamond
        "forgiveness" -> Icons.Outlined.Healing
        "productivity" -> Icons.Outlined.TrackChanges
        "home" -> Icons.Outlined.Home
        "purpose" -> Icons.Outlined.Explore
        "parenting" -> Icons.Outlined.ChildCare
        "friendship" -> Icons.Outlined.People
        "travel" -> Icons.Outlined.Flight
        "fertility" -> Icons.Outlined.PregnantWoman
        "eating" -> Icons.Outlined.Restaurant
        "recovery" -> Icons.Outlined.FitnessCenter
        "communication" -> Icons.Outlined.ChatBubbleOutline
        "leadership" -> Icons.Outlined.Stars
        "studies" -> Icons.Outlined.School
        "aging" -> Icons.Outlined.HourglassEmpty
        "luck" -> Icons.Outlined.Grass
        else -> Affirmations
    }

    // ---- EFT topics ----
    fun eftTopic(id: String): ImageVector = when (id) {
        "anxiety" -> Icons.Outlined.Waves
        "stress" -> Icons.Outlined.Cyclone
        "anger" -> Icons.Outlined.Whatshot
        "sadness" -> Icons.Outlined.Cloud
        "fear" -> Icons.Outlined.Shield
        "selfworth" -> Icons.Outlined.FavoriteBorder
        "money" -> Icons.Outlined.Savings
        "sleep" -> Icons.Outlined.Bedtime
        "cravings" -> Icons.Outlined.Cookie
        "procrastination" -> Icons.Outlined.HourglassEmpty
        "pain" -> Icons.Outlined.Healing
        "hurt" -> Icons.Outlined.HeartBroken
        else -> Eft
    }

    // ---- Moods: a gentle weather metaphor ----
    fun mood(id: String): ImageVector = when (id) {
        "joyful" -> Icons.Outlined.WbSunny
        "calm" -> Icons.Outlined.Spa
        "okay" -> Icons.Outlined.FilterDrama
        "sad" -> Icons.Outlined.Umbrella
        "anxious" -> Icons.Outlined.Waves
        "angry" -> Icons.Outlined.Thunderstorm
        else -> Icons.Outlined.FilterDrama
    }

    // ---- Sound library ----
    fun sound(id: String): ImageVector = when (id) {
        "rain" -> Icons.Outlined.Umbrella
        "thunder" -> Icons.Outlined.Thunderstorm
        "ocean" -> Icons.Outlined.Waves
        "stream" -> Icons.Outlined.Water
        "wind" -> Icons.Outlined.Air
        "fire" -> Icons.Outlined.LocalFireDepartment
        "birds" -> Icons.Outlined.FlutterDash
        "crickets" -> Icons.Outlined.NightsStay
        "flute" -> Icons.Outlined.MusicNote
        "bamboo" -> Icons.Outlined.Forest
        "chimes" -> Icons.Outlined.Interests
        "bell" -> Icons.Outlined.Notifications
        "bowl" -> Icons.Outlined.RiceBowl
        "crystal" -> Icons.Outlined.Diamond
        "tanpura" -> Icons.Outlined.GraphicEq
        "kalimba" -> Icons.Outlined.Piano
        "om" -> Icons.Outlined.SelfImprovement
        "drum" -> Icons.Outlined.SurroundSound
        "heartbeat" -> Icons.Outlined.MonitorHeart
        "white", "pink", "brown" -> Icons.Outlined.Grain
        else -> Icons.Outlined.MusicNote
    }

    /** Healing frequency rows: plain tone vs binaural (headphones). */
    fun frequency(binaural: Boolean): ImageVector = if (binaural) Icons.Outlined.Headphones else Icons.Outlined.GraphicEq

    // ---- Breath patterns ----
    fun breath(id: String): ImageVector = when (id) {
        "box" -> Icons.Outlined.CheckBoxOutlineBlank
        "478" -> Icons.Outlined.Bedtime
        "coherent" -> Icons.Outlined.AllInclusive
        "sigh" -> Icons.Outlined.Air
        "nadi" -> Icons.Outlined.SyncAlt
        "bhramari" -> Icons.Outlined.EmojiNature
        "calm" -> Icons.Outlined.Spa
        else -> Breath
    }

    // ---- Habit icons ----
    // Habits store an icon key. Older versions stored an emoji, so emoji keys are mapped too.
    val habitKeys: List<String> = listOf(
        "sparkle", "meditate", "water", "run", "read", "write", "gratitude", "leaf", "moon", "sun",
        "food", "nosmoke", "nophone", "music", "breath", "tap", "heart", "strength", "sleep", "call",
        "walk", "mind", "create", "lotus",
    )

    private val legacyHabit = mapOf(
        "✨" to "sparkle", "🧘" to "meditate", "💧" to "water", "🏃" to "run", "📖" to "read", "📝" to "write",
        "🙏" to "gratitude", "🌿" to "leaf", "🌙" to "moon", "☀️" to "sun", "☀" to "sun", "🍎" to "food",
        "🥗" to "food", "🍬" to "food", "🚭" to "nosmoke", "📵" to "nophone", "🎵" to "music", "🌬️" to "breath",
        "🌬" to "breath", "👆" to "tap", "❤️" to "heart", "❤" to "heart", "💪" to "strength", "🛏️" to "sleep",
        "🛏" to "sleep", "📞" to "call", "🚶" to "walk", "🧠" to "mind", "🎨" to "create",
    )

    /** Normalises a stored habit icon (key or legacy emoji) to a key. */
    fun habitKey(stored: String): String = legacyHabit[stored] ?: stored.takeIf { it in habitKeys } ?: "sparkle"

    fun habit(stored: String): ImageVector = when (habitKey(stored)) {
        "meditate" -> Icons.Outlined.SelfImprovement
        "water" -> Icons.Outlined.WaterDrop
        "run" -> Icons.AutoMirrored.Outlined.DirectionsRun
        "read" -> Icons.AutoMirrored.Outlined.MenuBook
        "write" -> Icons.Outlined.EditNote
        "gratitude" -> Icons.Outlined.VolunteerActivism
        "leaf" -> Icons.Outlined.Eco
        "moon" -> Icons.Outlined.Bedtime
        "sun" -> Icons.Outlined.WbSunny
        "food" -> Icons.Outlined.Restaurant
        "nosmoke" -> Icons.Outlined.SmokeFree
        "nophone" -> Icons.Outlined.PhonelinkOff
        "music" -> Icons.Outlined.MusicNote
        "breath" -> Icons.Outlined.Air
        "tap" -> Icons.Outlined.TouchApp
        "heart" -> Icons.Outlined.FavoriteBorder
        "strength" -> Icons.Outlined.FitnessCenter
        "sleep" -> Icons.Outlined.Bed
        "call" -> Icons.Outlined.LocalPhone
        "walk" -> Icons.AutoMirrored.Outlined.DirectionsWalk
        "mind" -> Icons.Outlined.Psychology
        "create" -> Icons.Outlined.Brush
        "lotus" -> Icons.Outlined.Spa
        else -> Icons.Outlined.AutoAwesome
    }

    /** Icon for a suggested habit, by its position in the suggestion list. */
    val suggestionKeys: List<String> = listOf(
        "meditate", "water", "run", "read", "write", "sparkle", "gratitude", "walk", "sleep", "sun",
        "food", "food", "nophone", "meditate", "breath", "tap", "music", "gratitude", "nosmoke", "call",
    )

    // Misc
    val Voice: ImageVector = Icons.Outlined.RecordVoiceOver
    val Diversity: ImageVector = Icons.Outlined.Diversity3
}
