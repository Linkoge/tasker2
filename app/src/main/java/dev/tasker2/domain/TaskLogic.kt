package dev.tasker2.domain

import dev.tasker2.data.Category
import dev.tasker2.data.Task

/** Pure, Android-free logic so it can be unit tested on the JVM. */
object TaskLogic {

    /** Clamp any value into the valid 1..10 priority range. */
    fun clampPriority(p: Int): Int = p.coerceIn(Task.MIN_PRIORITY, Task.MAX_PRIORITY)

    /** Open tasks: highest priority first, ties by creation time (oldest first), then id. */
    val openTaskOrder: Comparator<Task> =
        compareByDescending<Task> { it.priority }.thenBy { it.createdAt }.thenBy { it.id }

    fun sortOpen(tasks: List<Task>): List<Task> = tasks.filter { !it.completed }.sortedWith(openTaskOrder)

    /** Completed tasks: most recently completed first. */
    fun sortCompleted(tasks: List<Task>): List<Task> =
        tasks.filter { it.completed }.sortedWith(
            compareByDescending<Task> { it.completedAt ?: 0L }.thenByDescending { it.id },
        )

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
        val t = (clampPriority(priority) - 1) / 9f
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
