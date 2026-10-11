package com.Lia.assistant.access

import com.Lia.assistant.agent.core.DriverResult
import com.Lia.assistant.agent.core.ScreenDriver
import com.Lia.assistant.agent.core.ScreenObservation

/** Production driver. Refuses actions on any observation that is no longer the latest (STALE). */
class AccessibilityScreenDriver : ScreenDriver {
    private val service: NovaAccessibilityService?
        get() = NovaAccessibilityService.instance

    override fun capture(): ScreenObservation? = service?.readScreen()

    override fun clickElement(observationId: Long, elementId: String): DriverResult {
        val s = service ?: return DriverResult.UNSUPPORTED
        if (s.latestObservationId != observationId) return DriverResult.STALE
        return s.click(elementId)
    }

    override fun setText(observationId: Long, elementId: String, text: String): DriverResult {
        val s = service ?: return DriverResult.UNSUPPORTED
        if (s.latestObservationId != observationId) return DriverResult.STALE
        return s.setText(elementId, text)
    }

    override fun back(): DriverResult = service?.back() ?: DriverResult.UNSUPPORTED

    override fun scroll(down: Boolean): DriverResult = service?.scroll(down) ?: DriverResult.UNSUPPORTED
}
