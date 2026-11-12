@file:OptIn(ExperimentalLayoutApi::class)

package com.sjpixels.innerlotus.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.BuildConfig
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.audio.SoundEngine
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.Backup
import com.sjpixels.innerlotus.reminders.Reminders
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val s = App.settings
    val scope = rememberCoroutineScope()
    var pinDialog by remember { mutableStateOf(false) }
    var wipe by remember { mutableStateOf(false) }
    val backupExporter = rememberExporter("application/json")
    val backupImporter = rememberImporter { text ->
        val r = Backup.import(text, App.affirmations, App.journal, App.habits, App.mixes)
        Sharing.toast(ctx, tr("ui.data.backup_imported", "a" to r.affirmations, "j" to r.journal, "h" to r.habits, "m" to r.mixes))
    }
    var pendingReminder by remember { mutableStateOf<String?>(null) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when (pendingReminder) {
            "affirm" -> s.remindAffirm = granted
            "journal" -> s.remindJournal = granted
        }
        pendingReminder = null
        Reminders.apply(ctx)
        if (!granted) Sharing.toast(ctx, tr("ui.settings.notif_denied"))
    }
    fun toggleReminder(kind: String, on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(ctx)) {
            pendingReminder = kind
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (kind == "affirm") s.remindAffirm = on else s.remindJournal = on
        Reminders.apply(ctx)
    }
    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
    }
    fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)

    AppScaffold(title = tr("ui.settings.title")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.settings.title"))

            // Language
            SettingsSection(tr("ui.settings.language"), Icons.Outlined.Language) {
                GlassCard {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Lang.entries.forEach { l -> Pill(l.nativeName, l == I18n.lang, { App.setLanguage(l) }) }
                    }
                    if (I18n.lang.gendered) {
                        Spacer(Modifier.height(16.dp))
                        RowDivider()
                        Spacer(Modifier.height(14.dp))
                        GenderChooser()
                    }
                }
            }

            // Voice
            SettingsSection(tr("ui.settings.voice"), Icons.Outlined.RecordVoiceOver) {
                GlassCard {
                    val available = Speaker.isAvailable(I18n.lang)
                    VoiceStatus(
                        ok = available,
                        text = if (available) tr("ui.settings.voice_ok", "lang" to I18n.lang.nativeName)
                        else tr("ui.settings.voice_missing", "lang" to I18n.lang.nativeName),
                    )
                    Spacer(Modifier.height(16.dp))
                    ValueLabel(tr("ui.settings.rate"), "${"%.1f".format(s.ttsRate)}×")
                    LotusSlider(value = s.ttsRate, onValueChange = { s.ttsRate = it }, valueRange = 0.5f..1.5f)
                    Spacer(Modifier.height(6.dp))
                    ValueLabel(tr("ui.settings.pitch"), "%.1f".format(s.ttsPitch))
                    LotusSlider(value = s.ttsPitch, onValueChange = { s.ttsPitch = it }, valueRange = 0.7f..1.4f)
                    Spacer(Modifier.height(4.dp))
                    SwitchRow(tr("ui.settings.read_core"), s.readCore, { s.readCore = it })
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            tr("ui.settings.test_voice"),
                            { scope.launch { Speaker.speak(tr("ui.settings.voice_sample"), "test") } },
                            icon = Icons.Outlined.PlayArrow,
                        )
                        // Without a voice for this language, installing one is the next step: ink pill.
                        PillButton(tr("ui.tts.install"), { Speaker.openInstallVoices(ctx) }, icon = Icons.Outlined.Download, selected = !available)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextLink(tr("ui.settings.tts_system"), { Speaker.openTtsSettings(ctx) }, Modifier.offset(x = (-6).dp))
                }
            }

            // Background
            SettingsSection(tr("ui.settings.background"), Icons.Outlined.Wallpaper) {
                GlassCard {
                    // A switch rather than a two-option segment: "Change every few seconds" is too long
                    // for half a segmented track in most languages.
                    SwitchRow(tr("ui.settings.bg_rotate"), s.bgMode == "rotate", { s.bgMode = if (it) "rotate" else "fixed" })
                    if (s.bgMode == "rotate") Stepper(s.rotateSeconds, 5..120, { s.rotateSeconds = it }, tr("ui.settings.rotate_every"))
                    Spacer(Modifier.height(8.dp))
                    RowDivider()
                    Spacer(Modifier.height(14.dp))
                    Text(if (s.bgMode == "fixed") tr("ui.settings.bg_pick") else tr("ui.settings.bg_start"), style = LotusType.itemSub)
                    Spacer(Modifier.height(10.dp))
                    ThumbGrid(selected = s.bgFixed) { s.bgFixed = it }
                }
            }

            // Affirmation cards
            SettingsSection(tr("ui.settings.cards"), Icons.Outlined.Style) {
                GlassCard {
                    Text(tr("ui.settings.cards_hint"), style = LotusType.itemTitle)
                    Spacer(Modifier.height(10.dp))
                    Segmented(
                        options = listOf("area", "chakra", "app").map { it to tr("ui.settings.card_$it") },
                        selected = s.cardStyle,
                        onSelect = { s.cardStyle = it },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(tr("ui.settings.card_custom"), style = LotusType.itemSub)
                    Spacer(Modifier.height(10.dp))
                    ThumbGrid(selected = s.cardStyle) { s.cardStyle = it }
                    Spacer(Modifier.height(10.dp))
                    RowDivider()
                    SwitchRow(tr("ui.settings.shuffle"), s.shuffle, { s.shuffle = it })
                }
            }

            // Daily reminders
            SettingsSection(tr("ui.settings.reminders"), Icons.Outlined.NotificationsActive) {
                GlassCard {
                    ReminderRow(tr("ui.settings.remind_affirm"), s.remindAffirm, { toggleReminder("affirm", it) }, fmt(s.remindAffirmAt)) {
                        pickTime(s.remindAffirmAt) { s.remindAffirmAt = it; Reminders.apply(ctx) }
                    }
                    Spacer(Modifier.height(12.dp))
                    RowDivider()
                    Spacer(Modifier.height(2.dp))
                    ReminderRow(tr("ui.settings.remind_journal"), s.remindJournal, { toggleReminder("journal", it) }, fmt(s.remindJournalAt)) {
                        pickTime(s.remindJournalAt) { s.remindJournalAt = it; Reminders.apply(ctx) }
                    }
                }
            }

            // Privacy
            SettingsSection(tr("ui.settings.privacy"), Icons.Outlined.Lock) {
                GlassCard {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("ui.settings.journal_pin"), style = LotusType.itemTitle)
                            Text(if (PinLock.enabled) tr("ui.settings.pin_on") else tr("ui.settings.pin_off"), style = LotusType.itemSub)
                        }
                        Spacer(Modifier.width(12.dp))
                        PillButton(if (PinLock.enabled) tr("ui.common.change") else tr("ui.settings.set_pin"), { pinDialog = true })
                    }
                    if (PinLock.enabled) {
                        TextLink(
                            tr("ui.settings.remove_pin"),
                            { PinLock.clear(); Sharing.toast(ctx, tr("ui.settings.pin_removed")) },
                            Modifier.offset(x = (-6).dp),
                            icon = null,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    RowDivider()
                    SwitchRow(tr("ui.settings.keep_screen"), s.keepScreenOn, { s.keepScreenOn = it })
                }
                PrivacyNote()
            }

            // Your data
            SettingsSection(tr("ui.settings.data"), Icons.Outlined.Backup) {
                GlassCard {
                    Text(tr("ui.settings.data_hint"), style = LotusType.small)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        tr("ui.data.export_all"),
                        { backupExporter("inner_lotus_backup.json") { Backup.export(App.affirmations, App.journal, App.habits, App.mixes) } },
                        trailingIcon = Icons.Outlined.SaveAlt,
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(tr("ui.data.import"), backupImporter, trailingIcon = Icons.Outlined.FolderOpen)
                    Spacer(Modifier.height(16.dp))
                    RowDivider()
                    Spacer(Modifier.height(14.dp))
                    PillButton(tr("ui.settings.delete_everything"), { wipe = true }, icon = Icons.Outlined.DeleteOutline, color = Lotus.Danger)
                }
            }

            // About
            ListGroup(Modifier.padding(top = 10.dp)) {
                ListRow(
                    tr("ui.about.title"),
                    subtitle = tr("ui.about.version", "v" to BuildConfig.VERSION_NAME),
                    icon = Icons.Outlined.Info,
                    onClick = { App.nav.go(Screen.About) },
                )
            }
            MantraMalaCard()
        }
    }
    if (pinDialog) PinSetDialog { pinDialog = false }
    if (wipe) {
        ConfirmDialog(tr("ui.settings.delete_everything"), tr("ui.settings.delete_everything_confirm"), tr("ui.common.delete"), {
            SoundEngine.stopAll()
            App.affirmations.clearAll(); App.journal.deleteAll(); App.habits.clearAll(); App.mixes.clearAll()
            PinLock.clear()
            s.remindAffirm = false; s.remindJournal = false
            Reminders.apply(ctx)
            Sharing.toast(ctx, tr("ui.settings.deleted"))
        }, { wipe = false }, danger = true)
    }
}

