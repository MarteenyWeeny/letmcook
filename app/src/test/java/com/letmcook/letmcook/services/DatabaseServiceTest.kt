package com.letmcook.letmcook.services

import androidx.test.core.app.ApplicationProvider
import com.letmcook.letmcook.models.AppSettingsModel
import com.letmcook.letmcook.models.UserModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseServiceTest {

    private lateinit var databaseService: DatabaseService

    @Before
    fun setup() {
        databaseService = DatabaseService(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testUpsertAndGetUser() {
        val user = UserModel(
            id = "test@example.com",
            email = "test@example.com",
            fullName = "Test User",
            createdAt = "2023-10-27 10:00:00"
        )
        databaseService.upsertUser(user)

        val retrieved = databaseService.getUser("test@example.com")
        assertNotNull(retrieved)
        assertEquals("Test User", retrieved?.fullName)
    }

    @Test
    fun testUpdateAppSettings() {
        val initial = databaseService.getAppSettings()
        assertFalse(initial.useLargeText)

        val updated = AppSettingsModel(useLargeText = true, keepScreenOn = true)
        databaseService.updateAppSettings(updated)

        val retrieved = databaseService.getAppSettings()
        assertTrue(retrieved.useLargeText)
        assertTrue(retrieved.keepScreenOn)
    }
}
