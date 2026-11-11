package com.sjpixels.innerlotus.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.sjpixels.innerlotus.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.art.ChakraPainter
import com.sjpixels.innerlotus.ui.theme.Lotus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Scrollable page body with standard padding; respects the keyboard. */
@Composable
fun PageColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Translucent "frosted" card used everywhere on the pastel backgrounds. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    strong: Boolean = false,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth()
    val colors = CardDefaults.cardColors(containerColor = if (strong) Lotus.GlassStrong else Lotus.Glass)
    if (onClick != null) {
        Card(onClick = onClick, modifier = m, shape = RoundedCornerShape(22.dp), colors = colors) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Card(modifier = m, shape = RoundedCornerShape(22.dp), colors = colors) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 6.dp, start = 4.dp),
        style = MaterialTheme.typography.titleLarge,
        color = Lotus.PlumDeep,
    )
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = Lotus.Ink) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyLarge, color = color)
}

/** Play / stop button that reads [text] aloud with the device's text-to-speech. */
@Composable
fun ListenButton(tag: String, text: () -> String, label: Boolean = true, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val speaking = Speaker.speakingTag == tag
    FilledTonalButton(
        onClick = {
            if (speaking) Speaker.stop() else scope.launch { Speaker.speak(text(), tag) }
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Icon(if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        if (label) {
            Spacer(Modifier.width(6.dp))
            Text(if (speaking) tr("ui.common.stop") else tr("ui.common.listen"))
        }
    }
    DisposableEffect(tag) { onDispose { if (Speaker.speakingTag == tag) Speaker.stop() } }
}

/** Compact "learn about this" card linking to the full knowledge page. */
@Composable
fun LearnCard(topic: String, emoji: String = "📖") {
    val title = tr("learn.$topic.title")
    val body = tr("learn.$topic.body")
    GlassCard(onClick = { App.nav.go(Screen.Learn(topic)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Lotus.PlumDeep, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(body, maxLines = 3, overflow = TextOverflow.Ellipsis, color = Lotus.InkSoft, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ListenButton("learn:$topic", { "$title. $body" })
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { App.nav.go(Screen.Learn(topic)) }) { Text(tr("ui.common.read_more")) }
        }
    }
}

/** Watercolour chakra mandala. [index] 0 = root .. 6 = crown. */
@Composable
fun ChakraSymbol(index: Int, color: Long, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawIntoCanvas { c ->
            ChakraPainter.draw(c.nativeCanvas, this.size.width / 2f, this.size.height / 2f, this.size.minDimension / 2f, index, Color(color).toArgb())
        }
    }
}

/** A row of selectable pills (used instead of tabs). */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((id, label) in options) {
            val on = id == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Lotus.Plum else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) Color.White else Lotus.Ink, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp)
            }
        }
    }
}

/** Small selectable chip. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Lotus.Plum else Color.White.copy(alpha = 0.7f))
            .border(1.dp, if (selected) Lotus.Plum else Lotus.Plum.copy(alpha = 0.25f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = if (selected) Color.White else Lotus.Ink, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmojiCircle(emoji: String, size: Dp = 44.dp, bg: Color = Color.White.copy(alpha = 0.8f)) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) Lotus.Danger else Lotus.Plum, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel")) } },
    )
}

/** Keeps the screen awake while this composable is shown (if enabled in settings). */
@Composable
fun KeepScreenOn(active: Boolean = true) {
    val view = LocalView.current
    val enabled = active && App.settings.keepScreenOn
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

// ---- Background images decoded at reduced size (cards, thumbnails) -----------------------------

private val bitmapCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

@Composable
fun rememberBg(id: String, sample: Int): ImageBitmap? {
    val ctx = LocalContext.current
    val key = "$id@$sample"
    val state by produceState<ImageBitmap?>(initialValue = bitmapCache.get(key)?.asImageBitmap(), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val o = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeResource(ctx.resources, Sharing.bgRes(id), o)?.also { bitmapCache.put(key, it) }?.asImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
    return state
}

@Composable
fun BgImage(id: String, sample: Int, modifier: Modifier = Modifier) {
    val img = rememberBg(id, sample)
    if (img != null) {
        Image(bitmap = img, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(Color(0xFFEFE6F6)))
    }
}

// ---- File export / import through the system file picker (no storage permission needed) ----

/** Returns a function that asks the user where to save [mime] content, then writes it. */
@Composable
fun rememberExporter(mime: String, onDone: (Boolean) -> Unit = {}): (fileName: String, content: () -> String) -> Unit {
    val ctx = LocalContext.current
    val pending = remember { mutableStateOf<(() -> String)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri: Uri? ->
        val producer = pending.value
        if (uri != null && producer != null) {
            val ok = Sharing.writeText(ctx, uri, producer())
            Sharing.toast(ctx, if (ok) tr("ui.data.exported") else tr("ui.common.error"))
            onDone(ok)
        }
        pending.value = null
    }
    return { name, content ->
        pending.value = content
        launcher.launch(name)
    }
}

/** Returns a function that opens the system picker and passes the chosen file's text to [onText]. */
@Composable
fun rememberImporter(onText: (String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = Sharing.readText(ctx, uri)
            if (text == null) Sharing.toast(ctx, tr("ui.common.error")) else onText(text)
        }
    }
    return { launcher.launch(arrayOf("application/json", "text/plain", "text/csv", "text/comma-separated-values", "application/octet-stream", "*/*")) }
}

/** Short helper: a labelled numeric stepper. */
@Composable
fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = Lotus.Ink)
        TextButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−", fontSize = 20.sp) }
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Lotus.PlumDeep)
        TextButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+", fontSize = 20.sp) }
    }
}

/**
 * The Inner Lotus logo, drawn from the launcher icon's own layers so it always matches the app
 * icon. Shown as a rounded tile with a soft glow, like the icon on the home screen.
 */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier
            .size(size)
            .shadow(size * 0.18f, shape, ambientColor = Lotus.Plum, spotColor = Lotus.Plum)
            .clip(shape),
    ) {
        // Adaptive-icon layers are 108dp with the visible part in the middle 72dp: scale up 1.5x
        // so the lotus fills the tile the way a launcher shows it.
        val layer = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = layer, contentScale = ContentScale.FillBounds)
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = tr("ui.app_name"), modifier = layer, contentScale = ContentScale.FillBounds)
    }
}
