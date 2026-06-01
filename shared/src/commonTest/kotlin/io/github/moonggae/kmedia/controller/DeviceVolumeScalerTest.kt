package io.github.moonggae.kmedia.controller

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceVolumeScalerTest {
    @Test
    fun scaleDeviceVolumeMapsNormalizedVolumeIntoDeviceRange() {
        assertEquals(2, scaleDeviceVolume(volume = 0f, minVolume = 2, maxVolume = 12))
        assertEquals(7, scaleDeviceVolume(volume = 0.5f, minVolume = 2, maxVolume = 12))
        assertEquals(12, scaleDeviceVolume(volume = 1f, minVolume = 2, maxVolume = 12))
    }

    @Test
    fun scaleDeviceVolumeCoercesOutsideNormalizedRange() {
        assertEquals(2, scaleDeviceVolume(volume = -1f, minVolume = 2, maxVolume = 12))
        assertEquals(12, scaleDeviceVolume(volume = 2f, minVolume = 2, maxVolume = 12))
    }
}
