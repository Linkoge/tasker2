package dev.tasker2.data

import dev.tasker2.domain.BulkEntry
import dev.tasker2.domain.TaskLogic
import kotlinx.coroutines.flow.Flow

/** [TaskRepository] on top of the local Room database. [clock] is injectable for tests. */
class LocalTaskRepository(
    private val dao: TaskDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : TaskRepository {
    override val categories: Flow<List<Category>> = dao.observeCategories()
    override val tasks: Flow<List<Task>> = dao.observeTasks()

    override suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Category name must not be blank" }
        val now = clock()
        return dao.insertCategory(
            Category(name = trimmed, position = TaskLogic.nextPosition(dao.getCategories()), createdAt = now, updatedAt = now),
        )
    }

    override suspend fun renameCategory(category: Category, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val stored = dao.getCategories().firstOrNull { it.id == category.id } ?: return
        if (stored.name == trimmed) return
        dao.updateCategories(listOf(stored.copy(name = trimmed, updatedAt = clock())))
    }

    override suspend fun deleteCategory(category: Category) {
        val stored = dao.getCategories().firstOrNull { it.id == category.id } ?: return
        val now = clock()
        // Tasks first, then the category itself.
        dao.updateTasks(dao.getTasks().filter { it.categoryId == stored.id }.map { it.copy(deleted = true, updatedAt = now) })
        dao.updateCategories(listOf(stored.copy(deleted = true, updatedAt = now)))
        saveOrder(dao.getCategories(), now)
    }

    override suspend fun moveCategory(from: Int, to: Int) {
        val current = dao.getCategories()
        saveOrder(TaskLogic.reorder(current, from, to), clock(), current)
    }

    /** Persists the categories whose position differs from [before], stamping them with [now]. */
    private suspend fun saveOrder(ordered: List<Category>, now: Long, before: List<Category> = ordered) {
        val normalized = TaskLogic.normalize(ordered)
        val old = before.associateBy { it.id }
        val changed = normalized.filter { it.position != old[it.id]?.position }.map { it.copy(updatedAt = now) }
        // A single @Update call runs in one transaction.
        if (changed.isNotEmpty()) dao.updateCategories(changed)
    }

    override suspend fun addTask(categoryId: Long, title: String, notes: String, priority: Int): Long {
        val now = clock()
        return dao.insertTask(
            Task(
                categoryId = categoryId,
                title = title.trim(),
                notes = notes.trim(),
                priority = TaskLogic.clampPriority(priority),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Creates one task per non-blank title, in list order. createdAt strictly increases with the index
     * so equal-priority tasks are listed in the order they were entered.
     */
    override suspend fun addTasks(categoryId: Long, entries: List<BulkEntry>) {
        val base = clock()
        TaskLogic.bulkTasksToCreate(entries).forEachIndexed { i, e ->
            dao.insertTask(
                Task(
                    categoryId = categoryId,
                    title = e.title.trim(),
                    notes = e.notes.trim(),
                    priority = TaskLogic.clampPriority(e.priority),
                    createdAt = base + i,
                    updatedAt = base + i,
                ),
            )
        }
    }

    override suspend fun getTask(id: Long): Task? = dao.getTask(id)

    /** Applies the editable fields of [task] to the stored task; identity (uuid), createdAt and deleted are never taken from the caller. */
    override suspend fun updateTask(task: Task) {
        val stored = dao.getTask(task.id) ?: return
        save(
            stored,
            task.copy(
                title = task.title.trim(),
                notes = task.notes.trim(),
                priority = TaskLogic.clampPriority(task.priority),
                uuid = stored.uuid,
                createdAt = stored.createdAt,
                deleted = false,
            ),
        )
    }

    override suspend fun setCompleted(task: Task, completed: Boolean) {
        val stored = dao.getTask(task.id) ?: return
        save(stored, TaskLogic.setCompleted(stored, completed, clock()))
    }

    override suspend fun toggleCrossedOut(task: Task) {
        val stored = dao.getTask(task.id) ?: return
        save(stored, TaskLogic.toggleCrossedOut(stored))
    }

    override suspend fun moveTask(task: Task, categoryId: Long) {
        val stored = dao.getTask(task.id) ?: return
        if (stored.categoryId == categoryId) return
        save(stored, TaskLogic.moveToCategory(stored, categoryId))
    }

    override suspend fun deleteTask(task: Task) {
        val stored = dao.getTask(task.id) ?: return
        dao.updateTask(stored.copy(deleted = true, updatedAt = clock()))
    }

    override suspend fun snapshot() = StoreSnapshot(dao.getAllCategories(), dao.getAllTasks())

    /** Writes [next] with a fresh updatedAt, unless nothing changed. */
    private suspend fun save(stored: Task, next: Task) {
        if (next.copy(updatedAt = stored.updatedAt) == stored) return
        dao.updateTask(next.copy(updatedAt = maxOf(clock(), stored.updatedAt)))
    }
}
