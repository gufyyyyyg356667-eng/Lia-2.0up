package com.Lia.assistant.access

import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenFinderTest {
    private val screen = ScreenObservation(
        1, "com.example",
        listOf(
            UiElement("e0", "Share now", clickable = true, editable = false),
            UiElement("e1", "Share", clickable = true, editable = false),
            UiElement("e2", "Share", clickable = false, editable = false),
            UiElement("e3", "Caption", clickable = false, editable = true),
        ),
    )

    @Test fun exactLabelWinsOverContains() = assertEquals("e1", ScreenFinder.find(screen, "share")?.id)
    @Test fun containsIsUsedWhenNoExactMatch() = assertEquals("e0", ScreenFinder.find(screen, "now")?.id)
    @Test fun nonClickableIgnoredByDefault() = assertNull(ScreenFinder.find(screen, "caption"))
    @Test fun firstEditableFound() = assertEquals("e3", ScreenFinder.firstEditable(screen)?.id)
    @Test fun emptyQueryFindsNothing() = assertNull(ScreenFinder.find(screen, "  "))

    @Test fun formatMarksKinds() {
        val lines = ScreenFormat.lines(screen)
        assertEquals("[button] Share now", lines[0])
        assertEquals("[input] Caption", lines[3])
    }
}
