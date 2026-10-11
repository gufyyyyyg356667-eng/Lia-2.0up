package com.Lia.assistant.alerts

/** Short Bangla messages for phone events. Pure, so it is unit-tested. */
object PhoneEvents {
    fun charging(percent: Int): String =
        if (percent >= 100) "ফোন চার্জে বসানো হয়েছে, চার্জ ফুল আছে।"
        else if (percent < 0) "ফোন চার্জে বসানো হয়েছে।"
        else "ফোন চার্জে বসানো হয়েছে, এখন $percent%।"

    fun unplugged(percent: Int): String =
        if (percent < 0) "চার্জ থেকে বের করা হয়েছে।"
        else "চার্জ থেকে বের করা হয়েছে, এখন $percent%।"
}
