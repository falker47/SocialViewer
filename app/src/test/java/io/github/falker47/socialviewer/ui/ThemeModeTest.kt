package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun missingOrLegacyPreferenceResolvesFromSystemOnce() {
        assertEquals(ThemeMode.Light, ThemeMode.fromStorage(null, systemDark = false))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage(null, systemDark = true))
        assertEquals(ThemeMode.Light, ThemeMode.fromStorage("system", systemDark = false))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage("system", systemDark = true))
    }

    @Test
    fun storedValuesRoundTripToExpectedModes() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue, systemDark = !mode.isDark))
        }
    }

    @Test
    fun explicitModesAreStable() {
        assertFalse(ThemeMode.Light.isDark)
        assertTrue(ThemeMode.Dark.isDark)
    }
}
