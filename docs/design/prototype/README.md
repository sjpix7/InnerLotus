# Inner Lotus — complete screen redesign

Open `index.html` in a browser. No installation or internet connection is required for the design collection. The local preview is also available at http://localhost:8765 while the preview server is running.

## What is included

35 redesigned screen states corresponding to the three original design sheets, a shared visual system, and an interactive presentation mode. The originals are preserved. This folder contains a design prototype, not an integrated Android or iOS application: the source folder provided only PNG design sheets.

Use the category filters to review related screens. Choose “Explore” under a screen or “Try the prototype” to navigate the designs. The screen selector and arrow keys expose all 35 states. Long screens scroll inside the phone frame.

Prototype interactions include navigation, selected chips, mood selection, intensity slider, habit completion, adding a sample ritual, editable writing fields, favourite state, play/pause presentation, theme selection and browser text-to-speech. Sample journal entries and analytics are illustrative. Saved changes are not persisted between screens/reloads. Audio tracks, secure PIN storage, notifications, translations, import/export, and complete guided-session sequencing require integration into the actual application.

## Design direction

The five reference images informed the pink/lavender atmospheric backgrounds, mint and peach supporting colours, rounded translucent cards, restrained navigation, large breathing circle, editorial serif headings and spacious composition. Repeated large explainer panels are simplified; daily practices are grouped under a stable Home / Practice / Journal / Growth navigation. Calm now remains a direct route from Home.

The existing lotus logo is preserved in About. A new lotus illustration adds a consistent hero visual. Typography uses local Georgia and Arial/Helvetica fallbacks so the design opens offline.

## Shared components and tokens

- Ink: #302C43. Muted text: #746D83. Accent: #7760A9.
- Blush: #F6D4E6. Lavender: #E6DFF7. Sage: #DEEBE3. Cream: #FCF8F5.
- Card radius: 24px. Pill controls: 30px. Frame: 365 × 790px.
- Screen headings: Georgia 30px. Section headings: Georgia 20px. Body: Arial 12–13px.
- Components: app bars, tab navigation, practice tiles, translucent cards, segmented controls, mood selectors, chakra rows, sliders, switches, breathing orb, player controls, bottom sheet, journal fields, progress charts.
- Focus indicators and reduced-motion support are included. Before production, increase small secondary labels and interactive hit areas to native accessibility standards, verify contrast on all themes, and implement Dynamic Type/font scaling.

## Screen inventory

01 Welcome & language
02 Home / your sanctuary
03 Explore practices (replaces the scrolled home state)
04 Practice menu (replaces the narrow sidebar)
05 Affirmation life areas
06 More life areas
07 Chakra selection
08 Affirmation player
09 Personal affirmations editor
10 Import & export
11 EFT techniques
12 EFT topics
13 Before-session intensity
14 Guided tapping
15 Sound frequencies
16 Nature soundscapes
17 Sound mixes
18 Forgiveness intention
19 Forgiveness reflection
20 Journal overview
21 Gratitude editor
22 Daily rituals
23 Add ritual sheet
24 Growth analytics
25 Breathwork library
26 Breathing session
27 Chakra guide
28 Expanded chakra detail (replaces the scrolled guide state)
29 Calm now breathing
30 Grounding & support
31 Language & voice
32 Appearance
33 Privacy & reminders
34 About
35 Learning article

## Artwork

Generated using the built-in image generation tool. Saved at `assets/lotus-sanctuary.png`.

Final prompt:
“Create a premium editorial background illustration for Inner Lotus, a healing and mindfulness mobile app. Portrait 3:4 composition. A single exquisite translucent pale blush pink lotus blossom floating on mirror still lilac water in lower center, pearl dew, soft sculptural petals, distant hazy mauve mountains, tiny warm sun, luminous peach pink sky fading into pale lavender. Dreamy refined 3D photographic rendering, delicate glasslike petals, gentle film grain, serene ambient light, restrained pastel palette, no saturated colors. Upper 45 percent mostly empty pale sky for dark UI typography overlay. No text, no letters, no border, no phone mockup, no watermark.”
