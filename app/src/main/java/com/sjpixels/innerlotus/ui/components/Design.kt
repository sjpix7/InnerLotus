package com.sjpixels.innerlotus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sjpixels.innerlotus.core.Catalog
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.share.Sharing
import com.sjpixels.innerlotus.ui.glass.GlassMaterial
import com.sjpixels.innerlotus.ui.glass.glass
import com.sjpixels.innerlotus.ui.theme.Lotus
import com.sjpixels.innerlotus.ui.theme.LotusType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// =================================================================================================
//  Glass materials — frosted panels drawn over a blurred slice of the background (see ui/glass).
// =================================================================================================

object Glass {
    private val W = Color.White
    val Card = GlassMaterial(10, W.copy(alpha = 0.66f), W.copy(alpha = 0.52f), edgeTop = W.copy(alpha = 0.92f), edgeBottom = W.copy(alpha = 0.42f))
    val Strong = GlassMaterial(10, W.copy(alpha = 0.84f), W.copy(alpha = 0.76f), edgeTop = W.copy(alpha = 0.96f), edgeBottom = W.copy(alpha = 0.55f))
    val Pill = GlassMaterial(6, W.copy(alpha = 0.70f), W.copy(alpha = 0.60f), edgeTop = W.copy(alpha = 0.95f), edgeBottom = W.copy(alpha = 0.55f))
    val Segments = GlassMaterial(8, W.copy(alpha = 0.46f), W.copy(alpha = 0.38f), edgeTop = W, edgeBottom = W.copy(alpha = 0.6f))
    val Field = GlassMaterial(8, W.copy(alpha = 0.72f), W.copy(alpha = 0.64f), edgeTop = W.copy(alpha = 0.95f), edgeBottom = W.copy(alpha = 0.6f))
    val Bar = GlassMaterial(12, W.copy(alpha = 0.40f), W.copy(alpha = 0.26f), edgeWidth = 0.dp)
    val Ink = GlassMaterial(12, Lotus.Ink.copy(alpha = 0.93f), Lotus.Ink.copy(alpha = 0.90f), edgeTop = W.copy(alpha = 0.22f), edgeBottom = W.copy(alpha = 0.04f))

    private fun tint(c: Color, a: Float) =
        GlassMaterial(10, c.copy(alpha = a), c.copy(alpha = a * 0.88f), edgeTop = W.copy(alpha = 0.92f), edgeBottom = W.copy(alpha = 0.42f))
    val Lavender = tint(Color(0xFFE5DEF3), 0.74f)
    val Sage = tint(Color(0xFFE1EDE5), 0.78f)
    val Peach = tint(Color(0xFFFAE3D9), 0.78f)
    val Blush = tint(Color(0xFFF4DFEA), 0.74f)
}

/** Surface tone for cards and tiles. */
enum class Tone { Plain, Strong, Lavender, Sage, Peach, Blush }

fun Tone.material(): GlassMaterial = when (this) {
    Tone.Plain -> Glass.Card
    Tone.Strong -> Glass.Strong
    Tone.Lavender -> Glass.Lavender
    Tone.Sage -> Glass.Sage
    Tone.Peach -> Glass.Peach
    Tone.Blush -> Glass.Blush
}

/** Tile tones cycle lavender → sage → peach → blush (row 1: lavender, sage; row 2: peach, blush). */
fun toneFor(index: Int): Tone = when (index % 4) { 0 -> Tone.Lavender; 1 -> Tone.Sage; 2 -> Tone.Peach; else -> Tone.Blush }

val CardShape = RoundedCornerShape(24.dp)
val PillShape = RoundedCornerShape(50)

