package com.Lia.assistant.agent.social

enum class Platform { INSTAGRAM, FACEBOOK }

enum class SocialAction { CREATE_POST, CREATE_REEL, CREATE_STORY, TEXT_POST }

/** DRAFT is the default. A draft never reaches the publish tap. */
enum class TaskMode { PUBLISH, DRAFT }

sealed class ParseResult {
    data class Valid(val request: SocialTaskRequest) : ParseResult()
    data class Invalid(val reason: String) : ParseResult()
}

/**
 * A validated request from the model. Anything missing or unsafe is refused here, before any task exists.
 * Media must be a content:// URI shared into Lia, never a file path or a web link.
 */
data class SocialTaskRequest(
    val platform: Platform,
    val action: SocialAction,
    val mediaUri: String?,
    val caption: String,
    val mode: TaskMode,
    val targetAccount: String?,
) {
    companion object {
        const val MAX_CAPTION = 2200

        fun parse(
            platform: String?,
            action: String?,
            mediaUri: String?,
            caption: String?,
            mode: String?,
            targetAccount: String?,
        ): ParseResult {
            val p = platform?.uppercase()?.let { runCatching { Platform.valueOf(it) }.getOrNull() }
                ?: return ParseResult.Invalid("unknown platform")
            val a = action?.uppercase()?.let { runCatching { SocialAction.valueOf(it) }.getOrNull() }
                ?: return ParseResult.Invalid("unknown action")
            val m = if (mode.isNullOrBlank()) {
                TaskMode.DRAFT
            } else {
                mode.uppercase().let { runCatching { TaskMode.valueOf(it) }.getOrNull() }
                    ?: return ParseResult.Invalid("unknown mode")
            }
            val text = caption.orEmpty().trim()
            if (text.length > MAX_CAPTION) return ParseResult.Invalid("caption too long")
            val media = mediaUri?.trim()?.takeIf { it.isNotEmpty() }

            if (a == SocialAction.TEXT_POST) {
                if (p == Platform.INSTAGRAM) return ParseResult.Invalid("text-only posts are not available on Instagram")
                if (text.isEmpty()) return ParseResult.Invalid("text post needs text")
                if (media != null) return ParseResult.Invalid("text post cannot have media")
            } else {
                if (media == null) return ParseResult.Invalid("media is required")
                if (!media.startsWith("content://")) return ParseResult.Invalid("media must be shared into Lia")
            }
            return ParseResult.Valid(
                SocialTaskRequest(p, a, media, text, m, targetAccount?.trim()?.takeIf { it.isNotEmpty() })
            )
        }
    }
}
