package dev.tasker2

import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.data.TaskDao
import dev.tasker2.data.LocalTaskRepository
import dev.tasker2.data.TaskRepository
import dev.tasker2.domain.TaskLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryTest {
    private val dao = FakeDao()
    private val repo: TaskRepository = LocalTaskRepository(dao)

    @Test
    fun categoryReorderIsPersisted() = runBlocking {
        val names = listOf("Daily", "Weekly", "Monthly", "დღეს+")
        names.forEach { repo.addCategory(it) }
        repo.moveCategory(from = 2, to = 0)
        assertEquals(listOf("Monthly", "Daily", "Weekly", "დღეს+"), dao.getCategories().map { it.name })
        repo.moveCategory(from = 3, to = 2)
        val stored = dao.getCategories()
        assertEquals(listOf("Monthly", "Daily", "დღეს+", "Weekly"), stored.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), stored.map { it.position })
    }

    @Test
    fun newCategoryIsAppendedAndUnicodeNamePreserved() = runBlocking {
        repo.addCategory("A")
        repo.addCategory("  ყოველკვირეული  ")
        val stored = dao.getCategories()
        assertEquals(listOf("A", "ყოველკვირეული"), stored.map { it.name })
        assertEquals(listOf(0, 1), stored.map { it.position })
    }

    @Test
    fun deletingCategoryCompactsPositionsAndRemovesItsTasks() = runBlocking {
        val a = repo.addCategory("A")
        val b = repo.addCategory("B")
        repo.addCategory("C")
        repo.addTask(b, "in b", "", 5)
        repo.addTask(a, "in a", "", 5)
        repo.deleteCategory(dao.getCategories().first { it.id == b })
        assertEquals(listOf("A" to 0, "C" to 1), dao.getCategories().map { it.name to it.position })
        assertEquals(listOf("in a"), repo.tasks.first().map { it.title })
    }

    @Test
    fun moveTaskToAnotherCategory() = runBlocking {
        val daily = repo.addCategory("Daily")
        val weekly = repo.addCategory("Weekly")
        val id = repo.addTask(daily, "Pay bills", "notes", 9)
        val task = repo.getTask(id)!!
        repo.moveTask(task, weekly)

        val all = repo.tasks.first()
        val moved = all.single { it.id == id }
        assertEquals(weekly, moved.categoryId)
        assertEquals(9, moved.priority)
        assertEquals("notes", moved.notes)
        assertEquals(mapOf(weekly to 1), TaskLogic.openCounts(all))
        assertTrue(TaskLogic.sortOpen(all.filter { it.categoryId == daily }).isEmpty())
    }

    @Test
    fun addTaskClampsPriorityAndEditKeepsSortOrder() = runBlocking {
        val c = repo.addCategory("Daily")
        val low = repo.addTask(c, "low", "", 1)
        val high = repo.addTask(c, "high", "", 99)
        assertEquals(0, repo.getTask(repo.addTask(c, "neg", "", -4))!!.priority)
        assertEquals(1, repo.getTask(low)!!.priority)
        assertEquals(10, repo.getTask(high)!!.priority)
        repo.updateTask(repo.getTask(low)!!.copy(priority = 10, title = " now urgent "))
        val order = TaskLogic.sortOpen(dao.tasks.value).map { it.title }
        // Same priority: older task (created first) stays on top.
        assertEquals(listOf("now urgent", "high", "neg"), order)
    }

    @Test
    fun tasksWithoutPrioritySortLastAndCrossedOutThenCompleteWorks() = runBlocking {
        val c = repo.addCategory("Daily")
        val plain = repo.addTask(c, "plain", "", Task.NO_PRIORITY)
        val urgent = repo.addTask(c, "urgent", "", 2)
        assertEquals(0, repo.getTask(plain)!!.priority)
        assertEquals(listOf("urgent", "plain"), TaskLogic.sortOpen(dao.tasks.value).map { it.title })
        repo.toggleCrossedOut(repo.getTask(plain)!!)
        assertTrue(repo.getTask(plain)!!.crossedOut)
        assertEquals(2, TaskLogic.sortOpen(dao.tasks.value).size)
        repo.setCompleted(repo.getTask(plain)!!, true)
        assertEquals(listOf("urgent"), TaskLogic.sortOpen(dao.tasks.value).map { it.title })
        assertEquals(listOf("plain"), TaskLogic.sortCompleted(dao.tasks.value).map { it.title })
        repo.setCompleted(repo.getTask(plain)!!, false)
        repo.toggleCrossedOut(repo.getTask(plain)!!)
        assertEquals(false, repo.getTask(plain)!!.crossedOut)
        assertEquals(2, repo.getTask(urgent)!!.priority)
    }

    @Test
    fun bulkAddCreatesInOrderInOneCategorySkippingBlanks() = runBlocking {
        val dao = FakeDao()
        val repo: TaskRepository = LocalTaskRepository(dao)
        val c1 = repo.addCategory("A")
        val c2 = repo.addCategory("B")
        repo.addTasks(
            c2,
            listOf(
                dev.tasker2.domain.BulkEntry("one", "n", 7),
                dev.tasker2.domain.BulkEntry("  "),
                dev.tasker2.domain.BulkEntry("two", "", 7),
                dev.tasker2.domain.BulkEntry("three", "", 9),
                dev.tasker2.domain.BulkEntry("four", "", 7),
            ),
        )
        val all = dao.getTasks()
        assertEquals(4, all.size)
        assertTrue(all.all { it.categoryId == c2 } && c1 != c2)
        assertEquals(listOf("one", "two", "three", "four"), all.sortedBy { it.id }.map { it.title })
        assertEquals(7, all.first { it.title == "one" }.priority)
        assertEquals("n", all.first { it.title == "one" }.notes)
        assertEquals(listOf("three", "one", "two", "four"), TaskLogic.sortOpen(all).map { it.title })
    }

    @Test
    fun equalPriorityTiesFollowCreationOrderEvenWithSameTimestamp() {
        val t = { id: Long, title: String -> Task(id = id, categoryId = 1, title = title, priority = 5, createdAt = 100) }
        assertEquals(listOf("a", "b", "c"), TaskLogic.sortOpen(listOf(t(3, "c"), t(1, "a"), t(2, "b"))).map { it.title })
    }
}
