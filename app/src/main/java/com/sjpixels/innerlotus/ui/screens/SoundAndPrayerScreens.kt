@file:OptIn(ExperimentalLayoutApi::class)

package com.sjpixels.innerlotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.audio.SoundEngine
import com.sjpixels.innerlotus.audio.dsp.VoiceFactory
import com.sjpixels.innerlotus.content.Prayer
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.Mix
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.displayFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun layerName(id: String): String {
    val (kind, key) = id.substringBefore(':') to id.substringAfter(':')
    return when (kind) {
        "freq" -> tr("sound.freq.$key.name")
        "lib" -> tr("sound.lib.$key.name")
        "hz" -> "$key Hz"
        else -> id
    }
}

private fun presets(): List<Mix> = listOf(
    Mix("p_rain_temple", tr("ui.sound.preset_rain_temple"), mapOf("lib:rain" to 0.7f, "lib:bell" to 0.5f)),
    Mix("p_himalaya", tr("ui.sound.preset_himalaya"), mapOf("lib:stream" to 0.6f, "lib:birds" to 0.5f, "lib:bowl" to 0.6f)),
    Mix("p_sleep", tr("ui.sound.preset_deep_sleep"), mapOf("freq:delta" to 0.45f, "lib:brown" to 0.4f, "lib:rain" to 0.5f)),
    Mix("p_ocean", tr("ui.sound.preset_ocean"), mapOf("lib:ocean" to 0.7f, "lib:crystal" to 0.45f, "freq:f432" to 0.25f)),
    Mix("p_evening", tr("ui.sound.preset_indian_evening"), mapOf("lib:tanpura" to 0.6f, "lib:flute" to 0.6f, "lib:crickets" to 0.4f)),
    Mix("p_focus", tr("ui.sound.preset_focus"), mapOf("freq:alpha" to 0.4f, "lib:pink" to 0.3f, "lib:stream" to 0.5f)),
    Mix("p_heart", tr("ui.sound.preset_heart"), mapOf("freq:f639" to 0.35f, "lib:chimes" to 0.5f, "lib:wind" to 0.3f)),
    Mix("p_ground", tr("ui.sound.preset_grounding"), mapOf("lib:drum" to 0.5f, "lib:fire" to 0.5f, "freq:f396" to 0.25f)),
)

@Composable
fun SoundScreen() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("freq") }
    var saveDialog by remember { mutableStateOf(false) }
    AppScaffold(title = tr("ui.section.sound")) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f)) {
                PageColumn(PaddingValues(0.dp)) {
                    LearnCard("sound", "🎶")
                    Segmented(
                        options = listOf("freq" to tr("ui.sound.tab_freq"), "mix" to tr("ui.sound.tab_mix"), "saved" to tr("ui.sound.tab_saved")),
                        selected = tab, onSelect = { tab = it },
                    )
                    when (tab) {
                        "freq" -> FrequencyList()
                        "mix" -> SoundLibrary()
                        else -> SavedMixes()
                    }
                    Text(tr("ui.sound.safety"), fontSize = 12.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (SoundEngine.layers.isNotEmpty()) NowPlayingPanel(onSave = { saveDialog = true })
        }
    }
    if (saveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialog = false },
            title = { Text(tr("ui.sound.save_mix")) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("ui.sound.mix_name")) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    App.mixes.save(name.ifBlank { tr("ui.sound.my_mix") }, SoundEngine.layers.toMap())
                    SoundEngine.currentMixName = name
                    saveDialog = false
                    Sharing.toast(ctx, tr("ui.common.saved"))
                }) { Text(tr("ui.common.save")) }
            },
            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text(tr("ui.common.cancel")) } },
        )
    }
}

@Composable
private fun FrequencyList() {
    val ctx = LocalContext.current
    VoiceFactory.frequencies.forEach { f ->
        val id = "freq:${f.id}"
        val on = SoundEngine.isOn(id)
        GlassCard(strong = on, padding = 12.dp, modifier = if (on) Modifier.border(2.dp, Lotus.Plum, RoundedCornerShape(22.dp)) else Modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((if (f.binaural) "🎧 " else "〰️ ") + tr("sound.freq.${f.id}.name"), style = MaterialTheme.typography.titleMedium)
                    Text(tr("sound.freq.${f.id}.desc"), fontSize = 13.sp, color = Lotus.InkSoft)
                }
                FilledIconToggleButton(checked = on, onCheckedChange = { SoundEngine.toggle(ctx, id, 0.6f) }) {
                    Icon(if (on) Icons.Filled.Stop else Icons.Filled.PlayArrow, tr("ui.common.listen"))
                }
            }
            if (on) VolumeSlider(id)
        }
    }
}

