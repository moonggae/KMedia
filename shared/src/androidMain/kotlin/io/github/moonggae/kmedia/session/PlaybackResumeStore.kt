package io.github.moonggae.kmedia.session

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.Player
import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.util.MediaRequestHeadersRegistry
import io.github.moonggae.kmedia.util.asMusic
import io.github.moonggae.kmedia.util.mediaItems
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

internal class PlaybackResumeStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var lastSavedSnapshot: PlaybackResumeSnapshot? = null
    private var lastSavedElapsedMs: Long = 0L
    private var savingSuppressed = false

    fun save(player: Player, force: Boolean = false) {
        if (savingSuppressed) return

        if (player.hasProtectedMediaItems()) {
            clear()
            return
        }

        val snapshot = player.toResumeSnapshot() ?: return
        if (!force && !shouldPersist(snapshot)) return

        prefs.edit()
            .putString(KEY_SNAPSHOT, snapshot.toJson().toString())
            .apply()

        lastSavedSnapshot = snapshot
        lastSavedElapsedMs = SystemClock.elapsedRealtime()
    }

    fun restore(): PlaybackResumeSnapshot? {
        val snapshotJson = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching { snapshotJson.toResumeSnapshot() }
            .getOrNull()
            ?.also { lastSavedSnapshot = it }
            ?: run {
                clear()
                null
            }
    }

    fun clear() {
        clearSnapshot()
        savingSuppressed = false
    }

    fun clearForExplicitStop() {
        clearSnapshot()
        savingSuppressed = true
    }

    fun allowSaving() {
        savingSuppressed = false
    }

    private fun clearSnapshot() {
        prefs.edit().remove(KEY_SNAPSHOT).apply()
        lastSavedSnapshot = null
        lastSavedElapsedMs = 0L
    }

    private fun shouldPersist(snapshot: PlaybackResumeSnapshot): Boolean {
        val last = lastSavedSnapshot ?: restore() ?: return true
        val structureChanged =
            last.musics.map { it.id } != snapshot.musics.map { it.id } ||
                    last.currentIndex != snapshot.currentIndex ||
                    last.shuffleModeEnabled != snapshot.shuffleModeEnabled ||
                    last.repeatMode != snapshot.repeatMode ||
                    last.playWhenReady != snapshot.playWhenReady

        if (structureChanged) return true
        if (abs(last.positionMs - snapshot.positionMs) >= POSITION_SAVE_INTERVAL_MS) return true

        val elapsedSinceLastSave = SystemClock.elapsedRealtime() - lastSavedElapsedMs
        return elapsedSinceLastSave >= MAX_SAVE_INTERVAL_MS && last.positionMs != snapshot.positionMs
    }

    private companion object {
        const val PREFS_NAME = "io.github.moonggae.kmedia.playback_resume"
        const val KEY_SNAPSHOT = "snapshot"
        const val POSITION_SAVE_INTERVAL_MS = 2_000L
        const val MAX_SAVE_INTERVAL_MS = 10_000L
    }
}

internal data class PlaybackResumeSnapshot(
    val musics: List<Music>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleModeEnabled: Boolean,
    val repeatMode: Int,
    val playWhenReady: Boolean,
    val updatedAtMs: Long,
)

private fun Player.toResumeSnapshot(): PlaybackResumeSnapshot? {
    val currentMusicId = currentMediaItem?.mediaId
    val musics = mediaItems
        .map { it.asMusic() }
        .filter { it.uri.isNotBlank() }
    if (musics.isEmpty()) return null

    val safeIndex = musics.indexOfFirst { it.id == currentMusicId }
        .takeIf { it >= 0 }
        ?: 0
    val safePosition = max(contentPosition, 0L)

    return PlaybackResumeSnapshot(
        musics = musics,
        currentIndex = safeIndex,
        positionMs = safePosition,
        shuffleModeEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
        playWhenReady = playWhenReady,
        updatedAtMs = System.currentTimeMillis(),
    )
}

private fun Player.hasProtectedMediaItems(): Boolean =
    mediaItems.any { mediaItem ->
        MediaRequestHeadersRegistry.hasHeaders(mediaItem.mediaId)
    }

