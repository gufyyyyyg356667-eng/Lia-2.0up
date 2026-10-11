package com.Lia.assistant.agent.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeScreenDriverTest {
    private val first = ScreenObservation(1, "com.instagram.android", listOf(UiElement("share", "Share", true, false)))
    private val second = ScreenObservation(2, "com.instagram.android", listOf(UiElement("next", "Next", true, false)))

    @Test fun actionOnCurrentScreenWorks() {
        val d = FakeScreenDriver(listOf(first))
        d.capture()
        assertEquals(DriverResult.OK, d.clickElement(1, "share"))
    }

    @Test fun actionOnOldObservationIsStale() {
        val d = FakeScreenDriver(listOf(first, second))
        d.capture()
        d.capture()
        assertEquals(DriverResult.STALE, d.clickElement(1, "share"))
    }

    @Test fun unknownElementIsNotFound() {
        val d = FakeScreenDriver(listOf(first))
        d.capture()
        assertEquals(DriverResult.NOT_FOUND, d.clickElement(1, "missing"))
    }
}
