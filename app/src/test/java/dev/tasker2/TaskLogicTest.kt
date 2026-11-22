package dev.tasker2

import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.domain.TaskLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLogicTest {
    private fun task(id: Long, priority: Int, createdAt: Long, completed: Boolean = false, cat: Long = 1) =
        Task(id = id, categoryId = cat, title = "t$id", priority = priority, createdAt = createdAt, completed = completed)

    @Test
    fun openTasksSortedByPriorityDescThenCreationTime() {
        val tasks = listOf(
            task(1, 5, createdAt = 100),
            task(2, 10, createdAt = 300),
            task(3, 5, createdAt = 50),
            task(4, 1, createdAt = 10),
            task(5, 10, createdAt = 200),
            task(6, 9, createdAt = 1, completed = true),
        )
        val sorted = TaskLogic.sortOpen(tasks)
        assertEquals(listOf(5L, 2L, 3L, 1L, 4L), sorted.map { it.id })
    }

    @Test
    fun completedTasksAreExcludedFromOpenAndCounted() {
        val tasks = listOf(task(1, 5, 1), task(2, 5, 2, completed = true), task(3, 5, 3, cat = 2))
        assertEquals(mapOf(1L to 1, 2L to 1), TaskLogic.openCounts(tasks))
        assertEquals(listOf(2L), TaskLogic.sortCompleted(tasks).map { it.id })
    }

    @Test
    fun priorityIsClampedToTenLevels() {
        assertEquals(1, TaskLogic.clampPriority(-3))
        assertEquals(10, TaskLogic.clampPriority(42))
        assertEquals(7, TaskLogic.clampPriority(7))
        assertEquals(5, Task(categoryId = 1, title = "x").priority)
    }

    @Test
    fun urgencyColorGoesFromGrayYellowToRed() {
        fun rgb(p: Int) = TaskLogic.priorityColorArgb(p).let { Triple((it shr 16) and 0xFF, (it shr 8) and 0xFF, it and 0xFF) }
        val low = rgb(1)
        val high = rgb(10)
        assertTrue("low priority should be gray/yellow, not blue", low.first > 200 && low.second > 200 && low.third <= low.first)
        assertTrue("high priority should be red", high.first > 200 && high.second < 80 && high.third < 80)
        assertNotEquals(rgb(5), rgb(6))
    }

    private fun cats(vararg names: String) = names.mapIndexed { i, n -> Category(id = (i + 1).toLong(), name = n, position = i) }

    @Test
    fun reorderMovesCategoryAndRenumbersPositions() {
        val c = cats("Daily", "Weekly", "Monthly", "დღეს+")
        val moved = TaskLogic.reorder(c, from = 3, to = 0)
        assertEquals(listOf("დღეს+", "Daily", "Weekly", "Monthly"), moved.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), moved.map { it.position })
    }

    @Test
    fun moveUpAndDownSwapNeighbours() {
        val c = cats("A", "B", "C")
        assertEquals(listOf("B", "A", "C"), TaskLogic.moveUp(c, 1).map { it.name })
        assertEquals(listOf("A", "C", "B"), TaskLogic.moveDown(c, 1).map { it.name })
        // Out of range is a no-op.
        assertEquals(listOf("A", "B", "C"), TaskLogic.moveUp(c, 0).map { it.name })
        assertEquals(listOf("A", "B", "C"), TaskLogic.moveDown(c, 2).map { it.name })
    }

    @Test
    fun reorderUsesCurrentPositionsNotListOrder() {
        val shuffled = listOf(Category(3, "C", 2), Category(1, "A", 0), Category(2, "B", 1))
        assertEquals(listOf("B", "A", "C"), TaskLogic.reorder(shuffled, 1, 0).map { it.name })
    }

    @Test
    fun moveToCategoryKeepsEverythingButCategory() {
        val t = task(1, 8, 123, cat = 1).copy(notes = "n")
        val moved = TaskLogic.moveToCategory(t, 2)
        assertEquals(2L, moved.categoryId)
        assertEquals(t.copy(categoryId = 2), moved)
    }

    @Test
    fun setCompletedStampsAndClearsCompletionTime() {
        val t = task(1, 5, 1)
        val done = TaskLogic.setCompleted(t, true, now = 999)
        assertTrue(done.completed)
        assertEquals(999L, done.completedAt)
        val reopened = TaskLogic.setCompleted(done, false)
        assertNull(reopened.completedAt)
    }
}
