package com.Lia.assistant.access

/** Pure rules for finding and reading quick-settings tiles. No Android types, so they are unit-tested. */
object QuickToggleRules {

    /** The labels a tile may carry on different phones. */
    class Entry(val key: String, val labels: List<String>)

    private val table: List<Pair<List<String>, Entry>> = listOf(
        listOf("wifi", "wlan", "internet") to Entry("wifi", listOf("wi-fi", "wifi", "internet")),
        listOf("bluetooth", "bt") to Entry("bluetooth", listOf("bluetooth")),
        listOf("mobiledata", "cellulardata", "mobilenetwork") to Entry("mobile_data", listOf("mobile data", "cellular data", "mobile network")),
        listOf("airplane", "airplanemode", "flightmode", "flight") to Entry("airplane", listOf("airplane mode", "flight mode", "airplane")),
        listOf("hotspot", "mobilehotspot", "tethering") to Entry("hotspot", listOf("hotspot")),
        listOf("donotdisturb", "dnd") to Entry("dnd", listOf("do not disturb", "dnd")),
        listOf("location", "gps") to Entry("location", listOf("location")),
        listOf("batterysaver", "powersaving", "powersaver") to Entry("battery_saver", listOf("battery saver", "power saving", "power saver")),
        listOf("nfc") to Entry("nfc", listOf("nfc")),
        listOf("darkmode", "darktheme", "nightmode") to Entry("dark_mode", listOf("dark theme", "dark mode")),
        listOf("nightlight", "eyecomfort", "bluelightfilter") to Entry("night_light", listOf("night light", "eye comfort", "blue light filter")),
        listOf("autorotate", "rotation") to Entry("auto_rotate", listOf("auto-rotate", "auto rotate", "rotation")),
        listOf("screenrecord", "screenrecorder") to Entry("screen_record", listOf("screen record", "screen recorder")),
        listOf("cast", "screencast") to Entry("cast", listOf("screen cast", "cast")),
    )

    /** Finds the tile description for a spoken setting name like "Wi-Fi", "wifi" or "mobile data". */
    fun lookup(name: String): Entry? {
        val key = name.lowercase().filter { it.isLetter() }
        if (key.isEmpty()) return null
        return table.firstOrNull { key in it.first }?.second
    }

    /** True for on, false for off, null when the label does not say. A switch state wins over the words. */
    fun stateOf(label: String, checked: Boolean?): Boolean? {
        if (checked != null) return checked
        val l = label.lowercase()
        if (Regex("\\b(off|disabled)\\b").containsMatchIn(l)) return false
        if (Regex("\\b(on|enabled)\\b").containsMatchIn(l)) return true
        return null
    }

    /** "on" and "off" are wanted states; anything else means flip it. */
    fun wanted(state: String): Boolean? = when (state.trim().lowercase()) {
        "on", "true", "enable", "enabled" -> true
        "off", "false", "disable", "disabled" -> false
        else -> null
    }
}