/** One settings group: a small letter-spaced label with a thin accent icon, then its card(s). */
@Composable
private fun SettingsSection(label: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.padding(start = 4.dp).semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(9.dp))
            Kicker(label)
        }
        content()
    }
}

/** "A voice for English is installed…" with a small check (or a soft warning when missing). */
@Composable
private fun VoiceStatus(ok: Boolean, text: String) {
    Row {
        Icon(
            if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null,
            tint = if (ok) Lotus.SageInk else Lotus.Danger,
            modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = LotusType.small.copy(color = if (ok) Lotus.InkSoft else Lotus.Danger))
    }
}

/** Slider caption: "Speaking speed · 0.9×". */
@Composable
private fun ValueLabel(label: String, value: String) {
    Text(
        buildAnnotatedString {
            append(label)
            withStyle(SpanStyle(color = Lotus.InkSoft)) { append("  ·  $value") }
        },
        style = LotusType.itemTitle,
    )
}

/** Reminder switch with its time below as a small pill that opens the time picker. */
@Composable
private fun ReminderRow(title: String, on: Boolean, onToggle: (Boolean) -> Unit, time: String, onPickTime: () -> Unit) {
    SwitchRow(title, on, onToggle)
    PillButton(time, onPickTime, icon = Icons.Outlined.Schedule, color = if (on) Lotus.Ink else Lotus.InkSoft)
}

