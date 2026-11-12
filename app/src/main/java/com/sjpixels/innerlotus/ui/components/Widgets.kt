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
import com.sjpixels.innerlotus.ui.theme.LotusType
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.PlayArrow
import com.sjpixels.innerlotus.ui.glass.glass
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
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Frosted glass card used everywhere (padding 18dp, radius 24dp). [tone] tints it. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    strong: Boolean = false,
    padding: Dp = 18.dp,
    tone: Tone = Tone.Plain,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = if (strong && tone == Tone.Plain) Tone.Strong else tone
    GlassSurface(modifier.fillMaxWidth(), material = t.material(), onClick = onClick) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.padding(top = 12.dp, bottom = 2.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(styledTitle(text), style = LotusType.section)
    }
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = Lotus.Ink) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodyLarge, color = color)
}

/** Play / stop pill that reads [text] aloud with the device's text-to-speech. */
@Composable
fun ListenButton(tag: String, text: () -> String, label: Boolean = true, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val speaking = Speaker.speakingTag == tag
    val onClick = { if (speaking) Speaker.stop() else { scope.launch { Speaker.speak(text(), tag) }; Unit } }
    val icon = if (speaking) Icons.Outlined.Stop else Icons.Outlined.PlayArrow
    val desc = if (speaking) tr("ui.common.stop") else tr("ui.common.listen")
    if (label) PillButton(desc, onClick, modifier, icon = icon, selected = speaking)
    else CircleIconButton(icon, desc, onClick, modifier)
    DisposableEffect(tag) { onDispose { if (Speaker.speakingTag == tag) Speaker.stop() } }
}

/** "A moment to understand" card linking to the full knowledge page. */
@Composable
fun LearnCard(topic: String, @Suppress("UNUSED_PARAMETER") emoji: String = "") {
    val title = tr("learn.$topic.title")
    val body = tr("learn.$topic.body")
    GlassCard {
        Kicker(tr("ui.common.moment_to_understand"))
        Spacer(Modifier.height(8.dp))
        Text(title, style = LotusType.cardTitle)
        Spacer(Modifier.height(6.dp))
        Text(body, maxLines = 3, overflow = TextOverflow.Ellipsis, style = LotusType.small)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ListenButton("learn:$topic", { "$title. $body" })
            Spacer(Modifier.weight(1f))
            TextLink(tr("ui.common.explore_guide"), { App.nav.go(Screen.Learn(topic)) })
        }
    }
}

/** Chakra mandala: a soft disc in the chakra's (muted) colour with a fine white line symbol. */
@Composable
fun ChakraSymbol(index: Int, color: Long, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawIntoCanvas { c ->
            ChakraPainter.draw(c.nativeCanvas, this.size.width / 2f, this.size.height / 2f, this.size.minDimension / 2f, index, Color(color).toArgb(), watercolor = false)
        }
    }
}

/** Segmented control: glass track, selected option as an ink pill. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth(), shape = PillShape, material = Glass.Segments) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for ((id, label) in options) {
                val on = id == selected
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(PillShape)
                        .background(if (on) Lotus.Ink else Color.Transparent)
                        .clickable { onSelect(id) }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = LotusType.pill.copy(color = if (on) Color.White else Lotus.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Small selectable chip (selected = ink pill). */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillButton(text, onClick, modifier, selected = selected)
}

/** Legacy: round glass badge (screens now use IconBadge with an outlined icon). */
@Composable
fun EmojiCircle(emoji: String, size: Dp = 44.dp, bg: Color = Color.White.copy(alpha = 0.8f)) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    LotusDialog(
        onDismiss = onDismiss,
        title = title,
        text = { Text(text, style = LotusType.small) },
        confirm = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, style = LotusType.button.copy(color = if (danger) Lotus.Danger else Lotus.Accent))
            }
        },
        dismiss = { TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel"), style = LotusType.button.copy(color = Lotus.Ink)) } },
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

/** Labelled numeric stepper: "Label   (−)  3  (+)". */
@Composable
fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = LotusType.itemTitle)
        CircleIconButton(Icons.Outlined.Remove, "−", { if (value > range.first) onChange(value - 1) }, size = 38.dp, iconSize = 18.dp)
        Text("$value", style = LotusType.stat.copy(fontSize = 26.sp), textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
        CircleIconButton(Icons.Outlined.Add, "+", { if (value < range.last) onChange(value + 1) }, size = 38.dp, iconSize = 18.dp)
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
