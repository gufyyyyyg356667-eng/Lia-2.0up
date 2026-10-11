package com.Lia.assistant.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"
    const val HOME = "home"
    const val CHAT_ROUTE = "chat?id={id}"
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val VOICE = "voice"
    const val QUICK_ACTIONS = "quick_actions"
    const val PROFILE = "profile"
    const val PERSONALITY = "personality"
    const val ORB_STYLE = "orb_style"
    const val PERMISSIONS = "permissions"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    const val DEBUG = "debug"
    const val FORGE = "forge"
    const val MONITOR = "monitor"

    /** Routes that show the floating bottom bar. */
    val barRoutes = setOf(HOME, CHAT, HISTORY, SETTINGS)

    fun chat(id: String? = null): String = if (id == null) CHAT else "$CHAT?id=$id"
}
