package com.Lia.assistant.forge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeStageDetectorTest {
    @Test fun headOnlyStaysAtReading() {
        assertEquals(ForgeStage.READING, ForgeStageDetector.detect("<!DOCTYPE html><head><title>x</title>"))
    }

    @Test fun bodyMeansStructure() {
        assertEquals(ForgeStage.STRUCTURE, ForgeStageDetector.detect("<html><body><h1>Cafe</h1>"))
    }

    @Test fun twoSectionsMeanStyling() {
        val html = "<body><header></header><section id=\"menu\"></section>"
        assertEquals(ForgeStage.STYLING, ForgeStageDetector.detect(html))
    }

    @Test fun canvasCountsAsVisuals() {
        val html = "<body><header></header><section></section><canvas id=\"hero\"></canvas>"
        assertEquals(ForgeStage.VISUALS, ForgeStageDetector.detect(html))
    }

    @Test fun gsapMeansMotionAndClosingHtmlMeansFinalizing() {
        assertEquals(ForgeStage.MOTION, ForgeStageDetector.detect("<body><script>gsap.to(a)</script>"))
        assertEquals(ForgeStage.FINALIZING, ForgeStageDetector.detect("<body>x</body></html>"))
    }

    @Test fun sectionNamesComeFromIdOrLabel() {
        val html = "<section id=\"our-menu\"></section><section aria-label=\"Contact us\"></section>"
        assertEquals(listOf("our menu", "Contact us"), ForgeStageDetector.sectionNames(html))
    }

    @Test fun progressNeverGoesBackAndStaysBelowDone() {
        assertTrue(ForgeStageDetector.progress(ForgeStage.STYLING, 0) >= ForgeStageDetector.progress(ForgeStage.STRUCTURE, 0))
        assertTrue(ForgeStageDetector.progress(ForgeStage.FINALIZING, 999_999) <= 0.98f)
    }
}
