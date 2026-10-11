package com.Lia.assistant.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskPlanTest {
    @Test fun parsesOrderedSteps() {
        val steps = TaskPlan.parse("""{"tasks":["WhatsApp-এ রহিমকে হ্যালো পাঠাও","YouTube-এ গান চালাও"]}""")
        assertEquals(listOf("WhatsApp-এ রহিমকে হ্যালো পাঠাও", "YouTube-এ গান চালাও"), steps)
    }

    @Test fun ignoresSurroundingTextAndBlankSteps() {
        assertEquals(listOf("Open Settings"), TaskPlan.parse("Here: {\"tasks\":[\"Open Settings\",\"  \"]}"))
    }

    @Test fun capsNumberOfSteps() {
        val many = (1..10).joinToString(",") { "\"step $it\"" }
        assertEquals(TaskPlan.MAX_TASKS, TaskPlan.parse("{\"tasks\":[$many]}")!!.size)
    }

    @Test fun badReplyGivesNull() {
        assertNull(TaskPlan.parse("no json"))
        assertNull(TaskPlan.parse("""{"tasks":[]}"""))
    }
}
