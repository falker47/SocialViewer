package io.github.falker47.socialviewer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun missingOrUnknownPreferenceDefaultsToSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.System, ThemeMode.fromStorage("unknown"))
    }

    @Test
    fun storedValuesRoundTripToExpectedModes() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue))
        }
    }

    @Test
    fun systemModeFollowsAndroidTheme() {
        assertFalse(ThemeMode.System.resolveDark(systemDark = false))
        assertTrue(ThemeMode.System.resolveDark(systemDark = true))
    }

    @Test
    fun explicitModesOverrideAndroidTheme() {
        assertFalse(ThemeMode.Light.resolveDark(systemDark = true))
        assertTrue(ThemeMode.Dark.resolveDark(systemDark = false))
    }
}
