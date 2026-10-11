package com.Lia.assistant.alerts

/** Short Bangla messages and the rules for when to send them. Pure, so it is unit-tested. */
object MonitorMessages {
    const val LOW_FIRST = 15
    const val LOW_SECOND = 10
    const val RESET_ABOVE = 20

    fun full(): String = "চার্জ ফুল হয়েছে, চার্জার খুলে নিতে পারেন।"

    fun low(percent: Int): String = "ব্যাটারি কম, এখন $percent%। পাওয়ার সেভ চালু করতে পারেন।"

    fun headphones(connected: Boolean): String =
        if (connected) "হেডফোন লাগানো হয়েছে, গান এখন হেডফোনে যাবে।"
        else "হেডফোন খোলা হয়েছে, আওয়াজ এখন স্পিকারে।"

    fun networkLost(): String = "ইন্টারনেট সংযোগ হারিয়ে গেছে।"

    fun networkBack(): String = "ইন্টারনেট সংযোগ ফিরে এসেছে।"

    fun downloadDone(): String = "একটি ডাউনলোড শেষ হয়েছে।"

    fun hot(celsius: Int): String = "ফোন অনেক গরম হয়েছে, এখন প্রায় $celsius ডিগ্রি। ভারী অ্যাপ বন্ধ করুন।"

    fun storageLow(freePercent: Int): String = "স্টোরেজ প্রায় ভরা, খালি আছে $freePercent%। পুরনো ফাইল মুছতে পারেন।"

    fun eventSoon(title: String): String = "১৫ মিনিট পরে: $title।"

    /** Which low-battery level to announce now, or null. Each level is announced once per discharge. */
    fun lowThresholdToAnnounce(percent: Int, announced: Set<Int>): Int? = when {
        percent <= LOW_SECOND && LOW_SECOND !in announced -> LOW_SECOND
        percent <= LOW_FIRST && LOW_FIRST !in announced -> LOW_FIRST
        else -> null
    }
}
