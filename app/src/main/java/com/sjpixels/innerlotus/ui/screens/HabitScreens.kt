@file:OptIn(ExperimentalLayoutApi::class)

package com.sjpixels.innerlotus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.Habit
import com.sjpixels.innerlotus.data.HabitAnalytics
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.usage.UsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private val habitEmojis = listOf("✨", "🧘", "💧", "🏃", "📖", "📝", "🙏", "🌿", "🌙", "☀️", "🍎", "🚭", "📵", "🎵", "🌬️", "👆", "❤️", "💪", "🛏️", "📞", "🥗", "🚶", "🧠", "🎨")
private val habitColors = listOf(0xFF8A5CD6, 0xFFE58FB1, 0xFF3FA7A3, 0xFFE2B23C, 0xFF5C8DD6, 0xFF6BBF59, 0xFFE57A4F)
private val defaultEmoji = listOf("🧘", "💧", "🏃", "📖", "📝", "✨", "🙏", "🚶", "🛏️", "☀️", "🍎", "🍬", "📵", "🧘", "🌬️", "👆", "🎵", "🙏", "🚭", "📞")

private fun dayShort(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, I18n.lang.locale)
private fun dowShort(iso: Int): String = DayOfWeek.of(iso).getDisplayName(TextStyle.SHORT, I18n.lang.locale)

@Composable
fun HabitsScreen() {
    val state by App.habits.state.collectAsState()
    val today = LocalDate.now().toEpochDay()
    var day by rememberSaveable { mutableStateOf(today) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Habit?>(null) }
    val scheduled = state.habits.filter { HabitAnalytics.isScheduled(it, day) }
    val others = state.habits.filterNot { HabitAnalytics.isScheduled(it, day) }
    val doneCount = scheduled.count { HabitAnalytics.isDone(state, it, day) }

    AppScaffold(title = tr("ui.section.habits")) { padding ->
        PageColumn(padding) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { App.nav.go(Screen.HabitStats) }, modifier = Modifier.weight(1f)) { Text("📊 " + tr("ui.habits.analytics")) }
                FilledTonalButton(onClick = { App.nav.go(Screen.AppUsage) }, modifier = Modifier.weight(1f)) { Text("📱 " + tr("ui.habits.app_usage")) }
            }
            // week strip to log today or earlier days
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (back in 6 downTo 0) {
                    val d = today - back
                    val ld = LocalDate.ofEpochDay(d)
                    val on = d == day
                    val rate = HabitAnalytics.dayRate(state, d)
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (on) Lotus.Plum else Color.White.copy(alpha = 0.6f))
                            .clickable { day = d }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(dayShort(ld), fontSize = 11.sp, color = if (on) Color.White else Lotus.InkSoft)
                        Text("${ld.dayOfMonth}", fontWeight = FontWeight.Bold, color = if (on) Color.White else Lotus.Ink)
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (rate == 1f) Lotus.Teal else if ((rate ?: 0f) > 0f) Lotus.Gold else Color.Transparent))
                    }
                }
            }
            GlassCard(strong = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(if (scheduled.isEmpty()) 0f else doneCount.toFloat() / scheduled.size, "$doneCount/${scheduled.size}", tr("ui.habits.done"))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (day == today) tr("ui.habits.today") else Dates.full(LocalDate.ofEpochDay(day)), style = MaterialTheme.typography.titleMedium)
                        val best = state.habits.maxOfOrNull { HabitAnalytics.currentStreak(state, it, today) } ?: 0
                        Text("🔥 " + tr("ui.habits.best_streak_now", "n" to best), color = Lotus.InkSoft)
                        if (scheduled.isNotEmpty() && doneCount == scheduled.size) Text("🌸 " + tr("ui.habits.all_done"), color = Lotus.Teal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (state.habits.isEmpty()) {
                GlassCard {
                    Text(tr("ui.habits.empty"), color = Lotus.InkSoft)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        I18n.list("habit.defaults").take(8).forEachIndexed { i, name ->
                            Pill(defaultEmoji.getOrElse(i) { "✨" } + " " + name, false, {
                                App.habits.addHabit(Habit(name = name, emoji = defaultEmoji.getOrElse(i) { "✨" }, color = habitColors[i % habitColors.size]))
                            })
                        }
                    }
                }
            }
            scheduled.forEach { h ->
                HabitRow(h, state.logs[h.id]?.get(day) ?: 0, HabitAnalytics.currentStreak(state, h, today),
                    onChange = { n -> App.habits.setCount(h.id, day, n) },
                    onEdit = { editing = h }, onDelete = { deleting = h },
                    onUp = { App.habits.move(h.id, -1) }, onDown = { App.habits.move(h.id, 1) })
            }
            if (others.isNotEmpty()) {
                Text(tr("ui.habits.not_scheduled"), fontSize = 13.sp, color = Lotus.InkSoft)
                others.forEach { h -> GlassCard(padding = 10.dp, onClick = { editing = h }) { Text(h.emoji + "  " + h.name, color = Lotus.InkSoft) } }
            }
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(tr("ui.habits.add")) }
            HabitDataCard()
        }
    }
    if (adding) HabitDialog(null, onDismiss = { adding = false }) { App.habits.addHabit(it) }
    editing?.let { h -> HabitDialog(h, onDismiss = { editing = null }) { App.habits.updateHabit(it) } }
    deleting?.let { h ->
        ConfirmDialog(tr("ui.common.delete"), tr("ui.habits.delete_confirm", "name" to h.name), tr("ui.common.delete"), { App.habits.deleteHabit(h.id) }, { deleting = null }, danger = true)
    }
}

