package com.sjpixels.innerlotus.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.content.Affirm
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Cormorant
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import com.sjpixels.innerlotus.ui.theme.scriptFamily
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date

// =================================================================================================
//  Home
// =================================================================================================

/** Home: today, the affirmation of the day, a calm shortcut and every practice. */
@Composable
fun HomeScreen() {
    AppScaffold(title = tr("ui.app_name"), showBack = false, showLogo = true) { padding ->
        PageColumn(padding) {
            TodayRow()
            ScreenHeader(title = tr("ui.hero.home.title"), subtitle = tr("ui.hero.home.sub"))
            DailyAffirmationCard()
            CalmStrip(tr("ui.home.calm_title"), tr("ui.home.calm_sub"), onClick = { App.nav.go(Screen.CalmNow) })

            SectionHeading(tr("ui.home.practices"))
            TileGrid(Sections.features) { section, index, tileModifier ->
                PracticeTile(
                    icon = section.icon,
                    title = tr("ui.section.${section.key}"),
                    subtitle = tr("ui.section.${section.key}_sub"),
                    tone = toneFor(index),
                    modifier = tileModifier,
                    onClick = { App.nav.go(section.screen) },
                )
            }

            MantraMalaCard()
            // Footnotes sit on strong glass: the end of the page lands on the darker water of the art.
            GlassCard(strong = true, padding = 14.dp) {
                QuietNote(tr("ui.home.private_badge"), icon = Icons.Outlined.Lock)
                RowDivider()
                QuietNote(tr("legal.disclaimer"), Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** "THURSDAY, NOVEMBER 12" on the left; the time-of-day greeting as a quiet glass pill on the right. */
@Composable
private fun TodayRow() {
    val locale = I18n.lang.locale
    val date = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale).format(Date())
    val (greeting, icon) = when (LocalTime.now().hour) {
        in 4..11 -> tr("ui.home.greet_morning") to Icons.Outlined.WbTwilight
        in 12..16 -> tr("ui.home.greet_afternoon") to Icons.Outlined.WbSunny
        else -> tr("ui.home.greet_evening") to Icons.Outlined.Bedtime
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Kicker(date, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        GlassSurface(shape = PillShape, material = Glass.Pill) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Lotus.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(greeting, style = LotusType.pill.copy(color = Lotus.Ink), maxLines = 1)
            }
        }
    }
}

/**
 * The affirmation of the day on the glass-lotus art: core statement, main line (serif title) and
 * seed line, with Listen / Copy / Share along the bottom.
 */
@Composable
private fun DailyAffirmationCard() {
    val ctx = LocalContext.current
    val card = Affirm.daily()
    val chakra = Catalog.chakra(card.chakra)
    val core = card.core?.takeIf { it.isNotBlank() && chakra != null }
    val sub = card.sub?.takeIf { it.isNotBlank() }
    val kicker = tr("ui.home.daily")
    val locale = I18n.lang.locale

    val titleStyle = MaterialTheme.typography.headlineMedium
    val subStyle = affirmationLineStyle()
    val kickerStyle = LotusType.kicker
    val pillStyle = LotusType.pill
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // HeroImageCard has a fixed height: size it to today's words (any language, any font scale)
        // and keep a band of the lotus art visible between the words and the buttons.
        val fit = Constraints(maxWidth = with(density) { (maxWidth - 40.dp).roundToPx() }.coerceAtLeast(1))
        fun textHeight(text: String, style: TextStyle, maxLines: Int = Int.MAX_VALUE): Dp =
            with(density) { measurer.measure(text, style, maxLines = maxLines, constraints = fit).size.height.toDp() }

        var needed = 20.dp + textHeight(kicker.uppercase(locale), kickerStyle) + 8.dp + textHeight(card.main, titleStyle, maxLines = 4)
        if (sub != null) needed += 8.dp + textHeight(sub, subStyle)
        if (core != null) needed += 12.dp + maxOf(22.dp, textHeight(core.uppercase(locale), kickerStyle))
        needed += 64.dp + maxOf(42.dp, textHeight(tr("ui.common.listen"), pillStyle) + 18.dp) + 20.dp

        HeroImageCard(
            title = card.main,
            kicker = kicker,
            height = maxOf(needed, 280.dp),
            extra = {
                if (sub != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(sub, style = subStyle)
                }
                if (core != null && chakra != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChakraDot(chakra.index, size = 22.dp)
                        Spacer(Modifier.width(8.dp))
                        Kicker(core, color = Lotus.Ink.copy(alpha = 0.72f))
                    }
                }
            },
            bottom = {
                ListenButton("daily", { Affirm.speakText(card, App.settings.readCore) })
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Outlined.ContentCopy, tr("ui.common.copy"), { Sharing.copy(ctx, Affirm.plainText(card)) })
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Outlined.Share, tr("ui.common.share"), { Sharing.shareText(ctx, Affirm.plainText(card) + "\n— Inner Lotus") })
            },
        )
    }
}

