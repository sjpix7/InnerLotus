package com.sjpixels.innerlotus.ui.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws a chakra mandala in a soft watercolour style (original artwork, traditional geometry):
 * Root 4 petals + square, Sacral 6 + crescent, Solar 10 + triangle, Heart 12 + hexagram,
 * Throat 16 + triangle and circle, Third Eye 2 petals + triangle, Crown many petals.
 * Shared by the Compose UI and the shareable card renderer.
 */
object ChakraPainter {
    private val petals = intArrayOf(4, 6, 10, 12, 16, 2, 20)

    private fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun lighten(c: Int, f: Float) = Color.rgb(
        (Color.red(c) + (255 - Color.red(c)) * f).toInt(),
        (Color.green(c) + (255 - Color.green(c)) * f).toInt(),
        (Color.blue(c) + (255 - Color.blue(c)) * f).toInt(),
    )

    fun draw(c: Canvas, cx: Float, cy: Float, r: Float, index: Int, color: Int, watercolor: Boolean = true, line: Int = Color.WHITE) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        if (watercolor) {
            val rnd = Random(index * 977 + 13)
            repeat(6) {
                val dx = (rnd.nextFloat() - 0.5f) * r * 0.24f
                val dy = (rnd.nextFloat() - 0.5f) * r * 0.24f
                val rr = r * (0.78f + rnd.nextFloat() * 0.22f)
                fill.shader = RadialGradient(
                    cx + dx, cy + dy, rr,
                    intArrayOf(withAlpha(color, 120), withAlpha(color, 70), withAlpha(color, 0)),
                    floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP,
                )
                c.drawCircle(cx + dx, cy + dy, rr, fill)
            }
        }
        if (watercolor) {
            fill.shader = RadialGradient(
                cx, cy, r * 0.84f,
                intArrayOf(withAlpha(lighten(color, 0.25f), 235), withAlpha(color, 215), withAlpha(color, 0)),
                floatArrayOf(0f, 0.8f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, r * 0.84f, fill)
        } else {
            // soft halo, then a disc lit from the upper left (glass-like), no hard edge
            fill.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(withAlpha(color, 70), withAlpha(color, 0)),
                floatArrayOf(0.7f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, r, fill)
            fill.shader = RadialGradient(
                cx - r * 0.28f, cy - r * 0.32f, r * 1.25f,
                intArrayOf(lighten(color, 0.42f), color, withAlpha(color, 240)),
                floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP,
            )
            c.drawCircle(cx, cy, r * 0.8f, fill)
        }

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (r * 0.035f).coerceAtLeast(1f)
            this.color = withAlpha(line, 240)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val idx = index.coerceIn(0, 6)
        val rc = r * 0.36f
        when (idx) {
            5 -> drawPetals(c, cx, cy, rc, r * 0.8f, 2, 0.85f, 0.0, stroke)
            6 -> {
                drawPetals(c, cx, cy, rc, r * 0.68f, 20, (PI / 20 * 0.95).toFloat(), -PI / 2, stroke)
                drawPetals(c, cx, cy, rc * 0.92f, r * 0.5f, 10, (PI / 10 * 0.9).toFloat(), -PI / 2 + PI / 10, stroke)
            }
            else -> drawPetals(c, cx, cy, rc, r * 0.64f, petals[idx], (PI / petals[idx] * 0.92).toFloat(), -PI / 2, stroke)
        }
        c.drawCircle(cx, cy, rc, stroke)
        when (idx) {
            0 -> {
                val h = rc * 0.62f
                c.drawRect(cx - h, cy - h, cx + h, cy + h, stroke)
                triangle(c, cx, cy + rc * 0.04f, rc * 0.5f, down = true, p = stroke)
            }
            1 -> {
                c.drawArc(RectF(cx - rc * 0.58f, cy - rc * 0.42f, cx + rc * 0.58f, cy + rc * 0.74f), 15f, 150f, false, stroke)
                c.drawArc(RectF(cx - rc * 0.46f, cy - rc * 0.5f, cx + rc * 0.46f, cy + rc * 0.42f), 25f, 130f, false, stroke)
            }
            2 -> triangle(c, cx, cy, rc * 0.74f, down = true, p = stroke)
            3 -> {
                triangle(c, cx, cy, rc * 0.78f, down = false, p = stroke)
                triangle(c, cx, cy, rc * 0.78f, down = true, p = stroke)
            }
            4 -> {
                triangle(c, cx, cy, rc * 0.78f, down = true, p = stroke)
                c.drawCircle(cx, cy - rc * 0.08f, rc * 0.26f, stroke)
            }
            5 -> {
                triangle(c, cx, cy, rc * 0.62f, down = true, p = stroke)
                c.drawCircle(cx, cy - rc * 0.1f, rc * 0.14f, stroke)
            }
            6 -> c.drawCircle(cx, cy, rc * 0.18f, stroke)
        }
    }

    private fun pt(cx: Float, cy: Float, rad: Float, a: Double): Pair<Float, Float> =
        (cx + rad * cos(a)).toFloat() to (cy + rad * sin(a)).toFloat()

    private fun drawPetals(c: Canvas, cx: Float, cy: Float, base: Float, reach: Float, n: Int, halfWidth: Float, start: Double, p: Paint) {
        val path = Path()
        for (i in 0 until n) {
            val a = start + i * 2 * PI / n
            val (sx, sy) = pt(cx, cy, base, a - halfWidth)
            val (ex, ey) = pt(cx, cy, base, a + halfWidth)
            val (tx, ty) = pt(cx, cy, reach, a)
            val (c1x, c1y) = pt(cx, cy, reach * 0.9f, a - halfWidth * 0.95)
            val (c2x, c2y) = pt(cx, cy, reach * 0.9f, a + halfWidth * 0.95)
            path.moveTo(sx, sy)
            path.quadTo(c1x, c1y, tx, ty)
            path.quadTo(c2x, c2y, ex, ey)
        }
        c.drawPath(path, p)
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, rad: Float, down: Boolean, p: Paint) {
        val angles = if (down) doubleArrayOf(PI * 7 / 6, PI * 11 / 6, PI / 2) else doubleArrayOf(PI / 6, PI * 5 / 6, PI * 3 / 2)
        val path = Path()
        angles.forEachIndexed { i, a ->
            val (x, y) = pt(cx, cy, rad, a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, p)
    }
}
