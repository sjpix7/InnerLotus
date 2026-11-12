@file:OptIn(ExperimentalLayoutApi::class)

package com.sjpixels.innerlotus.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.Habit
import com.sjpixels.innerlotus.data.HabitAnalytics
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import com.sjpixels.innerlotus.usage.UsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

// Habit colours exactly as stored with each habit (unchanged, so existing habits keep theirs).
// They are drawn softly: a pale wash behind the icon, a deeper muted tone for the glyph and a
// gentle tone for bars and swatches.
private val habitColors = listOf(0xFF8A5CD6, 0xFFE58FB1, 0xFF3FA7A3, 0xFFE2B23C, 0xFF5C8DD6, 0xFF6BBF59, 0xFFE57A4F)

private fun habitWash(c: Long): Color = lerp(Color(c), Color.White, 0.82f)
private fun habitInk(c: Long): Color = lerp(Color(c), Lotus.Ink, 0.32f)
private fun habitTone(c: Long): Color = lerp(Color(c), Color.White, 0.22f)

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
            ScreenHeader(title = tr("ui.hero.habits.title"), kicker = tr("ui.hero.habits.kicker"))

            // Week strip to log today or the six days before; a quiet dot marks how each day went
            // (sage: everything done, gold: some done).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (back in 6 downTo 0) {
                    val d = today - back
                    val ld = LocalDate.ofEpochDay(d)
                    val on = d == day
                    val rate = HabitAnalytics.dayRate(state, d)
                    val dot = when {
                        rate == 1f -> if (on) Lotus.Sage else Lotus.SageInk
                        (rate ?: 0f) > 0f -> Lotus.Gold
                        else -> null
                    }
                    Box(Modifier.weight(1f)) {
                        DayChip(dayShort(ld), "${ld.dayOfMonth}", on, { day = d }, Modifier.fillMaxWidth())
                        if (dot != null) {
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(5.dp).clip(CircleShape).background(dot))
                        }
                    }
                }
            }

            // The chosen day at a glance.
            GlassCard(tone = Tone.Lavender) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Kicker(if (day == today) tr("ui.habits.today") else Dates.full(LocalDate.ofEpochDay(day)))
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Text(
                                buildAnnotatedString {
                                    append("$doneCount")
                                    withStyle(SpanStyle(color = Lotus.InkSoft, fontSize = 0.62.em)) { append(" / ${scheduled.size}") }
                                },
                                style = LotusType.stat,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tr("ui.habits.done"), style = LotusType.small, modifier = Modifier.alignByBaseline())
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Ring(if (scheduled.isEmpty()) 0f else doneCount.toFloat() / scheduled.size, "", size = 56.dp, color = Lotus.AccentSoft)
                }
                Spacer(Modifier.height(12.dp))
                val best = state.habits.maxOfOrNull { HabitAnalytics.currentStreak(state, it, today) } ?: 0
                IconLine(Icons.Outlined.LocalFireDepartment, tr("ui.habits.best_streak_now", "n" to best), Lotus.Gold)
                if (scheduled.isNotEmpty() && doneCount == scheduled.size) {
                    Spacer(Modifier.height(6.dp))
                    IconLine(Icons.Outlined.Spa, tr("ui.habits.all_done"), Lotus.SageInk, textColor = Lotus.SageInk)
                }
            }

            if (state.habits.isEmpty()) {
                GlassCard {
                    Text(tr("ui.habits.empty"), style = LotusType.small)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        I18n.list("habit.defaults").take(8).forEachIndexed { i, name ->
                            val key = LotusIcons.suggestionKeys.getOrElse(i) { "sparkle" }
                            PillButton(name, {
                                App.habits.addHabit(Habit(name = name, emoji = key, color = habitColors[i % habitColors.size]))
                            }, icon = LotusIcons.habit(key))
                        }
                    }
                }
            }

            if (scheduled.isNotEmpty()) {
                ListGroup {
                    scheduled.forEachIndexed { i, h ->
                        key(h.id) {
                            if (i > 0) RowDivider()
                            HabitRow(h, state.logs[h.id]?.get(day) ?: 0, HabitAnalytics.currentStreak(state, h, today),
                                onChange = { n -> App.habits.setCount(h.id, day, n) },
                                onEdit = { editing = h }, onDelete = { deleting = h },
                                onUp = { App.habits.move(h.id, -1) }, onDown = { App.habits.move(h.id, 1) })
                        }
                    }
                }
            }

            if (others.isNotEmpty()) {
                Kicker(tr("ui.habits.not_scheduled"), Modifier.padding(start = 4.dp, top = 4.dp))
                ListGroup {
                    others.forEachIndexed { i, h ->
                        if (i > 0) RowDivider()
                        ListRow(
                            title = h.name,
                            titleStyle = LotusType.itemTitle.copy(color = Lotus.InkSoft),
                            leading = { IconBadge(LotusIcons.habit(h.emoji), size = 40.dp, tint = Lotus.InkSoft, background = Lotus.BadgeBg.copy(alpha = 0.6f)) },
                            trailing = { Icon(Icons.Outlined.Edit, null, tint = Lotus.InkSoft, modifier = Modifier.size(18.dp)) },
                            onClick = { editing = h },
                        )
                    }
                }
            }

            PrimaryButton(tr("ui.habits.add"), { adding = true }, leadingIcon = Icons.Outlined.Add)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(tr("ui.habits.analytics"), { App.nav.go(Screen.HabitStats) }, Modifier.weight(1f), leadingIcon = Icons.Outlined.Insights)
                SecondaryButton(tr("ui.habits.app_usage"), { App.nav.go(Screen.AppUsage) }, Modifier.weight(1f), leadingIcon = Icons.Outlined.PhoneAndroid)
            }
            HabitDataCard()
        }
    }
    if (adding) HabitDialog(null, onDismiss = { adding = false }) { App.habits.addHabit(it) }
    editing?.let { h -> HabitDialog(h, onDismiss = { editing = null }) { App.habits.updateHabit(it) } }
    deleting?.let { h ->
        ConfirmDialog(tr("ui.common.delete"), tr("ui.habits.delete_confirm", "name" to h.name), tr("ui.common.delete"), { App.habits.deleteHabit(h.id) }, { deleting = null }, danger = true)
    }
}

