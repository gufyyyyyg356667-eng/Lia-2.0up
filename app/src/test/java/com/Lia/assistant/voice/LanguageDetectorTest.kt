package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {
    @Test fun devanagariIsHindi() = assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("नमस्ते आप कैसे हो"))
    @Test fun shortHinglish() = assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya haal hai yaar"))
    @Test fun englishQuestion() = assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("what's the weather"))
    @Test fun blankIsUnknown() = assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect("   "))
    @Test fun longHinglish() =
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("mujhe kal ka plan batao, aur kya karna chahiye?"))
}
