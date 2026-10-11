package com.Lia.assistant.agent.social

import com.Lia.assistant.agent.core.Blocker




/** Spots screens the agent must not get past alone: sign-in, codes, CAPTCHA, security checks. */
object BlockerDetector {
    fun detect(labels: List<String>): Blocker? {
        val text = labels.joinToString(" ").lowercase()
        return when {
            "captcha" in text || "not a robot" in text -> Blocker.CAPTCHA
            "two-factor" in text || "verification code" in text || "2fa" in text -> Blocker.TWO_FACTOR
            "suspicious" in text || "security check" in text -> Blocker.SECURITY_CHECK
            "log in" in text || "login" in text || "password" in text -> Blocker.LOGIN
            "permission" in text && "allow" in text -> Blocker.PERMISSION_PROMPT
            else -> null
        }
    }
}
