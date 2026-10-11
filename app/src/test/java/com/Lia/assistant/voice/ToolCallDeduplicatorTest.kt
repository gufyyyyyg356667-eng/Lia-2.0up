package com.Lia.assistant.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallDeduplicatorTest {
    @Test fun repeatedIdIsSkipped() {
        val d = ToolCallDeduplicator()
        assertTrue(d.firstTime("a"))
        assertFalse(d.firstTime("a"))
        assertTrue(d.firstTime("b"))
    }

    @Test fun oldestIdIsForgottenWhenFull() {
        val d = ToolCallDeduplicator(capacity = 2)
        d.firstTime("a"); d.firstTime("b"); d.firstTime("c")
        assertTrue("a should be forgotten once capacity is exceeded", d.firstTime("a"))
    }
}