@Composable
private fun HabitRow(h: Habit, count: Int, streak: Int, onChange: (Int) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val done = count >= h.target
    GlassCard(padding = 10.dp, strong = done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiCircle(h.emoji, 44.dp, Color(h.color).copy(alpha = 0.25f))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text((if (streak > 0) "🔥 $streak  " else "") + if (h.target > 1) "$count / ${h.target}" else "", fontSize = 12.sp, color = Lotus.InkSoft)
            }
            if (h.target > 1) {
                IconButton(onClick = { onChange((count - 1).coerceAtLeast(0)) }) { Icon(Icons.Filled.Remove, null) }
                Text("$count", fontWeight = FontWeight.Bold)
                IconButton(onClick = { onChange(count + 1) }) { Icon(Icons.Filled.Add, null) }
            } else {
                IconButton(onClick = { onChange(if (done) 0 else 1) }) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (done) Color(h.color) else Color.White)
                            .border(2.dp, Color(h.color), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Icon(Icons.Filled.Check, null, tint = Color.White) }
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Lotus.InkSoft) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("ui.common.edit")) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(tr("ui.habits.move_up")) }, onClick = { menu = false; onUp() })
                    DropdownMenuItem(text = { Text(tr("ui.habits.move_down")) }, onClick = { menu = false; onDown() })
                    DropdownMenuItem(text = { Text(tr("ui.common.delete"), color = Lotus.Danger) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun HabitDialog(initial: Habit?, onDismiss: () -> Unit, onSave: (Habit) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✨") }
    var color by remember { mutableStateOf(initial?.color ?: habitColors.first()) }
    var target by remember { mutableStateOf(initial?.target ?: 1) }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("ui.habits.add") else tr("ui.common.edit")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(tr("ui.habits.name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (initial == null) {
                    Text(tr("ui.habits.suggestions"), fontSize = 12.sp, color = Lotus.InkSoft)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        I18n.list("habit.defaults").forEachIndexed { i, s ->
                            Pill(s, name == s, { name = s; emoji = defaultEmoji.getOrElse(i) { emoji } })
                        }
                    }
                }
                Text(tr("ui.habits.icon"), fontSize = 12.sp, color = Lotus.InkSoft)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    habitEmojis.forEach { e ->
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (e == emoji) Lotus.Plum.copy(alpha = 0.25f) else Color.Transparent).clickable { emoji = e }, contentAlignment = Alignment.Center) {
                            Text(e, fontSize = 20.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitColors.forEach { c ->
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(c)).border(if (c == color) 3.dp else 0.dp, Lotus.Ink, CircleShape).clickable { color = c })
                    }
                }
                Stepper(target, 1..30, { target = it }, tr("ui.habits.times_per_day"))
                Text(tr("ui.habits.days") + "  " + if (days.isEmpty()) tr("ui.habits.every_day") else "", fontSize = 12.sp, color = Lotus.InkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (1..7).forEach { d ->
                        val on = d in days
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) Lotus.Plum else Color.White).border(1.dp, Lotus.Plum.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { days = if (on) days - d else days + d }.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(dowShort(d).take(3), fontSize = 11.sp, color = if (on) Color.White else Lotus.Ink, maxLines = 1) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val h = (initial ?: Habit(name = name.trim())).copy(name = name.trim(), emoji = emoji, color = color, target = target, days = days)
                onSave(h); onDismiss()
            }) { Text(tr("ui.common.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel")) } },
    )
}

