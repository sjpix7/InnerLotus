@file:OptIn(ExperimentalLayoutApi::class)

package com.sjpixels.innerlotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat

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

/** Line icon for a layer id ("lib:rain", "freq:alpha", "hz:432"). */
private fun layerIcon(id: String): ImageVector {
    val key = id.substringAfter(':')
    return when (id.substringBefore(':')) {
        "lib" -> LotusIcons.sound(key)
        "freq" -> LotusIcons.frequency(VoiceFactory.frequencies.firstOrNull { it.id == key }?.binaural == true)
        else -> LotusIcons.frequency(false)
    }
}

/** Badge for a mix: a fitting picture for the ready-made ones, otherwise its first sound. */
private fun mixIcon(m: Mix): ImageVector = when (m.id) {
    "p_rain_temple" -> LotusIcons.sound("rain")
    "p_himalaya" -> Icons.Outlined.Landscape
    "p_sleep" -> Icons.Outlined.Bedtime
    "p_ocean" -> LotusIcons.sound("ocean")
    "p_evening" -> Icons.Outlined.WbTwilight
    "p_focus" -> Icons.Outlined.Psychology
    "p_heart" -> Icons.Outlined.FavoriteBorder
    "p_ground" -> LotusIcons.sound("fire")
    else -> (m.layers.keys.firstOrNull { it.startsWith("lib:") } ?: m.layers.keys.firstOrNull())?.let(::layerIcon) ?: LotusIcons.Sound
}

/** Rounded highlight behind a playing row inside a list group. */
private val RowShape = RoundedCornerShape(18.dp)

@Composable
fun SoundScreen() {
    var tab by rememberSaveable { mutableStateOf("freq") }
    var saveDialog by remember { mutableStateOf(false) }
    AppScaffold(title = tr("ui.section.sound")) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f)) {
                PageColumn(PaddingValues(0.dp)) {
                    ScreenHeader(title = tr("ui.hero.sound.title"), subtitle = tr("ui.hero.sound.sub"))
                    LearnCard("sound")
                    Segmented(
                        options = listOf("freq" to tr("ui.sound.tab_freq"), "mix" to tr("ui.sound.tab_mix"), "saved" to tr("ui.sound.tab_saved")),
                        selected = tab, onSelect = { tab = it },
                    )
                    when (tab) {
                        "freq" -> FrequencyList()
                        "mix" -> SoundLibrary(onSave = { saveDialog = true })
                        else -> SavedMixes(onCreate = { tab = "mix" })
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        QuietNote(tr("ui.quiet.sound"), icon = Icons.AutoMirrored.Outlined.VolumeDown)
                        Text(
                            tr("ui.sound.safety"), style = LotusType.micro, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        )
                    }
                }
            }
            if (SoundEngine.layers.isNotEmpty()) NowPlayingPanel(onSave = { saveDialog = true })
        }
    }
    if (saveDialog) SaveMixDialog(onDismiss = { saveDialog = false })
}

@Composable
private fun SaveMixDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    LotusDialog(
        onDismiss = onDismiss,
        title = tr("ui.sound.save_mix"),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker(tr("ui.sound.mix_name"))
                LotusTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = tr("ui.sound.my_mix"), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirm = {
            TextButton(onClick = {
                App.mixes.save(name.ifBlank { tr("ui.sound.my_mix") }, SoundEngine.layers.toMap())
                SoundEngine.currentMixName = name
                onDismiss()
                Sharing.toast(ctx, tr("ui.common.saved"))
            }) { Text(tr("ui.common.save"), style = LotusType.button.copy(color = Lotus.Accent)) }
        },
        dismiss = {
            TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel"), style = LotusType.button.copy(color = Lotus.Ink)) }
        },
    )
}

// ------------------------------------------------------------------------------------------------
// Frequencies
// ------------------------------------------------------------------------------------------------

