package com.Lia.assistant.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.Lia.assistant.agent.core.DriverResult
import com.Lia.assistant.agent.core.ScreenObservation
import com.Lia.assistant.agent.core.UiElement

/**
 * Reads visible labels from the active window and acts on them.
 * A label's tap target is the nearest clickable parent; if none responds, a tap is sent to the label's position.
 */
class NovaAccessibilityService : AccessibilityService() {

    companion object {
        private const val MAX_ELEMENTS = 200
        private const val MAX_CLIMB = 6
        private const val TAP_MS = 80L
        private const val LONG_PRESS_MS = 800L
        private const val SWIPE_MS = 320L

        @Volatile var instance: NovaAccessibilityService? = null
            private set

        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty()
            val expected = ComponentName(ctx, NovaAccessibilityService::class.java).flattenToString()
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }

    /** What an element id points to: the node that owns the label, its clickable ancestor, and its screen position. */
    private class Target(val label: AccessibilityNodeInfo, val clickable: AccessibilityNodeInfo?, val bounds: Rect)

    private val targets = HashMap<String, Target>()

    @Volatile var latestObservationId: Long = 0
        private set
    private var counter = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /** Describes the last read, for the log: how many labels were found and how many were dropped. */
    @Volatile var lastStats: String = "not read yet"
        private set

    /**
     * Snapshot of labels on screen. First keeps only visible labels with a real size. If that finds
     * nothing (some apps report unusual visibility), it reads again without those checks.
     */
    @Synchronized
    fun readScreen(): ScreenObservation? {
        val root = rootInActiveWindow ?: run {
            lastStats = "no active window"
            return null
        }
        var elements = collect(root, strict = true)
        var mode = "visible"
        if (elements.isEmpty()) {
            elements = collect(root, strict = false)
            mode = "lenient"
        }
        counter++
        latestObservationId = counter
        lastStats = "mode=$mode, labels=${elements.size}"
        return ScreenObservation(counter, root.packageName?.toString().orEmpty(), elements)
    }

    private fun collect(root: AccessibilityNodeInfo, strict: Boolean): List<UiElement> {
        targets.clear()
        val elements = mutableListOf<UiElement>()
        val seenLabels = HashSet<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty() && elements.size < MAX_ELEMENTS) {
            val node = queue.removeFirst()
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.addLast(it) }
            if (strict && !node.isVisibleToUser) continue

            val raw = (node.text ?: node.contentDescription)?.toString()?.trim().orEmpty()
            // Password fields are shown as a generic label so their contents are never sent to the model.
            val label = if (node.isPassword) "Password field" else raw
            if (label.isEmpty() || !seenLabels.add(label.lowercase())) continue

            val bounds = Rect().also { node.getBoundsInScreen(it) }
            if (strict && (bounds.width() <= 0 || bounds.height() <= 0)) continue

