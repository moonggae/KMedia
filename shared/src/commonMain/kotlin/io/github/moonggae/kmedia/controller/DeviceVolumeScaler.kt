package io.github.moonggae.kmedia.controller

internal fun scaleDeviceVolume(
    volume: Float,
    minVolume: Int,
    maxVolume: Int,
): Int {
    val scaledVolume = minVolume + (volume * (maxVolume - minVolume)).toInt()
    return scaledVolume.coerceIn(minVolume, maxVolume)
}
