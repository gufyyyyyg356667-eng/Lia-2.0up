package com.Lia.assistant.forge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeErrorsTest {
    @Test fun overloadedIsRetryableAndFriendly() {
        val e = ForgeErrors.fromHttp(503, """{"error":{"status":"UNAVAILABLE"}}""")
        assertTrue(e.retryable)
        assertEquals(ForgeErrors.BUSY_MESSAGE, e.message)
    }

    @Test fun badKeyIsNotRetryable() {
        val e = ForgeErrors.fromHttp(400, null)
        assertFalse(e.retryable)
        assertTrue(e.message!!.contains("API key"))
    }

    @Test fun streamOverloadMapsToBusy() {
        val e = ForgeErrors.fromStream("The model is overloaded. Please try again later.")
        assertTrue(e.retryable)
        assertEquals(ForgeErrors.BUSY_MESSAGE, e.message)
    }

    @Test fun otherStreamErrorsDoNotLeakDetails() {
        val e = ForgeErrors.fromStream("secret internal detail")
        assertFalse(e.retryable)
        assertFalse(e.message!!.contains("secret"))
    }
}
