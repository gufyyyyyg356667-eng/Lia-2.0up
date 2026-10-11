package com.Lia.assistant.agent.core

enum class AgentState {
    IDLE, OPENING_APP, OBSERVING, RESOLVING_TARGET, VALIDATING_TARGET, EXECUTING_ACTION,
    WAITING_FOR_UI, VERIFYING, NEXT_STEP, RETRY, REOBSERVE, WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER, COMPLETED, FAILED, CANCELLED;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

class IllegalTransitionException(from: AgentState, to: AgentState) :
    IllegalStateException("Illegal transition $from -> $to")

/**
 * Task states with an explicit table of legal moves. FAILED and CANCELLED are reachable from every
 * non-terminal state. Terminal states never move. Every move is kept in [history].
 */
class TaskStateMachine {
    private val base: Map<AgentState, Set<AgentState>> = mapOf(
        AgentState.IDLE to setOf(AgentState.OPENING_APP),
        AgentState.OPENING_APP to setOf(AgentState.OBSERVING, AgentState.WAITING_FOR_USER),
        AgentState.OBSERVING to setOf(AgentState.RESOLVING_TARGET, AgentState.WAITING_FOR_USER),
        AgentState.RESOLVING_TARGET to setOf(AgentState.VALIDATING_TARGET, AgentState.RETRY, AgentState.REOBSERVE),
        AgentState.VALIDATING_TARGET to setOf(
            AgentState.EXECUTING_ACTION, AgentState.WAITING_FOR_CONFIRMATION,
            AgentState.REOBSERVE, AgentState.COMPLETED,
        ),
        AgentState.WAITING_FOR_CONFIRMATION to setOf(AgentState.EXECUTING_ACTION),
        AgentState.EXECUTING_ACTION to setOf(AgentState.WAITING_FOR_UI, AgentState.VERIFYING),
        AgentState.WAITING_FOR_UI to setOf(AgentState.OBSERVING),
        AgentState.VERIFYING to setOf(
            AgentState.NEXT_STEP, AgentState.RETRY, AgentState.WAITING_FOR_USER, AgentState.COMPLETED,
        ),
        AgentState.NEXT_STEP to setOf(AgentState.OBSERVING, AgentState.COMPLETED),
        AgentState.RETRY to setOf(AgentState.REOBSERVE, AgentState.RESOLVING_TARGET),
        AgentState.REOBSERVE to setOf(AgentState.OBSERVING),
        AgentState.WAITING_FOR_USER to setOf(AgentState.OBSERVING),
    )

    private val allowed: Map<AgentState, Set<AgentState>> = AgentState.entries
        .filterNot { it.isTerminal }
        .associateWith { s -> (base[s].orEmpty()) + setOf(AgentState.FAILED, AgentState.CANCELLED) }

    var state: AgentState = AgentState.IDLE
        private set

    private val _history = mutableListOf<Pair<AgentState, AgentState>>()
    val history: List<Pair<AgentState, AgentState>> get() = _history.toList()

    fun canMove(to: AgentState): Boolean = allowed[state]?.contains(to) == true

    fun moveTo(next: AgentState) {
        if (!canMove(next)) throw IllegalTransitionException(state, next)
        _history += state to next
        state = next
    }
}
