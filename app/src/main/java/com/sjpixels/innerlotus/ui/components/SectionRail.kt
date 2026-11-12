package com.sjpixels.innerlotus.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.ui.theme.Lotus

/** One app section: home-grid tile and side-rail shortcut share this list. */
data class Section(val emoji: String, val key: String, val screen: Screen) {
    /** Thin outlined icon for this section (the [emoji] field is legacy and no longer shown). */
    val icon: androidx.compose.ui.graphics.vector.ImageVector get() = com.sjpixels.innerlotus.ui.icons.LotusIcons.section(key)

    val label: String
        @Composable get() = when (key) {
            "home" -> tr("ui.home.title")
            "calm" -> tr("ui.calm.button")
            "settings" -> tr("ui.settings.title")
            else -> tr("ui.section.$key")
        }
}

object Sections {
    /** The feature sections shown as tiles on Home. */
    val features = listOf(
        Section("✨", "affirmations", Screen.Areas()),
        Section("👆", "eft", Screen.Eft),
        Section("🎶", "sound", Screen.Sound),
        Section("🌺", "hoopono", Screen.Hoopono),
        Section("📔", "journal", Screen.Journal),
        Section("✅", "habits", Screen.Habits),
        Section("🌬️", "breath", Screen.Breath),
        Section("🧘", "chakras", Screen.ChakraGuide),
    )
    val home = Section("🏠", "home", Screen.Home)
    val calm = Section("🆘", "calm", Screen.CalmNow)
    val settings = Section("⚙️", "settings", Screen.Settings)

    /** Which section a (possibly nested) screen belongs to, for highlighting. */
    fun of(s: Screen): String = when (s) {
        Screen.Home -> "home"
        is Screen.Areas, is Screen.ChakraPick, is Screen.Deck, is Screen.BulkEdit, Screen.AffirmData -> "affirmations"
        Screen.Eft, is Screen.EftSession -> "eft"
        Screen.Sound -> "sound"
        Screen.Hoopono -> "hoopono"
        Screen.Journal, is Screen.JournalEdit -> "journal"
        Screen.Habits, Screen.HabitStats, Screen.AppUsage -> "habits"
        Screen.Breath, is Screen.BreathSession -> "breath"
        Screen.ChakraGuide -> "chakras"
        Screen.CalmNow -> "calm"
        Screen.Settings, Screen.About -> "settings"
        is Screen.Learn -> ""
    }

    /** Jump straight to a section: its page sits directly on top of Home. */
    fun open(section: Section) {
        App.nav.home()
        if (section.screen != Screen.Home) App.nav.go(section.screen)
    }
}

/** Whether the rail is open; the root blurs the page behind it while true (frosted glass). */
object SectionRailState {
    var open by mutableStateOf(false)
}

// More opaque below Android 12, where the page behind cannot be blurred.
private val GlassBrush = Brush.verticalGradient(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.55f), Lotus.Plum.copy(alpha = 0.14f))
    else
        listOf(Color.White.copy(alpha = 0.94f), Color.White.copy(alpha = 0.88f), Color(0xFFEDE6F7))
)
private val GlassEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f))
)

/**
 * A small glass arrow tab on the left edge. Tapping it slides out a vertical glass rail of
 * section icons (with gaps above and below) for jumping straight to any part of the app.
 */
@Composable
fun BoxScope.SectionRail() {
    var open by SectionRailState::open
    BackHandler(enabled = open) { open = false }
    val current = Sections.of(App.nav.current)

    // First few opens: show every icon's label briefly so new users learn the icons.
    var hint by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        hint = false
        if (open && App.settings.railHintsShown < 3) {
            App.settings.railHintsShown += 1
            delay(320)
            hint = true
            delay(2200)
            hint = false
        }
    }

    // Collapsed: the arrow tab
    AnimatedVisibility(
        visible = !open,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(120)),
        modifier = Modifier.align(Alignment.CenterStart),
    ) {
        val desc = tr("ui.nav.sections")
        Box(
            Modifier
                .shadow(6.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp), clip = false)
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(GlassBrush)
                .border(1.dp, GlassEdge, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = desc }
                .width(22.dp)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Lotus.Ink, modifier = Modifier.size(20.dp))
        }
    }

    // Scrim: tap outside to close
    AnimatedVisibility(visible = open, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Lotus.Ink.copy(alpha = 0.16f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false }
        )
    }

    // Expanded: the vertical glass rail
    AnimatedVisibility(
        visible = open,
        enter = slideInHorizontally(tween(260)) { -it } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(180)),
        modifier = Modifier
            .align(Alignment.CenterStart)
            .systemBarsPadding()
            .padding(top = 72.dp, bottom = 72.dp),
    ) {
        // Square on the screen side, rounded on the open side: the panel grows out of the left edge.
        val shape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
        Column(
            Modifier
                .offset(x = (-2).dp) // hide the glass border on the screen-edge side
                .shadow(14.dp, shape, clip = false, ambientColor = Lotus.Plum, spotColor = Lotus.Plum)
                .clip(shape)
                .background(GlassBrush)
                .border(1.dp, GlassEdge, shape)
                .width(68.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { open = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = tr("ui.common.close"), tint = Lotus.Ink)
            }
            RailItem(Sections.home, current, hint) { open = false }
            RailDivider()
            Sections.features.forEach { RailItem(it, current, hint) { open = false } }
            RailDivider()
            RailItem(Sections.calm, current, hint) { open = false }
            RailItem(Sections.settings, current, hint) { open = false }
        }
    }
}

@Composable
private fun RailDivider() {
    HorizontalDivider(Modifier.width(30.dp).padding(vertical = 2.dp), color = Lotus.Plum.copy(alpha = 0.15f))
}

@Composable
private fun RailItem(section: Section, current: String, showHint: Boolean, onDone: () -> Unit) {
    val label = section.label
    val selected = section.key == current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()   // mouse / stylus hover
    val pressed by interaction.collectIsPressedAsState()   // finger down: touch "hover"
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (selected) Lotus.Ink else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button) {
                onDone()
                if (!selected || App.nav.current != section.screen) Sections.open(section)
            }
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(section.icon, contentDescription = null, tint = if (selected) Color.White else Lotus.Ink.copy(alpha = 0.82f), modifier = Modifier.size(22.dp))
        RailLabel(label, visible = hovered || pressed || showHint)
    }
}

/** Plum label pill shown to the right of a rail icon (outside the rail, so it isn't clipped). */
@Composable
private fun RailLabel(text: String, visible: Boolean) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(if (visible) 120 else 200), label = "railLabel")
    if (alpha == 0f) return
    val gap = with(LocalDensity.current) { 64.dp.roundToPx() }
    Popup(
        alignment = Alignment.CenterStart,
        offset = IntOffset(gap, 0),
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .graphicsLayer { this.alpha = alpha; translationX = (1f - alpha) * -12.dp.toPx() }
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Lotus.Ink.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
