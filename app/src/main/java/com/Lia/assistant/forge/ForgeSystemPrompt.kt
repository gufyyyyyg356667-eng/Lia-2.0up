package com.Lia.assistant.forge

/** Instructions for the page generator. The model must reply with a single HTML document and nothing else. */
object ForgeSystemPrompt {

    const val RETRY_HINT =
        "Write completely original markup and code. Finish the whole page and end with </html>."

    private const val RULES = """
Reply with ONLY one complete HTML document, starting with <!DOCTYPE html> and ending with </html>.
No commentary, no markdown fences.

Rules:
- Load Tailwind from https://cdn.tailwindcss.com and GSAP from https://cdnjs.cloudflare.com for motion.
- Use 5 or 6 sections: hero, about, features or menu, gallery or proof, call to action, footer.
- Mobile first and responsive. Hamburger navigation on small screens.
- Hero: an animated Three.js object or particle field from https://cdnjs.cloudflare.com. Final call to action: a second small Three.js scene or a scroll-driven 3D transform.
- Everywhere else use CSS 3D: perspective tilt cards, translateZ layers, flip cards.
- Use at most two sections with photos (https://picsum.photos/seed/<word>/1200/800), each with a dark gradient overlay for text.
- Every canvas must be visible and sized. Never hide content that waits for an animation: content is visible from the start.
- Respect prefers-reduced-motion: turn off looping animation and tilt.
- Buttons get a magnetic hover effect. Use a theme colour set and consistent spacing.
- Original copy and markup only. No lorem ipsum.
"""

    fun forNewSite(prompt: String): String =
        "You build animated, 3D-styled single-page websites.\n$RULES\nWebsite to build: ${prompt.trim()}"

    fun forEdit(currentHtml: String, change: String): String =
        "You edit an existing single-page website. Keep the structure and everything the change does not touch.\n" +
            "$RULES\nCurrent page:\n$currentHtml\n\nChange to make: ${change.trim()}"
}