@Composable
private fun VolumeSlider(id: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = Lotus.InkSoft, modifier = Modifier.size(18.dp))
        Slider(value = SoundEngine.layers[id] ?: 0.6f, onValueChange = { SoundEngine.setVolume(id, it) }, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Lotus.InkSoft, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SoundLibrary() {
    val ctx = LocalContext.current
    Text(tr("ui.sound.mix_hint"), color = Lotus.InkSoft, fontSize = 13.sp)
    VoiceFactory.library.forEach { (cat, ids) ->
        SectionTitle(tr("ui.sound.cat_$cat"))
        ids.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val id = "lib:$key"
                    val on = SoundEngine.isOn(id)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (on) Lotus.Plum.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.7f))
                            .clickable { SoundEngine.toggle(ctx, id, 0.7f) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(VoiceFactory.libraryEmoji[key] ?: "🎵", fontSize = 28.sp)
                        Text(tr("sound.lib.$key.name"), fontSize = 12.sp, color = if (on) Color.White else Lotus.Ink, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (SoundEngine.layers.isNotEmpty()) {
        SectionTitle(tr("ui.sound.now_mixing"))
        GlassCard(strong = true, padding = 12.dp) {
            SoundEngine.layers.keys.toList().forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(layerName(id), modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Slider(value = SoundEngine.layers[id] ?: 0.6f, onValueChange = { SoundEngine.setVolume(id, it) }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { SoundEngine.remove(id) }) { Icon(Icons.Filled.Close, tr("ui.common.remove")) }
                }
            }
        }
    }
}

@Composable
private fun SavedMixes() {
    val ctx = LocalContext.current
    val mine by App.mixes.mixes.collectAsState()
    var journeyMin by rememberSaveable { mutableIntStateOf(10) }
    SectionTitle(tr("ui.sound.my_mixes"))
    if (mine.isEmpty()) Text(tr("ui.sound.no_mixes"), color = Lotus.InkSoft)
    mine.forEach { m -> MixRow(m, onDelete = { App.mixes.delete(m.id) }) }
    SectionTitle(tr("ui.sound.presets"))
    presets().forEach { m -> MixRow(m, onDelete = null) }
    GlassCard(strong = true) {
        Text("🔁  " + tr("ui.sound.journey"), style = MaterialTheme.typography.titleMedium)
        Text(tr("ui.sound.journey_hint"), fontSize = 13.sp, color = Lotus.InkSoft)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(5, 10, 15, 30).forEach { m -> Pill(tr("ui.common.minutes_n", "n" to m), journeyMin == m, { journeyMin = m }) }
        }
        Spacer(Modifier.height(8.dp))
        if (SoundEngine.journey.isNotEmpty()) {
            OutlinedButton(onClick = { SoundEngine.stopJourney() }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.sound.journey_stop")) }
        } else {
            Button(onClick = { SoundEngine.startJourney(ctx, mine.ifEmpty { presets() }, journeyMin) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("ui.sound.journey_start"))
            }
        }
    }
}

@Composable
private fun MixRow(m: Mix, onDelete: (() -> Unit)?) {
    val ctx = LocalContext.current
    GlassCard(padding = 12.dp, onClick = { SoundEngine.loadMix(m, ctx) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayCircle, null, tint = Lotus.Plum, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium)
                Text(m.layers.keys.joinToString(" · ") { layerName(it) }, fontSize = 12.sp, color = Lotus.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, tr("ui.common.delete"), tint = Lotus.InkSoft) }
        }
    }
}

