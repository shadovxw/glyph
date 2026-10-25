# Glyph — Design Spec

**Visual reference (canvas):** https://claude.ai/artifact/CMCQHVKnUxp2z65xsp8yW3
The canvas is private to Vrishank. If Claude Code can't open it, this file is the full source of truth; every value below was taken from the canvas.

Glyph is a personal Android launcher for a **OnePlus 12R (OxygenOS)**. It has five swipeable home screens of widgets, a stock-style app drawer, a custom page transition, a live wallpaper and a few Quick Settings tiles.

**Two backgrounds:**
- **Home screen → Orbit and dots** (§5A), drawn by the launcher itself behind the widgets.
- **Lock screen → Bloom and wind** (§5B), the system live wallpaper. The opaque launcher hides it on the home screen, so in practice it only shows on the lock screen.

---

## 1. Principles

- **Black and white only.** The exceptions are the single red dot (`#D7262E`) in the live wallpaper, and the **Battery card's progress bar, which turns the same red below 20%**.
- **Sharp corners.** Cards, buttons, chips, avatars and tiles have no border radius. Exceptions: the phone frame, round status dots, and the app drawer, which copies OxygenOS exactly (section 6).
- **No borders, no shadows.** Cards are separated by spacing, not outlines.
- **Transparent cards by default.** Widget content sits directly on the live wallpaper (section 2.3).
- **Light and dark modes**, switched from the home screen. Dark mode uses true black (`#000000`) to save battery on AMOLED.
- **Touch targets at least 44 × 44 dp.**

---

## 2. Design tokens

All sizes are in **dp** (the canvas was drawn at 390 × 844, which maps 1:1 to dp on the 12R).

### 2.1 Colours

| Token | Light | Dark |
|---|---|---|
| `screen.bg` | `#F6F6F6` | `#000000` |
| `ink` (primary text, icons) | `#000000` | `#FFFFFF` |
| `ink.dim` (secondary text) | ink @ 62% | ink @ 62% |
| `ink.line` (dividers) | ink @ 8–10% | ink @ 8–10% |
| `ink.track` (progress background) | ink @ 14% | ink @ 14% |
| `chip` (filled square: avatars, date, badges, active button) | `#000000` bg, `#FFFFFF` text | `#FFFFFF` bg, `#000000` text |
| `accent.red` | `#D7262E` | `#D7262E` |

Dark mode with transparent cards: all card content switches to white ink and the chips invert.

### 2.2 Typography — very small type throughout

Fonts: **Geist** and **Geist Mono** (SIL Open Font License; bundle the `.ttf` files in `res/font`).
The whole UI uses deliberately small type; only the clock and key numbers are large.

| Role | Font | Size (sp) | Weight |
|---|---|---|---|
| Home clock | Geist | 56 | 200, letter-spacing −0.045em |
| Big stat (temperature, mail count) | Geist | 27–29 | 200 |
| Medium stat (open PRs, AQI) | Geist | 21–25 | 200 |
| Mono stat (battery %, alarm, charge W, city times) | Geist Mono | 17–19 | 300 |
| Screen title (WhatsApp, GitHub), song title | Geist | 13–14 | 400 |
| Primary row text, event title | Geist | 10.5–11.5 | 400 |
| Secondary row text, previews | Geist | 9–10 | 400, `ink.dim` |
| Card label | Geist | 9 | 400, `ink.dim` |
| Meta (times, repo names, offsets, badges) | Geist Mono | 8.5 | 400, `ink.dim` |
| Smallest (date chip month) | Geist | 7.5 | 400 |

Rule of thumb: when converting from the canvas, scale any older value by about ×0.67, with a minimum of 7.5 sp.
Respect the system font-size setting (use `sp`), but design at these sizes.
Font sizes quoted in the screen descriptions (section 3) are the older canvas values; use this table instead.

Labels use sentence case (setting option: lowercase).

### 2.3 Card styles

Setting `cardStyle`, default **transparent**:
- **transparent:** no background.
- **frosted:** white @ 55% (light) or 78% (dark) with blur. Real blur is only available inside the launcher on Android 12+.
- **solid:** `#FFFFFF`.

### 2.4 Spacing and layout

