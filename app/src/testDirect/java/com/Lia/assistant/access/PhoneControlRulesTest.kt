package com.Lia.assistant.access

import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneControlRulesTest {
    @Test fun settingNamesAreRecognised() {
        assertEquals("wifi", QuickToggleRules.lookup("Wi-Fi")?.key)
        assertEquals("wifi", QuickToggleRules.lookup("wifi")?.key)
        assertEquals("mobile_data", QuickToggleRules.lookup("Mobile data")?.key)
        assertEquals("airplane", QuickToggleRules.lookup("airplane mode")?.key)
        assertEquals("dnd", QuickToggleRules.lookup("Do not disturb")?.key)
        assertNull(QuickToggleRules.lookup("factory reset"))
        assertNull(QuickToggleRules.lookup("  "))
    }

    @Test fun tileStateIsRead() {
        assertEquals(true, QuickToggleRules.stateOf("Wi-Fi,Wi-Fi, On", null))
        assertEquals(false, QuickToggleRules.stateOf("Bluetooth, Off", null))
        assertNull(QuickToggleRules.stateOf("Internet", null))
        assertEquals(false, QuickToggleRules.stateOf("Wi-Fi, On", false)) // a switch state wins
    }

    @Test fun wantedState() {
        assertEquals(true, QuickToggleRules.wanted("on"))
        assertEquals(false, QuickToggleRules.wanted("OFF"))
        assertNull(QuickToggleRules.wanted("toggle"))
    }

    @Test fun morePhrasesNeedConfirmation() {
        assertTrue(ActionSafety.needsConfirmation("Power off"))
        assertTrue(ActionSafety.needsConfirmation("Restart"))
        assertTrue(ActionSafety.needsConfirmation("Clear data"))
        assertTrue(ActionSafety.needsConfirmation("icon: send button"))
        assertFalse(ActionSafety.needsConfirmation("Restarting soon is fine")) // whole word only
    }

    @Test fun newSettingsPanelsAreOfferedButNotSecurityOnes() {
        assertEquals("android.settings.NFC_SETTINGS", ActionSafety.allowedSettings["nfc"])
        assertFalse(ActionSafety.allowedSettings.containsKey("security"))
        assertFalse(ActionSafety.allowedSettings.containsKey("accounts"))
    }

    @Test fun newActionsAreAccepted() {
        listOf("swipe", "tap_xy", "long_press", "double_tap", "drag", "toggle_setting", "read_notifications").forEach {
            val d = AgentDecision.parse("""{"action":"$it","args":{}}""")
            assertTrue("$it should be allowed", d is AgentDecision.Act)
        }
    }

    @Test fun formatShowsSwitchStateAndPosition() {
        val screen = ScreenObservation(
            1, "com.android.systemui",
            listOf(
                UiElement("e0", "Wi-Fi", clickable = true, editable = false, checked = true, x = 20, y = 30),
                UiElement("e1", "Bluetooth", clickable = true, editable = false, checked = false),
                UiElement("e2", "Like #2", clickable = true, editable = false, x = 85, y = 40),
            ),
        )
        val lines = ScreenFormat.lines(screen)
        assertEquals("[switch on] Wi-Fi @20,30", lines[0])
        assertEquals("[switch off] Bluetooth", lines[1])
        assertEquals("[button] Like #2 @85,40", lines[2])
    }

    @Test fun editableByLabelPicksTheNamedFieldAndSkipsSecretOnes() {
        val screen = ScreenObservation(
            1, "com.example",
            listOf(
                UiElement("e0", "Password", clickable = false, editable = true),
                UiElement("e1", "Name", clickable = false, editable = true),
                UiElement("e2", "Phone number", clickable = false, editable = true),
            ),
        )
        assertEquals("e2", ScreenFinder.editableByLabel(screen, "phone")?.id)
        assertEquals("e1", ScreenFinder.editableByLabel(screen, "")?.id)
        assertNull(ScreenFinder.editableByLabel(screen, "password"))
    }
}
