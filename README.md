# KMedia - KMP Music Player Library
[![Maven Central](https://img.shields.io/maven-central/v/io.github.moonggae/kmedia)](https://mvnrepository.com/artifact/io.github.moonggae/kmedia)

Audio player library built with Kotlin Multiplatform (KMP). It provides a consistent API for music playback functionality across both Android and iOS.

### Android Sample

<p>
<img width="32%" src="readme-assets/android1.png">
<img width="32%" src="readme-assets/android2.png">
<img width="32%" src="readme-assets/android3.png">
</p>

### iOS Sample
<p>
<img width="32%" src="readme-assets/iOS1.PNG">
<img width="32%" src="readme-assets/iOS2.PNG">
<img width="32%" src="readme-assets/iOS3.PNG">
</p>

## Key Features

- Supports Kotlin Multiplatform (Android, iOS)
- Consistent music playback experience with a unified API
- Media caching support
- Playlist management (add, remove, reorder, replace)
- Dynamic music replacement during playback
- Shuffle and repeat mode support
- Sleep timer support (fixed duration / current track end)
- Playback state and position monitoring
- Background playback support
- Control Center (iOS) and Media Notification (Android) integration
- Per-track HTTP request headers for protected streaming URLs

## Setup

### Gradle Setup

For Kotlin Multiplatform, add the dependency below to your module's `build.gradle.kts` file:
```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("io.github.moonggae:kmedia:$kmedia_version")
    }
}
```

### Android Setup

Add the following permissions and service to your `AndroidManifest.xml`:

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<!-- Service registration -->
<service android:name="io.github.moonggae.kmedia.session.PlaybackService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

### iOS Setup

Add the following to your `Info.plist`:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>io.github.moonggae.kmedia.assetDownloadSession</string>
</array>
<key>UIBackgroundModes</key>
<array>
    <string>audio</string>
    <string>processing</string>
</array>
```

## Usage

### Basic Usage

Initialize KMedia once when your app starts. On Android, call it from `Application.onCreate()` so
`PlaybackService` can be created safely before any UI screen.

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        KMedia.initialize(
            context = this,
            config = KMediaConfig(
                cacheEnabled = true,
                cacheSizeMb = 1024
            )
        )
    }
}
```

On Android, you can customize the activity `Intent` used when the media notification is tapped.
The Android-specific overload keeps Android types out of the common `KMediaConfig`.

```kotlin
import android.net.Uri
import io.github.moonggae.kmedia.KMediaAndroidConfig
import io.github.moonggae.kmedia.initialize

KMedia.initialize(
    context = this,
    config = KMediaConfig(
        cacheEnabled = true,
        cacheSizeMb = 1024
    ),
    androidConfig = KMediaAndroidConfig(
        sessionActivityIntentProvider = { _, defaultIntent ->
            defaultIntent?.apply {
                data = Uri.parse("kmedia://session/open?source=notification")
                putExtra("source", "media_notification")
            }
        }
    )
)
```

Handle the intent from both `onCreate()` and `onNewIntent()` if your app needs the data or extras.

On iOS, call the same initialization API from your app entry point before creating the player UI.

```kotlin
KMedia.initialize(
    context = Unit,
    config = KMediaConfig(
        cacheEnabled = true,
        cacheSizeMb = 1024
    )
)
```

KMedia can be used as follows:

```kotlin
// Create a KMedia instance
val media = KMedia.create()

// Get music list
val musics = SampleMusicRepository().getSampleMusicList()

// Start playback
media.player.playMusics(musics, startIndex = 0)

// Play/Pause
media.player.play()
media.player.pause()

// Previous/Next track
media.player.previous()
media.player.next()

// Seek to position
media.player.seekTo(positionMs = 30000)
```

For protected audio streams, attach request headers to each `Music` item. Headers are
kept in memory for playback and are not used as cache keys.

```kotlin
val protectedMusic = Music(
    id = "asmr-1",
    title = "Rainy room",
    uri = "https://example.com/audio/asmr-1/stream",
    cacheKey = "audio-file-1",
    mimeType = "audio/mpeg",
    requestHeaders = mapOf(
        "Authorization" to "Bearer <token>",
        "X-Playback-Token" to "<playback-token>"
    )
)

media.player.playMusics(listOf(protectedMusic), startIndex = 0)
```

### Lifecycle

Call `KMedia.initialize(...)` once per app process before creating any `KMedia` instance.
On Android, call it from `Application.onCreate()` so `PlaybackService` can resolve its
dependencies even when it is created before UI.

`KMedia.create()` returns the process-wide KMedia facade.

`KMedia.release()` releases resources owned by that facade, such as sleep timer jobs and
the playback controller connection. It does not shut down KMedia's internal dependency graph.
Reinitializing KMedia with a different config in the same process is not supported.

### Monitoring Playback State

You can monitor the playback state through Flow:

```kotlin
val playbackState by media.playbackState.collectAsState()

// Check current playback status
when (playbackState.playingStatus) {
    PlayingStatus.PLAYING -> // Playing
    PlayingStatus.PAUSED -> // Paused
    PlayingStatus.BUFFERING -> // Buffering
    PlayingStatus.IDLE -> // Not ready
    PlayingStatus.ENDED -> // Playback completed
}

// Current position and duration
val position = playbackState.position
val duration = playbackState.duration

// Current music information
val currentMusic = playbackState.music
val currentIndex = playbackState.currentIndex
```

### Playlist Management

#### Basic Playlist Operations

```kotlin
// Add music to current playlist
media.player.addMusic(newMusic)

// Add multiple musics to playlist
media.player.addMusics(musicList)

// Remove music at specific index
media.player.removeMusic(index = 2)

// Clear entire playlist
media.player.clearPlaylist()
```

#### Dynamic Music Replacement

You can replace music in the current playlist without interrupting playback:

```kotlin
// Replace music at specific index
val updatedMusic = currentMusic.copy(
    uri = "https://example.com/new-track.mp3",
    title = "Updated Title"
)
media.player.replaceMusic(index = 0, music = updatedMusic)

// Replace current playing music
val playbackState by media.playbackState.collectAsState()
playbackState.music?.let { currentMusic ->
    if (needsUpdate(currentMusic)) {
        val newMusic = currentMusic.copy(
            uri = getUpdatedUri(currentMusic.id)
        )
        media.player.replaceMusic(playbackState.currentIndex, newMusic)
    }
}
```

**Use Cases for Music Replacement:**
- Loading high-quality audio URLs after initial playlist setup
- Updating expired streaming URLs
- Switching between different audio quality versions
- Dynamic content updates during playback

### Repeat and Shuffle Modes

```kotlin
// Set repeat mode
media.player.setRepeatMode(RepeatMode.REPEAT_MODE_ONE) // Repeat one
media.player.setRepeatMode(RepeatMode.REPEAT_MODE_ALL) // Repeat all
media.player.setRepeatMode(RepeatMode.REPEAT_MODE_OFF) // No repeat

// Set shuffle mode
media.player.setShuffleMode(true) // Enable shuffle
media.player.setShuffleMode(false) // Disable shuffle
```

### Sleep Timer

KMedia includes a built-in sleep timer with smooth fade-out before pause.

```kotlin
// Start timer with fixed duration (milliseconds)
media.sleepTimer.start(durationMs = 15 * 60 * 1000L) // 15 minutes

// Stop playback at the end of current track
media.sleepTimer.startUntilCurrentTrackEnd()

// Cancel timer
media.sleepTimer.cancel()
```

You can observe sleep timer state via Flow:

```kotlin
val sleepTimerState by media.sleepTimer.state.collectAsState()

if (sleepTimerState.isActive) {
    println("Mode: ${sleepTimerState.mode}")
    println("Remaining: ${sleepTimerState.remainingMs} ms")
}
```

Notes:
- Duration timer uses monotonic time for stable countdown behavior.
- "Current track end" mode tracks playback progress and `ENDED` state.
- On expiry, playback volume fades out smoothly, then playback is paused.

### Cache Management

```kotlin
// Check cache usage
val cacheUsage by media.cache.usedSizeBytes.collectAsState(initial = 0L)

// Observe cache statuses
val cacheStatuses by media.cache.statuses.collectAsState()

// Cache specific music
media.cache.preCacheMusic(url = "https://example.com/music.mp3", key = "music1")

// Cache protected music without putting tokens in the URL
media.cache.preCacheMusic(
    url = "https://example.com/audio/asmr-1/stream",
    key = "audio-file-1",
    requestHeaders = protectedMusic.requestHeaders
)

// Check cache status
val isCached = media.cache.checkMusicCached("music1")

// Remove cache
media.cache.removeCachedMusic("music1")

// Clear all cache
media.cache.clearCache()
```

## Advanced Features

### Cache Statuses

KMedia exposes cache status changes as state, so UI code can subscribe without registering
callbacks from `Application`.

Usage example:

```kotlin
val cacheStatuses by media.cache.statuses.collectAsState()

when (cacheStatuses["music1"]) {
    CacheStatus.NONE -> println("music1: No cache")
    CacheStatus.PARTIALLY_CACHED -> println("music1: Caching in progress")
    CacheStatus.FULLY_CACHED -> println("music1: Caching completed")
    null -> println("music1: No cache status yet")
}
```

### Playback Analytics Events

Playback analytics are exposed as an in-process event stream.

Events are delivered to active collectors only. KMedia does not persist analytics events, does
not replay historical events to new collectors, and does not guarantee delivery after process
death. If analytics delivery must be guaranteed, collect this stream from an application-level
coroutine and persist or upload events in your app layer.

Usage example:

```kotlin
LaunchedEffect(media) {
    media.analyticsEvents.collect { event ->
        val playPercentage = (event.totalPlayTimeMs.toFloat() / event.durationMs.toFloat()) * 100
        println("${event.musicId} playback completed: $playPercentage% played")

        // You can send analytics data to server here.
    }
}
```
