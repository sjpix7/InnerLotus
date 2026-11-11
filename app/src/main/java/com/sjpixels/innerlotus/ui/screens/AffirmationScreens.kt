package com.sjpixels.innerlotus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.Screen
import com.sjpixels.innerlotus.content.Affirm
import com.sjpixels.innerlotus.content.Composer
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.tr
import com.sjpixels.innerlotus.data.AffirmationImport
import com.sjpixels.innerlotus.data.Csv
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.tts.Speaker
import com.sjpixels.innerlotus.ui.components.*
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.displayFamily
import com.sjpixels.innerlotus.ui.theme.scriptFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

const val FAVORITES_AREA = "__fav"

private fun areaName(id: String) = if (id == FAVORITES_AREA) tr("ui.affirm.favorites") else tr("area.$id.name")

// ------------------------------------------------------------------------------------------------
// Area list
// ------------------------------------------------------------------------------------------------

@Composable
fun AreasScreen(chakra: String?) {
    val store by App.affirmations.state.collectAsState()
    var query by remember { mutableStateOf("") }
    AppScaffold(title = tr("ui.section.affirmations"), actions = {
        IconButton(onClick = { App.nav.go(Screen.AffirmData) }) { Icon(Icons.Filled.ImportExport, tr("ui.affirm.data_title"), tint = Lotus.Ink) }
    }) { padding ->
        PageColumn(padding) {
            LearnCard("affirmations", "✨")
            if (chakra != null) {
                val c = Catalog.chakra(chakra)
                if (c != null) GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChakraSymbol(c.index, c.color, 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("ui.affirm.chakra_selected", "chakra" to tr("chakra.$chakra.name")), modifier = Modifier.weight(1f))
                        TextButton(onClick = { App.nav.replace(Screen.Areas(null)) }) { Text(tr("ui.common.clear")) }
                    }
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(tr("ui.affirm.search")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.6f), focusedContainerColor = Color.White.copy(alpha = 0.85f)),
            )
            if (store.favorites.isNotEmpty() && query.isBlank()) {
                GlassCard(onClick = { App.nav.go(Screen.Deck(FAVORITES_AREA, null)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💖", fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("ui.affirm.favorites") + " (${store.favorites.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
            val q = query.trim().lowercase()
            val areas = Catalog.areas.filter { q.isEmpty() || tr("area.${it.id}.name").lowercase().contains(q) || tr("area.${it.id}.desc").lowercase().contains(q) }
            areas.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { a ->
                        val custom = store.custom[a.id]?.size ?: 0
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            padding = 12.dp,
                            onClick = { App.nav.go(if (chakra != null) Screen.Deck(a.id, chakra) else Screen.ChakraPick(a.id)) },
                        ) {
                            Text(a.emoji, fontSize = 28.sp)
                            Text(tr("area.${a.id}.name"), style = MaterialTheme.typography.titleSmall, color = Lotus.Ink, maxLines = 2, minLines = 2)
                            Text(tr("area.${a.id}.desc"), fontSize = 11.sp, color = Lotus.InkSoft, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "1008" + if (custom > 0) "  +  $custom ✍️" else "",
                                fontSize = 11.sp, color = Lotus.Plum, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Chakra picker (poster style: crown at the top, root at the bottom)
// ------------------------------------------------------------------------------------------------

@Composable
fun ChakraPickScreen(area: String) {
    val a = Catalog.area(area)
    AppScaffold(title = tr("area.$area.name")) { padding ->
        PageColumn(padding) {
            Text(tr("ui.affirm.pick_chakra"), color = Lotus.InkSoft)
            Button(
                onClick = { App.nav.go(Screen.Deck(area, null)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) { Text("🎲  " + tr("ui.affirm.random_all")) }
            GlassCard(strong = true, padding = 10.dp) {
                Catalog.chakras.reversed().forEach { c ->
                    val seed = Affirm.seeds(c.id).firstOrNull().orEmpty()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { App.nav.go(Screen.Deck(area, c.id)) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChakraSymbol(c.index, c.color, 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                tr("chakra.${c.id}.core"),
                                fontFamily = displayFamily(), fontWeight = FontWeight.Bold, fontSize = 26.sp,
                                color = Color(c.ink), letterSpacing = 1.5.sp, textAlign = TextAlign.Center,
                            )
                            Text(seed, fontFamily = scriptFamily(), fontSize = 19.sp, color = Color(c.ink), textAlign = TextAlign.Center)
                            Text(tr("chakra.${c.id}.name") + " · " + tr("chakra.${c.id}.sanskrit"), fontSize = 11.sp, color = Lotus.InkSoft)
                        }
                    }
                }
            }
            Text(tr("ui.affirm.poster_hint"), fontSize = 13.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { App.nav.go(Screen.ChakraGuide) }, modifier = Modifier.fillMaxWidth()) { Text("🧘 " + tr("ui.affirm.learn_chakras")) }
            Text(a.emoji + "  " + tr("area.$area.desc"), fontSize = 12.sp, color = Lotus.InkSoft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Swipe deck with text-to-speech modes
// ------------------------------------------------------------------------------------------------

@Composable
fun DeckScreen(area: String, chakra: String?) {
    val ctx = LocalContext.current
    val s = App.settings
    val store by App.affirmations.state.collectAsState()
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(if (area == FAVORITES_AREA) "fav" else "all") }
    val lang = I18n.lang

    // Build the list of cards for this area / chakra / filter.
    val cards: List<Affirm.Card> = remember(area, chakra, filter, store.custom, store.favorites, s.shuffle, store.shuffleSeed, lang, I18n.feminine) {
        val custom = store.custom[area].orEmpty().mapIndexed { i, t ->
            Affirm.custom(area, t, chakra ?: Composer.CHAKRAS[i % 7])
        }
        when {
            area == FAVORITES_AREA -> store.favorites.mapNotNull { Affirm.fromKey(it) }
            filter == "mine" -> custom
            filter == "fav" -> store.favorites.filter { it.startsWith("b|$area|") || it.startsWith("c|$area|") }.mapNotNull { Affirm.fromKey(it) }
            else -> {
                val refs = Composer.deck(chakra, Affirm.LINES, Affirm.SEEDS, if (s.shuffle || chakra == null) store.shuffleSeed + area.hashCode() else null)
                custom + refs.map { Affirm.builtIn(area, it) }
            }
        }
    }
    val posKey = "$area|${chakra ?: "all"}|$filter|${s.shuffle}"
    val start = (store.positions[posKey] ?: 0).coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val pager = rememberPagerState(initialPage = start) { cards.size }
    LaunchedEffect(pager.settledPage, posKey) { if (cards.isNotEmpty()) App.affirmations.setPosition(posKey, pager.settledPage) }

    var playing by remember { mutableStateOf(false) }
    KeepScreenOn(playing)
    LaunchedEffect(playing, s.ttsMode, s.ttsCount, cards.size) {
        if (!playing) return@LaunchedEffect
        while (isActive && playing && cards.isNotEmpty()) {
            val page = pager.currentPage.coerceIn(0, cards.size - 1)
            val text = Affirm.speakText(cards[page], s.readCore)
            val reps = if (s.ttsMode == "count") s.ttsCount.coerceIn(1, 108) else 1
            var ok = true
            for (r in 0 until reps) {
                if (!isActive) break
                ok = Speaker.speak(text, "deck")
                if (!ok) break
                delay(700)
            }
            if (!ok) { playing = false; break }
            when (s.ttsMode) {
                "manual" -> playing = false
                "repeat" -> {}
                else -> {
                    if (pager.currentPage < cards.size - 1) pager.animateScrollToPage(pager.currentPage + 1)
                    else playing = false
                    delay(400)
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }

    // Saving images on Android 8-9 needs the legacy storage permission.
    var pendingSave by remember { mutableStateOf<Affirm.Card?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val c = pendingSave
        pendingSave = null
        if (granted && c != null) scope.launch { saveCard(ctx, c) }
    }

    val title = areaName(area) + (Catalog.chakra(chakra)?.let { " · " + tr("chakra.${it.id}.name") } ?: "")
    AppScaffold(title = title, actions = {
        var menu by remember { mutableStateOf(false) }
        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Lotus.Ink) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (area != FAVORITES_AREA) {
                DropdownMenuItem(text = { Text(tr("ui.affirm.bulk_edit")) }, leadingIcon = { Icon(Icons.Filled.EditNote, null) }, onClick = { menu = false; App.nav.go(Screen.BulkEdit(area)) })
                DropdownMenuItem(text = { Text(tr("ui.affirm.change_chakra")) }, leadingIcon = { Icon(Icons.Filled.Spa, null) }, onClick = { menu = false; App.nav.replace(Screen.ChakraPick(area)) })
            }
            DropdownMenuItem(text = { Text(tr("ui.affirm.reshuffle")) }, leadingIcon = { Icon(Icons.Filled.Shuffle, null) }, onClick = { menu = false; App.affirmations.reshuffle() })
            DropdownMenuItem(text = { Text(tr("ui.affirm.data_title")) }, leadingIcon = { Icon(Icons.Filled.ImportExport, null) }, onClick = { menu = false; App.nav.go(Screen.AffirmData) })
            DropdownMenuItem(text = { Text(tr("learn.affirmations.title")) }, leadingIcon = { Icon(Icons.Filled.School, null) }, onClick = { menu = false; App.nav.go(Screen.Learn("affirmations")) })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (area != FAVORITES_AREA) {
                Segmented(
                    options = listOf(
                        "all" to tr("ui.affirm.filter_all", "n" to (if (chakra == null) 1008 else 144)),
                        "mine" to tr("ui.affirm.filter_mine", "n" to (store.custom[area]?.size ?: 0)),
                        "fav" to tr("ui.affirm.filter_fav"),
                    ),
                    selected = filter,
                    onSelect = { playing = false; filter = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (cards.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    GlassCard(strong = true) {
                        Text(if (filter == "mine") tr("ui.affirm.empty_mine") else tr("ui.affirm.empty_fav"), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (filter == "mine") {
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { App.nav.go(Screen.BulkEdit(area)) }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.affirm.bulk_edit")) }
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    pageSpacing = 14.dp,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { it },
                ) { page ->
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                    AffirmationCard(
                        card = cards[page],
                        index = page,
                        total = cards.size,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                            .graphicsLayer {
                                val sc = 1f - 0.08f * off.coerceIn(0f, 1f)
                                scaleX = sc; scaleY = sc
                                alpha = 1f - 0.35f * off.coerceIn(0f, 1f)
                                rotationZ = (page - pager.currentPage - pager.currentPageOffsetFraction) * -3f
                            },
                    )
                }
                val current = cards.getOrNull(pager.currentPage)
                DeckControls(
                    playing = playing,
                    onPlay = { playing = !playing; if (!playing) Speaker.stop() },
                    onPrev = { scope.launch { if (pager.currentPage > 0) pager.animateScrollToPage(pager.currentPage - 1) } },
                    onNext = { scope.launch { if (pager.currentPage < cards.size - 1) pager.animateScrollToPage(pager.currentPage + 1) } },
                )
                if (current != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val fav = current.favKey in store.favorites
                        IconButton(onClick = { App.affirmations.toggleFavorite(current.favKey) }) {
                            Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, tr("ui.affirm.favorite"), tint = if (fav) Lotus.Rose else Lotus.Ink)
                        }
                        IconButton(onClick = { Sharing.copy(ctx, Affirm.plainText(current)) }) { Icon(Icons.Filled.ContentCopy, tr("ui.common.copy"), tint = Lotus.Ink) }
                        IconButton(onClick = {
                            scope.launch {
                                val bmp = withContext(Dispatchers.Default) { Sharing.renderCard(ctx, cardSpec(current)) }
                                Sharing.shareImage(ctx, bmp, Affirm.plainText(current))
                            }
                        }) { Icon(Icons.Filled.Share, tr("ui.common.share"), tint = Lotus.Ink) }
                        IconButton(onClick = {
                            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) { pendingSave = current; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                            else scope.launch { saveCard(ctx, current) }
                        }) { Icon(Icons.Filled.Download, tr("ui.affirm.download"), tint = Lotus.Ink) }
                        if (area != FAVORITES_AREA) {
                            IconButton(onClick = { App.nav.go(Screen.BulkEdit(area)) }) { Icon(Icons.Filled.EditNote, tr("ui.affirm.bulk_edit"), tint = Lotus.Ink) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private suspend fun saveCard(ctx: android.content.Context, c: Affirm.Card) {
    val ok = withContext(Dispatchers.Default) {
        val bmp = Sharing.renderCard(ctx, cardSpec(c))
        Sharing.saveToGallery(ctx, bmp, "InnerLotus_" + System.currentTimeMillis())
    }
    Sharing.toast(ctx, if (ok) tr("ui.affirm.saved") else tr("ui.common.error"))
}

private fun cardBgId(c: Affirm.Card): String {
    val style = App.settings.cardStyle
    return when (style) {
        "area" -> if (c.area == FAVORITES_AREA) "lotus" else Catalog.area(c.area).bg
        "chakra" -> Catalog.chakra(c.chakra)?.bg ?: Catalog.area(c.area).bg
        "app" -> App.settings.bgFixed
        else -> style
    }
}

private fun cardSpec(c: Affirm.Card): Sharing.CardSpec {
    val ch = Catalog.chakra(c.chakra)
    return Sharing.CardSpec(
        core = c.core,
        main = c.main,
        sub = c.sub,
        label = areaName(c.area),
        chakraIndex = ch?.index,
        chakraColor = Color(ch?.color ?: 0xFF8E24AA).toArgb(),
        ink = Color(ch?.ink ?: 0xFF5B3F8F).toArgb(),
        bgRes = Sharing.bgRes(cardBgId(c)),
        scriptFont = I18n.lang.scriptFont,
    )
}

@Composable
private fun AffirmationCard(card: Affirm.Card, index: Int, total: Int, modifier: Modifier) {
    val ch = Catalog.chakra(card.chakra)
    val ink = Color(ch?.ink ?: 0xFF5B3F8F)
    val style = App.settings.cardStyle
    Box(modifier.clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = 0.3f))) {
        if (style != "app") BgImage(cardBgId(card), 2, Modifier.matchParentSize())
        if (style == "chakra" && ch != null) Box(Modifier.matchParentSize().background(Color(ch.color).copy(alpha = 0.10f)))
        Box(Modifier.matchParentSize().padding(14.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.55f)))
        Column(
            Modifier.matchParentSize().padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (ch != null) ChakraSymbol(ch.index, ch.color, 92.dp)
            Spacer(Modifier.height(10.dp))
            if (!card.core.isNullOrBlank()) {
                Text(card.core, fontFamily = displayFamily(), fontWeight = FontWeight.Bold, fontSize = 30.sp, color = ink, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            val size = when {
                card.main.length > 140 -> 19.sp
                card.main.length > 90 -> 22.sp
                else -> 26.sp
            }
            Text(card.main, fontFamily = displayFamily(), fontWeight = FontWeight.SemiBold, fontSize = size, lineHeight = size * 1.25f, color = Lotus.Ink, textAlign = TextAlign.Center)
            if (!card.sub.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(card.sub, fontFamily = scriptFamily(), fontSize = 22.sp, lineHeight = 28.sp, color = ink, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                (if (card.custom) "✍️  " else "") + areaName(card.area).uppercase() + "   ·   ${index + 1} / $total",
                fontSize = 11.sp, letterSpacing = 1.sp, color = Lotus.InkSoft, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DeckControls(playing: Boolean, onPlay: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    val s = App.settings
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            FilledTonalIconButton(onClick = onPrev) { Icon(Icons.Filled.SkipPrevious, tr("ui.common.previous")) }
            FilledIconButton(onClick = onPlay, modifier = Modifier.size(60.dp)) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, tr("ui.common.listen"), modifier = Modifier.size(32.dp))
            }
            FilledTonalIconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, tr("ui.common.next")) }
        }
        Spacer(Modifier.height(6.dp))
        Segmented(
            options = listOf(
                "manual" to tr("ui.affirm.mode_manual"),
                "auto" to tr("ui.affirm.mode_auto"),
                "repeat" to tr("ui.affirm.mode_repeat"),
                "count" to tr("ui.affirm.mode_count", "n" to s.ttsCount),
            ),
            selected = s.ttsMode,
            onSelect = { s.ttsMode = it },
        )
        if (s.ttsMode == "count") {
            Stepper(s.ttsCount, 1..108, { s.ttsCount = it }, tr("ui.affirm.times_each"))
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Bulk edit of the user's own affirmations
// ------------------------------------------------------------------------------------------------

@Composable
fun BulkEditScreen(area: String) {
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf(App.affirmations.customFor(area).joinToString("\n")) }
    var confirmClear by remember { mutableStateOf(false) }
    val exporter = rememberExporter("text/plain")
    val importer = rememberImporter { content ->
        val parsed = AffirmationImport.parse(content, area, Catalog.areas.map { it.id }.toSet(), areaNameMap())
        val lines = parsed.values.flatten()
        text = (text.lines() + lines).filter { it.isNotBlank() }.distinct().joinToString("\n")
        Sharing.toast(ctx, tr("ui.data.imported_n", "n" to lines.size))
    }
    val count = text.lines().count { it.isNotBlank() }
    AppScaffold(title = tr("ui.affirm.bulk_edit")) { padding ->
        PageColumn(padding) {
            Text(Catalog.area(area).emoji + "  " + tr("area.$area.name"), style = MaterialTheme.typography.titleLarge, color = Lotus.PlumDeep)
            Text(tr("ui.affirm.bulk_hint"), color = Lotus.InkSoft)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
                minLines = 10,
                label = { Text(tr("ui.affirm.bulk_label", "n" to count)) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.75f), focusedContainerColor = Color.White.copy(alpha = 0.92f)),
            )
            Button(
                onClick = {
                    App.affirmations.setCustom(area, text.lines())
                    Sharing.toast(ctx, tr("ui.common.saved"))
                    App.nav.back()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text(tr("ui.common.save")) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = importer, modifier = Modifier.weight(1f)) { Text(tr("ui.data.import")) }
                OutlinedButton(onClick = { exporter("inner_lotus_${area}_mine.txt") { text } }, modifier = Modifier.weight(1f)) { Text(tr("ui.data.export")) }
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) { Text(tr("ui.common.clear")) }
            }
            Text(tr("ui.affirm.bulk_formats"), fontSize = 12.sp, color = Lotus.InkSoft)
        }
    }
    if (confirmClear) ConfirmDialog(tr("ui.common.clear"), tr("ui.affirm.clear_confirm"), tr("ui.common.clear"), { text = "" }, { confirmClear = false }, danger = true)
}

private fun areaNameMap(): Map<String, String> {
    val m = HashMap<String, String>()
    for (a in Catalog.areas) {
        m[tr("area.${a.id}.name").lowercase()] = a.id
        m[I18n.english("area.${a.id}.name").lowercase()] = a.id
    }
    return m
}

// ------------------------------------------------------------------------------------------------
// Import / export of affirmations
// ------------------------------------------------------------------------------------------------

@Composable
fun AffirmDataScreen() {
    val ctx = LocalContext.current
    val store by App.affirmations.state.collectAsState()
    var area by rememberSaveable { mutableStateOf(Catalog.areas.first().id) }
    var chakra by rememberSaveable { mutableStateOf("all") }
    val jsonExporter = rememberExporter("application/json")
    val csvExporter = rememberExporter("text/csv")
    val importer = rememberImporter { content ->
        val trimmed = content.trimStart()
        val n = try {
            if (trimmed.startsWith("{")) App.affirmations.importJson(content)
            else AffirmationImport.parse(content, area, Catalog.areas.map { it.id }.toSet(), areaNameMap())
                .entries.sumOf { (a, lines) -> App.affirmations.addCustom(a, lines) }
        } catch (e: Exception) { -1 }
        Sharing.toast(ctx, if (n < 0) tr("ui.data.import_failed") else tr("ui.data.imported_n", "n" to n))
    }
    AppScaffold(title = tr("ui.affirm.data_title")) { padding ->
        PageColumn(padding) {
            GlassCard {
                Text(tr("ui.affirm.data_mine"), style = MaterialTheme.typography.titleMedium)
                Text(tr("ui.affirm.data_mine_hint", "n" to store.custom.values.sumOf { it.size }, "f" to store.favorites.size), color = Lotus.InkSoft, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { jsonExporter("inner_lotus_affirmations.json") { App.affirmations.exportJson() } }, modifier = Modifier.weight(1f)) { Text(tr("ui.data.export_all")) }
                    OutlinedButton(onClick = importer, modifier = Modifier.weight(1f)) { Text(tr("ui.data.import")) }
                }
                Spacer(Modifier.height(6.dp))
                Text(tr("ui.affirm.import_hint"), fontSize = 12.sp, color = Lotus.InkSoft)
            }
            GlassCard {
                Text(tr("ui.affirm.data_section"), style = MaterialTheme.typography.titleMedium)
                Text(tr("ui.affirm.data_section_hint"), color = Lotus.InkSoft, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                AreaDropdown(area) { area = it }
                Spacer(Modifier.height(6.dp))
                ChakraDropdown(chakra) { chakra = it }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        csvExporter("inner_lotus_${area}_${chakra}_${I18n.lang.code}.csv") { deckCsv(listOf(area), chakra) }
                    }, modifier = Modifier.weight(1f)) { Text(tr("ui.data.export_csv")) }
                    OutlinedButton(onClick = {
                        jsonExporter("inner_lotus_${area}_mine.json") { App.affirmations.exportJson(listOf(area)) }
                    }, modifier = Modifier.weight(1f)) { Text(tr("ui.affirm.export_mine_area")) }
                }
            }
            GlassCard {
                Text(tr("ui.affirm.data_library"), style = MaterialTheme.typography.titleMedium)
                Text(tr("ui.affirm.data_library_hint"), color = Lotus.InkSoft, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    csvExporter("inner_lotus_all_affirmations_${I18n.lang.code}.csv") { deckCsv(Catalog.areas.map { it.id }, "all") }
                }, modifier = Modifier.fillMaxWidth()) { Text(tr("ui.data.export_csv")) }
            }
        }
    }
}

/** CSV of built-in (and the user's own) affirmations in the current language. */
private fun deckCsv(areas: List<String>, chakra: String): String = buildString {
    append(Csv.row("area", "area_name", "chakra", "core", "affirmation", "chakra_affirmation")).append("\n")
    val chakras = if (chakra == "all") Composer.CHAKRAS else listOf(chakra)
    for (a in areas) {
        for (c in chakras) {
            for (ref in Composer.chakraDeck(c, Affirm.LINES, Affirm.SEEDS)) {
                val card = Affirm.builtIn(a, ref)
                append(Csv.row(a, tr("area.$a.name"), c, card.core.orEmpty(), card.main, card.sub.orEmpty())).append("\n")
            }
        }
        for (t in App.affirmations.customFor(a)) append(Csv.row(a, tr("area.$a.name"), "", "", t, "")).append("\n")
    }
}

@Composable
fun AreaDropdown(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(Catalog.area(selected).emoji + "  " + tr("area.$selected.name"), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 420.dp)) {
            Catalog.areas.forEach { a ->
                DropdownMenuItem(text = { Text(a.emoji + "  " + tr("area.${a.id}.name")) }, onClick = { open = false; onSelect(a.id) })
            }
        }
    }
}

@Composable
fun ChakraDropdown(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = if (selected == "all") tr("ui.affirm.all_chakras") else tr("chakra.$selected.name") + " · " + tr("chakra.$selected.core")
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(tr("ui.affirm.all_chakras")) }, onClick = { open = false; onSelect("all") })
            Catalog.chakras.forEach { c ->
                DropdownMenuItem(
                    text = { Text(tr("chakra.${c.id}.name") + " · " + tr("chakra.${c.id}.core")) },
                    leadingIcon = { ChakraSymbol(c.index, c.color, 24.dp) },
                    onClick = { open = false; onSelect(c.id) },
                )
            }
        }
    }
}
