package com.Lia.assistant.access

/**
 * Hard limits for screen control. Pure string rules, so they are unit-tested.
 * Irreversible buttons need an explicit yes from the user. Secret fields are never read or filled.
 */
object ActionSafety {
    private val confirmWords = listOf(
        "send", "pay", "buy", "purchase", "delete", "remove", "uninstall", "install", "post",
        "share", "confirm", "transfer", "withdraw", "log out", "logout", "reset", "erase", "factory reset", "power off", "restart", "clear data",
    )

    private val secretWords = listOf(
        "password", "passcode", "pin", "otp", "one-time", "verification code", "cvv", "card number", "security code",
    )

    fun needsConfirmation(label: String): Boolean {
        val l = label.lowercase()
        return confirmWords.any { w -> Regex("\\b${Regex.escape(w)}\\b").containsMatchIn(l) }
    }

    fun isSecretField(label: String): Boolean {
        val l = label.lowercase()
        return secretWords.any { w -> Regex("\\b${Regex.escape(w)}\\b").containsMatchIn(l) }
    }

    /** Settings panels Lia may open. Security, accounts and developer pages are not listed on purpose. */
    val allowedSettings: Map<String, String> = mapOf(
        "wifi" to "android.settings.WIFI_SETTINGS",
        "bluetooth" to "android.settings.BLUETOOTH_SETTINGS",
        "display" to "android.settings.DISPLAY_SETTINGS",
        "sound" to "android.settings.SOUND_SETTINGS",
        "battery" to "android.settings.BATTERY_SAVER_SETTINGS",
        "airplane" to "android.settings.AIRPLANE_MODE_SETTINGS",
        "location" to "android.settings.LOCATION_SOURCE_SETTINGS",
        "apps" to "android.settings.APPLICATION_SETTINGS",
        "hotspot" to "android.settings.TETHER_SETTINGS",
        "nfc" to "android.settings.NFC_SETTINGS",
    )

    fun isWebUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.startsWith("https://") || u.startsWith("http://")
    }
}
