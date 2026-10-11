package com.Lia.assistant.license

/** Outcome of checking the stored key. Pure, so it can be tested without Android or the network. */
enum class KeyStatus { NOT_CONFIGURED, NONE, BLOCKED, EXPIRED, ACTIVE }

object AccessKeyRules {
    fun decide(
        configured: Boolean,
        hasKey: Boolean,
        expiresAtMs: Long?,
        blocked: Boolean,
        nowMs: Long,
    ): KeyStatus = when {
        !configured -> KeyStatus.NOT_CONFIGURED
        !hasKey -> KeyStatus.NONE
        blocked -> KeyStatus.BLOCKED
        expiresAtMs != null && nowMs >= expiresAtMs -> KeyStatus.EXPIRED
        else -> KeyStatus.ACTIVE
    }

    fun message(status: KeyStatus): String = when (status) {
        KeyStatus.ACTIVE -> "Access key is active."
        KeyStatus.NONE -> "Add your access key in Profile to use voice and tools."
        KeyStatus.EXPIRED -> "This access key has expired. Add a new key in Profile."
        KeyStatus.BLOCKED -> "This access key was disabled. Contact the provider."
        KeyStatus.NOT_CONFIGURED -> "Licensing is not set up in this build."
    }
}

data class LicenseInfo(
    val active: Boolean,
    val plan: String,
    val expiresAt: Long?,
    val statusText: String,
    val blocked: Boolean,
    val hasRememberedKey: Boolean,
)
