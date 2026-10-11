package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMemoryTest {
    @Test fun strongestTapComesFirst() {
        val lines = AppMemory.describe(
            listOf(Triple("dm", "top-right", 2), Triple("search", "top-left", 9)),
            emptyList(),
        )
        assertTrue(lines[0].startsWith("\"search\""))
    }

    @Test fun userNotesAreIncluded() {
        val lines = AppMemory.describe(emptyList(), listOf("DM button is at the top right"))
        assertEquals("Note from the user: DM button is at the top right", lines[0])
    }

    @Test fun regionIsOmittedWhenUnknown() {
        assertEquals("\"ok\" (worked 1 times)", AppMemory.describe(listOf(Triple("ok", "", 1)), emptyList())[0])
    }
}
