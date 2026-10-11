package com.Lia.assistant.agent.social

import com.Lia.assistant.agent.core.AgentState
import com.Lia.assistant.agent.core.Blocker
import com.Lia.assistant.agent.core.ConfirmationGate
import com.Lia.assistant.agent.core.TaskStateMachine

enum class TaskOutcome { DRAFT_READY, PUBLISHED, SUBMITTED_UNVERIFIED, CANCELLED, FAILED }

/**
 * One social task. It owns the state machine and the confirmation gate, and enforces the rules:
 * publishing needs an explicit yes, a draft never publishes, a blocker pauses the task, and an
 * unverified submission is reported as unverified.
 */
class SocialTask(val request: SocialTaskRequest) {
    val machine = TaskStateMachine()
    private val gate = ConfirmationGate()

    var blocker: Blocker? = null
        private set
    var outcome: TaskOutcome? = null
        private set

    fun start() = machine.moveTo(AgentState.OPENING_APP)

    fun appReady() = machine.moveTo(AgentState.OBSERVING)

    /** Call with the labels of the current screen. Pauses the task if a blocker is shown. */
    fun onScreen(labels: List<String>) {
        val found = BlockerDetector.detect(labels) ?: return
        blocker = found
        machine.moveTo(AgentState.WAITING_FOR_USER)
    }

    fun resume() {
        if (machine.state == AgentState.WAITING_FOR_USER) {
            blocker = null
            machine.moveTo(AgentState.OBSERVING)
        }
    }

    /** The agent has found and checked the irreversible control. Draft mode stops here. */
    fun reachFinalStep() {
        machine.moveTo(AgentState.RESOLVING_TARGET)
        machine.moveTo(AgentState.VALIDATING_TARGET)
        if (request.mode == TaskMode.DRAFT) {
            outcome = TaskOutcome.DRAFT_READY
            machine.moveTo(AgentState.COMPLETED)
            return
        }
        gate.requestConfirmation()
        machine.moveTo(AgentState.WAITING_FOR_CONFIRMATION)
    }

    /** Returns true only when the publish tap is allowed to happen. */
    fun confirm(explicitYes: Boolean): Boolean {
        if (machine.state != AgentState.WAITING_FOR_CONFIRMATION) return false
        if (request.mode == TaskMode.DRAFT) return false
        if (!gate.confirm(explicitYes)) return false
        machine.moveTo(AgentState.EXECUTING_ACTION)
        machine.moveTo(AgentState.VERIFYING)
        return true
    }

    fun reject() {
        if (machine.state != AgentState.WAITING_FOR_CONFIRMATION) return
        gate.reject()
        cancel()
    }

    /** Called after the tap. If success cannot be confirmed on screen, report it as unverified. */
    fun finishVerification(verified: Boolean) {
        check(machine.state == AgentState.VERIFYING) { "not verifying" }
        outcome = if (verified) TaskOutcome.PUBLISHED else TaskOutcome.SUBMITTED_UNVERIFIED
        machine.moveTo(AgentState.COMPLETED)
    }

    fun cancel() {
        if (machine.state.isTerminal) return
        outcome = TaskOutcome.CANCELLED
        machine.moveTo(AgentState.CANCELLED)
    }

    fun fail() {
        if (machine.state.isTerminal) return
        outcome = TaskOutcome.FAILED
        machine.moveTo(AgentState.FAILED)
    }
}
