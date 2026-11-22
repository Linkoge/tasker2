package dev.tasker2.domain

import dev.tasker2.data.Category

/** Pure rules for the category tab menu (long-press / tap the selected tab). */
object CategoryMenu {
    fun canMoveLeft(index: Int): Boolean = index > 0
    fun canMoveRight(index: Int, size: Int): Boolean = index in 0 until size - 1

    /** The trimmed name, or null when it is blank and must be rejected. Unicode (e.g. Georgian) is kept as is. */
    fun validName(raw: String): String? = raw.trim().ifEmpty { null }

    fun deleteMessage(name: String, taskCount: Int): String = when (taskCount) {
        0 -> "The category “$name” will be deleted."
        1 -> "The category “$name” and its 1 task will be deleted."
        else -> "The category “$name” and its $taskCount tasks will be deleted."
    }

    /**
     * Tab to select once [deletedId] is gone: the current one if another tab was deleted, otherwise the tab that
     * takes its place (the right neighbour), or the left one if it was last; null when nothing is left.
     */
    fun selectionAfterDelete(categories: List<Category>, deletedId: Long, selectedId: Long): Long? {
        val remaining = categories.filter { it.id != deletedId }
        if (remaining.isEmpty()) return null
        if (deletedId != selectedId && remaining.any { it.id == selectedId }) return selectedId
        val index = categories.indexOfFirst { it.id == deletedId }.coerceAtLeast(0)
        return remaining[index.coerceAtMost(remaining.lastIndex)].id
    }
}
