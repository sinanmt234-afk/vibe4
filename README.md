# VIBE4MUSICZ — Luxury YouTube Music Client for Android

VIBE4MUSICZ is a Jetpack Compose + Media3 Android music streaming app connected to YouTube Music with an obsidian & champagne-gold aesthetic, automatic landscape Ambient Mode, Pro Audio DSP, network-aware streaming quality, and global Friends Party synchronization.

## Features
- **YouTube Music Integration**: Browse trending music, search songs, and stream audio via InnerTube (`InnerTubeClient.kt`) with automatic bitrate selection.
- **Landscape Ambient Mode**: Rotating your phone to landscape automatically switches to a split-screen view with high-resolution album artwork on the left and auto-scrolling synced lyrics on the right.
- **Pro Audio Engine & Settings**: Separate Wi-Fi and Cellular streaming quality selectors (Low, Medium, High, Lossless Tier), hardware Equalizer presets, Bass Boost, Audio Normalization, Gapless Playback, Skip Silence, Crossfade, and OLED Pure Black mode.
- **Friends Party Room**: Create a 6-character room code and share it with friends anywhere in the world for real-time synchronized playback and live emoji reactions over secure WebSockets (`wss://broker.emqx.io:8084/mqtt`).

## Build APK on GitHub
1. Extract this archive and push all files (including `.github/workflows/build-apk.yml`) to a new GitHub repository.
2. Open the **Actions** tab in your GitHub repository.
3. The **Build VIBE4MUSICZ APK** workflow will run automatically on push (or click **Run workflow**).
4. Once complete, download `VIBE4MUSICZ-debug-apk` from the workflow **Artifacts** section and install it on your Android device.
