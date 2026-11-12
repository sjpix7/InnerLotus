package com.sjpixels.innerlotus.ui.glass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sjpixels.innerlotus.share.Sharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Frosted glass that works on every Android version.
 *
 * The app always shows one pastel background image (sometimes crossfading to the next). Instead of a
 * costly live backdrop blur, each glass surface draws the matching slice of a pre-blurred, tiny copy
 * of that same image, aligned to where the surface sits on screen, then a white tint, an optional
 * colour wash and a light-catching hairline edge. The result reads as real frosted glass, follows
 * scrolling and the background crossfade, and costs one small bitmap draw per surface.
 */
object Backdrop {
    /** Background currently shown, and the one fading out (null when no crossfade is running). */
    var current by mutableStateOf("dawn")
        internal set
    var previous by mutableStateOf<String?>(null)
        internal set

    /** Crossfade progress 0..1 from [previous] to [current]. */
    internal val progress = Animatable(1f)

    /** Size of the full-screen background, in px (the coordinate space glass aligns to). */
    var size by mutableStateOf(IntSize.Zero)
        internal set

    /** Plain white veil drawn over the background photo (kept identical in glass). */
    val veil = Color(0x26FFFFFF)

    const val FADE_MS = 2200

    // Blurred copies: key "<id>@<radius>" -> bitmap (decoded at 1/8 size, so they are tiny).
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val ready = mutableStateMapOf<String, ImageBitmap>()

    /** Sample size used for the blurred copies (the source art is 941×1672). */
    private const val SAMPLE = 8

    internal fun blurred(id: String, radius: Int): ImageBitmap? = ready["$id@$radius"]

    /** Radii used by the glass materials; all are prepared whenever the background changes. */
    internal val radii = listOf(6, 8, 10, 12)

    /** The copy at [radius], or the nearest prepared one while it is still being made. */
    internal fun blurredAny(id: String, radius: Int): ImageBitmap? =
        ready["$id@$radius"] ?: radii.sortedBy { kotlin.math.abs(it - radius) }.firstNotNullOfOrNull { ready["$id@$it"] }

    internal suspend fun ensure(context: Context, id: String, radius: Int) {
        val key = "$id@$radius"
        if (ready.containsKey(key)) return
        cache.get(key)?.let { ready[key] = it; return }
        val img = withContext(Dispatchers.Default) {
            try {
                val o = BitmapFactory.Options().apply { inSampleSize = SAMPLE; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val src = BitmapFactory.decodeResource(context.resources, Sharing.bgRes(id), o) ?: return@withContext null
                val out = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
                FastBlur.blur(out, radius)
                out.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        } ?: return
        cache.put(key, img)
        ready[key] = img
    }

    /** Called by the background when it switches image. */
    internal suspend fun switchTo(id: String) {
        if (id == current) return
        previous = current
        current = id
        progress.snapTo(0f)
        progress.animateTo(1f, tween(FADE_MS, easing = LinearEasing))
        previous = null
    }

    internal fun setInitial(id: String) {
        current = id
        previous = null
    }
}

/** Parameters of one glass material. Alphas are baked into the colours. */
data class GlassMaterial(
    /** Blur radius in pixels of the 1/8-size copy (1 px ≈ 3–4 dp on screen). */
    val blur: Int,
    val tintTop: Color,
    val tintBottom: Color,
    /** Optional colour wash over the tint (Color.Transparent for none). */
    val wash: Color = Color.Transparent,
    val edgeTop: Color = Color.White.copy(alpha = 0.70f),
    val edgeBottom: Color = Color.White.copy(alpha = 0.18f),
    val edgeWidth: Dp = 1.dp,
)

/** Pre-warms the blurred copies for [ids] so glass appears without a flash. */
@Composable
fun PrewarmBackdrop(ids: List<String>, radii: List<Int>) {
    val ctx = LocalContext.current
    LaunchedEffect(ids, radii) {
        for (id in ids) for (r in radii) Backdrop.ensure(ctx.applicationContext, id, r)
    }
}

/**
 * Draws [material] glass behind this element's content. The caller clips to the shape
 * (GlassSurface applies Modifier.clip(shape)); [shape] is used for the light-catching edge.
 *
 * Implemented as a lightweight Modifier.Node: no composition per surface, brushes cached per
 * size, and a redraw only when the element actually moves on screen (scrolling) or the
 * background changes.
 */
fun Modifier.glass(material: GlassMaterial, shape: Shape = RectangleShape): Modifier = this then GlassElement(material, shape)

private data class GlassElement(val material: GlassMaterial, val shape: Shape) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(material, shape)
    override fun update(node: GlassNode) = node.update(material, shape)
}

private class GlassNode(var material: GlassMaterial, var shape: Shape) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var pos = Offset.Zero
    private var cachedFor: Size = Size.Unspecified
    private var tint: Brush? = null
    private var edge: Brush? = null
    private var outline: Outline? = null

