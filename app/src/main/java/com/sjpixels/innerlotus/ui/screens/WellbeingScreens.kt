package com.sjpixels.innerlotus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

// ------------------------------------------------------------------------------------------------
// Breathwork
// ------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreathScreen() {
    val s = App.settings
    AppScaffold(title = tr("ui.section.breath")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.breath.title"), subtitle = tr("ui.hero.breath.sub"))
            LearnCard("breath")
            GlassCard {
                Kicker(tr("ui.breath.duration"))
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 10, 15).forEach { m -> Pill(tr("ui.common.minutes_n", "n" to m), s.breathMinutes == m, { s.breathMinutes = m }) }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(tr("ui.breath.voice"), s.breathVoice, { s.breathVoice = it })
            }
            SectionHeading(tr("ui.section.breath_sub"))
            ListGroup {
                Breath.patterns.entries.forEachIndexed { i, (id, phases) ->
                    if (i > 0) RowDivider()
                    BreathPatternRow(id, phases) { App.nav.go(Screen.BreathSession(id)) }
                }
            }
            GlassCard(tone = Tone.Sage) {
                Text(tr("ui.breath.safety"), style = LotusType.small)
            }
        }
    }
}

/** One breathing pattern: line-icon badge, name, timing ("4 · 4 · 4 · 4"), description, round play. */
@Composable
private fun BreathPatternRow(id: String, phases: List<Breath.Phase>, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(LotusIcons.breath(id))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("breath.$id.name"), style = LotusType.itemTitle)
            Spacer(Modifier.height(2.dp))
            Text(timing(phases), style = LotusType.itemSub.copy(color = Lotus.Accent, letterSpacing = 0.06.em))
            Spacer(Modifier.height(4.dp))
            Text(tr("breath.$id.desc"), style = LotusType.itemSub)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Lotus.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** Phase lengths in seconds, e.g. "4 · 7 · 8" or "5.5 · 5.5". */
private fun timing(phases: List<Breath.Phase>): String =
    phases.joinToString(" · ") { p -> p.seconds.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } }

/** "m:ss" */
private fun clock(seconds: Float): String {
    val s = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
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
        BreathingOrb(
            scale,
            label = if (running) tr("breath.cue.${ph.cue}") else tr("ui.breath.ready"),
            value = if (running) "$left" else null,
        )
        PhaseLine(phases, current = if (running) phaseIdx else -1)
        Spacer(Modifier.height(18.dp))
        ProgressLine((elapsed / total).coerceIn(0f, 1f))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(clock(elapsed), style = LotusType.micro)
            Spacer(Modifier.weight(1f))
            Text(clock(total), style = LotusType.micro)
        }
        Spacer(Modifier.height(14.dp))
        PlayerRow(
            playing = running,
            onPlayPause = { onRunningChange(!running); if (running) Speaker.stop() },
            playDescription = if (running) tr("ui.common.stop") else tr("ui.common.play"),
        )
    }
}

/**
 * The glowing orb, following the breath: the circle's 0.45 (empty) … 1.0 (full) maps onto the
 * orb's gentle 0.72 … 1.0. Reading the animation here keeps per-frame recomposition to the orb.
 */
@Composable
private fun BreathingOrb(scale: Animatable<Float, AnimationVector1D>, label: String, value: String?) {
    val s = 0.72f + (scale.value - 0.45f).coerceIn(0f, 0.55f) / 0.55f * 0.28f
    BreathOrb(diameter = 240.dp, scale = s, label = label, value = value)
}