@Composable
private fun NowPlayingPanel(onSave: () -> Unit) {
    val ctx = LocalContext.current
    var timerMenu by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(SoundEngine.sleepEndsAt) {
        while (isActive && SoundEngine.sleepEndsAt > 0) { now = System.currentTimeMillis(); delay(1000) }
    }
    Surface(color = Lotus.PlumDeep.copy(alpha = 0.95f), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                SoundEngine.currentMixName.ifEmpty { SoundEngine.layers.keys.joinToString(" + ") { layerName(it) } },
                color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (SoundEngine.isPlaying) SoundEngine.pause() else SoundEngine.resume(ctx) }) {
                    Icon(if (SoundEngine.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, tr("ui.common.play"), tint = Color.White)
                }
                IconButton(onClick = { SoundEngine.stopAll() }) { Icon(Icons.Filled.Stop, tr("ui.common.stop"), tint = Color.White) }
                Box {
                    TextButton(onClick = { timerMenu = true }) {
                        Icon(Icons.Filled.Bedtime, null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        val left = ((SoundEngine.sleepEndsAt - now) / 60000L).coerceAtLeast(0L) + if (SoundEngine.sleepEndsAt > 0) 1 else 0
                        Text(if (SoundEngine.sleepEndsAt > 0) tr("ui.common.minutes_n", "n" to left) else tr("ui.sound.sleep_timer"), color = Color.White)
                    }
                    DropdownMenu(expanded = timerMenu, onDismissRequest = { timerMenu = false }) {
                        listOf(0, 15, 30, 45, 60, 90, 120).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(if (m == 0) tr("ui.common.off") else tr("ui.common.minutes_n", "n" to m)) },
                                onClick = { timerMenu = false; SoundEngine.setSleepTimer(m) },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSave) {
                    Icon(Icons.Filled.BookmarkAdd, null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text(tr("ui.common.save"), color = Color.White)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Ho'oponopono
// ------------------------------------------------------------------------------------------------

@Composable
fun HooponoScreen() {
    val ctx = LocalContext.current
    var topic by rememberSaveable { mutableStateOf("self") }
    var name by rememberSaveable { mutableStateOf("") }
    var rounds by rememberSaveable { mutableIntStateOf(3) }
    val lang = I18n.lang
    fun build(): String = Prayer.build(
        opening = tr("hoopono.opening"),
        intro = tr("hoopono.topic.$topic.intro"),
        focus = tr("hoopono.topic.$topic.focus"),
        phrases = listOf(tr("hoopono.p1"), tr("hoopono.p2"), tr("hoopono.p3"), tr("hoopono.p4")),
        closing = tr("hoopono.closing"),
        name = if (topic == "person") name.ifBlank { tr("hoopono.default_name") } else null,
        rounds = rounds,
    )
    var text by remember(topic, name, rounds, lang, I18n.feminine) { mutableStateOf(build()) }
    var looping by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    KeepScreenOn(looping)
    LaunchedEffect(looping) {
        if (!looping) return@LaunchedEffect
        count = 0
        while (isActive && looping) {
            if (!Speaker.speak(text, "hoopono")) { looping = false; break }
            count++
            delay(1500)
        }
    }
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }

    AppScaffold(title = tr("ui.section.hoopono")) { padding ->
        PageColumn(padding) {
            LearnCard("hooponopono", "🌺")
            GlassCard(strong = true) {
                Text(tr("hoopono.p1") + "  " + tr("hoopono.p2"), fontFamily = displayFamily(), fontSize = 20.sp, color = Lotus.PlumDeep, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(tr("hoopono.p3") + "  " + tr("hoopono.p4"), fontFamily = displayFamily(), fontSize = 20.sp, color = Lotus.Rose, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            SectionTitle(tr("ui.hoopono.choose_topic"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Catalog.hooponoTopics.forEach { t -> Pill(tr("hoopono.topic.$t.name"), topic == t, { looping = false; topic = t }) }
            }
            if (topic == "person") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(tr("ui.hoopono.name_label")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.7f), focusedContainerColor = Color.White.copy(alpha = 0.9f)),
                )
            }
            Stepper(rounds, 1..21, { rounds = it }, tr("ui.hoopono.rounds"))
            SectionTitle(tr("ui.hoopono.your_prayer"))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), minLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = displayFamily(), fontSize = 18.sp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.75f), focusedContainerColor = Color.White.copy(alpha = 0.92f)),
                supportingText = { Text(tr("ui.hoopono.edit_hint")) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { looping = !looping; if (!looping) Speaker.stop() }, modifier = Modifier.weight(1f)) {
                    Icon(if (looping) Icons.Filled.Stop else Icons.Filled.Repeat, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (looping) tr("ui.common.stop") else tr("ui.hoopono.autoplay"))
                }
                ListenButton("hoopono_once", { text }, modifier = Modifier.weight(1f))
            }
            if (looping || count > 0) Text(tr("ui.hoopono.repetitions", "n" to count), color = Lotus.Plum, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { Sharing.copy(ctx, text) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text(tr("ui.common.copy")) }
                OutlinedButton(onClick = { Sharing.shareText(ctx, text) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(4.dp)); Text(tr("ui.common.share")) }
                OutlinedButton(onClick = { text = build() }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text(tr("ui.common.reset")) }
            }
        }
    }
}
