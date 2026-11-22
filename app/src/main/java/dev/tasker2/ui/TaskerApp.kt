package dev.tasker2.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

/** Very small navigation: "main", "manage", "edit:<taskId>", "new:<categoryId>". */
@Composable
fun TaskerApp(vm: TaskViewModel = viewModel()) {
    val categories by vm.categories.collectAsState()
    val tasks by vm.tasks.collectAsState()
    var route by rememberSaveable { mutableStateOf("main") }
    var selectedCategoryId by rememberSaveable { mutableLongStateOf(-1L) }

    val cats = categories
    val allTasks = tasks
    if (cats == null || allTasks == null) return

    // Keep the selected tab valid (e.g. after deleting a category).
    LaunchedEffect(cats) {
        if (cats.none { it.id == selectedCategoryId }) selectedCategoryId = cats.firstOrNull()?.id ?: -1L
    }

    if (route != "main") BackHandler { route = "main" }

    when {
        route == "manage" -> ManageCategoriesScreen(
            categories = cats,
            tasks = allTasks,
            onBack = { route = "main" },
            onAdd = vm::addCategory,
            onRename = vm::renameCategory,
            onDelete = vm::deleteCategory,
            onMove = vm::moveCategory,
        )

        route.startsWith("new:") -> {
            val catId = route.removePrefix("new:").toLong()
            TaskEditorScreen(
                task = null,
                initialCategoryId = catId,
                categories = cats,
                onBack = { route = "main" },
                onSave = { draft ->
                    vm.addTask(draft.categoryId, draft.title, draft.notes, draft.priority)
                    selectedCategoryId = draft.categoryId
                    route = "main"
                },
                onDelete = null,
            )
        }

        route.startsWith("edit:") -> {
            val id = route.removePrefix("edit:").toLong()
            val task = allTasks.firstOrNull { it.id == id }
            if (task == null) {
                LaunchedEffect(Unit) { route = "main" }
            } else {
                TaskEditorScreen(
                    task = task,
                    initialCategoryId = task.categoryId,
                    categories = cats,
                    onBack = { route = "main" },
                    onSave = { draft ->
                        var updated = task.copy(title = draft.title, notes = draft.notes, priority = draft.priority, categoryId = draft.categoryId)
                        if (draft.completed != task.completed) {
                            updated = dev.tasker2.domain.TaskLogic.setCompleted(updated, draft.completed)
                        }
                        vm.updateTask(updated)
                        route = "main"
                    },
                    onDelete = {
                        vm.deleteTask(task)
                        route = "main"
                    },
                )
            }
        }

        else -> MainScreen(
            categories = cats,
            tasks = allTasks,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { selectedCategoryId = it },
            onAddTask = { if (selectedCategoryId >= 0) route = "new:$selectedCategoryId" },
            onEditTask = { route = "edit:${it.id}" },
            onToggleComplete = { t -> vm.setCompleted(t, !t.completed) },
            onToggleCrossedOut = vm::toggleCrossedOut,
            onMoveTask = vm::moveTask,
            onDeleteTask = vm::deleteTask,
            onManageCategories = { route = "manage" },
        )
    }
}
