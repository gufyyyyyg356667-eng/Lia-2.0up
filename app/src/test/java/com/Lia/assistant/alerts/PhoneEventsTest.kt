package com.Lia.assistant.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneEventsTest {
    @Test fun chargingWithPercent() = assertEquals("ফোন চার্জে বসানো হয়েছে, এখন 42%।", PhoneEvents.charging(42))
    @Test fun chargingFullSaysFull() = assertEquals("ফোন চার্জে বসানো হয়েছে, চার্জ ফুল আছে।", PhoneEvents.charging(100))
    @Test fun chargingWithoutPercent() = assertEquals("ফোন চার্জে বসানো হয়েছে।", PhoneEvents.charging(-1))
    @Test fun unpluggedWithPercent() = assertEquals("চার্জ থেকে বের করা হয়েছে, এখন 80%।", PhoneEvents.unplugged(80))
}
