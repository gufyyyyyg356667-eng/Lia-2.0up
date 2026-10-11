package com.Lia.assistant.action

import org.junit.Assert.assertEquals
import org.junit.Test

class BanglaNamesTest {
    @Test fun youtubeInBangla() = assertEquals("YouTube", BanglaNames.toEnglish("ইউটিউব"))
    @Test fun instagramInBangla() = assertEquals("Instagram", BanglaNames.toEnglish("ইনস্টাগ্রাম"))
    @Test fun englishNameUnchanged() = assertEquals("Spotify", BanglaNames.toEnglish("Spotify"))
    @Test fun longerPhraseFindsGoogleMaps() = assertEquals("Google Maps", BanglaNames.toEnglish("গুগল ম্যাপ খোলো"))
}
