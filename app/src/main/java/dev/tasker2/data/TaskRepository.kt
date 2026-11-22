package dev.tasker2.data

import dev.tasker2.domain.BulkEntry
import kotlinx.coroutines.flow.Flow

/** Everything stored, soft-deleted rows included: the unit a future sync reads and writes. */
data class StoreSnapshot(val categories: List<Category>, val tasks: List<Task>)

/**
 * The single storage layer for tasks, categories and their order. Screens and view models talk only to this
 * interface. [LocalTaskRepository] is the Room-backed implementation; a cloud implementation can replace it or
 * wrap it (observe [categories] / [tasks], push local changes, and apply remote ones through [snapshot]).
 *
 * Identity is the [Category.uuid] / [Task.uuid]; the Long `id` is only a local row handle. Every mutation bumps
 * `updatedAt`; deletes are soft (tombstones) and hidden from [categories], [tasks] and [getTask].
 */
interface TaskRepository {
    /** Live categories in tab order. Emits again after every change. */
    val categories: Flow<List<Category>>

    /** Live non-deleted tasks (any order). Emits again after every change. */
    val tasks: Flow<List<Task>>

    suspend fun addCategory(name: String): Long
    suspend fun renameCategory(category: Category, name: String)

    /** Soft-deletes the category and its tasks, then compacts the order of the remaining tabs. */
    suspend fun deleteCategory(category: Category)

    /** Move a category tab from index [from] to index [to]; the new order is persisted. */
    suspend fun moveCategory(from: Int, to: Int)

    suspend fun addTask(categoryId: Long, title: String, notes: String, priority: Int): Long
    suspend fun addTasks(categoryId: Long, entries: List<BulkEntry>)
    suspend fun getTask(id: Long): Task?
    suspend fun updateTask(task: Task)
    suspend fun setCompleted(task: Task, completed: Boolean)
    suspend fun toggleCrossedOut(task: Task)
    suspend fun moveTask(task: Task, categoryId: Long)

    /** Soft-deletes the task. */
    suspend fun deleteTask(task: Task)

    /** All rows including tombstones, for sync. */
    suspend fun snapshot(): StoreSnapshot
}
