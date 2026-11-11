package com.sjpixels.innerlotus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.ui.theme.Lotus

/** Vertical bars with labels underneath. Values are 0..1 (null = no data). */
@Composable
fun BarChart(values: List<Float?>, labels: List<String>, color: Color = Lotus.Plum, height: Dp = 120.dp, valueLabels: List<String>? = null) {
    Column(Modifier.fillMaxWidth()) {
        if (valueLabels != null) {
            Row(Modifier.fillMaxWidth()) {
                valueLabels.forEach { Text(it, modifier = Modifier.weight(1f), fontSize = 10.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center) }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.56f
            val track = Color.White.copy(alpha = 0.6f)
            values.forEachIndexed { i, v ->
                val x = i * slot + (slot - barW) / 2f
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2, barW / 2))
                val f = (v ?: 0f).coerceIn(0f, 1f)
                if (f > 0f) {
                    val h = size.height * f
                    drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2, barW / 2))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { Text(it, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

/** Calendar-style heatmap: columns are weeks, rows are weekdays. values oldest first. */
@Composable
fun Heatmap(values: List<Float?>, color: Color = Lotus.Teal, cell: Dp = 14.dp) {
    val weeks = (values.size + 6) / 7
    Canvas(Modifier.fillMaxWidth().height(cell * 7 + 6.dp * 6)) {
        val gap = 4.dp.toPx()
        val c = minOf(cell.toPx(), (size.width - gap * (weeks - 1)) / weeks.coerceAtLeast(1))
        values.forEachIndexed { i, v ->
            val col = i / 7
            val row = i % 7
            val alpha = if (v == null) 0.12f else 0.18f + 0.82f * v.coerceIn(0f, 1f)
            val base = if (v == null) Color.Gray else color
            drawRoundRect(base.copy(alpha = alpha), Offset(col * (c + gap), row * (c + gap)), Size(c, c), CornerRadius(c * 0.25f, c * 0.25f))
        }
    }
}

/** Circular progress ring with centred text. */
@Composable
fun Ring(fraction: Float, center: String, sub: String? = null, size: Dp = 96.dp, color: Color = Lotus.Plum) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.1f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.7f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(center, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.2f).sp, color = Lotus.Ink)
            if (sub != null) Text(sub, fontSize = 11.sp, color = Lotus.InkSoft)
        }
    }
}

/** Horizontal proportion bar. */
@Composable
fun HBar(fraction: Float, color: Color = Lotus.Plum, height: Dp = 8.dp) {
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.7f), Offset.Zero, size, r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(color, Offset.Zero, Size(w, size.height), r)
    }
}
