package dev.tasker2

import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.data.TaskDao
import dev.tasker2.data.TaskRepository
import dev.tasker2.domain.TaskLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory DAO mirroring Room's behaviour (ordering by position, cascade delete). */
private class FakeDao : TaskDao {
    val cats = MutableStateFlow<List<Category>>(emptyList())
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    private fun sortedCats() = cats.value.sortedWith(compareBy<Category> { it.position }.thenBy { it.id })
    override fun observeCategories(): Flow<List<Category>> = cats
    override suspend fun getCategories() = sortedCats()
    override suspend fun insertCategory(category: Category): Long {
        val id = nextId++
        cats.value = cats.value + category.copy(id = id)
        return id
    }
    override suspend fun updateCategories(categories: List<Category>) {
        val byId = categories.associateBy { it.id }
        cats.value = cats.value.map { byId[it.id] ?: it }
    }
    override suspend fun deleteCategory(category: Category) {
        cats.value = cats.value.filter { it.id != category.id }
        tasks.value = tasks.value.filter { it.categoryId != category.id }
    }
    override fun observeTasks(): Flow<List<Task>> = tasks
    override suspend fun getTask(id: Long) = tasks.value.firstOrNull { it.id == id }
    override suspend fun insertTask(task: Task): Long {
        val id = nextId++
        tasks.value = tasks.value + task.copy(id = id)
        return id
    }
    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }
    override suspend fun deleteTask(task: Task) {
        tasks.value = tasks.value.filter { it.id != task.id }
    }
}

class TaskRepositoryTest {
    private val dao = FakeDao()
    private val repo = TaskRepository(dao)

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
        assertEquals(listOf("in a"), dao.tasks.value.map { it.title })
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
        val low = repo.addTask(c, "low", "", 0)
        val high = repo.addTask(c, "high", "", 99)
        assertEquals(1, repo.getTask(low)!!.priority)
        assertEquals(10, repo.getTask(high)!!.priority)
        repo.updateTask(repo.getTask(low)!!.copy(priority = 10, title = " now urgent "))
        val order = TaskLogic.sortOpen(dao.tasks.value).map { it.title }
        // Same priority: older task (created first) stays on top.
        assertEquals(listOf("now urgent", "high"), order)
    }
}
