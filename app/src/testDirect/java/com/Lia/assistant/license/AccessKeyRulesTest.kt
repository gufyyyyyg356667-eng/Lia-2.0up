package com.Lia.assistant.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessKeyRulesTest {
    private val now = 1_000_000L

    @Test fun notConfiguredWinsOverEverything() =
        assertEquals(KeyStatus.NOT_CONFIGURED, AccessKeyRules.decide(false, true, null, false, now))

    @Test fun noKeyMeansNone() = assertEquals(KeyStatus.NONE, AccessKeyRules.decide(true, false, null, false, now))

    @Test fun blockedOverridesValidExpiry() =
        assertEquals(KeyStatus.BLOCKED, AccessKeyRules.decide(true, true, now + 10_000, true, now))

    @Test fun expiredAtOrAfterExpiry() {
        assertEquals(KeyStatus.EXPIRED, AccessKeyRules.decide(true, true, now, false, now))
        assertEquals(KeyStatus.EXPIRED, AccessKeyRules.decide(true, true, now - 1, false, now))
    }

    @Test fun activeBeforeExpiryOrWithoutExpiry() {
        assertEquals(KeyStatus.ACTIVE, AccessKeyRules.decide(true, true, now + 1, false, now))
        assertEquals(KeyStatus.ACTIVE, AccessKeyRules.decide(true, true, null, false, now))
    }

    @Test fun everyStatusHasMessage() {
        for (s in KeyStatus.entries) assertTrue(AccessKeyRules.message(s).isNotBlank())
        assertFalse(AccessKeyRules.message(KeyStatus.BLOCKED).isEmpty())
        assertEquals("Access key is active.", AccessKeyRules.message(KeyStatus.ACTIVE))
    }
}
