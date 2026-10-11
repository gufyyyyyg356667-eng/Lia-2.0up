package com.Lia.assistant.forge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeHtmlTest {
    @Test fun stripsHtmlFences() {
        assertEquals("<html></html>", ForgeHtml.stripFences("```html\n<html></html>\n```"))
        assertEquals("<p>x</p>", ForgeHtml.stripFences("```\n<p>x</p>\n```"))
        assertEquals("<p>x</p>", ForgeHtml.stripFences("<p>x</p>"))
    }

    @Test fun looksLikeHtmlChecksCommonStarts() {
        assertTrue(ForgeHtml.looksLikeHtml("<!DOCTYPE html><html>"))
        assertTrue(ForgeHtml.looksLikeHtml("<body>hi"))
        assertFalse(ForgeHtml.looksLikeHtml("Sorry, I can't do that."))
    }

    @Test fun repairClosesOpenTags() {
        val cut = "<html><body><script>var x = 1;"
        val fixed = ForgeHtml.repairTruncated(cut)
        assertTrue(fixed.contains("</script>"))
        assertTrue(fixed.contains("</body>"))
        assertTrue(fixed.trimEnd().endsWith("</html>"))
    }

    @Test fun repairLeavesCompletePageAlone() {
        val full = "<html><body><p>hi</p></body></html>"
        assertEquals(full, ForgeHtml.repairTruncated(full))
    }

    @Test fun safetyNetIsIdempotent() {
        val page = "<html><body><p>hi</p></body></html>"
        val once = ForgeHtml.withSafetyNet(page)
        assertTrue(once.contains(ForgeHtml.SAFETY_ID))
        assertEquals(once, ForgeHtml.withSafetyNet(once))
    }

    @Test fun safetyNetGoesBeforeBodyClose() {
        val out = ForgeHtml.withSafetyNet("<html><body>x</body></html>")
        assertTrue(out.indexOf(ForgeHtml.SAFETY_ID) < out.indexOf("</body>"))
    }
}