/** The privacy promise in a sage glass card; its first short paragraph becomes the serif title. */
@Composable
private fun PrivacyNote() {
    val paras = paragraphs(tr("legal.privacy"))
    val lead = paras.firstOrNull()?.takeIf { paras.size > 1 && it.length <= 80 }
    GlassCard(tone = Tone.Sage) {
        Icon(Icons.Outlined.Spa, null, tint = Lotus.Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(12.dp))
        if (lead != null) {
            Text(lead, style = LotusType.cardTitle)
            Spacer(Modifier.height(10.dp))
        }
        (if (lead != null) paras.drop(1) else paras).forEachIndexed { i, p ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Text(p, style = LotusType.small)
        }
    }
}

/** Splits "…\n\n…" text into trimmed paragraphs. */
private fun paragraphs(text: String): List<String> =
    text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }

/** Every background as a rounded thumbnail, five per row (the glass lotus "dawn" first). */
@Composable
private fun ThumbGrid(selected: String, onSelect: (String) -> Unit) {
    val perRow = 5
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Catalog.backgrounds.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { id -> Thumb(id, id == selected, { onSelect(id) }, Modifier.weight(1f)) }
                // Keep the last, shorter row on the same grid.
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** One thumbnail: 18dp rounded image; when chosen, a violet ring around it and a check badge. */
@Composable
private fun Thumb(id: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val img = rememberBg(id, 4)
    val ring = RoundedCornerShape(22.dp)
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(0.75f)
            .clip(ring)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .then(if (selected) Modifier.border(2.dp, Lotus.AccentSoft, ring) else Modifier)
            .padding(4.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(shape).background(Lotus.Lavender)) {
            if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = 0.7f), shape))
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Lotus.AccentSoft)
                    .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun PinSetDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val ok = a.length in 4..8 && a == b
    LotusDialog(
        onDismiss = onDismiss,
        title = tr("ui.settings.set_pin"),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("ui.pin.hint"), style = LotusType.small)
                Spacer(Modifier.height(2.dp))
                PinField(a, { if (it.length <= 8 && it.all(Char::isDigit)) a = it }, tr("ui.pin.new"))
                PinField(b, { if (it.length <= 8 && it.all(Char::isDigit)) b = it }, tr("ui.pin.repeat"))
            }
        },
        confirm = {
            TextButton(enabled = ok, onClick = { PinLock.set(a); Sharing.toast(ctx, tr("ui.settings.pin_set_done")); onDismiss() }) {
                Text(tr("ui.common.save"), style = LotusType.button.copy(color = if (ok) Lotus.Accent else Lotus.InkSoft.copy(alpha = 0.5f)))
            }
        },
        dismiss = {
            TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel"), style = LotusType.button.copy(color = Lotus.Ink)) }
        },
    )
}

/**
 * Glass digit field that hides what is typed — the look of [LotusTextField], plus the password
 * mask and number pad a PIN needs.
 */
