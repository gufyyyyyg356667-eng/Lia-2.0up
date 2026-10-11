package com.Lia.assistant.alerts

import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryGuideTest {
    @Test fun xiaomiGetsAutostartAdvice() = assertTrue(BatteryGuide.message("Xiaomi").contains("Autostart"))
    @Test fun oppoGetsBackgroundAdvice() = assertTrue(BatteryGuide.message("OPPO").contains("Allow background activity"))
    @Test fun vivoGetsIManagerAdvice() = assertTrue(BatteryGuide.message("vivo").contains("iManager"))
    @Test fun otherPhonesGetGenericAdvice() = assertTrue(BatteryGuide.message("samsung").contains("Unrestricted"))
}
