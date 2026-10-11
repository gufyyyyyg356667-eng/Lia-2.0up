package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolLogTest {
    @Test fun successIsNotAFailure() = assertFalse(ToolLog.isFailure("tapped"))
    @Test fun notFoundIsAFailure() = assertTrue(ToolLog.isFailure("element_not_found"))
    @Test fun askingForYesIsNotAFailure() = assertFalse(ToolLog.isFailure("needs_confirmation"))
    @Test fun offStateIsAFailure() = assertTrue(ToolLog.isFailure("location_off"))
}
