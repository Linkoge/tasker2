package dev.tasker2.data

import dev.tasker2.domain.TaskLogic
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val categories: Flow<List<Category>> = dao.observeCategories()
    val tasks: Flow<List<Task>> = dao.observeTasks()

    suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Category name must not be blank" }
        return dao.insertCategory(Category(name = trimmed, position = TaskLogic.nextPosition(dao.getCategories())))
    }

    suspend fun renameCategory(category: Category, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.updateCategories(listOf(category.copy(name = trimmed)))
    }

    /** Deletes the category and (via cascade) its tasks, then compacts positions. */
    suspend fun deleteCategory(category: Category) {
        dao.deleteCategory(category)
        val remaining = dao.getCategories()
        val normalized = TaskLogic.normalize(remaining)
        val changed = normalized.filterIndexed { i, c -> c != remaining[i] }
        if (changed.isNotEmpty()) dao.updateCategories(changed)
    }

    /** Move a category tab from index [from] to index [to]; the new order is persisted. */
    suspend fun moveCategory(from: Int, to: Int) {
        val current = dao.getCategories()
        val reordered = TaskLogic.reorder(current, from, to)
        // A single @Update call runs in one transaction.
        dao.updateCategories(reordered)
    }

    suspend fun addTask(categoryId: Long, title: String, notes: String, priority: Int): Long =
        dao.insertTask(
            Task(
                categoryId = categoryId,
                title = title.trim(),
                notes = notes.trim(),
                priority = TaskLogic.clampPriority(priority),
            ),
        )

    suspend fun getTask(id: Long): Task? = dao.getTask(id)

    suspend fun updateTask(task: Task) =
        dao.updateTask(task.copy(title = task.title.trim(), notes = task.notes.trim(), priority = TaskLogic.clampPriority(task.priority)))

    suspend fun setCompleted(task: Task, completed: Boolean) = dao.updateTask(TaskLogic.setCompleted(task, completed))

    suspend fun moveTask(task: Task, categoryId: Long) {
        if (task.categoryId == categoryId) return
        dao.updateTask(TaskLogic.moveToCategory(task, categoryId))
    }

    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
}
