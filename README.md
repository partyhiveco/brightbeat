# VOS Clone (Android)

A native Android rhythm game inspired by the classic PC game *Virtual
Orchestra Studio (VOS)* — falling notes across lanes, timed hits, combo
scoring. Built with Kotlin + Jetpack Compose (Canvas-based rendering) and
Media3 ExoPlayer for accurate audio-sync timing.

## Getting started

1. Open this folder in Android Studio (Koala or newer recommended).
2. Let Gradle sync — it will pull in Compose, Media3, and kotlinx-serialization.
3. The bundled demo set expects royalty-free/original audio files `demo1.mp3`
   through `demo8.mp3` under `app/src/main/assets/audio/` (see the README there
   for source suggestions — **no copyrighted music is bundled with this project
   on purpose**).
4. Run on a device or emulator (minSdk 26 / Android 8.0+).

## Project structure

```
app/src/main/java/com/example/vosclone/
 ├─ audio/     AudioEngine — ExoPlayer wrapper, single source of truth for playback time
 ├─ chart/     Chart data model (JSON) + loader
 ├─ engine/    NoteScheduler, ScoreJudge, GameSession — core gameplay logic, UI-agnostic
 ├─ ui/
 │   ├─ gameplay/  GameplayScreen — Canvas note-lane renderer + per-frame game loop
 │   ├─ menu/      MenuScreen — signal-pop setlist and locked-content entry point
 │   ├─ navigation/ SignalBottomNav — Setlist / Shop / Profile root navigation
 │   ├─ profile/   ProfileScreen — player progress and entitlement placeholder
 │   ├─ results/   ResultsScreen — score, grade, and replay actions
 │   └─ shop/      ShopScreen — subscription, song-pack, and reward placeholders
 └─ MainActivity.kt

app/src/main/assets/
 ├─ charts/demo1.json … demo8.json   setlist metadata (4 lanes)
 └─ audio/demo1.mp3 … demo8.mp3      bundled demo tracks

app/src/main/res/drawable-nodpi/
 ├─ vos_concert_backdrop_v2.png  generated concert-stage backdrop
 ├─ vos_starlight_cover.png      generated featured-song cover
 ├─ thumb_*.png                   generated song-list thumbnails
 ├─ vos_signal_backdrop_v2.png   legacy screen-print street backdrop
 ├─ vos_signal_crest_v2.png      generated screen-print signal emblem
 └─ night_drive_pack_cover.png   generated song-pack cover art
```

## Design system

The UI uses an original glossy pop-concert direction: deep navy, hot pink,
electric cyan, violet, lemon yellow, stage haze, crowd light sticks, crystal
artwork, and glassy music cards. It keeps the supplied street-radio energy
without copying any logos, characters, or layouts.

- **Palette** — deep concert navy with hot pink, cyan, lavender, lemon, and
  dedicated lane/judgement accents — see `ui/theme/Color.kt`.
- **Type** — a bold sans-serif hierarchy keeps fast gameplay readable while
  oversized labels and uppercase tracking create the broadcast identity — see
  `ui/theme/Type.kt`.
- **Screens** — `MenuScreen` is the setlist/on-air entry point,
  `GameplayScreen` is a full-screen lane field with receptor targets, animated
  judgement bursts, lane flashes, hold bars, and tap haptics, and
  `ResultsScreen` closes the loop with a high-contrast grade and replay
  actions. `ShopScreen` and
  `ProfileScreen` are intentionally honest placeholders for the proposed
  subscription, song-pack, cosmetic reward, and restore-purchase flows.
  Navigation remains a small sealed `Screen` state in `MainActivity` with a
  styled Home / Shop / Profile rail. The Home setlist loads all eight bundled
  demo charts; demo2–demo8 use deterministic BPM-grid auto-mapping until
  hand-authored note timings are supplied.



Every frame, `GameplayScreen` reads `AudioEngine.currentPositionMs()` —
ExoPlayer's actual playback position — rather than trusting wall-clock time.
Note y-positions and hit judgement are both derived from that same audio
clock, so decode/buffering latency doesn't cause visual drift. Results are
shown only after ExoPlayer reaches `STATE_ENDED`, so a short chart cannot cut
off a longer audio track. When the audio duration materially exceeds the last
authored note, `NoteScheduler` detects the coverage gap and adds a deterministic
BPM-grid placeholder pattern through the track end. The HUD labels this as
`FULL TRACK // AUTO-MAPPED`; replace that generated pattern with authored notes
before shipping a production song.

## Chart format

```json
{
  "title": "Demo Track 1",
  "audioFile": "demo1.mp3",
  "bpm": 120,
  "lanes": 4,
  "notes": [
    { "lane": 0, "timeMs": 2000, "type": "tap" }
  ]
}
```
`lane` is 0-indexed left→right. `timeMs` is the target hit time relative to
the start of audio playback. `type: "hold"` + `durationMs` creates a sustained
bar: press near `timeMs`, keep the lane held until the duration ends, and
release within the completion grace window. Gameplay dispatches pointer-down
events immediately and tracks each pointer independently, so 2–3 lane chords
can be played together.

## Roadmap / not yet built

- **In-app chart editor** — load a local audio file, scrub playback, tap
  lanes on the beat, export to the JSON format above. Reuses `AudioEngine`
  and the same lane-rendering code as gameplay.
- **Chart-authored rhythm polish** — the demo and coverage fallback now use
  syncopated patterns, chords, and occasional holds. Replace fallback notes
  with authored charts before shipping a production song.
- **Google Play Billing** — connect the Shop placeholders to Signal Pass,
  non-consumable song packs, entitlement restoration, and purchase analytics.
- **Persisted local storage** for user-authored charts (Room or just files
  under app-private storage) — kept device-local by design, see the
  copyright note below.

## A note on copyright / UGC

This scaffold is deliberately **local-only**: any user-imported song stays
on the user's device and is never uploaded or shared through the app. If you
later add chart/song sharing between users, that crosses into UGC-platform
territory and you'll want a takedown process and terms of service that put
rights-clearance on the uploading user — treat it like any other
UGC platform's copyright policy from day one.
