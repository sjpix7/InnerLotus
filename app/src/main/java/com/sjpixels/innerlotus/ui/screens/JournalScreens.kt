package com.sjpixels.innerlotus.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.JournalEntry
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/** Journal PIN (salted SHA-256, stored only on the device). */
object PinLock {
    val enabled: Boolean get() = App.settings.journalPinHash.isNotEmpty()

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

    fun set(pin: String) {
        val salt = UUID.randomUUID().toString()
        App.settings.journalPinSalt = salt
        App.settings.journalPinHash = hash(salt, pin)
        App.journalUnlocked = true
    }

    fun clear() {
        App.settings.journalPinHash = ""
        App.settings.journalPinSalt = ""
    }

    fun check(pin: String): Boolean = hash(App.settings.journalPinSalt, pin) == App.settings.journalPinHash
}

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    fun date(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun dateTime(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()
    fun full(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(I18n.lang.locale))
    fun medium(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(I18n.lang.locale))
    fun time(ms: Long): String = dateTime(ms).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(I18n.lang.locale))
    fun stamp(ms: Long): String = dateTime(ms).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(I18n.lang.locale))
    fun month(d: LocalDate): String {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(I18n.lang.locale, "MMMMyyyy")
        return d.format(DateTimeFormatter.ofPattern(pattern, I18n.lang.locale))
    }
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun toMillis(dt: LocalDateTime): Long = dt.atZone(zone).toInstant().toEpochMilli()
}

/** Line icon for a journal mode. */
private fun journalModeIcon(mode: String): ImageVector = when (mode) {
    "gratitude" -> Icons.Outlined.VolunteerActivism
    "prompt" -> Icons.Outlined.Lightbulb
    else -> Icons.Outlined.Draw
}

/** Translated mode name; unknown (imported) modes read as free writing. */
private fun journalModeLabel(mode: String): String =
    tr("ui.journal.mode_" + if (mode == "gratitude" || mode == "prompt") mode else "free")

/** "Three good things from today, big or small:" without its closing colon, for headlines and captions. */
private fun journalGratitudeIntro(): String = tr("ui.journal.gratitude_hint").trimEnd(':', '：', ' ', ' ', ' ')

/**
 * The question shown on the overview's prompt card. The prompt editor opened from that card starts
 * with it (instead of drawing another random prompt), then clears it.
 */
private var journalPromptHandoff: String? = null

private fun takeJournalPromptHandoff(prompts: List<String>): String? {
    val p = journalPromptHandoff
    journalPromptHandoff = null
    return p?.takeIf { it in prompts }
}