/** Any frosted surface. Use [GlassCard] for padded content cards. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CardShape,
    material: GlassMaterial = Glass.Card,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .glass(material, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = contentAlignment,
        content = content,
    )
}

// =================================================================================================
//  Typography helpers
// =================================================================================================

/** "A softer place to *begin.*" → the starred part in italic. Stars never show. */
fun styledTitle(s: String): AnnotatedString = buildAnnotatedString {
    var italic = false
    val parts = s.split('*')
    parts.forEachIndexed { i, part ->
        if (italic) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part) } else append(part)
        if (i < parts.lastIndex) italic = !italic
    }
}

/** Tiny letter-spaced caps line ("YOUR SPACE TO SIMPLY BE"). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = Lotus.InkSoft, textAlign: TextAlign? = null) {
    Text(text.uppercase(I18n.lang.locale), modifier = modifier, style = LotusType.kicker.copy(color = color), textAlign = textAlign)
}

/**
 * Editorial page header: optional kicker, serif headline (supports *italic* markup), optional
 * muted subtitle. [large] uses the bigger display size (welcome screen).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    subtitle: String? = null,
    center: Boolean = false,
    large: Boolean = false,
) {
    val align = if (center) TextAlign.Center else TextAlign.Start
    Column(
        modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!kicker.isNullOrBlank()) Kicker(kicker, textAlign = align)
        Text(
            styledTitle(title),
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
            textAlign = align,
        )
        if (!subtitle.isNullOrBlank()) Text(subtitle, style = LotusType.small, textAlign = align)
    }
}

/** Serif section heading with an optional accent link on the right ("See all ↗"). */
@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(styledTitle(title), style = LotusType.section, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextLink(action, onAction)
    }
}

/** Accent text link with a small arrow. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = Icons.Outlined.ArrowOutward) {
    Row(
        modifier
            .clip(PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = LotusType.button.copy(color = Lotus.Accent, fontSize = LotusType.pill.fontSize))
        if (icon != null) {
            Spacer(Modifier.width(4.dp))
            Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(15.dp))
        }
    }
}

/** Small centred footnote. */
@Composable
fun QuietNote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = Lotus.InkSoft, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = LotusType.micro, textAlign = TextAlign.Center)
    }
}

// =================================================================================================
//  Icons, badges, tiles, rows
// =================================================================================================

/** Soft rounded-square icon badge (lavender, violet glyph). */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Lotus.BadgeInk,
    background: Color = Lotus.BadgeBg,
    contentDescription: String? = null,
) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.36f)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Round glass icon button (back, more, language, play…). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Lotus.Ink,
    material: GlassMaterial = Glass.Pill,
) {
    GlassSurface(modifier.size(size), shape = CircleShape, material = material, onClick = onClick, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Small round "›" used at the end of list rows. */
@Composable
fun ChevronCircle(modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Lotus.Ink, modifier = Modifier.size(18.dp))
    }
}

/** Tinted practice tile for 2-column grids: icon, title, caption, optional footnote. */
@Composable
fun PracticeTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    footnote: String? = null,
    tone: Tone = Tone.Lavender,
    selected: Boolean = false,
    minHeight: Dp = 136.dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    GlassSurface(
        modifier
            .heightIn(min = minHeight)
            .then(if (selected) Modifier.border(2.dp, Lotus.AccentSoft.copy(alpha = 0.7f), shape) else Modifier),
        shape = shape,
        material = tone.material(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = LotusType.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = LotusType.itemSub, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!footnote.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(footnote, style = LotusType.micro.copy(color = Lotus.Accent))
            }
        }
    }
}