@Composable
private fun HabitDataCard() {
    val ctx = LocalContext.current
    val exporter = rememberExporter("application/json")
    val importer = rememberImporter { text ->
        val n = try { App.habits.importJson(text) } catch (e: Exception) { -1 }
        Sharing.toast(ctx, if (n < 0) tr("ui.data.import_failed") else tr("ui.data.imported_n", "n" to n))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { exporter("inner_lotus_habits.json") { App.habits.exportJson() } }, modifier = Modifier.weight(1f)) { Text(tr("ui.data.export")) }
        OutlinedButton(onClick = importer, modifier = Modifier.weight(1f)) { Text(tr("ui.data.import")) }
    }
}

@Composable
fun HabitStatsScreen() {
    val state by App.habits.state.collectAsState()
    val today = LocalDate.now().toEpochDay()
    AppScaffold(title = tr("ui.habits.analytics")) { padding ->
        PageColumn(padding) {
            if (state.habits.isEmpty()) {
                GlassCard { Text(tr("ui.habits.empty"), color = Lotus.InkSoft) }
                return@PageColumn
            }
            val week = HabitAnalytics.dailyRates(state, today, 7)
            val month = HabitAnalytics.dailyRates(state, today, 30)
            fun avg(l: List<Pair<Long, Float?>>): Float = l.mapNotNull { it.second }.let { if (it.isEmpty()) 0f else it.average().toFloat() }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(tr("ui.habits.today"), "${((HabitAnalytics.dayRate(state, today) ?: 0f) * 100).toInt()}%", Modifier.weight(1f))
                StatTile(tr("ui.habits.week"), "${(avg(week) * 100).toInt()}%", Modifier.weight(1f))
                StatTile(tr("ui.habits.month"), "${(avg(month) * 100).toInt()}%", Modifier.weight(1f))
                StatTile(tr("ui.habits.perfect_days"), "${HabitAnalytics.perfectDays(state, today, 30)}", Modifier.weight(1f))
            }
            GlassCard {
                Text(tr("ui.habits.last7"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                BarChart(week.map { it.second }, week.map { dayShort(LocalDate.ofEpochDay(it.first)) }, valueLabels = week.map { p -> p.second?.let { "${(it * 100).toInt()}%" } ?: "–" })
            }
            GlassCard {
                Text(tr("ui.habits.heatmap"), style = MaterialTheme.typography.titleMedium)
                Text(tr("ui.habits.heatmap_hint"), fontSize = 12.sp, color = Lotus.InkSoft)
                Spacer(Modifier.height(8.dp))
                val startDay = today - 83 - (LocalDate.ofEpochDay(today - 83).dayOfWeek.value - 1)
                val values = (startDay..today).map { HabitAnalytics.dayRate(state, it) }
                Heatmap(values)
            }
            GlassCard {
                val pattern = HabitAnalytics.weekdayPattern(state, today)
                Text(tr("ui.habits.weekday_pattern"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                BarChart(pattern, (1..7).map { dowShort(it) }, color = Lotus.Teal, height = 90.dp)
                if (pattern.any { it > 0f }) {
                    val best = pattern.indices.maxByOrNull { pattern[it] } ?: 0
                    val worst = pattern.indices.minByOrNull { pattern[it] } ?: 0
                    Spacer(Modifier.height(6.dp))
                    Text(tr("ui.habits.insight", "best" to DayOfWeek.of(best + 1).getDisplayName(TextStyle.FULL, I18n.lang.locale), "worst" to DayOfWeek.of(worst + 1).getDisplayName(TextStyle.FULL, I18n.lang.locale)), color = Lotus.InkSoft, fontSize = 13.sp)
                }
            }
            SectionTitle(tr("ui.habits.per_habit"))
            state.habits.forEach { h ->
                val rate = HabitAnalytics.completionRate(state, h, today, 30)
                GlassCard(padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiCircle(h.emoji, 38.dp, Color(h.color).copy(alpha = 0.25f))
                        Spacer(Modifier.width(10.dp))
                        Text(h.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${(rate * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(h.color))
                    }
                    Spacer(Modifier.height(6.dp))
                    HBar(rate, Color(h.color))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("ui.habits.stats_line", "cur" to HabitAnalytics.currentStreak(state, h, today), "best" to HabitAnalytics.bestStreak(state, h, today), "total" to HabitAnalytics.totalCompletions(state, h)),
                        fontSize = 12.sp, color = Lotus.InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier = modifier, padding = 10.dp, strong = true) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Lotus.PlumDeep, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(label, fontSize = 11.sp, color = Lotus.InkSoft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun AppUsageScreen() {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(UsageRepo.hasPermission(ctx)) }
    var range by rememberSaveable { mutableStateOf("today") }
    LaunchedEffect(granted) {
        while (!granted) { delay(1200); granted = UsageRepo.hasPermission(ctx) }
    }
    val h = tr("ui.time.h"); val m = tr("ui.time.m")
    AppScaffold(title = tr("ui.habits.app_usage")) { padding ->
        PageColumn(padding) {
            if (!granted) {
                GlassCard(strong = true) {
                    Text("📱  " + tr("ui.usage.permission_title"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("ui.usage.permission_text"), color = Lotus.InkSoft)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { UsageRepo.openPermissionSettings(ctx) }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.usage.allow")) }
                }
                return@PageColumn
            }
            Segmented(
                options = listOf("today" to tr("ui.habits.today"), "week" to tr("ui.habits.week"), "month" to tr("ui.habits.month")),
                selected = range, onSelect = { range = it },
            )
            val usage by produceState<List<UsageRepo.AppUsage>?>(null, range) {
                value = withContext(Dispatchers.IO) {
                    val today = LocalDate.now()
                    val days = when (range) { "week" -> 7L; "month" -> 30L; else -> 1L }
                    val start = UsageRepo.startOfDay(today.minusDays(days - 1))
                    try { UsageRepo.usage(ctx, start, System.currentTimeMillis()) } catch (e: Exception) { emptyList() }
                }
            }
            val trend by produceState<List<Pair<LocalDate, Long>>?>(null) {
                value = withContext(Dispatchers.IO) { try { UsageRepo.dailyTotals(ctx, 7) } catch (e: Exception) { emptyList() } }
            }
            val list = usage
            if (list == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val total = list.sumOf { it.millis }
                val days = when (range) { "week" -> 7; "month" -> 30; else -> 1 }
                GlassCard(strong = true) {
                    Text(tr("ui.usage.total"), color = Lotus.InkSoft)
                    Text(UsageRepo.formatDuration(total, h, m), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Lotus.PlumDeep)
                    if (days > 1) Text(tr("ui.usage.avg_day", "t" to UsageRepo.formatDuration(total / days, h, m)), color = Lotus.InkSoft)
                    list.firstOrNull { it.pkg == ctx.packageName }?.let {
                        Text("🌸 " + tr("ui.usage.in_app", "t" to UsageRepo.formatDuration(it.millis, h, m)), color = Lotus.Teal, fontSize = 13.sp)
                    }
                }
                trend?.let { t ->
                    if (t.isNotEmpty()) GlassCard {
                        Text(tr("ui.usage.trend"), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        val max = t.maxOf { it.second }.coerceAtLeast(1L)
                        BarChart(
                            t.map { it.second.toFloat() / max }, t.map { dayShort(it.first) }, color = Lotus.Rose,
                            valueLabels = t.map { UsageRepo.formatDuration(it.second, h, m) },
                        )
                    }
                }
                SectionTitle(tr("ui.usage.top_apps"))
                if (list.isEmpty()) Text(tr("ui.usage.none"), color = Lotus.InkSoft)
                val top = list.take(15)
                val maxMs = top.firstOrNull()?.millis?.coerceAtLeast(1L) ?: 1L
                top.forEach { a ->
                    GlassCard(padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = remember(a.pkg) { UsageRepo.icon(ctx, a.pkg)?.asImageBitmap() }
                            if (icon != null) Image(icon, null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) else EmojiCircle("📱", 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                HBar(a.millis.toFloat() / maxMs, Lotus.Plum, 6.dp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(UsageRepo.formatDuration(a.millis, h, m), fontSize = 13.sp, color = Lotus.InkSoft)
                        }
                    }
                }
                Text(tr("ui.usage.tip"), fontSize = 13.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Text("🔒 " + tr("ui.usage.private"), fontSize = 12.sp, color = Lotus.InkSoft)
        }
    }
}
