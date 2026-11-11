# Build progress notes (for continuing work)

Project: /home/claude/InnerLotus  (Kotlin + Jetpack Compose, package com.sjpixels.innerlotus)
Versions: AGP 8.13.0, Kotlin 2.2.20, Compose BOM 2025.12.00, Gradle wrapper 8.14.3, compile/target SDK 36, minSdk 26

## Content pipeline
- Source: content/src/<lang>/*.txt  (format: `key = value`, `key:` + `- item` lists, `key <<` ... `>>` blocks, `#` comments)
- Build: python3 tools/build_content.py  -> app/src/main/assets/i18n/<lang>.json (flat map key -> string | list)
- Languages: en hi ru ko zh es fr it ja. English is master; validator checks keys, list lengths, {placeholders}, {m|f} gender markup.
- Gendered grammar: write `{masc|fem}` in hi/ru/es/fr/it; app resolves by Settings > form of address.
- Affirmations: area.<id>.lines (24) x chakra.<id>.seeds (6) x 7 chakras = 1008 per area.

## Area ids (33)
wealth health love relationships family career business selflove confidence mental spirituality success
creativity growth gratitude joy sleep beauty forgiveness productivity home purpose parenting friendship
travel fertility eating recovery communication leadership studies aging luck

## Chakra ids: root sacral solar heart throat thirdeye crown
## EFT topics (12): anxiety stress anger sadness fear selfworth money sleep cravings procrastination pain hurt
## EFT points: kc th eb se ue un ch cb ua gp ; techniques: basic full gentle positive
## Ho'oponopono topics (15): self partner parents children friend work money health body home past ancestors earth anxiety person
## Sounds: ids in audio/dsp/Mixer.kt (sound.freq.* / sound.lib.* keys in practices.txt)
## Breath: box 478 coherent sigh nadi bhramari calm

## Status
- [x] Scaffold, fonts, backgrounds (res/drawable-nodpi/bg_*.webp), icon
- [x] English content (content/src/en/*.txt, 756 keys)
- [x] Kotlin code (all screens) — syntax-checked; pure modules tested via tools/jvmtest/run.sh
- [x] English UI strings
- [x] Translations: all 9 languages validated (756 keys / 1802 strings each)
- [x] Verification (105 JVM tests incl. 33x1008 uniqueness per language), docs (README, PRIVACY_POLICY, docs/PLAY_STORE.md), store graphics
- [ ] First Android Studio sync + device testing (not possible in the authoring sandbox)

## Translation files per language (same names as en): areas.txt chakras.txt learn.txt eft.txt practices.txt ui.txt