@Composable
fun JournalScreen() {
    if (PinLock.enabled && !App.journalUnlocked) { PinGate(); return }
    val ctx = LocalContext.current
    val entries by App.journal.entries.collectAsState()
    var group by rememberSaveable { mutableStateOf("day") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var menu by remember { mutableStateOf(false) }
    val prompts = I18n.list("journal.prompts")
    val teaser = rememberSaveable(I18n.lang) { prompts.randomOrNull().orEmpty() }
    val jsonExporter = rememberExporter("application/json")
    val txtExporter = rememberExporter("text/plain")
    val importer = rememberImporter { text ->
        val n = try { App.journal.importJson(text) } catch (e: Exception) { -1 }
        Sharing.toast(ctx, if (n < 0) tr("ui.data.import_failed") else tr("ui.data.imported_n", "n" to n))
    }
    AppScaffold(title = tr("ui.section.journal"), actions = {
        Box {
            CircleIconButton(Icons.Outlined.MoreHoriz, tr("ui.settings.data"), { menu = true })
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = RoundedCornerShape(22.dp),
                containerColor = Lotus.Sheet,
            ) {
                JournalMenuItem(tr("ui.journal.export_json"), Icons.Outlined.Download) {
                    menu = false; jsonExporter("inner_lotus_journal.json") { App.journal.exportJson() }
                }
                JournalMenuItem(tr("ui.journal.export_txt"), Icons.Outlined.Description) {
                    menu = false
                    txtExporter("inner_lotus_journal.txt") {
                        // The plain-text file keeps its original "symbol + mood name" format.
                        App.journal.exportText(App.journal.entries.value, { Dates.stamp(it) }, { m ->
                            Catalog.moods.firstOrNull { it.first == m }?.second.orEmpty() + " " + tr("journal.mood.$m")
                        })
                    }
                }
                JournalMenuItem(tr("ui.data.import"), Icons.Outlined.Upload) { menu = false; importer() }
                JournalMenuItem(tr("ui.journal.lock"), Icons.Outlined.Lock) { menu = false; App.nav.go(Screen.Settings) }
                JournalMenuItem(tr("ui.journal.delete_all"), Icons.Outlined.DeleteForever, color = Lotus.Danger) {
                    menu = false
                    confirm = tr("ui.journal.delete_all_confirm", "n" to entries.size) to { App.journal.deleteAll() }
                }
            }
        }
    }) { padding ->
        PageColumn(padding) {
            ScreenHeader(
                title = tr("ui.hero.journal.title"),
                kicker = tr("ui.hero.journal.kicker"),
                subtitle = tr("ui.hero.journal.sub"),
            )
            LearnCard("journal")
            TileGrid(listOf("free", "gratitude")) { mode, i, m ->
                PracticeTile(
                    icon = journalModeIcon(mode),
                    title = tr("ui.journal.mode_$mode"),
                    subtitle = if (mode == "gratitude") journalGratitudeIntro() else tr("ui.journal.placeholder"),
                    tone = toneFor(i),
                    modifier = m,
                    onClick = { App.nav.go(Screen.JournalEdit(null, mode)) },
                )
            }
            JournalPromptCard(teaser) {
                journalPromptHandoff = teaser.ifBlank { null }
                App.nav.go(Screen.JournalEdit(null, "prompt"))
            }
            MoodSummary(entries)
            SearchField(query, { query = it }, tr("ui.journal.search"))
            Segmented(
                options = listOf("day" to tr("ui.journal.by_day"), "week" to tr("ui.journal.by_week"), "month" to tr("ui.journal.by_month")),
                selected = group, onSelect = { group = it },
            )
            val q = query.trim().lowercase()
            val filtered = entries.filter { q.isEmpty() || it.text.lowercase().contains(q) || it.prompt.lowercase().contains(q) }
            if (filtered.isEmpty()) JournalEmptyCard(searching = entries.isNotEmpty())
            val groups = filtered.groupBy { e ->
                val d = Dates.date(e.created)
                when (group) { "week" -> Dates.weekStart(d); "month" -> d.withDayOfMonth(1); else -> d }
            }
            groups.forEach { (key, list) ->
                val label = when (group) {
                    "week" -> tr("ui.journal.week_of", "date" to Dates.medium(key))
                    "month" -> Dates.month(key)
                    else -> Dates.full(key)
                }
                JournalGroupHeader(label, list.size, onDelete = {
                    val ids = list.map { it.id }.toSet()
                    confirm = tr("ui.journal.delete_group_confirm", "n" to list.size, "label" to label) to { App.journal.deleteWhere { it.id in ids }; Unit }
                })
                list.forEach { e ->
                    EntryRow(e, timeOnly = group == "day", onDelete = { confirm = tr("ui.journal.delete_one_confirm") to { App.journal.delete(e.id) } })
                }
            }
            QuietNote(tr("ui.quiet.private"), icon = Icons.Outlined.Lock)
        }
    }
    confirm?.let { (msg, action) ->
        ConfirmDialog(tr("ui.common.delete"), msg, tr("ui.common.delete"), action, { confirm = null }, danger = true)
    }
}