@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val style = MaterialTheme.typography.bodyLarge.copy(color = Lotus.Ink)
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), material = Glass.Field) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics { contentDescription = placeholder },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(Lotus.Accent),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = Lotus.InkSoft.copy(alpha = 0.8f)))
                    inner()
                }
            },
        )
    }
}

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    val refs = listOf(
        "Cascio C.N. et al. (2016). Self-affirmation activates brain systems associated with self-related processing and reward. Social Cognitive and Affective Neuroscience.",
        "Wood J.V., Perunovic W.Q., Lee J.W. (2009). Positive self-statements: power for some, peril for others. Psychological Science.",
        "Church D., Yount G., Brooks A. (2012). The effect of Emotional Freedom Techniques on stress biochemistry. Journal of Nervous and Mental Disease.",
        "Clond M. (2016). Emotional Freedom Techniques for anxiety: a systematic review with meta-analysis. Journal of Nervous and Mental Disease.",
        "Nelms J.A., Castel L. (2016). A systematic review and meta-analysis of randomized and nonrandomized trials of clinical EFT for depression. Explore.",
        "Sebastian B., Nelms J. (2017). The effectiveness of EFT in the treatment of PTSD: a meta-analysis. Explore.",
        "Pennebaker J.W., Beall S.K. (1986). Confronting a traumatic event: toward an understanding of inhibition and disease. Journal of Abnormal Psychology.",
        "Emmons R.A., McCullough M.E. (2003). Counting blessings versus burdens. Journal of Personality and Social Psychology.",
        "Garcia-Argibay M., Santed M.A., Reales J.M. (2019). Efficacy of binaural auditory beats in cognition, anxiety, and pain perception: a meta-analysis. Psychological Research.",
        "Alvarsson J.J., Wiens S., Nilsson M.E. (2010). Stress recovery during exposure to nature sound and environmental noise. International Journal of Environmental Research and Public Health.",
        "Goldsby T.L. et al. (2017). Effects of singing bowl sound meditation on mood, tension, and well-being. Journal of Evidence-Based Complementary & Alternative Medicine.",
        "Akimoto K. et al. (2018). Effect of 528 Hz music on the endocrine system and autonomic nervous system. Health.",
        "Calamassi D., Pomponi G.P. (2019). Music tuned to 440 Hz versus 432 Hz and the health effects: a double-blind cross-over pilot study. Explore.",
        "Balban M.Y. et al. (2023). Brief structured respiration practices enhance mood and reduce physiological arousal. Cell Reports Medicine.",
    )
    AppScaffold(title = tr("ui.about.title")) { padding ->
        PageColumn(padding) {
            Column(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppLogo(72.dp)
                Spacer(Modifier.height(18.dp))
                Text(tr("ui.app_name"), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(tr("ui.onboard.tagline"), style = LotusType.small, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(10.dp))
                Kicker(tr("ui.about.version", "v" to BuildConfig.VERSION_NAME), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                PillButton(
                    tr("ui.footer.developed_by", "name" to Catalog.DEVELOPER_NAME),
                    { Sharing.openUrl(ctx, Catalog.DEVELOPER_URL) },
                    icon = Icons.Outlined.FavoriteBorder,
                )
            }

            // The signature glass lotus, with the short hero line over its sky.
            HeroImageCard(title = tr("ui.hero.about.sub"), image = Catalog.SIGNATURE_BG)

            GlassCard {
                CardHeading(Icons.Outlined.Lock, tr("ui.onboard.private_title"))
                Spacer(Modifier.height(14.dp))
                val paras = paragraphs(tr("legal.privacy"))
                paras.forEachIndexed { i, p ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Text(p, style = if (i == 0 && paras.size > 1) LotusType.itemTitle else LotusType.small)
                }
            }

            MantraMalaCard()

            GlassCard {
                CardHeading(Icons.AutoMirrored.Outlined.MenuBook, tr("ui.about.research"))
                Spacer(Modifier.height(14.dp))
                refs.forEachIndexed { i, ref ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    Reference(ref)
                }
            }

            GlassCard {
                CardHeading(Icons.Outlined.Balance, tr("ui.about.licences"))
                Spacer(Modifier.height(12.dp))
                Text(tr("ui.about.licences_text"), style = LotusType.small)
            }

            QuietNote(tr("legal.disclaimer"))
        }
    }
}

/** Card heading: soft icon badge + serif title. */
@Composable
private fun CardHeading(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Text(title, style = LotusType.cardTitle, modifier = Modifier.weight(1f))
    }
}

/** One study: a small violet dot, authors and year in ink, the rest muted. */
@Composable
private fun Reference(ref: String) {
    val cut = ref.indexOf("). ")
    val text = buildAnnotatedString {
        if (cut > 0) {
            withStyle(SpanStyle(color = Lotus.Ink)) { append(ref.substring(0, cut + 1)) }
            append(". ")
            append(ref.substring(cut + 3))
        } else {
            append(ref)
        }
    }
    Row {
        Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Lotus.AccentSoft))
        Spacer(Modifier.width(10.dp))
        Text(text, style = LotusType.itemSub)
    }
}
