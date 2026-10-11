package com.Lia.assistant.policy

import com.Lia.assistant.license.AccessKeyManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Play build must not contain the screen-control agent, and must not gate access by key. */
class PlayBuildPolicyTest {

    @Test fun agentCodeIsAbsent() {
        listOf(
            "com.Lia.assistant.agent.core.TaskStateMachine",
            "com.Lia.assistant.agent.social.SocialTask",
        ).forEach { name ->
            var found = true
            try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                found = false
            }
            assertFalse("$name must not be in the Play build", found)
        }
    }

    @Test fun noKeyGateInPlayBuild() {
        assertTrue(AccessKeyManager.isActive())
    }
}
