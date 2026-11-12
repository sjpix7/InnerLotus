package com.sjpixels.innerlotus.ui.theme

import android.content.Context
import android.graphics.Typeface
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Typefaces for drawing on an Android [android.graphics.Canvas] (the shareable card image) from the
 * bundled *variable* fonts at an exact weight. Compose picks weights itself; the platform Canvas
 * needs a Typeface built with font-variation settings, which requires a file path, so the font is
 * copied to the cache once.
 */
object CanvasFonts {
    private val cache = ConcurrentHashMap<String, Typeface>()

    fun get(context: Context, resId: Int, weight: Int): Typeface {
        val key = "$resId@$weight"
        cache[key]?.let { return it }
        val tf = try {
            val file = File(context.cacheDir, "font_${resId}.ttf")
            if (!file.exists() || file.length() == 0L) {
                context.resources.openRawResource(resId).use { input -> file.outputStream().use { input.copyTo(it) } }
            }
            Typeface.Builder(file).setFontVariationSettings("'wght' $weight").build()
        } catch (_: Exception) {
            null
        } ?: Typeface.SERIF
        cache[key] = tf
        return tf
    }
}