/** Second line of the affirmation: Cormorant italic where the bundled face covers the script. */
@Composable
private fun affirmationLineStyle(): TextStyle {
    val family = scriptFamily()
    return LotusType.cardTitle.copy(
        fontFamily = family,
        fontStyle = if (family == Cormorant) FontStyle.Italic else FontStyle.Normal,
        fontWeight = FontWeight.Medium,
        color = Lotus.Ink.copy(alpha = 0.8f),
    )
}

/** Invitation to the companion Mantra Mala app (also shown in Settings and About). */
@Composable
fun MantraMalaCard() {
    val ctx = LocalContext.current
    val open = { Sharing.openMantraMala(ctx) }
    GlassCard(strong = true, onClick = open) {
        Text("Mantra Mala", style = LotusType.cardTitle)
        Spacer(Modifier.height(6.dp))
        Text(tr("mantramala.text"), style = LotusType.small)
        Spacer(Modifier.height(4.dp))
        // TextLink pads its touch area; pull it back so the words line up with the text above.
        TextLink(tr("ui.common.read_more"), onClick = open, modifier = Modifier.offset(x = (-6).dp))
    }
}

// =================================================================================================
//  Welcome
// =================================================================================================

/** First-launch welcome over the glass-lotus art: language, form of address, privacy promise. */
@Composable
fun OnboardingScreen() {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Footer(Modifier.statusBarsPadding())
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(44.dp)
                Spacer(Modifier.width(12.dp))
                Text(tr("ui.app_name"), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            }
            ScreenHeader(
                title = tr("ui.hero.onboard.title"),
                modifier = Modifier.padding(top = 8.dp),
                kicker = tr("ui.hero.onboard.kicker"),
                subtitle = tr("ui.hero.onboard.sub"),
                center = true,
                large = true,
            )
            WelcomeCard()
            PrimaryButton(
                tr("ui.onboard.begin_journey"),
                onClick = {
                    App.settings.lang = I18n.lang.code
                    App.settings.onboarded = true
                },
            )
            // On tall screens the lotus shows here; on small ones the page simply scrolls.
            Spacer(Modifier.weight(1f))
            OnboardingFinePrint(expanded = showPrivacy, onToggle = { showPrivacy = !showPrivacy })
        }
    }
}

/** "Make yourself at home": the nine languages as chips, then the form of address if needed. */
@Composable
private fun WelcomeCard() {
    GlassCard(strong = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("ui.onboard.home_title"), style = LotusType.cardTitle, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(tr("ui.common.language"), style = LotusType.small)
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Lang.entries.forEach { l ->
                val on = l == I18n.lang
                Pill(l.nativeName, on, { App.setLanguage(l) }, Modifier.semantics { selected = on })
            }
        }
        AnimatedVisibility(visible = I18n.lang.gendered) {
            Column {
                Spacer(Modifier.height(16.dp))
                RowDivider()
                Spacer(Modifier.height(14.dp))
                GenderChooser()
            }
        }
    }
}