- Screen padding: **64** top, **20** sides, **22** bottom.
- Gap between cards: **12**.
- Card padding: **16** (14 vertical × 16 horizontal for compact cards).
- Progress bars: **3** tall, square ends.
- List row heights: chats **40**, PRs **52**, emails **48**, devices **40**.
- Dock: 2 plain icons (no background), centred, each a **52 × 52** hit area with a **22** icon: **Phone** and **Camera**. There is no app-drawer button; the drawer opens by swiping up (section 6).
- Transparent cards (the default) have **no inner padding**, so their content lines up with the 20 dp screen margin. The 16 dp card padding only applies to frosted and solid cards.
- Page indicator: see 2.6 (it replaces the old page dots).

### 2.5 Icons

Line icons: 1.5 stroke, round caps and joins, 24 viewBox, no fill, no tiles behind them. Never use emoji.

### 2.6 Page indicator

Centred above the dock, 22 dp from the bottom of the home area.

- **Track:** 120 dp wide, 2 dp tall, ink @ 14%, square ends.
- **Segment:** width = track ÷ page count (5 pages → 24 dp), 2 dp tall, solid ink.
  - Position = (current page + swipe offset) × segment width, so it follows the finger live while dragging.
  - On release it glides to the page in 480 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)`.
- **Label row** above the track (10 dp gap), Geist Mono, letter-spacing 0.04em:
  - **page number** `02` at 9.5 sp
  - directly followed by the **total** `/05` at 6.5 sp, ink @ 50%, 2 dp left margin, same baseline
  - then a 7 dp gap and the **page name** (`Music`, `WhatsApp`, …) at 9.5 sp.
- **Visibility:**
  - The label row (number, total and name) is **hidden at rest**. It appears when a swipe starts (fade in plus 4 dp upward slide, 260 ms) and shows the page you'll land on.
  - It fades out **1.1 s after the swipe ends**.
  - The page name also expands from zero width (320 ms) as it appears.
  - The track sits at 55% opacity at rest and 100% while swiping.
- Page names: Today, Music, WhatsApp, GitHub, Apps.
- **It is also a control.** The touch area is the label row plus the track with 20 dp / 10 dp of room around it.
  - **Press and drag sideways:** the pages move with the finger (one segment, 24 dp, of finger travel = one page, so the whole track is all five pages), the label shows while dragging, and on release it glides to the nearest page (480 ms, same easing).
  - **Tap: the page switcher.** A half circle (radius 170 dp, flat edge on the track, screen-bg fill with a 1 dp ink-line arc, opens over 260 ms from 70% scale) with the five page names on an arc, 72 × 32 dp each, current page filled. Tap a name to glide straight to that page; tap outside, Back or Home closes it. The page behind is dimmed a little.

---

## 3. Home: five screens

The page order is fixed. The dock and page indicator are on every screen.

### Screen 1 — Today
1. **Clock block:** day name (primary row text), time (56 / 200), "25 Oct · 18° Clear" (secondary row text, dim). Optional setting: draw the second hour digit in red.
2. **Mode switch** (right of the clock, bottom-aligned): three stacked 44 × 44 hit areas: **Auto** (a split circle: follow the phone's dark / light setting, **the default**), **Dark** (moon) and **Light** (sun). Moon and sun are manual overrides; Auto switches Glyph, the app drawer and the lock-screen wallpaper whenever the phone's setting changes. **Both have a transparent background** (no fill, no border). The active mode is shown by the icon alone: full `ink` for the active one, `ink.dim` for the inactive one. It changes all screens and the app drawer at once.
   **Animation switch** (built, immediately left of the mode switch): the same two stacked, transparent 44 × 44 tiles, **Play** (animation running) over **Pause** (stopped), the active one in full ink. It is one setting that freezes the Orbit background on the home screen and stops the rain and dust on the lock-screen wallpaper (saves battery); it is remembered.
3. **Search bar:** 48 tall, magnifier icon, placeholder "Search", then three line-icon buttons on the right, in this order: **voice** (microphone), **Lens** (camera search) and **incognito** (hat and glasses). Each is a 44 × 44 hit area. Voice opens Google voice search, Lens opens the Google Lens app (`com.google.ar.lens`), incognito opens a new incognito tab in Chrome (action `org.chromium.chrome.browser.incognito.OPEN_PRIVATE_TAB`). All three verified on the 12R.
4. **Row** (156 tall): **Weather** (label, 44 temp, condition and high/low, 4 hourly columns) and a stack of **Battery** (78%, progress bar) and **Screen time** (2h 14m, "−18% vs. avg").
5. **Next event:** 48 × 48 date chip (OCT / 25), "Next · in 49 min", title, time range.
6. **Row** (120): **Air quality** (AQI number, category, scale bar with a marker) and **Sun** (dotted arc, sun position, sunrise and sunset times).
7. **Steps:** count, "of 8,000", bar.
8. **App shortcuts:** plain line icons (24) with labels (10), 5 columns, in this order: Instagram, Chess.com, WhatsApp, GPay, Maps, Teams, Outlook, Gmail, Authenticator. They open the real apps.

### Screen 2 — YouTube Music
1. **Player card:** header ("YouTube Music" and a "Playing" dot); album art **320 tall** (it was 180), full width, desaturated to greyscale; title (20), "Artist · Album" (13, dim); dislike and like buttons; progress bar with elapsed and total time; controls: shuffle, previous, **pause/play (64 × 64 filled square)**, next, repeat.
2. **Playing on card:** connected audio devices, each a 40 row with icon, name, detail and status, plus a square marker (filled = active).
   - Earbuds: "L 80% · R 75% · case 40%", Playing
   - Headphones: "Last used 2 days ago · 55%", Off
   - Bluetooth speaker: "In range · tap to switch", Ready
   - A volume row at the bottom (speaker icon, bar, value). **It is live:** tap or drag the bar to set the media volume; it follows the hardware keys and re-reads when the output changes (each output keeps its own volume). The system caps headphone volume at its hearing-safety limit and shows its own "Volume alert".
3. **Behaviour (built):**
   - Tapping anywhere on the player, except its buttons, opens YouTube Music (on the playing song when the app offers a screen for it).
   - The device list shows **three rows at a time**; any more scroll inside the list (a "N more" line says so). Tapping a row opens the **Bluetooth settings** page. The phone's own speaker is always the last row. *Open question: whether tapping a row should instead switch the output from here; Android only allows that through its own output switcher.*
   - Data comes from the music app's media session (needs **Notification access**) and from `AudioManager` plus the paired devices (needs the Bluetooth permission for paired devices that are switched off).

### Screen 3 — WhatsApp (full screen, compact)
- Header: "WhatsApp" (18), "36 unread · 10 chats" (mono 11), a camera button, and a **New chat** button (44, filled).
- Filters: **All** (filled), Unread, Groups (transparent). Height 32.
- **13 chat rows at 40 tall:** 26 × 26 square avatar with initial, name (12), last message (11, dim), time (10, mono), unread badge (18 tall, filled; hidden when 0). Thin divider between rows.
- **Tapping anywhere on the page opens WhatsApp itself, not a particular chat.** The filters and the "new chat" button take their own taps.
- **Where the chats come from (built):** Android gives apps no access to WhatsApp's chat list. Glyph reads WhatsApp's message notifications (Notification access) and keeps, on the phone, each chat's name, last message, time and unread count (up to 60 chats, newest first). A chat appears when a message arrives and shows as read once its notification is gone (opened, or read on another device). So the list holds chats that have notified Glyph, not every chat you have; muted chats do not notify, so they do not appear. Unread totals in the header are the sum over those chats.
- The list scrolls; the header and filters stay put.
- **WhatsApp's own widget (built):** WhatsApp ships a chats widget (`com.whatsapp.appwidget.WidgetProvider`) that shows all chats. Glyph is a launcher, so it can host it. With the widget added, **the widget is the page** (below a small "WhatsApp / Remove widget" row), so every chat and its unread count come straight from WhatsApp. The page offers "Show all chats: add WhatsApp's own widget" until it is added; adding asks the user once (the system "allow Glyph to create widgets" screen, then the widget's own set-up screen if any). It draws in WhatsApp's own style and follows the phone's theme, and a tap on a row opens that chat (WhatsApp's behaviour), which overrides the "tap anywhere opens WhatsApp" rule while the widget is shown. The notification-based list above is the fallback when no widget is added.

The Mail, notes and to-dos page was **removed**. Its app shortcuts moved to Today, and its commit grid moved to GitHub.

### Screen 4 — GitHub (multiple accounts)
1. **Header card:** "GitHub" with a branch icon, "synced 2 min ago" (mono), **account chips** All / [personal] / [work] (32 tall, active chip filled); three stats: **Open PRs**, **Need your review**, **Failing checks** (medium stat). Chips filter everything below.
2. **Pull requests:** rows 52 tall: account tag square (P/W), repo (mono), title (primary row text), state (secondary, dim: Review requested / Changes requested / Approved / Draft / Open), and a CI icon (check = passing, × = failing, open arc = running).
3. **Commit grid:** last 22 weeks × 7 days, 10 × 10 cells with a 3 gap, 5 ink levels (10, 30, 50, 72, 100%).
Data comes from the GitHub GraphQL API with one personal access token per account, kept in `local.properties` (see section 8). Without a token the page says so and shows nothing else.

### Screen 5 — Apps (your own groups of apps)
The World clocks and orders page was **removed**; so were the Life page before it. (The notification parsing for Zomato and Blinkit orders is still in the code, with no page showing it.) Mockup: the "Groups" board of the design canvas.
- **Header:** "08 groups · 38 apps" (mono 8.5, dim), the title "Groups" (29 / 200), and a 44 × 44 filled **+** at the right that makes a new group. The page is called **Apps** in the indicator and the switcher.
- **Grid:** 3 columns, 8 gap across and 12 down; each group is a 112 tall tile with no fill: a 3 × 3 preview of its icons (20 dp; a dashed circle for each empty cell; "+N" in the ninth cell when there are more than nine), the name (11) and the count ("6 apps", mono 8.5). The page scrolls.
- **Open a group:** tap a tile and it grows in place to the full width (520 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)`); the groups after it slide down below it; the other groups fade to 35%; the open one gets a translucent panel fill (white 72% in light, 8% in dark). Its apps rise in one after another (48 dp icon, label 9; each rises 10 dp and scales from 90% over about 400 ms, starting 160 ms in and 35 ms after the one before), then an **Add** cell (dashed circle, +). Tap anywhere outside the open group to fold it back (the others slide back and fade in).
- **Open an app:** tap its icon; the app grows out of that icon (Android's own scale-up animation, `ActivityOptions.makeScaleUpAnimation`, given the icon's place on screen).
- **Edit** (button in the open group's header, 30 tall): renames the group (underlined text field) and shows a **−** (24 × 24 ink square) on each app to remove it; **Done** leaves edit mode.
- **Add** (and the + at the top, which makes an empty "New group" and opens it): a sheet slides up over the bottom 70% (420 ms) with "Add to [group]", "N selected" (mono) and a **Done** button; every installed app is a 42 tall row (26 dp icon, name, a square tick box: filled ink with a tick when in the group). Back, Home or Done closes it.
- **Icons:** the same as the drawer: each app's own Play Store icon, or a Glyph line icon for the custom list; Play Store icons are drawn at 80% of their slot so they sit at the same visual size as the line icons.
- **Data:** the groups are the user's; they are saved on the phone (DataStore, as JSON of group name and package names). On first run the design's eight groups are made from the apps this phone has (matched by name; a group with none of its apps is left out): Food (Zomato, Blinkit, Amazon, GPay), Work (Gmail, Outlook, Teams, Calendar, Classroom, Authenticator), Dev & AI (GitHub, Claude, ChatGPT, Assistant, Chrome, Browser), Games (Chess, Sudoku, Bubble Pop!, Clash of Clans), Social (WhatsApp, Instagram, Pinterest, YouTube, YT Music), System (Settings, Files, Calculator, Clock, Google, Authenticator), Photos (Photos, Camera, Pinterest), Travel (Chalo, Maps, Adani OneApp, BookMyShow, NoBrokerHood). An app can be in more than one group.

---

## 4. Page transition — Glyph blend

The background moves slower than the pages, cards trail one after another, and the leaving page gets a very light blur. `offset` is the page's distance from centre (−1…1) in the pager.

| Part | Value |
|---|---|
| Page | `translationX = offset × width` (normal pager slide) |
| Card cascade | each card `translationX = −offset × (index + 1) × 70 dp`; on release, stagger 55 ms per card, 520 ms |
| Blur | leaving page `blur = 2 dp × |offset|` (Android 12+ `RenderEffect`) |
| Fade | `alpha = 1 − 0.12 × |offset|` |
| Background parallax | the launcher's Orbit and dots background (§5A) shifts **60 dp per page** and the rings rotate **8° per page** |
| Easing | 480 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)` |
| Edges | rubber-band at the first and last page (drag × 0.3) |

---

## 5A. Home screen background — Orbit and dots

The launcher draws this itself (Compose `Canvas`), full-screen behind the widgets, with an opaque window. It is **not** a system wallpaper.

- **Base:** `screen.bg` (`#F6F6F6` light, `#000000` dark).
- **Dot grid:** 1 dp dots on a 24 dp grid in ink. It pans slowly diagonally, 240 dp every 60 s, on a loop. Fade it top to bottom (100% → 70% at 60% height → 45% at the bottom). Opacity 26% light, 32% dark.
- **Orbit rings:** a 520 dp group, partly off-screen, with three rings (r 110, r 170 dashed 2/7, r 235; 1 dp stroke). Two dots sit on the rings (r 5 and r 3.5). The whole group rotates once every **120 s**. One more dot (r 4) circles in reverse once every **70 s**. A small ring (r 60) "breathes" (scale 1 → 1.08, opacity 50% → 100%) every **9 s**. Opacity 32% light, 40% dark.
- **Position per screen:** each screen places the orbit group differently so the background varies as you swipe. Positions are relative to the screen's top-left (390 × 844 frame):

