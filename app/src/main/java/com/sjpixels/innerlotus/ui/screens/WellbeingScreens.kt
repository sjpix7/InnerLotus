package com.sjpixels.innerlotus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.audio.SoundEngine
import com.sjpixels.innerlotus.content.Breath
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.Mix
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.displayFamily
import com.sjpixels.innerlotus.ui.theme.scriptFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

// ------------------------------------------------------------------------------------------------
// Breathwork
// ------------------------------------------------------------------------------------------------

@Composable
fun BreathScreen() {
    val s = App.settings
    AppScaffold(title = tr("ui.section.breath")) { padding ->
        PageColumn(padding) {
            LearnCard("breath", "🌬️")
            GlassCard(padding = 12.dp) {
                Text(tr("ui.breath.duration"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 5, 10, 15).forEach { m -> Pill(tr("ui.common.minutes_n", "n" to m), s.breathMinutes == m, { s.breathMinutes = m }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔊  " + tr("ui.breath.voice"), modifier = Modifier.weight(1f))
                    Switch(checked = s.breathVoice, onCheckedChange = { s.breathVoice = it })
                }
            }
            Breath.patterns.forEach { (id, phases) ->
                GlassCard(onClick = { App.nav.go(Screen.BreathSession(id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiCircle(Breath.emoji[id] ?: "🌬️")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("breath.$id.name"), style = MaterialTheme.typography.titleMedium)
                            Text(phases.joinToString(" · ") { p -> p.seconds.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } }, color = Lotus.Plum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("breath.$id.desc"), fontSize = 13.sp, color = Lotus.InkSoft)
                        }
                        Icon(Icons.Filled.PlayCircle, null, tint = Lotus.Plum, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

/** Animated breathing circle that runs a pattern for [minutes]. */
@Composable
fun BreathCircle(pattern: String, running: Boolean, minutes: Float, voice: Boolean, onRunningChange: (Boolean) -> Unit, onFinish: () -> Unit = {}) {
    val phases = Breath.patterns[pattern] ?: Breath.patterns.getValue("calm")
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(0.45f) }
    var phaseIdx by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    val total = minutes * 60f
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        if (elapsed >= total) { elapsed = 0f; phaseIdx = 0 }
        while (isActive && elapsed < total) {
            val ph = phases[phaseIdx]
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (voice) Speaker.say(tr("breath.cue.${ph.cue}"))
            val target = when (ph.kind) { Breath.Kind.IN -> 1f; Breath.Kind.OUT -> 0.45f; Breath.Kind.HOLD -> scale.value }
            launch { scale.animateTo(target, tween((ph.seconds * 1000).toInt(), easing = LinearEasing)) }
            var rest = ph.seconds
            while (rest > 0f && isActive) {
                left = ceil(rest).toInt()
                val step = min(1f, rest)
                delay((step * 1000).toLong())
                rest -= step
                elapsed += step
            }
            phaseIdx = (phaseIdx + 1) % phases.size
        }
        if (elapsed >= total) { onRunningChange(false); onFinish() }
    }
    val ph = phases[phaseIdx]
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Color.White.copy(alpha = 0.45f), radius = r)
                drawCircle(
                    Brush.radialGradient(listOf(Lotus.Rose.copy(alpha = 0.85f), Lotus.Plum.copy(alpha = 0.75f)), radius = r * scale.value),
                    radius = r * scale.value,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (running) tr("breath.cue.${ph.cue}") else tr("ui.breath.ready"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (running) Text("$left", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { (elapsed / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(0.7f).clip(RoundedCornerShape(50)))
        val remain = (total - elapsed).toInt().coerceAtLeast(0)
        Text("%d:%02d".format(remain / 60, remain % 60), color = Lotus.InkSoft, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FilledIconButton(onClick = { onRunningChange(!running); if (running) Speaker.stop() }, modifier = Modifier.size(64.dp)) {
            Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
fun BreathSessionScreen(pattern: String) {
    val s = App.settings
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    KeepScreenOn(running)
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }
    AppScaffold(title = tr("breath.$pattern.name")) { padding ->
        PageColumn(padding) {
            Text(tr("breath.$pattern.desc"), color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BreathCircle(pattern, running, s.breathMinutes.toFloat(), s.breathVoice, { running = it }, { finished = true })
            if (finished) {
                GlassCard(strong = true) {
                    Text("🌸 " + tr("ui.breath.done"), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { App.nav.go(Screen.JournalEdit(null, "free")) }, modifier = Modifier.weight(1f)) { Text(tr("ui.breath.journal_it")) }
                        OutlinedButton(onClick = { App.nav.go(Screen.Areas()) }, modifier = Modifier.weight(1f)) { Text(tr("ui.section.affirmations")) }
                    }
                }
            }
            if (pattern == "nadi") Text(tr("ui.breath.nadi_hint"), fontSize = 13.sp, color = Lotus.InkSoft)
            Text(tr("ui.breath.safety"), fontSize = 12.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Chakra guide
// ------------------------------------------------------------------------------------------------

@Composable
fun ChakraGuideScreen() {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<String?>(null) }
    AppScaffold(title = tr("ui.section.chakras")) { padding ->
        PageColumn(padding) {
            LearnCard("chakra", "🧘")
            Catalog.chakras.forEach { c ->
                val expanded = open == c.id
                val ink = Color(c.ink)
                GlassCard(strong = expanded, onClick = { open = if (expanded) null else c.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChakraSymbol(c.index, c.color, 60.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${c.index + 1}. " + tr("chakra.${c.id}.name"), style = MaterialTheme.typography.titleMedium)
                            Text(tr("chakra.${c.id}.sanskrit"), fontSize = 13.sp, color = Lotus.InkSoft)
                            Text(tr("chakra.${c.id}.core"), fontFamily = displayFamily(), fontWeight = FontWeight.Bold, color = ink, fontSize = 18.sp, letterSpacing = 1.sp)
                        }
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        val rows = listOf(
                            "location" to "📍", "element" to "🌍", "bija" to "🕉️", "theme" to "✨",
                            "balanced" to "🌸", "imbalanced" to "⚠️", "practice" to "🧘",
                        )
                        rows.forEach { (k, e) ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text(e, modifier = Modifier.width(28.dp))
                                Column {
                                    Text(tr("ui.chakra.$k"), fontSize = 12.sp, color = Lotus.InkSoft)
                                    Text(tr("chakra.${c.id}.$k"))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        I18n.list("chakra.${c.id}.seeds").forEach { seed ->
                            Text("• $seed", fontFamily = scriptFamily(), color = ink, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        val toneId = "freq:f${c.toneHz.toInt()}"
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ListenButton("chakra:${c.id}", {
                                listOf("name", "location", "element", "theme", "balanced", "imbalanced", "practice").joinToString(". ") { tr("chakra.${c.id}.$it") } + ". " +
                                    I18n.list("chakra.${c.id}.seeds").joinToString(" ")
                            }, label = false)
                            FilledTonalButton(onClick = { SoundEngine.toggle(ctx, toneId, 0.5f) }) {
                                Text((if (SoundEngine.isOn(toneId)) "■ " else "♪ ") + "${c.toneHz.toInt()} Hz")
                            }
                            FilledTonalButton(onClick = { App.nav.go(Screen.Areas(c.id)) }) { Text(tr("ui.section.affirmations"), maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Calm now (SOS)
// ------------------------------------------------------------------------------------------------

@Composable
fun CalmNowScreen() {
    val ctx = LocalContext.current
    var breathing by remember { mutableStateOf(true) }
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    KeepScreenOn(true)
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }
    AppScaffold(title = tr("ui.calm.title")) { padding ->
        PageColumn(padding) {
            Text(tr("sos.intro"), style = MaterialTheme.typography.titleLarge, color = Lotus.PlumDeep, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(tr("sos.breathe"), color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BreathCircle("calm", breathing, 2f, App.settings.breathVoice, { breathing = it })
            GlassCard(strong = true) {
                Text(tr("sos.ground.title"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                I18n.list("sos.ground").forEachIndexed { i, line ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { checked[i] = !checked[i] }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(if (checked[i]) Lotus.Teal else Color.White), contentAlignment = Alignment.Center) {
                            Text("${5 - i}", color = if (checked[i]) Color.White else Lotus.Ink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(line, modifier = Modifier.weight(1f))
                    }
                }
            }
            GlassCard {
                Text("🤍  " + tr("sos.heart"), fontFamily = displayFamily(), fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                ListenButton("sos", { tr("sos.intro") + " " + I18n.list("sos.ground").joinToString(" ") + " " + tr("sos.heart") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { App.nav.go(Screen.EftSession("anxiety", "gentle")) }, modifier = Modifier.weight(1f)) { Text("👆 " + tr("ui.calm.tap")) }
                FilledTonalButton(onClick = {
                    SoundEngine.loadMix(Mix("calm", tr("ui.calm.sound_mix"), mapOf("lib:rain" to 0.6f, "lib:bowl" to 0.5f, "freq:f528" to 0.2f)), ctx)
                }, modifier = Modifier.weight(1f)) { Text("🎶 " + tr("ui.calm.sound")) }
            }
            GlassCard(strong = true) {
                Text("☎️  " + tr("ui.calm.crisis_title"), style = MaterialTheme.typography.titleMedium, color = Lotus.Danger)
                Spacer(Modifier.height(4.dp))
                Text(tr("sos.crisis"), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:14416")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    }, colors = ButtonDefaults.buttonColors(containerColor = Lotus.Danger), modifier = Modifier.weight(1f)) { Text("Tele-MANAS 14416") }
                    OutlinedButton(onClick = { Sharing.openUrl(ctx, Catalog.HELPLINE_URL) }, modifier = Modifier.weight(1f)) { Text("findahelpline.com") }
                }
            }
        }
    }
}
