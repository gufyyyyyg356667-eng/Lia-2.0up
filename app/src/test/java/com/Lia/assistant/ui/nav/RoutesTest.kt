package com.Lia.assistant.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test fun chatWithoutIdUsesPlainRoute() = assertEquals("chat", Routes.chat())
    @Test fun chatWithIdCarriesId() = assertEquals("chat?id=abc", Routes.chat("abc"))
    @Test fun barShowsOnlyOnMainTabs() {
        assertTrue(Routes.HOME in Routes.barRoutes)
        assertTrue(Routes.SETTINGS in Routes.barRoutes)
        assertFalse(Routes.VOICE in Routes.barRoutes)
        assertFalse(Routes.PERSONALITY in Routes.barRoutes)
    }
}
