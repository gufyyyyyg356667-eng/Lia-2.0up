package com.Lia.assistant.alerts

/** Bangla steps to stop the phone from closing Lia in the background, by phone maker. Pure. */
object BatteryGuide {
    fun message(manufacturer: String): String {
        val m = manufacturer.lowercase()
        return when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
                "Settings → Apps → Manage apps → Lia: Autostart চালু করুন এবং Battery-তে No restrictions দিন।"
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ->
                "Settings → Battery → App battery management → Lia: Allow background activity চালু করুন।"
            m.contains("vivo") || m.contains("iqoo") ->
                "iManager → App manager → Autostart চালু করুন, আর Background power consumption-এ Lia-কে allow করুন।"
            else ->
                "Settings → Apps → Lia AI → Battery → Unrestricted (বা Don't optimize) সিলেক্ট করুন।"
        }
    }
}