            val clickable = clickableAncestor(node)
            val id = "e${elements.size}"
            targets[id] = Target(node, clickable, bounds)
            elements += UiElement(
                id, label.take(60), clickable != null, node.isEditable,
                checked = if (node.isCheckable) node.isChecked else null,
                x = percentOf(bounds.centerX(), screenWidth()), y = percentOf(bounds.centerY(), screenHeight()),
                scrollable = node.isScrollable, longClickable = node.isLongClickable,
            )
        }
        return elements
    }

    /** The node itself if it can be clicked, otherwise the nearest clickable parent. */
    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var climbed = 0
        while (current != null && climbed <= MAX_CLIMB) {
            if (current.isClickable) return current
            current = current.parent
            climbed++
        }
        return null
    }

    /** Where an element sits on screen, e.g. "top-right". Null if unknown. Uses only its position. */
    fun regionOf(elementId: String): String? {
        val target = targets[elementId] ?: return null
        val root = rootInActiveWindow ?: return null
        val screen = Rect()
        root.getBoundsInScreen(screen)
        if (screen.width() <= 0 || screen.height() <= 0) return null
        val x = (target.bounds.centerX() - screen.left).toFloat() / screen.width()
        val y = (target.bounds.centerY() - screen.top).toFloat() / screen.height()
        val vertical = when { y < 0.33f -> "top"; y < 0.66f -> "middle"; else -> "bottom" }
        val horizontal = when { x < 0.33f -> "left"; x < 0.66f -> "center"; else -> "right" }
        return "$vertical-$horizontal"
    }

    @Synchronized
    fun click(elementId: String): DriverResult {
        val target = targets[elementId] ?: return DriverResult.NOT_FOUND
        target.clickable?.let { if (it.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return DriverResult.OK }
        // Nothing clickable responded: tap the label's position on screen.
        return if (tapAt(target.bounds.exactCenterX(), target.bounds.exactCenterY())) DriverResult.OK else DriverResult.FAILED
    }

    private fun tapAt(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val path = Path().apply { moveTo(x, y); lineTo(x + 1f, y + 1f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    @Synchronized
    fun setText(elementId: String, text: String): DriverResult {
        val target = targets[elementId] ?: return DriverResult.NOT_FOUND
        val node = target.label
        if (!node.isEditable) return DriverResult.UNSUPPORTED
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) DriverResult.OK else DriverResult.FAILED
    }

    /** Presses the Enter / search key in the focused text field (Android 11+). */
    fun pressEnter(): DriverResult {
        if (Build.VERSION.SDK_INT < 30) return DriverResult.UNSUPPORTED
        val root = rootInActiveWindow ?: return DriverResult.FAILED
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return DriverResult.NOT_FOUND
        val action = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
        return if (focused.performAction(action)) DriverResult.OK else DriverResult.FAILED
    }

    fun global(action: Int): DriverResult =
        if (performGlobalAction(action)) DriverResult.OK else DriverResult.FAILED

    fun scroll(down: Boolean): DriverResult {
        val root = rootInActiveWindow ?: return DriverResult.FAILED
        val scrollable = findVerticalScrollable(root) ?: findScrollable(root) ?: return DriverResult.UNSUPPORTED
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return if (scrollable.performAction(action)) DriverResult.OK else DriverResult.FAILED
    }

    fun back(): DriverResult = global(GLOBAL_ACTION_BACK)

    fun home(): DriverResult = global(GLOBAL_ACTION_HOME)

    /** The largest visible scrollable area that is taller than it is wide: the main vertical list, not a sideways row. */
    private fun findVerticalScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestArea = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable && node.isVisibleToUser) {
                val r = Rect()
                node.getBoundsInScreen(r)
                if (r.height() > r.width() && r.width() * r.height() > bestArea) {
                    best = node
                    bestArea = r.width() * r.height()
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.addLast(it) }
        }
        return best
    }

    private fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable && node.isVisibleToUser) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.addLast(it) }
        }
        return null
    }

    // ---- gestures and text (used by PhoneControl and ExtraTools) ----

    private fun activeRoot(): AccessibilityNodeInfo? = rootInActiveWindow

    private fun screenWidth(): Int = resources.displayMetrics.widthPixels

    private fun percentOf(px: Int, total: Int): Int = if (total <= 0) -1 else (px * 100 / total).coerceIn(0, 100)

    private fun screenHeight(): Int = resources.displayMetrics.heightPixels

    /** Labels of the elements under a screen position (percent), smallest first. */
    @Synchronized
    fun labelsAt(xPercent: Double, yPercent: Double): List<String> {
        val px = (screenWidth() * xPercent / 100.0).toInt()
        val py = (screenHeight() * yPercent / 100.0).toInt()
        return targets.values
            .filter { it.bounds.contains(px, py) }
            .sortedBy { it.bounds.width().toLong() * it.bounds.height() }
            .take(3)
            .map { (it.label.text ?: it.label.contentDescription)?.toString().orEmpty() }
    }

    /** Long press on an element: its own long-click action if it has one, otherwise a held touch on its position. */
    @Synchronized
    fun longClick(elementId: String): DriverResult {
        val target = targets[elementId] ?: return DriverResult.NOT_FOUND
        var node: AccessibilityNodeInfo? = target.label
        var climbed = 0
        while (node != null && climbed <= MAX_CLIMB) {
            if (node.isLongClickable && node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) return DriverResult.OK
            node = node.parent
            climbed++
        }
        return if (pressAt(target.bounds.exactCenterX(), target.bounds.exactCenterY(), LONG_PRESS_MS)) DriverResult.OK else DriverResult.FAILED
    }

    @Synchronized
    fun doubleClick(elementId: String): DriverResult {
        val target = targets[elementId] ?: return DriverResult.NOT_FOUND
        return doubleTapAt(target.bounds.exactCenterX(), target.bounds.exactCenterY())
    }

    private fun gesture(path: Path, durationMs: Long): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(g, null, null)
    }

    private fun pressAt(x: Float, y: Float, durationMs: Long): Boolean {
        val path = Path().apply { moveTo(x, y); lineTo(x + 1f, y + 1f) }
        return gesture(path, durationMs)
    }

    private fun doubleTapAt(x: Float, y: Float): DriverResult {
        if (!tapAt(x, y)) return DriverResult.FAILED
        Thread.sleep(160)
        return if (tapAt(x, y)) DriverResult.OK else DriverResult.FAILED
    }

    private fun toPx(xPercent: Double, yPercent: Double): Pair<Float, Float> =
        Pair((screenWidth() * xPercent / 100.0).toFloat(), (screenHeight() * yPercent / 100.0).toFloat())

    /** Tap at a position given as percent of the screen (0-100). */
    fun tapPercent(xPercent: Double, yPercent: Double): DriverResult {
        if (xPercent !in 0.0..100.0 || yPercent !in 0.0..100.0) return DriverResult.UNSUPPORTED
        val (x, y) = toPx(xPercent, yPercent)
        return if (tapAt(x, y)) DriverResult.OK else DriverResult.FAILED
    }

    fun longPressPercent(xPercent: Double, yPercent: Double): DriverResult {
        if (xPercent !in 0.0..100.0 || yPercent !in 0.0..100.0) return DriverResult.UNSUPPORTED
        val (x, y) = toPx(xPercent, yPercent)
        return if (pressAt(x, y, LONG_PRESS_MS)) DriverResult.OK else DriverResult.FAILED
    }

    fun doubleTapPercent(xPercent: Double, yPercent: Double): DriverResult {
        if (xPercent !in 0.0..100.0 || yPercent !in 0.0..100.0) return DriverResult.UNSUPPORTED
        val (x, y) = toPx(xPercent, yPercent)
        return doubleTapAt(x, y)
    }

    /** Drags a finger from one position to another (percent of the screen). */
    fun dragPercent(fromX: Double, fromY: Double, toX: Double, toY: Double, durationMs: Long = SWIPE_MS): DriverResult {
        val values = listOf(fromX, fromY, toX, toY)
        if (values.any { it !in 0.0..100.0 }) return DriverResult.UNSUPPORTED
        val (sx, sy) = toPx(fromX, fromY)
        val (ex, ey) = toPx(toX, toY)
        val path = Path().apply { moveTo(sx, sy); lineTo(ex, ey) }
        val wait = durationMs.coerceIn(100L, 3000L)
        val ok = gesture(path, wait)
        if (ok) Thread.sleep(wait + 60)
        return if (ok) DriverResult.OK else DriverResult.FAILED
    }

    /** Swipes in the middle of the screen. [direction] is where the finger moves: "up" scrolls a list down. */
    fun swipe(direction: String): DriverResult {
        val d = direction.lowercase()
        val gesture = when (d) {
            "up" -> dragPercent(50.0, 72.0, 50.0, 28.0)
            "down" -> dragPercent(50.0, 28.0, 50.0, 72.0)
            "left" -> dragPercent(78.0, 50.0, 22.0, 50.0)
            "right" -> dragPercent(22.0, 50.0, 78.0, 50.0)
            else -> return DriverResult.UNSUPPORTED
        }
        if (gesture == DriverResult.OK) return gesture
        // The system refused the gesture: scroll the main list in the same direction instead.
        return when (d) {
            "down" -> scroll(false)
            "up" -> scroll(true)
            else -> gesture
        }
    }

    /** Complete text of one labelled element. Never for password fields. */
    @Synchronized
    fun fullText(elementId: String): String? {
        val node = targets[elementId]?.label ?: return null
        if (node.isPassword) return null
        return (node.text ?: node.contentDescription)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** All visible text on the screen in reading order, without password fields. */
    @Synchronized
    fun fullScreenText(maxChars: Int = 4000): String {
        val root = activeRoot() ?: return ""
        val out = StringBuilder()
        val seen = HashSet<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        while (stack.isNotEmpty() && out.length < maxChars) {
            val node = stack.removeLast()
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
            if (!node.isVisibleToUser || node.isPassword) continue
            val t = (node.text ?: node.contentDescription)?.toString()?.trim().orEmpty()
            if (t.isNotEmpty() && seen.add(t)) out.append(t).append('\n')
        }
        return out.toString().take(maxChars)
    }

    /** The focused text field, unless it is a password field (those are never touched). */
    private fun focusedField(): AccessibilityNodeInfo? {
        val f = activeRoot()?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        return if (f.isPassword) null else f
    }

    fun clearFocusedText(): DriverResult {
        val f = focusedField() ?: return DriverResult.NOT_FOUND
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }
        return if (f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) DriverResult.OK else DriverResult.FAILED
    }

    fun pasteIntoFocused(): DriverResult {
        val f = focusedField() ?: return DriverResult.NOT_FOUND
        return if (f.performAction(AccessibilityNodeInfo.ACTION_PASTE)) DriverResult.OK else DriverResult.FAILED
    }
}