| Screen | Orbit group top-left |
|---|---|
| 1 Today | top −150, right −190 |
| 2 Music | top −200, left −60 |
| 3 WhatsApp | top −170, right −250 |
| 4 GitHub | top 380, right −260 |
| 5 Apps | top −170, right −200 |

- Settings: background style (orbit + dots / orbit / dots / none), and **motion on/off, which is built** as the animation switch beside the mode switch on Today (it also stops the wallpaper's rain and dust).
- Pause the animation when the launcher isn't visible (`Lifecycle` `ON_STOP`).

## 5B. Lock screen live wallpaper — Bloom and wind

Set as the system live wallpaper (`WallpaperService`). It shows on the **lock screen**; on the home screen the launcher's own background covers it.

A still composition with only two moving layers.

**Still layers** (positions on a 390 × 844 frame; scale to the screen):
- **Dot grid:** 0.8 dots on a 30 grid, ink @ 12%.
- **Paper folds:** two faint triangles, ink fill 3.5–4.5%, 0.6 stroke @ 25%:
  - right edge: (390,110) (248,300) (390,520)
  - left edge: (0,470) (170,640) (0,800)
- **Vertical line:** x = 232 from the top down to y = 330, 0.7 stroke @ 55%, with dots at y = 64 (r 2.6), 170 (r 2), 248 (r 1.4).
- **Dotted arc:** quadratic from (40,205) through control (160,170) to (232,300), dash 1/5 @ 60%.
- **Wind thread:** cubic path along the lower right edge, dash 3/5 @ 45%.
- **Lotus: removed from the wallpaper** (it was at x 34, y 386, size 322 × 458). Everything else stays exactly as specified. The lotus images (`lotus_light.png`, `lotus_dark.png`) are kept in `res/drawable-nodpi` in case they are used elsewhere.
- **Red dot:** 9 dp at (244, 380), `#D7262E`.

**Moving layers:**
- **Rain:** 12 lines, 1 dp wide, 50–180 tall, gradient from transparent to ink; each falls in 2.5–7.5 s on a loop with a random phase; layer opacity 30%.
- **Dust:** 28 dots, 1.5–4.5 dp, each rising the full screen height in 12–34 s; opacity 0.2–0.8 by size; layer opacity 70%.

**Modes:** light uses the `#F6F6F6` background with black ink. Dark uses `#000000` with white ink.

**Performance:** cap at 30 fps, pause in `onVisibilityChanged(false)` (this fires automatically when the home screen covers it), and use positions seeded once (no per-frame allocation).

**Light/dark:** follow the Glyph mode switch (shared setting in DataStore), so the lock screen and home screen always match.

**Lock-only option:** if OxygenOS offers "Lock screen only" when applying the live wallpaper (Android 14+ supports separate lock and home wallpapers), use it and check `Engine.getWallpaperFlags()`. Otherwise apply it to both; the launcher still covers it on home.

**Asset (delivered):** `app/src/main/res/drawable-nodpi/lotus_light.png` and `lotus_dark.png`, 1024 × 1536, made from `assets/lotus_light_source.png` and `assets/lotus_dark_source.png`. At the 322 dp slot this is about 1:1 on the 12R (322 dp ≈ 1040 px), so the 1440 px target is not needed. Spec for any replacement: a **PNG (or SVG)** of the same lotus, portrait, lotus only (no threads, dots or red), stem running off the bottom edge. Both the light and the dark versions must have a **fully transparent background**, with soft petal edges kept as partial transparency: `lotus_light.png` (black ink) and `lotus_dark.png` (white ink, the inverted version). Both are generated from one source image by turning its darkness into alpha, so the source only needs a flat white background.

**System colours:** override `onComputeColors()` to report **neutral greys only** (primary `#000000`/`#FFFFFF` by mode, secondary `#7A7A7A`), so OxygenOS accents (Control Centre toggles and so on) follow the monochrome theme. **Do not report the red.**

---

## 6. App drawer (swipe up)

**It must look and behave exactly like the stock OxygenOS drawer.** Do not restyle it with the Glyph rules above; sharp corners do not apply here.

- Opens by swiping up from any home screen (there is no dock button); swipe down, press Back, or press Home to close. Launching an app closes it.
- **Suggested apps:** the 10 most launched from Glyph; until there is history, a default list of common apps that are installed (Chrome, YouTube, WhatsApp, Instagram, Maps, Gmail, GPay, Teams, Outlook, Authenticator, Photos).
- **Categories** come from the app's Play Store category, with manual overrides for AI (Claude, ChatGPT, Gemini, Copilot) and Shopping & food (Swiggy, Zomato, Blinkit, Amazon, Flipkart).
- Top: a rounded (fully pill-shaped) **All | Categories** switch with a rounded selected segment, and a ⋮ menu on the right.
- **All view:** suggested apps (2 rows of 5), a divider, then every app A–Z in a 5-column grid; app icon about 56 dp with its label below (11). Letter index A–Z and # down the right edge (tap or drag to jump).
- **Categories view:** sections such as Social, AI, Work, Shopping & food, Games, Travel & local and Tools, each a 5-column grid.
- **Search:** pill-shaped bar at the bottom that filters live.
- **Icons:** each app's **own Play Store icon**, unmodified. Only apps on the custom list get a Glyph line icon instead. **Custom list (keep it configurable):** Camera, WhatsApp, Instagram, Chess.com, Pinterest, Phone. Every other app keeps its Play Store icon for now.
- Background follows the light/dark mode.

---

## 7. Quick Settings tiles (`TileService`)

These are added to the OxygenOS Control Centre. Monochrome icons; active/inactive state where relevant.
- **Hotspot QR** (built): opens a full-screen QR (black on white in either mode) that another phone scans to join the hotspot. The hotspot's name and password are typed once on the QR screen ("Edit" changes them), because Android doesn't expose them; kept in the app's private storage. Subtitle "Set up" until then.
- **Glyph mode** (built): lit while Glyph is dark; a tap switches to the other mode (leaving "Auto"). Subtitle Auto / Dark / Light.
- **Focus** (built): a tap starts a 25 minute Pomodoro, a second tap stops it; subtitle "N min left"; a "Focus done" notification at the end (needs the notification permission, asked once).
- **Homelab** (built, not configured): lit while the server accepts a connection; subtitle Up / Down / Not set up. The address is `homelab.url` in `local.properties` (for example `http://192.168.1.10:8080`); a tap opens it.
- **GPay scan** (built): opens GPay's "Scan any QR code" shortcut (`QR_SCANNER`, which Glyph may start because it is the Home app); opens GPay if the shortcut is missing.

The first time Glyph is shown after install it uses `StatusBarManager.requestAddTileService` (Android 13+) to ask "Add tile?" for each one.

**Swipe down on home has two stages** (page already at its top; a drag that touched the drawer never counts):
- **Short pull (56-300 dp): the shortcuts popup.** A Glyph panel slides down from the top and follows the finger; released past 56 dp it stays, with a scrim (tap outside, drag up or Back closes it; a long drag down from there goes on to the shade). Seven shortcuts in 4 + 3 square buttons (56 dp, lit = filled ink, off = track grey), label and a mono subtitle: Hotspot QR, Hotspot (opens the phone's hotspot settings; Android does not let apps switch it), Flashlight (torch, lit while on), Glyph mode (Dark / Light), Focus (25 min / N min left), Homelab (Up / Down / Not set up), GPay scan. Same actions as the tiles above.
- **Long pull (300 dp or more): the system notification shade** (icons, notifications, quick settings). While pulling past about 210 dp the popup fades to show the handover. It is the system shade, not a Glyph one: Android has no way to draw it ourselves.

---

## 8. Data sources and permissions

| Widget | Source | Permission / setup |
|---|---|---|
| YouTube Music, playing-on devices | `MediaSessionManager` sessions (package `com.google.android.apps.youtube.music`); Bluetooth device battery | Notification access; `BLUETOOTH_CONNECT` |
| WhatsApp chats (**built**) | `NotificationListenerService`; WhatsApp chats come only from its message notifications and are remembered on the phone | Notification access |
| Weather, hourly, sunrise/sunset | Open-Meteo forecast (no key), cached on the phone. **Built.** | `ACCESS_COARSE_LOCATION` |
| Air quality | Open-Meteo air-quality API, **US AQI** (0-500; the bar shows 0-300). **Built.** | same location |
| Calendar / next event | `CalendarContract.Instances`, next 7 days, skipping all-day and declined events. **Built.** | `READ_CALENDAR` |
| Battery | `ACTION_BATTERY_CHANGED`; the bar turns red below 20%. **Built.** Charging watts: not built. | none |
| Screen time | `UsageStatsManager.queryEvents`: foreground time today, compared with the same time of day over the previous 7 days. **Built.** | Usage access (manual toggle in Settings) |
| Steps | Health Connect `StepsRecord`, today's total. **Built.** It only shows what an app has written to Health Connect. | Health Connect steps permission |
| Next alarm | `AlarmManager.getNextAlarmClock()` | none |
| GitHub (multi-account) | GitHub GraphQL API, one token per account. **Built.** | `github.accounts=Personal:TOKEN,Work:TOKEN` in `local.properties` |
| Homelab tile | TCP connect to the host and port in `homelab.url` | `homelab.url=http://host:port` in `local.properties` |
| Flashlight | `CameraManager.setTorchMode` | none |
| QR scanner | CameraX + ML Kit barcode | `CAMERA` |

**Today never shows made-up numbers.** A card without its data shows a dash and says why; when the user can fix it, tapping it asks (location, calendar and Health Connect prompt on first run, then from the card; usage access opens its Settings page). Data refreshes whenever Glyph comes forward, at most once a minute.

OxygenOS: Glyph must be set to **"Don't optimize"** for battery, or the listeners and updates will stall.

---

## 9. Open items

- **Custom recents.** The swipe up from the very bottom is Android's recents gesture and opens OnePlus's own fallback recents (`com.android.quickstep.RecentsActivity`) when Glyph is the Home app. Its look and its static preview cards cannot be changed from an app. Proposed Glyph version: a **Recent** row at the top of the drawer showing the last 5-6 used apps (from `UsageStatsManager`, needs Usage Access), styled like the rest of Glyph. No previews and no swipe-to-close (not possible without system access). Decide how it is opened if not in the drawer.

- Teams and Outlook widgets (Microsoft Graph needs IT consent; otherwise use notifications).
- GitHub account names for the chips.
- Package name (suggested `me.shadovxw.glyph`).
