package io.github.moonggae.kmedia.controller

import io.github.moonggae.kmedia.model.RepeatMode
import kotlin.random.Random

internal class ShuffleOrder(
    private val random: Random = Random.Default,
) {
    private var shuffledIndices = mutableListOf<Int>()

    fun updateShuffleIndices(currentIndex: Int, totalSize: Int) {
        if (totalSize <= 0 || currentIndex !in 0 until totalSize) {
            shuffledIndices.clear()
            return
        }

        val remainingIndices = (0 until totalSize)
            .filter { it != currentIndex }
            .toMutableList()
        remainingIndices.shuffle(random)

        shuffledIndices = mutableListOf(currentIndex).apply {
            addAll(remainingIndices)
        }
    }

    fun getNextIndex(currentIndex: Int, repeatMode: RepeatMode): Int? {
        val currentShuffledIndex = shuffledIndices.indexOf(currentIndex)
        return when {
            currentShuffledIndex < 0 -> null
            currentShuffledIndex == shuffledIndices.lastIndex &&
                    repeatMode == RepeatMode.REPEAT_MODE_OFF -> null
            currentShuffledIndex == shuffledIndices.lastIndex ->
                shuffledIndices.firstOrNull()
            else -> shuffledIndices.getOrNull(currentShuffledIndex + 1)
        }
    }

    fun getPreviousIndex(currentIndex: Int): Int? {
        val currentShuffledIndex = shuffledIndices.indexOf(currentIndex)
        return if (currentShuffledIndex > 0) {
            shuffledIndices[currentShuffledIndex - 1]
        } else null
    }

    fun removeIndex(index: Int) {
        shuffledIndices.remove(index)
        shuffledIndices = shuffledIndices
            .map { if (it > index) it - 1 else it }
            .toMutableList()
    }

    fun clear() {
        shuffledIndices.clear()
    }

    fun addNewIndices(startIndex: Int, count: Int) {
        val newIndices = (startIndex until startIndex + count).toMutableList()
        newIndices.shuffle(random)
        shuffledIndices.addAll(newIndices)
    }
}