/** Small line icon followed by muted text (streak, all done, time in the app). */
@Composable
private fun IconLine(icon: ImageVector, text: String, tint: Color, textColor: Color = Lotus.InkSoft) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = LotusType.small.copy(color = textColor))
    }
}

/** One habit inside the day's list: badge, name, streak / count, completion toggle, overflow menu. */
@Composable
private fun HabitRow(h: Habit, count: Int, streak: Int, onChange: (Int) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onUp: () -> Unit, onDown: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val done = count >= h.target
    val moreLabel = tr("ui.common.edit")
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(LotusIcons.habit(h.emoji), tint = habitInk(h.color), background = habitWash(h.color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(h.name, style = LotusType.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (streak > 0 || h.target > 1) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (streak > 0) {
                        Icon(Icons.Outlined.LocalFireDepartment, null, tint = Lotus.Gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("$streak", style = LotusType.itemSub)
                        if (h.target > 1) Text("  ·  ", style = LotusType.itemSub)
                    }
                    if (h.target > 1) Text("$count / ${h.target}", style = LotusType.itemSub)
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        if (h.target > 1) {
            RoundAction(Icons.Outlined.Remove, "−", enabled = count > 0) { onChange((count - 1).coerceAtLeast(0)) }
            CountToggle(count, h.target) { onChange(count + 1) }
        } else {
            CheckToggle(done, h.name) { onChange(if (done) 0 else 1) }
        }
        Box {
            Box(
                Modifier
                    .size(width = 40.dp, height = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { menu = true }
                    .semantics { contentDescription = moreLabel },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.MoreVert, null, tint = Lotus.InkSoft, modifier = Modifier.size(20.dp)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(22.dp), containerColor = Lotus.Sheet) {
                MenuRow(Icons.Outlined.Edit, tr("ui.common.edit")) { menu = false; onEdit() }
                MenuRow(Icons.Outlined.KeyboardArrowUp, tr("ui.habits.move_up")) { menu = false; onUp() }
                MenuRow(Icons.Outlined.KeyboardArrowDown, tr("ui.habits.move_down")) { menu = false; onDown() }
                MenuRow(Icons.Outlined.DeleteOutline, tr("ui.common.delete"), Lotus.Danger) { menu = false; onDelete() }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, color: Color = Lotus.Ink, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = LotusType.itemTitle.copy(color = color)) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = if (color == Lotus.Ink) Lotus.InkSoft else color, modifier = Modifier.size(20.dp)) },
    )
}

/** Round check-off: an empty circle, then a filled lavender circle with a white check. */
@Composable
private fun CheckToggle(done: Boolean, label: String, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (done) DoneDisc()
        else Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, Lotus.AccentSoft.copy(alpha = 0.55f), CircleShape))
    }
}