/** Two-column grid of tiles (no lazy layout needed for short lists). */
@Composable
fun <T> TileGrid(items: List<T>, modifier: Modifier = Modifier, gap: Dp = 12.dp, tile: @Composable (item: T, index: Int, modifier: Modifier) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(2).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEachIndexed { c, item -> tile(item, r * 2 + c, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Glass card holding [ListRow]s separated by hairlines. */
@Composable
fun ListGroup(modifier: Modifier = Modifier, tone: Tone = Tone.Plain, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), material = tone.material()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

/** Hairline between rows inside a [ListGroup]. */
@Composable
fun RowDivider() = HorizontalDivider(thickness = 1.dp, color = Lotus.Line)

/** List row: badge/leading, title + caption, trailing (chevron by default). */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { ChevronCircle() },
    titleStyle: TextStyle = LotusType.itemTitle,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> { leading(); Spacer(Modifier.width(14.dp)) }
            icon != null -> { IconBadge(icon); Spacer(Modifier.width(14.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = titleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = LotusType.itemSub, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
    }
}

// =================================================================================================
//  Buttons & pills
// =================================================================================================

/** The one primary action on a screen: dark ink pill, label left, arrow right. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = Icons.Outlined.ArrowOutward,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(PillShape)
            .background(if (enabled) Lotus.Ink else Lotus.Ink.copy(alpha = 0.35f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = LotusType.button.copy(color = Color.White), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(trailingIcon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Secondary full-width glass pill. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    color: Color = Lotus.Ink,
) {
    GlassSurface(modifier.fillMaxWidth().heightIn(min = 52.dp), shape = PillShape, material = Glass.Pill, onClick = onClick, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = LotusType.button.copy(color = color), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (trailingIcon != null) {
                Spacer(Modifier.width(10.dp))
                Icon(trailingIcon, null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Compact pill button ("▷ Listen", "+ My words"). [selected] = ink fill. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    color: Color = Lotus.Ink,
) {
    val content = @Composable {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) Color.White else color, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = LotusType.pill.copy(color = if (selected) Color.White else color), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (selected) {
        Box(modifier.defaultMinSize(minHeight = 40.dp).clip(PillShape).background(Lotus.Ink).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) { content() }
    } else {
        GlassSurface(modifier.defaultMinSize(minHeight = 40.dp), shape = PillShape, material = Glass.Pill, onClick = onClick, contentAlignment = Alignment.Center) { content() }
    }
}

// =================================================================================================
//  Inputs
// =================================================================================================

/** Glass search field with a thin magnifier. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp), material = Glass.Field, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = Lotus.InkSoft, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            LotusBasicField(value, onValueChange, placeholder, Modifier.weight(1f), singleLine = true)
        }
    }
}

/** Glass text field (single or multi-line) with placeholder. */
@Composable
fun LotusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle? = null,
) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), material = Glass.Field) {
        LotusBasicField(
            value, onValueChange, placeholder,
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            singleLine = singleLine, minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions, textStyle = textStyle,
        )
    }
}

@Composable
private fun LotusBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle? = null,
) {
    val style = (textStyle ?: MaterialTheme.typography.bodyLarge).copy(color = Lotus.Ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        keyboardOptions = keyboardOptions,
        textStyle = style,
        cursorBrush = SolidColor(Lotus.Accent),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = Lotus.InkSoft.copy(alpha = 0.8f)))
                inner()
            }
        },
    )
}

/** Material switch in the lotus palette (lavender-violet when on). */
@Composable
fun LotusSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Lotus.AccentSoft,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFD9D2E3),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Labelled switch row ("Spoken guidance" / caption / switch). */
@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LotusType.itemTitle)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = LotusType.itemSub)
        }
        Spacer(Modifier.width(12.dp))
        LotusSwitch(checked, onCheckedChange)
    }
}

/** Slider in the lotus palette: thin violet track, white round thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Lotus.AccentSoft,
            inactiveTrackColor = Color.White.copy(alpha = 0.75f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
        thumb = {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Lotus.AccentSoft, CircleShape),
            )
        },
    )
}

// =================================================================================================
//  Breath, player, progress
// =================================================================================================

/**
 * Glowing breathing orb: rose-lavender sphere with soft halo rings, a small label, a big light
 * number and a caption. [scale] animates the sphere (0.75–1.0).
 */