private fun PlaybackResumeSnapshot.toJson(): JSONObject = JSONObject()
    .put(PlaybackResumeStoreKeys.VERSION, PlaybackResumeStoreKeys.VERSION_VALUE)
    .put(PlaybackResumeStoreKeys.ITEMS, musics.toJson())
    .put(PlaybackResumeStoreKeys.CURRENT_INDEX, currentIndex)
    .put(PlaybackResumeStoreKeys.POSITION_MS, positionMs)
    .put(PlaybackResumeStoreKeys.SHUFFLE_MODE_ENABLED, shuffleModeEnabled)
    .put(PlaybackResumeStoreKeys.REPEAT_MODE, repeatMode)
    .put(PlaybackResumeStoreKeys.PLAY_WHEN_READY, playWhenReady)
    .put(PlaybackResumeStoreKeys.UPDATED_AT_MS, updatedAtMs)

private fun List<Music>.toJson(): JSONArray = JSONArray().also { array ->
    forEach { music ->
        array.put(
            JSONObject()
                .put(PlaybackResumeStoreKeys.MUSIC_ID, music.id)
                .putNullable(PlaybackResumeStoreKeys.MUSIC_TITLE, music.title)
                .putNullable(PlaybackResumeStoreKeys.MUSIC_ARTIST, music.artist)
                .putNullable(PlaybackResumeStoreKeys.MUSIC_COVER_URL, music.coverUrl)
                .put(PlaybackResumeStoreKeys.MUSIC_URI, music.uri)
                .put(PlaybackResumeStoreKeys.MUSIC_CACHE_KEY, music.cacheKey)
                .putNullable(PlaybackResumeStoreKeys.MUSIC_MIME_TYPE, music.mimeType)
        )
    }
}

private fun String.toResumeSnapshot(): PlaybackResumeSnapshot? {
    val root = JSONObject(this)
    if (root.optInt(PlaybackResumeStoreKeys.VERSION, 0) != PlaybackResumeStoreKeys.VERSION_VALUE) {
        return null
    }

    val musics = root.getJSONArray(PlaybackResumeStoreKeys.ITEMS).toMusics()
    if (musics.isEmpty()) return null

    return PlaybackResumeSnapshot(
        musics = musics,
        currentIndex = root.optInt(PlaybackResumeStoreKeys.CURRENT_INDEX, 0).coerceIn(musics.indices),
        positionMs = max(root.optLong(PlaybackResumeStoreKeys.POSITION_MS, 0L), 0L),
        shuffleModeEnabled = root.optBoolean(PlaybackResumeStoreKeys.SHUFFLE_MODE_ENABLED, false),
        repeatMode = root.optInt(PlaybackResumeStoreKeys.REPEAT_MODE, Player.REPEAT_MODE_OFF),
        playWhenReady = root.optBoolean(PlaybackResumeStoreKeys.PLAY_WHEN_READY, false),
        updatedAtMs = root.optLong(PlaybackResumeStoreKeys.UPDATED_AT_MS, 0L),
    )
}

private fun JSONArray.toMusics(): List<Music> = buildList {
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        val uri = item.optString(PlaybackResumeStoreKeys.MUSIC_URI).takeIf { it.isNotBlank() } ?: continue
        val id = item.optString(PlaybackResumeStoreKeys.MUSIC_ID).takeIf { it.isNotBlank() } ?: uri
        add(
            Music(
                id = id,
                title = item.optNullableString(PlaybackResumeStoreKeys.MUSIC_TITLE),
                artist = item.optNullableString(PlaybackResumeStoreKeys.MUSIC_ARTIST),
                coverUrl = item.optNullableString(PlaybackResumeStoreKeys.MUSIC_COVER_URL),
                uri = uri,
                cacheKey = item.optNullableString(PlaybackResumeStoreKeys.MUSIC_CACHE_KEY)
                    ?.takeIf { it.isNotBlank() }
                    ?: id,
                mimeType = item.optNullableString(PlaybackResumeStoreKeys.MUSIC_MIME_TYPE),
            )
        )
    }
}

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
    put(key, value ?: JSONObject.NULL)

private object PlaybackResumeStoreKeys {
    const val VERSION = "version"
    const val VERSION_VALUE = 1
    const val ITEMS = "items"
    const val CURRENT_INDEX = "currentIndex"
    const val POSITION_MS = "positionMs"
    const val SHUFFLE_MODE_ENABLED = "shuffleModeEnabled"
    const val REPEAT_MODE = "repeatMode"
    const val PLAY_WHEN_READY = "playWhenReady"
    const val UPDATED_AT_MS = "updatedAtMs"
    const val MUSIC_ID = "id"
    const val MUSIC_TITLE = "title"
    const val MUSIC_ARTIST = "artist"
    const val MUSIC_COVER_URL = "coverUrl"
    const val MUSIC_URI = "uri"
    const val MUSIC_CACHE_KEY = "cacheKey"
    const val MUSIC_MIME_TYPE = "mimeType"
}