/** For habits done several times a day: adds one; a thin arc fills toward the target. */
@Composable
private fun CountToggle(count: Int, target: Int, onAdd: () -> Unit) {
    val done = count >= target
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = "+"; stateDescription = "$count / $target" },
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            DoneDisc()
        } else {
            Canvas(Modifier.size(30.dp)) {
                val stroke = 2.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                drawCircle(Color.White.copy(alpha = 0.7f))
                drawArc(Lotus.AccentSoft.copy(alpha = 0.3f), 0f, 360f, false, topLeft = Offset(stroke / 2, stroke / 2), size = arc, style = Stroke(1.5.dp.toPx()))
                drawArc(
                    Lotus.AccentSoft, -90f, 360f * count / target, false,
                    topLeft = Offset(stroke / 2, stroke / 2), size = arc, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Icon(Icons.Outlined.Add, null, tint = Lotus.Accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DoneDisc() {
    Box(Modifier.size(30.dp).clip(CircleShape).background(Lotus.AccentSoft), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** Small quiet round button (the "one less" of multi-count habits). */
@Composable
private fun RoundAction(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)).border(1.dp, Lotus.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (enabled) Lotus.Ink else Lotus.InkSoft.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
    }
}

/** Add / edit a habit, as a bottom sheet. Stores an icon key (never an emoji). */
@Composable
private fun HabitDialog(initial: Habit?, onDismiss: () -> Unit, onSave: (Habit) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    // Older habits stored an emoji; it maps to the matching icon key here and is saved as that key.
    var icon by remember { mutableStateOf(initial?.let { LotusIcons.habitKey(it.emoji) } ?: "sparkle") }
    var color by remember { mutableStateOf(initial?.color ?: habitColors.first()) }
    var target by remember { mutableStateOf(initial?.target ?: 1) }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    val suggestions = I18n.list("habit.defaults")
    LotusSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (initial == null) tr("ui.habits.add") else tr("ui.common.edit"),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                CircleIconButton(Icons.Outlined.Close, tr("ui.common.close"), onDismiss)
            }
            LotusTextField(name, { name = it }, tr("ui.habits.name"), singleLine = true)

            if (initial == null) {
                Kicker(tr("ui.habits.suggestions"), Modifier.padding(top = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEachIndexed { i, s ->
                        Pill(s, name == s, { name = s; icon = LotusIcons.suggestionKeys.getOrElse(i) { icon } })
                    }
                }
            }

            // Icon: round choices, six to a row (selected = ink).
            Kicker(tr("ui.habits.icon"), Modifier.padding(top = 4.dp))
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LotusIcons.habitKeys.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { k ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                IconChoice(LotusIcons.habit(k), k == icon, iconLabel(k, suggestions)) { icon = k }
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // Colour: small round swatches, the chosen one ringed in ink.
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                habitColors.forEach { c ->
                    val on = c == color
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .selectable(selected = on, role = Role.RadioButton) { color = c }
                                .then(if (on) Modifier.border(1.5.dp, Lotus.Ink, CircleShape) else Modifier)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(habitTone(c)),
                        )
                    }
                }
            }

            Stepper(target, 1..30, { target = it }, tr("ui.habits.times_per_day"))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker(tr("ui.habits.days"))
                if (days.isEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(tr("ui.habits.every_day"), style = LotusType.micro)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..7).forEach { d ->
                    val on = d in days
                    DayToggle(dowShort(d).take(3), on, Modifier.weight(1f)) { days = if (on) days - d else days + d }
                }
            }

            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                tr("ui.common.save"),
                onClick = {
                    val h = (initial ?: Habit(name = name.trim())).copy(name = name.trim(), emoji = icon, color = color, target = target, days = days)
                    onSave(h); onDismiss()
                },
                enabled = name.isNotBlank(),
                trailingIcon = Icons.Outlined.Check,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextLink(tr("ui.common.cancel"), onDismiss, icon = null)
            }
        }
    }
}

/** A translated name for a habit icon, borrowed from the suggestion that uses it (when there is one). */
private fun iconLabel(key: String, suggestions: List<String>): String? =
    LotusIcons.suggestionKeys.indexOf(key).takeIf { it >= 0 }?.let { suggestions.getOrNull(it) }

/** Ink when selected, frosted glass otherwise. [modifier] carries the click / selection. */
@Composable
private fun ChoiceSurface(selected: Boolean, shape: Shape, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    if (selected) Box(modifier.clip(shape).background(Lotus.Ink), contentAlignment = Alignment.Center, content = content)
    else GlassSurface(modifier, shape = shape, material = Glass.Pill, contentAlignment = Alignment.Center, content = content)
}

