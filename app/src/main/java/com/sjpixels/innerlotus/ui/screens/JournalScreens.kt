package com.sjpixels.innerlotus.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.JournalEntry
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
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

private fun moodEmoji(id: String) = Catalog.moods.firstOrNull { it.first == id }?.second ?: ""
private fun modeEmoji(mode: String) = when (mode) { "gratitude" -> "🙏"; "prompt" -> "💭"; else -> "✍️" }

@Composable
fun JournalScreen() {
    if (PinLock.enabled && !App.journalUnlocked) { PinGate(); return }
    val ctx = LocalContext.current
    val entries by App.journal.entries.collectAsState()
    var group by rememberSaveable { mutableStateOf("day") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var menu by remember { mutableStateOf(false) }
    val jsonExporter = rememberExporter("application/json")
    val txtExporter = rememberExporter("text/plain")
    val importer = rememberImporter { text ->
        val n = try { App.journal.importJson(text) } catch (e: Exception) { -1 }
        Sharing.toast(ctx, if (n < 0) tr("ui.data.import_failed") else tr("ui.data.imported_n", "n" to n))
    }
    AppScaffold(title = tr("ui.section.journal"), actions = {
        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Lotus.Ink) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(tr("ui.journal.export_json")) }, leadingIcon = { Icon(Icons.Filled.Download, null) }, onClick = {
                menu = false; jsonExporter("inner_lotus_journal.json") { App.journal.exportJson() }
            })
            DropdownMenuItem(text = { Text(tr("ui.journal.export_txt")) }, leadingIcon = { Icon(Icons.Filled.Description, null) }, onClick = {
                menu = false
                txtExporter("inner_lotus_journal.txt") {
                    App.journal.exportText(App.journal.entries.value, { Dates.stamp(it) }, { m -> moodEmoji(m) + " " + tr("journal.mood.$m") })
                }
            })
            DropdownMenuItem(text = { Text(tr("ui.data.import")) }, leadingIcon = { Icon(Icons.Filled.Upload, null) }, onClick = { menu = false; importer() })
            DropdownMenuItem(text = { Text(tr("ui.journal.lock")) }, leadingIcon = { Icon(Icons.Filled.Lock, null) }, onClick = { menu = false; App.nav.go(Screen.Settings) })
            DropdownMenuItem(text = { Text(tr("ui.journal.delete_all"), color = Lotus.Danger) }, leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = Lotus.Danger) }, onClick = {
                menu = false
                confirm = tr("ui.journal.delete_all_confirm", "n" to entries.size) to { App.journal.deleteAll() }
            })
        }
    }) { padding ->
        PageColumn(padding) {
            LearnCard("journal", "📔")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("free" to "✍️", "gratitude" to "🙏", "prompt" to "💭").forEach { (mode, emoji) ->
                    GlassCard(modifier = Modifier.weight(1f), padding = 10.dp, strong = true, onClick = { App.nav.go(Screen.JournalEdit(null, mode)) }) {
                        Text(emoji, fontSize = 26.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(tr("ui.journal.mode_$mode"), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, minLines = 2)
                    }
                }
            }
            MoodSummary(entries)
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text(tr("ui.journal.search")) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.6f), focusedContainerColor = Color.White.copy(alpha = 0.85f)),
            )
            Segmented(
                options = listOf("day" to tr("ui.journal.by_day"), "week" to tr("ui.journal.by_week"), "month" to tr("ui.journal.by_month")),
                selected = group, onSelect = { group = it },
            )
            val q = query.trim().lowercase()
            val filtered = entries.filter { q.isEmpty() || it.text.lowercase().contains(q) || it.prompt.lowercase().contains(q) }
            if (filtered.isEmpty()) {
                GlassCard { Text(if (entries.isEmpty()) tr("ui.journal.empty") else tr("ui.journal.no_match"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = Lotus.InkSoft) }
            }
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = Lotus.PlumDeep, modifier = Modifier.weight(1f))
                    Text(tr("ui.journal.entries_n", "n" to list.size), fontSize = 12.sp, color = Lotus.InkSoft)
                    IconButton(onClick = {
                        val ids = list.map { it.id }.toSet()
                        confirm = tr("ui.journal.delete_group_confirm", "n" to list.size, "label" to label) to { App.journal.deleteWhere { it.id in ids }; Unit }
                    }) { Icon(Icons.Filled.DeleteOutline, tr("ui.common.delete"), tint = Lotus.InkSoft) }
                }
                list.forEach { e -> EntryRow(e, onDelete = { confirm = tr("ui.journal.delete_one_confirm") to { App.journal.delete(e.id) } }) }
            }
        }
    }
    confirm?.let { (msg, action) ->
        ConfirmDialog(tr("ui.common.delete"), msg, tr("ui.common.delete"), action, { confirm = null }, danger = true)
    }
}

