package com.Lia.assistant.agent.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialTaskRequestTest {

    private fun parse(platform: String? = "instagram", action: String? = "create_post",
                      media: String? = "content://media/1", caption: String? = "hi", mode: String? = null) =
        SocialTaskRequest.parse(platform, action, media, caption, mode, null)

    @Test fun defaultsToDraft() {
        val r = parse() as ParseResult.Valid
        assertEquals(TaskMode.DRAFT, r.request.mode)
    }

    @Test fun mediaMustBeSharedContentUri() {
        val r = parse(media = "/sdcard/photo.jpg") as ParseResult.Invalid
        assertTrue(r.reason.contains("shared"))
    }

    @Test fun mediaIsRequiredForPosts() {
        assertTrue(parse(media = null) is ParseResult.Invalid)
    }

    @Test fun unknownPlatformAndActionRefused() {
        assertTrue(parse(platform = "tiktok") is ParseResult.Invalid)
        assertTrue(parse(action = "delete_account") is ParseResult.Invalid)
    }

    @Test fun textOnlyPostNotOnInstagram() {
        val r = SocialTaskRequest.parse("instagram", "text_post", null, "hello", null, null)
        assertTrue(r is ParseResult.Invalid)
    }

    @Test fun textPostOnFacebookNeedsText() {
        assertTrue(SocialTaskRequest.parse("facebook", "text_post", null, "  ", null, null) is ParseResult.Invalid)
        assertTrue(SocialTaskRequest.parse("facebook", "text_post", null, "hello", null, null) is ParseResult.Valid)
    }

    @Test fun captionLengthLimited() {
        val long = "a".repeat(SocialTaskRequest.MAX_CAPTION + 1)
        assertTrue(parse(caption = long) is ParseResult.Invalid)
    }
}