/** Overflow-menu row: outlined accent icon + label ([color] tints both, e.g. for delete). */
@Composable
private fun JournalMenuItem(text: String, icon: ImageVector, color: Color = Lotus.Ink, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = LotusType.itemTitle.copy(color = color)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (color == Lotus.Ink) Lotus.Accent else color, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

/** Wide blush card: a question to write about, opening the prompt editor with it. */
@Composable
private fun JournalPromptCard(question: String, onClick: () -> Unit) {
    GlassCard(tone = Tone.Blush, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(journalModeIcon("prompt"), contentDescription = null, tint = Lotus.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Kicker(tr("ui.journal.mode_prompt"))
        }
        if (question.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(question, style = LotusType.cardTitle)
        }
        Spacer(Modifier.height(6.dp))
        TextLink(tr("ui.learn.go_journal"), onClick, Modifier.offset(x = (-6).dp))
    }
}

/** Calm card shown when the journal is empty, or when a search finds nothing. */
@Composable
private fun JournalEmptyCard(searching: Boolean) {
    GlassCard(padding = 22.dp) {
        Icon(
            if (searching) Icons.Outlined.SearchOff else LotusIcons.Journal,
            contentDescription = null,
            tint = Lotus.Accent,
            modifier = Modifier.size(26.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (searching) tr("ui.journal.no_match") else tr("ui.journal.empty"),
            style = LotusType.small,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Serif date heading for a day / week / month, its entry count, and a quiet delete. */
@Composable
private fun JournalGroupHeader(label: String, count: Int, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = LotusType.section)
            Text(tr("ui.journal.entries_n", "n" to count), style = LotusType.itemSub)
        }
        IconButton(onClick = onDelete, modifier = Modifier.offset(x = 14.dp)) {
            Icon(Icons.Outlined.DeleteOutline, tr("ui.common.delete"), tint = Lotus.InkSoft, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EntryRow(e: JournalEntry, timeOnly: Boolean, onDelete: () -> Unit) {
    val open: () -> Unit = { App.nav.go(Screen.JournalEdit(e.id, e.mode)) }
    val lines = remember(e.text) { e.text.lines().map { it.trim() }.filter { it.isNotEmpty() } }
    // Prompt entries lead with their question; others with their first line.
    val title = e.prompt.trim().ifEmpty { lines.firstOrNull().orEmpty() }
    val excerpt = (if (e.prompt.isNotBlank()) lines else lines.drop(1)).joinToString(" · ")
    GlassCard(onClick = open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(journalModeIcon(e.mode), contentDescription = journalModeLabel(e.mode), tint = Lotus.Accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Kicker(if (timeOnly) Dates.time(e.created) else Dates.stamp(e.created), Modifier.weight(1f))
            if (Catalog.moods.any { it.first == e.mood }) {
                Spacer(Modifier.width(8.dp))
                JournalMoodTag(e.mood)
            }
        }
        if (title.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(title, style = LotusType.cardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (excerpt.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(excerpt, style = LotusType.small, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextLink(tr("ui.common.edit"), open, Modifier.offset(x = (-6).dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.offset(x = 14.dp)) {
                Icon(Icons.Outlined.DeleteOutline, tr("ui.common.delete"), tint = Lotus.InkSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Small glass pill: mood icon + name. */
@Composable
private fun JournalMoodTag(id: String) {
    GlassSurface(shape = PillShape, material = Glass.Pill) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(LotusIcons.mood(id), contentDescription = null, tint = Lotus.Accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("journal.mood.$id"), style = LotusType.pill.copy(color = Lotus.Ink), maxLines = 1)
        }
    }
}

@Composable
private fun MoodSummary(entries: List<JournalEntry>) {
    val since = System.currentTimeMillis() - 30L * 86_400_000L
    val recent = entries.filter { it.created >= since && it.mood.isNotEmpty() }
    if (recent.isEmpty()) return
    val counts = Catalog.moods.map { (id, _) -> id to recent.count { it.mood == id } }
    val max = counts.maxOf { it.second }.coerceAtLeast(1)
    GlassCard {
        Kicker(tr("ui.journal.mood_30"))
        Spacer(Modifier.height(14.dp))
        JournalMoodCells(counts.map { tr("journal.mood.${it.first}") }) { i, m, cell ->
            val (id, n) = counts[i]
            val top = n == max
            val disc = (cell - 8.dp).coerceAtMost(40.dp)
            Column(m.padding(horizontal = 2.dp).semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
                val glyph = @Composable {
                    Icon(LotusIcons.mood(id), contentDescription = null, tint = if (n > 0) Lotus.Ink else Lotus.InkSoft, modifier = Modifier.size(disc * 0.5f))
                }
                // Most frequent mood(s): the blush "selected" disc of the mood picker; others: plain glass.
                if (top) {
                    Box(
                        Modifier
                            .size(disc)
                            .clip(CircleShape)
                            .background(Lotus.Blush.copy(alpha = 0.6f))
                            .border(1.dp, Lotus.Mauve.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { glyph() }
                } else {
                    GlassSurface(Modifier.size(disc), shape = CircleShape, material = Glass.Pill, contentAlignment = Alignment.Center) { glyph() }
                }
                Spacer(Modifier.height(6.dp))
                Text("$n", style = LotusType.cardTitle.copy(color = if (n > 0) Lotus.Ink else Lotus.InkSoft))
                Text(tr("journal.mood.$id"), style = LotusType.micro, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * One cell per mood: a single row when every label fits its column, otherwise rows of three, so
 * longer mood names (Russian, Italian…) are never cut off on narrow phones.
 */
@Composable
private fun JournalMoodCells(labels: List<String>, cell: @Composable (index: Int, modifier: Modifier, width: Dp) -> Unit) {
    val measurer = rememberTextMeasurer()
    val style = LotusType.micro
    val slack = with(LocalDensity.current) { 8.dp.roundToPx() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val count = labels.size.coerceAtLeast(1)
        val widest = remember(labels, style) { labels.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0 }
        val perRow = if (widest + slack <= constraints.maxWidth / count) count else (count + 1) / 2
        val cellWidth = maxWidth / perRow
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            labels.indices.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { i -> cell(i, Modifier.weight(1f), cellWidth) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun JournalEditScreen(id: String?, mode: String) {
    if (PinLock.enabled && !App.journalUnlocked) { PinGate(); return }
    val ctx = LocalContext.current
    val existing = remember(id) { id?.let { App.journal.get(it) } }
    val entryMode = existing?.mode ?: mode
    val prompts = I18n.list("journal.prompts")
    var created by rememberSaveable { mutableStateOf(existing?.created ?: System.currentTimeMillis()) }
    var prompt by rememberSaveable {
        mutableStateOf(existing?.prompt ?: if (entryMode == "prompt") (takeJournalPromptHandoff(prompts) ?: prompts.randomOrNull()).orEmpty() else "")
    }
    var mood by rememberSaveable { mutableStateOf(existing?.mood ?: "") }
    val initialLines = existing?.text?.split("\n").orEmpty()
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    var g1 by rememberSaveable { mutableStateOf(initialLines.getOrElse(0) { "" }) }
    var g2 by rememberSaveable { mutableStateOf(initialLines.getOrElse(1) { "" }) }
    var g3 by rememberSaveable { mutableStateOf(initialLines.drop(2).joinToString("\n")) }
    var confirmDelete by remember { mutableStateOf(false) }
    val body = if (entryMode == "gratitude") listOf(g1, g2, g3).joinToString("\n") else text
    val pickDate: () -> Unit = {
        val dt = Dates.dateTime(created)
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min ->
                created = Dates.toMillis(LocalDateTime.of(y, m + 1, d, h, min))
            }, dt.hour, dt.minute, android.text.format.DateFormat.is24HourFormat(ctx)).show()
        }, dt.year, dt.monthValue - 1, dt.dayOfMonth).show()
    }

    AppScaffold(title = tr("ui.journal.mode_$entryMode")) { padding ->
        PageColumn(padding) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Kicker(Dates.stamp(created), Modifier.weight(1f))
                    TextLink(tr("ui.journal.change_date"), pickDate, Modifier.offset(x = 6.dp), icon = Icons.Outlined.CalendarMonth)
                }
                when (entryMode) {
                    "prompt" -> {
                        if (prompt.isNotBlank()) {
                            Text(prompt, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                        TextLink(
                            tr("ui.journal.new_prompt"),
                            { prompt = prompts.filter { it != prompt }.randomOrNull() ?: prompt },
                            Modifier.offset(x = (-6).dp),
                            icon = Icons.Outlined.Shuffle,
                        )
                    }
                    "gratitude" -> ScreenHeader(title = journalGratitudeIntro(), subtitle = tr("ui.hero.journal.sub"))
                    else -> ScreenHeader(title = tr("ui.hero.journal.title"), subtitle = tr("ui.hero.journal.sub"))
                }
            }
            if (entryMode == "gratitude") {
                JournalNumberedField(1, g1) { g1 = it }
                JournalNumberedField(2, g2) { g2 = it }
                JournalNumberedField(3, g3) { g3 = it }
            } else {
                LotusTextField(text, { text = it }, tr("ui.journal.placeholder"), minLines = 10)
            }
            SectionHeading(tr("ui.journal.how_feel"))
            val moodIds = Catalog.moods.map { it.first }
            JournalMoodCells(moodIds.map { tr("journal.mood.$it") }) { i, m, cell ->
                val mid = moodIds[i]
                ChoiceIcon(
                    icon = LotusIcons.mood(mid),
                    label = tr("journal.mood.$mid"),
                    selected = mood == mid,
                    onClick = { mood = if (mood == mid) "" else mid },
                    modifier = m,
                    size = (cell - 8.dp).coerceAtMost(54.dp),
                )
            }
            PrimaryButton(
                tr("ui.common.save"),
                onClick = {
                    if (body.isBlank()) {
                        Sharing.toast(ctx, tr("ui.journal.empty_entry"))
                    } else {
                        App.journal.upsert(
                            (existing ?: JournalEntry(mode = entryMode)).copy(created = created, prompt = prompt, text = body.trimEnd(), mood = mood)
                        )
                        Sharing.toast(ctx, tr("ui.common.saved"))
                        App.nav.back()
                    }
                },
                trailingIcon = Icons.Outlined.Check,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ListenButton("journal_entry", { (if (prompt.isNotBlank()) "$prompt. " else "") + body })
                if (existing != null) {
                    PillButton(tr("ui.common.delete"), { confirmDelete = true }, icon = Icons.Outlined.DeleteOutline, color = Lotus.Danger)
                }
            }
            QuietNote(tr("ui.journal.private_note"), icon = Icons.Outlined.Lock)
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(tr("ui.common.delete"), tr("ui.journal.delete_one_confirm"), tr("ui.common.delete"), {
            App.journal.delete(existing.id); App.nav.back()
        }, { confirmDelete = false }, danger = true)
    }
}

/** Gratitude line: "01" kicker over a glass field. */
@Composable
private fun JournalNumberedField(n: Int, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Kicker(n.toString().padStart(2, '0'), Modifier.padding(start = 4.dp))
        LotusTextField(value, onValueChange, placeholder = "", minLines = 2)
    }
}

/** Asks for the journal PIN once per app session. */
@Composable
fun PinGate() {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    AppScaffold(title = tr("ui.section.journal")) { padding ->
        PageColumn(padding) {
            Spacer(Modifier.height(24.dp))
            GlassSurface(
                Modifier.size(88.dp).align(Alignment.CenterHorizontally),
                shape = CircleShape,
                material = Glass.Strong,
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Lotus.Accent, modifier = Modifier.size(34.dp))
            }
            ScreenHeader(title = tr("ui.pin.enter"), kicker = tr("ui.hero.journal.kicker"), center = true)
            JournalPinField(pin, error) { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = false } }
            if (error) {
                Text(tr("ui.pin.wrong"), style = LotusType.small.copy(color = Lotus.Danger), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            PrimaryButton(
                tr("ui.pin.unlock"),
                onClick = { if (PinLock.check(pin)) App.journalUnlocked = true else error = true },
                trailingIcon = Icons.Outlined.LockOpen,
            )
            TextLink(tr("ui.pin.forgot"), { forgot = true }, Modifier.align(Alignment.CenterHorizontally), icon = null)
        }
    }
    if (forgot) {
        ConfirmDialog(tr("ui.pin.forgot"), tr("ui.pin.reset_confirm"), tr("ui.pin.reset"), {
            App.journal.deleteAll(); PinLock.clear(); App.journalUnlocked = true
        }, { forgot = false }, danger = true)
    }
}

/** Glass PIN field: digits masked as large centred dots; a soft danger edge after a wrong PIN. */
@Composable
private fun JournalPinField(value: String, error: Boolean, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val label = tr("ui.pin.enter")
    GlassSurface(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (error) Modifier.border(1.dp, Lotus.Danger.copy(alpha = 0.6f), shape) else Modifier),
        shape = shape,
        material = Glass.Field,
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = LotusType.cardTitle.copy(textAlign = TextAlign.Center, letterSpacing = 0.3.em),
            cursorBrush = SolidColor(Lotus.Accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .semantics { contentDescription = label },
        )
    }
}