/**
 * Frosted footnote under the art: the privacy promise (tap for the full "private by design" text)
 * and the wellness disclaimer. Strong glass keeps the small print readable over the water.
 */
@Composable
private fun OnboardingFinePrint(expanded: Boolean, onToggle: () -> Unit) {
    GlassCard(strong = true, padding = 14.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    onClickLabel = if (expanded) tr("ui.common.close") else tr("ui.common.read_more"),
                    role = Role.Button,
                    onClick = onToggle,
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(22.dp)) // balances the chevron so the note stays centred
            QuietNote(tr("ui.onboard.private_note"), Modifier.weight(1f), icon = Icons.Outlined.Lock)
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = Lotus.InkSoft,
                modifier = Modifier.size(22.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                tr("ui.onboard.private_text"),
                style = LotusType.small,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 12.dp),
            )
        }
        RowDivider()
        QuietNote(tr("legal.disclaimer"), Modifier.padding(top = 4.dp))
    }
}

/** "Speak to me as": feminine / masculine word forms, as two chips. Used on Welcome and in Settings. */
@Composable
fun GenderChooser() {
    Column(Modifier.fillMaxWidth()) {
        Text(tr("ui.settings.address"), style = LotusType.itemTitle)
        Spacer(Modifier.height(2.dp))
        Text(tr("ui.settings.address_hint"), style = LotusType.itemSub)
        Spacer(Modifier.height(12.dp))
        val feminine = I18n.feminine
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(tr("ui.settings.feminine"), feminine, { App.setFeminine(true) }, Modifier.semantics { selected = feminine })
            Pill(tr("ui.settings.masculine"), !feminine, { App.setFeminine(false) }, Modifier.semantics { selected = !feminine })
        }
    }
}

// =================================================================================================
//  Learn
// =================================================================================================

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
    // The bar names the practice this guide belongs to; the article title is the page headline.
    val sectionKey = when (topic) {
        "hooponopono" -> "hoopono"
        "chakra" -> "chakras"
        else -> topic
    }
    val section = Sections.features.firstOrNull { it.key == sectionKey }
    AppScaffold(title = section?.label ?: title) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = title, kicker = tr("ui.hero.learn.kicker"))
            ListenButton("learn:$topic", { "$title. $body" })
            GlassCard(strong = true, padding = 20.dp) {
                ArticleBody(body)
            }
            if (action != null) {
                PrimaryButton(action.first, onClick = { App.nav.go(action.second) })
            }
        }
    }
}

/** Article text: paragraphs, "•" bullets and numbered steps with hanging indents, short "Heading:" lines in serif. */
@Composable
private fun ArticleBody(body: String) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = Lotus.Ink)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        body.lines().forEach { raw ->
            val line = raw.trim()
            val step = NumberedStep.matchEntire(line)
            when {
                line.isEmpty() -> Spacer(Modifier.height(6.dp))
                line.startsWith("•") -> ArticleListItem("•", line.removePrefix("•").trim(), bodyStyle)
                step != null -> ArticleListItem(step.groupValues[1], step.groupValues[2], bodyStyle)
                isArticleHeading(line) -> Text(
                    line.trimEnd(':', '：').trimEnd(),
                    style = LotusType.section,
                    modifier = Modifier.padding(top = 6.dp),
                )
                else -> Text(line, style = bodyStyle)
            }
        }
    }
}

private val NumberedStep = Regex("""^(\d{1,2}[.)])\s*(.+)$""")

/** "How to practise:", "Tips:" … — short lines that introduce a list. */
private fun isArticleHeading(line: String): Boolean =
    line.length <= 48 && (line.endsWith(':') || line.endsWith('：'))

@Composable
private fun ArticleListItem(marker: String, text: String, style: TextStyle) {
    Row(Modifier.fillMaxWidth()) {
        Text(marker, style = style.copy(color = Lotus.Accent), modifier = Modifier.width(24.dp))
        Text(text, style = style, modifier = Modifier.weight(1f))
    }
}
