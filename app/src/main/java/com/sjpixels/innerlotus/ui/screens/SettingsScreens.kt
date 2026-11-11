package com.sjpixels.innerlotus.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            // Language
            SectionTitle("🌐 " + tr("ui.settings.language"))
            GlassCard {
                Lang.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                        row.forEach { l ->
                            Pill("${l.flag} ${l.nativeName}", l == I18n.lang, { App.setLanguage(l) }, Modifier.weight(1f))
                        }
                    }
                }
                if (I18n.lang.gendered) {
                    Spacer(Modifier.height(10.dp))
                    GenderChooser()
                }
            }

            // Voice
            SectionTitle("🔊 " + tr("ui.settings.voice"))
            GlassCard {
                val available = Speaker.isAvailable(I18n.lang)
                Text(
                    if (available) tr("ui.settings.voice_ok", "lang" to I18n.lang.nativeName) else tr("ui.settings.voice_missing", "lang" to I18n.lang.nativeName),
                    color = if (available) Lotus.Teal else Lotus.Danger, fontSize = 13.sp,
                )
                Text(tr("ui.settings.rate") + "  ${"%.1f".format(s.ttsRate)}×")
                Slider(value = s.ttsRate, onValueChange = { s.ttsRate = it }, valueRange = 0.5f..1.5f)
                Text(tr("ui.settings.pitch") + "  ${"%.1f".format(s.ttsPitch)}")
                Slider(value = s.ttsPitch, onValueChange = { s.ttsPitch = it }, valueRange = 0.7f..1.4f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("ui.settings.read_core"), modifier = Modifier.weight(1f))
                    Switch(checked = s.readCore, onCheckedChange = { s.readCore = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { scope.launch { Speaker.speak(tr("ui.settings.voice_sample"), "test") } }, modifier = Modifier.weight(1f)) { Text(tr("ui.settings.test_voice")) }
                    OutlinedButton(onClick = { Speaker.openInstallVoices(ctx) }, modifier = Modifier.weight(1f)) { Text(tr("ui.tts.install")) }
                }
                TextButton(onClick = { Speaker.openTtsSettings(ctx) }) { Text(tr("ui.settings.tts_system")) }
            }

            // Backgrounds
            SectionTitle("🖼️ " + tr("ui.settings.background"))
            GlassCard {
                Segmented(
                    options = listOf("rotate" to tr("ui.settings.bg_rotate"), "fixed" to tr("ui.settings.bg_fixed")),
                    selected = s.bgMode, onSelect = { s.bgMode = it },
                )
                if (s.bgMode == "rotate") Stepper(s.rotateSeconds, 5..120, { s.rotateSeconds = it }, tr("ui.settings.rotate_every"))
                Text(if (s.bgMode == "fixed") tr("ui.settings.bg_pick") else tr("ui.settings.bg_start"), fontSize = 13.sp, color = Lotus.InkSoft)
                Spacer(Modifier.height(6.dp))
                ThumbGrid(selected = s.bgFixed) { s.bgFixed = it }
            }

            // Card style
            SectionTitle("🃏 " + tr("ui.settings.cards"))
            GlassCard {
                Text(tr("ui.settings.cards_hint"), fontSize = 13.sp, color = Lotus.InkSoft)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("area", "chakra", "app").forEach { st -> Pill(tr("ui.settings.card_$st"), s.cardStyle == st, { s.cardStyle = st }, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("ui.settings.card_custom"), fontSize = 13.sp, color = Lotus.InkSoft)
                ThumbGrid(selected = s.cardStyle) { s.cardStyle = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("ui.settings.shuffle"), modifier = Modifier.weight(1f))
                    Switch(checked = s.shuffle, onCheckedChange = { s.shuffle = it })
                }
            }

            // Reminders
            SectionTitle("⏰ " + tr("ui.settings.reminders"))
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("ui.settings.remind_affirm"))
                        TextButton(onClick = { pickTime(s.remindAffirmAt) { s.remindAffirmAt = it; Reminders.apply(ctx) } }) { Text("🕗 " + fmt(s.remindAffirmAt)) }
                    }
                    Switch(checked = s.remindAffirm, onCheckedChange = { toggleReminder("affirm", it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("ui.settings.remind_journal"))
                        TextButton(onClick = { pickTime(s.remindJournalAt) { s.remindJournalAt = it; Reminders.apply(ctx) } }) { Text("🕘 " + fmt(s.remindJournalAt)) }
                    }
                    Switch(checked = s.remindJournal, onCheckedChange = { toggleReminder("journal", it) })
                }
            }

            // Privacy & sessions
            SectionTitle("🔒 " + tr("ui.settings.privacy"))
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("ui.settings.journal_pin"))
                        Text(if (PinLock.enabled) tr("ui.settings.pin_on") else tr("ui.settings.pin_off"), fontSize = 12.sp, color = Lotus.InkSoft)
                    }
                    FilledTonalButton(onClick = { pinDialog = true }) { Text(if (PinLock.enabled) tr("ui.common.change") else tr("ui.settings.set_pin")) }
                }
                if (PinLock.enabled) TextButton(onClick = { PinLock.clear(); Sharing.toast(ctx, tr("ui.settings.pin_removed")) }) { Text(tr("ui.settings.remove_pin")) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("ui.settings.keep_screen"), modifier = Modifier.weight(1f))
                    Switch(checked = s.keepScreenOn, onCheckedChange = { s.keepScreenOn = it })
                }
                Spacer(Modifier.height(6.dp))
                Text(tr("legal.privacy"), fontSize = 13.sp, color = Lotus.InkSoft)
            }

            // Data
            SectionTitle("💾 " + tr("ui.settings.data"))
            GlassCard {
                Text(tr("ui.settings.data_hint"), fontSize = 13.sp, color = Lotus.InkSoft)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { backupExporter("inner_lotus_backup.json") { Backup.export(App.affirmations, App.journal, App.habits, App.mixes) } }, modifier = Modifier.weight(1f)) { Text(tr("ui.data.export_all")) }
                    OutlinedButton(onClick = backupImporter, modifier = Modifier.weight(1f)) { Text(tr("ui.data.import")) }
                }
                TextButton(onClick = { wipe = true }) { Text(tr("ui.settings.delete_everything"), color = Lotus.Danger) }
            }

            GlassCard(onClick = { App.nav.go(Screen.About) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌸  " + tr("ui.about.title"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Filled.ChevronRight, null)
                }
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

@Composable
private fun ThumbGrid(selected: String, onSelect: (String) -> Unit) {
    Catalog.backgrounds.chunked(6).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
            row.forEach { id ->
                val img = rememberBg(id, 8)
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.56f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(if (id == selected) 3.dp else 0.dp, Lotus.Plum, RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { onSelect(id) },
                ) {
                    if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("ui.settings.set_pin")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("ui.pin.hint"), fontSize = 13.sp, color = Lotus.InkSoft)
                OutlinedTextField(a, { if (it.length <= 8 && it.all(Char::isDigit)) a = it }, label = { Text(tr("ui.pin.new")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                OutlinedTextField(b, { if (it.length <= 8 && it.all(Char::isDigit)) b = it }, label = { Text(tr("ui.pin.repeat")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = { PinLock.set(a); Sharing.toast(ctx, tr("ui.settings.pin_set_done")); onDismiss() }) { Text(tr("ui.common.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("ui.common.cancel")) } },
    )
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
            GlassCard(strong = true) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ChakraSymbol(6, Catalog.chakras[6].color, 90.dp)
                    Text(tr("ui.app_name"), style = MaterialTheme.typography.headlineMedium, color = Lotus.PlumDeep)
                    Text(tr("ui.onboard.tagline"), textAlign = TextAlign.Center, color = Lotus.InkSoft)
                    Text(tr("ui.about.version", "v" to BuildConfig.VERSION_NAME), fontSize = 12.sp, color = Lotus.InkSoft)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { Sharing.openUrl(ctx, Catalog.DEVELOPER_URL) }) { Text(tr("ui.footer.developed_by") + " sjpixels.com") }
                }
            }
            GlassCard {
                Text("🔒 " + tr("ui.onboard.private_title"), style = MaterialTheme.typography.titleMedium)
                Text(tr("legal.privacy"), fontSize = 13.sp, color = Lotus.InkSoft)
                Spacer(Modifier.height(8.dp))
                Text(tr("legal.disclaimer"), fontSize = 13.sp, color = Lotus.InkSoft)
            }
            MantraMalaCard()
            GlassCard {
                Text("📚 " + tr("ui.about.research"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                refs.forEach { Text("• $it", fontSize = 12.sp, color = Lotus.InkSoft, modifier = Modifier.padding(vertical = 2.dp)) }
            }
            GlassCard {
                Text("⚖️ " + tr("ui.about.licences"), style = MaterialTheme.typography.titleMedium)
                Text(tr("ui.about.licences_text"), fontSize = 12.sp, color = Lotus.InkSoft)
            }
        }
    }
}
