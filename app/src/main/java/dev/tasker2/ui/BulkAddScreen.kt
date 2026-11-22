package dev.tasker2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import dev.tasker2.data.Category
import dev.tasker2.domain.BulkEntry
import dev.tasker2.domain.BulkState
import dev.tasker2.domain.TaskLogic

private val BulkStateSaver = listSaver<BulkState, Any>(
    save = { s -> listOf<Any>(s.selected) + s.entries.flatMap { listOf(it.title, it.notes, it.priority) } },
    restore = { l ->
        val entries = l.drop(1).chunked(3).map { BulkEntry(it[0] as String, it[1] as String, it[2] as Int) }
        BulkState(entries, l[0] as Int).normalized()
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddScreen(
    initialCategoryId: Long,
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (categoryId: Long, entries: List<BulkEntry>) -> Unit,
) {
    var state by rememberSaveable(stateSaver = BulkStateSaver) { mutableStateOf(BulkState()) }
    var categoryId by rememberSaveable { mutableLongStateOf(initialCategoryId) }
    val toCreate = TaskLogic.bulkTasksToCreate(state.entries)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New tasks") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(state.entries) { i, entry ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${i + 1}.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
                        OutlinedTextField(
                            value = entry.title,
                            onValueChange = { state = state.setTitle(i, it) },
                            enabled = state.isEnabled(i),
                            singleLine = true,
                            label = { Text("Title") },
                            modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) state = state.select(i) },
                        )
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Details for ${state.selected + 1}.", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = state.current.notes, onValueChange = { state = state.setNotes(it) },
                    label = { Text("Notes") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                PriorityEditor(state.current.priority.toFloat()) { state = state.setPriority(Math.round(it)) }
                CategoryDropdown(categories, categoryId) { categoryId = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(categoryId, toCreate) },
                        enabled = toCreate.isNotEmpty() && categories.any { it.id == categoryId },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}
