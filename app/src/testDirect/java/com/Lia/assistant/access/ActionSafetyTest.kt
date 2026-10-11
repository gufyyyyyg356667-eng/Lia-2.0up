package com.Lia.assistant.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionSafetyTest {
    @Test fun irreversibleButtonsNeedConfirmation() {
        assertTrue(ActionSafety.needsConfirmation("Send"))
        assertTrue(ActionSafety.needsConfirmation("Pay now"))
        assertTrue(ActionSafety.needsConfirmation("Uninstall app"))
    }

    @Test fun ordinaryButtonsDoNotNeedConfirmation() {
        assertFalse(ActionSafety.needsConfirmation("Open"))
        assertFalse(ActionSafety.needsConfirmation("Back"))
        assertFalse(ActionSafety.needsConfirmation("Sending tips")) // whole word only
    }

    @Test fun secretFieldsAreDetected() {
        assertTrue(ActionSafety.isSecretField("Password"))
        assertTrue(ActionSafety.isSecretField("Enter OTP"))
        assertTrue(ActionSafety.isSecretField("PIN"))
        assertFalse(ActionSafety.isSecretField("Search"))
    }

    @Test fun onlyWebLinksOpen() {
        assertTrue(ActionSafety.isWebUrl("https://example.com"))
        assertFalse(ActionSafety.isWebUrl("file:///sdcard/x"))
        assertFalse(ActionSafety.isWebUrl("intent://something"))
    }

    @Test fun securitySettingsAreNotOffered() {
        assertFalse(ActionSafety.allowedSettings.containsKey("security"))
        assertFalse(ActionSafety.allowedSettings.containsKey("accounts"))
        assertEquals("android.settings.WIFI_SETTINGS", ActionSafety.allowedSettings["wifi"])
    }
}
