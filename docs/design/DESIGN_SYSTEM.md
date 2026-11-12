# Inner Lotus — design system ("glass lotus at dawn")

The UI follows the reference prototype in `docs/design/prototype/` (open `index.html`; rendered
screens in `screens_1-3.jpg`) and the signature image `app/src/main/res/drawable-nodpi/bg_dawn.webp`
(a translucent glass lotus on still water at dawn). Goal: **subtle, calm, elegant, self-explanatory**.

## Principles
1. **Quiet surfaces.** Frosted glass over the pastel background; hairline light edges; almost no shadow.
2. **One voice per screen.** Small letter-spaced kicker → large serif headline (one *italic* word) →
   muted subtitle. Then content. One dark-ink primary action at most.
3. **Line icons only.** Thin Material *Outlined* icons, never emoji (language flags in the language
   list are the one exception). Every icon that acts alone has a translated content description.
4. **Readable.** Body ≥ 15sp, captions ≥ 12sp, ink on glass ≥ 4.5:1, touch targets ≥ 44dp.
5. **Soft colour.** Chakras keep their rainbow order in muted tones; accents are lavender-violet.

## Tokens (`ui/theme/Theme.kt` → `Lotus`)
| Token | Hex | Use |
|---|---|---|
| Ink | #302C43 | headings, body, primary pill, selected chips |
| InkSoft | #574F6A | captions, subtitles, inactive icons |
| Accent | #7760A9 | links, icons in tiles, focus |
| AccentSoft | #9278B7 | switches, sliders, progress |
| Blush / Lavender / Sage / Peach / Cream | #F6D4E6 / #E6DFF7 / #DEEBE3 / #FAE3D7 / #FCF8F5 | tints |
| BadgeBg / BadgeInk | #E9E1F4 / #7D619C | icon badges |
| Sheet | #F8F3FA | dialogs, sheets, menus |
| Rose / SageInk / Gold / Danger | #C686AB / #5E8A6B / #C9A55A / #B14F63 | favourite / done / streak / destructive |
| Line | #61497B @ 13% | dividers |
Chakra colours (`Catalog.chakras`): root #C68891, sacral #D59E7E, solar #D2B36F, heart #8FAE97,
throat #88B6C4, third eye #8990C3, crown #AB8DBB (each with a deeper `ink` for text).

## Type (`MaterialTheme.typography` + `LotusType`)
Cormorant Garamond (display, serif, has *italic*) + Jost (UI). Hindi/CJK fall back to the system font.
| Role | Style |
|---|---|
| Welcome headline | `typography.displaySmall` — Cormorant 500, 38sp |
| Screen headline | `typography.headlineLarge` — Cormorant 500, 34sp (use `ScreenHeader`) |
| Hero / card big title | `typography.headlineMedium` — Cormorant 500, 30sp |
| Section heading | `LotusType.section` — Cormorant 600, 23sp |
| Card title (serif) | `LotusType.cardTitle` — Cormorant 600, 22sp |
| Top-bar title | `LotusType.barTitle` — Cormorant 600, 21sp |
| Quote / prayer | `LotusType.quote` — Cormorant 500, 25sp |
| Big number / stat | `LotusType.bigNumber` 68sp / `LotusType.stat` 34sp (Cormorant) |
| Row / tile title | `LotusType.itemTitle` — Jost 500, 15sp |
| Row caption | `LotusType.itemSub` — Jost 400, 12.5sp, InkSoft |
| Body | `typography.bodyLarge` 16sp / `bodyMedium` 15sp — Jost |
| Small muted | `LotusType.small` — Jost 14sp, InkSoft |
| Kicker | `Kicker(text)` — Jost 500, 11sp, caps, +0.16em |
| Footnote | `LotusType.micro` / `QuietNote` — Jost 12sp |
| Buttons / pills | `LotusType.button` 15sp / `LotusType.pill` 13.5sp |
Never use `FontWeight.Bold` or raw `fontSize` for headings; pick a role above.

