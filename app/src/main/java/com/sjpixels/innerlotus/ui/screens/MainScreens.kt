package com.sjpixels.innerlotus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.content.Affirm
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.scriptFamily
import java.time.LocalTime

private data class Tile(val emoji: String, val key: String, val screen: Screen)

private val tiles = listOf(
    Tile("✨", "affirmations", Screen.Areas()),
    Tile("👆", "eft", Screen.Eft),
    Tile("🎶", "sound", Screen.Sound),
    Tile("🌺", "hoopono", Screen.Hoopono),
    Tile("📔", "journal", Screen.Journal),
    Tile("✅", "habits", Screen.Habits),
    Tile("🌬️", "breath", Screen.Breath),
    Tile("🧘", "chakras", Screen.ChakraGuide),
)

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    AppScaffold(title = tr("ui.app_name"), showBack = false) { padding ->
        PageColumn(padding) {
            val hour = LocalTime.now().hour
            val greet = when (hour) {
                in 4..11 -> tr("ui.home.greet_morning")
                in 12..16 -> tr("ui.home.greet_afternoon")
                else -> tr("ui.home.greet_evening")
            }
            Text(greet, style = MaterialTheme.typography.headlineSmall, color = Lotus.PlumDeep)
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Lotus.Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("ui.home.private_badge"), fontSize = 12.sp, color = Lotus.InkSoft)
            }

            // Affirmation of the day
            val card = Affirm.daily()
            val chakra = Catalog.chakra(card.chakra)
            GlassCard(strong = true) {
                Text(tr("ui.home.daily"), style = MaterialTheme.typography.labelLarge, color = Lotus.InkSoft)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chakra != null) ChakraSymbol(chakra.index, chakra.color, 58.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (card.core != null && chakra != null) Text(card.core, color = Color(chakra.ink), fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                        Text(card.main, style = MaterialTheme.typography.titleMedium, color = Lotus.Ink)
                        if (card.sub != null && chakra != null) Text(card.sub, fontFamily = scriptFamily(), color = Color(chakra.ink), fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ListenButton("daily", { Affirm.speakText(card, App.settings.readCore) })
                    IconButton(onClick = { Sharing.copy(ctx, Affirm.plainText(card)) }) { Icon(Icons.Filled.ContentCopy, tr("ui.common.copy"), tint = Lotus.Plum) }
                    IconButton(onClick = { Sharing.shareText(ctx, Affirm.plainText(card) + "\n— Inner Lotus") }) { Icon(Icons.Filled.Share, tr("ui.common.share"), tint = Lotus.Plum) }
                }
            }

            // Calm now
            Button(
                onClick = { App.nav.go(Screen.CalmNow) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lotus.Rose),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("🆘  " + tr("ui.calm.button"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Sections grid
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t ->
                        GlassCard(modifier = Modifier.weight(1f), onClick = { App.nav.go(t.screen) }, padding = 14.dp) {
                            Text(t.emoji, fontSize = 30.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(tr("ui.section.${t.key}"), style = MaterialTheme.typography.titleMedium, color = Lotus.Ink, maxLines = 2)
                            Text(tr("ui.section.${t.key}_sub"), fontSize = 12.sp, color = Lotus.InkSoft, maxLines = 2, minLines = 2)
                        }
                    }
                }
            }

            MantraMalaCard()
            Text(tr("legal.disclaimer"), fontSize = 11.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }
    }
}

@Composable
fun MantraMalaCard() {
    val ctx = LocalContext.current
    GlassCard(onClick = { Sharing.openMantraMala(ctx) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📿", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mantra Mala", style = MaterialTheme.typography.titleMedium, color = Lotus.PlumDeep)
                Text(tr("mantramala.text"), fontSize = 13.sp, color = Lotus.InkSoft)
            }
            Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Lotus.Plum)
        }
    }
}

/** First-launch welcome: language, form of address, privacy promise. */
@Composable
fun OnboardingScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        ChakraSymbol(6, Catalog.chakras[6].color, 110.dp)
        Text(tr("ui.app_name"), style = MaterialTheme.typography.displaySmall, color = Lotus.PlumDeep)
        Text(tr("ui.onboard.tagline"), textAlign = TextAlign.Center, color = Lotus.InkSoft)
        GlassCard(strong = true) {
            Text(tr("ui.onboard.choose_language"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Lang.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { l ->
                        val on = l == I18n.lang
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (on) Lotus.Plum.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { App.setLanguage(l) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(l.flag, fontSize = 26.sp)
                            Text(l.nativeName, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        }
                    }
                }
            }
            if (I18n.lang.gendered) {
                Spacer(Modifier.height(10.dp))
                GenderChooser()
            }
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Lotus.Teal)
                Spacer(Modifier.width(8.dp))
                Text(tr("ui.onboard.private_title"), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(tr("ui.onboard.private_text"), color = Lotus.InkSoft)
        }
        Text(tr("legal.disclaimer"), fontSize = 12.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center)
        Button(
            onClick = {
                App.settings.lang = I18n.lang.code
                App.settings.onboarded = true
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text(tr("ui.onboard.begin"), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        Footer()
    }
}

@Composable
fun GenderChooser() {
    Text(tr("ui.settings.address"), style = MaterialTheme.typography.titleSmall)
    Text(tr("ui.settings.address_hint"), fontSize = 12.sp, color = Lotus.InkSoft)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(tr("ui.settings.feminine"), I18n.feminine, { App.setFeminine(true) })
        Pill(tr("ui.settings.masculine"), !I18n.feminine, { App.setFeminine(false) })
    }
}

/** A knowledge-base article with read-aloud. */
@Composable
fun LearnScreen(topic: String) {
    val title = tr("learn.$topic.title")
    val body = tr("learn.$topic.body")
    val action: Pair<String, Screen>? = when (topic) {
        "affirmations" -> tr("ui.learn.go_affirmations") to Screen.Areas()
        "eft" -> tr("ui.learn.go_eft") to Screen.Eft
        "hooponopono" -> tr("ui.learn.go_hoopono") to Screen.Hoopono
        "journal" -> tr("ui.learn.go_journal") to Screen.JournalEdit(null, "free")
        "sound" -> tr("ui.learn.go_sound") to Screen.Sound
        "breath" -> tr("ui.learn.go_breath") to Screen.Breath
        "chakra" -> tr("ui.learn.go_chakra") to Screen.ChakraGuide
        else -> null
    }
    AppScaffold(title = title) { padding ->
        PageColumn(padding) {
            GlassCard(strong = true) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Lotus.PlumDeep)
                Spacer(Modifier.height(10.dp))
                ListenButton("learn:$topic", { "$title. $body" })
                Spacer(Modifier.height(12.dp))
                body.split("\n").forEach { para ->
                    if (para.isBlank()) Spacer(Modifier.height(6.dp))
                    else Text(
                        para,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Lotus.Ink,
                        modifier = Modifier.padding(start = if (para.startsWith("•")) 8.dp else 0.dp, bottom = 2.dp),
                    )
                }
            }
            if (action != null) {
                Button(onClick = { App.nav.go(action.second) }, modifier = Modifier.fillMaxWidth()) { Text(action.first) }
            }
        }
    }
}
