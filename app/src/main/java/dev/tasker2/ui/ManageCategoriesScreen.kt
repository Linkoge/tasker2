package dev.tasker2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.ui.theme.CardColor
import dev.tasker2.ui.theme.Muted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    categories: List<Category>,
    tasks: List<Task>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Category, String) -> Unit,
    onDelete: (Category) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var renamingId by rememberSaveable { mutableLongStateOf(-1L) }
    var deletingId by rememberSaveable { mutableLongStateOf(-1L) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage categories") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add category") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
        ) {
            if (categories.isEmpty()) {
                item { Text("No categories. Add one below.", color = Muted, modifier = Modifier.padding(16.dp)) }
            }
            itemsIndexed(categories, key = { _, c -> c.id }) { index, cat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                        .background(CardColor).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Move ${cat.name} up")
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = index < categories.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Move ${cat.name} down")
                    }
                    IconButton(onClick = { renamingId = cat.id }) { Icon(Icons.Filled.Edit, "Rename ${cat.name}") }
                    IconButton(onClick = { deletingId = cat.id }) { Icon(Icons.Filled.Delete, "Delete ${cat.name}") }
                }
            }
        }
    }

    if (adding) {
        NameDialog(title = "New category", initial = "", onDismiss = { adding = false }) {
            onAdd(it); adding = false
        }
    }
    categories.firstOrNull { it.id == renamingId }?.let { cat ->
        NameDialog(title = "Rename category", initial = cat.name, onDismiss = { renamingId = -1L }) {
            onRename(cat, it); renamingId = -1L
        }
    }
    categories.firstOrNull { it.id == deletingId }?.let { cat ->
        val n = tasks.count { it.categoryId == cat.id }
        AlertDialog(
            onDismissRequest = { deletingId = -1L },
            title = { Text("Delete \"${cat.name}\"?") },
            text = { Text(if (n == 0) "This category is empty." else "Its $n task(s) will be deleted too.") },
            confirmButton = { TextButton(onClick = { onDelete(cat); deletingId = -1L }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deletingId = -1L }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Name") })
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