@Composable
private fun FrequencyList() {
    val ctx = LocalContext.current
    val list = VoiceFactory.frequencies
    ListGroup {
        Spacer(Modifier.height(6.dp))
        list.forEachIndexed { i, f ->
            val id = "freq:${f.id}"
            val on = SoundEngine.isOn(id)
            if (i > 0) {
                // No hairline next to a highlighted (playing) row; keep the 1dp so nothing jumps.
                val quiet = on || SoundEngine.isOn("freq:${list[i - 1].id}")
                Box(Modifier.padding(horizontal = 10.dp)) { if (quiet) Spacer(Modifier.height(1.dp)) else RowDivider() }
            }
            FrequencyRow(f, on) { SoundEngine.toggle(ctx, id, 0.6f) }
        }
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * One healing-frequency row: line badge (headphones = binaural beat), name, the full description
 * and a round play / stop button. While it plays the row is tinted and shows its own volume.
 */
@Composable
private fun FrequencyRow(f: VoiceFactory.FreqSpec, on: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(if (on) Lotus.Lavender.copy(alpha = 0.75f) else Color.Transparent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateBadge(LotusIcons.frequency(f.binaural), on)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("sound.freq.${f.id}.name"), style = LotusType.itemTitle)
                Spacer(Modifier.height(2.dp))
                Text(tr("sound.freq.${f.id}.desc"), style = LotusType.itemSub)
            }
            Spacer(Modifier.width(10.dp))
            PlayCircle(playing = on, contentDescription = tr(if (on) "ui.common.stop" else "ui.common.play"))
        }
        if (on) VolumeSlider("freq:${f.id}", Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp))
    }
}

/** Icon badge that turns violet with a white glyph while its sound plays. */
@Composable
private fun StateBadge(icon: ImageVector, on: Boolean) {
    IconBadge(icon, tint = if (on) Color.White else Lotus.BadgeInk, background = if (on) Lotus.AccentSoft else Lotus.BadgeBg)
}

/** Round play affordance at the end of a row: white with an ink glyph, ink when active. */
@Composable
private fun PlayCircle(playing: Boolean, contentDescription: String?, playingIcon: ImageVector = Icons.Outlined.Stop) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(if (playing) Lotus.Ink else Color.White.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) playingIcon else Icons.Outlined.PlayArrow, contentDescription,
            tint = if (playing) Color.White else Lotus.Ink, modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun VolumeSlider(id: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.VolumeDown, null, tint = Lotus.InkSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        LotusSlider(value = SoundEngine.layers[id] ?: 0.6f, onValueChange = { SoundEngine.setVolume(id, it) }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = Lotus.InkSoft, modifier = Modifier.size(18.dp))
    }
}

// ------------------------------------------------------------------------------------------------
// Soundscapes
// ------------------------------------------------------------------------------------------------

@Composable
private fun SoundLibrary(onSave: () -> Unit) {
    val ctx = LocalContext.current
    Text(tr("ui.sound.mix_hint"), style = LotusType.small)
    VoiceFactory.library.forEach { (cat, ids) ->
        SectionHeading(tr("ui.sound.cat_$cat"))
        TileGrid(ids) { key, i, m ->
            val id = "lib:$key"
            val on = SoundEngine.isOn(id)
            // The caption swaps like the prototype: the sound's description, or "Now mixing" while it
            // plays (accent note). Both fit the default tile height, so a row keeps one height.
            PracticeTile(
                icon = LotusIcons.sound(key),
                title = tr("sound.lib.$key.name"),
                modifier = m.semantics { selected = on },
                subtitle = if (on) null else tr("sound.lib.$key.desc"),
                footnote = if (on) tr("ui.sound.now_mixing") else null,
                tone = toneFor(i),
                selected = on,
                onClick = { SoundEngine.toggle(ctx, id, 0.7f) },
            )
        }
    }
    val active = SoundEngine.layers.keys.toList()
    if (active.isNotEmpty()) {
        SectionHeading(tr("ui.sound.now_mixing"))
        GlassCard(strong = true, padding = 16.dp) {
            active.forEachIndexed { i, id ->
                if (i > 0) Box(Modifier.padding(vertical = 6.dp)) { RowDivider() }
                LayerVolume(id)
            }
        }
        PrimaryButton(tr("ui.sound.save_mix"), onClick = onSave, trailingIcon = Icons.Outlined.BookmarkAdd)
    }
}

