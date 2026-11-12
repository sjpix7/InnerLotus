package com.sjpixels.innerlotus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VoiceOverOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Cormorant
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import com.sjpixels.innerlotus.ui.theme.bodyFamily
import com.sjpixels.innerlotus.ui.theme.displayFamily
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun EftScreen() {
    val s = App.settings
    var showPoints by remember { mutableStateOf(false) }
    AppScaffold(title = tr("ui.section.eft")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.eft.title"))
            LearnCard("eft")

            SectionHeading(tr("ui.eft.technique"))
            ListGroup {
                Catalog.eftTechniques.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    TechniqueRow(t, on = s.eftTechnique == t) { s.eftTechnique = t }
                }
            }
            ListGroup {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(LotusIcons.Voice)
                    Spacer(Modifier.width(14.dp))
                    SwitchRow(tr("ui.eft.voice_guide"), s.eftVoice, { s.eftVoice = it }, Modifier.weight(1f))
                }
            }

            // The kicker ties the topic grid to the chosen technique.
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Kicker(tr("eft.tech.${s.eftTechnique}.name"))
                SectionHeading(tr("ui.hero.eft_topics.title"))
                Spacer(Modifier.height(4.dp))
                Text(tr("ui.hero.eft_topics.sub"), style = LotusType.small)
            }
            TileGrid(Catalog.eftTopics) { topic, i, m ->
                PracticeTile(
                    icon = LotusIcons.eftTopic(topic),
                    title = tr("eft.topic.$topic.name"),
                    modifier = m,
                    tone = toneFor(i),
                    minHeight = 116.dp,
                    onClick = { App.nav.go(Screen.EftSession(topic, s.eftTechnique)) },
                )
            }

            PointsGuide(showPoints) { showPoints = !showPoints }
            QuietNote(tr("ui.quiet.eft"))
        }
    }
}

/** One technique in the radio list; the chosen one opens to show its description. */
@Composable
private fun TechniqueRow(id: String, on: Boolean, onSelect: () -> Unit) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        ListRow(
            title = tr("eft.tech.$id.name"),
            modifier = Modifier.semantics { selected = on },
            leading = { IconBadge(techniqueIcon(id)) },
            trailing = {
                Icon(
                    if (on) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (on) Lotus.Accent else Lotus.InkSoft.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp),
                )
            },
            onClick = onSelect,
        )
        if (on) {
            Text(
                tr("eft.tech.$id.desc"),
                style = LotusType.small,
                modifier = Modifier.padding(start = 58.dp, end = 4.dp, bottom = 14.dp),
            )
        }
    }
}

private fun techniqueIcon(id: String): ImageVector = when (id) {
    "full" -> Icons.Outlined.Psychology
    "gentle" -> Icons.Outlined.Air
    "positive" -> Icons.Outlined.WbSunny
    else -> Icons.Outlined.TouchApp
}

/** Expandable reference: the figure with numbered points, then each point and where to find it. */
@Composable
private fun PointsGuide(expanded: Boolean, onToggle: () -> Unit) {
    ListGroup {
        ListRow(
            title = tr("ui.eft.points_title"),
            icon = Icons.Outlined.Place,
            trailing = { ExpandCircle(expanded) },
            onClick = onToggle,
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                RowDivider()
                EftFigure(
                    active = null,
                    numbered = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .aspectRatio(EftArt.labelledBox.width / EftArt.labelledBox.height),
                )
                EftArt.order.forEachIndexed { i, p ->
                    if (i > 0) RowDivider()
                    ListRow(
                        title = tr("eft.point.$p.name"),
                        subtitle = tr("eft.point.$p.where"),
                        leading = { NumberBadge(i + 1) },
                        trailing = null,
                    )
                }
            }
        }
    }
}

/** Round chevron that turns over when the section is open. */
@Composable
private fun ExpandCircle(expanded: Boolean) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "expand")
    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Lotus.Ink, modifier = Modifier.size(18.dp).rotate(turn))
    }
}

/** Point number, matching the labels on the figure (sits in the 44dp badge column). */
@Composable
private fun NumberBadge(n: Int) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Lotus.BadgeBg), contentAlignment = Alignment.Center) {
            Text("$n", style = LotusType.pill.copy(color = Lotus.BadgeInk, fontWeight = FontWeight.Medium))
        }
    }
}

