package com.Lia.assistant.access

import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement

/** Pure helpers for choosing and describing elements on a screen. No Android types. */
object ScreenFinder {

    /** Exact label first, then the first label that contains the query. Clickable elements win ties. */
    fun find(observation: ScreenObservation, query: String, needClickable: Boolean = true): UiElement? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        val pool = observation.elements.filter { !needClickable || it.clickable }
        return pool.firstOrNull { it.label.lowercase() == q }
            ?: pool.firstOrNull { it.label.lowercase().contains(q) }
    }

    /** First editable field that is not a password, PIN or code field. */
    fun firstEditable(observation: ScreenObservation): UiElement? =
        observation.elements.firstOrNull { it.editable && !ActionSafety.isSecretField(it.label) }

    /** The editable field whose label matches [field]; password, PIN and code fields are never returned. */
    fun editableByLabel(observation: ScreenObservation, field: String): UiElement? {
        val q = field.trim().lowercase()
        if (q.isEmpty()) return firstEditable(observation)
        val pool = observation.elements.filter { it.editable && !ActionSafety.isSecretField(it.label) }
        return pool.firstOrNull { it.label.lowercase() == q } ?: pool.firstOrNull { it.label.lowercase().contains(q) }
    }
}

object ScreenFormat {
    /**
     * One line per element for the model, e.g. "[button] Share @85,40". "@x,y" is the element's centre as a
     * percent of the screen and is left out when unknown. Switches show their state: "[switch on] Wi-Fi".
     * Capped so the prompt stays small.
     */
    fun lines(observation: ScreenObservation, max: Int = 40): List<String> =
        observation.elements.take(max).map { el ->
            val kind = when {
                el.checked != null -> if (el.checked) "switch on" else "switch off"
                el.editable -> "input"
                el.clickable -> "button"
                else -> "text"
            }
            val where = if (el.x >= 0 && el.y >= 0) " @${el.x},${el.y}" else ""
            "[$kind] ${el.label}$where"
        }
}