## Glass (`ui/glass/Glass.kt`, materials in `ui/components/Design.kt` → `Glass`)
Each surface draws the blurred slice of the background behind it + tint + hairline edge.
`Glass.Card` (white 66→52%), `Strong` (84→76%), `Pill` (70→60%), `Segments` (46→38%),
`Field` (72→64%), `Ink` (dark, mini player), tints `Lavender`, `Sage`, `Peach`, `Blush`.
`Tone` picks a material for cards/tiles; `toneFor(i)` cycles lavender → sage → peach → blush.

## Components (`ui/components/Design.kt`, `Widgets.kt`, `Scaffold.kt`)
- `AppScaffold(title, showBack, showLogo, actions)` — round glass back, centred serif title, language
  chip ("EN"), settings; quiet developer line above. `PageColumn(padding) {}` — 20dp margins, 14dp gaps.
- `ScreenHeader(title, kicker, subtitle, center, large)` — editorial header; `*word*` = italic.
- `SectionHeading(title, action, onAction)`, `SectionTitle(text, icon)`, `Kicker`, `TextLink`, `QuietNote`.
- `GlassCard(tone, strong, padding, onClick) {}`, `GlassSurface(shape, material, onClick) {}`.
- `PracticeTile(icon, title, subtitle, footnote, tone, selected, onClick)` + `TileGrid(items) { item, i, m -> }`.
- `ListGroup {}` + `ListRow(title, subtitle, icon|leading, trailing, onClick)` + `RowDivider()`; `IconBadge(icon)`; `ChevronCircle()`.
- `PrimaryButton(text, trailingIcon = ArrowOutward)` — dark ink pill (one per screen).
  `SecondaryButton(text)` — glass pill. `PillButton(text, icon, selected)` — compact. `Pill(text, selected)` — chip.
  `CircleIconButton(icon, desc)` — round glass. `Segmented(options, selected)` — glass track + ink pill.
- `SearchField`, `LotusTextField(minLines…)`, `LotusSwitch`, `SwitchRow(title, subtitle)`, `LotusSlider`, `Stepper`.
- Sessions: `BreathOrb(label, value, caption, scale)`, `PlayButton`, `PlayerRow(left, right)`, `ProgressLine`, `StepBar`.
- `ChoiceIcon(icon, label, selected)` (moods/sounds), `StatTile(value, label, kicker)`, `DayChip`.
- `HeroImageCard(title, kicker, image = "dawn", bottom = {…})` — the glass-lotus art card. `CalmStrip`.
- `Sparkle()` — 4-point glint, the only ornament. `ChakraDot(index)` / `ChakraSymbol` — soft chakra disc.
- `LotusDialog`, `ConfirmDialog`, `LotusSheet {}` (bottom sheet), `ListenButton`, `LearnCard(topic)`.
Avoid raw Material `Button`, `FilledTonalButton`, `OutlinedButton`, `Card`, `TextField`,
`OutlinedTextField`, `Slider`, `Switch`, `RadioButton`, `Checkbox`, chips, `Surface` — use the above.

## Icons (`ui/icons/LotusIcons.kt`)
`section(key)`, `area(id)` (33 life areas), `eftTopic(id)`, `mood(id)` (weather metaphor: sun, lotus,
cloud, umbrella, waves, storm), `sound(id)`, `frequency(binaural)`, `breath(id)`, `habit(storedKey)`
(maps legacy emoji too), `learn(topic)`. Other icons: `Icons.Outlined.*` / `Icons.AutoMirrored.Outlined.*`.

## Copy
Editorial headers live in `content/src/<lang>/ui.txt` under `ui.hero.*` (kicker / title / sub),
plus `ui.home.calm_*`, `ui.home.practices`, `ui.onboard.*`, `ui.common.explore_guide`,
`ui.common.moment_to_understand`, `ui.quiet.*`. Titles may contain `*italic*` markup — render with
`ScreenHeader` / `styledTitle()`.
