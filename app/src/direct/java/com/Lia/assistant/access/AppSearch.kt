package com.Lia.assistant.access

import android.content.Context
import com.Lia.assistant.agent.core.DriverResult
import com.Lia.assistant.agent.core.ScreenDriver
import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement
import org.json.JSONObject

/** Automation helpers: finding things that are not on the first screen, and searching inside an app. */
object AppSearch {
    private val searchWords = listOf("search", "সার্চ", "find")

    /** Looks for [text] on screen, scrolling down up to 4 times if it is not visible yet. */
    fun findWithScroll(driver: ScreenDriver, text: String): Pair<ScreenObservation, UiElement>? {
        var obs = driver.capture() ?: return null
        for (attempt in 0..4) {
            ScreenFinder.find(obs, text)?.let { return obs to it }
            if (attempt == 4) break
            if (driver.scroll(down = true) != DriverResult.OK) break
            Thread.sleep(400)
            obs = driver.capture() ?: return null
        }
        return null
    }

    /** Searches inside the open app: taps the search control if needed, waits for the field, types, presses Enter. */
    fun searchInApp(driver: ScreenDriver, query: String): JSONObject {
        val q = query.trim()
        if (q.isEmpty()) return JSONObject().put("result", "nothing_to_search")
        var obs = driver.capture() ?: return JSONObject().put("result", "failed")

        val existing = ScreenFinder.firstEditable(obs)
        val field: UiElement
        if (existing != null) {
            field = existing
        } else {
            val button = searchWords.firstNotNullOfOrNull { ScreenFinder.find(obs, it) }
                ?: return JSONObject().put("result", "search_not_found")
            if (driver.clickElement(obs.observationId, button.id) != DriverResult.OK) {
                return JSONObject().put("result", "search_tap_failed")
            }
            val appeared = waitForField(driver, timeoutMs = 4000)
                ?: return JSONObject().put("result", "no_search_field")
            obs = appeared.first
            field = appeared.second
        }

        // Typing into a fresh observation so the element id is current.
        if (driver.setText(obs.observationId, field.id, q) != DriverResult.OK) {
            return JSONObject().put("result", "no_search_field")
        }
        val enter = NovaAccessibilityService.instance?.pressEnter() ?: DriverResult.UNSUPPORTED
        val submitted = enter == DriverResult.OK
        return JSONObject()
            .put("result", "searched")
            .put("message", if (submitted) "Searched for $q." else "Typed $q. Press Enter or tap the search result.")
    }

    /** Taps labelled buttons in order, e.g. "8,0,0,+,7,0,0,=". Stops at the first button it cannot find. */
    fun tapSequence(driver: ScreenDriver, labelsCsv: String): JSONObject {
        val labels = labelsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (labels.isEmpty()) return JSONObject().put("result", "nothing_to_tap")
        if (labels.any { ActionSafety.needsConfirmation(it) }) {
            return JSONObject().put("result", "needs_confirmation")
                .put("message", "One of these buttons needs the user's yes first: ${labels.joinToString(", ")}")
        }
        var done = 0
        for (label in labels) {
            val obs = driver.capture() ?: return JSONObject().put("result", "failed").put("message", "Could not read the screen at step ${done + 1}.")
            val target = ScreenFinder.find(obs, label)
                ?: return JSONObject().put("result", "element_not_found")
                    .put("message", "Could not find button \"$label\" (step ${done + 1} of ${labels.size}).")
            if (driver.clickElement(obs.observationId, target.id) != DriverResult.OK) {
                return JSONObject().put("result", "tap_failed").put("message", "Button \"$label\" did not respond (step ${done + 1}).")
            }
            done++
            Thread.sleep(250)
        }
        return JSONObject().put("result", "tapped").put("message", "Tapped $done buttons in order.")
    }

    private fun waitForField(driver: ScreenDriver, timeoutMs: Long): Pair<ScreenObservation, UiElement>? {
        val until = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < until) {
            val obs = driver.capture()
            if (obs != null) {
                ScreenFinder.firstEditable(obs)?.let { return obs to it }
            }
            Thread.sleep(300)
        }
        return null
    }
}
