package com.Lia.assistant.forge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeIntentTest {
    @Test fun buildRequestsMatch() {
        assertTrue(ForgeIntent.matches("Lia, ek cafe ki website banao"))
        assertTrue(ForgeIntent.matches("build me a landing page for my bakery"))
        assertTrue(ForgeIntent.matches("make a web page for my yoga class"))
    }

    @Test fun questionsDoNotMatch() {
        assertFalse(ForgeIntent.matches("how do I build a website?"))
        assertFalse(ForgeIntent.matches("what is a landing page"))
        assertFalse(ForgeIntent.matches("kya tum website bana sakti ho?"))
    }

    @Test fun unrelatedBuildTalkDoesNotMatch() {
        assertFalse(ForgeIntent.matches("build a bridge in my garden"))
        assertFalse(ForgeIntent.matches("website kholo"))
        assertFalse(ForgeIntent.matches(""))
    }
}
