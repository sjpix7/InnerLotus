# Publishing Inner Lotus on Google Play

This guide covers everything Play Console asks for, in order. Items marked **☐** are actions for you.

---

## 1. Before you upload

- ☐ **Build:** `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`, signed with your upload key (see README).
- ☐ **Test on real phones** in at least 2–3 languages. Do this on Android 8, 12–13 and 15–16 if possible. Check:
  - TTS read-aloud (install voices if the app prompts)
  - sound playing with the screen off and the Stop button in the notification
  - the Usage-access flow, reminders after a reboot, and share/save of a card image
  - export → uninstall → reinstall → import
- **Target API:** `targetSdk 36` is already set. Since 31 Aug 2026, new apps and updates must target API 36.
- ☐ **Privacy policy URL:** upload `docs/privacy-policy.html` to your site (e.g. `https://sjpixels.com/innerlotus/privacy`) and use that URL.
- ☐ **Contact email:** Play requires a public developer email. Add it to your developer profile and, if you like, to the privacy policy's Contact section.

---

## 2. Main store listing

| Field | Value |
|---|---|
| App name | see the table in §3 (30 characters max) |
| Category | **Health & Fitness** |
| Tags (pick up to 5) | Meditation, Mental wellness, Sleep, Habit tracker, Journal |
| Icon | `docs/store/icon-512.png` (512×512, 32-bit PNG) |
| Feature graphic | `docs/store/feature-graphic-1024x500.png` |
| Phone screenshots | ☐ 4–8 screenshots, 1080×1920 or 1080×2400, from a real device or emulator |
| Website | https://sjpixels.com/ |

**Suggested screenshots and captions:**

1. Home: "Heal, breathe and bloom"
2. Chakra picker: "Choose a chakra to heal"
3. Affirmation card: "33 life areas · 1008 affirmations each"
4. EFT session: "Guided EFT tapping, step by step"
5. Sound mixer: "Mix healing sounds, play all night"
6. Journal: "A private journal that never leaves your phone"
7. Habits + analytics: "Build gentle habits"
8. Language menu: "9 languages"

### Full description (English, ~2,600 characters)

```
Inner Lotus is a calm, private companion for everyday healing — affirmations, EFT tapping, sound healing, Ho'oponopono, journaling, habits and breathwork in one beautiful app.

Everything works offline. No account, no ads, no tracking. What you write stays on your phone.

✿ AFFIRMATIONS FOR 33 AREAS OF LIFE
Wealth, health, love, family, career, self-love, confidence, sleep, forgiveness, purpose and many more. Each area has 1008 unique affirmations, built around the seven chakras — from Root ("I am safe, centred and grounded") to Crown ("I am divine").
• Swipe through beautiful cards
• Choose a chakra to focus on, or receive all seven
• Listen with your phone's own voice: tap to play, auto-play, repeat or count
• Save favourites, copy, share or download a card as an image
• Add your own affirmations, and import or export them

✿ EFT TAPPING
Guided sessions for anxiety, stress, anger, sadness, fear, money worries, sleep, cravings, procrastination, pain and more. Rate your feeling, follow each tapping point with spoken guidance, and see how much calmer you feel.

✿ SOUND HEALING
Solfeggio and 432 Hz tones, OM, Schumann and binaural beats, rain, ocean, forest stream, birdsong, bamboo flute, temple bell, singing bowls, tanpura and more. Layer any sounds into your own mix, save it, and let it play continuously with a sleep timer.

✿ HO'OPONOPONO
Create a personal prayer of forgiveness for yourself, a partner, family, work, money, health or anyone you name. Edit every word and let it play on a gentle loop.

✿ PRIVATE JOURNAL
Free writing, gratitude and 30 reflective prompts. Track your mood, browse by day, week or month, search, lock with a PIN, and export or import whenever you like.

✿ HABIT TRACKER
Simple daily habits with streaks, a 12-week heatmap and weekly insights — plus optional screen-time insights from Android's app usage.

✿ BREATHWORK & CALM NOW
Box breathing, 4-7-8, coherent breathing, cyclic sighing, alternate-nostril and bee breath. A one-tap "Calm now" space with grounding and soothing sounds.

✿ LEARN
Short, research-informed guides to affirmations, EFT, sound, journaling, breath and the chakras — each one can be read aloud.

✿ MADE FOR YOU
• 9 languages: English, हिन्दी, Русский, 한국어, 中文, Español, Français, Italiano, 日本語
• Masculine or feminine form of address in languages that need it
• Rotating nature backgrounds or a fixed favourite
• Daily reminders

For mantra chanting with a 108-bead counter, try our companion app Mantra Mala.

Inner Lotus is a wellness and self-care app. It does not provide medical advice and is not a substitute for professional care.

Developed by sjpixels — https://sjpixels.com/
```

> Keep claims modest. Play's policies forbid misleading health claims, so phrases like "cures", "treats" or "heals disease" must not appear. The text above uses wellness language only.

---

## 3. Localized listings (title ≤ 30, short description ≤ 80, all checked)

Add each language under **Store presence → Main store listing → Manage translations**. For the full description, either translate the English text above or use Play Console's built-in translation, then have a native speaker review it.

