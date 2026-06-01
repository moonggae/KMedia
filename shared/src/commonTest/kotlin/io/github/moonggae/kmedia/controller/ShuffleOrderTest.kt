package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.RepeatMode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShuffleOrderTest {
    @Test
    fun shuffledOrderStartsFromCurrentIndexAndVisitsEveryOtherIndexOnce() {
        val order = ShuffleOrder(random = Random(1))
        val visited = mutableListOf<Int>()
        var currentIndex = 2

        order.updateShuffleIndices(currentIndex = currentIndex, totalSize = 5)

        while (true) {
            val nextIndex = order.getNextIndex(currentIndex, RepeatMode.REPEAT_MODE_OFF) ?: break
            visited += nextIndex
            currentIndex = nextIndex
        }

        assertEquals(setOf(0, 1, 3, 4), visited.toSet())
        assertEquals(4, visited.size)
        assertNull(order.getNextIndex(currentIndex, RepeatMode.REPEAT_MODE_OFF))
        assertEquals(2, order.getNextIndex(currentIndex, RepeatMode.REPEAT_MODE_ALL))
    }

    @Test
    fun removeIndexDropsRemovedPlaylistIndexAndShiftsHigherIndices() {
        val order = ShuffleOrder(random = Random(2))
        order.updateShuffleIndices(currentIndex = 1, totalSize = 4)

        order.removeIndex(0)

        val visited = mutableListOf<Int>()
        var currentIndex = 0
        while (true) {
            val nextIndex = order.getNextIndex(currentIndex, RepeatMode.REPEAT_MODE_OFF) ?: break
            visited += nextIndex
            currentIndex = nextIndex
        }

        assertEquals(setOf(1, 2), visited.toSet())
        assertEquals(2, visited.size)
    }
}
