package com.Lia.assistant.agent.core

/** Result of one action sent to the device. [STALE] means the screen changed since it was observed. */
enum class DriverResult { OK, FAILED, STALE, NOT_FOUND, UNSUPPORTED }

enum class ActionMethod { ACCESSIBILITY_CLICK, GESTURE_ON_ELEMENT_BOUNDS, VISION_TAP, SET_TEXT }

/** Something that needs the user before the agent can continue. */
enum class Blocker { LOGIN, CAPTCHA, TWO_FACTOR, SECURITY_CHECK, PERMISSION_PROMPT }

enum class FailureReason { TARGET_NOT_FOUND, TARGET_CHANGED, NO_EFFECT, TIMEOUT, DRIVER_ERROR }

data class UiElement(
    val id: String,
    val label: String,
    val clickable: Boolean,
    val editable: Boolean,
    val checked: Boolean? = null,
    val x: Int = -1,
    val y: Int = -1,
    val scrollable: Boolean = false,
    val longClickable: Boolean = false,
)

data class ScreenObservation(
    val observationId: Long,
    val packageName: String,
    val elements: List<UiElement>,
)

/** The only boundary with the device. Tests use a scripted fake. */
interface ScreenDriver {
    fun capture(): ScreenObservation?
    fun clickElement(observationId: Long, elementId: String): DriverResult
    fun setText(observationId: Long, elementId: String, text: String): DriverResult
    fun back(): DriverResult
    fun scroll(down: Boolean): DriverResult
}
