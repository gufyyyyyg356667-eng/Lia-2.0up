package com.Lia.assistant.forge

/** Clean-up for generated pages. Pure string work. */
object ForgeHtml {

    /** Removes a leading ```html fence and a trailing ``` fence. */
    fun stripFences(input: String): String =
        input.trim()
            .replace(Regex("^```[a-zA-Z]*\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

    fun looksLikeHtml(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("<html") || lower.contains("<!doctype") || lower.contains("<body")
    }

    /** Closes script, style, body and html tags left open by a cut-off stream. */
    fun repairTruncated(input: String): String {
        var s = input.trimEnd()
        for (tag in listOf("script", "style")) {
            val opens = Regex("<$tag\\b", RegexOption.IGNORE_CASE).findAll(s).count()
            val closes = Regex("</$tag>", RegexOption.IGNORE_CASE).findAll(s).count()
            if (opens > closes) s += "\n</$tag>"
        }
        for (tag in listOf("body", "html")) {
            val opened = Regex("<$tag\\b", RegexOption.IGNORE_CASE).containsMatchIn(s)
            if (opened && !Regex("</$tag>", RegexOption.IGNORE_CASE).containsMatchIn(s)) s += "\n</$tag>"
        }
        if (!s.lowercase().contains("</html>")) s += "\n</html>"
        return s
    }

    /**
     * Adds a script that fades in elements stuck at opacity 0 inside the viewport after 1.4 s.
     * Idempotent: a page that already has it is returned unchanged.
     */
    fun withSafetyNet(html: String): String {
        if (html.contains(SAFETY_ID)) return html
        val script = """
<script id="$SAFETY_ID">
(function () {
  if (window.__liaSafety) return;
  window.__liaSafety = true;
  setTimeout(function () {
    var els = document.querySelectorAll('body *');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      if (el.closest && el.closest('[aria-hidden="true"]')) continue;
      var tag = el.tagName;
      if (tag === 'CANVAS' || tag === 'SCRIPT' || tag === 'STYLE') continue;
      var cs = getComputedStyle(el);
      if (cs.position === 'fixed' || cs.pointerEvents === 'none') continue;
      if (parseFloat(cs.opacity) !== 0) continue;
      var r = el.getBoundingClientRect();
      if (r.width === 0 || r.height === 0 || r.bottom < 0 || r.top > window.innerHeight) continue;
      el.style.opacity = '1';
      el.style.transition = 'opacity 0.4s';
    }
  }, 1400);
})();
</script>
"""
        val idx = html.lastIndexOf("</body>", ignoreCase = true)
        return if (idx >= 0) html.substring(0, idx) + script + html.substring(idx)
        else html + script
    }

    const val SAFETY_ID = "lia-safety-net"
}
