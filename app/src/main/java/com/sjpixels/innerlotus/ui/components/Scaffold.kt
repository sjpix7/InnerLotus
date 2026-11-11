@file:OptIn(ExperimentalMaterial3Api::class)

package com.sjpixels.innerlotus.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import kotlinx.coroutines.delay

/**
 * Standard page frame: top bar (back, title, language flag menu, settings), content, and the
 * "Developed by https://sjpixels.com/" footer that appears on every page.
 */
@Composable
fun AppScaffold(
    title: String,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge, color = Lotus.Ink)
                },
                navigationIcon = {
                    if (showBack && App.nav.canGoBack) {
                        IconButton(onClick = { App.nav.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("ui.common.back"), tint = Lotus.Ink)
                        }
                    }
                },
                actions = {
                    actions()
                    LanguageMenu()
                    if (App.nav.current != Screen.Settings) {
                        IconButton(onClick = { App.nav.go(Screen.Settings) }) {
                            Icon(Icons.Filled.Settings, contentDescription = tr("ui.settings.title"), tint = Lotus.Ink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            Column {
                if (SoundEngine.isPlaying && App.nav.current != Screen.Sound) MiniPlayer()
                Footer()
            }
        },
    ) { padding -> content(padding) }
}

/** Flag button in the top-right corner with a dropdown of the nine languages. */
@Composable
fun LanguageMenu() {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
            Text(I18n.lang.flag, fontSize = 22.sp)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = tr("ui.common.language"), tint = Lotus.Ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Lang.entries.forEach { l ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(l.flag, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(l.nativeName, fontWeight = FontWeight.SemiBold)
                                if (l.nativeName != l.englishName) Text(l.englishName, fontSize = 12.sp, color = Lotus.InkSoft)
                            }
                        }
                    },
                    trailingIcon = {
                        if (l == I18n.lang) Icon(Icons.Filled.Check, contentDescription = null, tint = Lotus.Plum)
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

@Composable
fun Footer() {
    val ctx = LocalContext.current
    Surface(color = Color.White.copy(alpha = 0.6f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable { Sharing.openUrl(ctx, Catalog.DEVELOPER_URL) }
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("ui.footer.developed_by") + " ", fontSize = 12.sp, color = Lotus.InkSoft)
            Text(
                "https://sjpixels.com/",
                fontSize = 12.sp,
                color = Lotus.Plum,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

/** Shown on other pages while Sound Healing keeps playing in the background. */
@Composable
fun MiniPlayer() {
    Surface(
        color = Lotus.PlumDeep.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { App.nav.go(Screen.Sound) }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                SoundEngine.currentMixName.ifEmpty { tr("ui.sound.playing_n", "n" to SoundEngine.layers.size) },
                color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { SoundEngine.stopAll() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = tr("ui.common.stop"), tint = Color.White)
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
    val current = if (rotate) ids[index] else s.bgFixed
    Box(Modifier.fillMaxSize().background(Color(0xFFF6F1FA))) {
        Crossfade(targetState = current, animationSpec = tween(2200), label = "bg") { id ->
            Image(
                painter = painterResource(Sharing.bgRes(id)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Lotus.Veil))
    }
}

/** Home button for deep screens. */
@Composable
fun HomeAction() {
    IconButton(onClick = { App.nav.home() }) {
        Icon(Icons.Filled.Home, contentDescription = tr("ui.home.title"), tint = Lotus.Ink)
    }
}
