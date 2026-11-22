package dev.tasker2.domain

import dev.tasker2.data.Category
import dev.tasker2.data.Task

/** Pure, Android-free logic so it can be unit tested on the JVM. */
object TaskLogic {

    /** Clamp any value into 0 (none) or 1..10. */
    fun clampPriority(p: Int): Int = p.coerceIn(Task.NO_PRIORITY, Task.MAX_PRIORITY)

    fun hasPriority(p: Int): Boolean = p >= Task.MIN_PRIORITY

    /** Crossed-out (or completed) rows are drawn entirely in neutral grays: no color at all. */
    fun isDimmed(task: Task): Boolean = task.completed || task.crossedOut

    /** Pure neutral grays (R == G == B) used for every element of a crossed-out row. */
    object Gray {
        const val TEXT = 0xFF8C8C8C.toInt()      // title, strike line, number, circle, checkbox border
        const val BADGE_FILL = 0xFF2E2E2E.toInt()
        const val BAR_TRACK = 0xFF3A3A3A.toInt()
        const val CHECK_FILL = 0xFF6E6E6E.toInt() // checked square fill
        const val CHECKMARK = 0xFF1E1E1E.toInt()
        val all = listOf(TEXT, BADGE_FILL, BAR_TRACK, CHECK_FILL, CHECKMARK)
        fun isGray(argb: Int): Boolean = ((argb shr 16) and 0xFF) == ((argb shr 8) and 0xFF) && ((argb shr 8) and 0xFF) == (argb and 0xFF)
    }

    /** Share of the priority color kept in "Faded color" mode (the rest is gray). */
    const val FADE_COLOR_SHARE = 0.22f

    /** Blends [argb] into the neutral gray level [gray] keeping only [share] of the color. Opaque ARGB. */
    fun fadeToward(argb: Int, gray: Int, share: Float = FADE_COLOR_SHARE): Int {
        fun ch(c: Int) = Math.round(gray * (1 - share) + c * share).coerceIn(0, 255)
        return (0xFF shl 24) or (ch((argb shr 16) and 0xFF) shl 16) or (ch((argb shr 8) and 0xFF) shl 8) or ch(argb and 0xFF)
    }

    /** Colors for every part of a crossed-out row. [accent] is the strike line, number, bar and checkbox border. */
    data class CrossedPalette(val text: Int, val accent: Int, val badgeFill: Int, val barTrack: Int, val checkFill: Int, val checkmark: Int)

    /**
     * Gray mode, and any task without a priority: pure gray (R == G == B everywhere).
     * Faded mode with a priority: the same grays with a strongly desaturated hint of the priority color (about 78% gray);
     * the title stays gray.
     */
    fun crossedPalette(priority: Int, style: CrossedOutStyle): CrossedPalette {
        if (style == CrossedOutStyle.GRAY || !hasPriority(priority)) {
            return CrossedPalette(Gray.TEXT, Gray.TEXT, Gray.BADGE_FILL, Gray.BAR_TRACK, Gray.CHECK_FILL, Gray.CHECKMARK)
        }
        val c = priorityColorArgb(priority)
        return CrossedPalette(
            text = Gray.TEXT,
            accent = fadeToward(c, Gray.TEXT and 0xFF),
            badgeFill = Gray.BADGE_FILL,   // stays a dark neutral; the hint is in the number and border
            barTrack = Gray.BAR_TRACK,
            checkFill = fadeToward(c, Gray.CHECK_FILL and 0xFF),
            checkmark = Gray.CHECKMARK,
        )
    }

    /** Whether the number badge / the bar are drawn for a task, given the display switches. */
    fun showBadge(priority: Int, settings: AppSettings): Boolean = hasPriority(priority) && settings.showPriorityNumber
    fun showBar(priority: Int, settings: AppSettings): Boolean = hasPriority(priority) && settings.showPriorityBar

    /** Open tasks: prioritized first (highest first; none = 0 sorts last), ties by creation time (oldest first), then id. */
    val openTaskOrder: Comparator<Task> =
        compareByDescending<Task> { it.priority }.thenBy { it.createdAt }.thenBy { it.id }

    fun sortOpen(tasks: List<Task>): List<Task> = tasks.filter { !it.completed }.sortedWith(openTaskOrder)

    /** Completed tasks: most recently completed first. */
    fun sortCompleted(tasks: List<Task>): List<Task> =
        tasks.filter { it.completed }.sortedWith(
            compareByDescending<Task> { it.completedAt ?: 0L }.thenByDescending { it.id },
        )

    /** Bulk-add entries that become tasks: non-blank titles, in order. */
    fun bulkTasksToCreate(entries: List<BulkEntry>): List<BulkEntry> = entries.filter { it.title.isNotBlank() }

    /** Number of open tasks per category id. */
    fun openCounts(tasks: List<Task>): Map<Long, Int> =
        tasks.filter { !it.completed }.groupingBy { it.categoryId }.eachCount()

    /**
     * Move the category at [from] to index [to] (both indexes in the current tab order) and
     * return all categories with contiguous positions 0..n-1 in the new order.
     */
    fun reorder(categories: List<Category>, from: Int, to: Int): List<Category> {
        val ordered = categories.sortedWith(compareBy<Category> { it.position }.thenBy { it.id }).toMutableList()
        if (from !in ordered.indices || to !in ordered.indices) return normalize(ordered)
        val item = ordered.removeAt(from)
        ordered.add(to, item)
        return normalize(ordered)
    }

    fun moveUp(categories: List<Category>, index: Int): List<Category> = reorder(categories, index, index - 1)
    fun moveDown(categories: List<Category>, index: Int): List<Category> = reorder(categories, index, index + 1)

    /** Re-number positions 0..n-1 in list order. */
    fun normalize(ordered: List<Category>): List<Category> =
        ordered.mapIndexed { i, c -> if (c.position == i) c else c.copy(position = i) }

    /** Position for a newly appended category. */
    fun nextPosition(categories: List<Category>): Int = (categories.maxOfOrNull { it.position } ?: -1) + 1

    /** Return a copy of [task] assigned to [categoryId]; everything else (priority, state) is kept. */
    fun moveToCategory(task: Task, categoryId: Long): Task = task.copy(categoryId = categoryId)

    fun setCompleted(task: Task, completed: Boolean, now: Long = System.currentTimeMillis()): Task =
        task.copy(completed = completed, completedAt = if (completed) now else null)

    /** Toggle the crossed-out state; the task stays open/completed as it was. */
    fun toggleCrossedOut(task: Task): Task = task.copy(crossedOut = !task.crossedOut)

    /**
     * Urgency color as ARGB for priority 1..10: goes from gray-yellow (1) through orange to red (10).
     */
    fun priorityColorArgb(priority: Int): Int {
        val t = (priority.coerceIn(Task.MIN_PRIORITY, Task.MAX_PRIORITY) - 1) / 9f
        val hue = 52f * (1f - t)
        val sat = 0.25f + 0.65f * t
        return hsvToArgb(hue, sat, 0.95f)
    }

    private fun hsvToArgb(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val hp = (h / 60f) % 6f
        val x = c * (1 - kotlin.math.abs(hp % 2f - 1))
        val (r1, g1, b1) = when {
            hp < 1 -> Triple(c, x, 0f)
            hp < 2 -> Triple(x, c, 0f)
            hp < 3 -> Triple(0f, c, x)
            hp < 4 -> Triple(0f, x, c)
            hp < 5 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = v - c
        fun ch(f: Float) = ((f + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
    }
}
