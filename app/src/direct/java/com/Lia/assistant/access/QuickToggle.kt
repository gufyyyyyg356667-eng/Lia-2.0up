package com.Lia.assistant.access

import android.accessibilityservice.AccessibilityService
import com.Lia.assistant.agent.core.DriverResult
import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement
import org.json.JSONObject

/**
 * Turns Wi-Fi, Bluetooth, mobile data, airplane mode, hotspot and similar on or off by tapping their
 * tile in quick settings. Android does not let apps flip these directly; the tile is the way.
 */
object QuickToggle {
    private const val PAGES = 3

    private fun result(value: String) = JSONObject().put("result", value)

    private fun findTile(obs: ScreenObservation, entry: QuickToggleRules.Entry): UiElement? =
        entry.labels.firstNotNullOfOrNull { ScreenFinder.find(obs, it) }

    fun run(name: String, state: String): JSONObject {
        val svc = NovaAccessibilityService.instance ?: return result("accessibility_not_enabled")
        val entry = QuickToggleRules.lookup(name) ?: return result("setting_not_supported")
        val want = QuickToggleRules.wanted(state)

        if (svc.global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) != DriverResult.OK) return result("failed")
        Thread.sleep(900)

        var tile: UiElement? = null
        var obs: ScreenObservation? = null
        for (page in 0 until PAGES) {
            obs = svc.readScreen() ?: break
            tile = findTile(obs, entry)
            if (tile != null) break
            // Tiles can be on a second page: swipe sideways and look again.
            svc.swipe("left")
            Thread.sleep(500)
        }
        val found = tile
        val seen = obs
        if (found == null || seen == null) {
            svc.global(AccessibilityService.GLOBAL_ACTION_BACK)
            return result("tile_not_found")
        }

        val before = QuickToggleRules.stateOf(found.label, found.checked)
        if (want != null && before == want) {
            svc.global(AccessibilityService.GLOBAL_ACTION_BACK)
            return result(if (want) "already_on" else "already_off")
        }
        if (svc.click(found.id) != DriverResult.OK) {
            svc.global(AccessibilityService.GLOBAL_ACTION_BACK)
            return result("failed")
        }
        Thread.sleep(800)

        val after = svc.readScreen()?.let { o -> findTile(o, entry) }?.let { QuickToggleRules.stateOf(it.label, it.checked) }
        return when (after) {
            true -> { svc.global(AccessibilityService.GLOBAL_ACTION_BACK); result("turned_on") }
            false -> { svc.global(AccessibilityService.GLOBAL_ACTION_BACK); result("turned_off") }
            // Some phones open a panel instead of flipping the tile. Leave it open so the agent can read it and continue.
            null -> result("tapped_state_unknown").put("message", "Tapped the ${entry.key} tile. Read the screen to check the state or finish in the panel.")
        }
    }
}