@Composable
private fun IconChoice(icon: ImageVector, selected: Boolean, label: String?, onClick: () -> Unit) {
    val m = Modifier
        .widthIn(max = 52.dp)
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(CircleShape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics { if (label != null) contentDescription = label }
    ChoiceSurface(selected, CircleShape, m) {
        Icon(icon, null, tint = if (selected) Color.White else Lotus.Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DayToggle(label: String, on: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val m = modifier
        .heightIn(min = 44.dp)
        .clip(shape)
        .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() })
    ChoiceSurface(on, shape, m) {
        Text(label, style = LotusType.pill.copy(color = if (on) Color.White else Lotus.Ink), maxLines = 1)
    }
}

@Composable
private fun HabitDataCard() {
    val ctx = LocalContext.current
    val exporter = rememberExporter("application/json")
    val importer = rememberImporter { text ->
        val n = try { App.habits.importJson(text) } catch (e: Exception) { -1 }
        Sharing.toast(ctx, if (n < 0) tr("ui.data.import_failed") else tr("ui.data.imported_n", "n" to n))
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(tr("ui.data.export"), { exporter("inner_lotus_habits.json") { App.habits.exportJson() } }, icon = Icons.Outlined.FileUpload)
            PillButton(tr("ui.data.import"), importer, icon = Icons.Outlined.FileDownload)
        }
        Spacer(Modifier.height(4.dp))
        QuietNote(tr("ui.quiet.private"), icon = Icons.Outlined.Lock)
    }
}

@Composable
fun HabitStatsScreen() {
    val state by App.habits.state.collectAsState()
    val today = LocalDate.now().toEpochDay()
    AppScaffold(title = tr("ui.habits.analytics")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.stats.title"), kicker = tr("ui.hero.stats.kicker"))
            if (state.habits.isEmpty()) {
                GlassCard { Text(tr("ui.habits.empty"), style = LotusType.small) }
                return@PageColumn
            }
            val week = HabitAnalytics.dailyRates(state, today, 7)
            val month = HabitAnalytics.dailyRates(state, today, 30)
            fun avg(l: List<Pair<Long, Float?>>): Float = l.mapNotNull { it.second }.let { if (it.isEmpty()) 0f else it.average().toFloat() }
            val stats = listOf(
                "${((HabitAnalytics.dayRate(state, today) ?: 0f) * 100).toInt()}%" to tr("ui.habits.today"),
                "${(avg(week) * 100).toInt()}%" to tr("ui.habits.week"),
                "${(avg(month) * 100).toInt()}%" to tr("ui.habits.month"),
                "${HabitAnalytics.perfectDays(state, today, 30)}" to tr("ui.habits.perfect_days"),
            )
            // Two by two; each row as tall as its tallest tile.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (value, label) -> StatTile(value = value, label = label, modifier = Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }

            SectionHeading(tr("ui.habits.last7"))
            GlassCard {
                BarChart(
                    week.map { it.second }, week.map { dayShort(LocalDate.ofEpochDay(it.first)) },
                    color = Lotus.AccentSoft.copy(alpha = 0.55f),
                    valueLabels = week.map { p -> p.second?.let { "${(it * 100).toInt()}%" } ?: "–" },
                )
            }

            SectionHeading(tr("ui.habits.heatmap"))
            GlassCard {
                val startDay = today - 83 - (LocalDate.ofEpochDay(today - 83).dayOfWeek.value - 1)
                val values = (startDay..today).map { HabitAnalytics.dayRate(state, it) }
                Heatmap(values, color = Lotus.AccentSoft, cell = 20.dp)
                Spacer(Modifier.height(12.dp))
                HeatLegend(tr("ui.habits.heatmap_hint"))
            }

            SectionHeading(tr("ui.habits.weekday_pattern"))
            GlassCard {
                val pattern = HabitAnalytics.weekdayPattern(state, today)
                BarChart(pattern, (1..7).map { dowShort(it) }, color = Lotus.Rose.copy(alpha = 0.55f), height = 90.dp)
                if (pattern.any { it > 0f }) {
                    val best = pattern.indices.maxByOrNull { pattern[it] } ?: 0
                    val worst = pattern.indices.minByOrNull { pattern[it] } ?: 0
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tr("ui.habits.insight", "best" to DayOfWeek.of(best + 1).getDisplayName(TextStyle.FULL, I18n.lang.locale), "worst" to DayOfWeek.of(worst + 1).getDisplayName(TextStyle.FULL, I18n.lang.locale)),
                        style = LotusType.small,
                    )
                }
            }

            SectionHeading(tr("ui.habits.per_habit"))
            ListGroup {
                state.habits.forEachIndexed { i, h ->
                    if (i > 0) RowDivider()
                    HabitStatRow(
                        h, HabitAnalytics.completionRate(state, h, today, 30),
                        tr("ui.habits.stats_line", "cur" to HabitAnalytics.currentStreak(state, h, today), "best" to HabitAnalytics.bestStreak(state, h, today), "total" to HabitAnalytics.totalCompletions(state, h)),
                    )
                }
            }
        }
    }
}

