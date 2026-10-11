package com.Lia.assistant.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentDecisionTest {
    @Test fun parsesAnAction() {
        val d = AgentDecision.parse("""{"action":"tap_text","args":{"text":"Wi-Fi"}}""") as AgentDecision.Act
        assertEquals("tap_text", d.name)
        assertEquals("Wi-Fi", d.args["text"])
    }

    @Test fun ignoresSurroundingText() {
        val d = AgentDecision.parse("Sure! ```json\n{\"action\":\"go_back\",\"args\":{}}\n```")
        assertTrue(d is AgentDecision.Act)
    }

    @Test fun parsesDoneAndAsk() {
        assertEquals("Opened Wi-Fi.", (AgentDecision.parse("""{"action":"done","summary":"Opened Wi-Fi."}""") as AgentDecision.Done).summary)
        assertEquals("Which song?", (AgentDecision.parse("""{"action":"ask","question":"Which song?"}""") as AgentDecision.Ask).question)
    }

    @Test fun unknownActionIsRejected() {
        assertNull(AgentDecision.parse("""{"action":"delete_account","args":{}}"""))
    }

    @Test fun badOrEmptyReplyIsRejected() {
        assertNull(AgentDecision.parse("no json here"))
        assertNull(AgentDecision.parse("{not valid json}"))
        assertNull(AgentDecision.parse("""{"action":"ask"}"""))
    }

    @Test fun promptNamesEveryAllowedAction() {
        val prompt = AgentPrompt.system("Lia")
        AgentDecision.allowedActions.forEach { name -> assertTrue("prompt should mention $name", prompt.contains(name)) }
    }
}
