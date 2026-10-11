package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppMatcherTest {
    private val labels = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "calculator" to "com.android.calculator2",
        "chrome" to "com.android.chrome",
    )

    @Test fun exactLabelIgnoresCase() = assertEquals("com.google.android.youtube", AppMatcher.match("YouTube", labels))
    @Test fun prefixMatches() = assertEquals("com.whatsapp", AppMatcher.match("wha", labels))
    @Test fun unknownAppIsNull() = assertNull(AppMatcher.match("xyzzy", labels))
    @Test fun blankIsNull() = assertNull(AppMatcher.match("   ", labels))
}
