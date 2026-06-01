package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.RepeatMode

class ShuffleManager {
    private val order = ShuffleOrder()

    fun updateShuffleIndices(currentIndex: Int, totalSize: Int) =
        order.updateShuffleIndices(currentIndex, totalSize)

    fun getNextIndex(currentIndex: Int, repeatMode: RepeatMode): Int? =
        order.getNextIndex(currentIndex, repeatMode)

    fun getPreviousIndex(currentIndex: Int): Int? = order.getPreviousIndex(currentIndex)

    fun removeIndex(index: Int) = order.removeIndex(index)

    fun clear() = order.clear()

    fun addNewIndices(startIndex: Int, count: Int) = order.addNewIndices(startIndex, count)
}
