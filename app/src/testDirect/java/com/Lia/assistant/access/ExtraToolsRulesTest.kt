package com.Lia.assistant.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraToolsRulesTest {
    @Test fun callButtonsAreKnown() {
        assertTrue(CallRules.labels("answer")!!.contains("accept"))
        assertTrue(CallRules.labels("END")!!.contains("hang up"))
        assertNull(CallRules.labels("explode"))
    }

    @Test fun everyExtraToolIsAnAllowedAgentAction() {
        ExtraTools.names.forEach { name ->
            val d = AgentDecision.parse("""{"action":"$name","args":{}}""")
            assertTrue("$name should be allowed", d is AgentDecision.Act)
        }
    }

    @Test fun everyExtraToolIsInThePrompt() {
        val prompt = AgentPrompt.system("Lia")
        ExtraTools.names.forEach { assertTrue("prompt should mention $it", prompt.contains(it)) }
    }

    @Test fun everyNewToolIsDeclaredForVoice() {
        val declared = ScreenToolCatalog.declarations()
        val names = (0 until declared.length()).map { declared.getJSONObject(it).getString("name") }.toSet()
        (ExtraTools.names + PhoneControl.names).forEach { assertTrue("$it should be declared", it in names) }
        assertEquals(names.size, declared.length())
    }
}
