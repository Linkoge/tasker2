package dev.tasker2

import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.data.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory DAO mirroring Room's behaviour (soft-delete filtering, ordering by position, unique uuids). */
class FakeDao : TaskDao {
    val cats = MutableStateFlow<List<Category>>(emptyList())
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    private fun live(list: List<Category>) = list.filter { !it.deleted }.sortedWith(compareBy<Category> { it.position }.thenBy { it.id })
    override fun observeCategories(): Flow<List<Category>> = cats.map { live(it) }
    override suspend fun getCategories() = live(cats.value)
    override suspend fun getAllCategories() = cats.value.sortedBy { it.id }
    override suspend fun insertCategory(category: Category): Long {
        require(cats.value.none { it.uuid == category.uuid }) { "duplicate uuid" }
        val id = nextId++
        cats.value = cats.value + category.copy(id = id)
        return id
    }
    override suspend fun updateCategories(categories: List<Category>) {
        val byId = categories.associateBy { it.id }
        cats.value = cats.value.map { byId[it.id] ?: it }
    }
    override fun observeTasks(): Flow<List<Task>> = tasks.map { l -> l.filter { !it.deleted } }
    override suspend fun getTasks() = tasks.value.filter { !it.deleted }
    override suspend fun getAllTasks() = tasks.value.sortedBy { it.id }
    override suspend fun getTask(id: Long) = tasks.value.firstOrNull { it.id == id && !it.deleted }
    override suspend fun insertTask(task: Task): Long {
        require(tasks.value.none { it.uuid == task.uuid }) { "duplicate uuid" }
        val id = nextId++
        tasks.value = tasks.value + task.copy(id = id)
        return id
    }
    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }
    override suspend fun updateTasks(tasks: List<Task>) {
        val byId = tasks.associateBy { it.id }
        this.tasks.value = this.tasks.value.map { byId[it.id] ?: it }
    }
}
