package com.Lia.assistant.access

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRulesTest {
    @Test fun answerHasAcceptAlias() = assertTrue(CallRules.labels("answer")!!.contains("accept"))
    @Test fun actionIsCaseInsensitive() = assertTrue(CallRules.labels("END")!!.contains("hang up"))
    @Test fun unknownActionIsNull() = assertNull(CallRules.labels("explode"))
    @Test fun cameraButtonsAreListed() {
        assertTrue(CallRules.shutterLabels.contains("shutter"))
        assertTrue(CallRules.flipLabels.contains("selfie"))
    }
}
