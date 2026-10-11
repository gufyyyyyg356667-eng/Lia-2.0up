package com.Lia.assistant.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickToggleRulesTest {
    @Test fun settingNamesAreRecognised() {
        assertEquals("wifi", QuickToggleRules.lookup("Wi-Fi")?.key)
        assertEquals("wifi", QuickToggleRules.lookup("wifi")?.key)
        assertEquals("airplane", QuickToggleRules.lookup("airplane mode")?.key)
        assertNull(QuickToggleRules.lookup("factory reset"))
        assertNull(QuickToggleRules.lookup("  "))
    }

    @Test fun stateComesFromSwitchOrLabel() {
        assertEquals(false, QuickToggleRules.stateOf("Bluetooth, Off", null))
        assertEquals(false, QuickToggleRules.stateOf("Wi-Fi, On", false))
        assertNull(QuickToggleRules.stateOf("Internet", null))
    }

    @Test fun wantedParsesOnAndOff() {
        assertEquals(true, QuickToggleRules.wanted("on"))
        assertEquals(false, QuickToggleRules.wanted("OFF"))
        assertNull(QuickToggleRules.wanted("toggle"))
    }
}