private sealed interface EftStep {
    data object Rate : EftStep
    data class Setup(val statements: List<String>) : EftStep
    data class Point(val point: String, val phrase: String, val round: Int, val number: Int) : EftStep
    data class Gamut(val index: Int, val text: String) : EftStep
    data object Breathe : EftStep
    data object ReRate : EftStep
    data object Result : EftStep
}

private fun buildSteps(topic: String, technique: String): List<EftStep> {
    val steps = ArrayList<EftStep>()
    steps.add(EftStep.Rate)
    steps.add(EftStep.Setup(com.sjpixels.innerlotus.core.I18n.list("eft.topic.$topic.setup")))
    val r1 = com.sjpixels.innerlotus.core.I18n.list("eft.topic.$topic.round1")
    val r2 = com.sjpixels.innerlotus.core.I18n.list("eft.topic.$topic.round2")
    if (technique != "positive") Catalog.eftSequence.forEachIndexed { i, p -> steps.add(EftStep.Point(p, r1.getOrElse(i) { "" }, 1, i + 1)) }
    if (technique == "full") com.sjpixels.innerlotus.core.I18n.list("eft.gamut").forEachIndexed { i, t -> steps.add(EftStep.Gamut(i, t)) }
    Catalog.eftSequence.forEachIndexed { i, p -> steps.add(EftStep.Point(p, r2.getOrElse(i) { "" }, 2, i + 1)) }
    steps.add(EftStep.Breathe)
    steps.add(EftStep.ReRate)
    steps.add(EftStep.Result)
    return steps
}

/** Session stages, one StepBar segment each (round 1 is absent in "positive", the 9 Gamut only in "full"). */
private val EftStep.stage: Int
    get() = when (this) {
        EftStep.Rate -> 0
        is EftStep.Setup -> 1
        is EftStep.Point -> if (round == 1) 2 else 4
        is EftStep.Gamut -> 3
        EftStep.Breathe -> 5
        EftStep.ReRate -> 6
        EftStep.Result -> 7
    }

