# 🌌 Neon Grid

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Design](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20DB-orange.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Network](https://img.shields.io/badge/Network-MQTT%20PubSub-lightgrey.svg?style=flat)](https://hivemq.com)

**Neon Grid** is a stylish, ultra-modern cyberpunk-themed real-time multiplayer Tic-Tac-Toe arena built entirely on a modern native Android stack. Gone are the boring grid lines and silent turns—Neon Grid revitalizes the classic matchup with premium **glowing vector design aesthetics**, a real-time **Mqtt-based matchmaking and gaming network pipeline**, **adaptive failover reliability layers**, and a **runtime haptic audio synthesizer** that creates retro arcade-style soundscapes directly out of raw math!

---

## 🚀 Key Highlights & Features

### 📡 1. Real-Time Online Matchmaking (MQTT Pub/Sub)
- **Zero-Config Rooms**: Host any screen or board instantly. Secure 6-digit dynamic room codes keep setups clean and easy.
- **Failover Safe Connection**: Built-in automated secondary-broker fallback. If the primary EMQX broker experiences a hiccup, Neon Grid automatically and seamlessly shifts the game to HiveMQ within milliseconds, retaining matches on-the-fly.
- **Auto-Sync Hands**: Automatic state preservation logic. When network latency occurs (or if players briefly lose packets), the state-preservation handshake updates both ends immediately on reconnection.

### 🔊 2. Real-Time DSP Audio Synthesizer (`SoundSynth`)
- **Procedural PCM Wave Generation**: No bulky audio assets (.mp3/.wav files) inside the APK! All audio is synthesized procedurally at runtime inside 16-bit PCM buffer loops (`22050 Hz SAMPLE_RATE`).
- **No-Click Smooth Decay Envelopes**: Integrates high-fidelity sine-wave, square-wave, and triangle-wave oscillators combined with dynamic sound level envelopes (linear fade transitions) to completely eliminate speaker "click and pop" noise.
- **Arcade Soundscapes**: Custom major root-chord arpeggios for wins, melancholic retro buzzy tones for losses, and distinctive cozy pitch shifts for 'X' and 'O' moves.

### 🎮 3. Multi-Tier Competitive Modes
- **Online Arena**: Bidirectional websocket synchronization. Fully verified handshake checks make sure invalid attempts never disrupt the grid.
- **Couch pass-and-play**: Play locally offline with friends in a polished turn-swapping local environment.
- **Tuned Offline CPU AI**: Challenge an intelligent, reactive AI system locally designed to test your tactical skills.

### 🗄️ 4. Room Database SQL Persistence
- **Historic Logs**: Track every single finish with details showcasing the date, opponent's names, local/online statuses, and final results.
- **Player Stats & Progression**: Automatic calculation of Wins, Losses, and Draws stored securely. Progresses player XP and raises levels dynamically.

---

## 🎨 Visual Identity & Theme

Neon Grid trades "generic UI" designs for high-contrast cyberpunk elegance. The interface utilizes generous negative space, sleek glowing borders, and energetic visual feedback:

| Color Accent | Hex Value | Purpose |
| :--- | :--- | :--- |
| **Slate Dark** (Base Canvas) | `#14151B` | High-contrast, easy-on-the-eyes dark backdrop |
| **Slate Card** (Grouping panels) | `#1F2026` | Elegant card borders separating dashboard cards |
| **Neon Cyan** (Glow Indicator 1) | `#00F2FE` | Active turns, player metrics, and primary buttons |
| **Neon Magenta** (Glow Indicator 2) | `#FE0979` | Opponent colors, warning bars, brand titles |
| **Neon Yellow** (Success highlights) | `#FFE600` | Victories, level meters, and star badges |

**Custom Composables & Canvas Animations:**
- **`PulseAmbientBackground`**: A dynamic ambient layout that pulses and matches your screen dimensions gracefully using two offset glowing radial vector circles.
- **Grid Particle Canvas**: Each tap draws glowing symbols accented with precise animations to give haptic arcade feedback to your steps.

---

## 🛠️ System Architecture

Neon Grid is engineered with robust, industry-standard modern Android patterns (**Clean Architecture + MVVM**):

```
       ┌─────────────────────────────────────────────────────────┐
       │                      MainActivity                       │
       └────────────────────────────┬────────────────────────────┘
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │                TikTokToeApp (Compose UI)                │
       │  [Menu] [Setup] [LobbyWait] [Arena] [History] [Profile] │
       └────────────────────────────┬────────────────────────────┘
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │                      GameViewModel                      │
       │       Manages StateFlows, Room logic & Handshakes       │
       └──────────────────────┬─────────────┬────────────────────┘
                              │             │
              ┌───────────────┘             └────────────────┐
              ▼                                              ▼
┌───────────────────────────┐                  ┌───────────────────────────┐
│        MqttManager        │                  │     GameDatabase (Room)   │
│   (Pub/Sub Broker Sync)   │                  │   (ProfileStats & Matches)│
└─────────────┬─────────────┘                  └───────────────────────────┘
              │
      Failover Pipeline
              ▼
  [EMQX / HiveMQ TCP Socket]
```

### The Live Match Handshake Pipeline
When two clients connect online over MQTT, they subscribe to a shared room topic (`tiktoktoe_cecb93d3/rooms/{RoomCode}`):
1. **Host** generates random Room Code, inserts profile specs, and subscribes.
2. **Guest** inputs the Room Code, sends a `JOIN` packet containing their player statistics.
3. **Host** receives the `JOIN` request, welcomes the guest with a `WELCOME` handshake, and initiates the board.
4. **Moves** are published as coordinate packets (`boardIndex`, `playerSymbol`). Both clients locally evaluate wins to avoid unauthorized state tempering.

---

## 💾 Local SQLite Database Schema

Powered by **Jetpack Room Framework**, the database manages persistence in two clean tables:

### 1. `profile_stats` Table:
Stores the continuous state of the player profile.
*   `id` (String, Primary Key) -> Holds `"primary_user_profile"`
*   `username` (String) -> The custom name representing the player (default: `"NeonPlayer"`)
*   `wins` (Integer) -> Aggregated victory count
*   `losses` (Integer) -> Aggregated defeat count
*   `draws` (Integer) -> Aggregated tie count
*   `level` (Integer) -> Current experience level

### 2. `match_history` Table:
Maintains the logs of completed challenges.
*   `id` (Long, Autogenerate, Primary Key) -> Unique matching identifier
*   `timestamp` (Long) -> Unix epoch indicating exact completion date
*   `opponentName` (String) -> Opponent player username
*   `gameMode` (String) -> `"ONLINE"`, `"CPU"`, or `"LOCAL"`
*   `outcome` (String) -> `"WIN"`, `"LOSS"`, or `"DRAW"`

---

## 📦 File Structure

Here are the important files inside our architecture:

```
app/src/main/java/com/example/
 ├── MainActivity.kt               # Setups Android edge-to-edge screens, loads VM and App Composables
 ├── audio/
 │    └── SoundSynth.kt            # Procedural audio waveform buffers, click eliminations, sound arpeggios
 ├── data/
 │    ├── database/
 │    │    ├── Entities.kt         # Database Room entities containing profile variables and History models
 │    │    ├── GameDao.kt          # Room Interface containing queries for saving matches and loading stats
 │    │    └── GameDatabase.kt     # Main local Room Database Builder
 │    └── network/
 │         └── MqttManager.kt      # Online MQTT pub/sub streams, connection check loops, double-broker failsafe
 └── ui/
      ├── game/
      │    ├── GameScreens.kt      # Single view UI screens (Menu, Setup, Play grid, Profile, History UI)
      │    └── GameViewModel.kt    # State engine handling board calculations, remote MQTT frames, state trees
      └── theme/
           ├── Color.kt            # Palette definitions of Cyberpunk accent colors
           ├── Theme.kt            # Central material container theme
           └── Type.kt             # Monospace typography pairing
```

---

## ⚙️ Compilation & Setup

### Prerequisites
- **Android Studio Jellyfish+** (or compile cleanly with JDK 17 via command line).
- Target SDK: `34`, Min SDK: `26`.

### Build Commands
To compile and assemble a testable debug APK, use Gradle inside the root directory:

```bash
# Clean project resources safely
gradle clean

# Assemble debug binary outputs
gradle assembleDebug

# Run unit verification tests
gradle :app:testDebugUnitTest
```

---
# 👨‍💻 Author
# Suman M
⭐ If you enjoyed this project, consider giving the repository a star on GitHub!

