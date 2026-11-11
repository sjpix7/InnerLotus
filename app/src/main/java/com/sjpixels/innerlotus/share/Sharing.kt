package com.sjpixels.innerlotus.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.sjpixels.innerlotus.R
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.ui.art.ChakraPainter
import java.io.File
import java.io.FileOutputStream

/** Renders affirmation cards to images and handles share / save / copy / open-link actions. */
object Sharing {

    data class CardSpec(
        val core: String?,
        val main: String,
        val sub: String?,
        val label: String,
        val chakraIndex: Int?,
        val chakraColor: Int,
        val ink: Int,
        val bgRes: Int,
        val scriptFont: Boolean,
    )

    fun renderCard(context: Context, s: CardSpec, w: Int = 1080, h: Int = 1350): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(246, 241, 250))
        try {
            BitmapFactory.decodeResource(context.resources, s.bgRes)?.let { bg ->
                val scale = maxOf(w.toFloat() / bg.width, h.toFloat() / bg.height)
                val sw = (w / scale).toInt(); val sh = (h / scale).toInt()
                val sx = (bg.width - sw) / 2; val sy = (bg.height - sh) / 2
                c.drawBitmap(bg, Rect(sx, sy, sx + sw, sy + sh), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
                bg.recycle()
            }
        } catch (_: OutOfMemoryError) {
        }
        val panel = RectF(64f, 140f, w - 64f, h - 150f)
        c.drawRoundRect(panel, 64f, 64f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 255, 255) })

        val serifBold = font(context, R.font.cormorant_bold) ?: Typeface.SERIF
        val serif = font(context, R.font.cormorant_semibold) ?: Typeface.SERIF
        val script = if (s.scriptFont) font(context, R.font.dancing_script) else font(context, R.font.cormorant_italic)

        var y = panel.top + 70f
        if (s.chakraIndex != null) {
            ChakraPainter.draw(c, w / 2f, y + 110f, 120f, s.chakraIndex, s.chakraColor)
            y += 250f
        }
        val innerW = (panel.width() - 120f).toInt()
        if (!s.core.isNullOrBlank()) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = 84f; color = s.ink; letterSpacing = 0.06f }
            y += drawText(c, s.core, p, innerW, w / 2f, y, 1) + 24f
        }
        val bottomReserve = (if (s.sub.isNullOrBlank()) 0f else 170f) + 110f
        val avail = panel.bottom - bottomReserve - y
        var size = 70f
        val mainPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; color = Color.rgb(43, 34, 64) }
        while (size > 34f) {
            mainPaint.textSize = size
            if (layout(s.main, mainPaint, innerW).height <= avail) break
            size -= 3f
        }
        y += drawText(c, s.main, mainPaint, innerW, w / 2f, y, 8) + 30f
        if (!s.sub.isNullOrBlank()) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = script ?: Typeface.SERIF; textSize = if (s.scriptFont) 54f else 48f; color = s.ink }
            drawText(c, s.sub, p, innerW, w / 2f, y, 3)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = 34f; color = Color.argb(170, 60, 50, 90); letterSpacing = 0.12f }
        drawText(c, s.label.uppercase(), labelPaint, innerW, w / 2f, panel.bottom - 80f, 1)
        val foot = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; color = Color.argb(200, 70, 60, 100) }
        drawText(c, "Inner Lotus  ·  sjpixels.com", foot, w - 100, w / 2f, h - 100f, 1)
        return bmp
    }

    private fun font(context: Context, id: Int): Typeface? = try { ResourcesCompat.getFont(context, id) } catch (_: Exception) { null }

    private fun layout(text: String, p: TextPaint, width: Int, maxLines: Int = 12): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.08f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .build()

    /** Draws centred text with its top at [top]; returns the height used. */
    private fun drawText(c: Canvas, text: String, p: TextPaint, width: Int, centerX: Float, top: Float, maxLines: Int): Float {
        val l = layout(text, p, width, maxLines)
        c.save()
        c.translate(centerX - width / 2f, top)
        l.draw(c)
        c.restore()
        return l.height.toFloat()
    }

    fun bgRes(id: String): Int = when (id) {
        "lavender" -> R.drawable.bg_lavender
        "cherry" -> R.drawable.bg_cherry
        "lake" -> R.drawable.bg_lake
        "woodland" -> R.drawable.bg_woodland
        "stream" -> R.drawable.bg_stream
        "mountains" -> R.drawable.bg_mountains
        "ocean" -> R.drawable.bg_ocean
        "bamboo" -> R.drawable.bg_bamboo
        "wave" -> R.drawable.bg_wave
        "clouds" -> R.drawable.bg_clouds
        "garden" -> R.drawable.bg_garden
        "autumn" -> R.drawable.bg_autumn
        "botanical" -> R.drawable.bg_botanical
        "lagoon" -> R.drawable.bg_lagoon
        "meadow" -> R.drawable.bg_meadow
        "snow" -> R.drawable.bg_snow
        "dune" -> R.drawable.bg_dune
        else -> R.drawable.bg_lotus
    }

    fun shareImage(context: Context, bmp: Bitmap, text: String? = null) {
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val f = File(dir, "inner_lotus_card.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (text != null) putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, tr("ui.common.share")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast(context, tr("ui.common.error"))
        }
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        try {
            context.startActivity(Intent.createChooser(send, tr("ui.common.share")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    /** Android 10+: no permission needed. Android 8-9: caller must hold WRITE_EXTERNAL_STORAGE. */
    fun saveToGallery(context: Context, bmp: Bitmap, name: String): Boolean = try {
        val fileName = "$name.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/InnerLotus")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw IllegalStateException()
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "InnerLotus").apply { mkdirs() }
            val f = File(dir, fileName)
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), arrayOf("image/png"), null)
        }
        true
    } catch (e: Exception) {
        false
    }

    fun copy(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Inner Lotus", text))
        if (Build.VERSION.SDK_INT < 33) toast(context, tr("ui.common.copied"))
    }

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            toast(context, tr("ui.common.no_browser"))
        }
    }

    fun openMantraMala(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Catalog.MANTRA_MALA_MARKET)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            openUrl(context, Catalog.MANTRA_MALA_URL)
        }
    }

    fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // ---- Storage Access Framework helpers (export / import files chosen by the user) ----

    fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        true
    } catch (_: Exception) {
        false
    }

    fun readText(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: Exception) {
        null
    }
}
