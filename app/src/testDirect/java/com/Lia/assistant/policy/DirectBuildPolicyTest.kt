package com.Lia.assistant.policy

import org.junit.Assert.assertTrue
import org.junit.Test

/** The direct build must contain the screen-control and licensing code. */
class DirectBuildPolicyTest {
    @Test fun directOnlyCodeIsPresent() {
        listOf(
            "com.Lia.assistant.agent.core.TaskStateMachine",
            "com.Lia.assistant.agent.social.SocialTask",
            "com.Lia.assistant.license.AccessKeyManager",
        ).forEach { name ->
            assertTrue("$name should be in the direct build", Class.forName(name) != null)
        }
    }
}