/** "BREATHE IN · HOLD · BREATHE OUT · HOLD" with the current phase in the accent colour. */
@Composable
private fun PhaseLine(phases: List<Breath.Phase>, current: Int) {
    val locale = I18n.lang.locale
    val line = buildAnnotatedString {
        phases.forEachIndexed { i, p ->
            if (i > 0) append(" · ")
            val cue = tr("breath.cue.${p.cue}").uppercase(locale)
            if (i == current) withStyle(SpanStyle(color = Lotus.Accent)) { append(cue) } else append(cue)
        }
    }
    Text(line, style = LotusType.kicker, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
fun BreathSessionScreen(pattern: String) {
    val s = App.settings
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    KeepScreenOn(running)
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }
    AppScaffold(title = tr("ui.section.breath")) { padding ->
        PageColumn(padding) {
            ScreenHeader(
                title = "*" + tr("breath.$pattern.name") + "*",
                kicker = tr("ui.common.minutes_n", "n" to s.breathMinutes),
                subtitle = tr("breath.$pattern.desc"),
                center = true,
            )
            BreathCircle(pattern, running, s.breathMinutes.toFloat(), s.breathVoice, { running = it }, { finished = true })
            if (finished) {
                GlassCard(tone = Tone.Sage) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Spa, contentDescription = null, tint = Lotus.SageInk, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(tr("ui.breath.done"), style = LotusType.cardTitle, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(
                                tr("ui.breath.journal_it"), { App.nav.go(Screen.JournalEdit(null, "free")) },
                                Modifier.weight(1f).fillMaxHeight(), leadingIcon = LotusIcons.Journal,
                            )
                            SecondaryButton(
                                tr("ui.section.affirmations"), { App.nav.go(Screen.Areas()) },
                                Modifier.weight(1f).fillMaxHeight(), leadingIcon = LotusIcons.Affirmations,
                            )
                        }
                    }
                }
            }
            if (pattern == "nadi") {
                GlassCard(tone = Tone.Lavender) {
                    Row {
                        Icon(LotusIcons.breath(pattern), contentDescription = null, tint = Lotus.Accent, modifier = Modifier.padding(top = 1.dp).size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(tr("ui.breath.nadi_hint"), style = LotusType.small.copy(color = Lotus.Ink))
                    }
                }
            }
            QuietNote(tr("ui.breath.safety"))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Chakra guide
// ------------------------------------------------------------------------------------------------

@Composable
fun ChakraGuideScreen() {
    var open by remember { mutableStateOf<String?>(null) }
    AppScaffold(title = tr("ui.section.chakras")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.chakra.title"), kicker = tr("ui.hero.chakra.kicker"), subtitle = tr("ui.hero.chakra.sub"))
            LearnCard("chakra")
            ListGroup {
                Catalog.chakras.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    val expanded = open == c.id
                    ChakraRow(c, expanded) { open = if (expanded) null else c.id }
                    AnimatedVisibility(visible = expanded) { ChakraDetail(c) }
                }
            }
        }
    }
}

/** Collapsed row: chakra disc, name + Sanskrit, the core statement in the chakra's ink, chevron. */
@Composable
private fun ChakraRow(c: Catalog.Chakra, expanded: Boolean, onToggle: () -> Unit) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "chakraChevron")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChakraDot(c.index, size = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("chakra.${c.id}.name"), style = LotusType.itemTitle)
            Spacer(Modifier.height(2.dp))
            Text(tr("chakra.${c.id}.sanskrit"), style = LotusType.itemSub)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            tr("chakra.${c.id}.core").uppercase(I18n.lang.locale),
            style = LotusType.itemSub.copy(color = Color(c.ink), fontWeight = FontWeight.Medium, letterSpacing = 0.12.em),
            textAlign = TextAlign.End,
            maxLines = 2,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = Lotus.Ink, modifier = Modifier.size(18.dp).rotate(turn))
        }
    }
}

/** The detail lines of an open chakra, each with a thin line icon. */
private val chakraFacts: List<Pair<String, ImageVector>> = listOf(
    "location" to Icons.Outlined.Place,
    "element" to Icons.Outlined.Public,
    "bija" to Icons.Outlined.RecordVoiceOver,
    "theme" to Icons.Outlined.AutoAwesome,
    "balanced" to Icons.Outlined.Spa,
    "imbalanced" to Icons.Outlined.ErrorOutline,
    "practice" to Icons.Outlined.SelfImprovement,
)