/** One active layer: icon, name, volume in percent, remove; then its volume slider. */
@Composable
private fun LayerVolume(id: String) {
    val volume = SoundEngine.layers[id] ?: 0.6f
    val percent = remember(I18n.lang) { NumberFormat.getPercentInstance(I18n.lang.locale) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(layerIcon(id), null, tint = Lotus.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(layerName(id), style = LotusType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(percent.format(volume.toDouble()), style = LotusType.micro)
        Spacer(Modifier.width(8.dp))
        CircleIconButton(Icons.Outlined.Close, tr("ui.common.remove"), { SoundEngine.remove(id) }, size = 40.dp, iconSize = 17.dp, tint = Lotus.InkSoft)
    }
    LotusSlider(value = volume, onValueChange = { SoundEngine.setVolume(id, it) })
}

// ------------------------------------------------------------------------------------------------
// Mixes
// ------------------------------------------------------------------------------------------------

@Composable
private fun SavedMixes(onCreate: () -> Unit) {
    val ctx = LocalContext.current
    val mine by App.mixes.mixes.collectAsState()
    var journeyMin by rememberSaveable { mutableIntStateOf(10) }
    SectionHeading(tr("ui.sound.my_mixes"))
    if (mine.isEmpty()) {
        GlassCard {
            Icon(Icons.Outlined.LibraryMusic, null, tint = Lotus.Accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(12.dp))
            Text(tr("ui.sound.no_mixes"), style = LotusType.small)
            Spacer(Modifier.height(14.dp))
            PillButton(tr("ui.sound.tab_mix"), onCreate, icon = Icons.Outlined.Add)
        }
    } else {
        ListGroup {
            mine.forEachIndexed { i, m ->
                if (i > 0) RowDivider()
                MixRow(m, onDelete = { App.mixes.delete(m.id) })
            }
        }
    }
    SectionHeading(tr("ui.sound.presets"))
    ListGroup {
        presets().forEachIndexed { i, m ->
            if (i > 0) RowDivider()
            MixRow(m, onDelete = null)
        }
    }
    GlassCard(tone = Tone.Lavender) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Loop, null, tint = Lotus.Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(tr("ui.sound.journey"), style = LotusType.cardTitle, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("ui.sound.journey_hint"), style = LotusType.small)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { m -> Pill(tr("ui.common.minutes_n", "n" to m), journeyMin == m, { journeyMin = m }) }
        }
        Spacer(Modifier.height(16.dp))
        if (SoundEngine.journey.isNotEmpty()) {
            SecondaryButton(tr("ui.sound.journey_stop"), onClick = { SoundEngine.stopJourney() }, trailingIcon = Icons.Outlined.Stop)
        } else {
            PrimaryButton(tr("ui.sound.journey_start"), onClick = { SoundEngine.startJourney(ctx, mine.ifEmpty { presets() }, journeyMin) })
        }
    }
}

@Composable
private fun MixRow(m: Mix, onDelete: (() -> Unit)?) {
    val ctx = LocalContext.current
    val current = SoundEngine.isPlaying && SoundEngine.currentMixName == m.name
    ListRow(
        title = m.name,
        subtitle = m.layers.keys.joinToString(" · ") { layerName(it) },
        leading = { StateBadge(mixIcon(m), current) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    CircleIconButton(Icons.Outlined.DeleteOutline, tr("ui.common.delete"), onDelete, size = 40.dp, iconSize = 18.dp, tint = Lotus.InkSoft)
                    Spacer(Modifier.width(8.dp))
                }
                PlayCircle(playing = current, contentDescription = tr("ui.common.play"), playingIcon = Icons.Outlined.GraphicEq)
            }
        },
        onClick = { SoundEngine.loadMix(m, ctx) },
    )
}

// ------------------------------------------------------------------------------------------------
// Now playing
// ------------------------------------------------------------------------------------------------

