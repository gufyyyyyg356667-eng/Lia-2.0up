package com.Lia.assistant.agent.social

import com.Lia.assistant.agent.core.AgentState
import com.Lia.assistant.agent.core.Blocker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialTaskFlowTest {

    private fun request(mode: TaskMode) = SocialTaskRequest(
        platform = Platform.INSTAGRAM,
        action = SocialAction.CREATE_POST,
        mediaUri = "content://media/1",
        caption = "hello",
        mode = mode,
        targetAccount = null,
    )

    private fun publishTaskAtConfirmation(): SocialTask {
        val t = SocialTask(request(TaskMode.PUBLISH))
        t.start()
        t.appReady()
        t.reachFinalStep()
        return t
    }

    @Test fun publishPausesBeforeTheIrreversibleTap() {
        val t = publishTaskAtConfirmation()
        assertEquals(AgentState.WAITING_FOR_CONFIRMATION, t.machine.state)
        assertNull(t.outcome)
    }

    @Test fun confirmWithoutExplicitYesIsRefused() {
        val t = publishTaskAtConfirmation()
        assertFalse(t.confirm(explicitYes = false))
        assertEquals(AgentState.WAITING_FOR_CONFIRMATION, t.machine.state)
    }

    @Test fun confirmWithoutPendingRequestIsRefused() {
        val t = SocialTask(request(TaskMode.PUBLISH))
        t.start()
        t.appReady()
        assertFalse(t.confirm(explicitYes = true))
    }

    @Test fun explicitYesAllowsPublishTap() {
        val t = publishTaskAtConfirmation()
        assertTrue(t.confirm(explicitYes = true))
        assertEquals(AgentState.VERIFYING, t.machine.state)
    }

    @Test fun confirmCannotBeUsedTwice() {
        val t = publishTaskAtConfirmation()
        assertTrue(t.confirm(true))
        assertFalse(t.confirm(true))
    }

    @Test fun draftNeverPublishes() {
        val t = SocialTask(request(TaskMode.DRAFT))
        t.start()
        t.appReady()
        t.reachFinalStep()
        assertEquals(TaskOutcome.DRAFT_READY, t.outcome)
        assertEquals(AgentState.COMPLETED, t.machine.state)
        assertFalse(t.confirm(explicitYes = true))
    }

    @Test fun unverifiedSubmissionIsReportedAsUnverified() {
        val t = publishTaskAtConfirmation()
        t.confirm(true)
        t.finishVerification(verified = false)
        assertEquals(TaskOutcome.SUBMITTED_UNVERIFIED, t.outcome)
        assertEquals(AgentState.COMPLETED, t.machine.state)
    }

    @Test fun verifiedSubmissionIsPublished() {
        val t = publishTaskAtConfirmation()
        t.confirm(true)
        t.finishVerification(verified = true)
        assertEquals(TaskOutcome.PUBLISHED, t.outcome)
    }

    @Test fun loginScreenPausesTheTask() {
        val t = SocialTask(request(TaskMode.PUBLISH))
        t.start()
        t.appReady()
        t.onScreen(listOf("Log in", "Password"))
        assertEquals(AgentState.WAITING_FOR_USER, t.machine.state)
        assertEquals(Blocker.LOGIN, t.blocker)
    }

    @Test fun captchaScreenPausesTheTask() {
        val t = SocialTask(request(TaskMode.PUBLISH))
        t.start()
        t.appReady()
        t.onScreen(listOf("Confirm you're not a robot", "CAPTCHA"))
        assertEquals(Blocker.CAPTCHA, t.blocker)
    }

    @Test fun resumeAfterBlockerContinuesObserving() {
        val t = SocialTask(request(TaskMode.PUBLISH))
        t.start()
        t.appReady()
        t.onScreen(listOf("Enter verification code"))
        t.resume()
        assertEquals(AgentState.OBSERVING, t.machine.state)
        assertNull(t.blocker)
    }

    @Test fun cancelFromConfirmationEndsTheTask() {
        val t = publishTaskAtConfirmation()
        t.reject()
        assertEquals(AgentState.CANCELLED, t.machine.state)
        assertEquals(TaskOutcome.CANCELLED, t.outcome)
    }

    @Test fun blockerDetectorIgnoresNormalScreens() {
        assertNull(BlockerDetector.detect(listOf("Share", "Caption", "Add location")))
    }
}
