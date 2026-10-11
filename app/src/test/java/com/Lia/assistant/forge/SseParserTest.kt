package com.Lia.assistant.forge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseParserTest {
    @Test fun dataLinesOnly() {
        assertEquals("{\"a\":1}", SseParser.dataPayload("data: {\"a\":1}"))
        assertNull(SseParser.dataPayload("event: message"))
    }

    @Test fun textFromPartsIsConcatenated() {
        val json = """{"candidates":[{"content":{"parts":[{"text":"<html>"},{"text":"<body>"}]}}]}"""
        assertEquals("<html><body>", SseParser.extractText(json))
    }

    @Test fun emptyOrBrokenJsonGivesNoText() {
        assertEquals("", SseParser.extractText("not json"))
        assertEquals("", SseParser.extractText("{}"))
    }

    @Test fun errorMessageIsReturned() {
        assertEquals("The model is overloaded", SseParser.extractError("""{"error":{"message":"The model is overloaded"}}"""))
        assertNull(SseParser.extractError("""{"candidates":[]}"""))
    }

    @Test fun finishReasonOnlyWhenPresent() {
        assertEquals("STOP", SseParser.extractFinishReason("""{"candidates":[{"finishReason":"STOP"}]}"""))
        assertNull(SseParser.extractFinishReason("""{"candidates":[{"content":{}}]}"""))
    }
}
