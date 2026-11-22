package dev.tasker2.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tasker2.data.AppDatabase
import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.data.TaskRepository
import dev.tasker2.domain.BulkEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository(AppDatabase.get(app).taskDao())

    /** null until the first emission from the database. */
    val categories: StateFlow<List<Category>?> = repo.categories
        .map<List<Category>, List<Category>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tasks: StateFlow<List<Task>?> = repo.tasks
        .map<List<Task>, List<Task>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addCategory(name: String) = launch { if (name.isNotBlank()) repo.addCategory(name) }
    fun renameCategory(c: Category, name: String) = launch { repo.renameCategory(c, name) }
    fun deleteCategory(c: Category) = launch { repo.deleteCategory(c) }
    fun moveCategory(from: Int, to: Int) = launch { repo.moveCategory(from, to) }

    fun addTask(categoryId: Long, title: String, notes: String, priority: Int) =
        launch { if (title.isNotBlank()) repo.addTask(categoryId, title, notes, priority) }
    fun addTasks(categoryId: Long, entries: List<BulkEntry>) = launch { repo.addTasks(categoryId, entries) }
    fun updateTask(task: Task) = launch { repo.updateTask(task) }
    fun setCompleted(task: Task, completed: Boolean) = launch { repo.setCompleted(task, completed) }
    fun toggleCrossedOut(task: Task) = launch { repo.toggleCrossedOut(task) }
    fun moveTask(task: Task, categoryId: Long) = launch { repo.moveTask(task, categoryId) }
    fun deleteTask(task: Task) = launch { repo.deleteTask(task) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
