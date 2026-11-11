# Inner Lotus uses no reflection-based serialization (kotlinx.serialization tree API only),
# so the default Android optimize rules are enough. Keep the foreground service and receivers
# referenced from the manifest (AGP keeps them automatically; listed here for clarity).
-keep class com.sjpixels.innerlotus.audio.SoundService { *; }
-keep class com.sjpixels.innerlotus.reminders.** { *; }