@Composable
fun EftSessionScreen(topic: String, technique: String) {
    val s = App.settings
    val steps = remember(topic, technique, com.sjpixels.innerlotus.core.I18n.lang) { buildSteps(topic, technique) }
    var index by remember { mutableIntStateOf(0) }
    var before by remember { mutableFloatStateOf(7f) }
    var after by remember { mutableFloatStateOf(7f) }
    var auto by remember { mutableStateOf(s.eftVoice) }
    var round by remember { mutableIntStateOf(1) }
    val step = steps[index.coerceIn(0, steps.size - 1)]
    val gentle = technique == "gentle"
    KeepScreenOn(true)

    fun next() { if (index < steps.size - 1) index++ }
    fun prev() { if (index > 0) index-- }

    // Voice guidance: read the step, give time to tap, then move on.
    LaunchedEffect(index, auto) {
        if (!auto) return@LaunchedEffect
        when (val st = step) {
            is EftStep.Setup -> {
                Speaker.speak(tr("ui.eft.setup_say"))
                for (line in st.statements) { if (!Speaker.speak(line)) return@LaunchedEffect; delay(900) }
                next()
            }
            is EftStep.Point -> {
                Speaker.speak(tr("eft.point.${st.point}.name") + ". " + st.phrase)
                delay(if (gentle) 6500 else 4500)
                next()
            }
            is EftStep.Gamut -> {
                if (st.index == 0) Speaker.speak(tr("ui.eft.gamut_intro"))
                Speaker.speak(st.text)
                delay(2200)
                next()
            }
            EftStep.Breathe -> {
                Speaker.speak(tr("ui.eft.breathe"))
                delay(6000)
                next()
            }
            EftStep.ReRate -> Speaker.speak(tr("ui.eft.rerate"))
            EftStep.Rate -> Speaker.speak(tr("ui.eft.rate_q"))
            EftStep.Result -> {}
        }
    }
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }

    val stages = remember(steps) { steps.map { it.stage }.distinct() }
    val gamutCount = remember(steps) { steps.count { it is EftStep.Gamut } }
    val topicName = tr("eft.topic.$topic.name")
    val techLine = tr("eft.tech.$technique.name") + (if (round > 1) " · " + tr("ui.eft.round_n", "n" to round) else "")
    val statementStyle = LotusType.cardTitle.copy(
        fontWeight = FontWeight.Medium,
        fontStyle = if (trueItalic()) FontStyle.Italic else FontStyle.Normal,
    )

    AppScaffold(title = topicName, actions = { HomeAction() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StepBar(
                count = stages.size,
                done = stages.indexOf(step.stage) + 1,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 2.dp),
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Size the art to the room left by the text, so a step fits on one screen where it can.
                val figureHeight = (maxHeight - 302.dp).coerceIn(170.dp, 280.dp)
                val ratingOrb = (maxHeight - 380.dp).coerceIn(150.dp, 230.dp)
                val breathOrb = (maxHeight - 260.dp).coerceIn(160.dp, 260.dp)
                AnimatedContent(
                    targetState = index.coerceIn(0, steps.size - 1),
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(140)) },
                    label = "eftStep",
                ) { i ->
                    val st = steps[i]
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (st) {
                            EftStep.Rate -> {
                                ScreenHeader(
                                    title = tr("ui.hero.eft_rating.title"),
                                    kicker = tr("ui.hero.eft_rating.kicker") + " · " + techLine,
                                    subtitle = tr("ui.eft.rate_q"),
                                )
                                RatingBlock(topicName, before, ratingOrb) { before = it }
                                QuietNote(tr("ui.eft.safety"))
                            }
                            is EftStep.Setup -> {
                                ScreenHeader(title = tr("eft.point.kc.name"), kicker = techLine, subtitle = tr("ui.eft.setup_hint"), center = true)
                                EftFigure("kc", numbered = false, modifier = Modifier.fillMaxWidth().height(figureHeight))
                                GlassCard(strong = true) {
                                    st.statements.forEachIndexed { k, line ->
                                        if (k > 0) Spacer(Modifier.height(14.dp))
                                        Text("“$line”", style = statementStyle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            is EftStep.Point -> {
                                ScreenHeader(
                                    title = tr("eft.point.${st.point}.name"),
                                    kicker = "$techLine · ${st.number} / ${Catalog.eftSequence.size}",
                                    subtitle = tr("eft.point.${st.point}.where"),
                                    center = true,
                                )
                                EftFigure(st.point, numbered = false, modifier = Modifier.fillMaxWidth().height(figureHeight))
                                PhraseCard(
                                    cue = if (gentle) tr("ui.eft.touch_breathe") else tr("ui.eft.tap_times"),
                                    phrase = st.phrase,
                                    color = if (st.round == 1) Lotus.Ink else Lotus.Accent,
                                )
                            }
                            is EftStep.Gamut -> {
                                ScreenHeader(
                                    title = tr("eft.point.gp.name"),
                                    kicker = "$techLine · ${st.index + 1} / $gamutCount",
                                    subtitle = tr("ui.eft.gamut_intro"),
                                    center = true,
                                )
                                EftFigure("gp", numbered = false, modifier = Modifier.fillMaxWidth().height(figureHeight))
                                GlassCard(strong = true) {
                                    Text(st.text, style = LotusType.quote, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            EftStep.Breathe -> {
                                Kicker(techLine, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                BreathingOrb(breathOrb)
                                Text(tr("ui.eft.breathe"), style = LotusType.quote, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            EftStep.ReRate -> {
                                ScreenHeader(title = tr("ui.eft.rerate"), kicker = techLine)
                                RatingBlock(topicName, after, ratingOrb) { after = it }
                            }
                            EftStep.Result -> {
                                // Held from when the result appeared, so "Another round" doesn't
                                // rewrite this page while it fades out.
                                val from = remember { before }
                                val to = remember { after }
                                val line = remember { techLine }
                                val drop = (from - to).toInt()
                                Spacer(Modifier.height(4.dp))
                                IconBadge(if (drop > 0) Icons.Outlined.Spa else Icons.Outlined.FavoriteBorder, size = 72.dp)
                                ScreenHeader(
                                    title = resultTitle(from, to),
                                    kicker = line,
                                    subtitle = when {
                                        to <= 1f -> tr("ui.eft.result_clear")
                                        drop > 0 -> tr("ui.eft.result_better")
                                        else -> tr("ui.eft.result_same")
                                    },
                                    center = true,
                                )
                                Spacer(Modifier.height(2.dp))
                                PrimaryButton(
                                    tr("ui.eft.another_round"),
                                    onClick = { before = after; round++; index = 1 },
                                    trailingIcon = Icons.Outlined.Replay,
                                )
                                SecondaryButton(tr("ui.common.finish"), onClick = { App.nav.back() })
                            }
                        }
                    }
                }
            }

            // Controls stay put while the steps change above them.
            val controls = when (step) {
                EftStep.Rate -> 0
                EftStep.ReRate -> 1
                EftStep.Result -> 2
                else -> 3
            }
            AnimatedContent(
                targetState = controls,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "eftControls",
            ) { kind ->
                when (kind) {
                    0, 1 -> PrimaryButton(
                        text = if (kind == 0) tr("ui.learn.go_eft") else tr("ui.common.next"),
                        onClick = { Speaker.stop(); next() },
                        trailingIcon = if (kind == 0) Icons.Outlined.ArrowOutward else Icons.AutoMirrored.Outlined.ArrowForward,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    2 -> Spacer(Modifier.fillMaxWidth())
                    else -> Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
                        PlayerRow(
                            playing = auto,
                            onPlayPause = { auto = !auto; if (!auto) Speaker.stop() },
                            left = {
                                CircleIconButton(Icons.Outlined.SkipPrevious, tr("ui.common.previous"), { prev() }, size = 50.dp, iconSize = 22.dp)
                            },
                            right = {
                                CircleIconButton(Icons.Outlined.SkipNext, tr("ui.common.next"), { Speaker.stop(); next() }, size = 50.dp, iconSize = 22.dp)
                            },
                            playDescription = tr("ui.eft.voice_guide"),
                        )
                        QuietNote(
                            if (auto) tr("ui.eft.voice_guide") else tr("ui.eft.voice_guide") + " · " + tr("ui.common.off"),
                            icon = if (auto) Icons.Outlined.RecordVoiceOver else Icons.Outlined.VoiceOverOff,
                        )
                    }
                }
            }
        }
    }
}

/** The serif has a real italic (Latin and Cyrillic); Hindi and CJK fall back to an upright system face. */
private fun trueItalic(): Boolean = displayFamily() == Cormorant

/** "From 7 to 4", with the new number in italic where the serif has a true italic. */
@Composable
private fun resultTitle(before: Float, after: Float): String {
    val b = after.toInt().toString()
    return tr("ui.eft.result", "a" to before.toInt(), "b" to if (trueItalic()) "*$b*" else b)
}

/** Intensity check-in: a soft orb that swells with the number, and the 0–10 slider. */
@Composable
private fun RatingBlock(label: String, value: Float, orb: Dp, onChange: (Float) -> Unit) {
    val swell by animateFloatAsState(0.8f + 0.02f * value.coerceIn(0f, 10f), label = "intensity")
    BreathOrb(diameter = orb, scale = swell, label = label, value = "${value.toInt()}")
    GlassCard {
        LotusSlider(value = value, onValueChange = { onChange(it.toInt().toFloat()) }, valueRange = 0f..10f, steps = 9)
        Row(Modifier.fillMaxWidth()) {
            Text(tr("ui.eft.calm") + " · 0", style = LotusType.itemSub, modifier = Modifier.weight(1f))
            Text(tr("ui.eft.intense") + " · 10", style = LotusType.itemSub, textAlign = TextAlign.End)
        }
    }
}

/** "Tap about 5 to 7 times while you say:" over the phrase, in the quote face. */
@Composable
private fun PhraseCard(cue: String, phrase: String, color: Color) {
    GlassCard(strong = true) {
        Kicker(cue, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (phrase.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("“$phrase”", style = LotusType.quote.copy(color = color), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Slowly breathing orb with a thin breath glyph. */
@Composable
private fun BreathingOrb(diameter: Dp) {
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    Box(contentAlignment = Alignment.Center) {
        BreathOrb(diameter = diameter, scale = breath)
        Icon(Icons.Outlined.Air, contentDescription = null, tint = Lotus.Accent, modifier = Modifier.size(46.dp))
    }
}

// =================================================================================================
//  Tapping-point figure
// =================================================================================================

/**
 * Upper-body figure showing the tapping points (original drawing): a calm head and shoulders in
 * thin lavender line, with a round inset of the hand for the Karate Chop and Gamut points.
 * [active] pulses in rose; [numbered] labels every point in order of use (Karate Chop = 1).
 */
@Composable
fun EftFigure(active: String?, numbered: Boolean, modifier: Modifier = Modifier) {
    val pulse = if (active != null) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.6f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p",
        )
    } else null
    val art = remember { EftArtShapes() }
    val measurer = rememberTextMeasurer()
    val numberStyle = TextStyle(fontFamily = bodyFamily(), fontWeight = FontWeight.Medium, color = Lotus.BadgeInk)
    Canvas(modifier) {
        val box = if (numbered) EftArt.labelledBox else EftArt.plainBox
        val k = min(size.width / box.width, size.height / box.height)
        val ox = (size.width - box.width * k) / 2f - box.left * k
        val oy = (size.height - box.height * k) / 2f - box.top * k
        withTransform({ translate(ox, oy); scale(k, k, pivot = Offset.Zero) }) {
            with(art) { drawArt(active, numbered, pulse?.value ?: 1f) }
        }
        if (numbered) {
            // Numbers are set outside the scaled space so the text stays crisp.
            val style = numberStyle.copy(fontSize = (8.5f * k).toSp())
            EftArt.order.forEachIndexed { i, id ->
                val c = EftArt.labels.getValue(id)
                val t = measurer.measure("${i + 1}", style)
                drawText(t, topLeft = Offset(ox + c.x * k - t.size.width / 2f, oy + c.y * k - t.size.height / 2f))
            }
        }
    }
}

/** Figure layout in drawing units (head and shoulders centred on x = 110, hand lens at the lower right). */
private object EftArt {
    val lens = Offset(182f, 194f)
    const val LENS_R = 34f

    val points: Map<String, Offset> = mapOf(
        "th" to Offset(110f, 9.5f), "eb" to Offset(100f, 58f), "se" to Offset(81f, 70f), "ue" to Offset(93f, 79f),
        "un" to Offset(110f, 89f), "ch" to Offset(110f, 105f), "cb" to Offset(101f, 142f), "ua" to Offset(55f, 178f),
        "kc" to Offset(197.2f, 203f), "gp" to Offset(188.7f, 195.5f),
    )

    /** Number labels, set out from the face so they never crowd each other. */
    val labels: Map<String, Offset> = mapOf(
        "th" to Offset(140f, 9f), "eb" to Offset(50f, 50f), "se" to Offset(36f, 70f), "ue" to Offset(50f, 90f),
        "un" to Offset(160f, 86f), "ch" to Offset(160f, 108f), "cb" to Offset(80f, 154f), "ua" to Offset(30f, 190f),
        "kc" to Offset(209f, 221f), "gp" to Offset(206f, 168f),
    )

    /** Order of use: set-up point, the sequence, then the Gamut point. */
    val order: List<String> get() = listOf("kc") + Catalog.eftSequence + listOf("gp")

    /** Visible area without labels (room for the pulse), and with the number labels around it. */
    val plainBox = Rect(37f, -4f, 224f, 236f)
    val labelledBox = Rect(17f, -2f, 222f, 236f)
}

private class EftArtShapes {
    private val line = Lotus.AccentSoft
    private val fill = Lotus.Cream

    private val body = Path().apply {
        moveTo(97f, 104f); lineTo(97f, 122f)
        quadraticTo(54f, 128f, 48f, 160f)
        lineTo(48f, 236f); lineTo(172f, 236f); lineTo(172f, 160f)
        quadraticTo(166f, 128f, 123f, 122f)
        lineTo(123f, 104f); close()
    }
    private val head = Path().apply {
        moveTo(110f, 8f)
        cubicTo(132f, 8f, 147f, 24f, 147f, 50f)
        cubicTo(147f, 80f, 137f, 104f, 123f, 111f)
        quadraticTo(110f, 117f, 97f, 111f)
        cubicTo(83f, 104f, 73f, 80f, 73f, 50f)
        cubicTo(73f, 24f, 88f, 8f, 110f, 8f)
        close()
    }
    private val face = Path().apply {
        moveTo(74f, 46f); quadraticTo(80f, 24f, 110f, 26f); quadraticTo(140f, 24f, 146f, 46f) // hairline
        moveTo(85f, 60f); quadraticTo(92f, 55f, 100f, 59f) // brows
        moveTo(120f, 59f); quadraticTo(128f, 55f, 135f, 60f)
        moveTo(87f, 70f); quadraticTo(93f, 74f, 99f, 70f) // closed eyes
        moveTo(121f, 70f); quadraticTo(127f, 74f, 133f, 70f)
        moveTo(111f, 66f); lineTo(107f, 83f); lineTo(114f, 83f) // nose
        moveTo(101f, 95f); quadraticTo(110f, 101f, 119f, 95f) // smile
        moveTo(103f, 134f); quadraticTo(86f, 137f, 70f, 136f) // collarbones
        moveTo(117f, 134f); quadraticTo(134f, 137f, 150f, 136f)
    }
    private val arms = Path().apply {
        moveTo(64f, 162f); quadraticTo(62f, 196f, 63f, 236f)
        moveTo(156f, 162f); quadraticTo(158f, 196f, 157f, 236f)
    }

    // Back of a hand, fingers up, little finger on the right: palm, four fingers, wrist (+ thumb).
    private val hand: List<Pair<Rect, Float>> = listOf(
        Rect(168f, 188f, 197f, 218f) to 8f,
        Rect(168.8f, 167f, 175.2f, 196f) to 3.2f,
        Rect(175.4f, 164f, 182f, 196f) to 3.3f,
        Rect(182.2f, 166f, 188.6f, 196f) to 3.2f,
        Rect(189.1f, 173f, 194.9f, 196f) to 2.9f,
        Rect(171.5f, 212f, 193.5f, 242f) to 3f,
    )
    private val thumbA = Offset(170f, 209f)
    private val thumbB = Offset(158f, 193f)
    private val knuckles = Path().apply {
        for (x in listOf(172f, 178.7f, 185.4f, 192f)) { moveTo(x - 2f, 192f); quadraticTo(x, 190f, x + 2f, 192f) }
    }
    private val lensClip = Path().apply { addOval(Rect(EftArt.lens, EftArt.LENS_R - 0.5f)) }

    // The torso fades out at the bottom instead of ending on a hard line.
    private fun fade(c: Color) = Brush.verticalGradient(0f to c, 0.8f to c, 1f to c.copy(alpha = 0f), startY = 0f, endY = 236f)
    private val fillFade = fade(fill)
    private val lineFade = fade(line)
    private val armFade = fade(line.copy(alpha = 0.5f))
    private val dash = PathEffect.dashPathEffect(floatArrayOf(2.4f, 2.4f))

    fun DrawScope.drawArt(active: String?, numbered: Boolean, pulse: Float) {
        val w = 1.6f
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(body, fillFade)
        drawPath(body, lineFade, style = stroke)
        drawPath(arms, armFade, style = stroke)
        drawPath(head, fill)
        drawPath(head, line, style = stroke)
        drawPath(face, line, style = stroke)

        // Hand inset. Outline first (twice the width), then the fills, so the parts read as one shape.
        drawCircle(Color.White.copy(alpha = 0.88f), EftArt.LENS_R, EftArt.lens)
        drawCircle(line.copy(alpha = 0.35f), EftArt.LENS_R, EftArt.lens, style = Stroke(1f))
        clipPath(lensClip) {
            hand.forEach { (r, c) -> drawRoundRect(line, r.topLeft, r.size, CornerRadius(c), style = Stroke(2 * w)) }
            drawLine(line, thumbA, thumbB, strokeWidth = 7f + 2 * w, cap = StrokeCap.Round)
            hand.forEach { (r, c) -> drawRoundRect(fill, r.topLeft, r.size, CornerRadius(c)) }
            drawLine(fill, thumbA, thumbB, strokeWidth = 7f, cap = StrokeCap.Round)
            drawPath(knuckles, line.copy(alpha = 0.6f), style = Stroke(1f, cap = StrokeCap.Round))
        }

        if (numbered) EftArt.order.forEach { id ->
            drawLine(Lotus.Rose.copy(alpha = 0.75f), EftArt.points.getValue(id), EftArt.labels.getValue(id), strokeWidth = 0.9f, pathEffect = dash)
        }
        if (numbered || active == null) EftArt.points.values.forEach { drawCircle(Lotus.Rose, 2.8f, it) }
        active?.let { EftArt.points[it] }?.let { c ->
            drawCircle(Lotus.Rose.copy(alpha = 0.3f), 11f * pulse, c)
            drawCircle(Lotus.Rose, 4f, c)
        }
        if (numbered) EftArt.labels.values.forEach { c ->
            drawCircle(Lotus.BadgeBg, 7.5f, c)
            drawCircle(line.copy(alpha = 0.3f), 7.5f, c, style = Stroke(0.8f))
        }
    }
}
