package com.sjpixels.innerlotus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import com.sjpixels.innerlotus.ui.icons.LotusIcons
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import com.sjpixels.innerlotus.ui.theme.scriptFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val FAVORITES_AREA = "__fav"

private fun areaName(id: String) = if (id == FAVORITES_AREA) tr("ui.affirm.favorites") else tr("area.$id.name")

/** Core statements are stored in capitals ("I LOVE"); rows show them as a phrase ("I love"). */
private fun sentenceCase(s: String): String {
    val locale = I18n.lang.locale
    return s.lowercase(locale).replaceFirstChar { it.titlecase(locale) }
}

// ------------------------------------------------------------------------------------------------
// Area list
// ------------------------------------------------------------------------------------------------

@Composable
fun AreasScreen(chakra: String?) {
    val store by App.affirmations.state.collectAsState()
    var query by remember { mutableStateOf("") }
    AppScaffold(title = tr("ui.section.affirmations")) { padding ->
        PageColumn(padding) {
            ScreenHeader(title = tr("ui.hero.areas.title"), subtitle = tr("ui.hero.areas.sub"))
            LearnCard("affirmations")
            if (chakra != null) {
                val c = Catalog.chakra(chakra)
                if (c != null) GlassCard(tone = Tone.Lavender, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChakraDot(c.index, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            tr("ui.affirm.chakra_selected", "chakra" to tr("chakra.$chakra.name")),
                            style = LotusType.itemTitle,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        TextLink(tr("ui.common.clear"), { App.nav.replace(Screen.Areas(null)) }, icon = Icons.Outlined.Close)
                    }
                }
            }
            // Search, with import & export beside it (the top bar has no room for a third button).
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchField(query, { query = it }, tr("ui.affirm.search"), Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                CircleIconButton(
                    Icons.Outlined.ImportExport,
                    tr("ui.affirm.data_title"),
                    { App.nav.go(Screen.AffirmData) },
                    size = 52.dp,
                    iconSize = 22.dp,
                )
            }
            if (store.favorites.isNotEmpty() && query.isBlank()) {
                ListGroup {
                    ListRow(
                        title = tr("ui.affirm.favorites"),
                        leading = {
                            IconBadge(Icons.Outlined.FavoriteBorder, tint = Lotus.Rose, background = Lotus.Blush.copy(alpha = 0.8f))
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${store.favorites.size}", style = LotusType.itemTitle.copy(color = Lotus.InkSoft))
                                Spacer(Modifier.width(10.dp))
                                ChevronCircle()
                            }
                        },
                        onClick = { App.nav.go(Screen.Deck(FAVORITES_AREA, null)) },
                    )
                }
            }
            val q = query.trim().lowercase()
            val areas = Catalog.areas.filter { q.isEmpty() || tr("area.${it.id}.name").lowercase().contains(q) || tr("area.${it.id}.desc").lowercase().contains(q) }
            TileGrid(areas) { a, i, m ->
                val custom = store.custom[a.id]?.size ?: 0
                PracticeTile(
                    icon = LotusIcons.area(a.id),
                    title = tr("area.${a.id}.name"),
                    subtitle = tr("area.${a.id}.desc"),
                    // 1008 built-in cards, plus the user's own ("Mine (3)") when there are any.
                    footnote = "1008" + if (custom > 0) "  ·  " + tr("ui.affirm.filter_mine", "n" to custom) else "",
                    tone = toneFor(i),
                    // Tall enough for a 2-line title + 2-line caption, so tiles in a row line up.
                    minHeight = 174.dp,
                    modifier = m,
                    onClick = { App.nav.go(if (chakra != null) Screen.Deck(a.id, chakra) else Screen.ChakraPick(a.id)) },
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Chakra picker (poster order: crown at the top, root at the bottom)
// ------------------------------------------------------------------------------------------------

@Composable
fun ChakraPickScreen(area: String) {
    AppScaffold(title = tr("ui.section.affirmations")) { padding ->
        PageColumn(padding) {
            ScreenHeader(
                title = tr("ui.hero.chakra_pick.title"),
                kicker = tr("area.$area.name"),
                subtitle = tr("ui.hero.chakra_pick.sub"),
            )
            SecondaryButton(
                text = tr("ui.affirm.random_all"),
                onClick = { App.nav.go(Screen.Deck(area, null)) },
                leadingIcon = Icons.Outlined.Shuffle,
                trailingIcon = Icons.Outlined.ArrowOutward,
            )
            ListGroup {
                Catalog.chakras.reversed().forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    ListRow(
                        title = tr("chakra.${c.id}.name"),
                        subtitle = tr("chakra.${c.id}.sanskrit"),
                        leading = { ChakraDot(c.index, 44.dp) },
                        trailing = { CoreWord(tr("chakra.${c.id}.core")) },
                        onClick = { App.nav.go(Screen.Deck(area, c.id)) },
                    )
                }
            }
            QuietNote(tr("ui.affirm.poster_hint"))
            TextLink(
                tr("ui.affirm.learn_chakras"),
                { App.nav.go(Screen.ChakraGuide) },
                Modifier.align(Alignment.CenterHorizontally),
                icon = Icons.Outlined.FilterVintage,
            )
            QuietNote(tr("area.$area.desc"), icon = LotusIcons.area(area))
        }
    }
}

/** Accent "I love ↗" at the end of a chakra row. */
@Composable
private fun CoreWord(core: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(sentenceCase(core), style = LotusType.pill.copy(color = Lotus.Accent), maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.ArrowOutward, null, tint = Lotus.Accent, modifier = Modifier.size(15.dp))
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

    // Every card names its chakra, so the bar keeps to the area and stays readable.
    AppScaffold(title = areaName(area)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter + "more" menu (kept here rather than in the crowded top bar).
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (area != FAVORITES_AREA) {
                    Segmented(
                        options = listOf(
                            "all" to tr("ui.affirm.filter_all", "n" to (if (chakra == null) 1008 else 144)),
                            "mine" to tr("ui.affirm.filter_mine", "n" to (store.custom[area]?.size ?: 0)),
                            "fav" to tr("ui.affirm.filter_fav"),
                        ),
                        selected = filter,
                        onSelect = { playing = false; filter = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                DeckMenu(area)
            }
            if (cards.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyDeck(mine = filter == "mine", onWrite = { App.nav.go(Screen.BulkEdit(area)) })
                }
            } else {
                // Card-stack swipe: the top card follows the finger, tilts and flies off to the side,
                // while the next cards wait stacked underneath and rise into place.
                HorizontalPager(
                    state = pager,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { it },
                ) { page ->
                    // 0 = on top, >0 = waiting in the stack, <0 = swiped away to the left
                    val p = page - pager.currentPage - pager.currentPageOffsetFraction
                    AffirmationCard(
                        card = cards[page],
                        index = page,
                        total = cards.size,
                        modifier = Modifier
                            .zIndex(-p)
                            .fillMaxSize()
                            .padding(top = 6.dp, bottom = 22.dp)
                            .graphicsLayer { deckTransform(p, 16.dp.toPx(), 12.dp.toPx()) },
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
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fav = current.favKey in store.favorites
                        CircleIconButton(
                            if (fav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            tr("ui.affirm.favorite"),
                            { App.affirmations.toggleFavorite(current.favKey) },
                            size = 46.dp,
                            tint = if (fav) Lotus.Rose else Lotus.Ink,
                            material = if (fav) Glass.Blush else Glass.Pill,
                        )
                        CircleIconButton(Icons.Outlined.ContentCopy, tr("ui.common.copy"), { Sharing.copy(ctx, Affirm.plainText(current)) }, size = 46.dp)
                        CircleIconButton(
                            Icons.Outlined.Share,
                            tr("ui.common.share"),
                            {
                                scope.launch {
                                    val bmp = withContext(Dispatchers.Default) { Sharing.renderCard(ctx, cardSpec(current)) }
                                    Sharing.shareImage(ctx, bmp, Affirm.plainText(current))
                                }
                            },
                            size = 46.dp,
                        )
                        CircleIconButton(
                            Icons.Outlined.Download,
                            tr("ui.affirm.download"),
                            {
                                val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) { pendingSave = current; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                                else scope.launch { saveCard(ctx, current) }
                            },
                            size = 46.dp,
                        )
                        if (area != FAVORITES_AREA) {
                            CircleIconButton(Icons.Outlined.EditNote, tr("ui.affirm.bulk_edit"), { App.nav.go(Screen.BulkEdit(area)) }, size = 46.dp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Round "more" button with the deck's options: bulk edit, change chakra, reshuffle, data, guide. */
@Composable
private fun DeckMenu(area: String) {
    var open by remember { mutableStateOf(false) }
    Box {
        CircleIconButton(Icons.Outlined.MoreHoriz, null, { open = true }, size = 48.dp, iconSize = 22.dp)
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Lotus.Sheet,
        ) {
            if (area != FAVORITES_AREA) {
                DeckMenuItem(tr("ui.affirm.bulk_edit"), Icons.Outlined.EditNote) { open = false; App.nav.go(Screen.BulkEdit(area)) }
                DeckMenuItem(tr("ui.affirm.change_chakra"), LotusIcons.Chakras) { open = false; App.nav.replace(Screen.ChakraPick(area)) }
            }
            DeckMenuItem(tr("ui.affirm.reshuffle"), Icons.Outlined.Shuffle) { open = false; App.affirmations.reshuffle() }
            DeckMenuItem(tr("ui.affirm.data_title"), Icons.Outlined.ImportExport) { open = false; App.nav.go(Screen.AffirmData) }
            DeckMenuItem(tr("learn.affirmations.title"), Icons.AutoMirrored.Outlined.MenuBook) { open = false; App.nav.go(Screen.Learn("affirmations")) }
        }
    }
}

@Composable
private fun DeckMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = LotusType.itemTitle) },
        leadingIcon = { Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}

/** Empty "Mine" / "Favourites" deck: a quiet note, and a way to start writing for "Mine". */
@Composable
private fun EmptyDeck(mine: Boolean, onWrite: () -> Unit) {
    GlassCard(strong = true, padding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (mine) IconBadge(Icons.Outlined.EditNote, size = 52.dp)
            else IconBadge(Icons.Outlined.FavoriteBorder, size = 52.dp, tint = Lotus.Rose, background = Lotus.Blush.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            Text(
                if (mine) tr("ui.affirm.empty_mine") else tr("ui.affirm.empty_fav"),
                style = MaterialTheme.typography.bodyLarge.copy(color = Lotus.Ink),
                textAlign = TextAlign.Center,
            )
            if (mine) {
                Spacer(Modifier.height(18.dp))
                PrimaryButton(tr("ui.affirm.bulk_edit"), onWrite, trailingIcon = Icons.Outlined.Edit)
            }
        }
    }
}

/**
 * Places one deck card for stack position [p] (0 = top card, 1, 2 = waiting underneath,
 * negative = swiped off to the left). [spacing] is the pager's page spacing, [step] the vertical
 * offset between stacked cards.
 */
private fun GraphicsLayerScope.deckTransform(p: Float, spacing: Float, step: Float) {
    shape = RoundedCornerShape(30.dp)
    clip = false
    transformOrigin = TransformOrigin(0.5f, 1.15f) // pivot below the card: it swings in an arc
    if (p >= 0f) {
        // Waiting cards: cancel the pager's sideways offset so they sit centred under the top card.
        val depth = p.coerceAtMost(2.6f)
        translationX = -p * (size.width + spacing)
        translationY = depth * step
        val sc = 1f - 0.06f * depth
        scaleX = sc; scaleY = sc
        alpha = (1f - 0.3f * (depth - 1f).coerceAtLeast(0f)).coerceIn(0f, 1f) * if (p > 2.5f) 0f else 1f
        shadowElevation = (14f - 6f * depth).coerceAtLeast(2f) * density
        rotationZ = 0f
    } else {
        // Leaving / returning card: follows the finger, tilts and is thrown a little further.
        val t = (-p).coerceAtMost(1.2f)
        translationX = -t * size.width * 0.18f
        translationY = -t * step
        rotationZ = -t * 14f
        scaleX = 1f; scaleY = 1f
        alpha = 1f - (t - 0.75f).coerceAtLeast(0f) * 2f
        shadowElevation = 18f * density
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

/**
 * One affirmation card: the card picture under a frosted veil, the chakra disc (or a sparkle for
 * cards without a chakra), chakra name, core statement in the chakra's ink, the affirmation in
 * serif, the chakra line in italic, and a quiet "AREA · 01 / 144" label.
 */
@Composable
private fun AffirmationCard(card: Affirm.Card, index: Int, total: Int, modifier: Modifier) {
    val ch = Catalog.chakra(card.chakra)
    val ink = ch?.let { Color(it.ink) } ?: Lotus.Accent
    val style = App.settings.cardStyle
    val shape = RoundedCornerShape(30.dp)
    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.34f))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.45f))), shape),
    ) {
        // Short cards (small phones, or with the count stepper open) use a tighter layout.
        val compact = maxHeight < 440.dp
        val fullHeight = if (constraints.hasBoundedHeight) maxHeight else 0.dp
        if (style != "app") BgImage(cardBgId(card), 2, Modifier.matchParentSize())
        if (style == "chakra" && ch != null) Box(Modifier.matchParentSize().background(Color(ch.color).copy(alpha = 0.10f)))
        // Frosted veil: the picture stays a soft wash of colour behind the words.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.66f),
                    0.5f to Color.White.copy(alpha = 0.52f),
                    1f to Color.White.copy(alpha = 0.62f),
                ),
            ),
        )
        // Centred when it fits; very long affirmations scroll instead of being cut off.
        Column(Modifier.matchParentSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = fullHeight)
                    .padding(horizontal = 26.dp, vertical = if (compact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (ch != null) {
                    ChakraDot(ch.index, if (compact) 46.dp else 72.dp)
                    Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
                    if (!compact) {
                        // Small caps need more contrast than the large ink lines: deepen the ink a little.
                        Kicker(
                            tr("chakra.${ch.id}.name") + "  ·  " + tr("chakra.${ch.id}.sanskrit"),
                            color = lerp(ink, Lotus.Ink, 0.3f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    Sparkle(size = 26.dp)
                    Spacer(Modifier.height(16.dp))
                }
                if (!card.core.isNullOrBlank()) {
                    Text(
                        card.core,
                        style = MaterialTheme.typography.headlineSmall.copy(color = ink, letterSpacing = 0.14.em),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                }
                val size = when {
                    card.main.length > 140 -> if (compact) 17.sp else 19.sp
                    card.main.length > 90 -> if (compact) 19.sp else 22.sp
                    else -> if (compact) 22.sp else 25.sp
                }
                Text(card.main, style = LotusType.quote.copy(fontSize = size, lineHeight = size * 1.3f), textAlign = TextAlign.Center)
                if (!card.sub.isNullOrBlank()) {
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                    Text(
                        card.sub,
                        style = LotusType.quote.copy(
                            fontFamily = scriptFamily(),
                            fontStyle = FontStyle.Italic,
                            fontSize = if (compact) 19.sp else 22.sp,
                            lineHeight = if (compact) 25.sp else 29.sp,
                            color = ink,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The user's own words carry a small pen.
                    if (card.custom) {
                        Icon(Icons.Outlined.Edit, null, tint = Lotus.InkSoft, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Kicker(
                        areaName(card.area) + "  ·  " + (index + 1).toString().padStart(2, '0') + " / " + total,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckControls(playing: Boolean, onPlay: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    val s = App.settings
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayerRow(
            playing = playing,
            onPlayPause = onPlay,
            left = { CircleIconButton(Icons.Outlined.SkipPrevious, tr("ui.common.previous"), onPrev, size = 48.dp, iconSize = 22.dp) },
            right = { CircleIconButton(Icons.Outlined.SkipNext, tr("ui.common.next"), onNext, size = 48.dp, iconSize = 22.dp) },
            playDescription = if (playing) tr("ui.common.stop") else tr("ui.common.listen"),
        )
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
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), material = Glass.Segments) {
                Box(Modifier.padding(start = 18.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                    Stepper(s.ttsCount, 1..108, { s.ttsCount = it }, tr("ui.affirm.times_each"))
                }
            }
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
    // Two of the area's own affirmations, shown as a gentle example while the box is empty.
    val example = remember(area, I18n.lang, I18n.feminine) { Affirm.lines(area).take(2).joinToString("\n") }
    AppScaffold(title = tr("ui.section.affirmations")) { padding ->
        PageColumn(padding) {
            ScreenHeader(
                title = tr("ui.hero.bulk.title"),
                kicker = tr("ui.hero.bulk.kicker"),
                subtitle = tr("ui.affirm.bulk_hint"),
            )
            GlassCard {
                AreaChip(area)
                Spacer(Modifier.height(16.dp))
                Text(tr("ui.affirm.bulk_label", "n" to count), style = LotusType.itemTitle)
                Spacer(Modifier.height(10.dp))
                LotusTextField(value = text, onValueChange = { text = it }, placeholder = example, minLines = 10)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = Lotus.InkSoft, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("ui.quiet.private"), style = LotusType.micro)
                }
            }
            PrimaryButton(
                tr("ui.common.save"),
                onClick = {
                    App.affirmations.setCustom(area, text.lines())
                    Sharing.toast(ctx, tr("ui.common.saved"))
                    App.nav.back()
                },
                trailingIcon = Icons.Outlined.Check,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(tr("ui.data.import"), importer, Modifier.weight(1f), icon = Icons.Outlined.FileUpload)
                PillButton(tr("ui.data.export"), { exporter("inner_lotus_${area}_mine.txt") { text } }, Modifier.weight(1f), icon = Icons.Outlined.FileDownload)
                PillButton(tr("ui.common.clear"), { confirmClear = true }, Modifier.weight(1f), icon = Icons.Outlined.Clear)
            }
            QuietNote(tr("ui.affirm.bulk_formats"))
        }
    }
    if (confirmClear) ConfirmDialog(tr("ui.common.clear"), tr("ui.affirm.clear_confirm"), tr("ui.common.clear"), { text = "" }, { confirmClear = false }, danger = true)
}

/** Non-interactive glass pill naming a life area, with its line icon. */
@Composable
private fun AreaChip(area: String) {
    GlassSurface(shape = PillShape, material = Glass.Pill) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(LotusIcons.area(area), null, tint = Lotus.Accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(tr("area.$area.name"), style = LotusType.pill.copy(color = Lotus.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
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
            ScreenHeader(title = tr("ui.hero.data.title"))
            DataCard(
                Icons.Outlined.CollectionsBookmark,
                tr("ui.affirm.data_mine"),
                tr("ui.affirm.data_mine_hint", "n" to store.custom.values.sumOf { it.size }, "f" to store.favorites.size),
            ) {
                PrimaryButton(
                    tr("ui.data.export_all"),
                    { jsonExporter("inner_lotus_affirmations.json") { App.affirmations.exportJson() } },
                    trailingIcon = Icons.Outlined.FileDownload,
                )
                SecondaryButton(tr("ui.data.import"), importer, trailingIcon = Icons.Outlined.FileUpload)
                Text(tr("ui.affirm.import_hint"), style = LotusType.micro)
            }
            DataCard(Icons.Outlined.TableChart, tr("ui.affirm.data_section"), tr("ui.affirm.data_section_hint")) {
                AreaDropdown(area) { area = it }
                ChakraDropdown(chakra) { chakra = it }
                SecondaryButton(
                    tr("ui.data.export_csv"),
                    { csvExporter("inner_lotus_${area}_${chakra}_${I18n.lang.code}.csv") { deckCsv(listOf(area), chakra) } },
                    trailingIcon = Icons.Outlined.FileDownload,
                )
                SecondaryButton(
                    tr("ui.affirm.export_mine_area"),
                    { jsonExporter("inner_lotus_${area}_mine.json") { App.affirmations.exportJson(listOf(area)) } },
                    trailingIcon = Icons.Outlined.FileDownload,
                )
            }
            DataCard(Icons.AutoMirrored.Outlined.LibraryBooks, tr("ui.affirm.data_library"), tr("ui.affirm.data_library_hint")) {
                SecondaryButton(
                    tr("ui.data.export_csv"),
                    { csvExporter("inner_lotus_all_affirmations_${I18n.lang.code}.csv") { deckCsv(Catalog.areas.map { it.id }, "all") } },
                    trailingIcon = Icons.Outlined.FileDownload,
                )
            }
            QuietNote(tr("ui.quiet.private"), icon = Icons.Outlined.Lock)
        }
    }
}

/** Glass card for one kind of import / export: badge, serif title, hint, then its actions. */
@Composable
private fun DataCard(icon: ImageVector, title: String, hint: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(padding = 20.dp) {
        IconBadge(icon)
        Spacer(Modifier.height(14.dp))
        Text(title, style = LotusType.cardTitle)
        Spacer(Modifier.height(6.dp))
        Text(hint, style = LotusType.small)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
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

/** Glass field that opens a menu (area / chakra pickers). [leading] sits in a 24dp slot. */
@Composable
private fun DropdownField(text: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        material = Glass.Field,
        onClick = onClick,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(12.dp))
            Text(text, style = LotusType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Lotus.InkSoft, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun AreaDropdown(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        DropdownField(tr("area.$selected.name"), { open = true }) {
            Icon(LotusIcons.area(selected), null, tint = Lotus.Accent, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = 420.dp),
            shape = RoundedCornerShape(22.dp),
            containerColor = Lotus.Sheet,
        ) {
            Catalog.areas.forEach { a ->
                DropdownMenuItem(
                    text = { Text(tr("area.${a.id}.name"), style = LotusType.itemTitle) },
                    leadingIcon = { Icon(LotusIcons.area(a.id), null, tint = Lotus.Accent, modifier = Modifier.size(20.dp)) },
                    trailingIcon = { if (a.id == selected) Icon(Icons.Outlined.Check, null, tint = Lotus.Accent, modifier = Modifier.size(18.dp)) },
                    onClick = { open = false; onSelect(a.id) },
                )
            }
        }
    }
}

@Composable
fun ChakraDropdown(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = if (selected == "all") tr("ui.affirm.all_chakras") else tr("chakra.$selected.name") + " · " + tr("chakra.$selected.core")
    val current = Catalog.chakra(selected)
    Box {
        DropdownField(label, { open = true }) {
            if (current != null) ChakraDot(current.index, 24.dp)
            else Icon(LotusIcons.Chakras, null, tint = Lotus.Accent, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = Lotus.Sheet,
        ) {
            DropdownMenuItem(
                text = { Text(tr("ui.affirm.all_chakras"), style = LotusType.itemTitle) },
                leadingIcon = { Icon(LotusIcons.Chakras, null, tint = Lotus.Accent, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (selected == "all") Icon(Icons.Outlined.Check, null, tint = Lotus.Accent, modifier = Modifier.size(18.dp)) },
                onClick = { open = false; onSelect("all") },
            )
            Catalog.chakras.forEach { c ->
                DropdownMenuItem(
                    text = { Text(tr("chakra.${c.id}.name") + " · " + tr("chakra.${c.id}.core"), style = LotusType.itemTitle) },
                    leadingIcon = { ChakraDot(c.index, 24.dp) },
                    trailingIcon = { if (c.id == selected) Icon(Icons.Outlined.Check, null, tint = Lotus.Accent, modifier = Modifier.size(18.dp)) },
                    onClick = { open = false; onSelect(c.id) },
                )
            }
        }
    }
}