@Composable
fun BreathOrb(
    modifier: Modifier = Modifier,
    diameter: Dp = 250.dp,
    scale: Float = 1f,
    label: String? = null,
    value: String? = null,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .size(diameter + 64.dp)
            .then(if (onClick != null) Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = diameter.toPx() / 2f * scale
            drawCircle(Color.White.copy(alpha = 0.13f), r + 32.dp.toPx(), c)
            drawCircle(Color.White.copy(alpha = 0.20f), r + 16.dp.toPx(), c)
            drawCircle(
                Brush.radialGradient(
                    0f to Color(0xFFFFEDF1), 0.45f to Color(0xFFF3D0E5), 1f to Color(0xFFBCA9DD),
                    center = Offset(c.x - r * 0.3f, c.y - r * 0.5f), radius = r * 1.7f,
                ),
                r, c,
            )
            drawCircle(Color.White.copy(alpha = 0.85f), r, c, style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!label.isNullOrBlank()) Text(label, style = LotusType.small.copy(color = Lotus.Ink), textAlign = TextAlign.Center)
            if (!value.isNullOrBlank()) Text(value, style = LotusType.bigNumber, textAlign = TextAlign.Center)
            if (!caption.isNullOrBlank()) Text(caption, style = LotusType.micro.copy(color = Lotus.Ink.copy(alpha = 0.8f)), textAlign = TextAlign.Center, modifier = Modifier.width(diameter * 0.7f))
        }
    }
}

/** Big dark play / pause button. */
@Composable
fun PlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 64.dp, contentDescription: String? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(Lotus.Ink).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription, tint = Color.White, modifier = Modifier.size(size * 0.42f))
    }
}

/** Player row: optional side buttons around the play button. */
@Composable
fun PlayerRow(
    playing: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    playDescription: String? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        left?.invoke()
        PlayButton(playing, onPlayPause, contentDescription = playDescription)
        right?.invoke()
    }
}

/** Thin progress line. */
@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, color: Color = Lotus.AccentSoft) {
    Box(modifier.fillMaxWidth().height(3.dp).clip(PillShape).background(Color.White.copy(alpha = 0.7f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(3.dp).clip(PillShape).background(color))
    }
}

/** Segmented step progress ("point 2 of 8"). */
@Composable
fun StepBar(count: Int, done: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count.coerceAtLeast(1)) { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(PillShape).background(if (i < done) Color(0xFFAC8DBB) else Color.White.copy(alpha = 0.6f)))
        }
    }
}

// =================================================================================================
//  Choices, stats, hero
// =================================================================================================

/** Round glass choice with a line icon and a tiny label (moods, sounds). */
@Composable
fun ChoiceIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 54.dp) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val shape = CircleShape
        if (selected) {
            Box(
                Modifier.size(size).clip(shape).background(Color(0xFFF6E6ED)).border(1.5.dp, Color(0xFFDBC1D4), shape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, label, tint = Lotus.Ink, modifier = Modifier.size(size * 0.46f)) }
        } else {
            GlassSurface(Modifier.size(size), shape = shape, material = Glass.Pill, contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = Lotus.InkSoft, modifier = Modifier.size(size * 0.46f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = LotusType.micro.copy(color = Lotus.Ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Small glass stat tile: kicker, serif number, caption. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, kicker: String? = null, tone: Tone = Tone.Plain) {
    GlassSurface(modifier, material = tone.material()) {
        Column(Modifier.padding(16.dp)) {
            if (!kicker.isNullOrBlank()) { Kicker(kicker); Spacer(Modifier.height(4.dp)) }
            Text(value, style = LotusType.stat)
            Text(label, style = LotusType.itemSub, maxLines = 2)
        }
    }
}

/** Day chip for week strips (selected = ink). */
@Composable
fun DayChip(top: String, bottom: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content = @Composable {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, style = LotusType.micro.copy(color = if (selected) Color.White.copy(alpha = 0.8f) else Lotus.InkSoft))
            Spacer(Modifier.height(4.dp))
            Text(bottom, style = LotusType.itemTitle.copy(color = if (selected) Color.White else Lotus.Ink))
        }
    }
    val shape = RoundedCornerShape(20.dp)
    if (selected) Box(modifier.clip(shape).background(Lotus.Ink).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
    else GlassSurface(modifier, shape = shape, material = Glass.Pill, onClick = onClick, contentAlignment = Alignment.Center) { content() }
}

/**
 * Large rounded image card (the glass-lotus art by default) with kicker + serif title over the
 * sky, and optional chip / play button along the bottom.
 */
@Composable
fun HeroImageCard(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    image: String = Catalog.SIGNATURE_BG,
    height: Dp = 236.dp,
    imageBias: Float = 0.62f,
    onClick: (() -> Unit)? = null,
    bottom: (@Composable RowScope.() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.9f), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Image(
            painterResource(Sharing.bgRes(image)), null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, imageBias),
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.42f), 0.55f to Color.White.copy(alpha = 0.06f), 1f to Color.Transparent)))
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            if (!kicker.isNullOrBlank()) { Kicker(kicker, color = Lotus.Ink.copy(alpha = 0.72f)); Spacer(Modifier.height(8.dp)) }
            Text(styledTitle(title), style = MaterialTheme.typography.headlineMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            extra?.invoke(this)
            Spacer(Modifier.weight(1f))
            if (bottom != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = bottom)
        }
    }
}

