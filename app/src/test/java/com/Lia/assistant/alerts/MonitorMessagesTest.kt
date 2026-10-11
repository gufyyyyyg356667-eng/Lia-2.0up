package com.Lia.assistant.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorMessagesTest {
    @Test fun lowAnnouncesTenFirstWhenBelowTen() = assertEquals(10, MonitorMessages.lowThresholdToAnnounce(9, emptySet()))
    @Test fun lowAnnouncesFifteenWhenBelowFifteen() = assertEquals(15, MonitorMessages.lowThresholdToAnnounce(14, emptySet()))
    @Test fun eachLevelOnlyOnce() = assertNull(MonitorMessages.lowThresholdToAnnounce(14, setOf(15)))
    @Test fun nothingAboveFifteen() = assertNull(MonitorMessages.lowThresholdToAnnounce(40, emptySet()))
    @Test fun headphoneWordingDiffers() {
        assertEquals(true, MonitorMessages.headphones(true).contains("লাগানো"))
        assertEquals(true, MonitorMessages.headphones(false).contains("খোলা"))
    }
}