/** "Less → more" swatches (same steps as the heatmap) beside the hint. */
@Composable
private fun HeatLegend(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(0.18f, 0.45f, 0.72f, 1f).forEach { a ->
            Box(Modifier.padding(end = 3.dp).size(10.dp).clip(RoundedCornerShape(3.dp)).background(Lotus.AccentSoft.copy(alpha = a)))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = LotusType.micro, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HabitStatRow(h: Habit, rate: Float, line: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(LotusIcons.habit(h.emoji), tint = habitInk(h.color), background = habitWash(h.color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(h.name, style = LotusType.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text("${(rate * 100).toInt()}%", style = LotusType.cardTitle)
            }
            Spacer(Modifier.height(6.dp))
            HBar(rate, habitTone(h.color), 6.dp)
            Spacer(Modifier.height(6.dp))
            Text(line, style = LotusType.itemSub)
        }
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
                    IconBadge(Icons.Outlined.PhoneAndroid)
                    Spacer(Modifier.height(16.dp))
                    Text(tr("ui.usage.permission_title"), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("ui.usage.permission_text"), style = LotusType.small)
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(tr("ui.usage.allow"), { UsageRepo.openPermissionSettings(ctx) })
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
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Lotus.AccentSoft, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                }
            } else {
                val total = list.sumOf { it.millis }
                val days = when (range) { "week" -> 7; "month" -> 30; else -> 1 }
                GlassCard(tone = Tone.Lavender) {
                    Kicker(tr("ui.usage.total"))
                    Spacer(Modifier.height(6.dp))
                    Text(UsageRepo.formatDuration(total, h, m), style = LotusType.stat)
                    if (days > 1) Text(tr("ui.usage.avg_day", "t" to UsageRepo.formatDuration(total / days, h, m)), style = LotusType.small)
                    list.firstOrNull { it.pkg == ctx.packageName }?.let {
                        Spacer(Modifier.height(10.dp))
                        IconLine(Icons.Outlined.Spa, tr("ui.usage.in_app", "t" to UsageRepo.formatDuration(it.millis, h, m)), Lotus.SageInk, textColor = Lotus.SageInk)
                    }
                }
                trend?.let { t ->
                    if (t.isNotEmpty()) {
                        SectionHeading(tr("ui.usage.trend"))
                        GlassCard {
                            val max = t.maxOf { it.second }.coerceAtLeast(1L)
                            BarChart(
                                t.map { it.second.toFloat() / max }, t.map { dayShort(it.first) }, color = Lotus.Rose.copy(alpha = 0.6f),
                                valueLabels = t.map { UsageRepo.formatDuration(it.second, h, m) },
                            )
                        }
                    }
                }
                SectionHeading(tr("ui.usage.top_apps"))
                if (list.isEmpty()) Text(tr("ui.usage.none"), style = LotusType.small)
                val top = list.take(15)
                val maxMs = top.firstOrNull()?.millis?.coerceAtLeast(1L) ?: 1L
                if (top.isNotEmpty()) {
                    ListGroup {
                        top.forEachIndexed { i, a ->
                            if (i > 0) RowDivider()
                            UsageRow(a, a.millis.toFloat() / maxMs, UsageRepo.formatDuration(a.millis, h, m))
                        }
                    }
                }
                GlassCard(tone = Tone.Sage) {
                    Row {
                        Icon(Icons.Outlined.Air, null, tint = Lotus.SageInk, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(tr("ui.usage.tip"), style = LotusType.small.copy(color = Lotus.Ink))
                    }
                }
            }
            QuietNote(tr("ui.usage.private"), icon = Icons.Outlined.Lock)
        }
    }
}

/** List-row styled app line: app icon, name, time, and a thin bar relative to the top app. */
@Composable
private fun UsageRow(a: UsageRepo.AppUsage, fraction: Float, time: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        val icon = remember(a.pkg) { UsageRepo.icon(ctx, a.pkg)?.asImageBitmap() }
        if (icon != null) Image(icon, null, Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
        else IconBadge(Icons.Outlined.PhoneAndroid, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.label, style = LotusType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(time, style = LotusType.itemSub)
            }
            Spacer(Modifier.height(7.dp))
            HBar(fraction, Lotus.AccentSoft.copy(alpha = 0.7f), 5.dp)
        }
    }
}
