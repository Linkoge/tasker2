package dev.tasker2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tasker2.data.Category
import dev.tasker2.data.Task
import kotlin.math.roundToInt

data class TaskDraft(
    val title: String,
    val notes: String,
    val priority: Int,
    val categoryId: Long,
    val completed: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    task: Task?,
    initialCategoryId: Long,
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val key = task?.id ?: -1L
    var title by rememberSaveable(key) { mutableStateOf(task?.title ?: "") }
    var notes by rememberSaveable(key) { mutableStateOf(task?.notes ?: "") }
    var priority by rememberSaveable(key) { mutableFloatStateOf((task?.priority ?: Task.DEFAULT_PRIORITY).toFloat()) }
    var categoryId by rememberSaveable(key) { mutableLongStateOf(initialCategoryId) }
    var completed by rememberSaveable(key) { mutableStateOf(task?.completed ?: false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val p = priority.roundToInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (task == null) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete task") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(priorityLabel(p), color = priorityColor(p))
                    Spacer(Modifier.width(12.dp))
                    PriorityIndicator(p)
                }
                Slider(
                    value = priority,
                    onValueChange = { priority = it },
                    valueRange = Task.MIN_PRIORITY.toFloat()..Task.MAX_PRIORITY.toFloat(),
                    steps = Task.MAX_PRIORITY - Task.MIN_PRIORITY - 1,
                    colors = SliderDefaults.colors(thumbColor = priorityColor(p), activeTrackColor = priorityColor(p)),
                )
                Row {
                    Text("1 · low", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("10 · most urgent", style = MaterialTheme.typography.labelSmall)
                }
            }

            CategoryDropdown(categories, categoryId) { categoryId = it }

            if (task != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = completed, onCheckedChange = { completed = it })
                    Text("Completed")
                }
            }

            Button(
                onClick = {
                    onSave(TaskDraft(title.trim(), notes.trim(), p, categoryId, completed))
                },
                enabled = title.isNotBlank() && categories.any { it.id == categoryId },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text(title) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun priorityLabel(p: Int) = when {
    p >= 9 -> "Critical"
    p >= 7 -> "High"
    p >= 4 -> "Medium"
    else -> "Low"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(categories: List<Category>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.name) }, onClick = { onSelect(cat.id); expanded = false })
            }
        }
    }
}