| Lang | App name | Short description |
|---|---|---|
| en-US | Inner Lotus: Affirmations, EFT | Affirmations, EFT tapping, sound healing & journal. Offline and private. |
| hi-IN | Inner Lotus: प्रतिज्ञान और EFT | प्रतिज्ञान, EFT टैपिंग, ध्वनि उपचार और डायरी। पूरी तरह ऑफ़लाइन और निजी। |
| ru-RU | Inner Lotus: аффирмации, EFT | Аффирмации, EFT-таппинг, звукотерапия и дневник. Работает офлайн, всё приватно. |
| ko-KR | Inner Lotus: 확언·EFT·사운드 힐링 | 확언, EFT 태핑, 사운드 힐링, 일기. 오프라인 작동, 데이터는 내 휴대폰에만. |
| zh-CN | Inner Lotus：肯定语与身心疗愈 | 肯定语、EFT 敲击、声音疗愈与日记。完全离线，数据只留在你的手机上。 |
| es-ES | Inner Lotus: afirmaciones, EFT | Afirmaciones, tapping EFT, sonidos sanadores y diario. Sin conexión y privado. |
| fr-FR | Inner Lotus : affirmations EFT | Affirmations, tapping EFT, sons apaisants et journal. Hors ligne et privé. |
| it-IT | Inner Lotus: affermazioni, EFT | Affermazioni, tapping EFT, suoni di guarigione e diario. Offline e privato. |
| ja-JP | Inner Lotus：アファメーション | アファメーション、EFTタッピング、サウンドヒーリング、日記。オフラインで安心。 |

---

## 4. App content (Policy → App content)

### Privacy policy
Paste the hosted URL of `docs/privacy-policy.html`.

### Ads
**No**, the app does not contain ads.

### App access
**All functionality is available without special access.** There is no login. The optional journal PIN is set by the user.

### Content rating (IARC questionnaire)
- Category: **Reference, News, or Educational** (or "All other app types").
- Violence, sexuality, profanity, drugs, gambling, user-generated content shared with others: **No**.
- Users can interact or exchange content: **No**. Sharing a card uses Android's share sheet and there is no in-app communication.
- The app shows crisis-helpline information. That is supportive content, not depiction of self-harm.
- Expected result: **Everyone / PEGI 3 / USK 0**.

### Target audience
Choose **13–15, 16–17 and 18+**, or 18+ only. Don't select under-13 age groups; the app isn't designed for children. That keeps it out of the Families policy requirements.

### Data safety
Data that is processed only on the device and never sent off it does **not** need to be declared.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (nothing is transmitted) |
| Do you provide a way for users to request that their data is deleted? | Data is never collected. Users can delete everything in-app (Settings → Delete all my data) or by uninstalling. |

Resulting badge: **"No data collected · No data shared with third parties."**

### Health apps declaration
Every app on Play must complete this form. Select:
- **Health and fitness → Stress Management, Relaxation, Mental Acuity** (affirmations, EFT, breathwork, meditation-style sound)
- **Health and fitness → Sleep Management** (sleep sounds, sleep timer, sleep affirmations)

Do **not** select Medical categories. The app makes no diagnostic or treatment claims and shows a wellness disclaimer in-app and in the listing.

### Foreground service permissions
The app targets Android 14+ and uses a foreground service of type **`mediaPlayback`**, so Play requires a declaration:

- Type: **Media playback**
- Description: *"Plays healing sounds and frequencies that the user starts in the Sound Healing section, so they keep playing in the background or with the screen off (for example for sleep). A persistent notification shows what is playing and has a Stop button. Playback never starts automatically."*
- ☐ **Video link** (unlisted YouTube or Drive is fine). Record ~20–30 seconds: open Sound Healing → tap Rain + Singing bowl → press Home or lock the screen → show the notification still playing → tap Stop.

### Other permissions to be ready to explain
- **Usage access (`PACKAGE_USAGE_STATS`)**: no Console form exists. The app shows a prominent in-app disclosure before sending the user to settings, and the data never leaves the device. If a reviewer asks, describe it as *"optional screen-time insights inside the Habit Tracker, read on-device only."*
- **`<queries>` for launcher apps and TTS engines**: these are narrow intent queries and do not need the QUERY_ALL_PACKAGES declaration.

### Government apps, Financial features, News, COVID-19
**No / not applicable.**

---

## 5. Release

1. ☐ **Testing → Internal testing**: upload the AAB and add yourself and a few testers. Check the pre-launch report for crashes and accessibility warnings.
2. ☐ **Closed testing** *(required for new personal developer accounts)*: run with at least **12 testers for 14 consecutive days** before you can apply for production access.
3. ☐ **Production**: roll out at 20% first, watch Android vitals (ANRs and crashes), then go to 100%.
4. ☐ For each update, raise `versionCode`, add release notes in all 9 languages and keep `targetSdk` current.

---

## 6. Post-launch ideas

- Ask for a review after the 5th completed session with the Play In-App Review API. This adds a Google Play library dependency, so re-check the Data safety answers if you add it.
- Add home-screen widgets (affirmation of the day, quick "Calm now").
- Add Wear OS breathing and a tile.
- Add more affirmation lines per area: the composer scales automatically if you keep `LINES` in sync.
