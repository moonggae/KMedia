package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.Music
import io.github.moonggae.kmedia.model.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistStateTest {
    @Test
    fun updatePlaylistClampsStartIndexToValidItem() {
        val playlist = PlaylistState()

        playlist.updatePlaylist(musics = tracks(3), startIndex = 99)

        assertEquals(2, playlist.currentIndex)
        assertEquals("track-2", playlist.getCurrentMusic()?.id)
    }

    @Test
    fun nextReturnsNullAtEndWhenRepeatIsOff() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 2)

        assertNull(playlist.getNextIndex())
        assertFalse(playlist.hasNext())
    }

    @Test
    fun nextWrapsAtEndWhenRepeatAllIsEnabled() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 2)
        playlist.setRepeatMode(RepeatMode.REPEAT_MODE_ALL)

        assertEquals(0, playlist.getNextIndex())
        assertTrue(playlist.hasNext())
    }

    @Test
    fun emptyPlaylistDoesNotHaveNextEvenWhenRepeatAllIsEnabled() {
        val playlist = PlaylistState()
        playlist.setRepeatMode(RepeatMode.REPEAT_MODE_ALL)

        assertNull(playlist.getNextIndex())
        assertFalse(playlist.hasNext())
    }

    @Test
    fun enablingShuffleBeforeAddingFirstItemDoesNotCreateSelfNext() {
        val playlist = PlaylistState()

        playlist.setShuffleMode(true)
        playlist.appendMusics(tracks(1))

        assertNull(playlist.getNextIndex())
        assertFalse(playlist.hasNext())
    }

    @Test
    fun previousReturnsNullAtBeginning() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 0)

        assertNull(playlist.getPreviousIndex())
        assertFalse(playlist.hasPrevious())
    }

    @Test
    fun appendMusicsSkipsExistingItems() {
        val playlist = PlaylistState()
        val existingTracks = tracks(2)
        playlist.updatePlaylist(musics = existingTracks, startIndex = 0)

        playlist.appendMusics(listOf(existingTracks[1], track("track-2")))

        assertEquals(listOf("track-0", "track-1", "track-2"), playlist.ids())
    }

    @Test
    fun movingNonCurrentItemPreservesCurrentMusic() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(4), startIndex = 1)

        playlist.moveMediaItem(fromIndex = 3, toIndex = 0)

        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(2, playlist.currentIndex)
        assertEquals(listOf("track-3", "track-0", "track-1", "track-2"), playlist.ids())
    }

    @Test
    fun movingCurrentItemPreservesCurrentMusicAtNewIndex() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(4), startIndex = 1)

        playlist.moveMediaItem(fromIndex = 1, toIndex = 3)

        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(3, playlist.currentIndex)
        assertEquals(listOf("track-0", "track-2", "track-3", "track-1"), playlist.ids())
    }

    @Test
    fun removingNonCurrentItemBeforeCurrentPreservesCurrentMusic() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(4), startIndex = 2)

        val result = playlist.removeMusic("track-0")

        assertIs<PlaylistRemovalResult.RemovedNonCurrent>(result)
        assertEquals("track-2", result.currentMusic.id)
        assertEquals("track-2", playlist.getCurrentMusic()?.id)
        assertEquals(1, playlist.currentIndex)
        assertEquals(listOf("track-1", "track-2", "track-3"), playlist.ids())
    }

    @Test
    fun removingNonCurrentItemAfterCurrentPreservesCurrentMusic() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(4), startIndex = 1)

        val result = playlist.removeMusic("track-3")

        assertIs<PlaylistRemovalResult.RemovedNonCurrent>(result)
        assertEquals("track-1", result.currentMusic.id)
        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(1, playlist.currentIndex)
        assertEquals(listOf("track-0", "track-1", "track-2"), playlist.ids())
    }

    @Test
    fun removingCurrentMiddleItemSelectsNextItem() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(4), startIndex = 1)

        val result = playlist.removeMusic("track-1")

        assertIs<PlaylistRemovalResult.RemovedCurrent>(result)
        assertEquals("track-2", result.replacementMusic.id)
        assertEquals("track-2", playlist.getCurrentMusic()?.id)
        assertEquals(1, playlist.currentIndex)
        assertEquals(listOf("track-0", "track-2", "track-3"), playlist.ids())
    }

    @Test
    fun removingCurrentLastItemSelectsPreviousItem() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 2)

        val result = playlist.removeMusic("track-2")

        assertIs<PlaylistRemovalResult.RemovedCurrent>(result)
        assertEquals("track-1", result.replacementMusic.id)
        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(1, playlist.currentIndex)
        assertEquals(listOf("track-0", "track-1"), playlist.ids())
    }

    @Test
    fun removingMissingItemReturnsNotFoundAndKeepsPlaylist() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 1)

        val result = playlist.removeMusic("missing")

        assertEquals(PlaylistRemovalResult.NotFound, result)
        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(listOf("track-0", "track-1", "track-2"), playlist.ids())
    }

    @Test
    fun removingOnlyItemReturnsPlaylistBecameEmpty() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(1), startIndex = 0)

        val result = playlist.removeMusic("track-0")

        assertEquals(PlaylistRemovalResult.PlaylistBecameEmpty, result)
        assertNull(playlist.getCurrentMusic())
        assertEquals(0, playlist.currentIndex)
        assertTrue(playlist.getPlaylist().isEmpty())
    }

    @Test
    fun replacingCurrentItemUpdatesCurrentMusic() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 1)

        playlist.replaceMusic(index = 1, music = track("replacement"))

        assertEquals("replacement", playlist.getCurrentMusic()?.id)
        assertEquals(listOf("track-0", "replacement", "track-2"), playlist.ids())
    }

    @Test
    fun replacingNonCurrentItemDoesNotChangeCurrentMusic() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 1)

        playlist.replaceMusic(index = 2, music = track("replacement"))

        assertEquals("track-1", playlist.getCurrentMusic()?.id)
        assertEquals(listOf("track-0", "track-1", "replacement"), playlist.ids())
    }

    @Test
    fun clearResetsPlaylistModesAndCurrentState() {
        val playlist = PlaylistState()
        playlist.updatePlaylist(musics = tracks(3), startIndex = 1)
        playlist.setRepeatMode(RepeatMode.REPEAT_MODE_ALL)
        playlist.setShuffleMode(true)

        playlist.clear()

        assertTrue(playlist.getPlaylist().isEmpty())
        assertNull(playlist.getCurrentMusic())
        assertEquals(0, playlist.currentIndex)
        assertEquals(RepeatMode.REPEAT_MODE_OFF, playlist.repeatMode)
        assertFalse(playlist.isShuffleOn)
        assertFalse(playlist.hasNext())
        assertFalse(playlist.hasPrevious())
    }

    private fun tracks(count: Int): List<Music> = List(count) { index ->
        track("track-$index")
    }

    private fun track(id: String) = Music(
        id = id,
        title = id,
        uri = "https://example.com/$id.mp3",
    )

    private fun PlaylistState.ids(): List<String> = getPlaylist().map { it.id }
}
