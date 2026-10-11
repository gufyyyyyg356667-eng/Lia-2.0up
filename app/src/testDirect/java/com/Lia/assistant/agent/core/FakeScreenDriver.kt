package com.Lia.assistant.agent.core

/** Scripted device. Each capture advances the screen; actions on an older observation report STALE. */
class FakeScreenDriver(private val screens: List<ScreenObservation>) : ScreenDriver {
    private var index = -1
    val clicked = mutableListOf<String>()

    override fun capture(): ScreenObservation? {
        if (index + 1 >= screens.size) return null
        index++
        return screens[index]
    }

    override fun clickElement(observationId: Long, elementId: String): DriverResult {
        if (currentId() != observationId) return DriverResult.STALE
        val element = screens[index].elements.firstOrNull { it.id == elementId } ?: return DriverResult.NOT_FOUND
        if (!element.clickable) return DriverResult.UNSUPPORTED
        clicked += elementId
        return DriverResult.OK
    }

    override fun setText(observationId: Long, elementId: String, text: String): DriverResult =
        if (currentId() != observationId) DriverResult.STALE else DriverResult.UNSUPPORTED

    override fun back(): DriverResult = DriverResult.OK
    override fun scroll(down: Boolean): DriverResult = DriverResult.OK

    private fun currentId(): Long = if (index in screens.indices) screens[index].observationId else -1L
}