@Composable
private fun EntryRow(e: JournalEntry, onDelete: () -> Unit) {
    GlassCard(padding = 12.dp, onClick = { App.nav.go(Screen.JournalEdit(e.id, e.mode)) }) {
        Row(verticalAlignment = Alignment.Top) {
            Text(moodEmoji(e.mood).ifEmpty { modeEmoji(e.mode) }, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(Dates.stamp(e.created), fontSize = 12.sp, color = Lotus.InkSoft)
                if (e.prompt.isNotBlank()) Text(e.prompt, fontSize = 13.sp, color = Lotus.Plum, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.text.trim().replace("\n", " · "), maxLines = 3, overflow = TextOverflow.Ellipsis, color = Lotus.Ink)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, tr("ui.common.delete"), tint = Lotus.InkSoft, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun MoodSummary(entries: List<JournalEntry>) {
    val since = System.currentTimeMillis() - 30L * 86_400_000L
    val recent = entries.filter { it.created >= since && it.mood.isNotEmpty() }
    if (recent.isEmpty()) return
    val counts = Catalog.moods.map { (id, emoji) -> Triple(id, emoji, recent.count { it.mood == id }) }
    val max = counts.maxOf { it.third }.coerceAtLeast(1)
    GlassCard(padding = 12.dp) {
        Text(tr("ui.journal.mood_30"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        counts.forEach { (id, emoji, n) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(emoji, modifier = Modifier.width(28.dp))
                Text(tr("journal.mood.$id"), fontSize = 12.sp, modifier = Modifier.width(76.dp), maxLines = 1)
                Box(Modifier.weight(1f)) { HBar(n.toFloat() / max, Lotus.Teal) }
                Text(" $n", fontSize = 12.sp, modifier = Modifier.width(30.dp))
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
    var prompt by rememberSaveable { mutableStateOf(existing?.prompt ?: if (entryMode == "prompt") prompts.randomOrNull().orEmpty() else "") }
    var mood by rememberSaveable { mutableStateOf(existing?.mood ?: "") }
    val initialLines = existing?.text?.split("\n").orEmpty()
    var text by rememberSaveable { mutableStateOf(existing?.text ?: "") }
    var g1 by rememberSaveable { mutableStateOf(initialLines.getOrElse(0) { "" }) }
    var g2 by rememberSaveable { mutableStateOf(initialLines.getOrElse(1) { "" }) }
    var g3 by rememberSaveable { mutableStateOf(initialLines.drop(2).joinToString("\n")) }
    var confirmDelete by remember { mutableStateOf(false) }
    val body = if (entryMode == "gratitude") listOf(g1, g2, g3).joinToString("\n") else text
    val fieldColors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.75f), focusedContainerColor = Color.White.copy(alpha = 0.92f))

    AppScaffold(title = tr("ui.journal.mode_$entryMode")) { padding ->
        PageColumn(padding) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                val dt = Dates.dateTime(created)
                DatePickerDialog(ctx, { _, y, m, d ->
                    TimePickerDialog(ctx, { _, h, min ->
                        created = Dates.toMillis(LocalDateTime.of(y, m + 1, d, h, min))
                    }, dt.hour, dt.minute, android.text.format.DateFormat.is24HourFormat(ctx)).show()
                }, dt.year, dt.monthValue - 1, dt.dayOfMonth).show()
            }) {
                Icon(Icons.Filled.Event, null, tint = Lotus.Plum)
                Spacer(Modifier.width(8.dp))
                Text(Dates.stamp(created), color = Lotus.PlumDeep, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Edit, tr("ui.journal.change_date"), tint = Lotus.InkSoft, modifier = Modifier.size(16.dp))
            }
            if (entryMode == "prompt") {
                GlassCard(strong = true, padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💭  $prompt", style = MaterialTheme.typography.titleMedium, color = Lotus.PlumDeep, modifier = Modifier.weight(1f))
                        IconButton(onClick = { prompt = prompts.filter { it != prompt }.randomOrNull() ?: prompt }) { Icon(Icons.Filled.Shuffle, tr("ui.journal.new_prompt")) }
                    }
                }
            }
            if (entryMode == "gratitude") {
                Text(tr("ui.journal.gratitude_hint"), color = Lotus.InkSoft)
                OutlinedTextField(g1, { g1 = it }, label = { Text("1.") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                OutlinedTextField(g2, { g2 = it }, label = { Text("2.") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                OutlinedTextField(g3, { g3 = it }, label = { Text("3.") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            } else {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp), minLines = 10,
                    placeholder = { Text(tr("ui.journal.placeholder")) },
                    shape = RoundedCornerShape(16.dp), colors = fieldColors,
                )
            }
            Text(tr("ui.journal.how_feel"), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Catalog.moods.forEach { (mid, emoji) ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (mood == mid) Lotus.Plum.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { mood = if (mood == mid) "" else mid }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(emoji, fontSize = 26.sp)
                        Text(tr("journal.mood.$mid"), fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            Button(
                onClick = {
                    if (body.isBlank()) { Sharing.toast(ctx, tr("ui.journal.empty_entry")); return@Button }
                    App.journal.upsert(
                        (existing ?: JournalEntry(mode = entryMode)).copy(created = created, prompt = prompt, text = body.trimEnd(), mood = mood)
                    )
                    Sharing.toast(ctx, tr("ui.common.saved"))
                    App.nav.back()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text(tr("ui.common.save")) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ListenButton("journal_entry", { (if (prompt.isNotBlank()) "$prompt. " else "") + body })
                if (existing != null) {
                    OutlinedButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, null, tint = Lotus.Danger); Spacer(Modifier.width(4.dp)); Text(tr("ui.common.delete"), color = Lotus.Danger)
                    }
                }
            }
            Text("🔒 " + tr("ui.journal.private_note"), fontSize = 12.sp, color = Lotus.InkSoft)
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(tr("ui.common.delete"), tr("ui.journal.delete_one_confirm"), tr("ui.common.delete"), {
            App.journal.delete(existing.id); App.nav.back()
        }, { confirmDelete = false }, danger = true)
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
            Spacer(Modifier.height(30.dp))
            GlassCard(strong = true) {
                Text("🔒", fontSize = 44.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(tr("ui.pin.enter"), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { pin = it; error = false } },
                    singleLine = true, isError = error, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = { if (error) Text(tr("ui.pin.wrong")) },
                )
                Button(onClick = { if (PinLock.check(pin)) App.journalUnlocked = true else error = true }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.pin.unlock")) }
                TextButton(onClick = { forgot = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(tr("ui.pin.forgot")) }
            }
        }
    }
    if (forgot) {
        ConfirmDialog(tr("ui.pin.forgot"), tr("ui.pin.reset_confirm"), tr("ui.pin.reset"), {
            App.journal.deleteAll(); PinLock.clear(); App.journalUnlocked = true
        }, { forgot = false }, danger = true)
    }
}
