package com.sjpixels.innerlotus.core

import java.util.Locale

/**
 * The nine supported languages. [gendered] languages use `{masc|fem}` markup in content
 * (resolved by the "form of address" setting). [scriptFont] tells the UI whether the
 * Dancing Script face can render this language (Latin only).
 */
enum class Lang(
    val code: String,
    val flag: String,
    val nativeName: String,
    val englishName: String,
    val tag: String,
    val gendered: Boolean,
    val scriptFont: Boolean,
) {
    EN("en", "🇬🇧", "English", "English", "en-US", false, true),
    HI("hi", "🇮🇳", "हिन्दी", "Hindi", "hi-IN", true, false),
    RU("ru", "🇷🇺", "Русский", "Russian", "ru-RU", true, false),
    KO("ko", "🇰🇷", "한국어", "Korean", "ko-KR", false, false),
    ZH("zh", "🇨🇳", "中文", "Chinese", "zh-CN", false, false),
    ES("es", "🇪🇸", "Español", "Spanish", "es-ES", true, true),
    FR("fr", "🇫🇷", "Français", "French", "fr-FR", true, true),
    IT("it", "🇮🇹", "Italiano", "Italian", "it-IT", true, true),
    JA("ja", "🇯🇵", "日本語", "Japanese", "ja-JP", false, false);

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        fun of(code: String?): Lang = entries.firstOrNull { it.code == code } ?: EN

        /** Best match for the phone's language on first launch. */
        fun fromSystem(locale: Locale = Locale.getDefault()): Lang =
            entries.firstOrNull { it.code == locale.language } ?: EN
    }
}