/** Lavender glass strip: "Need a moment of calm? / Let's take one breath together. ↗". */
@Composable
fun CalmStrip(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.Waves) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), material = Glass.Lavender, onClick = onClick) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Lotus.Accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = LotusType.itemTitle)
                Text(subtitle, style = LotusType.itemSub)
            }
            Icon(Icons.Outlined.ArrowOutward, null, tint = Lotus.Ink, modifier = Modifier.size(18.dp))
        }
    }
}

/** Four-point sparkle glint — the only ornament in the app. */
@Composable
fun Sparkle(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = Color(0xFFAA7C89), filled: Boolean = false) {
    Canvas(modifier.size(size)) {
        val c = center
        val r = min(this.size.width, this.size.height) / 2f
        val w = r * 0.22f
        val p = Path().apply {
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0 - 90.0)
                val rad = if (i % 2 == 0) r else w
                val x = c.x + (cos(a) * rad).toFloat()
                val y = c.y + (sin(a) * rad).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        if (filled) drawPath(p, color) else drawPath(p, color, style = Stroke(1.4.dp.toPx()))
    }
}

/** Soft chakra disc with its white line mandala (muted chakra colours). [index] 0 = root. */
@Composable
fun ChakraDot(index: Int, size: Dp = 42.dp, modifier: Modifier = Modifier) {
    val ch = Catalog.chakras.getOrNull(index) ?: Catalog.chakras.last()
    ChakraSymbol(index, ch.color, size, modifier)
}

// =================================================================================================
//  Dialogs & sheets
// =================================================================================================

/** Alert dialog in the lotus style (pale sheet, serif title, ink/accent actions). */
@Composable
fun LotusDialog(
    onDismiss: () -> Unit,
    title: String?,
    confirm: @Composable () -> Unit,
    dismiss: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirm,
        dismissButton = dismiss,
        title = title?.let { { Text(styledTitle(it), style = LotusType.cardTitle) } },
        text = text,
        shape = RoundedCornerShape(30.dp),
        containerColor = Lotus.Sheet,
        titleContentColor = Lotus.Ink,
        textContentColor = Lotus.InkSoft,
    )
}

/** Bottom sheet in the lotus style (pale, rounded, small handle). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotusSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Lotus.Sheet,
        scrimColor = Color(0x3845394A),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(width = 40.dp, height = 4.dp).clip(PillShape).background(Color(0xFFB9AFC3))) },
    ) {
        Column(Modifier.fillMaxWidth().padding(PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp)), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Remembered interaction source helper (rarely needed by screens). */
@Composable
fun rememberInteraction() = remember { MutableInteractionSource() }