/** Open chakra: facts, its seed affirmations, listen / tone, and a way into its affirmations. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChakraDetail(c: Catalog.Chakra) {
    val ctx = LocalContext.current
    val ink = Color(c.ink)
    val soft = Color(c.color)
    Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 18.dp)) {
        chakraFacts.forEach { (k, icon) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(soft.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Kicker(tr("ui.chakra.$k"))
                    Spacer(Modifier.height(3.dp))
                    Text(tr("chakra.${c.id}.$k"), style = MaterialTheme.typography.bodyMedium.copy(color = Lotus.Ink))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(soft.copy(alpha = 0.14f))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            I18n.list("chakra.${c.id}.seeds").forEach { seed ->
                Text(seed, style = LotusType.cardTitle.copy(color = ink, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium))
            }
        }
        Spacer(Modifier.height(14.dp))
        val toneId = "freq:f${c.toneHz.toInt()}"
        val toneOn = SoundEngine.isOn(toneId)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ListenButton("chakra:${c.id}", {
                listOf("name", "location", "element", "theme", "balanced", "imbalanced", "practice").joinToString(". ") { tr("chakra.${c.id}.$it") } + ". " +
                    I18n.list("chakra.${c.id}.seeds").joinToString(" ")
            })
            PillButton(
                "${c.toneHz.toInt()} Hz",
                { SoundEngine.toggle(ctx, toneId, 0.5f) },
                icon = if (toneOn) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                selected = toneOn,
            )
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(tr("ui.section.affirmations"), { App.nav.go(Screen.Areas(c.id)) }, trailingIcon = Icons.Outlined.ArrowOutward)
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
            ScreenHeader(title = tr("sos.intro"), kicker = tr("ui.hero.calm.kicker"), subtitle = tr("sos.breathe"), center = true)
            BreathCircle("calm", breathing, 2f, App.settings.breathVoice, { breathing = it })
            Spacer(Modifier.height(18.dp))
            SectionHeading(tr("sos.ground.title"))
            ListGroup {
                I18n.list("sos.ground").forEachIndexed { i, line ->
                    if (i > 0) RowDivider()
                    GroundingRow(5 - i, line, checked[i]) { checked[i] = !checked[i] }
                }
            }
            GlassCard(tone = Tone.Blush) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Lotus.Rose, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(tr("sos.heart"), style = LotusType.quote, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    ListenButton("sos", { tr("sos.intro") + " " + I18n.list("sos.ground").joinToString(" ") + " " + tr("sos.heart") })
                }
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    tr("ui.calm.tap"), { App.nav.go(Screen.EftSession("anxiety", "gentle")) },
                    Modifier.weight(1f).fillMaxHeight(), leadingIcon = LotusIcons.Eft,
                )
                SecondaryButton(
                    tr("ui.calm.sound"), {
                        SoundEngine.loadMix(Mix("calm", tr("ui.calm.sound_mix"), mapOf("lib:rain" to 0.6f, "lib:bowl" to 0.5f, "freq:f528" to 0.2f)), ctx)
                    },
                    Modifier.weight(1f).fillMaxHeight(), leadingIcon = LotusIcons.Sound,
                )
            }
            GlassCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalPhone, contentDescription = null, tint = Lotus.Danger, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("ui.calm.crisis_title"), style = LotusType.cardTitle.copy(color = Lotus.Danger))
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("sos.crisis"), style = MaterialTheme.typography.bodyMedium.copy(color = Lotus.Ink))
                Spacer(Modifier.height(16.dp))
                CrisisCallButton("Tele-MANAS 14416") {
                    try { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:14416")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextLink("findahelpline.com", { Sharing.openUrl(ctx, Catalog.HELPLINE_URL) })
                }
            }
        }
    }
}

/** One 5-4-3-2-1 step: serif number in a soft badge, the prompt, and a tick once noticed. */
@Composable
private fun GroundingRow(n: Int, text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(if (checked) Lotus.SageInk else Lotus.BadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.headlineSmall.copy(color = if (checked) Color.White else Lotus.BadgeInk))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(color = if (checked) Lotus.InkSoft else Lotus.Ink),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) Lotus.SageInk else Lotus.InkSoft.copy(alpha = 0.45f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Crisis call: a soft-danger pill (the one place the app uses this tone for an action). */
@Composable
private fun CrisisCallButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(PillShape)
            .background(Lotus.Danger)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocalPhone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = LotusType.button.copy(color = Color.White), modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ArrowOutward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