    fun update(m: GlassMaterial, sh: Shape) {
        if (m != material || sh != shape) {
            material = m; shape = sh; cachedFor = Size.Unspecified
            invalidateDraw()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // Screen position, so glass inside dialogs/popups (separate windows) also lines up.
        val p = coordinates.positionOnScreen()
        if (p != pos) { pos = p; invalidateDraw() }
    }

    override fun ContentDrawScope.draw() {
        if (size != cachedFor) {
            cachedFor = size
            // Veil and tint merged into one layer (one fill instead of two).
            tint = Brush.verticalGradient(listOf(over(material.tintTop, Backdrop.veil), over(material.tintBottom, Backdrop.veil)))
            edge = Brush.linearGradient(listOf(material.edgeTop, material.edgeBottom), start = Offset.Zero, end = Offset(size.width, size.height))
            outline = shape.createOutline(size, layoutDirection, this)
        }
        clipRect {
            val p = Backdrop.progress.value
            val prev = Backdrop.previous
            val prevImg = prev?.let { Backdrop.blurredAny(it, material.blur) }
            val curImg = Backdrop.blurredAny(Backdrop.current, material.blur)
            if (prevImg != null && p < 1f) drawBackdropSlice(prevImg, pos, 1f)
            if (curImg != null) drawBackdropSlice(curImg, pos, if (prevImg != null) p else 1f)
            drawRect(tint!!)
            if (material.wash.alpha > 0f) drawRect(material.wash)
        }
        if (material.edgeWidth.value > 0f) drawOutline(outline!!, edge!!, style = Stroke(material.edgeWidth.toPx()))
        drawContent()
    }
}

/** Colour of [top] composited over [bottom] (both translucent), as one translucent colour. */
private fun over(top: Color, bottom: Color): Color {
    val a = top.alpha + bottom.alpha * (1f - top.alpha)
    if (a <= 0f) return Color.Transparent
    fun ch(t: Float, b: Float) = (t * top.alpha + b * bottom.alpha * (1f - top.alpha)) / a
    return Color(ch(top.red, bottom.red), ch(top.green, bottom.green), ch(top.blue, bottom.blue), a)
}

/** Draws the region of the full-screen (Crop-scaled) background that lies under this element. */
private fun DrawScope.drawBackdropSlice(img: ImageBitmap, screenPos: Offset, alpha: Float) {
    val bg = Backdrop.size
    val w = if (bg.width > 0) bg.width.toFloat() else size.width
    val h = if (bg.height > 0) bg.height.toFloat() else size.height
    val scale = max(w / img.width, h / img.height)
    val dw = img.width * scale
    val dh = img.height * scale
    val ox = (w - dw) / 2f - screenPos.x
    val oy = (h - dh) / 2f - screenPos.y
    drawImage(
        image = img,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(img.width, img.height),
        dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

/**
 * Full-screen app background: the photo (crossfading when it changes) plus a white veil.
 * Publishes its size and current image to [Backdrop] so glass surfaces can match it.
 */
@Composable
fun BackdropImage(id: String, modifier: Modifier = Modifier, painter: @Composable (id: String, alpha: () -> Float) -> Unit) {
    val ctx = LocalContext.current
    remember { Backdrop.setInitial(id); true }
    LaunchedEffect(id) {
        // Prepare the blurred copies first (tiny, off the main thread), then crossfade.
        for (r in Backdrop.radii) Backdrop.ensure(ctx.applicationContext, id, r)
        Backdrop.switchTo(id)
    }
    Box(modifier.fillMaxSize().onSizeChanged { Backdrop.size = it }) {
        Backdrop.previous?.let { painter(it) { 1f } }
        painter(Backdrop.current) { if (Backdrop.previous != null) Backdrop.progress.value else 1f }
    }
}

/** Separable 3-pass box blur on ARGB pixels (≈ gaussian), in place. Fast on tiny bitmaps. */
internal object FastBlur {
    fun blur(bmp: Bitmap, radius: Int) {
        if (radius < 1) return
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        // three box passes of radius r approximate a gaussian with sigma ~ r
        val r = max(1, (radius / 1.7f).roundToInt())
        repeat(3) {
            boxH(px, tmp, w, h, r)
            boxV(tmp, px, w, h, r)
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }

    private fun boxH(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = src[row + i.coerceIn(0, w - 1)]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (x in 0 until w) {
                dst[row + x] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = src[row + (x + r + 1).coerceAtMost(w - 1)]
                val sub = src[row + (x - r).coerceAtLeast(0)]
                a += (add ushr 24) - (sub ushr 24)
                rr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                b += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }

    private fun boxV(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = src[i.coerceIn(0, h - 1) * w + x]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (y in 0 until h) {
                dst[y * w + x] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = src[(y + r + 1).coerceAtMost(h - 1) * w + x]
                val sub = src[(y - r).coerceAtLeast(0) * w + x]
                a += (add ushr 24) - (sub ushr 24)
                rr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                b += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}
