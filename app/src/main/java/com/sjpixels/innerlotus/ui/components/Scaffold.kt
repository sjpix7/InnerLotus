@file:OptIn(ExperimentalMaterial3Api::class)

package com.sjpixels.innerlotus.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.audio.SoundEngine
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.theme.Lotus
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.sjpixels.innerlotus.ui.theme.LotusType
import androidx.compose.ui.unit.em
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.graphics.graphicsLayer
import com.sjpixels.innerlotus.ui.glass.BackdropImage
import com.sjpixels.innerlotus.ui.glass.Backdrop
import kotlinx.coroutines.delay

/**
 * Standard page frame, after the reference design:
 *  - a quiet "Developed with Love by Sj Pixels" line under the status bar,
 *  - a top bar with a round glass back button, a centred serif title, and round glass buttons for
 *    language (short code) and settings. Home shows the logo + wordmark instead of back + title.
 */
@Composable
fun AppScaffold(
    title: String,
    showBack: Boolean = true,
    /** Home: logo + wordmark on the left instead of a back button and centred title. */
    showLogo: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Footer()
                // Measure both side clusters so the centred title never runs under them.
                val density = LocalDensity.current
                var leftW by remember { mutableIntStateOf(0) }
                var rightW by remember { mutableIntStateOf(0) }
                val inset = with(density) { maxOf(leftW, rightW).toDp() } + 10.dp
                Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp)) {
                    Row(Modifier.align(Alignment.CenterStart).onSizeChanged { leftW = it.width }, verticalAlignment = Alignment.CenterVertically) {
                        if (showLogo) {
                            AppLogo(38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(title, style = LotusType.barTitle.copy(fontSize = 25.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                        } else if (showBack && App.nav.canGoBack) {
                            CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, tr("ui.common.back"), { App.nav.back() })
                        }
                    }
                    if (!showLogo) {
                        Text(
                            title,
                            style = LotusType.barTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = inset),
                        )
                    }
                    Row(Modifier.align(Alignment.CenterEnd).onSizeChanged { rightW = it.width }, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        actions()
                        LanguageMenu()
                        if (App.nav.current != Screen.Settings) {
                            CircleIconButton(Icons.Outlined.Settings, tr("ui.settings.title"), { App.nav.go(Screen.Settings) })
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (SoundEngine.isPlaying && App.nav.current != Screen.Sound) MiniPlayer()
            else Spacer(Modifier.fillMaxWidth().navigationBarsPadding())
        },
    ) { padding -> content(padding) }
}

/** Round glass button showing the language code; opens the nine languages. */
@Composable
fun LanguageMenu() {
    var open by remember { mutableStateOf(false) }
    Box {
        GlassSurface(Modifier.size(42.dp), shape = CircleShape, material = Glass.Pill, onClick = { open = true }, contentAlignment = Alignment.Center) {
            Text(
                I18n.lang.code.uppercase(),
                style = LotusType.kicker.copy(color = Lotus.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em),
                modifier = Modifier.semantics { contentDescription = tr("ui.common.language") },
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Lotus.Sheet,
        ) {
            Lang.entries.forEach { l ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(l.flag, fontSize = 16.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(l.nativeName, style = LotusType.itemTitle)
                                if (l.nativeName != l.englishName) Text(l.englishName, style = LotusType.itemSub)
                            }
                        }
                    },
                    trailingIcon = {
                        if (l == I18n.lang) Icon(Icons.Outlined.Check, contentDescription = null, tint = Lotus.Accent)
                    },
                    onClick = {
                        open = false
                        App.setLanguage(l)
                    },
                )
            }
        }
    }
}

/**
 * "Developed with Love by Sj Pixels" — a quiet line at the very top; only the name is a link.
 * [modifier] supplies any system-bar inset padding.
 */
@Composable
fun Footer(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    // Translations place {name} where their grammar needs it (e.g. Hindi/Japanese put it first).
    val parts = tr("ui.footer.developed_by").split("{name}", limit = 2)
    val text = buildAnnotatedString {
        append(parts[0])
        withLink(
            LinkAnnotation.Clickable(
                tag = "developer",
                styles = TextLinkStyles(SpanStyle(color = Lotus.Accent, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)),
            ) { Sharing.openUrl(ctx, Catalog.DEVELOPER_URL) }
        ) { append(Catalog.DEVELOPER_NAME) }
        append(parts.getOrElse(1) { "" })
    }
    Box(modifier.fillMaxWidth().padding(top = 2.dp, bottom = 0.dp), contentAlignment = Alignment.Center) {
        Text(text, style = LotusType.micro.copy(fontSize = 10.sp, letterSpacing = 0.04.em), textAlign = TextAlign.Center)
    }
}

/** Floating ink-glass pill shown on other pages while Sound Healing keeps playing. */
@Composable
fun MiniPlayer() {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
        GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), material = Glass.Ink, onClick = { App.nav.go(Screen.Sound) }) {
            Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    SoundEngine.currentMixName.ifEmpty { tr("ui.sound.playing_n", "n" to SoundEngine.layers.size) },
                    style = LotusType.itemTitle.copy(color = Color.White), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { SoundEngine.stopAll() }) {
                    Icon(Icons.Outlined.Close, contentDescription = tr("ui.common.stop"), tint = Color.White)
                }
            }
        }
    }
}

/** App background: one fixed image, or a gentle crossfade to the next image every N seconds. */
@Composable
fun AppBackground() {
    val s = App.settings
    val ids = Catalog.backgrounds
    var index by remember { mutableIntStateOf(ids.indexOf(s.bgFixed).coerceAtLeast(0)) }
    val rotate = s.bgMode == "rotate"
    LaunchedEffect(rotate, s.rotateSeconds) {
        if (rotate) {
            while (true) {
                delay(s.rotateSeconds.coerceAtLeast(5) * 1000L)
                index = (index + 1) % ids.size
            }
        }
    }
    // The welcome screen always shows the signature glass-lotus image.
    val current = when {
        !s.onboarded -> Catalog.SIGNATURE_BG
        rotate -> ids[index]
        else -> s.bgFixed
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFF6F1FA))) {
        BackdropImage(current) { id, alpha ->
            Image(
                painter = painterResource(Sharing.bgRes(id)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha() },
            )
        }
        Box(Modifier.fillMaxSize().background(Backdrop.veil))
    }
}

/** Home button for deep screens (round glass). */
@Composable
fun HomeAction() {
    CircleIconButton(Icons.Outlined.Home, tr("ui.home.title"), { App.nav.home() })
}
