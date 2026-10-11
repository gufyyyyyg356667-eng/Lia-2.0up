package com.Lia.assistant.agent.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskStateMachineTest {

    @Test fun happyPathIsAccepted() {
        val m = TaskStateMachine()
        listOf(
            AgentState.OPENING_APP, AgentState.OBSERVING, AgentState.RESOLVING_TARGET,
            AgentState.VALIDATING_TARGET, AgentState.EXECUTING_ACTION, AgentState.VERIFYING,
            AgentState.COMPLETED,
        ).forEach { m.moveTo(it) }
        assertEquals(AgentState.COMPLETED, m.state)
        assertEquals(7, m.history.size)
    }

    @Test fun illegalMoveThrows() {
        val m = TaskStateMachine()
        try {
            m.moveTo(AgentState.OBSERVING)
            fail("IDLE -> OBSERVING must be refused")
        } catch (e: IllegalTransitionException) {
            assertEquals(AgentState.IDLE, m.state)
        }
    }

    @Test fun terminalStatesNeverExit() {
        val paths = mapOf(
            AgentState.FAILED to listOf(AgentState.OPENING_APP, AgentState.FAILED),
            AgentState.CANCELLED to listOf(AgentState.CANCELLED),
            AgentState.COMPLETED to listOf(
                AgentState.OPENING_APP, AgentState.OBSERVING, AgentState.RESOLVING_TARGET,
                AgentState.VALIDATING_TARGET, AgentState.COMPLETED,
            ),
        )
        for ((terminal, path) in paths) {
            val m = TaskStateMachine()
            path.forEach { m.moveTo(it) }
            assertEquals(terminal, m.state)
            for (next in AgentState.entries) {
                assertFalse("$terminal must not move to $next", m.canMove(next))
            }
        }
    }

    @Test fun failedAndCancelledReachableFromEveryNonTerminalState() {
        for (s in AgentState.entries.filterNot { it.isTerminal }) {
            val m = TaskStateMachine()
            // Walk to s with legal moves only from IDLE, using a breadth-first search.
            val path = pathTo(s)
            path.forEach { m.moveTo(it) }
            assertEquals(s, m.state)
            assertTrue("FAILED from $s", m.canMove(AgentState.FAILED))
            assertTrue("CANCELLED from $s", m.canMove(AgentState.CANCELLED))
        }
    }

    @Test fun blockerPauseAndResume() {
        val m = TaskStateMachine()
        listOf(AgentState.OPENING_APP, AgentState.OBSERVING, AgentState.WAITING_FOR_USER, AgentState.OBSERVING)
            .forEach { m.moveTo(it) }
        assertEquals(AgentState.OBSERVING, m.state)
    }

    @Test fun historyRecordsEveryMove() {
        val m = TaskStateMachine()
        m.moveTo(AgentState.OPENING_APP)
        m.moveTo(AgentState.CANCELLED)
        assertEquals(listOf(AgentState.IDLE to AgentState.OPENING_APP, AgentState.OPENING_APP to AgentState.CANCELLED), m.history)
    }

    /** Shortest legal path from IDLE to [target], found by BFS over the machine's own rules. */
    private fun pathTo(target: AgentState): List<AgentState> {
        val queue = ArrayDeque<List<AgentState>>()
        queue.addLast(emptyList())
        val seen = mutableSetOf<AgentState>()
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val m = TaskStateMachine()
            path.forEach { m.moveTo(it) }
            if (m.state == target) return path
            if (!seen.add(m.state)) continue
            for (next in AgentState.entries) {
                if (m.canMove(next) && next != AgentState.FAILED && next != AgentState.CANCELLED) {
                    queue.addLast(path + next)
                }
            }
        }
        error("no path to $target")
    }
}
