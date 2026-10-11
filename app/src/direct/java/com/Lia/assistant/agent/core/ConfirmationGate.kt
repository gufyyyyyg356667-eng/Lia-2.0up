package com.Lia.assistant.agent.core

/**
 * Guards the irreversible tap. Only an explicit yes from the user, given after a request was made,
 * opens the gate once.
 */
class ConfirmationGate {
    private var awaiting = false

    val isAwaiting: Boolean get() = awaiting

    fun requestConfirmation() {
        awaiting = true
    }

    /** Returns true only if a confirmation was requested and [explicitYes] is true. */
    fun confirm(explicitYes: Boolean): Boolean {
        if (!awaiting || !explicitYes) return false
        awaiting = false
        return true
    }

    fun reject() {
        awaiting = false
    }
}
