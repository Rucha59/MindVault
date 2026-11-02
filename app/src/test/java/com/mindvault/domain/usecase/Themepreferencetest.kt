package com.mindvault.domain.usecase

import com.mindvault.domain.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceTest {

    @Test
    fun systemFollowsDeviceSetting() {
        assertTrue(ThemePreference.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemePreference.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun lightAndDarkIgnoreDeviceSetting() {
        assertFalse(ThemePreference.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(ThemePreference.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun observeEmitsCurrentPreference() = runBlocking {
        val repo = FakeSettingsRepository(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, ObserveThemePreferenceUseCase(repo)().first())
    }

    @Test
    fun setPersistsAndIsObserved() = runBlocking {
        val repo = FakeSettingsRepository()
        SetThemePreferenceUseCase(repo)(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, ObserveThemePreferenceUseCase(repo)().first())
    }
}