@Composable
private fun NowPlayingPanel(onSave: () -> Unit) {
    val ctx = LocalContext.current
    var timerMenu by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(SoundEngine.sleepEndsAt) {
        while (isActive && SoundEngine.sleepEndsAt > 0) { now = System.currentTimeMillis(); delay(1000) }
    }
    val timerOn = SoundEngine.sleepEndsAt > 0
    val named = SoundEngine.currentMixName.isNotEmpty()
    val title = if (named) SoundEngine.currentMixName else SoundEngine.layers.keys.joinToString(" + ") { layerName(it) }
    val caption = when {
        timerOn -> {
            val left = ((SoundEngine.sleepEndsAt - now) / 60000L).coerceAtLeast(0L) + 1
            tr("ui.sound.sleep_timer") + " · " + tr("ui.common.minutes_n", "n" to left)
        }
        named -> SoundEngine.layers.keys.joinToString(" · ") { layerName(it) }
        else -> null
    }
    GlassSurface(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        shape = RoundedCornerShape(30.dp),
        material = Glass.Strong,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayButton(
                playing = SoundEngine.isPlaying,
                onClick = { if (SoundEngine.isPlaying) SoundEngine.pause() else SoundEngine.resume(ctx) },
                size = 50.dp,
                contentDescription = tr("ui.common.play"),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = LotusType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (caption != null) {
                    Text(
                        caption, style = LotusType.itemSub.copy(color = if (timerOn) Lotus.Accent else Lotus.InkSoft),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box {
                CircleIconButton(
                    Icons.Outlined.Bedtime, tr("ui.sound.sleep_timer"), { timerMenu = true },
                    tint = if (timerOn) Lotus.Accent else Lotus.Ink,
                )
                DropdownMenu(
                    expanded = timerMenu,
                    onDismissRequest = { timerMenu = false },
                    shape = RoundedCornerShape(22.dp),
                    containerColor = Lotus.Sheet,
                ) {
                    Kicker(tr("ui.sound.sleep_timer"), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    listOf(0, 15, 30, 45, 60, 90, 120).forEach { m ->
                        DropdownMenuItem(
                            text = { Text(if (m == 0) tr("ui.common.off") else tr("ui.common.minutes_n", "n" to m), style = LotusType.itemTitle) },
                            onClick = { timerMenu = false; SoundEngine.setSleepTimer(m) },
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            CircleIconButton(Icons.Outlined.BookmarkAdd, tr("ui.sound.save_mix"), onSave)
            Spacer(Modifier.width(6.dp))
            CircleIconButton(Icons.Outlined.Stop, tr("ui.common.stop"), { SoundEngine.stopAll() })
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
            ScreenHeader(title = tr("ui.hero.hoopono.title"), kicker = tr("ui.section.hoopono"))
            // The four phrases: a quiet serif quote, the last two in italic.
            GlassCard(tone = Tone.Lavender, padding = 24.dp) {
                Text(
                    tr("hoopono.p1") + " " + tr("hoopono.p2"),
                    style = LotusType.quote, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("hoopono.p3") + " " + tr("hoopono.p4"),
                    style = LotusType.quote.copy(fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            LearnCard("hooponopono")
            SectionHeading(tr("ui.hoopono.choose_topic"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Catalog.hooponoTopics.forEach { t -> Pill(tr("hoopono.topic.$t.name"), topic == t, { looping = false; topic = t }) }
            }
            if (topic == "person") {
                LotusTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = tr("ui.hoopono.name_label"), singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            GlassCard(padding = 16.dp) {
                Stepper(rounds, 1..21, { rounds = it }, tr("ui.hoopono.rounds"))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeading(tr("ui.hoopono.your_prayer"))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Edit, null, tint = Lotus.InkSoft, modifier = Modifier.padding(top = 3.dp).size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("ui.hoopono.edit_hint"), style = LotusType.small)
                }
            }
            LotusTextField(
                value = text, onValueChange = { text = it },
                placeholder = tr("ui.hoopono.your_prayer"),
                minLines = 8,
                textStyle = LotusType.quote.copy(fontSize = 20.sp, lineHeight = 30.sp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(
                        text = if (looping) tr("ui.common.stop") else tr("ui.hoopono.autoplay"),
                        onClick = { looping = !looping; if (!looping) Speaker.stop() },
                        modifier = Modifier.weight(1f),
                        trailingIcon = if (looping) Icons.Outlined.Stop else Icons.Outlined.Repeat,
                    )
                    ListenButton("hoopono_once", { text }, modifier = Modifier.heightIn(min = 54.dp))
                }
                if (looping || count > 0) {
                    Text(
                        tr("ui.hoopono.repetitions", "n" to count),
                        style = LotusType.itemTitle.copy(color = Lotus.Accent), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(tr("ui.common.copy"), { Sharing.copy(ctx, text) }, icon = Icons.Outlined.ContentCopy)
                PillButton(tr("ui.common.share"), { Sharing.shareText(ctx, text) }, icon = Icons.Outlined.Share)
                PillButton(tr("ui.common.reset"), { text = build() }, icon = Icons.Outlined.Refresh)
            }
        }
    }
}
