package com.sjpixels.innerlotus.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.displayFamily
import kotlinx.coroutines.delay

@Composable
fun EftScreen() {
    val s = App.settings
    var showPoints by remember { mutableStateOf(false) }
    AppScaffold(title = tr("ui.section.eft")) { padding ->
        PageColumn(padding) {
            LearnCard("eft", "👆")
            SectionTitle(tr("ui.eft.technique"))
            Catalog.eftTechniques.forEach { t ->
                val on = s.eftTechnique == t
                GlassCard(
                    onClick = { s.eftTechnique = t },
                    strong = on,
                    modifier = if (on) Modifier.border(2.dp, Lotus.Plum, RoundedCornerShape(22.dp)) else Modifier,
                    padding = 12.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = on, onClick = { s.eftTechnique = t })
                        Column {
                            Text(tr("eft.tech.$t.name"), style = MaterialTheme.typography.titleMedium)
                            Text(tr("eft.tech.$t.desc"), fontSize = 13.sp, color = Lotus.InkSoft)
                        }
                    }
                }
            }
            GlassCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔊  " + tr("ui.eft.voice_guide"), modifier = Modifier.weight(1f))
                    Switch(checked = s.eftVoice, onCheckedChange = { s.eftVoice = it })
                }
            }
            SectionTitle(tr("ui.eft.choose_topic"))
            Catalog.eftTopics.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { topic ->
                        GlassCard(modifier = Modifier.weight(1f), padding = 12.dp, onClick = { App.nav.go(Screen.EftSession(topic, s.eftTechnique)) }) {
                            Text(Catalog.eftTopicEmoji[topic] ?: "✨", fontSize = 28.sp)
                            Text(tr("eft.topic.$topic.name"), style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2)
                        }
                    }
                }
            }
            GlassCard(onClick = { showPoints = !showPoints }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍  " + tr("ui.eft.points_title"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(if (showPoints) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
                if (showPoints) {
                    Spacer(Modifier.height(8.dp))
                    EftFigure(active = null, numbered = true, modifier = Modifier.fillMaxWidth().aspectRatio(0.9f))
                    (listOf("kc") + Catalog.eftSequence + listOf("gp")).forEachIndexed { i, p ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Lotus.Plum, modifier = Modifier.width(28.dp))
                            Column {
                                Text(tr("eft.point.$p.name"), fontWeight = FontWeight.SemiBold)
                                Text(tr("eft.point.$p.where"), fontSize = 13.sp, color = Lotus.InkSoft)
                            }
                        }
                    }
                }
            }
            Text(tr("ui.eft.safety"), fontSize = 12.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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

    AppScaffold(title = tr("eft.topic.$topic.name"), actions = { HomeAction() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            LinearProgressIndicator(progress = { (index + 1f) / steps.size }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)))
            Text(
                tr("eft.tech.$technique.name") + (if (round > 1) "  ·  " + tr("ui.eft.round_n", "n" to round) else ""),
                fontSize = 12.sp, color = Lotus.InkSoft, modifier = Modifier.padding(top = 4.dp),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val activePoint = when (step) {
                    is EftStep.Setup -> "kc"
                    is EftStep.Point -> step.point
                    is EftStep.Gamut -> "gp"
                    else -> null
                }
                if (activePoint != null) EftFigure(activePoint, numbered = false, modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(0.9f))
                GlassCard(strong = true) {
                    when (step) {
                        EftStep.Rate -> RatingBlock(tr("ui.eft.rate_q"), before) { before = it }
                        is EftStep.Setup -> {
                            Text(tr("eft.point.kc.name"), style = MaterialTheme.typography.titleLarge, color = Lotus.PlumDeep)
                            Text(tr("ui.eft.setup_hint"), color = Lotus.InkSoft, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            step.statements.forEach {
                                Text("“$it”", fontFamily = displayFamily(), fontSize = 20.sp, fontStyle = FontStyle.Italic, color = Lotus.Ink, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        is EftStep.Point -> {
                            Text("${step.number}/8 · " + tr("eft.point.${step.point}.name"), style = MaterialTheme.typography.titleLarge, color = Lotus.PlumDeep)
                            Text(tr("eft.point.${step.point}.where"), color = Lotus.InkSoft, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(if (gentle) tr("ui.eft.touch_breathe") else tr("ui.eft.tap_times"), fontSize = 13.sp, color = Lotus.Teal, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("“${step.phrase}”", fontFamily = displayFamily(), fontSize = 26.sp, color = if (step.round == 1) Lotus.Ink else Lotus.Plum, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        is EftStep.Gamut -> {
                            Text(tr("eft.point.gp.name") + " · ${step.index + 1}/9", style = MaterialTheme.typography.titleLarge, color = Lotus.PlumDeep)
                            Text(tr("ui.eft.gamut_intro"), color = Lotus.InkSoft, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(step.text, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        EftStep.Breathe -> {
                            Text("🌬️", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(tr("ui.eft.breathe"), fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        EftStep.ReRate -> RatingBlock(tr("ui.eft.rerate"), after) { after = it }
                        EftStep.Result -> {
                            val drop = (before - after).toInt()
                            Text(if (drop > 0) "🌸" else "🤍", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text(tr("ui.eft.result", "a" to before.toInt(), "b" to after.toInt()), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when {
                                    after <= 1f -> tr("ui.eft.result_clear")
                                    drop > 0 -> tr("ui.eft.result_better")
                                    else -> tr("ui.eft.result_same")
                                },
                                color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { before = after; round++; index = 1 }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.eft.another_round")) }
                            OutlinedButton(onClick = { App.nav.back() }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.common.finish")) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { prev() }, enabled = index > 0) { Icon(Icons.Filled.SkipPrevious, null); Text(tr("ui.common.back")) }
                FilledIconButton(onClick = { auto = !auto; if (!auto) Speaker.stop() }, modifier = Modifier.size(56.dp)) {
                    Icon(if (auto) Icons.Filled.Pause else Icons.Filled.RecordVoiceOver, tr("ui.eft.voice_guide"))
                }
                Button(onClick = { Speaker.stop(); next() }, enabled = step != EftStep.Result) { Text(tr("ui.common.next")); Icon(Icons.Filled.SkipNext, null) }
            }
        }
    }
}

@Composable
private fun RatingBlock(question: String, value: Float, onChange: (Float) -> Unit) {
    val faces = listOf("😌", "🙂", "🙂", "😐", "😐", "😕", "😟", "😟", "😣", "😫", "😭")
    Text(question, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(faces[value.toInt().coerceIn(0, 10)], fontSize = 42.sp)
        Spacer(Modifier.width(14.dp))
        Text("${value.toInt()}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Lotus.PlumDeep)
        Text(" / 10", fontSize = 18.sp, color = Lotus.InkSoft)
    }
    Slider(value = value, onValueChange = { onChange(it.toInt().toFloat()) }, valueRange = 0f..10f, steps = 9)
    Row(Modifier.fillMaxWidth()) {
        Text(tr("ui.eft.calm"), fontSize = 12.sp, color = Lotus.InkSoft, modifier = Modifier.weight(1f))
        Text(tr("ui.eft.intense"), fontSize = 12.sp, color = Lotus.InkSoft)
    }
}

/** Upper-body figure showing the tapping points (original drawing). */
@Composable
fun EftFigure(active: String?, numbered: Boolean, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p",
    )
    val points = mapOf(
        "th" to Offset(0.5f, 0.07f), "eb" to Offset(0.46f, 0.205f), "se" to Offset(0.375f, 0.245f),
        "ue" to Offset(0.445f, 0.29f), "un" to Offset(0.5f, 0.325f), "ch" to Offset(0.5f, 0.375f),
        "cb" to Offset(0.43f, 0.585f), "ua" to Offset(0.24f, 0.69f), "kc" to Offset(0.935f, 0.93f),
        "gp" to Offset(0.86f, 0.86f),
    )
    val order = listOf("kc") + Catalog.eftSequence + listOf("gp")
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width
            fun p(x: Float, y: Float) = Offset(x * u, y * u)
            val line = Lotus.PlumDeep.copy(alpha = 0.75f)
            val skin = Color(0xFFFFF3EC)
            val st = Stroke(width = u * 0.008f, cap = StrokeCap.Round)
            // torso
            val torso = Path().apply {
                moveTo(0.46f * u, 0.47f * u)
                cubicTo(0.36f * u, 0.5f * u, 0.24f * u, 0.52f * u, 0.2f * u, 0.6f * u)
                lineTo(0.2f * u, 1.1f * u)
                lineTo(0.8f * u, 1.1f * u)
                lineTo(0.8f * u, 0.6f * u)
                cubicTo(0.76f * u, 0.52f * u, 0.64f * u, 0.5f * u, 0.54f * u, 0.47f * u)
                close()
            }
            drawPath(torso, skin)
            drawPath(torso, line, style = st)
            // neck + head
            drawRect(skin, topLeft = p(0.455f, 0.4f), size = Size(0.09f * u, 0.09f * u))
            drawOval(skin, topLeft = p(0.35f, 0.065f), size = Size(0.3f * u, 0.37f * u))
            drawOval(line, topLeft = p(0.35f, 0.065f), size = Size(0.3f * u, 0.37f * u), style = st)
            // hair
            drawArc(line.copy(alpha = 0.35f), 180f, 180f, false, topLeft = p(0.35f, 0.065f), size = Size(0.3f * u, 0.2f * u), style = Stroke(u * 0.03f))
            // brows, eyes, nose, mouth
            drawLine(line, p(0.415f, 0.215f), p(0.47f, 0.205f), st.width, StrokeCap.Round)
            drawLine(line, p(0.53f, 0.205f), p(0.585f, 0.215f), st.width, StrokeCap.Round)
            drawArc(line, 200f, 140f, false, topLeft = p(0.42f, 0.235f), size = Size(0.05f * u, 0.03f * u), style = st)
            drawArc(line, 200f, 140f, false, topLeft = p(0.53f, 0.235f), size = Size(0.05f * u, 0.03f * u), style = st)
            drawLine(line, p(0.5f, 0.25f), p(0.49f, 0.305f), st.width, StrokeCap.Round)
            drawLine(line, p(0.49f, 0.305f), p(0.51f, 0.31f), st.width, StrokeCap.Round)
            drawArc(line, 20f, 140f, false, topLeft = p(0.465f, 0.33f), size = Size(0.07f * u, 0.03f * u), style = st)
            // collarbones
            drawLine(line, p(0.47f, 0.555f), p(0.32f, 0.575f), st.width, StrokeCap.Round)
            drawLine(line, p(0.53f, 0.555f), p(0.68f, 0.575f), st.width, StrokeCap.Round)
            // arm lines
            drawLine(line, p(0.26f, 0.66f), p(0.26f, 1.05f), st.width, StrokeCap.Round)
            drawLine(line, p(0.74f, 0.66f), p(0.74f, 0.8f), st.width, StrokeCap.Round)
            // hand (palm facing viewer, little-finger edge on the right)
            drawRoundRect(skin, topLeft = p(0.78f, 0.84f), size = Size(0.14f * u, 0.17f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.04f * u))
            drawRoundRect(line, topLeft = p(0.78f, 0.84f), size = Size(0.14f * u, 0.17f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.04f * u), style = st)
            for (i in 0..3) drawLine(line, p(0.795f + i * 0.035f, 0.84f), p(0.795f + i * 0.035f, 0.78f), st.width * 1.6f, StrokeCap.Round)
            // points
            for ((id, o) in points) {
                val c = p(o.x, o.y)
                if (id == active) {
                    drawCircle(Lotus.Rose.copy(alpha = 0.35f), radius = u * 0.06f * pulse, center = c)
                    drawCircle(Lotus.Rose, radius = u * 0.022f, center = c)
                } else {
                    drawCircle(Lotus.Plum.copy(alpha = if (active == null) 0.85f else 0.35f), radius = u * 0.014f, center = c)
                }
            }
        }
        if (numbered) {
            // number badges are drawn as text overlays for readability
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                order.forEachIndexed { i, id ->
                    val o = points.getValue(id)
                    Text(
                        "${i + 1}",
                        fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(x = w * o.x + 6.dp, y = w * o.y - 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Lotus.Plum)
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
