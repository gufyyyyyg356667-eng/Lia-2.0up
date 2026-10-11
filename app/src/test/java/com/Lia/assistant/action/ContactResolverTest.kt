package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {
    @Test fun phoneNumbersAreDetected() {
        assertTrue(ContactResolver.looksLikePhone("+91 98765 43210"))
        assertFalse(ContactResolver.looksLikePhone("Mummy"))
        assertFalse(ContactResolver.looksLikePhone("123"))
    }

    @Test fun exactMatchWinsOverContains() {
        val picked = ContactResolver.pick("Rahul", listOf("Rahul Sharma" to "111", "Rahul" to "222"))
        assertEquals("Rahul" to "222", picked)
    }

    @Test fun noMatchIsNull() {
        assertNull(ContactResolver.pick("zzz", listOf("Rahul" to "111")))
    }
}
